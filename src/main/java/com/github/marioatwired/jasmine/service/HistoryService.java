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
import com.github.marioatwired.jasmine.queue.PlaybackHistory;
import com.github.marioatwired.jasmine.service.dto.HistoryInfo;
import com.github.marioatwired.jasmine.utils.FormatUtil;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import java.util.ArrayList;
import java.util.List;

/**
 * Playback history queries and history-to-queue operations.
 */
public class HistoryService
{
    private final Bot bot;
    private final MusicService musicService;

    public HistoryService(Bot bot, MusicService musicService)
    {
        this.bot = bot;
        this.musicService = musicService;
    }

    private AudioHandler getHandler(Guild guild)
    {
        return (AudioHandler) guild.getAudioManager().getSendingHandler();
    }

    public HistoryInfo getHistoryInfo(Guild guild, JDA jda)
    {
        AudioHandler handler = getHandler(guild);
        if (handler == null)
        {
            return null;
        }

        List<QueuedTrack> list = handler.getPreviousTracks();
        int maxSize = handler.getQueue().getHistory().getMaxSize();
        boolean disabled = maxSize == 0;

        long totalDuration = 0;
        String[] trackStrings = new String[list.size()];
        for (int i = 0; i < list.size(); i++)
        {
            totalDuration += list.get(i).getTrack().getDuration();
            trackStrings[i] = list.get(i).toString();
        }

        return new HistoryInfo(trackStrings, totalDuration, maxSize, disabled);
    }

    public void queueFromHistory(Guild guild, Member member, int historyPosition,
                                 TextChannel channel, OutputAdapter output)
    {
        AudioHandler handler = getHandler(guild);
        if (handler == null)
        {
            output.replyError("There is no player in this server!");
            return;
        }

        var history = handler.getQueue().getHistory();
        if (!requireHistoryEnabled(history, output))
        {
            return;
        }
        if (history.isEmpty())
        {
            output.replyError("Playback history is empty!");
            return;
        }
        if (historyPosition < 1 || historyPosition > history.size())
        {
            output.replyError("Position must be between 1 and " + history.size() + "!");
            return;
        }

        int index = historyPosition - 1;
        QueuedTrack qt = history.get(index);
        handler.getQueue().removeFromHistoryAt(index);

        AudioTrack track = qt.getTrack().makeClone();
        if (musicService.isTooLong(track))
        {
            output.replyError(musicService.formatTooLongError(track));
            return;
        }

        RequestMetadata rm = new RequestMetadata(member.getUser(),
                new RequestMetadata.RequestInfo(qt.getTrack().getInfo().uri, qt.getTrack().getInfo().uri),
                channel.getIdLong());
        QueuedTrack newQt = new QueuedTrack(track, rm);
        handler.setLastReason(member.getUser().getName() + " added from history.");
        int position = handler.addTrack(newQt) + 1;
        String title = FormatUtil.getTrackTitle(track);
        output.replySuccess(musicService.formatTrackAddedMessage(title, track.getDuration(), position));
    }

    public void queueAllFromHistory(Guild guild, Member member, TextChannel channel,
                                    OutputAdapter output)
    {
        AudioHandler handler = getHandler(guild);
        if (handler == null)
        {
            output.replyError("There is no player in this server!");
            return;
        }

        var history = handler.getQueue().getHistory();
        if (!requireHistoryEnabled(history, output))
        {
            return;
        }
        if (history.isEmpty())
        {
            output.replyError("Playback history is empty!");
            return;
        }

        handler.setLastReason(member.getUser().getName() + " added entire history to queue.");
        int added = 0;
        int skipped = 0;

        // Work from a stable snapshot and clear source entries before adding tracks so
        // onTrackStart history updates cannot be accidentally removed by this operation.
        List<QueuedTrack> historySnapshot = new ArrayList<>(history.getList());
        handler.getQueue().clearHistory();
        for (QueuedTrack qt : historySnapshot)
        {
            AudioTrack track = qt.getTrack().makeClone();
            if (musicService.isTooLong(track))
            {
                skipped++;
                continue;
            }

            RequestMetadata rm = new RequestMetadata(member.getUser(),
                    new RequestMetadata.RequestInfo(qt.getTrack().getInfo().uri, qt.getTrack().getInfo().uri),
                    channel.getIdLong());
            QueuedTrack newQt = new QueuedTrack(track, rm);
            handler.addTrack(newQt);
            added++;
        }

        if (added == 0 && skipped > 0)
        {
            output.replyError("All " + skipped + " track(s) were too long to add.");
            return;
        }
        String message = "Added **" + added + "** track(s) to the queue.";
        if (skipped > 0)
        {
            message += " Skipped " + skipped + " track(s) (too long).";
        }
        output.replySuccess(message);
        if (handler.getPlayer().getPlayingTrack() != null)
        {
            bot.getNowplayingHandler().requestReconcile(guild.getIdLong(), "history-queueall-loaded");
        }
    }

    public void playFromHistoryNow(Guild guild, Member member, int historyPosition,
                                   TextChannel channel, OutputAdapter output)
    {
        AudioHandler handler = getHandler(guild);
        if (handler == null)
        {
            output.replyError("There is no player in this server!");
            return;
        }

        var history = handler.getQueue().getHistory();
        if (!requireHistoryEnabled(history, output))
        {
            return;
        }
        if (history.isEmpty())
        {
            output.replyError("Playback history is empty!");
            return;
        }
        if (historyPosition < 1 || historyPosition > history.size())
        {
            output.replyError("Position must be between 1 and " + history.size() + "!");
            return;
        }

        int index = historyPosition - 1;
        QueuedTrack qt = history.get(index);
        handler.getQueue().removeFromHistoryAt(index);

        AudioTrack track = qt.getTrack().makeClone();
        if (musicService.isTooLong(track))
        {
            output.replyError(musicService.formatTooLongError(track));
            return;
        }

        RequestMetadata rm = new RequestMetadata(member.getUser(),
                new RequestMetadata.RequestInfo(qt.getTrack().getInfo().uri, qt.getTrack().getInfo().uri),
                channel.getIdLong());
        QueuedTrack newQt = new QueuedTrack(track, rm);
        handler.setLastReason(member.getUser().getName() + " playing from history.");
        handler.addTrackToFront(newQt);
        if (handler.getPlayer().getPlayingTrack() != null)
        {
            handler.getPlayer().stopTrack();
        }
        String title = FormatUtil.getTrackTitle(track);
        output.replySuccess("Now playing **" + FormatUtil.filter(title) + "**");
    }

    private boolean requireHistoryEnabled(PlaybackHistory<QueuedTrack> history, OutputAdapter output)
    {
        if (history.getMaxSize() == 0)
        {
            output.replyError(MusicService.HISTORY_DISABLED_MESSAGE);
            return false;
        }
        return true;
    }
}
