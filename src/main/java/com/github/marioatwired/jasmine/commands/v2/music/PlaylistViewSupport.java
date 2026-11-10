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
package com.github.marioatwired.jasmine.commands.v2.music;

import com.github.marioatwired.jasmine.playlist.PlaylistLoader;
import com.github.marioatwired.jasmine.service.MusicService;
import com.github.marioatwired.jasmine.service.dto.PlaylistDetailsInfo;
import com.github.marioatwired.jasmine.service.dto.PlaylistNamesInfo;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.util.List;
import java.util.function.Consumer;

/**
 * Shared playlist detail view construction for text and slash commands.
 */
public final class PlaylistViewSupport
{
    private PlaylistViewSupport()
    {
    }

    public static void open(MusicService musicService, Guild guild, Member member, long userId,
                            String requestedName, int requestedPage, Consumer<Result> callback)
    {
        String sanitized = PlaylistLoader.sanitizeName(requestedName);
        if (sanitized.isEmpty())
        {
            callback.accept(error("Please include a playlist name."));
            return;
        }

        PlaylistNamesInfo namesInfo = musicService.getAvailablePlaylistNames();
        if (namesInfo.hasError())
        {
            callback.accept(error(namesInfo.errorMessage));
            return;
        }

        int playlistIndex = -1;
        String playlistName = null;
        for (int i = 0; i < namesInfo.names.size(); i++)
        {
            if (namesInfo.names.get(i).equalsIgnoreCase(sanitized))
            {
                playlistIndex = i + 1;
                playlistName = namesInfo.names.get(i);
                break;
            }
        }
        if (playlistIndex < 1)
        {
            callback.accept(error("Playlist `" + sanitized + "` doesn't exist!"));
            return;
        }

        PlaylistDetailsInfo details = musicService.getPlaylistDetails(playlistName);
        if (details == null)
        {
            callback.accept(error("Playlist `" + playlistName + "` doesn't exist!"));
            return;
        }

        int totalPages = Math.max(1, PlaylistsSlashCmd.getPlaylistTrackTotalPages(details.totalItems));
        int page = Math.max(1, Math.min(requestedPage, totalPages));
        int listPage = (playlistIndex - 1) / PlaylistsSlashCmd.PLAYLISTS_PER_PAGE + 1;
        boolean canEdit = guild != null && member != null
                && musicService.canEditPlaylistEntries(guild, member);
        String resolvedPlaylistName = playlistName;
        int resolvedPlaylistIndex = playlistIndex;

        musicService.loadPlaylistPageWithTracks(resolvedPlaylistName, page,
                PlaylistsSlashCmd.PLAYLIST_TRACKS_PER_PAGE, result ->
                {
                    if (result == null)
                    {
                        callback.accept(error(
                                "Playlist `" + resolvedPlaylistName + "` no longer exists."));
                        return;
                    }

                    int resultPages = Math.max(1,
                            PlaylistsSlashCmd.getPlaylistTrackTotalPages(result.totalItems));
                    int safePage = Math.max(1, Math.min(page, resultPages));
                    int tracksOnPage = PlaylistsSlashCmd.getPlaylistTracksOnPage(
                            safePage, result.totalItems);
                    MessageEmbed embed = PlaylistsSlashCmd.buildPlaylistTracksEmbed(
                            resolvedPlaylistName,
                            result.totalItems,
                            safePage,
                            resultPages,
                            result.formattedLines,
                            0,
                            canEdit,
                            false,
                            member != null ? member.getColors().getPrimary() : null
                    );

                    List<ActionRow> components = guild != null && member != null
                            ? PlaylistsSlashCmd.buildPlaylistDetailsComponents(
                                    resolvedPlaylistIndex,
                                    listPage,
                                    safePage,
                                    resultPages,
                                    tracksOnPage,
                                    0,
                                    userId,
                                    canEdit,
                                    false)
                            : List.of();

                    callback.accept(new Result(null, resolvedPlaylistName, resolvedPlaylistIndex,
                            listPage, embed, components));
                });
    }

    private static Result error(String message)
    {
        return new Result(message, null, 0, 0, null, List.of());
    }

    public record Result(String errorMessage, String playlistName, int playlistIndex,
                         int listPage, MessageEmbed embed, List<ActionRow> components)
    {
        public boolean hasError()
        {
            return errorMessage != null;
        }

        public boolean hasComponents()
        {
            return components != null && !components.isEmpty();
        }
    }
}
