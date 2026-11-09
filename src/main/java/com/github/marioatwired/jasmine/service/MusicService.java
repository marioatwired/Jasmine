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
import com.github.marioatwired.jasmine.service.dto.ForceSkipResult;
import com.github.marioatwired.jasmine.service.dto.HistoryInfo;
import com.github.marioatwired.jasmine.service.dto.NowPlayingInfo;
import com.github.marioatwired.jasmine.service.dto.PlaylistDetailsInfo;
import com.github.marioatwired.jasmine.service.dto.PlaylistDraftContext;
import com.github.marioatwired.jasmine.service.dto.PlaylistDraftMutationResult;
import com.github.marioatwired.jasmine.service.dto.PlaylistNamesInfo;
import com.github.marioatwired.jasmine.service.dto.PlaylistPreviewWithTracks;
import com.github.marioatwired.jasmine.service.dto.PlaylistTracksPageInfo;
import com.github.marioatwired.jasmine.service.dto.PlaylistTransferResult;
import com.github.marioatwired.jasmine.service.dto.QueueInfo;
import com.github.marioatwired.jasmine.service.dto.TrackAddResult;
import com.github.marioatwired.jasmine.service.dto.VolumeResult;
import com.github.marioatwired.jasmine.settings.RepeatMode;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

/**
 * Facade for music operations. Playback control lives in {@link PlayerService};
 * queue mutation lives in {@link QueueService}; playlists, history, and
 * now-playing formatting have their own services.
 */
public class MusicService
{
    private static final Logger LOG = LoggerFactory.getLogger(MusicService.class);

    private final PlayerService playerService;
    private final QueueService queueService;
    private final NowPlayingService nowPlayingService;
    private final HistoryService historyService;
    private final PlaylistService playlistService;
    private final PlaylistTransferService playlistTransferService;

    public MusicService(Bot bot)
    {
        this(bot, new PlayerService(bot));
    }

    public MusicService(Bot bot, PlayerService playerService)
    {
        this.playerService = playerService;
        this.queueService = new QueueService(bot);
        this.nowPlayingService = new NowPlayingService(bot);
        this.historyService = new HistoryService(bot, playerService);
        this.playlistService = new PlaylistService(bot, playerService);
        this.playlistTransferService = new PlaylistTransferService(bot);
        LOG.info("MusicService initialized");
    }

    // ========== Shared Track Utilities ==========

    /**
     * Checks if a track exceeds the maximum allowed duration.
     *
     * @param track The track to check
     * @return true if the track is too long
     */
    public boolean isTooLong(AudioTrack track)
    {
        return playerService.isTooLong(track);
    }

    /**
     * Formats an error message for a track that is too long.
     *
     * @param track The track that is too long
     * @return Formatted error message
     */
    public String formatTooLongError(AudioTrack track)
    {
        return playerService.formatTooLongError(track);
    }

    /**
     * Formats a success message for a track that was added to the queue.
     *
     * @param title    The track title
     * @param duration The track duration in milliseconds
     * @param position The queue position (0 = now playing, >0 = queue position)
     * @return Formatted success message
     */
    public String formatTrackAddedMessage(String title, long duration, int position)
    {
        return playerService.formatTrackAddedMessage(title, duration, position);
    }

    /**
     * Adds a track to the queue and returns the result.
     *
     * @param guild     The guild
     * @param member    The member adding the track
     * @param track     The track to add
     * @param queryArgs The original query/args used to find this track
     * @param channel   The text channel for request metadata
     * @return TrackAddResult containing position and formatted message, or null if track is too long
     */
    public TrackAddResult addTrackToQueue(Guild guild, Member member, AudioTrack track,
                                          String queryArgs, TextChannel channel)
    {
        return playerService.addTrackToQueue(guild, member, track, queryArgs, channel);
    }

    /**
     * Adds a track to the front of the queue and returns the result.
     *
     * @param guild     The guild
     * @param member    The member adding the track
     * @param track     The track to add
     * @param queryArgs The original query/args used to find this track
     * @param channel   The text channel for request metadata
     * @return TrackAddResult containing position and formatted message, or null if track is too long
     */
    public TrackAddResult addTrackToFront(Guild guild, Member member, AudioTrack track,
                                          String queryArgs, TextChannel channel)
    {
        return playerService.addTrackToFront(guild, member, track, queryArgs, channel);
    }

