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
package com.github.marioatwired.jasmine.unit.commands;

import com.github.marioatwired.jasmine.commands.PlaylistCommandMessages;
import com.github.marioatwired.jasmine.service.dto.PlaylistMutationResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("PlaylistCommandMessages Tests")
class PlaylistCommandMessagesTest
{
    @Test
    @DisplayName("appendSummary() uses singular wording")
    void appendSummaryUsesSingularWording()
    {
        PlaylistMutationResult result = PlaylistMutationResult.appendSuccess(3, 1, 0);

        assertEquals("Added 1 item to playlist `mix`.",
                PlaylistCommandMessages.appendSummary("mix", result));
    }

    @Test
    @DisplayName("appendSummary() reports skipped duplicates")
    void appendSummaryReportsSkippedDuplicates()
    {
        PlaylistMutationResult result = PlaylistMutationResult.appendSuccess(4, 2, 1);

        assertEquals("Added 2 items to playlist `mix`. 1 duplicate skipped.",
                PlaylistCommandMessages.appendSummary("mix", result));
    }

    @Test
    @DisplayName("allDuplicates() handles one duplicate")
    void allDuplicatesHandlesOneDuplicate()
    {
        assertEquals("The item is already in playlist `mix`.",
                PlaylistCommandMessages.allDuplicates("mix", 1));
    }

    @Test
    @DisplayName("allDuplicates() handles several duplicates")
    void allDuplicatesHandlesSeveralDuplicates()
    {
        assertEquals("All 3 items are already in playlist `mix`.",
                PlaylistCommandMessages.allDuplicates("mix", 3));
    }
}
