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
package com.github.marioatwired.jasmine.playlist;

import java.util.ArrayList;
import java.util.List;

/**
 * Parses user-entered playlist item lists consistently across text and slash
 * commands.
 */
public final class PlaylistInputParser
{
    private PlaylistInputParser()
    {
    }

    /**
     * Splits a pipe-separated list, trims each entry, strips Discord's
     * angle-bracket URL wrapping, and ignores empty entries.
     *
     * @param input raw user input, possibly {@code null}
     * @return immutable list of non-empty entries in input order
     */
    public static List<String> parsePipeSeparated(String input)
    {
        if (input == null)
        {
            return List.of();
        }

        List<String> items = new ArrayList<>();
        for (String value : input.split("\\|"))
        {
            String item = value.trim();
            if (item.startsWith("<") && item.endsWith(">"))
            {
                item = item.substring(1, item.length() - 1).trim();
            }
            if (!item.isEmpty())
            {
                items.add(item);
            }
        }
        return List.copyOf(items);
    }
}
