/*
 * Copyright 2026 Mario
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
package com.github.marioatwired.jasmine.queue;

import com.github.marioatwired.jasmine.Bot;
import com.github.marioatwired.jasmine.audio.AudioHandler;
import com.github.marioatwired.jasmine.audio.QueuedTrack;
import com.github.marioatwired.jasmine.audio.RequestMetadata;
import com.github.marioatwired.jasmine.utils.OtherUtil;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Saves and restores per-guild playback queues across bot restarts.
 * <p>
 * Tracks are stored as URIs and reloaded through Lavaplayer. Restoration is
 * intentionally passive: no voice connection is opened and no audio starts
 * until a user invokes a playback command.
 */
public final class QueuePersistenceService
{
    private static final Logger LOG = LoggerFactory.getLogger(QueuePersistenceService.class);

    static final String FILE_NAME = "queuedata.json";
    private static final long SAVE_INTERVAL_SECONDS = 15L;
    private static final long RESTORE_TIMEOUT_SECONDS = 60L;

    private final Bot bot;
    private final QueueStateStore store;
    private final Object lock = new Object();
    private final Map<Long, QueueState.GuildQueue> queues = new TreeMap<>();
    private final Set<Long> restoringGuilds = new HashSet<>();
    private final Set<Long> restoredGuilds = new HashSet<>();

    private byte[] lastWritten;
    private boolean loaded;
    private boolean started;
    private ScheduledFuture<?> saveTask;

    public QueuePersistenceService(Bot bot)
    {
        this(bot, OtherUtil.getPath(FILE_NAME));
    }

    public QueuePersistenceService(Bot bot, Path stateFile)
    {
        this.bot = Objects.requireNonNull(bot, "bot");
        this.store = new QueueStateStore(Objects.requireNonNull(stateFile, "stateFile"));
    }

    /**
     * Starts the periodic save task. Safe to call more than once.
     */
    public void start()
    {
        synchronized (lock)
        {
            if (started)
                return;
            started = true;
            ensureLoadedLocked();
        }

        try
        {
            saveTask = bot.getThreadpool().scheduleWithFixedDelay(
                    this::saveSafely,
                    SAVE_INTERVAL_SECONDS,
                    SAVE_INTERVAL_SECONDS,
                    TimeUnit.SECONDS);
        }
        catch (RuntimeException ex)
        {
            LOG.warn("Could not start queue persistence task: {}", ex.getMessage());
        }
    }

    /**
     * Stops the periodic save task. The final save is performed by {@link Bot#shutdown()}.
     */
    public void stop()
    {
        ScheduledFuture<?> task;
        synchronized (lock)
        {
            task = saveTask;
            saveTask = null;
            started = false;
        }
        if (task != null)
            task.cancel(false);
    }

    /**
     * Saves the current state of every active guild queue.
     */
    public void saveAll()
    {
        JDA jda = bot.getJDA();
        if (jda == null)
            return;

        synchronized (lock)
        {
            ensureLoadedLocked();

            Map<Long, QueueState.GuildQueue> updated = new TreeMap<>(queues);
            try
            {
                for (Guild guild : jda.getGuilds())
                {
                    long guildId = guild.getIdLong();
                    if (restoringGuilds.contains(guildId))
                        continue;

                    AudioHandler handler = getHandler(guild);
                    if (handler == null)
                        continue;

                    QueueState.GuildQueue snapshot = snapshot(handler);
                    if (snapshot.isEmpty())
                        updated.remove(guildId);
                    else
                        updated.put(guildId, snapshot);
                }
            }
            catch (RuntimeException ex)
            {
                LOG.warn("Could not snapshot queue state: {}", ex.getMessage());
                return;
            }

            byte[] bytes;
            try
            {
                bytes = store.serialize(new QueueState(updated));
            }
            catch (IOException ex)
            {
                LOG.warn("Could not serialize queue state: {}", ex.getMessage());
                return;
            }

            if (Arrays.equals(bytes, lastWritten))
            {
                replaceQueues(updated);
                return;
            }

            // Do not create an empty file on a fresh installation.
            if (updated.isEmpty() && lastWritten == null)
                return;

            try
            {
                store.write(bytes);
                replaceQueues(updated);
                lastWritten = bytes;
            }
            catch (IOException ex)
            {
                LOG.warn("Could not save queue state to {}: {}", store.getFile(), ex.getMessage());
            }
        }
    }

