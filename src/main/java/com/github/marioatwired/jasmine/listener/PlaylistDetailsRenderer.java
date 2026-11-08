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
package com.github.marioatwired.jasmine.listener;

import com.github.marioatwired.jasmine.Bot;
import com.github.marioatwired.jasmine.commands.v2.music.PlaylistsSlashCmd;
import com.github.marioatwired.jasmine.service.MusicService;
import com.github.marioatwired.jasmine.service.dto.PlaylistDraftContext;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;

import java.awt.Color;
import java.util.List;
import java.util.function.Consumer;

/**
 * Builds and renders the playlist details and move-selection views.
 */
class PlaylistDetailsRenderer
{
    private final MusicService musicService;

    PlaylistDetailsRenderer(Bot bot)
    {
        this.musicService = bot.getMusicService();
    }

    void render(ButtonInteractionEvent event, int playlistIndex, int listPage,
                int detailsPage, int selectedTrack, long userId, String playlistName,
                PlaylistDraftContext draftContext)
    {
        render(result -> event.editMessageEmbeds(result.embed()).setComponents(result.components()).queue(),
                message -> event.editMessage(message).setEmbeds().setComponents().queue(),
                event.getGuild(), event.getMember(), playlistIndex, listPage, detailsPage, selectedTrack, userId,
                playlistName, draftContext);
    }

    void render(StringSelectInteractionEvent event, int playlistIndex, int listPage,
                int detailsPage, int selectedTrack, long userId, String playlistName,
                PlaylistDraftContext draftContext)
    {
        render(result -> event.editMessageEmbeds(result.embed()).setComponents(result.components()).queue(),
                message -> event.editMessage(message).setEmbeds().setComponents().queue(),
                event.getGuild(), event.getMember(), playlistIndex, listPage, detailsPage, selectedTrack, userId,
                playlistName, draftContext);
    }

    void render(InteractionHook hook, ButtonInteractionEvent event,
                int playlistIndex, int listPage, int detailsPage,
                int selectedTrack, long userId, String playlistName,
                PlaylistDraftContext draftContext)
    {
        render(result -> hook.editOriginalEmbeds(result.embed()).setComponents(result.components()).queue(),
                message -> hook.editOriginal(message)
                        .setEmbeds(java.util.Collections.emptyList())
                        .setComponents(java.util.Collections.emptyList())
                        .queue(),
                event.getGuild(), event.getMember(), playlistIndex, listPage, detailsPage, selectedTrack, userId,
                playlistName, draftContext);
    }

    private void render(Consumer<PlaylistDetailsRenderResult> renderTarget,
                        Consumer<String> onMissingPlaylist,
                        Guild guild,
                        Member member,
                        int playlistIndex, int listPage, int detailsPage,
                        int selectedTrack, long userId, String playlistName,
                        PlaylistDraftContext draftContext)
    {
        musicService.loadPlaylistPageWithTracks(
                draftContext, detailsPage, PlaylistsSlashCmd.PLAYLIST_TRACKS_PER_PAGE, result ->
                {
                    if (result == null)
                    {
                        onMissingPlaylist.accept("Playlist no longer exists. Click Back and Refresh.");
                        return;
                    }
                    int totalPages = Math.max(1, PlaylistsSlashCmd.getPlaylistTrackTotalPages(result.totalItems));
                    int safePage = Math.max(1, Math.min(detailsPage, totalPages));
                    int tracksOnPage = PlaylistsSlashCmd.getPlaylistTracksOnPage(safePage, result.totalItems);
                    int firstOnPage = (safePage - 1) * PlaylistsSlashCmd.PLAYLIST_TRACKS_PER_PAGE + 1;
                    int lastOnPage = firstOnPage + tracksOnPage - 1;
                    int safeSelected = (selectedTrack >= firstOnPage && selectedTrack <= lastOnPage) ? selectedTrack : 0;

                    Color color = member != null ? member.getColors().getPrimary() : null;
                    MessageEmbed embed = PlaylistsSlashCmd.buildPlaylistTracksEmbed(
                            result.playlistName, result.totalItems, safePage, totalPages, result.formattedLines,
                            safeSelected, result.dirty, color);
                    boolean canEdit = guild != null
                            && member != null
                            && musicService.canEditPlaylistEntries(guild, member);
                    List<ActionRow> components = PlaylistsSlashCmd.buildPlaylistDetailsComponents(
                            playlistIndex, listPage, safePage, totalPages, tracksOnPage, safeSelected, userId,
                            canEdit, result.dirty);
                    renderTarget.accept(new PlaylistDetailsRenderResult(embed, components));
                });
    }

    void renderMoveMenu(ButtonInteractionEvent event, PlaylistDraftContext draftContext,
                        int playlistIndex, int listPage, int detailsPage, int selectedTrack,
                        long userId)
    {
        musicService.loadPlaylistPageWithTracks(
                draftContext, detailsPage, PlaylistsSlashCmd.PLAYLIST_TRACKS_PER_PAGE, result ->
                {
                    if (result == null)
                    {
                        event.reply("Playlist no longer exists. Click Back and Refresh.").setEphemeral(true).queue();
                        return;
                    }
                    int totalPages = Math.max(1, PlaylistsSlashCmd.getPlaylistTrackTotalPages(result.totalItems));
                    int safePage = Math.max(1, Math.min(detailsPage, totalPages));
                    int firstOnPage = (safePage - 1) * PlaylistsSlashCmd.PLAYLIST_TRACKS_PER_PAGE + 1;
                    int tracksOnPage = PlaylistsSlashCmd.getPlaylistTracksOnPage(safePage, result.totalItems);
                    int lastOnPage = firstOnPage + tracksOnPage - 1;
                    int safeSelected = (selectedTrack >= firstOnPage && selectedTrack <= lastOnPage) ? selectedTrack : 0;

                    MessageEmbed embed = PlaylistsSlashCmd.buildPlaylistTracksEmbed(
                            result.playlistName,
                            result.totalItems,
                            safePage,
                            totalPages,
                            result.formattedLines,
                            safeSelected,
                            result.dirty,
                            event.getMember() != null ? event.getMember().getColors().getPrimary() : null
                    );

                    StringSelectMenu.Builder menuBuilder = StringSelectMenu
                            .create("playlistdetails_move_select_" + playlistIndex + "_" + listPage + "_"
                                    + safeSelected + "_" + safePage + "_" + userId)
                            .setPlaceholder("Select new position")
                            .setMinValues(1)
                            .setMaxValues(1);
                    int maxOptions = Math.min(result.totalItems, 25);
                    for (int i = 1; i <= maxOptions; i++)
                    {
                        if (i != safeSelected)
                        {
                            menuBuilder.addOption("Position " + i, String.valueOf(i));
                        }
                    }
                    String cancelMoveId = "playlistdetails_" + playlistIndex + "_" + listPage + "_cancelmove_"
                            + safePage + "_" + safeSelected + "_" + userId;
                    event.editMessageEmbeds(embed)
                            .setComponents(
                                    ActionRow.of(menuBuilder.build()),
                                    ActionRow.of(Button.secondary(cancelMoveId, "Back"))
                            )
                            .queue();
                });
    }

    private record PlaylistDetailsRenderResult(MessageEmbed embed, List<ActionRow> components)
    {
    }
}
