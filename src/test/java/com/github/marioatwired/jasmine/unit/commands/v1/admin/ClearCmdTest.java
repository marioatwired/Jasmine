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
package com.github.marioatwired.jasmine.unit.commands.v1.admin;

import com.github.marioatwired.jasmine.Bot;
import com.github.marioatwired.jasmine.BotConfig;
import com.github.marioatwired.jasmine.commands.v1.admin.ClearCmd;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("ClearCmd Tests")
class ClearCmdTest
{
    private ClearCmd command;

    @BeforeEach
    void setUp()
    {
        Bot bot = mock(Bot.class);
        BotConfig config = mock(BotConfig.class);
        when(bot.getConfig()).thenReturn(config);
        when(config.getAliases(anyString())).thenReturn(new String[0]);
        command = new ClearCmd(bot);
    }

    @Test
    @DisplayName("is a guild-only admin command")
    void isGuildOnlyAdminCommand()
    {
        assertEquals("clear", command.getName());
        assertTrue(command.isGuildOnly());
        assertEquals("Admin", command.getCategory().getName());
    }

    @Test
    @DisplayName("documents the optional amount argument")
    void documentsAmountArgument()
    {
        assertEquals("[amount]", command.getArguments());
        assertTrue(command.getHelp().contains("current channel"));
    }
}
