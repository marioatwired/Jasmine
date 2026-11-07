package com.github.marioatwired.jasmine.unit.service;

import com.github.marioatwired.jasmine.TestBase;
import com.github.marioatwired.jasmine.playlist.PlaylistLoader;
import com.github.marioatwired.jasmine.service.MusicService;
import com.github.marioatwired.jasmine.service.PlaylistPreviewService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PlaylistPreviewServiceTest extends TestBase
{
    private PlaylistLoader playlistLoader;
    private PlaylistPreviewService previewService;

    @BeforeEach
    @Override
    public void setUp()
    {
        super.setUp();
        playlistLoader = mock(PlaylistLoader.class);
        when(bot.getPlaylistLoader()).thenReturn(playlistLoader);

        PlaylistLoader.Playlist playlist = mock(PlaylistLoader.Playlist.class);
        when(playlist.getName()).thenReturn("mix");
        when(playlist.getItems()).thenReturn(new ArrayList<>(List.of("a", "b", "c")));
        when(playlistLoader.getPlaylistResult("mix"))
                .thenReturn(PlaylistLoader.PlaylistResult.success(playlist));

        previewService = new PlaylistPreviewService(bot);
    }

    @Test
    void resolvesTrackUrlByPosition()
    {
        assertEquals("b", previewService.getPlaylistTrackUrlAtPosition("mix", 2));
        assertNull(previewService.getPlaylistTrackUrlAtPosition("mix", 4));
    }

    @Test
    void resolvesTrackUrlFromDraftItems()
    {
        assertEquals("c", previewService.getPlaylistTrackUrlAtPosition(List.of("a", "b", "c"), 3));
        assertNull(previewService.getPlaylistTrackUrlAtPosition(List.of("a", "b", "c"), 0));
    }

    @Test
    void emptyPageReturnsPageInfoWithoutLoadingTracks()
    {
        AtomicReference<MusicService.PlaylistTracksPageInfo> pageRef = new AtomicReference<>();

        previewService.loadPlaylistPageWithTracks("mix", 3, 2, pageRef::set);

        MusicService.PlaylistTracksPageInfo page = pageRef.get();
        assertEquals("mix", page.playlistName);
        assertEquals(3, page.totalItems);
        assertEquals(4, page.startIndex);
        assertFalse(page.hasMore);
        assertFalse(page.dirty);
        assertTrue(page.formattedLines.isEmpty());
    }

    @Test
    void draftPageKeepsDirtyFlag()
    {
        AtomicReference<MusicService.PlaylistTracksPageInfo> pageRef = new AtomicReference<>();

        previewService.loadPlaylistPageWithTracks("mix", List.of("a"), 2, 1, true, pageRef::set);

        MusicService.PlaylistTracksPageInfo page = pageRef.get();
        assertEquals(1, page.totalItems);
        assertEquals(1, page.startIndex);
        assertFalse(page.hasMore);
        assertTrue(page.dirty);
        assertTrue(page.formattedLines.isEmpty());
    }

    @Test
    void missingPlaylistReturnsNullPreview()
    {
        PlaylistLoader.PlaylistError error = PlaylistLoader.PlaylistError.of(
                PlaylistLoader.PlaylistErrorType.PLAYLIST_NOT_FOUND, "Playlist not found.", "Playlists", null);
        when(playlistLoader.getPlaylistResult("missing"))
                .thenReturn(PlaylistLoader.PlaylistResult.failure(error));
        AtomicReference<MusicService.PlaylistPreviewWithTracks> previewRef = new AtomicReference<>();

        previewService.loadPlaylistPreviewWithTracks("missing", 5, previewRef::set);

        assertNull(previewRef.get());
    }
}
