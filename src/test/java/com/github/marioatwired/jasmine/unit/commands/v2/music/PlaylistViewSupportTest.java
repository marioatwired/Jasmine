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
package com.github.marioatwired.jasmine.unit.commands.v2.music;

import com.github.marioatwired.jasmine.commands.v2.music.PlaylistViewSupport;
import com.github.marioatwired.jasmine.service.MusicService;
import com.github.marioatwired.jasmine.service.dto.PlaylistDetailsInfo;
import com.github.marioatwired.jasmine.service.dto.PlaylistNamesInfo;
import com.github.marioatwired.jasmine.service.dto.PlaylistTracksPageInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("PlaylistViewSupport Tests")
class PlaylistViewSupportTest
{
    private MusicService musicService;

    @BeforeEach
    void setUp()
    {
        musicService = mock(MusicService.class);
    }

    @Test
    @DisplayName("builds an embed for a known playlist")
    void buildsEmbedForKnownPlaylist()
    {
        when(musicService.getAvailablePlaylistNames())
                .thenReturn(PlaylistNamesInfo.success(List.of("mix")));
        when(musicService.getPlaylistDetails("mix"))
                .thenReturn(new PlaylistDetailsInfo("mix", 2, List.of(), false));
        doAnswer(invocation ->
        {
            Consumer<PlaylistTracksPageInfo> callback = invocation.getArgument(3);
            callback.accept(new PlaylistTracksPageInfo(
                    "mix", 2, 0, false, false, List.of("line1", "line2")));
            return null;
        }).when(musicService).loadPlaylistPageWithTracks(eq("mix"), eq(1), anyInt(), any());

        AtomicReference<PlaylistViewSupport.Result> resultRef = new AtomicReference<>();
        PlaylistViewSupport.open(musicService, null, null, 42L, "mix", 1, resultRef::set);

        PlaylistViewSupport.Result result = resultRef.get();
        assertNotNull(result);
        assertFalse(result.hasError());
        assertNotNull(result.embed());
        assertEquals("Playlist: mix", result.embed().getTitle());
        assertTrue(result.components().isEmpty());
    }

    @Test
    @DisplayName("returns an error for an unknown playlist")
    void returnsErrorForUnknownPlaylist()
    {
        when(musicService.getAvailablePlaylistNames())
                .thenReturn(PlaylistNamesInfo.success(List.of("mix")));

        AtomicReference<PlaylistViewSupport.Result> resultRef = new AtomicReference<>();
        PlaylistViewSupport.open(musicService, null, null, 42L, "missing", 1, resultRef::set);

        PlaylistViewSupport.Result result = resultRef.get();
        assertNotNull(result);
        assertTrue(result.hasError());
        assertTrue(result.errorMessage().contains("doesn't exist"));
    }
}
