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

/**
 * Data class containing playback history information for display.
 */
public class HistoryInfo
{
    public final String[] tracks;
    public final long totalDuration;
    public final int maxSize;
    public final boolean disabled;

    public HistoryInfo(String[] tracks, long totalDuration, int maxSize)
    {
        this(tracks, totalDuration, maxSize, maxSize == 0);
    }

    public HistoryInfo(String[] tracks, long totalDuration, int maxSize, boolean disabled)
    {
        this.tracks = tracks;
        this.totalDuration = totalDuration;
        this.maxSize = maxSize;
        this.disabled = disabled;
    }

    public boolean isEmpty()
    {
        return tracks.length == 0;
    }

    public boolean isDisabled()
    {
        return disabled;
    }
}