    /**
     * Restores every saved guild queue that still has a matching guild.
     */
    public void restoreAll()
    {
        JDA jda = bot.getJDA();
        if (jda == null)
            return;

        List<Long> guildIds;
        synchronized (lock)
        {
            ensureLoadedLocked();
            guildIds = new ArrayList<>(queues.keySet());
        }

        for (long guildId : guildIds)
        {
            Guild guild = jda.getGuildById(guildId);
            if (guild == null)
                continue;

            try
            {
                bot.getPlayerManager().setUpHandler(guild);
            }
            catch (RuntimeException ex)
            {
                LOG.warn("Could not restore queue for guild {}: {}", guildId, ex.getMessage());
            }
        }
    }

    /**
     * Restores the saved queue for a guild into a newly created handler.
     * The handler is left idle; no tracks are played.
     */
    public void restore(Guild guild, AudioHandler handler)
    {
        if (guild == null || handler == null)
            return;

        long guildId = guild.getIdLong();
        QueueState.GuildQueue saved;
        synchronized (lock)
        {
            ensureLoadedLocked();

            if (restoredGuilds.contains(guildId) || restoringGuilds.contains(guildId))
                return;

            if (hasActivePlayback(handler))
            {
                restoredGuilds.add(guildId);
                return;
            }

            saved = queues.get(guildId);
            if (saved == null || saved.isEmpty())
            {
                restoredGuilds.add(guildId);
                return;
            }

            restoringGuilds.add(guildId);
        }

        List<QueueState.StoredTrack> tracks = saved.orderedTracks().stream()
                .filter(QueueState.StoredTrack::hasUri)
                .toList();
        if (tracks.isEmpty())
        {
            finishRestore(guildId, handler, List.of());
            return;
        }

        List<CompletableFuture<QueuedTrack>> futures = tracks.stream()
                .map(track -> loadTrack(guild, track))
                .toList();

        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
                .whenComplete((ignored, error) ->
                {
                    if (error != null)
                        LOG.debug("Queue restore completed with errors for guild {}: {}",
                                guildId, error.getMessage());
                    finishRestore(guildId, handler, futures);
                });
    }