    // ========== Player Operations ==========

    public void playNext(Guild guild, Member member, String args, TextChannel channel, OutputAdapter output)
    {
        playerService.playNext(guild, member, args, channel, output);
    }

    public void play(Guild guild, Member member, String args, TextChannel channel, OutputAdapter output)
    {
        playerService.play(guild, member, args, channel, output);
    }

    public void previous(Guild guild, Member member, OutputAdapter output)
    {
        playerService.previous(guild, member, output);
    }

    public void shuffle(Guild guild, Member member, int startIndex, OutputAdapter output)
    {
        queueService.shuffle(guild, member, startIndex, output);
    }

    /**
     * Shuffles only the tracks added by a specific user.
     *
     * @param guild  The guild
     * @param userId The user ID whose tracks to shuffle
     * @return The number of tracks shuffled
     */
    public int shuffleUserTracks(Guild guild, long userId)
    {
        return queueService.shuffleUserTracks(guild, userId);
    }

    public void cycleRepeatMode(Guild guild, Member member, OutputAdapter output)
    {
        playerService.cycleRepeatMode(guild, member, output);
    }

    /**
     * Gets the current repeat mode for a guild.
     *
     * @param guild The guild
     * @return The current RepeatMode
     */
    public RepeatMode getRepeatMode(Guild guild)
    {
        return playerService.getRepeatMode(guild);
    }

    /**
     * Sets the repeat mode for a guild.
     *
     * @param guild The guild
     * @param mode  The repeat mode to set
     */
    public void setRepeatMode(Guild guild, RepeatMode mode)
    {
        playerService.setRepeatMode(guild, mode);
    }

    public void adjustVolume(Guild guild, Member member, int change, OutputAdapter output)
    {
        playerService.adjustVolume(guild, member, change, output);
    }

    /**
     * Gets the current volume for a guild.
     *
     * @param guild The guild
     * @return The current volume (0-150)
     */
    public int getVolume(Guild guild)
    {
        return playerService.getVolume(guild);
    }

    /**
     * Sets the volume to an absolute value.
     *
     * @param guild  The guild
     * @param volume The new volume (0-150)
     * @return VolumeResult containing the old and new volume, or null if invalid
     */
    public VolumeResult setVolume(Guild guild, int volume)
    {
        return playerService.setVolume(guild, volume);
    }

    public void stop(Guild guild, Member member, OutputAdapter output)
    {
        playerService.stop(guild, member, output);
    }

    /**
     * Stops playback and clears the queue (simple version without permission check).
     * Use this when DJ permission is already verified by the caller.
     *
     * @param guild The guild
     */
    public void stopAndClear(Guild guild)
    {
        playerService.stopAndClear(guild);
    }

    public void pause(Guild guild, Member member, OutputAdapter output)
    {
        playerService.pause(guild, member, output);
    }

    /**
     * Checks if the player is currently paused.
     *
     * @param guild The guild
     * @return true if paused, false otherwise
     */
    public boolean isPaused(Guild guild)
    {
        return playerService.isPaused(guild);
    }

    /**
     * Sets the paused state of the player.
     *
     * @param guild  The guild
     * @param paused true to pause, false to resume
     * @return The title of the currently playing track, or null if nothing is playing
     */
    public String setPaused(Guild guild, boolean paused)
    {
        return playerService.setPaused(guild, paused);
    }

    public void skip(Guild guild, Member member, OutputAdapter output)
    {
        playerService.skip(guild, member, output);
    }

    /**
     * Force skips the currently playing track (no permission check).
     * Use this when DJ permission is already verified by the caller.
     *
     * @param guild The guild
     * @return ForceSkipResult containing track info and requester, or null if nothing playing
     */
    public ForceSkipResult forceSkip(Guild guild)
    {
        return playerService.forceSkip(guild);
    }

    public void skipWithVote(Guild guild, Member member, int listeners, OutputAdapter output)
    {
        playerService.skipWithVote(guild, member, listeners, output);
    }

