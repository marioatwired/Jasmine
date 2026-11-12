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
package com.github.marioatwired.jasmine.commands.v2.dj;

import com.github.marioatwired.jasmine.Bot;
import com.github.marioatwired.jasmine.commands.v2.DJSlashCommand;
import com.github.marioatwired.jasmine.commands.v2.SlashOutputAdapters.SlashEventOutputAdapter;
import com.jagrosh.jdautilities.command.SlashCommandEvent;

/**
 * Clears pending tracks without stopping the current track.
 */
public class ClearSlashCmd extends DJSlashCommand
{
    public ClearSlashCmd(Bot bot)
    {
        super(bot);
        this.name = "clear";
        this.help = "clears the song queue without stopping the current track";
        this.bePlaying = false;
    }

    @Override
    public void doDJCommand(SlashCommandEvent event)
    {
        bot.getMusicService().clearQueue(
                event.getGuild(),
                event.getMember(),
                new SlashEventOutputAdapter(event)
        );
    }
}
