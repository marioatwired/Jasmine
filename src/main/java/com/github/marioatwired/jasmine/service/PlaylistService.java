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
import com.github.marioatwired.jasmine.playlist.PlaylistLoader.Playlist;
import com.github.marioatwired.jasmine.playlist.PlaylistLoader;
import com.github.marioatwired.jasmine.service.dto.PlaylistDetailsInfo;
import com.github.marioatwired.jasmine.service.dto.PlaylistDraftContext;
import com.github.marioatwired.jasmine.service.dto.PlaylistDraftMutationResult;
import com.github.marioatwired.jasmine.service.dto.PlaylistMutationResult;
import com.github.marioatwired.jasmine.service.dto.PlaylistNamesInfo;
import com.github.marioatwired.jasmine.service.dto.PlaylistPreviewWithTracks;
import com.github.marioatwired.jasmine.service.dto.PlaylistTracksPageInfo;
import com.github.marioatwired.jasmine.service.dto.TrackAddResult;
import com.github.marioatwired.jasmine.utils.FormatUtil;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Saved playlist management and playlist operation orchestration.
 */
public class PlaylistService
{
    private final Bot bot;
    private final TrackEnqueuer trackEnqueuer;
    private final PlaylistDraftService playlistDraftService;
    private final PlaylistMutationService playlistMutationService;
    private final PlaylistPreviewService playlistPreviewService;
    private final FavoritesService favoritesService;
    private final HistoryPlaylistService historyPlaylistService;

    public PlaylistService(Bot bot, TrackEnqueuer trackEnqueuer)
    {
        this.bot = bot;
        this.trackEnqueuer = trackEnqueuer;
        this.playlistDraftService = new PlaylistDraftService(bot);
        this.playlistMutationService = new PlaylistMutationService(bot);
        this.playlistPreviewService = new PlaylistPreviewService(bot);
        this.favoritesService = new FavoritesService(bot);
        this.historyPlaylistService = new HistoryPlaylistService(bot);
    }

    private AudioHandler getHandler(Guild guild)
    {
        return (AudioHandler) guild.getAudioManager().getSendingHandler();
    }

    private boolean isTooLong(AudioTrack track)
    {
        return trackEnqueuer.isTooLong(track);
    }

    private String formatTooLongError(AudioTrack track)
    {
        return trackEnqueuer.formatTooLongError(track);
    }

    private TrackAddResult addTrackToFront(Guild guild, Member member, AudioTrack track,
                                           String queryArgs, TextChannel channel)
    {
        return trackEnqueuer.addTrackToFront(guild, member, track, queryArgs, channel);
    }

    // ========== Playlist operations ==========

    public void addCurrentTrackToFavorites(Guild guild, Member member, OutputAdapter output)
    {
        favoritesService.addCurrentTrackToFavorites(guild, member, output);
    }

    public void addCurrentTrackToPlaylist(Guild guild, Member member, String playlistName,
                                          OutputAdapter output)
    {
        favoritesService.addCurrentTrackToPlaylist(guild, member, playlistName, output);
    }

    public PlaylistMutationResult renamePlaylist(String oldName, String newName)
    {
        return playlistMutationService.renamePlaylist(oldName, newName);
    }

    public PlaylistNamesInfo getAvailablePlaylistNames()
    {
        PlaylistLoader.PlaylistResult<List<String>> result = bot.getPlaylistLoader().getPlaylistNamesResult();
        if (!result.isSuccess())
        {
            return PlaylistNamesInfo.error(PlaylistErrorMessages.toMessage(result.getError()));
        }

        List<String> names = result.getValue();
        Map<String, Integer> itemCounts = new LinkedHashMap<>();
        for (String name : names)
        {
            PlaylistLoader.PlaylistResult<Playlist> playlistResult =
                    bot.getPlaylistLoader().getPlaylistResult(name);
            if (playlistResult.isSuccess())
            {
                itemCounts.put(name, playlistResult.getValue().getItems().size());
            }
        }
        return PlaylistNamesInfo.success(names, itemCounts);
    }

