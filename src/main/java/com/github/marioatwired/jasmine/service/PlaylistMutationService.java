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
import com.github.marioatwired.jasmine.playlist.PlaylistLoader;
import com.github.marioatwired.jasmine.playlist.PlaylistLoader.Playlist;
import com.github.marioatwired.jasmine.service.dto.PlaylistMutationResult;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Direct, non-interactive mutations of saved playlist files.
 */
public class PlaylistMutationService
{
    private final Bot bot;

    public PlaylistMutationService(Bot bot)
    {
        this.bot = bot;
    }

    public PlaylistMutationResult appendItems(String playlistName, List<String> newItems)
    {
        List<String> normalized = new ArrayList<>();
        if (newItems != null)
        {
            for (String item : newItems)
            {
                if (item != null && !item.isBlank())
                {
                    normalized.add(item.trim());
                }
            }
        }
        if (normalized.isEmpty())
        {
            return PlaylistMutationResult.error("Please include at least one URL to add!");
        }

        return update(playlistName, items ->
        {
            items.addAll(normalized);
            return PlaylistMutationResult.success(items.size());
        });
    }

    public PlaylistMutationResult removeItem(String playlistName, int position)
    {
        return update(playlistName, items ->
        {
            if (position < 1 || position > items.size())
            {
                return PlaylistMutationResult.error(
                        "Position must be between 1 and " + items.size() + ".");
            }
            items.remove(position - 1);
            return PlaylistMutationResult.success(items.size());
        });
    }

    public PlaylistMutationResult moveItem(String playlistName, int fromPosition, int toPosition)
    {
        return update(playlistName, items ->
        {
            if (fromPosition < 1 || fromPosition > items.size()
                    || toPosition < 1 || toPosition > items.size())
            {
                return PlaylistMutationResult.error(
                        "Positions must be between 1 and " + items.size() + ".");
            }
            if (fromPosition == toPosition)
            {
                return PlaylistMutationResult.error(
                        "That song is already at position #" + toPosition + ".");
            }
            String item = items.remove(fromPosition - 1);
            items.add(toPosition - 1, item);
            return PlaylistMutationResult.success(items.size());
        });
    }

    public PlaylistMutationResult replaceItem(String playlistName, int position, String item)
    {
        String normalized = item == null ? "" : item.trim();
        if (normalized.isEmpty())
        {
            return PlaylistMutationResult.error("Please include the replacement URL.");
        }

        return update(playlistName, items ->
        {
            if (position < 1 || position > items.size())
            {
                return PlaylistMutationResult.error(
                        "Position must be between 1 and " + items.size() + ".");
            }
            items.set(position - 1, normalized);
            return PlaylistMutationResult.success(items.size());
        });
    }

    private PlaylistMutationResult update(String playlistName,
                                          Function<List<String>, PlaylistMutationResult> mutation)
    {
        PlaylistLoader.PlaylistResult<Playlist> readResult =
                bot.getPlaylistLoader().getPlaylistResult(playlistName);
        if (!readResult.isSuccess())
        {
            return PlaylistMutationResult.error(
                    PlaylistErrorMessages.toMessage(readResult.getError()));
        }

        Playlist playlist = readResult.getValue();
        List<String> items = new ArrayList<>(playlist.getItems());
        PlaylistMutationResult mutationResult = mutation.apply(items);
        if (!mutationResult.success)
        {
            return mutationResult;
        }

        StringBuilder content = new StringBuilder();
        if (playlist.isShuffle())
        {
            content.append("#shuffle").append(System.lineSeparator());
        }
        content.append(String.join(System.lineSeparator(), items));

        PlaylistLoader.PlaylistResult<Void> writeResult =
                bot.getPlaylistLoader().writePlaylistResult(playlistName, content.toString());
        if (!writeResult.isSuccess())
        {
            return PlaylistMutationResult.error(
                    PlaylistErrorMessages.toMessage(writeResult.getError()));
        }
        return PlaylistMutationResult.success(items.size());
    }
}
