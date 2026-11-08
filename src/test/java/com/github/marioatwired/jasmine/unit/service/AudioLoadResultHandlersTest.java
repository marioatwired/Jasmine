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
import com.github.marioatwired.jasmine.audio.AudioHandler;
import com.github.marioatwired.jasmine.audio.AudioLoadWrapper;
import com.github.marioatwired.jasmine.audio.PlayerManager;
import com.github.marioatwired.jasmine.service.AudioLoadResultHandlers;
import com.github.marioatwired.jasmine.service.OutputAdapter;
import com.github.marioatwired.jasmine.service.TrackEnqueuer;
import com.github.marioatwired.jasmine.service.dto.TrackAddResult;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.SelfMember;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.managers.AudioManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AudioLoadResultHandlersTest
{
    private TrackEnqueuer trackEnqueuer;
    private Bot bot;
    private BotConfig config;
    private OutputAdapter output;
    private Guild guild;
    private Member member;
    private User user;
    private TextChannel channel;
    private PlayerManager playerManager;
    private AudioHandler audioHandler;

    @BeforeEach
    void setUp()
    {
        trackEnqueuer = mock(TrackEnqueuer.class);
        bot = mock(Bot.class);
        config = mock(BotConfig.class);
        output = mock(OutputAdapter.class);
        guild = mock(Guild.class);
        member = mock(Member.class);
        user = mock(User.class);
        channel = mock(TextChannel.class);
        playerManager = mock(PlayerManager.class);
        audioHandler = mock(AudioHandler.class);
        AudioManager audioManager = mock(AudioManager.class);
        SelfMember selfMember = mock(SelfMember.class);

        when(bot.getConfig()).thenReturn(config);
        when(bot.getPlayerManager()).thenReturn(playerManager);
        when(bot.getAudioLoadWrapper()).thenReturn(AudioLoadWrapper.NO_OP);
        when(config.getSuccess()).thenReturn("OK");
        when(config.getWarning()).thenReturn("WARN");
        when(config.getError()).thenReturn("ERROR");
        when(config.getMaxTime()).thenReturn("5:00");
        when(guild.getId()).thenReturn("guild-1");
        when(guild.getSelfMember()).thenReturn(selfMember);
        when(guild.getAudioManager()).thenReturn(audioManager);
        when(audioManager.getSendingHandler()).thenReturn(audioHandler);
        when(member.getUser()).thenReturn(user);
        when(member.getIdLong()).thenReturn(42L);
        when(user.getName()).thenReturn("Tester");
        when(channel.getIdLong()).thenReturn(77L);
    }

    @Test
    void trackLoadedAddsTrackToQueue()
    {
        AudioTrack track = track("Song");
        AudioLoadResultHandlers.PlayResultHandler handler = playHandler("query", false);
        when(trackEnqueuer.addTrackToQueue(guild, member, track, "query", channel))
                .thenReturn(new TrackAddResult(1, "Added **Song**", "Song"));

        handler.trackLoaded(track);

        verify(output).editMessage(contains("Added **Song**"));
    }

    @Test
    void tooLongTrackShowsWarning()
    {
        AudioTrack track = track("Long Song");
        AudioLoadResultHandlers.PlayResultHandler handler = playHandler("query", false);
        when(trackEnqueuer.addTrackToQueue(guild, member, track, "query", channel)).thenReturn(null);
        when(trackEnqueuer.formatTooLongError(track)).thenReturn("Track is too long.");

        handler.trackLoaded(track);

        verify(output).editMessage(contains("Track is too long."));
    }

    @Test
    void playNextTrackAddsToFront()
    {
        AudioTrack track = track("Next Song");
        AudioLoadResultHandlers.PlayNextResultHandler handler = new AudioLoadResultHandlers.PlayNextResultHandler(
                trackEnqueuer, bot, output, guild, member, "query", false, channel);
        when(trackEnqueuer.addTrackToFront(guild, member, track, "query", channel))
                .thenReturn(new TrackAddResult(0, "Playing next **Next Song**", "Next Song"));

        handler.trackLoaded(track);

        verify(trackEnqueuer).addTrackToFront(guild, member, track, "query", channel);
        verify(output).editMessage(contains("Playing next"));
    }

    @Test
    void noMatchesFallsBackToYouTubeSearch()
    {
        AudioLoadResultHandlers.PlayResultHandler handler = playHandler("query", false);

        handler.noMatches();

        verify(playerManager).loadItemOrdered(eq(guild), eq("ytsearch:query"), any(AudioLoadResultHandler.class));
    }

    @Test
    void noMatchesOnFallbackShowsNoResults()
    {
        AudioLoadResultHandlers.PlayResultHandler handler = playHandler("query", true);

        handler.noMatches();

        verify(output).editMessage(contains("No results found"));
        verify(playerManager, never()).loadItemOrdered(any(), any(String.class), any(AudioLoadResultHandler.class));
    }

    @Test
    void commonLoadFailureShowsErrorMessage()
    {
        AudioLoadResultHandlers.PlayResultHandler handler = playHandler("query", false);

        handler.loadFailed(new FriendlyException("boom", Severity.COMMON, null));

        verify(output).editMessage(contains("Error loading: boom"));
    }

    @Test
    void playlistLoadedAddsAllTracks()
    {
        AudioTrack first = track("First");
        AudioTrack second = track("Second");
        AudioPlaylist playlist = playlist("mix", false, null, first, second);
        AudioLoadResultHandlers.PlayResultHandler handler = playHandler("query", false);
        when(trackEnqueuer.isTooLong(any())).thenReturn(false);

        handler.playlistLoaded(playlist);

        verify(audioHandler, org.mockito.Mockito.times(2)).addTrack(any());
        verify(output).editMessage(contains("added to the queue"));
    }

    @Test
    void playlistLoadedReportsWhenAllTracksAreTooLong()
    {
        AudioTrack first = track("First");
        AudioTrack second = track("Second");
        AudioPlaylist playlist = playlist("mix", false, null, first, second);
        AudioLoadResultHandlers.PlayResultHandler handler = playHandler("query", false);
        when(trackEnqueuer.isTooLong(any())).thenReturn(true);

        handler.playlistLoaded(playlist);

        verify(output).editMessage(contains("All entries"));
    }

    private AudioLoadResultHandlers.PlayResultHandler playHandler(String args, boolean ytsearch)
    {
        return new AudioLoadResultHandlers.PlayResultHandler(
                trackEnqueuer, bot, output, guild, member, args, ytsearch, channel);
    }

    private AudioTrack track(String title)
    {
        AudioTrack track = mock(AudioTrack.class);
        AudioTrackInfo info = new AudioTrackInfo(title, "Author", 1000, "id-" + title, false,
                "https://example.com/" + title);
        when(track.getInfo()).thenReturn(info);
        when(track.getIdentifier()).thenReturn("id-" + title);
        return track;
    }

    private AudioPlaylist playlist(String name, boolean searchResult, AudioTrack selected, AudioTrack... tracks)
    {
        AudioPlaylist playlist = mock(AudioPlaylist.class);
        when(playlist.getName()).thenReturn(name);
        when(playlist.getTracks()).thenReturn(List.of(tracks));
        when(playlist.isSearchResult()).thenReturn(searchResult);
        when(playlist.getSelectedTrack()).thenReturn(selected);
        return playlist;
    }
}