    public void queuePlaylist(Guild guild, Member member, String playlistName, TextChannel channel, OutputAdapter output)
    {
        PlaylistLoader.PlaylistResult<Playlist> playlistResult = bot.getPlaylistLoader().getPlaylistResult(playlistName);
        if (!playlistResult.isSuccess())
        {
            output.replyError(PlaylistErrorMessages.toMessage(playlistResult.getError()));
            return;
        }
        Playlist playlist = playlistResult.getValue();

        AudioHandler handler = getHandler(guild);
        if (handler == null)
        {
            handler = bot.getPlayerManager().setUpHandler(guild);
        }
        if (handler == null)
        {
            output.replyError("There is no player in this server!");
            return;
        }

        AudioHandler finalHandler = handler;
        playlist.loadTracks(bot.getPlayerManager(), at -> {
            RequestMetadata metadata = new RequestMetadata(member.getUser(),
                    new RequestMetadata.RequestInfo("playlist " + playlist.getName(), at.getInfo().uri),
                    channel.getIdLong());
            finalHandler.addTrack(new QueuedTrack(at, metadata));
        }, () -> {
            int loadedCount = playlist.getTracks().size();
            int errorCount = playlist.getErrors().size();
            if (loadedCount == 0)
            {
                output.replyWarning("No tracks were loaded from playlist `" + playlist.getName() + "`!");
                return;
            }

            StringBuilder msg = new StringBuilder("Queued **")
                    .append(loadedCount)
                    .append("** track(s) from playlist `")
                    .append(playlist.getName())
                    .append("`.");
            if (errorCount > 0)
            {
                msg.append(" Failed to load ").append(errorCount).append(" item(s).");
            }
            output.replySuccess(msg.toString());
            if (finalHandler.getPlayer().getPlayingTrack() != null)
            {
                bot.getNowplayingHandler().requestReconcile(guild.getIdLong(), "playlist-loaded");
            }
        });
    }

    public void playPlaylistNow(Guild guild, Member member, String playlistName, TextChannel channel, OutputAdapter output)
    {
        PlaylistLoader.PlaylistResult<Playlist> playlistResult = bot.getPlaylistLoader().getPlaylistResult(playlistName);
        if (!playlistResult.isSuccess())
        {
            output.replyError(PlaylistErrorMessages.toMessage(playlistResult.getError()));
            return;
        }

        AudioHandler handler = getHandler(guild);
        if (handler == null)
        {
            handler = bot.getPlayerManager().setUpHandler(guild);
        }
        if (handler == null)
        {
            output.replyError("There is no player in this server!");
            return;
        }

        handler.stopAndClearQueuePreserveHistory();
        queuePlaylist(guild, member, playlistName, channel, new OutputAdapter()
        {
            @Override
            public void replySuccess(String content)
            {
                output.replySuccess("Now playing playlist `" + playlistName + "` (queue replaced). " + content);
            }

            @Override
            public void replyError(String content)
            {
                output.replyError(content);
            }

            @Override
            public void replyWarning(String content)
            {
                output.replyWarning(content);
            }

            @Override
            public void editMessage(String content)
            {
            }

            @Override
            public void editMessage(String content, Consumer<Message> onSuccess)
            {
            }

            @Override
            public void editNowPlaying(AudioHandler handler)
            {
            }

            @Override
            public void editNoMusic(AudioHandler handler)
            {
            }

            @Override
            public void onShowHelp()
            {
            }
        });
    }

    public PlaylistDetailsInfo getPlaylistDetails(String playlistName)
    {
        PlaylistLoader.PlaylistResult<Playlist> playlistResult = bot.getPlaylistLoader().getPlaylistResult(playlistName);
        if (!playlistResult.isSuccess())
        {
            return null;
        }
        Playlist playlist = playlistResult.getValue();

        List<String> items = playlist.getItems();
        int previewLimit = Math.min(5, items.size());
        List<String> preview = new ArrayList<>(items.subList(0, previewLimit));
        boolean hasMore = items.size() > previewLimit;
        return new PlaylistDetailsInfo(playlist.getName(), items.size(), preview, hasMore);
    }

    public boolean canEditPlaylistEntries(Guild guild, Member member)
    {
        return bot.getConfig().getOwnerId() == member.getIdLong()
                || DJCommand.checkDJPermission(bot, guild, member);
    }

    public PlaylistMutationResult appendPlaylistItems(String playlistName, List<String> items)
    {
        return playlistMutationService.appendItems(playlistName, items);
    }

    public PlaylistMutationResult removePlaylistItem(String playlistName, int position)
    {
        return playlistMutationService.removeItem(playlistName, position);
    }

    public PlaylistMutationResult movePlaylistItem(String playlistName, int fromPosition, int toPosition)
    {
        return playlistMutationService.moveItem(playlistName, fromPosition, toPosition);
    }

    public PlaylistMutationResult replacePlaylistItem(String playlistName, int position, String item)
    {
        return playlistMutationService.replaceItem(playlistName, position, item);
    }

