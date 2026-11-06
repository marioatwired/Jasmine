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
import com.github.marioatwired.jasmine.settings.Settings;
import com.github.marioatwired.jasmine.utils.FormatUtil;
import com.github.marioatwired.jasmine.utils.TimeUtil;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;

import java.util.List;

/**
 * Read-only formatting and data assembly for now-playing and queue views.
 */
public class NowPlayingService
{
    private final Bot bot;

    public NowPlayingService(Bot bot)
    {
        this.bot = bot;
    }

    private AudioHandler getHandler(Guild guild)
    {
        return (AudioHandler) guild.getAudioManager().getSendingHandler();
    }

    private Settings getSettings(Guild guild)
    {
        return bot.getSettingsManager().getSettings(guild);
    }

    public MusicService.NowPlayingInfo getNowPlayingInfo(Guild guild, JDA jda)
    {
        AudioHandler handler = getHandler(guild);
        if (handler == null)
        {
            return null;
        }

        return new MusicService.NowPlayingInfo(
                handler.getNowPlaying(jda),
                handler.getNoMusicPlaying(jda),
                handler.getPlayer().getPlayingTrack() != null
        );
    }

    public MusicService.QueueInfo getQueueInfo(Guild guild, JDA jda)
    {
        AudioHandler handler = getHandler(guild);
        if (handler == null)
        {
            return null;
        }

        List<QueuedTrack> list = handler.getQueue().getList();
        Settings settings = getSettings(guild);

        long totalDuration = 0;
        String[] trackStrings = new String[list.size()];
        for (int i = 0; i < list.size(); i++)
        {
            totalDuration += list.get(i).getTrack().getDuration();
            trackStrings[i] = list.get(i).toString();
        }

        String nowPlayingTitle = null;
        String statusEmoji = handler.getStatusEmoji();
        if (handler.getPlayer().getPlayingTrack() != null)
        {
            nowPlayingTitle = FormatUtil.getTrackTitle(handler.getPlayer().getPlayingTrack());
        }

        return new MusicService.QueueInfo(
                trackStrings,
                totalDuration,
                nowPlayingTitle,
                statusEmoji,
                settings.getRepeatMode(),
                settings.getQueueType(),
                handler.getNowPlaying(jda),
                handler.getNoMusicPlaying(jda)
        );
    }

    public String formatQueueTitle(MusicService.QueueInfo info, String successEmoji)
    {
        StringBuilder sb = new StringBuilder();
        if (info.nowPlayingTitle != null)
        {
            sb.append(info.statusEmoji).append(" **").append(info.nowPlayingTitle).append("**\n");
        }

        return FormatUtil.filter(sb.append(successEmoji).append(" Current Queue | ").append(info.tracks.length)
                .append(" entries | `").append(TimeUtil.formatTime(info.totalDuration)).append("` ")
                .append("| ").append(info.queueType.getEmoji()).append(" `").append(info.queueType.getUserFriendlyName()).append('`')
                .append(info.repeatMode.getEmoji() != null ? " | " + info.repeatMode.getEmoji() : "").toString());
    }
}
