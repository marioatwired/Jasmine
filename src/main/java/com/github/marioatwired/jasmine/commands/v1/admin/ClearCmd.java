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
package com.github.marioatwired.jasmine.commands.v1.admin;

import com.jagrosh.jdautilities.command.CommandEvent;
import com.github.marioatwired.jasmine.Bot;
import com.github.marioatwired.jasmine.commands.v1.AdminCommand;
import com.github.marioatwired.jasmine.utils.ChannelClearHelper;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

/**
 * Clears messages in the channel where the command is used.
 */
public class ClearCmd extends AdminCommand
{
    private static final int MAX_AMOUNT = 1000;

    private final Bot bot;

    public ClearCmd(Bot bot)
    {
        this.bot = bot;
        this.name = "clear";
        this.help = "clears messages in the current channel";
        this.arguments = "[amount]";
        this.guildOnly = true;
        this.aliases = bot.getConfig().getAliases(this.name);
    }

    @Override
    protected void execute(CommandEvent event)
    {
        TextChannel channel = event.getTextChannel();
        if (channel == null)
        {
            event.replyError("This command can only be used in a text channel.");
            return;
        }
        if (!event.getSelfMember().hasPermission(channel, Permission.MESSAGE_MANAGE))
        {
            event.replyError("I don't have permission to manage messages in this channel.");
            return;
        }

        String rawAmount = event.getArgs().trim();
        int amount;
        if (rawAmount.isEmpty())
        {
            amount = bot.getConfig().getClearChannelDeleteLimit();
        }
        else
        {
            try
            {
                amount = Integer.parseInt(rawAmount);
            }
            catch (NumberFormatException ex)
            {
                event.replyError("Please provide a number between 1 and " + MAX_AMOUNT + ".");
                return;
            }
            if (amount < 1 || amount > MAX_AMOUNT)
            {
                event.replyError("Please provide a number between 1 and " + MAX_AMOUNT + ".");
                return;
            }
        }

        ChannelClearHelper.ClearPolicy policy = ChannelClearHelper.ClearPolicy.of(
                amount,
                bot.getConfig().getClearChannelAgeDays()
        );
        event.reply(event.getClient().getWarning() + " Clearing messages...");
        ChannelClearHelper.purgeChannel(channel, new ChannelClearHelper.PurgeCallback()
        {
            @Override
            public void onCleared(int count)
            {
                channel.sendMessage(event.getClient().getSuccess()
                        + " Cleared " + count + " messages.").queue();
            }

            @Override
            public void onError(Throwable t)
            {
                String message = t.getMessage() != null ? t.getMessage() : t.toString();
                channel.sendMessage(event.getClient().getError() + " Error: " + message).queue();
            }
        }, policy);
    }
}
