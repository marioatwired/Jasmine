package com.github.marioatwired.jasmine.unit.service;

import com.github.marioatwired.jasmine.TestBase;
import com.github.marioatwired.jasmine.playlist.PlaylistLoader;
import com.github.marioatwired.jasmine.service.MusicService;
import com.github.marioatwired.jasmine.service.PlaylistDraftService;
import com.github.marioatwired.jasmine.service.dto.PlaylistDraftContext;
import com.github.marioatwired.jasmine.service.dto.PlaylistDraftMutationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PlaylistDraftServiceTest extends TestBase
{
    private PlaylistLoader playlistLoader;
    private PlaylistDraftService draftService;
    private PlaylistDraftContext context;

    @BeforeEach
    @Override
    public void setUp()
    {
        super.setUp();
        playlistLoader = mock(PlaylistLoader.class);
        when(bot.getPlaylistLoader()).thenReturn(playlistLoader);

        PlaylistLoader.Playlist playlist = mock(PlaylistLoader.Playlist.class);
        when(playlist.getItems()).thenReturn(new ArrayList<>(List.of("a", "b", "c")));
        when(playlistLoader.getPlaylistResult("mix"))
                .thenReturn(PlaylistLoader.PlaylistResult.success(playlist));
        when(playlistLoader.writePlaylistResult(eq("mix"), anyString()))
                .thenReturn(PlaylistLoader.PlaylistResult.success(null));

        draftService = new PlaylistDraftService(bot);
        context = draftService.buildPlaylistDraftContext(GUILD_ID, 1L, 2L, OWNER_ID, "mix");
    }

    @Test
    void removeDraftItemCreatesDraftAndMarksItDirty()
    {
        PlaylistDraftMutationResult result = draftService.removePlaylistDraftItem(context, 2);

        assertTrue(result.success);
        assertEquals(2, result.totalItems);
        assertTrue(result.dirty);
        assertEquals(1, result.revision);
        assertTrue(draftService.isPlaylistDraftDirty(context));
        assertEquals(2, draftService.getPlaylistTrackCount(context));
    }

    @Test
    void moveDraftItemPersistsNewOrderWhenSaved()
    {
        PlaylistDraftMutationResult moveResult = draftService.movePlaylistDraftItem(context, 1, 3);

        assertTrue(moveResult.success);
        assertTrue(moveResult.dirty);

        PlaylistDraftMutationResult saveResult = draftService.savePlaylistDraft(context);

        assertTrue(saveResult.success);
        assertFalse(saveResult.dirty);
        assertFalse(draftService.isPlaylistDraftDirty(context));
        verify(playlistLoader).writePlaylistResult("mix", "b" + System.lineSeparator()
                + "c" + System.lineSeparator() + "a");
    }

    @Test
    void discardDraftRestoresStoredPlaylistView()
    {
        draftService.removePlaylistDraftItem(context, 1);

        draftService.discardPlaylistDraft(context);

        assertFalse(draftService.isPlaylistDraftDirty(context));
        assertEquals(3, draftService.getPlaylistTrackCount(context));
    }

    @Test
    void saveWithoutChangesReturnsError()
    {
        PlaylistDraftMutationResult result = draftService.savePlaylistDraft(context);

        assertFalse(result.success);
        assertEquals("There are no unsaved changes.", result.errorMessage);
    }

    @Test
    void missingPlaylistCannotCreateDraft()
    {
        PlaylistLoader.PlaylistError error = PlaylistLoader.PlaylistError.of(
                PlaylistLoader.PlaylistErrorType.PLAYLIST_NOT_FOUND, "Playlist not found.", "Playlists", null);
        when(playlistLoader.getPlaylistResult("missing"))
                .thenReturn(PlaylistLoader.PlaylistResult.failure(error));

        PlaylistDraftContext missingContext =
                draftService.buildPlaylistDraftContext(GUILD_ID, 1L, 2L, OWNER_ID, "missing");
        PlaylistDraftMutationResult result = draftService.removePlaylistDraftItem(missingContext, 1);

        assertFalse(result.success);
        assertEquals("Playlist no longer exists.", result.errorMessage);
        assertEquals(-1, draftService.getPlaylistTrackCount(missingContext));
    }

    @Test
    void invalidPositionReturnsError()
    {
        PlaylistDraftMutationResult result = draftService.removePlaylistDraftItem(context, 4);

        assertFalse(result.success);
        assertEquals("Position must be between 1 and 3.", result.errorMessage);
    }
}
