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
package com.github.marioatwired.jasmine.unit.service;

import com.github.marioatwired.jasmine.Bot;
import com.github.marioatwired.jasmine.BotConfig;
import com.github.marioatwired.jasmine.playlist.PlaylistLoader;
import com.github.marioatwired.jasmine.service.PlaylistMutationService;
import com.github.marioatwired.jasmine.service.dto.PlaylistMutationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("PlaylistMutationService Tests")
class PlaylistMutationServiceTest
{
    @TempDir
    Path tempDir;

    private PlaylistLoader loader;
    private PlaylistMutationService service;
    private final String nl = System.lineSeparator();

    @BeforeEach
    void setUp()
    {
        BotConfig config = mock(BotConfig.class);
        when(config.getPlaylistsFolder()).thenReturn(tempDir.toString());
        loader = new PlaylistLoader(config);

        Bot bot = mock(Bot.class);
        when(bot.getPlaylistLoader()).thenReturn(loader);
        service = new PlaylistMutationService(bot);
    }

    @Test
    @DisplayName("appendItems() preserves the shuffle directive")
    void appendItemsPreservesShuffleDirective() throws IOException
    {
        writePlaylist("mix", "#shuffle\nurl1\nurl2\n");

        PlaylistMutationResult result = service.appendItems("mix", List.of("url3", "url4"));

        assertTrue(result.success);
        assertEquals(4, result.totalItems);
        assertEquals("#shuffle" + nl + "url1" + nl + "url2" + nl + "url3" + nl + "url4",
                readPlaylist("mix"));
    }

    @Test
    @DisplayName("removeItem() removes the requested position")
    void removeItemRemovesPosition() throws IOException
    {
        writePlaylist("mix", "url1\nurl2\nurl3\n");

        PlaylistMutationResult result = service.removeItem("mix", 2);

        assertTrue(result.success);
        assertEquals("url1" + nl + "url3", readPlaylist("mix"));
    }

    @Test
    @DisplayName("moveItem() reorders the requested position")
    void moveItemReordersPosition() throws IOException
    {
        writePlaylist("mix", "url1\nurl2\nurl3\n");

        PlaylistMutationResult result = service.moveItem("mix", 1, 3);

        assertTrue(result.success);
        assertEquals("url2" + nl + "url3" + nl + "url1", readPlaylist("mix"));
    }

    @Test
    @DisplayName("replaceItem() replaces the requested position")
    void replaceItemReplacesPosition() throws IOException
    {
        writePlaylist("mix", "url1\nurl2\n");

        PlaylistMutationResult result = service.replaceItem("mix", 2, "new-url");

        assertTrue(result.success);
        assertEquals("url1" + nl + "new-url", readPlaylist("mix"));
    }

    @Test
    @DisplayName("invalid positions return an error and do not write")
    void invalidPositionReturnsError() throws IOException
    {
        writePlaylist("mix", "url1" + nl + "url2" + nl);

        PlaylistMutationResult result = service.removeItem("mix", 4);

        assertFalse(result.success);
        assertTrue(result.errorMessage.contains("between 1 and 2"));
        assertEquals("url1" + nl + "url2" + nl, readPlaylist("mix"));
    }

    @Test
    @DisplayName("missing playlists return an error")
    void missingPlaylistReturnsError()
    {
        PlaylistMutationResult result = service.appendItems("missing", List.of("url1"));

        assertFalse(result.success);
        assertTrue(result.errorMessage.contains("does not exist"));
    }

    private void writePlaylist(String name, String content) throws IOException
    {
        Files.writeString(tempDir.resolve(name + ".txt"), content);
    }

    private String readPlaylist(String name) throws IOException
    {
        return Files.readString(tempDir.resolve(name + ".txt"));
    }
}
