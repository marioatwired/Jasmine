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
package com.github.marioatwired.jasmine.unit.commands.v1.owner;

import com.github.marioatwired.jasmine.Bot;
import com.github.marioatwired.jasmine.BotConfig;
import com.github.marioatwired.jasmine.commands.v1.owner.PlaylistCmd;
import com.jagrosh.jdautilities.command.Command;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("PlaylistCmd Tests")
class PlaylistCmdTest
{
    private PlaylistCmd command;
    private Map<String, Command> children;

    @BeforeEach
    void setUp()
    {
        Bot bot = mock(Bot.class);
        BotConfig config = mock(BotConfig.class);
        when(bot.getConfig()).thenReturn(config);
        when(config.getAliases(anyString())).thenReturn(new String[0]);

        command = new PlaylistCmd(bot);
        children = Arrays.stream(command.getChildren())
                .collect(Collectors.toMap(Command::getName, Function.identity()));
    }

    @Test
    @DisplayName("playlist parent is not owner-only")
    void parentIsNotOwnerOnly()
    {
        assertFalse(command.isOwnerCommand());
    }

    @Test
    @DisplayName("view and editing subcommands are available to DJs")
    void editingSubcommandsAreNotOwnerOnly()
    {
        for (String name : new String[]{"all", "view", "append", "addcurrent",
                "rename", "remove", "move", "replace"})
        {
            Command child = children.get(name);
            assertNotNull(child, "Missing subcommand: " + name);
            assertFalse(child.isOwnerCommand(), name + " should not be owner-only");
        }
    }

    @Test
    @DisplayName("destructive management subcommands remain owner-only")
    void managementSubcommandsRemainOwnerOnly()
    {
        for (String name : new String[]{"make", "delete", "export", "import", "setdefault"})
        {
            Command child = children.get(name);
            assertNotNull(child, "Missing subcommand: " + name);
            assertTrue(child.isOwnerCommand(), name + " should be owner-only");
        }
    }

    @Test
    @DisplayName("append accepts the legacy amend/ammend spellings")
    void appendAcceptsLegacyAliases()
    {
        Command append = children.get("append");
        assertNotNull(append);
        assertTrue(Arrays.asList(append.getAliases()).contains("amend"));
        assertTrue(Arrays.asList(append.getAliases()).contains("ammend"));
    }
}