    public void addCurrentTrackToFavorites(Guild guild, Member member, OutputAdapter output)
    {
        playlistService.addCurrentTrackToFavorites(guild, member, output);
    }

    public void seek(Guild guild, Member member, String timeString, OutputAdapter output)
    {
        playerService.seek(guild, member, timeString, output);
    }

    // ========== Queue Operations ==========

    public void removeTrack(Guild guild, Member member, int position, OutputAdapter output)
    {
        queueService.removeTrack(guild, member, position, output);
    }

    public void removeAllTracks(Guild guild, Member member, OutputAdapter output)
    {
        queueService.removeAllTracks(guild, member, output);
    }

    /**
     * Removes all tracks from a specific user (for DJ force remove).
     *
     * @param guild  The guild
     * @param userId The user ID whose tracks to remove
     * @return The number of tracks removed
     */
    public int removeAllTracksByUser(Guild guild, long userId)
    {
        return queueService.removeAllTracksByUser(guild, userId);
    }

    /**
     * Checks if the queue is empty.
     *
     * @param guild The guild
     * @return true if the queue is empty
     */
    public boolean isQueueEmpty(Guild guild)
    {
        return queueService.isQueueEmpty(guild);
    }

    public void moveTrack(Guild guild, Member member, int from, int to, OutputAdapter output)
    {
        queueService.moveTrack(guild, member, from, to, output);
    }

    /**
     * Moves a track to position 1, making it play next.
     * Convenience method that wraps moveTrack.
     *
     * @param guild    The guild
     * @param member   The member executing the command
     * @param position The 1-based position of the track to move
     * @param output   The output adapter for responses
     */
    public void playNext(Guild guild, Member member, int position, OutputAdapter output)
    {
        queueService.playNext(guild, member, position, output);
    }

    /**
     * Plays a track from the queue immediately while preserving the rest of the queue.
     * Moves the track to position 1 and skips the currently playing track.
     *
     * @param guild    The guild
     * @param member   The member executing the command
     * @param position The 1-based position of the track to play now
     * @param output   The output adapter for responses
     */
    public void playNow(Guild guild, Member member, int position, OutputAdapter output)
    {
        playerService.playNow(guild, member, position, output);
    }

    /**
     * Moves a track from one position to another (no permission check).
     * Use this when DJ permission is already verified by the caller.
     *
     * @param guild The guild
     * @param from  The 1-based source position
     * @param to    The 1-based destination position
     * @return The title of the moved track, or null if invalid positions
     */
    public String moveTrackPosition(Guild guild, int from, int to)
    {
        return queueService.moveTrackPosition(guild, from, to);
    }

    /**
     * Checks if a position is valid in the queue.
     *
     * @param guild    The guild
     * @param position The 1-based position to check
     * @return true if the position is valid
     */
    public boolean isValidQueuePosition(Guild guild, int position)
    {
        return queueService.isValidQueuePosition(guild, position);
    }

    public void skipTo(Guild guild, Member member, int position, OutputAdapter output)
    {
        playerService.skipTo(guild, member, position, output);
    }

    /**
     * Skips to a specific position in the queue (no permission check).
     * Use this when DJ permission is already verified by the caller.
     *
     * @param guild    The guild
     * @param position The 1-based position to skip to
     * @return The title of the track skipped to, or null if invalid position
     */
    public String skipToPosition(Guild guild, int position)
    {
        return playerService.skipToPosition(guild, position);
    }

    /**
     * Gets the current queue size.
     *
     * @param guild The guild
     * @return The number of tracks in the queue
     */
    public int getQueueSize(Guild guild)
    {
        return queueService.getQueueSize(guild);
    }

    // ========== Now Playing ==========

    /**
     * Gets the now playing message for a guild.
     *
     * @param guild The guild
     * @param jda   The JDA instance
     * @return NowPlayingInfo containing the message data, or null if no handler
     */
    public NowPlayingInfo getNowPlayingInfo(Guild guild, JDA jda)
    {
        return nowPlayingService.getNowPlayingInfo(guild, jda);
    }

    // ========== Queue Info ==========

    public QueueInfo getQueueInfo(Guild guild, JDA jda)
    {
        return nowPlayingService.getQueueInfo(guild, jda);
    }

