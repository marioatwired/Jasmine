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
package com.github.marioatwired.jasmine.service;

import com.github.marioatwired.jasmine.Bot;
import com.github.marioatwired.jasmine.playlist.PlaylistLoader;
import com.github.marioatwired.jasmine.service.dto.PlaylistTransferResult;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Imports and exports saved playlists as portable text files.
 */
public class PlaylistTransferService
{
    public static final int MAX_IMPORT_BYTES = 1024 * 1024;

    private final Bot bot;

    public PlaylistTransferService(Bot bot)
    {
        this.bot = bot;
    }

    /**
     * Imports playlist text into a new playlist. Existing playlists are never
     * overwritten.
     *
     * @param requestedName user-provided playlist name
     * @param content       uploaded file bytes
     * @return import result
     */
    public PlaylistTransferResult importPlaylist(String requestedName, byte[] content)
    {
        String name = PlaylistLoader.sanitizeName(requestedName);
        if (name.isEmpty())
            return PlaylistTransferResult.failure("Please provide a valid playlist name.");

        if (content == null || content.length == 0)
            return PlaylistTransferResult.failure("The attached playlist file is empty.");
        if (content.length > MAX_IMPORT_BYTES)
            return PlaylistTransferResult.failure("The attached playlist is too large (maximum 1 MiB).");

        String text = new String(content, StandardCharsets.UTF_8);
        if (text.startsWith("\uFEFF"))
            text = text.substring(1);
        if (text.indexOf('\0') >= 0)
            return PlaylistTransferResult.failure("The attached playlist contains binary data.");

        int entries = countEntries(text);
        if (entries == 0)
            return PlaylistTransferResult.failure(
                    "The attached file does not contain any playlist entries.");

        if (bot.getPlaylistLoader().getPlaylistResult(name).isSuccess())
            return PlaylistTransferResult.failure("Playlist `" + name + "` already exists!");

        PlaylistLoader.PlaylistResult<Void> createResult = bot.getPlaylistLoader()
                .createPlaylistResult(name);
        if (!createResult.isSuccess())
            return PlaylistTransferResult.failure(
                    PlaylistErrorMessages.toMessage(createResult.getError()));

        PlaylistLoader.PlaylistResult<Void> writeResult = bot.getPlaylistLoader()
                .writePlaylistResult(name, text);
        if (!writeResult.isSuccess())
        {
            bot.getPlaylistLoader().deletePlaylistResult(name);
            return PlaylistTransferResult.failure(
                    PlaylistErrorMessages.toMessage(writeResult.getError()));
        }

        return PlaylistTransferResult.success(
                "Imported " + entries + " entries into playlist `" + name + "`.",
                name, entries, null);
    }

    /**
     * Reads a saved playlist and returns its raw text as downloadable bytes.
     *
     * @param requestedName user-provided playlist name
     * @return export result
     */
    public PlaylistTransferResult exportPlaylist(String requestedName)
    {
        String name = PlaylistLoader.sanitizeName(requestedName);
        if (name.isEmpty())
            return PlaylistTransferResult.failure("Please provide a valid playlist name.");

        PlaylistLoader.PlaylistResult<List<String>> readResult = bot.getPlaylistLoader()
                .readPlaylistResult(name);
        if (!readResult.isSuccess())
            return PlaylistTransferResult.failure(
                    PlaylistErrorMessages.toMessage(readResult.getError()));

        List<String> lines = readResult.getValue();
        if (lines == null || lines.isEmpty())
            return PlaylistTransferResult.failure("Playlist `" + name + "` is empty.");

        String content = String.join(System.lineSeparator(), lines) + System.lineSeparator();
        int entries = countEntries(content);
        if (entries == 0)
            return PlaylistTransferResult.failure(
                    "Playlist `" + name + "` does not contain any playlist entries.");

        return PlaylistTransferResult.success(
                "Exported playlist `" + name + "` (" + entries + " entries).",
                name, entries, content.getBytes(StandardCharsets.UTF_8));
    }

    private static int countEntries(String text)
    {
        int entries = 0;
        for (String line : text.lines().toList())
        {
            String value = line.trim();
            if (!value.isEmpty() && !value.startsWith("#") && !value.startsWith("//"))
                entries++;
        }
        return entries;
    }
}
