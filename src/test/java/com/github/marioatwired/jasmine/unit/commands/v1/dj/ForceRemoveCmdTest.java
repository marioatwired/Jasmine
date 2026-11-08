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
package com.github.marioatwired.jasmine.unit.commands.v1.dj;

import com.github.marioatwired.jasmine.Bot;
import com.github.marioatwired.jasmine.BotConfig;
import com.github.marioatwired.jasmine.commands.v1.dj.ForceRemoveCmd;
import com.github.marioatwired.jasmine.service.MusicService;
import com.jagrosh.jdautilities.command.CommandClient;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.utils.FinderUtil;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import net.dv8tion.jda.api.components.MessageTopLevelComponent;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import net.dv8tion.jda.api.requests.restaction.interactions.MessageEditCallbackAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ForceRemoveCmdTest
{
    private Bot bot;
    private BotConfig config;
    private MusicService musicService;
    private CommandEvent event;
    private CommandClient client;
    private Guild guild;
    private User author;
    private TextChannel channel;
    private EventWaiter waiter;
    private Member alice;
    private Member bob;
    private User aliceUser;
    private User bobUser;

    @BeforeEach
    void setUp()
    {
        bot = mock(Bot.class);
        config = mock(BotConfig.class);
        musicService = mock(MusicService.class);
        event = mock(CommandEvent.class);
        client = mock(CommandClient.class);
        guild = mock(Guild.class);
        author = mock(User.class);
        channel = mock(TextChannel.class);
        waiter = mock(EventWaiter.class);
        alice = mock(Member.class);
        bob = mock(Member.class);
        aliceUser = mock(User.class);
        bobUser = mock(User.class);

        when(bot.getConfig()).thenReturn(config);
        when(bot.getMusicService()).thenReturn(musicService);
        when(bot.getWaiter()).thenReturn(waiter);
        when(config.getAliases(anyString())).thenReturn(new String[0]);
        when(event.getArgs()).thenReturn("someone");
        when(event.getAuthor()).thenReturn(author);
        when(event.getClient()).thenReturn(client);
        when(event.getGuild()).thenReturn(guild);
        when(event.getChannel()).thenReturn(channel);
        when(author.getId()).thenReturn("42");
        when(client.getError()).thenReturn("ERROR");
        when(musicService.isQueueEmpty(guild)).thenReturn(false);
        when(alice.getIdLong()).thenReturn(1L);
        when(alice.getUser()).thenReturn(aliceUser);
        when(bob.getIdLong()).thenReturn(2L);
        when(bob.getUser()).thenReturn(bobUser);
        when(aliceUser.getName()).thenReturn("Alice");
        when(aliceUser.getDiscriminator()).thenReturn("0000");
        when(aliceUser.getIdLong()).thenReturn(1L);
        when(bobUser.getName()).thenReturn("Bob");
        when(bobUser.getDiscriminator()).thenReturn("0000");
        when(bobUser.getIdLong()).thenReturn(2L);
    }

    @Test
    void multipleMatchesOpenSelectMenuAndRemoveSelectedUser()
    {
        ForceRemoveCmd command = new ForceRemoveCmd(bot);
        MessageCreateAction sendAction = mock(MessageCreateAction.class);
        MessageEditCallbackAction editAction = mock(MessageEditCallbackAction.class);
        when(channel.sendMessage("Found multiple users:")).thenReturn(sendAction);
        when(sendAction.setComponents(any(MessageTopLevelComponent[].class))).thenReturn(sendAction);
        doAnswer(invocation ->
        {
            Consumer<net.dv8tion.jda.api.entities.Message> callback = invocation.getArgument(0);
            callback.accept(mock(net.dv8tion.jda.api.entities.Message.class));
            return null;
        }).when(sendAction).queue(any(Consumer.class));

        ArgumentCaptor<Consumer<StringSelectInteractionEvent>> selection =
                ArgumentCaptor.forClass(Consumer.class);

        try (MockedStatic<FinderUtil> finder = mockStatic(FinderUtil.class))
        {
            finder.when(() -> FinderUtil.findMembers("someone", guild)).thenReturn(List.of(alice, bob));

            command.doCommand(event);

            verify(sendAction).setComponents(any(MessageTopLevelComponent[].class));
            verify(waiter).waitForEvent(eq(StringSelectInteractionEvent.class), any(), selection.capture(),
                    eq(1L), eq(TimeUnit.MINUTES), any());
        }

        StringSelectInteractionEvent selectEvent = mock(StringSelectInteractionEvent.class);
        when(selectEvent.getValues()).thenReturn(List.of("1"));
        when(selectEvent.editMessage(anyString())).thenReturn(editAction);
        when(editAction.setComponents(any(MessageTopLevelComponent[].class))).thenReturn(editAction);
        when(musicService.removeAllTracksByUser(guild, 1L)).thenReturn(2);

        selection.getValue().accept(selectEvent);

        verify(selectEvent).editMessage(contains("Alice"));
        verify(musicService).removeAllTracksByUser(guild, 1L);
        verify(event).replySuccess(contains("2"));
    }
}
