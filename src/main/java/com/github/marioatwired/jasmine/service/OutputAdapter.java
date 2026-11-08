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
package com.github.marioatwired.jasmine.service;

import com.github.marioatwired.jasmine.audio.AudioHandler;
import net.dv8tion.jda.api.entities.Message;

import java.util.function.Consumer;

/**
 * Adapter interface for abstracting output operations.
 * <p>
 * This interface allows services to be command-type agnostic - the same service
 * methods work for text commands, slash commands, and button interactions.
 * Each command type provides its own implementation.
 *
 * @see com.github.marioatwired.jasmine.commands.BaseOutputAdapter
 * @see com.github.marioatwired.jasmine.commands.v1.TextOutputAdapters
 * @see com.github.marioatwired.jasmine.commands.v2.SlashOutputAdapters
 */
public interface OutputAdapter
{
    void replySuccess(String content);
    void replyError(String content);
    void replyWarning(String content);
    void editMessage(String content);
    void editMessage(String content, Consumer<Message> onSuccess);
    void editNowPlaying(AudioHandler handler);
    void editNoMusic(AudioHandler handler);
    void onShowHelp();
}
