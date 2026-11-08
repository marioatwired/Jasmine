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
import com.github.marioatwired.jasmine.queue.AbstractQueue;
import com.github.marioatwired.jasmine.service.dto.ForceSkipResult;
import com.github.marioatwired.jasmine.service.dto.TrackAddResult;
import com.github.marioatwired.jasmine.service.dto.VolumeResult;
import com.github.marioatwired.jasmine.settings.RepeatMode;
import com.github.marioatwired.jasmine.settings.Settings;
import com.github.marioatwired.jasmine.utils.FormatUtil;
import com.github.marioatwired.jasmine.utils.TimeUtil;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Playback control, track loading, and track enqueue operations.
 * <p>
 * Queue inspection and mutation live in {@link QueueService}.
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

    public void previous(Guild guild, Member member, OutputAdapter output)
    {
        LOG.debug("Previous track requested: guild={}, user={}",
                guild.getId(), member.getUser().getName());

        AudioHandler handler = getHandler(guild);
        boolean isDJ = DJCommand.checkDJPermission(bot, guild, member);

        if (!isDJ && handler.getRequestMetadata().getOwner() != member.getIdLong())
        {
            LOG.debug("Previous rejected: user lacks permission");
            output.replyError("You need to be a DJ or the requester to go back!");
            return;
        }
        AudioTrack playing = handler.getPlayer().getPlayingTrack();

        if (playing != null && playing.getPosition() > 5000)
        {
            playing.setPosition(0);
            LOG.info("Track restarted: guild={}, track=\"{}\"", guild.getId(), playing.getInfo().title);
            output.replySuccess("Restarted **" + FormatUtil.getTrackTitle(playing) + "**");
            return;
        }

        var history = handler.getQueue().getHistory();
        if (playing != null && !history.isEmpty())
        {
            QueuedTrack mostRecent = history.get(0);
            AudioTrack mostRecentTrack = mostRecent != null ? mostRecent.getTrack() : null;
            if (mostRecentTrack != null
                    && Objects.equals(mostRecentTrack.getIdentifier(), playing.getIdentifier()))
            {
                handler.getQueue().removeFromHistoryAt(0);
            }
        }

        if (history.isEmpty())
        {
            LOG.debug("Previous rejected: no history available");
            output.replyError("There are no previous tracks!");
            return;
        }

        AudioTrack currentlyPlaying = handler.getPlayer().getPlayingTrack();
        QueuedTrack currentQueued = currentlyPlaying != null
                ? new QueuedTrack(currentlyPlaying.makeClone(), handler.getRequestMetadata())
                : null;

        QueuedTrack previous = handler.getQueue().rewind(currentQueued);
        if (previous != null)
        {
            handler.getPlayer().playTrack(previous.getTrack());
            LOG.info("Went to previous track: guild={}, track=\"{}\"",
                    guild.getId(), previous.getTrack().getInfo().title);
            output.replySuccess("Went back to **" + FormatUtil.getTrackTitle(previous.getTrack()) + "**");
        }
        else
        {
            LOG.debug("Previous failed: no previous tracks in history");
            output.replyError("There are no previous tracks!");
        }
    }

    public void cycleRepeatMode(Guild guild, Member member, OutputAdapter output)
    {
        if (!requireDJPermission(guild, member, output, "use this button"))
            return;

        AudioHandler handler = getHandler(guild);
        RepeatMode mode = getSettings(guild).getRepeatMode();
        RepeatMode nextMode;
        switch (mode) {
            case OFF:
                nextMode = RepeatMode.ALL;
                break;
            case ALL:
                nextMode = RepeatMode.SINGLE;
                break;
            case SINGLE:
            default:
                nextMode = RepeatMode.OFF;
                break;
        }
        getSettings(guild).setRepeatMode(nextMode);
        output.editNowPlaying(handler);
    }

    public RepeatMode getRepeatMode(Guild guild)
    {
        return getSettings(guild).getRepeatMode();
    }

    public void setRepeatMode(Guild guild, RepeatMode mode)
    {
        LOG.info("Repeat mode changed: guild={}, mode={}", guild.getId(), mode.getUserFriendlyName());
        getSettings(guild).setRepeatMode(mode);
    }

    public void adjustVolume(Guild guild, Member member, int change, OutputAdapter output)
    {
        if (!requireDJPermission(guild, member, output, "use this button"))
            return;

        AudioHandler handler = getHandler(guild);
        int newVol = handler.getPlayer().getVolume() + change;
        newVol = Math.max(0, Math.min(150, newVol));
        handler.getPlayer().setVolume(newVol);
        getSettings(guild).setVolume(newVol);
        output.editNowPlaying(handler);
    }

    public int getVolume(Guild guild)
    {
        AudioHandler handler = getHandler(guild);
        return handler.getPlayer().getVolume();
    }

    public VolumeResult setVolume(Guild guild, int volume)
    {
        LOG.debug("Volume change requested: guild={}, volume={}", guild.getId(), volume);

        if (volume < 0 || volume > 150)
        {
            LOG.warn("Volume change rejected: invalid value {} (must be 0-150)", volume);
            return null;
        }

        AudioHandler handler = getHandler(guild);
        int oldVolume = handler.getPlayer().getVolume();
        handler.getPlayer().setVolume(volume);
        getSettings(guild).setVolume(volume);

        LOG.info("Volume changed: guild={}, oldVolume={}, newVolume={}", guild.getId(), oldVolume, volume);

        return new VolumeResult(oldVolume, volume);
    }

    public void stop(Guild guild, Member member, OutputAdapter output)
    {
        if (!requireDJPermission(guild, member, output, "use this button"))
            return;

        AudioHandler handler = getHandler(guild);
        handler.stopAndClearQueuePreserveHistory();
        guild.getAudioManager().closeAudioConnection();
        output.editNoMusic(handler);
    }

    public void stopAndClear(Guild guild)
    {
        LOG.info("Stopping playback and clearing queue: guild={}", guild.getId());

        AudioHandler handler = getHandler(guild);
        handler.stopAndClearQueuePreserveHistory();
        guild.getAudioManager().closeAudioConnection();

        LOG.debug("Audio connection closed: guild={}", guild.getId());
    }

    public void pause(Guild guild, Member member, OutputAdapter output)
    {
        if (!requireDJPermission(guild, member, output, "use this button"))
            return;

        AudioHandler handler = getHandler(guild);
        handler.getPlayer().setPaused(!handler.getPlayer().isPaused());
        output.editNowPlaying(handler);
    }

    public boolean isPaused(Guild guild)
    {
        AudioHandler handler = getHandler(guild);
        return handler.getPlayer().isPaused();
    }

    public String setPaused(Guild guild, boolean paused)
    {
        AudioHandler handler = getHandler(guild);
        handler.getPlayer().setPaused(paused);
        AudioTrack track = handler.getPlayer().getPlayingTrack();
        String trackTitle = track != null ? FormatUtil.getTrackTitle(track) : null;

        LOG.info("Player {} : guild={}, track=\"{}\"",
                paused ? "paused" : "resumed", guild.getId(), trackTitle);

        return trackTitle;
    }

    public void skip(Guild guild, Member member, OutputAdapter output)
    {
        AudioHandler handler = getHandler(guild);
        boolean isDJ = DJCommand.checkDJPermission(bot, guild, member);

        RequestMetadata skipRm = handler.getRequestMetadata();
        if (!isDJ && skipRm.getOwner() != member.getIdLong())
        {
            output.replyError("You need to be a DJ or the requester to skip!");
            return;
        }
        if (getSettings(guild).getRepeatMode() == RepeatMode.ALL)
        {
            var track = handler.getPlayer().getPlayingTrack();
            if (track != null)
                handler.addTrack(new QueuedTrack(track.makeClone(), track.getUserData(RequestMetadata.class)));
        }
        handler.setLastReason(member.getUser().getName() + " skipped forward.");
        handler.getPlayer().stopTrack();
        output.replySuccess("Skipped!");
    }

    public ForceSkipResult forceSkip(Guild guild)
    {
        LOG.debug("Force skip requested: guild={}", guild.getId());

        AudioHandler handler = getHandler(guild);
        AudioTrack track = handler.getPlayer().getPlayingTrack();
        if (track == null)
        {
            LOG.debug("Force skip: nothing playing in guild={}", guild.getId());
            return null;
        }

        RequestMetadata rm = handler.getRequestMetadata();
        String trackTitle = FormatUtil.getTrackTitle(track);
        String requesterInfo = rm.getOwner() == 0L ? "(autoplay)" : "(requested by **" + FormatUtil.formatUsername(rm.user) + "**)";

        handler.getPlayer().stopTrack();

        LOG.info("Track force-skipped: guild={}, track=\"{}\"", guild.getId(), trackTitle);

        return new ForceSkipResult(trackTitle, requesterInfo);
    }

    public void skipWithVote(Guild guild, Member member, int listeners, OutputAdapter output)
    {
        LOG.debug("Skip vote requested: guild={}, user={}, listeners={}",
                guild.getId(), member.getUser().getName(), listeners);

        AudioHandler handler = getHandler(guild);
        RequestMetadata rm = handler.getRequestMetadata();

        double skipRatio = getSettings(guild).getSkipRatio();
        if (skipRatio == -1)
        {
            skipRatio = bot.getConfig().getSkipRatio();
        }

        if (member.getIdLong() == rm.getOwner() || skipRatio == 0)
        {
            String trackTitle = FormatUtil.getTrackTitle(handler.getPlayer().getPlayingTrack());
            handler.getPlayer().stopTrack();
            LOG.info("Track skipped by owner/instant skip: guild={}, user={}, track=\"{}\"",
                    guild.getId(), member.getUser().getName(), trackTitle);
            output.replySuccess("Skipped **" + trackTitle + "**");
            return;
        }

        String oderId = member.getId();
        boolean alreadyVoted = handler.getVotes().contains(oderId);

        if (!alreadyVoted)
        {
            handler.getVotes().add(oderId);
        }

        int skippers = (int) handler.getVotes().stream()
                .filter(id -> guild.getMemberById(id) != null &&
                        guild.getMemberById(id).getVoiceState() != null &&
                        guild.getMemberById(id).getVoiceState().getChannel() != null)
                .count();
        int required = (int) Math.ceil(listeners * skipRatio);

        String voteStatus = "[" + skippers + " votes, " + required + "/" + listeners + " needed]";

        if (alreadyVoted)
        {
            LOG.debug("Duplicate skip vote: guild={}, user={}", guild.getId(), member.getUser().getName());
            output.replyWarning("You already voted to skip this song `" + voteStatus + "`");
        }
        else if (skippers >= required)
        {
            String trackTitle = FormatUtil.getTrackTitle(handler.getPlayer().getPlayingTrack());
            String requester = rm.getOwner() == 0L ? "(autoplay)" : "(requested by **" + FormatUtil.formatUsername(rm.user) + "**)";
            handler.getPlayer().stopTrack();
            LOG.info("Track skipped by vote: guild={}, track=\"{}\", votes={}/{}",
                    guild.getId(), trackTitle, skippers, required);
            output.replySuccess("You voted to skip the song `" + voteStatus + "`\nSkipped **" + trackTitle + "** " + requester);
        }
        else
        {
            LOG.debug("Skip vote registered: guild={}, user={}, votes={}/{}",
                    guild.getId(), member.getUser().getName(), skippers, required);
            output.replySuccess("You voted to skip the song `" + voteStatus + "`");
        }
    }

    public void seek(Guild guild, Member member, String timeString, OutputAdapter output)
    {
        LOG.debug("Seek requested: guild={}, user={}, time={}",
                guild.getId(), member.getUser().getName(), timeString);

        AudioHandler handler = getHandler(guild);
        AudioTrack playingTrack = handler.getPlayer().getPlayingTrack();

        if (playingTrack == null)
        {
            LOG.debug("Seek rejected: no track playing in guild={}", guild.getId());
            output.replyError("There is no track currently playing!");
            return;
        }

        if (!playingTrack.isSeekable())
        {
            LOG.debug("Seek rejected: track not seekable - track=\"{}\"", playingTrack.getInfo().title);
            output.replyError("This track is not seekable.");
            return;
        }

        boolean isDJ = DJCommand.checkDJPermission(bot, guild, member);
        RequestMetadata rm = playingTrack.getUserData(RequestMetadata.class);
        if (!isDJ && (rm == null || rm.getOwner() != member.getIdLong()))
        {
            LOG.debug("Seek rejected: user lacks permission - user={}, track=\"{}\"",
                    member.getUser().getName(), playingTrack.getInfo().title);
            output.replyError("You cannot seek **" + FormatUtil.getTrackTitle(playingTrack) + "** because you didn't add it!");
            return;
        }

        TimeUtil.SeekTime seekTime = TimeUtil.parseTime(timeString);
        if (seekTime == null)
        {
            LOG.debug("Seek rejected: invalid time format - input=\"{}\"", timeString);
            output.replyError("Invalid seek! Expected format: [+ | -] <HH:MM:SS | MM:SS | SS> or <0h0m0s>\nExamples: `1:02:23` `+1:10` `-90`, `1h10m`, `+90s`");
            return;
        }

        long currentPosition = playingTrack.getPosition();
        long trackDuration = playingTrack.getDuration();
        long seekMilliseconds = seekTime.relative ? currentPosition + seekTime.milliseconds : seekTime.milliseconds;

        if (seekMilliseconds < 0)
        {
            seekMilliseconds = 0;
        }
        if (seekMilliseconds > trackDuration)
        {
            LOG.debug("Seek rejected: position {} exceeds track duration {}",
                    TimeUtil.formatTime(seekMilliseconds), TimeUtil.formatTime(trackDuration));
            output.replyError("Cannot seek to `" + TimeUtil.formatTime(seekMilliseconds) + "` because the current track is `" + TimeUtil.formatTime(trackDuration) + "` long!");
            return;
        }

        try
        {
            playingTrack.setPosition(seekMilliseconds);
            LOG.info("Seek successful: guild={}, user={}, track=\"{}\", position={}",
                    guild.getId(), member.getUser().getName(), playingTrack.getInfo().title,
                    TimeUtil.formatTime(playingTrack.getPosition()));
            output.replySuccess("Successfully seeked to `" + TimeUtil.formatTime(playingTrack.getPosition()) + "/" + TimeUtil.formatTime(trackDuration) + "`!");
        }
        catch (Exception e)
        {
            LOG.error("Seek failed: guild={}, track=\"{}\", error={}",
                    guild.getId(), playingTrack.getInfo().title, e.getMessage(), e);
            output.replyError("An error occurred while trying to seek: " + e.getMessage());
        }
    }

    public void playNow(Guild guild, Member member, int position, OutputAdapter output)
    {
        if (!requireDJPermission(guild, member, output, "play a track immediately"))
            return;

        AudioHandler handler = getHandler(guild);
        AbstractQueue<QueuedTrack> queue = handler.getQueue();

        if (isInvalidPosition(queue, position))
        {
            output.replyError("`" + position + "` is not a valid position in the queue!");
            return;
        }

        // Get the track info before moving
        QueuedTrack queuedTrack = queue.get(position - 1);
        String trackTitle = FormatUtil.getTrackTitle(queuedTrack.getTrack());

        // Move the track to position 1
        if (position > 1)
        {
            queue.moveItem(position - 1, 0);
        }

        // Start the selected track immediately, or skip the current one so it starts next.
        if (!playNextIfIdle(handler))
            handler.getPlayer().stopTrack();

        output.replySuccess("Now playing **" + trackTitle + "**");
    }

    public void skipTo(Guild guild, Member member, int position, OutputAdapter output)
    {
        if (!requireDJPermission(guild, member, output, "skip to a specific position"))
            return;

        AudioHandler handler = getHandler(guild);

        if (!validateQueuePosition(handler, position, output))
            return;

        handler.getQueue().skip(position - 1);
        String trackTitle = FormatUtil.getTrackTitle(handler.getQueue().get(0).getTrack());
        if (!playNextIfIdle(handler))
            handler.getPlayer().stopTrack();
        output.replySuccess("Skipped to **" + trackTitle + "**");
    }

    public String skipToPosition(Guild guild, int position)
    {
        LOG.debug("Skip to position: guild={}, position={}", guild.getId(), position);

        AudioHandler handler = getHandler(guild);
        int queueSize = handler.getQueue().size();

        if (position < 1 || position > queueSize)
        {
            LOG.debug("Skip to position rejected: invalid position {} (queueSize={})",
                    position, queueSize);
            return null;
        }

        handler.getQueue().skip(position - 1);
        String trackTitle = FormatUtil.getTrackTitle(handler.getQueue().get(0).getTrack());
        if (!playNextIfIdle(handler))
            handler.getPlayer().stopTrack();

        LOG.info("Skipped to position: guild={}, position={}, track=\"{}\"",
                guild.getId(), position, trackTitle);

        return trackTitle;
    }

    private AudioHandler getHandler(Guild guild)
    {
        return (AudioHandler) guild.getAudioManager().getSendingHandler();
    }

    /**
     * Starts the next queued track when the player is idle. Returns false when
     * a track is already playing, in which case normal skip behavior applies.
     */
    private boolean playNextIfIdle(AudioHandler handler)
    {
        if (handler.getPlayer().getPlayingTrack() != null)
            return false;

        QueuedTrack next = handler.getQueue().pull();
        if (next == null)
            return false;

        handler.getPlayer().playTrack(next.getTrack());
        return true;
    }

    private Settings getSettings(Guild guild)
    {
        return bot.getSettingsManager().getSettings(guild);
    }

    private boolean requireDJPermission(Guild guild, Member member, OutputAdapter output, String action)
    {
        if (!DJCommand.checkDJPermission(bot, guild, member))
        {
            output.replyError("You need to be a DJ to " + action + "!");
            return false;
        }
        return true;
    }

    private boolean isInvalidPosition(AbstractQueue<QueuedTrack> queue, int position)
    {
        return position < 1 || position > queue.size();
    }

    private boolean validateQueuePosition(AudioHandler handler, int position, OutputAdapter output)
    {
        int size = handler.getQueue().size();
        if (position < 1 || position > size)
        {
            output.replyError("Position must be a valid integer between 1 and " + size + "!");
            return false;
        }
        return true;
    }
}