    private CompletableFuture<QueuedTrack> loadTrack(Guild guild, QueueState.StoredTrack stored)
    {
        CompletableFuture<QueuedTrack> future = new CompletableFuture<>();
        try
        {
            bot.getPlayerManager().loadItemOrdered(guild, stored.uri, new AudioLoadResultHandler()
            {
                @Override
                public void trackLoaded(AudioTrack track)
                {
                    future.complete(toQueuedTrack(track, stored));
                }

                @Override
                public void playlistLoaded(AudioPlaylist playlist)
                {
                    if (playlist != null && !playlist.getTracks().isEmpty())
                        future.complete(toQueuedTrack(playlist.getTracks().get(0), stored));
                    else
                    {
                        LOG.debug("Skipping restored track with empty playlist result: {}", stored.uri);
                        future.complete(null);
                    }
                }

                @Override
                public void noMatches()
                {
                    LOG.debug("Skipping restored track with no matches: {}", stored.uri);
                    future.complete(null);
                }

                @Override
                public void loadFailed(FriendlyException exception)
                {
                    LOG.warn("Could not restore track {}: {}", stored.uri,
                            exception != null ? exception.getMessage() : "unknown error");
                    future.complete(null);
                }
            });
        }
        catch (RuntimeException ex)
        {
            LOG.warn("Could not restore track {}: {}", stored.uri, ex.getMessage());
            future.complete(null);
        }
        future.completeOnTimeout(null, RESTORE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        return future;
    }

    private void finishRestore(long guildId, AudioHandler handler,
                               List<CompletableFuture<QueuedTrack>> futures)
    {
        List<QueuedTrack> tracks = new ArrayList<>(futures.size());
        for (CompletableFuture<QueuedTrack> future : futures)
        {
            try
            {
                QueuedTrack track = future.getNow(null);
                if (track != null)
                    tracks.add(track);
            }
            catch (RuntimeException ignored)
            {
                // A failed future is treated like a track that could not be loaded.
            }
        }

        Runnable apply = () ->
        {
            try
            {
                if (!tracks.isEmpty())
                    handler.restoreTracks(tracks);
            }
            catch (RuntimeException ex)
            {
                LOG.warn("Could not apply restored queue for guild {}: {}", guildId, ex.getMessage());
            }
            finally
            {
                synchronized (lock)
                {
                    restoringGuilds.remove(guildId);
                    restoredGuilds.add(guildId);
                }
            }
        };

        try
        {
            bot.getThreadpool().execute(apply);
        }
        catch (RuntimeException ex)
        {
            apply.run();
        }
    }

    private static QueuedTrack toQueuedTrack(AudioTrack track, QueueState.StoredTrack stored)
    {
        RequestMetadata source = stored.metadata;
        RequestMetadata.RequestInfo sourceInfo = source != null ? source.requestInfo : null;
        String query = sourceInfo != null ? sourceInfo.query : null;
        String url = sourceInfo != null ? sourceInfo.url : null;
        long start = stored.position > 0
                ? stored.position
                : sourceInfo != null ? sourceInfo.startTimestamp : 0L;
        if (track.isSeekable() && track.getDuration() > 0)
            start = Math.min(start, Math.max(0L, track.getDuration() - 1L));

        RequestMetadata metadata = new RequestMetadata(
                source != null ? source.user : null,
                new RequestMetadata.RequestInfo(query, url, start),
                source != null ? source.channelId : 0L);
        return new QueuedTrack(track, metadata);
    }

    private static QueueState.GuildQueue snapshot(AudioHandler handler)
    {
        AudioPlayer player = handler.getPlayer();
        QueueState.StoredTrack current = player == null
                ? null
                : toStoredTrack(player.getPlayingTrack(), null);

        List<QueueState.StoredTrack> tracks = new ArrayList<>();
        var handlerQueue = handler.getQueue();
        if (handlerQueue != null)
        {
            List<QueuedTrack> queuedTracks;
            try
            {
                queuedTracks = new ArrayList<>(handlerQueue.getList());
            }
            catch (RuntimeException ex)
            {
                LOG.warn("Could not snapshot a queue while it was changing: {}", ex.getMessage());
                queuedTracks = List.of();
            }

            for (QueuedTrack queuedTrack : queuedTracks)
            {
                if (queuedTrack == null)
                    continue;
                QueueState.StoredTrack stored = toStoredTrack(
                        queuedTrack.getTrack(), queuedTrack.getRequestMetadata());
                if (stored != null)
                    tracks.add(stored);
            }
        }

        return new QueueState.GuildQueue(current, tracks);
    }

    private static QueueState.StoredTrack toStoredTrack(AudioTrack track, RequestMetadata fallback)
    {
        if (track == null || track.getInfo() == null)
            return null;

        String uri = track.getInfo().uri;
        if (uri == null || uri.isBlank())
            return null;

        RequestMetadata metadata = track.getUserData(RequestMetadata.class);
        if (metadata == null)
            metadata = fallback;

        long position = 0L;
        try
        {
            position = Math.max(0L, track.getPosition());
        }
        catch (RuntimeException ignored)
        {
            // Some source managers throw for tracks that have not started yet.
        }

        if (metadata != null && metadata.requestInfo != null)
            position = Math.max(position, metadata.requestInfo.startTimestamp);

        return new QueueState.StoredTrack(uri, position, metadata);
    }

    private static boolean hasActivePlayback(AudioHandler handler)
    {
        AudioPlayer player = handler.getPlayer();
        if (player != null && player.getPlayingTrack() != null)
            return true;
        var queue = handler.getQueue();
        return queue != null && !queue.isEmpty();
    }

    private static AudioHandler getHandler(Guild guild)
    {
        if (guild.getAudioManager() == null)
            return null;
        var handler = guild.getAudioManager().getSendingHandler();
        return handler instanceof AudioHandler ? (AudioHandler) handler : null;
    }

    private void ensureLoadedLocked()
    {
        if (loaded)
            return;

        loaded = true;
        QueueState state = store.load();
        state.guilds.forEach((guildId, queue) ->
        {
            if (guildId != null && queue != null)
                queues.put(guildId, queue);
        });
        lastWritten = store.readRaw();

        if (!queues.isEmpty())
            LOG.info("Loaded queue state for {} guild(s) from {}", queues.size(), store.getFile());
    }

    private void replaceQueues(Map<Long, QueueState.GuildQueue> updated)
    {
        queues.clear();
        queues.putAll(updated);
    }

    private void saveSafely()
    {
        try
        {
            saveAll();
        }
        catch (RuntimeException ex)
        {
            LOG.warn("Unexpected queue persistence error: {}", ex.getMessage());
        }
    }
}
