package com.github.marioatwired.jasmine.unit.service;

import com.github.marioatwired.jasmine.TestBase;
import com.github.marioatwired.jasmine.service.MusicService;
import com.github.marioatwired.jasmine.service.OutputAdapter;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class PlayerServiceTest extends TestBase {

    private OutputAdapter output = mock(OutputAdapter.class);

    private MusicService musicService;

    @BeforeEach
    @Override
    public void setUp() {
        super.setUp();
        musicService = new MusicService(bot);
    }

    @Test
    public void testPlayResumeWhenPaused() {
        when(audioPlayer.getPlayingTrack()).thenReturn(audioTrack);
        when(audioPlayer.isPaused()).thenReturn(true);
        AudioTrackInfo info = new AudioTrackInfo("Title", "Author", 1000, "identifier", true, "uri");
        when(audioTrack.getInfo()).thenReturn(info);

        musicService.play(guild, member, null, textChannel, output);

        verify(audioPlayer).setPaused(false);
        verify(output).replySuccess(anyString());
    }

    @Test
    public void testPlayWithArgsCallsLoadItem() {
        String args = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"; // this is the best song ever
        
        musicService.play(guild, member, args, textChannel, output);

        verify(playerManager).loadItemOrdered(eq(guild), eq(args), any());
    }

    @Test
    public void testPlayEmptyArgsShowsHelpWhenNotPaused() {
        when(audioPlayer.getPlayingTrack()).thenReturn(null);

        musicService.play(guild, member, null, textChannel, output);

        verify(output).onShowHelp();
    }
}
