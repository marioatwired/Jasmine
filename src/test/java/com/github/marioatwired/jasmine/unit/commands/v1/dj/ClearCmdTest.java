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
import com.github.marioatwired.jasmine.commands.v1.dj.ClearCmd;
import com.github.marioatwired.jasmine.service.MusicService;
import com.github.marioatwired.jasmine.service.OutputAdapter;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("ClearCmd Tests")
class ClearCmdTest
{
    private Bot bot;
    private MusicService musicService;
    private ClearCmd command;

    @BeforeEach
    void setUp()
    {
        bot = mock(Bot.class);
        BotConfig config = mock(BotConfig.class);
        musicService = mock(MusicService.class);
        when(bot.getConfig()).thenReturn(config);
        when(bot.getMusicService()).thenReturn(musicService);
        when(config.getAliases(anyString())).thenReturn(new String[0]);
        command = new ClearCmd(bot);
    }

    @Test
    @DisplayName("is a DJ command that clears the queue")
    void isDjQueueCommand()
    {
        assertEquals("clear", command.getName());
        assertEquals("DJ", command.getCategory().getName());
        assertTrue(command.getHelp().contains("song queue"));
    }

    @Test
    @DisplayName("delegates queue clearing to MusicService")
    void delegatesToMusicService()
    {
        CommandEvent event = mock(CommandEvent.class);
        Guild guild = mock(Guild.class);
        Member member = mock(Member.class);
        when(event.getGuild()).thenReturn(guild);
        when(event.getMember()).thenReturn(member);

        command.doCommand(event);

        verify(musicService).clearQueue(any(Guild.class), any(Member.class), any(OutputAdapter.class));
    }
}