    public PlaylistDraftContext buildPlaylistDraftContext(long guildId, long channelId, long messageId,
                                                          long userId, String playlistName)
    {
        return playlistDraftService.buildPlaylistDraftContext(guildId, channelId, messageId, userId, playlistName);
    }

    public PlaylistDraftMutationResult removePlaylistDraftItem(PlaylistDraftContext context, int position)
    {
        return playlistDraftService.removePlaylistDraftItem(context, position);
    }

    public PlaylistDraftMutationResult movePlaylistDraftItem(PlaylistDraftContext context, int fromPosition, int toPosition)
    {
        return playlistDraftService.movePlaylistDraftItem(context, fromPosition, toPosition);
    }

    public PlaylistDraftMutationResult savePlaylistDraft(PlaylistDraftContext context)
    {
        return playlistDraftService.savePlaylistDraft(context);
    }

    public void discardPlaylistDraft(PlaylistDraftContext context)
    {
        playlistDraftService.discardPlaylistDraft(context);
    }

    public boolean isPlaylistDraftDirty(PlaylistDraftContext context)
    {
        return playlistDraftService.isPlaylistDraftDirty(context);
    }

    public int getPlaylistTrackCount(PlaylistDraftContext context)
    {
        return playlistDraftService.getPlaylistTrackCount(context);
    }

    public void loadPlaylistPreviewWithTracks(String playlistName, int maxPreview,
                                             Consumer<PlaylistPreviewWithTracks> callback)
    {
        playlistPreviewService.loadPlaylistPreviewWithTracks(playlistName, maxPreview, callback);
    }

    public void loadPlaylistPageWithTracks(String playlistName, int page, int pageSize,
                                           Consumer<PlaylistTracksPageInfo> callback)
    {
        playlistPreviewService.loadPlaylistPageWithTracks(playlistName, page, pageSize, callback);
    }

    public void loadPlaylistPageWithTracks(PlaylistDraftContext context, int page, int pageSize,
                                           Consumer<PlaylistTracksPageInfo> callback)
    {
        List<String> draftItems = playlistDraftService.getDraftItems(context);
        if (draftItems != null)
        {
            playlistPreviewService.loadPlaylistPageWithTracks(
                    context.playlistName(), draftItems, page, pageSize,
                    playlistDraftService.isPlaylistDraftDirty(context), callback);
            return;
        }
        playlistPreviewService.loadPlaylistPageWithTracks(context.playlistName(), page, pageSize, callback);
    }

    public String getPlaylistTrackUrlAtPosition(String playlistName, int position)
    {
        return playlistPreviewService.getPlaylistTrackUrlAtPosition(playlistName, position);
    }

    public String getPlaylistTrackUrlAtPosition(PlaylistDraftContext context, int position)
    {
        List<String> draftItems = playlistDraftService.getDraftItems(context);
        if (draftItems != null)
        {
            return playlistPreviewService.getPlaylistTrackUrlAtPosition(draftItems, position);
        }
        return playlistPreviewService.getPlaylistTrackUrlAtPosition(context.playlistName(), position);
    }

    public void playNowFromUrl(Guild guild, Member member, String url, TextChannel channel, OutputAdapter output)
    {
        if (url == null || url.isBlank())
        {
            output.replyError("That playlist entry could not be loaded.");
            return;
        }
        bot.getPlayerManager().loadItemOrdered(guild, url, bot.getAudioLoadWrapper().wrap(url, new AudioLoadResultHandler()
        {
            private void loadSingle(AudioTrack track)
            {
                TrackAddResult result = addTrackToFront(guild, member, track, url, channel);
                if (result == null)
                {
                    output.replyError(formatTooLongError(track));
                    return;
                }
                AudioHandler handler = getHandler(guild);
                if (handler != null && handler.getPlayer().getPlayingTrack() != null)
                {
                    handler.getPlayer().stopTrack();
                }
                output.replySuccess("Now playing **" + FormatUtil.filter(FormatUtil.getTrackTitle(track)) + "**");
            }

            @Override
            public void trackLoaded(AudioTrack track)
            {
                loadSingle(track);
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
                    output.replyError("That playlist entry could not be loaded.");
                    return;
                }
                loadSingle(single);
            }

            @Override
            public void noMatches()
            {
                output.replyError("No matches found for that playlist entry.");
            }

            @Override
            public void loadFailed(FriendlyException exception)
            {
                output.replyError("Error loading playlist entry: " + exception.getMessage());
            }
        }));
    }

    public void saveHistoryAsPlaylist(Guild guild, Member member, String playlistName, OutputAdapter output)
    {
        historyPlaylistService.saveHistoryAsPlaylist(guild, member, playlistName, output);
    }

}
