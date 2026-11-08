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
 * Parses playlist text files and applies the shuffle directive.
 */
final class PlaylistParser
{
    private PlaylistParser()
    {
    }

    static ParsedPlaylist parse(List<String> rawLines, boolean includeShuffleDirective)
    {
        boolean shuffle = false;
        List<String> items = new ArrayList<>();
        for (String line : rawLines)
        {
            String value = line.trim();
            if (value.isEmpty())
            {
                continue;
            }
            if (value.startsWith("#") || value.startsWith("//"))
            {
                if (includeShuffleDirective)
                {
                    String compact = value.replaceAll("\\s+", "");
                    if (compact.equalsIgnoreCase("#shuffle") || compact.equalsIgnoreCase("//shuffle"))
                    {
                        shuffle = true;
                    }
                }
                continue;
            }
            items.add(value);
        }
        return new ParsedPlaylist(items, shuffle);
    }

    static <T> void shuffle(List<T> list)
    {
        for (int first = 0; first < list.size(); first++)
        {
            int second = (int) (Math.random() * list.size());
            T tmp = list.get(first);
            list.set(first, list.get(second));
            list.set(second, tmp);
        }
    }
}

record ParsedPlaylist(List<String> items, boolean shuffle)
{
}
