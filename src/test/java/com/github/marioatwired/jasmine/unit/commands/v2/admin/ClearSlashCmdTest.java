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
package com.github.marioatwired.jasmine.unit.commands.v2.admin;

import com.github.marioatwired.jasmine.commands.v2.admin.ClearSlashCmd;
import com.github.marioatwired.jasmine.testutil.commands.SlashCommandTestFixture;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("ClearSlashCmd Tests")
class ClearSlashCmdTest
{
    private ClearSlashCmd command;

    @BeforeEach
    void setUp()
    {
        command = new ClearSlashCmd(SlashCommandTestFixture.create().getBot());
    }

    @Test
    @DisplayName("builds a clear command with an optional amount")
    void buildsCommandData()
    {
        SlashCommandData data = (SlashCommandData) command.buildCommandData();
        OptionData amount = data.getOptions().get(0);

        assertEquals("clear", command.getName());
        assertEquals(1, data.getOptions().size());
        assertEquals("amount", amount.getName());
        assertEquals(OptionType.INTEGER, amount.getType());
        assertFalse(amount.isRequired());
        assertEquals(1L, amount.getMinValue().longValue());
        assertEquals(1000L, amount.getMaxValue().longValue());
        assertTrue(command.getHelp().contains("current channel"));
    }
}
