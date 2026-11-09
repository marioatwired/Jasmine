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
package com.github.marioatwired.jasmine.unit.service;

import com.github.marioatwired.jasmine.Bot;
import com.github.marioatwired.jasmine.BotConfig;
import com.github.marioatwired.jasmine.playlist.PlaylistLoader;
import com.github.marioatwired.jasmine.service.PlaylistTransferService;
import com.github.marioatwired.jasmine.service.dto.PlaylistTransferResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PlaylistTransferServiceTest
{
    @TempDir
    Path temporaryDirectory;

    private PlaylistLoader loader;
    private PlaylistTransferService service;

    @BeforeEach
    void setUp()
    {
        BotConfig config = mock(BotConfig.class);
        when(config.getPlaylistsFolder()).thenReturn(temporaryDirectory.toString());
        loader = new PlaylistLoader(config);

        Bot bot = mock(Bot.class);
        when(bot.getPlaylistLoader()).thenReturn(loader);
        service = new PlaylistTransferService(bot);
    }

    @Test
    void importPlaylistWritesNewPlaylistAndPreservesDirectives() throws Exception
    {
        String content = "#shuffle\nhttps://example.com/one\nhttps://example.com/two\n";

        PlaylistTransferResult result = service.importPlaylist("My Imported List",
                content.getBytes(StandardCharsets.UTF_8));

        assertTrue(result.success());
        assertEquals("My_Imported_List", result.playlistName());
        assertEquals(2, result.entries());
        assertNotNull(loader.getPlaylist("My_Imported_List"));
        assertEquals(2, loader.getPlaylist("My_Imported_List").getItems().size());
        assertTrue(Files.readString(temporaryDirectory.resolve("My_Imported_List.txt"))
                .contains("#shuffle"));
    }

    @Test
    void importPlaylistRejectsExistingPlaylist() throws Exception
    {
        Files.writeString(temporaryDirectory.resolve("existing.txt"), "https://example.com/one");

        PlaylistTransferResult result = service.importPlaylist("existing",
                "https://example.com/two".getBytes(StandardCharsets.UTF_8));

        assertFalse(result.success());
        assertTrue(result.message().contains("already exists"));
    }

    @Test
    void importPlaylistRejectsUnsafeNamesAndInvalidContent() throws Exception
    {
        assertFalse(service.importPlaylist("..",
                "https://example.com/two".getBytes(StandardCharsets.UTF_8)).success());
        assertFalse(service.importPlaylist("name",
                "#shuffle\n// comment only\n".getBytes(StandardCharsets.UTF_8)).success());
        assertFalse(service.importPlaylist("binary",
                new byte[]{'a', 0, 'b'}).success());
        assertFalse(service.importPlaylist("large",
                new byte[PlaylistTransferService.MAX_IMPORT_BYTES + 1]).success());
        assertFalse(Files.exists(temporaryDirectory.resolve("name.txt")));
    }

    @Test
    void exportPlaylistReturnsRawTextAndEntryCount() throws Exception
    {
        Files.writeString(temporaryDirectory.resolve("exported_list.txt"),
                "#shuffle\nhttps://example.com/one\nhttps://example.com/two\n");

        PlaylistTransferResult result = service.exportPlaylist("Exported List");

        assertTrue(result.success());
        assertEquals("Exported_List", result.playlistName());
        assertEquals(2, result.entries());
        assertNotNull(result.content());
        String content = new String(result.content(), StandardCharsets.UTF_8);
        assertTrue(content.contains("#shuffle"));
        assertTrue(content.contains("https://example.com/one"));
    }

    @Test
    void exportPlaylistRejectsMissingAndEmptyPlaylists() throws Exception
    {
        assertFalse(service.exportPlaylist("missing").success());

        Files.writeString(temporaryDirectory.resolve("empty.txt"), "#shuffle\n");
        PlaylistTransferResult empty = service.exportPlaylist("empty");
        assertFalse(empty.success());
        assertTrue(empty.message().contains("does not contain any playlist entries"));
    }
}
