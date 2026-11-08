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
package com.github.marioatwired.jasmine.unit.queue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.marioatwired.jasmine.Bot;
import com.github.marioatwired.jasmine.audio.AudioHandler;
import com.github.marioatwired.jasmine.audio.PlayerManager;
import com.github.marioatwired.jasmine.audio.QueuedTrack;
import com.github.marioatwired.jasmine.audio.RequestMetadata;
import com.github.marioatwired.jasmine.queue.AbstractQueue;
import com.github.marioatwired.jasmine.queue.QueuePersistenceService;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.managers.AudioManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class QueuePersistenceServiceTest
{
    private static final long GUILD_ID = 123456789L;
    private static final String CURRENT_URI = "https://example.com/current";
    private static final String FIRST_URI = "https://example.com/first";
    private static final String SECOND_URI = "https://example.com/second";

    @TempDir
    Path temporaryDirectory;

    @Test
    void saveAllWritesCurrentTrackQueueAndMetadata() throws Exception
    {
        Fixture fixture = new Fixture(temporaryDirectory.resolve("queuedata.json"));
        RequestMetadata metadata = metadata(42L, "query", 99L);
        QueuedTrack first = queuedTrack(FIRST_URI, metadata);
        QueuedTrack second = queuedTrack(SECOND_URI, metadata);
        AudioTrack current = track(CURRENT_URI, 180_000L, true);
        when(current.getPosition()).thenReturn(12_345L);
        when(fixture.player.getPlayingTrack()).thenReturn(current);
        when(fixture.queue.getList()).thenReturn(List.of(first, second));

        fixture.service.saveAll();

        JsonNode root = new ObjectMapper().readTree(Files.readString(fixture.stateFile));
        assertTrue(root.has("version"));
        JsonNode guild = root.path("guilds").path(String.valueOf(GUILD_ID));
        assertEquals(CURRENT_URI, guild.path("current").path("uri").asText());
        assertEquals(12_345L, guild.path("current").path("position").asLong());
        assertEquals(2, guild.path("queue").size());
        assertEquals(FIRST_URI, guild.path("queue").get(0).path("uri").asText());
        assertEquals(42L, guild.path("queue").get(0).path("metadata").path("user").path("id").asLong());
    }

    @Test
    void saveAllPreservesQueuesForGuildsWithoutAHandler() throws Exception
    {
        Fixture fixture = new Fixture(temporaryDirectory.resolve("queuedata.json"));
        QueuedTrack queued = queuedTrack(FIRST_URI, metadata(42L, "query", 99L));
        when(fixture.queue.getList()).thenReturn(List.of(queued));

        fixture.service.saveAll();

        when(fixture.jda.getGuilds()).thenReturn(List.of());
        fixture.service.saveAll();

        JsonNode root = new ObjectMapper().readTree(Files.readString(fixture.stateFile));
        assertEquals(1, root.path("guilds").size());
        assertTrue(root.path("guilds").has(String.valueOf(GUILD_ID)));
    }

    @Test
    void saveAllRemovesStateWhenQueueIsCleared() throws Exception
    {
        Fixture fixture = new Fixture(temporaryDirectory.resolve("queuedata.json"));
        QueuedTrack queued = queuedTrack(FIRST_URI, metadata(42L, "query", 99L));
        when(fixture.queue.getList()).thenReturn(List.of(queued));
        fixture.service.saveAll();

        when(fixture.queue.getList()).thenReturn(List.of());
        fixture.service.saveAll();

        JsonNode root = new ObjectMapper().readTree(Files.readString(fixture.stateFile));
        assertTrue(root.path("guilds").isEmpty());
    }

    @Test
    void startSchedulesPeriodicSaveAndStopCancelsIt()
    {
        Fixture fixture = new Fixture(temporaryDirectory.resolve("queuedata.json"));
        ScheduledFuture<?> saveTask = mock(ScheduledFuture.class);
        doReturn(saveTask).when(fixture.threadpool).scheduleWithFixedDelay(any(Runnable.class),
                anyLong(), anyLong(), any(TimeUnit.class));

        fixture.service.start();
        fixture.service.stop();

        verify(fixture.threadpool).scheduleWithFixedDelay(any(Runnable.class), eq(15L), eq(15L),
                eq(TimeUnit.SECONDS));
        verify(saveTask).cancel(false);
    }

    @Test
    void restoreAllCreatesHandlersForSavedGuilds()
    {
        Fixture fixture = new Fixture(temporaryDirectory.resolve("queuedata.json"));
        QueuedTrack queued = queuedTrack(FIRST_URI, metadata(42L, "query", 99L));
        when(fixture.queue.getList()).thenReturn(List.of(queued));
        fixture.service.saveAll();

        RestoreFixture restore = new RestoreFixture(fixture.stateFile);
        restore.configureSuccessfulLoads();
        when(restore.jda.getGuildById(GUILD_ID)).thenReturn(restore.guild);
        when(restore.playerManager.setUpHandler(restore.guild)).thenAnswer(invocation ->
        {
            restore.service.restore(restore.guild, restore.handler);
            return restore.handler;
        });

        restore.service.restoreAll();

        verify(restore.handler).restoreTracks(restore.captor.capture());
        assertEquals(1, restore.captor.getValue().size());
        assertEquals(FIRST_URI, restore.captor.getValue().get(0).getTrack().getInfo().uri);
    }

    @Test
    void restoreLoadsCurrentTrackFirstAndAppendsQueueInOrder()
    {
        Fixture fixture = new Fixture(temporaryDirectory.resolve("queuedata.json"));
        QueuedTrack first = queuedTrack(FIRST_URI, metadata(42L, "query", 99L));
        QueuedTrack second = queuedTrack(SECOND_URI, metadata(42L, "query", 99L));
        AudioTrack current = track(CURRENT_URI, 180_000L, true);
        when(current.getPosition()).thenReturn(12_345L);
        when(fixture.player.getPlayingTrack()).thenReturn(current);
        when(fixture.queue.getList()).thenReturn(List.of(first, second));
        fixture.service.saveAll();

        RestoreFixture restore = new RestoreFixture(fixture.stateFile);
        Map<String, AudioTrack> loadedTracks = restore.configureSuccessfulLoads();

        restore.service.restore(restore.guild, restore.handler);

        verify(restore.handler).restoreTracks(restore.captor.capture());
        List<QueuedTrack> restored = restore.captor.getValue();
        assertEquals(3, restored.size());
        assertEquals(CURRENT_URI, restored.get(0).getTrack().getInfo().uri);
        assertEquals(FIRST_URI, restored.get(1).getTrack().getInfo().uri);
        assertEquals(SECOND_URI, restored.get(2).getTrack().getInfo().uri);
        verify(loadedTracks.get(CURRENT_URI)).setPosition(12_345L);
    }

    @Test
    void restoreSkipsTracksThatCannotBeLoaded()
    {
        Fixture fixture = new Fixture(temporaryDirectory.resolve("queuedata.json"));
        QueuedTrack first = queuedTrack(FIRST_URI, metadata(42L, "query", 99L));
        QueuedTrack second = queuedTrack(SECOND_URI, metadata(42L, "query", 99L));
        when(fixture.queue.getList()).thenReturn(List.of(first, second));
        fixture.service.saveAll();

        RestoreFixture restore = new RestoreFixture(fixture.stateFile);
        doAnswer(invocation ->
        {
            String uri = invocation.getArgument(1);
            AudioLoadResultHandler resultHandler = invocation.getArgument(2);
            if (FIRST_URI.equals(uri))
            {
                resultHandler.trackLoaded(track(uri, 180_000L, false));
            }
            else
            {
                resultHandler.noMatches();
            }
            return null;
        }).when(restore.playerManager).loadItemOrdered(eq(restore.guild), anyString(),
                any(AudioLoadResultHandler.class));

        restore.service.restore(restore.guild, restore.handler);

        verify(restore.handler).restoreTracks(restore.captor.capture());
        assertEquals(1, restore.captor.getValue().size());
        assertEquals(FIRST_URI, restore.captor.getValue().get(0).getTrack().getInfo().uri);
    }

    @Test
    void restoreDoesNotStartPlaybackWhenHandlerAlreadyHasTracks()
    {
        Fixture fixture = new Fixture(temporaryDirectory.resolve("queuedata.json"));
        QueuedTrack queued = queuedTrack(FIRST_URI, metadata(42L, "query", 99L));
        when(fixture.queue.getList()).thenReturn(List.of(queued));
        fixture.service.saveAll();

        RestoreFixture restore = new RestoreFixture(fixture.stateFile);
        when(restore.queue.isEmpty()).thenReturn(false);

        restore.service.restore(restore.guild, restore.handler);

        verify(restore.playerManager, never()).loadItemOrdered(any(), anyString(), any());
        verify(restore.handler, never()).restoreTracks(anyList());
    }

    @Test
    void publicApisTolerateMissingAndMalformedStateFiles()
    {
        Path missing = temporaryDirectory.resolve("missing.json");
        Fixture missingFixture = new Fixture(missing);
        assertDoesNotThrow(missingFixture.service::restoreAll);
        assertDoesNotThrow(missingFixture.service::saveAll);

        Path malformed = temporaryDirectory.resolve("malformed.json");
        try
        {
            Files.writeString(malformed, "{not json");
        }
        catch (Exception ex)
        {
            fail(ex);
        }

        RestoreFixture restore = new RestoreFixture(malformed);
        assertDoesNotThrow(() -> restore.service.restore(restore.guild, restore.handler));
        assertDoesNotThrow(restore.service::restoreAll);
    }

    private static AudioTrack track(String uri, long duration, boolean seekable)
    {
        AudioTrack track = mock(AudioTrack.class);
        AudioTrackInfo info = new AudioTrackInfo("Title", "Author", duration, uri, false, uri);
        when(track.getInfo()).thenReturn(info);
        when(track.getDuration()).thenReturn(duration);
        when(track.isSeekable()).thenReturn(seekable);
        return track;
    }

    private static QueuedTrack queuedTrack(String uri, RequestMetadata metadata)
    {
        return new QueuedTrack(track(uri, 180_000L, false), metadata);
    }

    private static RequestMetadata metadata(long userId, String query, long channelId)
    {
        return new RequestMetadata(
                new RequestMetadata.UserInfo(userId, "user", "0001", "https://example.com/avatar"),
                new RequestMetadata.RequestInfo(query, "https://example.com/request", 0L),
                channelId);
    }

    private static final class Fixture
    {
        private final Bot bot = mock(Bot.class);
        private final JDA jda = mock(JDA.class);
        private final Guild guild = mock(Guild.class);
        private final AudioManager audioManager = mock(AudioManager.class);
        private final AudioHandler handler = mock(AudioHandler.class);
        private final AudioPlayer player = mock(AudioPlayer.class);
        private final ScheduledExecutorService threadpool = mock(ScheduledExecutorService.class);
        @SuppressWarnings("unchecked")
        private final AbstractQueue<QueuedTrack> queue = mock(AbstractQueue.class);
        private final Path stateFile;
        private final QueuePersistenceService service;

        private Fixture(Path stateFile)
        {
            this.stateFile = stateFile;
            when(bot.getJDA()).thenReturn(jda);
            when(bot.getThreadpool()).thenReturn(threadpool);
            when(jda.getGuilds()).thenReturn(List.of(guild));
            when(jda.getGuildById(GUILD_ID)).thenReturn(guild);
            when(guild.getIdLong()).thenReturn(GUILD_ID);
            when(guild.getAudioManager()).thenReturn(audioManager);
            when(audioManager.getSendingHandler()).thenReturn(handler);
            when(handler.getPlayer()).thenReturn(player);
            when(handler.getQueue()).thenReturn(queue);
            when(player.getPlayingTrack()).thenReturn(null);
            when(queue.getList()).thenReturn(List.of());
            when(queue.isEmpty()).thenReturn(true);
            service = new QueuePersistenceService(bot, stateFile);
        }
    }

    private static final class RestoreFixture
    {
        private final Bot bot = mock(Bot.class);
        private final JDA jda = mock(JDA.class);
        private final Guild guild = mock(Guild.class);
        private final AudioHandler handler = mock(AudioHandler.class);
        private final AudioPlayer player = mock(AudioPlayer.class);
        @SuppressWarnings("unchecked")
        private final AbstractQueue<QueuedTrack> queue = mock(AbstractQueue.class);
        private final PlayerManager playerManager = mock(PlayerManager.class);
        private final ScheduledExecutorService threadpool = mock(ScheduledExecutorService.class);
        private final QueuePersistenceService service;
        @SuppressWarnings("unchecked")
        private final ArgumentCaptor<List<QueuedTrack>> captor =
                ArgumentCaptor.forClass((Class<List<QueuedTrack>>) (Class<?>) List.class);

        private RestoreFixture(Path stateFile)
        {
            when(bot.getJDA()).thenReturn(jda);
            when(bot.getPlayerManager()).thenReturn(playerManager);
            when(bot.getThreadpool()).thenReturn(threadpool);
            when(jda.getGuilds()).thenReturn(List.of());
            when(guild.getIdLong()).thenReturn(GUILD_ID);
            when(handler.getPlayer()).thenReturn(player);
            when(handler.getQueue()).thenReturn(queue);
            when(player.getPlayingTrack()).thenReturn(null);
            when(queue.isEmpty()).thenReturn(true);
            doAnswer(invocation ->
            {
                ((Runnable) invocation.getArgument(0)).run();
                return null;
            }).when(threadpool).execute(any(Runnable.class));
            service = new QueuePersistenceService(bot, stateFile);
        }

        private Map<String, AudioTrack> configureSuccessfulLoads()
        {
            Map<String, AudioTrack> loaded = new HashMap<>();
            doAnswer(invocation ->
            {
                String uri = invocation.getArgument(1);
                AudioLoadResultHandler resultHandler = invocation.getArgument(2);
                AudioTrack track = track(uri, 180_000L, true);
                loaded.put(uri, track);
                resultHandler.trackLoaded(track);
                return null;
            }).when(playerManager).loadItemOrdered(eq(guild), anyString(),
                    any(AudioLoadResultHandler.class));
            return loaded;
        }
    }
}
