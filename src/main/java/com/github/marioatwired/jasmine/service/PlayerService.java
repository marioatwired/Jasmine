/*
 * Copyright 2026 Arif Banai (arif-banai)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.marioatwired.jasmine.service;

import com.github.marioatwired.jasmine.Bot;
import com.github.marioatwired.jasmine.audio.AudioHandler;
import com.github.marioatwired.jasmine.audio.QueuedTrack;
import com.github.marioatwired.jasmine.audio.RequestMetadata;
import com.github.marioatwired.jasmine.commands.v1.DJCommand;
import com.github.marioatwired.jasmine.service.dto.TrackAddResult;
import com.github.marioatwired.jasmine.utils.FormatUtil;
import com.github.marioatwired.jasmine.utils.TimeUtil;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Track loading and enqueue operations.
 * <p>
 * Playback control and DJ actions live in {@link PlaybackControlService}.
 */
public class PlayerService implements TrackEnqueuer
{
    private static final Logger LOG = LoggerFactory.getLogger(PlayerService.class);

    private final Bot bot;

    public PlayerService(Bot bot)
    {
        this.bot = bot;
    }

    @FunctionalInterface
    private interface TrackAdder
    {
        int add(AudioHandler handler, QueuedTrack track);
    }

    public boolean isTooLong(AudioTrack track)
    {
        return bot.getConfig().isTooLong(track);
    }

    public String formatTooLongError(AudioTrack track)
    {
        String title = FormatUtil.getTrackTitle(track);
        return "This track (**" + title + "**) is longer than the allowed maximum: `"
                + TimeUtil.formatTime(track.getDuration()) + "` > `" + bot.getConfig().getMaxTime() + "`";
    }

    public String formatTrackAddedMessage(String title, long duration, int position)
    {
        return "Added **" + FormatUtil.filter(title) + "** (`" + TimeUtil.formatTime(duration) + "`) "
                + (position == 0 ? "to begin playing" : " to the queue at position " + position);
    }

    private TrackAddResult addTrackInternal(Guild guild, Member member, AudioTrack track,
                                            String queryArgs, TextChannel channel,
                                            TrackAdder adder, String reason, String logLocation)
    {
        LOG.debug("Adding track to {}: guild={}, user={}, track={}",
                logLocation, guild.getId(), member.getUser().getName(), track.getInfo().title);

        if (isTooLong(track))
        {
            LOG.warn("Track rejected (too long): {} - duration: {} > max: {}",
                    track.getInfo().title, TimeUtil.formatTime(track.getDuration()), bot.getConfig().getMaxTime());
            return null;
        }

        AudioHandler handler = getHandler(guild);
        handler.setLastReason(member.getUser().getName() + " " + reason);
        QueuedTrack queuedTrack = new QueuedTrack(track,
                new RequestMetadata(member.getUser(),
                        new RequestMetadata.RequestInfo(queryArgs, track.getInfo().uri),
                        channel.getIdLong()));
        int position = adder.add(handler, queuedTrack) + 1;

        String title = FormatUtil.getTrackTitle(track);
        String message = formatTrackAddedMessage(title, track.getDuration(), position);

        LOG.info("Track added to {}: guild={}, user={}, track=\"{}\", position={}",
                logLocation, guild.getId(), member.getUser().getName(), title, position);

        return new TrackAddResult(position, message, title);
    }

    public TrackAddResult addTrackToQueue(Guild guild, Member member, AudioTrack track,
                                          String queryArgs, TextChannel channel)
    {
        return addTrackInternal(guild, member, track, queryArgs, channel,
                AudioHandler::addTrack, "added to the queue.", "queue");
    }

    public TrackAddResult addTrackToFront(Guild guild, Member member, AudioTrack track,
                                          String queryArgs, TextChannel channel)
    {
        return addTrackInternal(guild, member, track, queryArgs, channel,
                AudioHandler::addTrackToFront, "added to the front of the queue.", "front of queue");
    }

    public void playNext(Guild guild, Member member, String args, TextChannel channel, OutputAdapter output)
    {
        LOG.debug("PlayNext requested: guild={}, user={}, query={}",
                guild.getId(), member.getUser().getName(), args);

        if (args == null || args.isEmpty())
        {
            LOG.debug("PlayNext rejected: empty query");
            output.replyWarning("Please include a song title or URL!");
            return;
        }

        if (args.startsWith("<") && args.endsWith(">"))
            args = args.substring(1, args.length() - 1);

        LOG.info("Loading track for playNext: guild={}, user={}, query=\"{}\"",
                guild.getId(), member.getUser().getName(), args);

        bot.getPlayerManager().loadItemOrdered(guild, args,
                bot.getAudioLoadWrapper().wrap(args, new AudioLoadResultHandlers.PlayNextResultHandler(this, bot, output, guild, member, args, false, channel)));
    }

    public void play(Guild guild, Member member, String args, TextChannel channel, OutputAdapter output)
    {
        LOG.debug("Play requested: guild={}, user={}, args={}",
                guild.getId(), member.getUser().getName(), args);

        if (args != null && args.startsWith("\"") && args.endsWith("\""))
            args = args.substring(1, args.length() - 1);

        if (args == null || args.isEmpty())
        {
            AudioHandler handler = getHandler(guild);
            if (handler.getPlayer().getPlayingTrack() != null && handler.getPlayer().isPaused())
            {
                if (DJCommand.checkDJPermission(bot, guild, member))
                {
                    handler.getPlayer().setPaused(false);
                    LOG.info("Playback resumed: guild={}, user={}, track=\"{}\"",
                            guild.getId(), member.getUser().getName(), handler.getPlayer().getPlayingTrack().getInfo().title);
                    String resumedTitle = FormatUtil.getTrackTitle(handler.getPlayer().getPlayingTrack());
                    output.replySuccess("Resumed **" + resumedTitle + "**.");
                }
                else
                {
                    LOG.debug("Resume rejected: user lacks DJ permission");
                    output.replyError("Only DJs can unpause the player!");
                }
                return;
            }

            if (handler.getPlayer().getPlayingTrack() == null && handler.getQueue() != null)
            {
                QueuedTrack next = handler.getQueue().pull();
                if (next != null)
                {
                    handler.getPlayer().playTrack(next.getTrack());
                    LOG.info("Resumed queue: guild={}, track=\"{}\"",
                            guild.getId(), FormatUtil.getTrackTitle(next.getTrack()));
                    output.replySuccess("Resumed **" + FormatUtil.getTrackTitle(next.getTrack()) + "**.");
                    return;
                }
            }

            output.onShowHelp();
            return;
        }

        LOG.info("Loading track: guild={}, user={}, query=\"{}\"",
                guild.getId(), member.getUser().getName(), args);

        bot.getPlayerManager().loadItemOrdered(guild, args,
                bot.getAudioLoadWrapper().wrap(args, new AudioLoadResultHandlers.PlayResultHandler(this, bot, output, guild, member, args, false, channel)));
    }

    private AudioHandler getHandler(Guild guild)
    {
        return (AudioHandler) guild.getAudioManager().getSendingHandler();
    }
}
