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
package com.github.marioatwired.jasmine.unit.playlist;

import com.github.marioatwired.jasmine.playlist.PlaylistInputParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("PlaylistInputParser Tests")
class PlaylistInputParserTest
{
    @Test
    @DisplayName("parsePipeSeparated() returns an empty list for null")
    void nullInputReturnsEmptyList()
    {
        assertTrue(PlaylistInputParser.parsePipeSeparated(null).isEmpty());
    }

    @Test
    @DisplayName("parsePipeSeparated() trims and filters empty entries")
    void trimsAndFiltersEmptyEntries()
    {
        assertEquals(List.of("url1", "url2"),
                PlaylistInputParser.parsePipeSeparated(" url1 |  | url2 | "));
    }

    @Test
    @DisplayName("parsePipeSeparated() strips Discord angle brackets")
    void stripsAngleBrackets()
    {
        assertEquals(List.of("https://example.com/one", "https://example.com/two"),
                PlaylistInputParser.parsePipeSeparated(
                        "<https://example.com/one> | <https://example.com/two>"));
    }

    @Test
    @DisplayName("parsePipeSeparated() preserves order and duplicates")
    void preservesOrderAndDuplicates()
    {
        assertEquals(List.of("first", "second", "first"),
                PlaylistInputParser.parsePipeSeparated("first|second|first"));
    }
}
