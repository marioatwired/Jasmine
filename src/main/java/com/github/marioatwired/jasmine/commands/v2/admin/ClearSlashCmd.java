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
package com.github.marioatwired.jasmine.commands.v2.admin;

import com.jagrosh.jdautilities.command.SlashCommandEvent;
import com.github.marioatwired.jasmine.Bot;
import com.github.marioatwired.jasmine.commands.v2.AdminSlashCommand;
import com.github.marioatwired.jasmine.utils.ChannelClearHelper;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.List;

/**
 * Clears messages in the channel where the command is used.
 */
public class ClearSlashCmd extends AdminSlashCommand
{
    private static final int MAX_AMOUNT = 1000;

    public ClearSlashCmd(Bot bot)
    {
        super(bot);
        this.name = "clear";
        this.help = "clears messages in the current channel";
        this.contexts = new InteractionContextType[]{InteractionContextType.GUILD};
        this.options = List.of(
                new OptionData(OptionType.INTEGER, "amount",
                        "number of messages to delete (default: configured limit)", false)
                        .setMinValue(1)
                        .setMaxValue(MAX_AMOUNT)
        );
    }

    @Override
    public void doAdminCommand(SlashCommandEvent event)
    {
        if (event.getGuild() == null)
        {
            event.reply(event.getClient().getError() + " This command can only be used in a server.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        TextChannel channel = event.getTextChannel();
        if (channel == null)
        {
            event.reply(event.getClient().getError() + " This command can only be used in a text channel.")
                    .setEphemeral(true)
                    .queue();
            return;
        }
        if (!event.getGuild().getSelfMember().hasPermission(channel, Permission.MESSAGE_MANAGE))
        {
            event.reply(event.getClient().getError()
                            + " I don't have permission to manage messages in this channel.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        int amount = event.getOption("amount") != null
                ? (int) event.getOption("amount").getAsLong()
                : bot.getConfig().getClearChannelDeleteLimit();
        ChannelClearHelper.ClearPolicy policy = ChannelClearHelper.ClearPolicy.of(
                amount,
                bot.getConfig().getClearChannelAgeDays()
        );

        event.deferReply(true).queue(hook ->
                ChannelClearHelper.purgeChannel(channel, new ChannelClearHelper.PurgeCallback()
                {
                    @Override
                    public void onCleared(int count)
                    {
                        hook.editOriginal("Cleared " + count + " messages.").queue();
                    }

                    @Override
                    public void onError(Throwable t)
                    {
                        String message = t.getMessage() != null ? t.getMessage() : t.toString();
                        hook.editOriginal(event.getClient().getError() + " Error: " + message).queue();
                    }
                }, policy));
    }
}
