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
package com.github.marioatwired.jasmine.unit.commands.v2.music;

import com.github.marioatwired.jasmine.commands.v2.music.PlaylistSlashCmd;
import com.github.marioatwired.jasmine.testutil.commands.SlashCommandTestFixture;
import com.jagrosh.jdautilities.command.SlashCommand;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("PlaylistSlashCmd Tests")
class PlaylistSlashCmdTest
{
    private PlaylistSlashCmd command;
    private Map<String, SlashCommand> children;

    @BeforeEach
    void setUp()
    {
        SlashCommandTestFixture fixture = SlashCommandTestFixture.create();
        command = new PlaylistSlashCmd(fixture.getBot());
        children = Arrays.stream(command.getChildren())
                .collect(Collectors.toMap(SlashCommand::getName, Function.identity()));
    }

    @Test
    @DisplayName("exposes view and editing subcommands")
    void exposesExpectedSubcommands()
    {
        assertEquals("playlist", command.getName());
        assertEquals(5, children.size());
        assertTrue(children.keySet().containsAll(
                java.util.List.of("view", "append", "remove", "move", "replace")));
    }

    @Test
    @DisplayName("builds Discord subcommand data")
    void buildsSubcommandData()
    {
        SlashCommandData data = (SlashCommandData) command.buildCommandData();

        assertEquals(5, data.getSubcommands().size());
        assertTrue(data.getSubcommands().stream()
                .map(subcommand -> subcommand.getName())
                .toList()
                .containsAll(java.util.List.of("view", "append", "remove", "move", "replace")));
    }

    @Test
    @DisplayName("editing subcommands are not owner-only")
    void editingSubcommandsAreNotOwnerOnly()
    {
        for (String name : new String[]{"view", "append", "remove", "move", "replace"})
        {
            SlashCommand child = children.get(name);
            assertNotNull(child);
            assertFalse(child.isOwnerCommand());
        }
    }

    @Test
    @DisplayName("view accepts a required name and optional page")
    void viewOptionsAreCorrect()
    {
        SlashCommand view = children.get("view");
        OptionData name = option(view, "name");
        OptionData page = option(view, "page");

        assertEquals(OptionType.STRING, name.getType());
        assertTrue(name.isRequired());
        assertEquals(OptionType.INTEGER, page.getType());
        assertFalse(page.isRequired());
    }

    @Test
    @DisplayName("editing subcommands expose their required options")
    void editingOptionsAreCorrect()
    {
        assertTrue(option(children.get("append"), "urls").isRequired());
        assertTrue(option(children.get("remove"), "position").isRequired());
        assertTrue(option(children.get("move"), "from").isRequired());
        assertTrue(option(children.get("move"), "to").isRequired());
        assertTrue(option(children.get("replace"), "url").isRequired());
    }

    private OptionData option(SlashCommand command, String name)
    {
        return command.getOptions().stream()
                .filter(option -> option.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing option: " + name));
    }
}
