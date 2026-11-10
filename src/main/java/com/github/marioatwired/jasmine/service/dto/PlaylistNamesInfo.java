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
package com.github.marioatwired.jasmine.service.dto;

import java.util.List;
import java.util.Map;

/**
 * Result for listing playlists where storage errors are explicit.
 */
public class PlaylistNamesInfo
{
    public final List<String> names;
    public final Map<String, Integer> itemCounts;
    public final String errorMessage;

    private PlaylistNamesInfo(List<String> names, Map<String, Integer> itemCounts, String errorMessage)
    {
        this.names = names;
        this.itemCounts = itemCounts;
        this.errorMessage = errorMessage;
    }

    public static PlaylistNamesInfo success(List<String> names)
    {
        return success(names, Map.of());
    }

    public static PlaylistNamesInfo success(List<String> names, Map<String, Integer> itemCounts)
    {
        return new PlaylistNamesInfo(names, itemCounts, null);
    }

    public static PlaylistNamesInfo error(String errorMessage)
    {
        return new PlaylistNamesInfo(List.of(), Map.of(), errorMessage);
    }

    public boolean hasError()
    {
        return errorMessage != null;
    }

    public Integer itemCount(String name)
    {
        return itemCounts.get(name);
    }
}
