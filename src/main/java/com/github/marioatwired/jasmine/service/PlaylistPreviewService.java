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
import com.github.marioatwired.jasmine.playlist.PlaylistLoader.Playlist;
import com.github.marioatwired.jasmine.playlist.PlaylistLoader;
import com.github.marioatwired.jasmine.service.dto.PlaylistPreviewWithTracks;
import com.github.marioatwired.jasmine.service.dto.PlaylistTracksPageInfo;
import com.github.marioatwired.jasmine.utils.FormatUtil;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Loads playlist track previews and paginated playlist views.
 */
public class PlaylistPreviewService
{
    private static final String COULD_NOT_LOAD_LINE = "`[?:??]` **Could not load**";

    private final Bot bot;

    public PlaylistPreviewService(Bot bot)
    {
        this.bot = bot;
    }

    public void loadPlaylistPreviewWithTracks(String playlistName, int maxPreview,
                                             Consumer<PlaylistPreviewWithTracks> callback)
    {
        loadPlaylistLines(playlistName, 0, maxPreview, false, result ->
        {
            if (result == null)
            {
                callback.accept(null);
                return;
            }
            callback.accept(new PlaylistPreviewWithTracks(
                    result.playlistName,
                    result.totalItems,
                    result.hasMore,
                    result.formattedLines
            ));
        });
    }

    public void loadPlaylistPageWithTracks(String playlistName, int page, int pageSize,
                                           Consumer<PlaylistTracksPageInfo> callback)
    {
        int safePage = Math.max(1, page);
        int safePageSize = Math.max(1, pageSize);
        int startIndex = (safePage - 1) * safePageSize;
        loadPlaylistLines(playlistName, startIndex, safePageSize, false, callback);
    }

    public void loadPlaylistPageWithTracks(String playlistName, List<String> sourceItems, int page, int pageSize,
                                           boolean dirty, Consumer<PlaylistTracksPageInfo> callback)
    {
        int safePage = Math.max(1, page);
        int safePageSize = Math.max(1, pageSize);
        int startIndex = (safePage - 1) * safePageSize;
        loadPlaylistLines(playlistName, sourceItems, startIndex, safePageSize, dirty, callback);
    }

    public String getPlaylistTrackUrlAtPosition(String playlistName, int position)
    {
        PlaylistLoader.PlaylistResult<Playlist> playlistResult = bot.getPlaylistLoader().getPlaylistResult(playlistName);
        if (!playlistResult.isSuccess())
        {
            return null;
        }
        return getPlaylistTrackUrlAtPosition(playlistResult.getValue().getItems(), position);
    }

    public String getPlaylistTrackUrlAtPosition(List<String> items, int position)
    {
        if (position < 1 || position > items.size())
        {
            return null;
        }
        return items.get(position - 1);
    }

    private void loadPlaylistLines(String playlistName, int startIndex, int maxCount,
                                   boolean dirty, Consumer<PlaylistTracksPageInfo> callback)
    {
        PlaylistLoader.PlaylistResult<Playlist> playlistResult = bot.getPlaylistLoader().getPlaylistResult(playlistName);
        if (!playlistResult.isSuccess())
        {
            callback.accept(null);
            return;
        }
        Playlist playlist = playlistResult.getValue();
        loadPlaylistLines(playlist.getName(), playlist.getItems(), startIndex, maxCount, dirty, callback);
    }

    private void loadPlaylistLines(String playlistName, List<String> sourceItems, int startIndex, int maxCount,
                                   boolean dirty, Consumer<PlaylistTracksPageInfo> callback)
    {
        List<String> items = sourceItems == null ? List.of() : sourceItems;
        int safeStartIndex = Math.max(0, startIndex);
        if (safeStartIndex >= items.size())
        {
            callback.accept(new PlaylistTracksPageInfo(
                    playlistName, items.size(), safeStartIndex, false, dirty, new ArrayList<>()));
            return;
        }

        int toLoad = Math.min(Math.max(0, maxCount), items.size() - safeStartIndex);
        if (toLoad == 0)
        {
            callback.accept(new PlaylistTracksPageInfo(
                    playlistName, items.size(), safeStartIndex, items.size() > safeStartIndex, dirty, new ArrayList<>()));
            return;
        }

        String[] lines = new String[toLoad];
        AtomicInteger completed = new AtomicInteger(0);
        Runnable maybeDone = () ->
        {
            if (completed.incrementAndGet() == toLoad)
            {
                callback.accept(new PlaylistTracksPageInfo(
                        playlistName,
                        items.size(),
                        safeStartIndex,
                        safeStartIndex + toLoad < items.size(),
                        dirty,
                        Arrays.asList(lines)
                ));
            }
        };

        for (int i = 0; i < toLoad; i++)
        {
            int index = i;
            String url = items.get(safeStartIndex + i);
            Object orderingId = "playlist-lines-" + playlistName + "-" + (safeStartIndex + i);
            bot.getPlayerManager().loadItemOrdered(orderingId, url, new AudioLoadResultHandler()
            {
                @Override
                public void trackLoaded(AudioTrack track)
                {
                    lines[index] = FormatUtil.formatTrackLineForEmbed(track);
                    maybeDone.run();
                }

                @Override
                public void playlistLoaded(AudioPlaylist ap)
                {
                    AudioTrack single;
                    if (ap.isSearchResult() && !ap.getTracks().isEmpty())
                    {
                        single = ap.getTracks().get(0);
                    }
                    else if (ap.getSelectedTrack() != null)
                    {
                        single = ap.getSelectedTrack();
                    }
                    else if (!ap.getTracks().isEmpty())
                    {
                        single = ap.getTracks().get(0);
                    }
                    else
                    {
                        lines[index] = COULD_NOT_LOAD_LINE;
                        maybeDone.run();
                        return;
                    }
                    lines[index] = FormatUtil.formatTrackLineForEmbed(single);
                    maybeDone.run();
                }

                @Override
                public void noMatches()
                {
                    lines[index] = COULD_NOT_LOAD_LINE;
                    maybeDone.run();
                }

                @Override
                public void loadFailed(FriendlyException exception)
                {
                    lines[index] = COULD_NOT_LOAD_LINE;
                    maybeDone.run();
                }
            });
        }
    }
}