    public String formatQueueTitle(QueueInfo info, String successEmoji)
    {
        return nowPlayingService.formatQueueTitle(info, successEmoji);
    }

    // ========== History Info ==========

    /**
     * Gets playback history information for display (e.g. history command embed).
     * History index 0 is most recent. Does not require music to be playing.
     *
     * @param guild The guild
     * @param jda   The JDA instance (for consistency with getQueueInfo)
     * @return HistoryInfo, or null if no handler exists
     */
    public HistoryInfo getHistoryInfo(Guild guild, JDA jda)
    {
        return historyService.getHistoryInfo(guild, jda);
    }

    /**
     * Adds the track at the given 1-based history position to the queue, and removes it from history (de-dup).
     *
     * @param guild           The guild
     * @param member          The member adding the track
     * @param historyPosition 1-based position (1 = most recent)
     * @param channel         The text channel for request metadata
     * @param output          The output adapter
     */
    public void queueFromHistory(Guild guild, Member member, int historyPosition, TextChannel channel, OutputAdapter output)
    {
        historyService.queueFromHistory(guild, member, historyPosition, channel, output);
    }

    /**
     * Adds all tracks from playback history to the queue, removing them from history (de-dup).
     * Tracks that exceed the max duration are skipped. Order in queue is most recent first (playback will be chronological).
     *
     * @param guild   The guild
     * @param member  The member adding the tracks
     * @param channel The text channel for request metadata
     * @param output  The output adapter
     */
    public void queueAllFromHistory(Guild guild, Member member, TextChannel channel, OutputAdapter output)
    {
        historyService.queueAllFromHistory(guild, member, channel, output);
    }

    /**
     * Plays the track at the given 1-based history position immediately, and removes it from history (de-dup).
     *
     * @param guild           The guild
     * @param member          The member
     * @param historyPosition 1-based position (1 = most recent)
     * @param channel         The text channel for request metadata
     * @param output          The output adapter
     */
    public void playFromHistoryNow(Guild guild, Member member, int historyPosition, TextChannel channel, OutputAdapter output)
    {
        historyService.playFromHistoryNow(guild, member, historyPosition, channel, output);
    }

    public PlaylistNamesInfo getAvailablePlaylistNames()
    {
        return playlistService.getAvailablePlaylistNames();
    }

    public PlaylistTransferResult importPlaylist(String name, byte[] content)
    {
        return playlistTransferService.importPlaylist(name, content);
    }

    public PlaylistTransferResult exportPlaylist(String name)
    {
        return playlistTransferService.exportPlaylist(name);
    }

    /**
     * Queues all tracks from a named saved playlist.
     *
     * @param guild        The guild
     * @param member       The member requesting playback
     * @param playlistName The saved playlist name
     * @param channel      The text channel for request metadata
     * @param output       The output adapter
     */
    public void queuePlaylist(Guild guild, Member member, String playlistName, TextChannel channel, OutputAdapter output)
    {
        playlistService.queuePlaylist(guild, member, playlistName, channel, output);
    }

    /**
     * Clears current playback queue and starts playing the selected saved playlist immediately.
     *
     * @param guild        The guild
     * @param member       The member requesting playback
     * @param playlistName The saved playlist name
     * @param channel      The text channel for request metadata
     * @param output       The output adapter
     */
    public void playPlaylistNow(Guild guild, Member member, String playlistName, TextChannel channel, OutputAdapter output)
    {
        playlistService.playPlaylistNow(guild, member, playlistName, channel, output);
    }

    /**
     * Gets lightweight metadata for a saved playlist.
     *
     * @param playlistName The saved playlist name
     * @return playlist details, or null if playlist does not exist
     */
    public PlaylistDetailsInfo getPlaylistDetails(String playlistName)
    {
        return playlistService.getPlaylistDetails(playlistName);
    }

    /**
     * Returns true when the member is allowed to edit playlists in interactive details view.
     * Policy: bot owner OR DJ permission.
     */
    public boolean canEditPlaylistEntries(Guild guild, Member member)
    {
        return playlistService.canEditPlaylistEntries(guild, member);
    }

    public PlaylistDraftContext buildPlaylistDraftContext(long guildId, long channelId, long messageId,
                                                          long userId, String playlistName)
    {
        return playlistService.buildPlaylistDraftContext(guildId, channelId, messageId, userId, playlistName);
    }

