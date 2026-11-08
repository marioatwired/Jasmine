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

import com.github.marioatwired.jasmine.settings.QueueType;
import com.github.marioatwired.jasmine.settings.RepeatMode;

/**
 * Data class containing queue information for display.
 */
public class QueueInfo
{
    public final String[] tracks;
    public final long totalDuration;
    public final String nowPlayingTitle;
    public final String statusEmoji;
    public final RepeatMode repeatMode;
    public final QueueType queueType;
    public final Object nowPlayingMessage;
    public final Object noMusicMessage;

    public QueueInfo(String[] tracks, long totalDuration, String nowPlayingTitle, String statusEmoji,
                     RepeatMode repeatMode, QueueType queueType, Object nowPlayingMessage, Object noMusicMessage)
    {
        this.tracks = tracks;
        this.totalDuration = totalDuration;
        this.nowPlayingTitle = nowPlayingTitle;
        this.statusEmoji = statusEmoji;
        this.repeatMode = repeatMode;
        this.queueType = queueType;
        this.nowPlayingMessage = nowPlayingMessage;
        this.noMusicMessage = noMusicMessage;
    }

    public boolean isEmpty()
    {
        return tracks.length == 0;
    }
}
