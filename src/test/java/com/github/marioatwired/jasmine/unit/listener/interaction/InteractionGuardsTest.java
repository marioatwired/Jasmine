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
package com.github.marioatwired.jasmine.unit.listener.interaction;

import com.github.marioatwired.jasmine.listener.interaction.InteractionGuards;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("InteractionGuards Tests")
class InteractionGuardsTest
{
    private ReplyCallbackAction replyAction;

    @BeforeEach
    void setUp()
    {
        replyAction = mock(ReplyCallbackAction.class);
        when(replyAction.setEphemeral(anyBoolean())).thenReturn(replyAction);
        doNothing().when(replyAction).queue();
    }

    @Test
    @DisplayName("requireSameUser() accepts the invoking user")
    void buttonSameUserReturnsTrue()
    {
        ButtonInteractionEvent event = buttonEvent(42L);

        assertTrue(InteractionGuards.requireSameUser(event, 42L));
        verify(event, never()).reply(anyString());
    }

    @Test
    @DisplayName("requireSameUser() rejects another user")
    void buttonDifferentUserRepliesAndReturnsFalse()
    {
        ButtonInteractionEvent event = buttonEvent(42L);

        assertFalse(InteractionGuards.requireSameUser(event, 7L));
        verify(event).reply("Only the user who ran the command can use this interaction!");
        verify(replyAction).setEphemeral(true);
        verify(replyAction).queue();
    }

    @Test
    @DisplayName("requireSameUser() supports a custom rejection message")
    void buttonUsesCustomMessage()
    {
        ButtonInteractionEvent event = buttonEvent(42L);

        assertFalse(InteractionGuards.requireSameUser(event, 7L,
                "Only the invoker can use this panel."));
        verify(event).reply("Only the invoker can use this panel.");
    }

    @Test
    @DisplayName("requireSameUser() also guards string-select interactions")
    void stringSelectDifferentUserRepliesAndReturnsFalse()
    {
        StringSelectInteractionEvent event = mock(StringSelectInteractionEvent.class);
        User user = mock(User.class);
        when(user.getIdLong()).thenReturn(42L);
        when(event.getUser()).thenReturn(user);
        when(event.reply(anyString())).thenReturn(replyAction);

        assertFalse(InteractionGuards.requireSameUser(event, 7L));
        verify(event).reply("Only the user who ran the command can use this interaction!");
    }

    private ButtonInteractionEvent buttonEvent(long userId)
    {
        ButtonInteractionEvent event = mock(ButtonInteractionEvent.class);
        User user = mock(User.class);
        when(user.getIdLong()).thenReturn(userId);
        when(event.getUser()).thenReturn(user);
        when(event.reply(anyString())).thenReturn(replyAction);
        return event;
    }
}