    public PlaylistDraftMutationResult removePlaylistDraftItem(PlaylistDraftContext context, int position)
    {
        return playlistService.removePlaylistDraftItem(context, position);
    }

    public PlaylistDraftMutationResult movePlaylistDraftItem(PlaylistDraftContext context, int fromPosition, int toPosition)
    {
        return playlistService.movePlaylistDraftItem(context, fromPosition, toPosition);
    }

    public PlaylistDraftMutationResult savePlaylistDraft(PlaylistDraftContext context)
    {
        return playlistService.savePlaylistDraft(context);
    }

    public void discardPlaylistDraft(PlaylistDraftContext context)
    {
        playlistService.discardPlaylistDraft(context);
    }

    public boolean isPlaylistDraftDirty(PlaylistDraftContext context)
    {
        return playlistService.isPlaylistDraftDirty(context);
    }

    public int getPlaylistTrackCount(PlaylistDraftContext context)
    {
        return playlistService.getPlaylistTrackCount(context);
    }

    /**
     * Asynchronously loads the first N playlist URLs and invokes the callback with formatted track lines
     * (same style as Queue/History: duration + linked title). Uses a unique load identifier per index so
     * loads run in parallel. If the playlist does not exist, the callback is invoked with null.
     *
     * @param playlistName name of the saved playlist
     * @param maxPreview   maximum number of preview entries to load (e.g. 5)
     * @param callback     invoked with the result, or null if playlist not found
     */
    public void loadPlaylistPreviewWithTracks(String playlistName, int maxPreview,
                                             Consumer<PlaylistPreviewWithTracks> callback)
    {
        playlistService.loadPlaylistPreviewWithTracks(playlistName, maxPreview, callback);
    }

    /**
     * Asynchronously loads one page of playlist URLs and resolves each to a formatted track line
     * (same style as Queue/History: duration + linked title).
     *
     * @param playlistName playlist name
     * @param page         1-based page number
     * @param pageSize     page size (e.g. 10)
     * @param callback     invoked with page result, or null when playlist does not exist
     */
    public void loadPlaylistPageWithTracks(String playlistName, int page, int pageSize,
                                           Consumer<PlaylistTracksPageInfo> callback)
    {
        playlistService.loadPlaylistPageWithTracks(playlistName, page, pageSize, callback);
    }

    /**
     * Same as {@link #loadPlaylistPageWithTracks(String, int, int, Consumer)} but reads from the current
     * interaction draft when one exists for the given context.
     */
    public void loadPlaylistPageWithTracks(PlaylistDraftContext context, int page, int pageSize,
                                           Consumer<PlaylistTracksPageInfo> callback)
    {
        playlistService.loadPlaylistPageWithTracks(context, page, pageSize, callback);
    }

    /**
     * Returns the playlist item URL at a 1-based position, or null when missing.
     */
    public String getPlaylistTrackUrlAtPosition(String playlistName, int position)
    {
        return playlistService.getPlaylistTrackUrlAtPosition(playlistName, position);
    }

    /**
     * Returns the draft-aware playlist item URL at a 1-based position, or null when missing.
     */
    public String getPlaylistTrackUrlAtPosition(PlaylistDraftContext context, int position)
    {
        return playlistService.getPlaylistTrackUrlAtPosition(context, position);
    }

    /**
     * Loads a URL and adds it to the front of queue, then starts it immediately.
     */
    public void playNowFromUrl(Guild guild, Member member, String url, TextChannel channel, OutputAdapter output)
    {
        playlistService.playNowFromUrl(guild, member, url, channel, output);
    }

    /**
     * Saves the current playback history as a playlist file. Rejects if a playlist with the name already exists.
     * Requires DJ permission or bot owner.
     *
     * @param guild        The guild
     * @param member       The member
     * @param playlistName The name for the new playlist (sanitized)
     * @param output       The output adapter
     */
    public void saveHistoryAsPlaylist(Guild guild, Member member, String playlistName, OutputAdapter output)
    {
        playlistService.saveHistoryAsPlaylist(guild, member, playlistName, output);
    }

}
