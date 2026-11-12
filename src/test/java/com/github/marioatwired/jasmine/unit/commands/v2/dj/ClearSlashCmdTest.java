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
package com.github.marioatwired.jasmine.unit.commands.v2.dj;

import com.github.marioatwired.jasmine.commands.v2.dj.ClearSlashCmd;
import com.github.marioatwired.jasmine.service.MusicService;
import com.github.marioatwired.jasmine.service.OutputAdapter;
import com.github.marioatwired.jasmine.testutil.commands.SlashCommandTestFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@DisplayName("ClearSlashCmd Tests")
class ClearSlashCmdTest
{
    private SlashCommandTestFixture fixture;
    private ClearSlashCmd command;

    @BeforeEach
    void setUp()
    {
        fixture = SlashCommandTestFixture.create();
        command = new ClearSlashCmd(fixture.getBot());
    }

    @Test
    @DisplayName("is a DJ command with no options")
    void isDjQueueCommand()
    {
        assertEquals("clear", command.getName());
        assertEquals("DJ", command.getCategory().getName());
        assertTrue(command.getHelp().contains("song queue"));
        assertTrue(command.getOptions().isEmpty());
    }

    @Test
    @DisplayName("delegates queue clearing to MusicService")
    void delegatesToMusicService()
    {
        command.doDJCommand(fixture.getEvent());

        verify(fixture.getMusicService()).clearQueue(
                any(),
                any(),
                any(OutputAdapter.class)
        );
    }
}
