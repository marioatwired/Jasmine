/*
 * Copyright 2019 John Grosh <john.a.grosh@gmail.com>.
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
package com.github.marioatwired.jasmine.commands.v1.dj;

import com.github.marioatwired.jasmine.Bot;
import com.github.marioatwired.jasmine.commands.v1.DJCommand;
import com.github.marioatwired.jasmine.service.MusicService;
import com.github.marioatwired.jasmine.utils.FormatUtil;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.utils.FinderUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author Michaili K.
 */
public class ForceRemoveCmd extends DJCommand
{
    private final MusicService musicService;

    public ForceRemoveCmd(Bot bot)
    {
        super(bot);
        this.musicService = bot.getMusicService();
        this.name = "forceremove";
        this.help = "removes all entries by a user from the queue";
        this.arguments = "<user>";
        this.aliases = bot.getConfig().getAliases(this.name);
        this.beListening = false;
        this.bePlaying = true;
        this.botPermissions = new Permission[]{Permission.MESSAGE_EMBED_LINKS};
    }

    @Override
    public void doCommand(CommandEvent event)
    {
        if (event.getArgs().isEmpty())
        {
            event.replyError("You need to mention a user!");
            return;
        }

        if (musicService.isQueueEmpty(event.getGuild()))
        {
            event.replyError("There is nothing in the queue!");
            return;
        }

        List<Member> found = FinderUtil.findMembers(event.getArgs(), event.getGuild());

        if (found.isEmpty())
        {
            event.replyError("Unable to find the user!");
            return;
        }
        else if (found.size() > 1)
        {
            StringSelectMenu.Builder menu = StringSelectMenu.create("forceremove_select_" + event.getAuthor().getId())
                    .setPlaceholder("Select a user")
                    .setMinValues(1)
                    .setMaxValues(1);
            for (int i = 0; i < found.size() && i < 4; i++)
            {
                Member member = found.get(i);
                menu.addOption(member.getUser().getName(), String.valueOf(member.getIdLong()),
                        "Remove their entries from the queue");
            }

            event.getChannel().sendMessage("Found multiple users:")
                    .setComponents(ActionRow.of(menu.build()))
                    .queue(msg -> bot.getWaiter().waitForEvent(
                            StringSelectInteractionEvent.class,
                            e -> e.getMessageId().equals(msg.getId())
                                    && e.getUser().getIdLong() == event.getAuthor().getIdLong(),
                            e ->
                            {
                                long selectedId;
                                try
                                {
                                    selectedId = Long.parseLong(e.getValues().get(0));
                                }
                                catch (NumberFormatException ex)
                                {
                                    e.editMessage(event.getClient().getError() + " Invalid selection.")
                                            .setComponents().queue();
                                    return;
                                }

                                Member target = found.stream()
                                        .filter(member -> member.getIdLong() == selectedId)
                                        .findFirst()
                                        .orElse(null);
                                if (target == null)
                                {
                                    e.editMessage(event.getClient().getError() + " User no longer available.")
                                            .setComponents().queue();
                                    return;
                                }

                                e.editMessage("Selected " + FormatUtil.formatUsername(target.getUser()) + ".")
                                        .setComponents().queue();
                                removeAllEntries(target.getUser(), event);
                            },
                            1, TimeUnit.MINUTES,
                            () -> msg.editMessage("Selection timed out.").setComponents().queue()));

            return;
        }

        removeAllEntries(found.get(0).getUser(), event);
    }

    private void removeAllEntries(User target, CommandEvent event)
    {
        int count = musicService.removeAllTracksByUser(event.getGuild(), target.getIdLong());
        if (count == 0)
        {
            event.replyWarning("**" + target.getName() + "** doesn't have any songs in the queue!");
        }
        else
        {
            event.replySuccess("Successfully removed `" + count + "` entries from " + FormatUtil.formatUsername(target) + ".");
        }
    }
}
