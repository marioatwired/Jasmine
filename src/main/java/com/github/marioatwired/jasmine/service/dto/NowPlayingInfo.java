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

import net.dv8tion.jda.api.utils.messages.MessageCreateData;

/**
 * Data class containing now playing information.
 */
public class NowPlayingInfo
{
    public final MessageCreateData nowPlayingMessage;
    public final MessageCreateData noMusicMessage;
    public final boolean isPlaying;

    public NowPlayingInfo(MessageCreateData nowPlayingMessage,
                          MessageCreateData noMusicMessage,
                          boolean isPlaying)
    {
        this.nowPlayingMessage = nowPlayingMessage;
        this.noMusicMessage = noMusicMessage;
        this.isPlaying = isPlaying;
    }
}
