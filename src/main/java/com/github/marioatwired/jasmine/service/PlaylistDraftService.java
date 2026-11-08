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
import com.github.marioatwired.jasmine.playlist.PlaylistLoader.Playlist;
import com.github.marioatwired.jasmine.playlist.PlaylistLoader;
import com.github.marioatwired.jasmine.service.dto.PlaylistDraftContext;
import com.github.marioatwired.jasmine.service.dto.PlaylistDraftMutationResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Draft state for interactive playlist editing.
 */
public class PlaylistDraftService
{
    private final Bot bot;
    private final Map<PlaylistDraftKey, PlaylistDraftState> playlistDrafts = new ConcurrentHashMap<>();

    public PlaylistDraftService(Bot bot)
    {
        this.bot = bot;
    }

    public PlaylistDraftContext buildPlaylistDraftContext(long guildId, long channelId, long messageId,
                                                          long userId, String playlistName)
    {
        return new PlaylistDraftContext(guildId, channelId, messageId, userId, playlistName);
    }

    public PlaylistDraftMutationResult removePlaylistDraftItem(PlaylistDraftContext context, int position)
    {
        PlaylistDraftState state = getOrCreatePlaylistDraftState(context);
        if (state == null)
        {
            return PlaylistDraftMutationResult.error("Playlist no longer exists.");
        }
        if (position < 1 || position > state.items.size())
        {
            return PlaylistDraftMutationResult.error("Position must be between 1 and " + state.items.size() + ".");
        }
        state.items.remove(position - 1);
        state.dirty = true;
        state.revision++;
        return PlaylistDraftMutationResult.success(state.items.size(), true, state.revision);
    }

    public PlaylistDraftMutationResult movePlaylistDraftItem(PlaylistDraftContext context, int fromPosition, int toPosition)
    {
        PlaylistDraftState state = getOrCreatePlaylistDraftState(context);
        if (state == null)
        {
            return PlaylistDraftMutationResult.error("Playlist no longer exists.");
        }
        int size = state.items.size();
        if (fromPosition < 1 || fromPosition > size || toPosition < 1 || toPosition > size)
        {
            return PlaylistDraftMutationResult.error("Positions must be between 1 and " + size + ".");
        }
        if (fromPosition == toPosition)
        {
            return PlaylistDraftMutationResult.success(size, state.dirty, state.revision);
        }
        String item = state.items.remove(fromPosition - 1);
        state.items.add(toPosition - 1, item);
        state.dirty = true;
        state.revision++;
        return PlaylistDraftMutationResult.success(state.items.size(), true, state.revision);
    }

    public PlaylistDraftMutationResult savePlaylistDraft(PlaylistDraftContext context)
    {
        PlaylistDraftState state = playlistDrafts.get(toDraftKey(context));
        if (state == null)
        {
            return PlaylistDraftMutationResult.error("There are no unsaved changes.");
        }
        if (!state.dirty)
        {
            return PlaylistDraftMutationResult.error("There are no unsaved changes.");
        }
        String content = String.join(System.lineSeparator(), state.items);
        PlaylistLoader.PlaylistResult<Void> writeResult = bot.getPlaylistLoader()
                .writePlaylistResult(context.playlistName(), content);
        if (!writeResult.isSuccess())
        {
            return PlaylistDraftMutationResult.error(
                    PlaylistErrorMessages.toMessage(writeResult.getError()));
        }
        state.dirty = false;
        state.revision++;
        return PlaylistDraftMutationResult.success(state.items.size(), false, state.revision);
    }

    public void discardPlaylistDraft(PlaylistDraftContext context)
    {
        playlistDrafts.remove(toDraftKey(context));
    }

    public boolean isPlaylistDraftDirty(PlaylistDraftContext context)
    {
        PlaylistDraftState state = playlistDrafts.get(toDraftKey(context));
        return state != null && state.dirty;
    }

    public int getPlaylistTrackCount(PlaylistDraftContext context)
    {
        PlaylistDraftState state = playlistDrafts.get(toDraftKey(context));
        if (state != null)
        {
            return state.items.size();
        }
        PlaylistLoader.PlaylistResult<Playlist> playlistResult = bot.getPlaylistLoader()
                .getPlaylistResult(context.playlistName());
        if (!playlistResult.isSuccess())
        {
            return -1;
        }
        return playlistResult.getValue().getItems().size();
    }

    /**
     * Returns a snapshot of the draft items for the given context, or null when no draft exists.
     */
    List<String> getDraftItems(PlaylistDraftContext context)
    {
        PlaylistDraftState state = playlistDrafts.get(toDraftKey(context));
        return state == null ? null : new ArrayList<>(state.items);
    }

    private PlaylistDraftKey toDraftKey(PlaylistDraftContext context)
    {
        return new PlaylistDraftKey(
                context.guildId(),
                context.channelId(),
                context.messageId(),
                context.userId(),
                context.playlistName()
        );
    }

    private PlaylistDraftState getOrCreatePlaylistDraftState(PlaylistDraftContext context)
    {
        PlaylistDraftKey key = toDraftKey(context);
        PlaylistDraftState existing = playlistDrafts.get(key);
        if (existing != null)
        {
            return existing;
        }

        PlaylistLoader.PlaylistResult<Playlist> playlistResult = bot.getPlaylistLoader()
                .getPlaylistResult(context.playlistName());
        if (!playlistResult.isSuccess())
        {
            return null;
        }
        Playlist playlist = playlistResult.getValue();
        PlaylistDraftState created = new PlaylistDraftState(new ArrayList<>(playlist.getItems()));
        PlaylistDraftState raced = playlistDrafts.putIfAbsent(key, created);
        return raced != null ? raced : created;
    }

    private record PlaylistDraftKey(long guildId, long channelId, long messageId, long userId, String playlistName)
    {
    }

    private static class PlaylistDraftState
    {
        private final List<String> items;
        private boolean dirty;
        private int revision;

        private PlaylistDraftState(List<String> items)
        {
            this.items = items;
        }
    }
}
