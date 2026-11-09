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
import com.github.marioatwired.jasmine.commands.v1.DJCommand;
import com.github.marioatwired.jasmine.playlist.PlaylistLoader;
import com.github.marioatwired.jasmine.queue.PlaybackHistory;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;

import java.util.ArrayList;
import java.util.List;

/**
 * Exports the playback history to a saved playlist.
 */
public class HistoryPlaylistService
{
    private final Bot bot;

    public HistoryPlaylistService(Bot bot)
    {
        this.bot = bot;
    }

    public void saveHistoryAsPlaylist(Guild guild, Member member, String playlistName, OutputAdapter output)
    {
        if (!canSaveHistoryAsPlaylist(guild, member))
        {
            output.replyError("You need to be a DJ or the bot owner to save history as a playlist!");
            return;
        }

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

        List<QueuedTrack> previous = handler.getPreviousTracks();
        List<String> uris = extractHttpUrisFromHistory(previous);
        if (uris.isEmpty())
        {
            output.replyWarning("No valid track URLs in history to save (only http(s) sources are stored).");
            return;
        }

        String sanitized = PlaylistLoader.sanitizeName(playlistName);
        if (sanitized.isEmpty())
        {
            output.replyError("Please provide a valid playlist name!");
            return;
        }

        if (bot.getPlaylistLoader().getPlaylistResult(sanitized).isSuccess())
        {
            output.replyError("Playlist `" + sanitized + "` already exists! Choose a different name.");
            return;
        }

        PlaylistLoader.PlaylistResult<Void> createResult = bot.getPlaylistLoader().createPlaylistResult(sanitized);
        if (!createResult.isSuccess())
        {
            output.replyError(PlaylistErrorMessages.toMessage(createResult.getError()));
            return;
        }

        String content = String.join("\r\n", uris);
        PlaylistLoader.PlaylistResult<Void> writeResult = bot.getPlaylistLoader().writePlaylistResult(sanitized, content);
        if (!writeResult.isSuccess())
        {
            output.replyError(PlaylistErrorMessages.toMessage(writeResult.getError()));
            return;
        }
        output.replySuccess("Saved " + uris.size() + " tracks from history to playlist `" + sanitized + "`!");
    }

    private AudioHandler getHandler(Guild guild)
    {
        return (AudioHandler) guild.getAudioManager().getSendingHandler();
    }

    private boolean canSaveHistoryAsPlaylist(Guild guild, Member member)
    {
        return bot.getConfig().getOwnerId() == member.getIdLong()
                || DJCommand.checkDJPermission(bot, guild, member);
    }

    private static List<String> extractHttpUrisFromHistory(List<QueuedTrack> previous)
    {
        List<String> uris = new ArrayList<>();
        for (QueuedTrack qt : previous)
        {
            String uri = qt.getTrack().getInfo().uri;
            if (uri != null && uri.startsWith("http"))
            {
                uris.add(uri);
            }
        }
        return uris;
    }

    private boolean requireHistoryEnabled(PlaybackHistory<QueuedTrack> history, OutputAdapter output)
    {
        if (history.getMaxSize() == 0)
        {
            output.replyError(HistoryService.HISTORY_DISABLED_MESSAGE);
            return false;
        }
        return true;
    }
}
