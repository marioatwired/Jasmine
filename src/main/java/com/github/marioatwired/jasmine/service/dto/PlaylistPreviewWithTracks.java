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

/**
 * Result of loading playlist preview with resolved track lines (duration + title, same style as Queue/History).
 */
public class PlaylistPreviewWithTracks
{
    public final String playlistName;
    public final int totalItems;
    public final boolean hasMore;
    public final List<String> formattedLines;

    public PlaylistPreviewWithTracks(String playlistName, int totalItems, boolean hasMore, List<String> formattedLines)
    {
        this.playlistName = playlistName;
        this.totalItems = totalItems;
        this.hasMore = hasMore;
        this.formattedLines = formattedLines;
    }
}
