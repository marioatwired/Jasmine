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
package com.github.marioatwired.jasmine.unit.commands.v1.music;

import com.github.marioatwired.jasmine.Bot;
import com.github.marioatwired.jasmine.BotConfig;
import com.github.marioatwired.jasmine.audio.AudioHandler;
import com.github.marioatwired.jasmine.audio.AudioLoadWrapper;
import com.github.marioatwired.jasmine.audio.PlayerManager;
import com.github.marioatwired.jasmine.commands.v1.music.SearchCmd;
import com.jagrosh.jdautilities.command.CommandClient;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import net.dv8tion.jda.api.components.MessageTopLevelComponent;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.RoleColors;
import net.dv8tion.jda.api.entities.SelfMember;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.managers.AudioManager;
import net.dv8tion.jda.api.requests.restaction.MessageEditAction;
import net.dv8tion.jda.api.requests.restaction.interactions.MessageEditCallbackAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SearchCmdTest
{
    private Bot bot;
    private BotConfig config;
    private CommandEvent event;
    private CommandClient client;
    private Guild guild;
    private Member member;
    private User user;
    private SelfMember selfMember;
    private TextChannel channel;
    private PlayerManager playerManager;
    private EventWaiter waiter;
    private AudioHandler audioHandler;
    private Message message;
    private MessageEditAction editAction;

    @BeforeEach
    void setUp()
    {
        bot = mock(Bot.class);
        config = mock(BotConfig.class);
        event = mock(CommandEvent.class);
        client = mock(CommandClient.class);
        guild = mock(Guild.class);
        member = mock(Member.class);
        user = mock(User.class);
        selfMember = mock(SelfMember.class);
        channel = mock(TextChannel.class);
        playerManager = mock(PlayerManager.class);
        waiter = mock(EventWaiter.class);
        audioHandler = mock(AudioHandler.class);
        message = mock(Message.class);
        editAction = mock(MessageEditAction.class);
        AudioManager audioManager = mock(AudioManager.class);

        when(bot.getConfig()).thenReturn(config);
        when(bot.getPlayerManager()).thenReturn(playerManager);
        when(bot.getAudioLoadWrapper()).thenReturn(AudioLoadWrapper.NO_OP);
        when(bot.getWaiter()).thenReturn(waiter);
        when(config.getSearching()).thenReturn("search");
        when(config.getAliases(anyString())).thenReturn(new String[0]);
        when(config.getSuccess()).thenReturn("OK");
        when(config.getWarning()).thenReturn("WARN");
        when(config.getError()).thenReturn("ERROR");
        when(event.getArgs()).thenReturn("query");
        when(event.getAuthor()).thenReturn(user);
        when(event.getGuild()).thenReturn(guild);
        when(event.getMember()).thenReturn(member);
        when(event.getChannel()).thenReturn(channel);
        when(event.getClient()).thenReturn(client);
        when(event.getSelfMember()).thenReturn(selfMember);
        when(user.getId()).thenReturn("42");
        when(user.getIdLong()).thenReturn(42L);
        when(channel.getIdLong()).thenReturn(77L);
        when(guild.getAudioManager()).thenReturn(audioManager);
        when(audioManager.getSendingHandler()).thenReturn(audioHandler);
        when(selfMember.getColors()).thenReturn(RoleColors.DEFAULT);
        when(message.editMessageEmbeds(any(MessageEmbed[].class))).thenReturn(editAction);
        when(editAction.setContent(anyString())).thenReturn(editAction);
        when(editAction.setComponents(any(MessageTopLevelComponent.class))).thenReturn(editAction);
        doAnswer(invocation ->
        {
            Consumer<Message> callback = invocation.getArgument(0);
            callback.accept(mock(Message.class));
            return null;
        }).when(editAction).queue(any(Consumer.class));
    }

    @Test
    void searchResultsOpenSelectMenuAndSelectionAddsTrack()
    {
        SearchCmd command = new SearchCmd(bot);
        AudioTrack track = track("Song");
        AudioPlaylist playlist = mock(AudioPlaylist.class);
        when(playlist.getTracks()).thenReturn(List.of(track));
        when(playlist.isSearchResult()).thenReturn(false);
        when(playlist.getSelectedTrack()).thenReturn(null);
        doAnswer(invocation ->
        {
            Consumer<Message> callback = invocation.getArgument(1);
            callback.accept(message);
            return null;
        }).when(event).reply(anyString(), any(Consumer.class));
        ArgumentCaptor<AudioLoadResultHandler> loadHandler = ArgumentCaptor.forClass(AudioLoadResultHandler.class);
        ArgumentCaptor<Consumer<StringSelectInteractionEvent>> selectHandler = ArgumentCaptor.forClass(Consumer.class);

        command.doCommand(event);
        verify(playerManager).loadItemOrdered(eq(guild), eq("ytsearch:query"), loadHandler.capture());
        loadHandler.getValue().playlistLoaded(playlist);

        verify(message).editMessageEmbeds(any(MessageEmbed[].class));
        verify(waiter).waitForEvent(eq(StringSelectInteractionEvent.class), any(), selectHandler.capture(),
                eq(1L), eq(TimeUnit.MINUTES), any());

        StringSelectInteractionEvent selectEvent = mock(StringSelectInteractionEvent.class);
        MessageEditCallbackAction selectEditAction = mock(MessageEditCallbackAction.class);
        when(selectEvent.getValues()).thenReturn(List.of("0"));
        when(selectEvent.editMessage(anyString())).thenReturn(selectEditAction);
        when(selectEditAction.setEmbeds(any(MessageEmbed[].class))).thenReturn(selectEditAction);
        when(selectEditAction.setComponents(any(MessageTopLevelComponent[].class))).thenReturn(selectEditAction);

        selectHandler.getValue().accept(selectEvent);

        verify(selectEvent).editMessage(contains("Added"));
    }

    private AudioTrack track(String title)
    {
        AudioTrack track = mock(AudioTrack.class);
        when(track.getInfo()).thenReturn(new AudioTrackInfo(title, "Author", 1000, "id-" + title, false,
                "https://example.com/" + title));
        when(track.getDuration()).thenReturn(1000L);
        return track;
    }
}
