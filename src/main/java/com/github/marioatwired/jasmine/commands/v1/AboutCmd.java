/*
 * Copyright 2026 Mario
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
package com.github.marioatwired.jasmine.commands.v1;

import com.github.marioatwired.jasmine.utils.OtherUtil;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.JDAUtilitiesInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDAInfo;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.ApplicationInfo;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.ChannelType;

import java.awt.Color;

/**
 * About command with version information sourced from runtime metadata rather
 * than the unresolved build placeholder shipped by JDA-Chewtils.
 */
public class AboutCmd extends Command
{
    private static final String FEATURE_ICON = "\uD83C\uDFB6";

    private final Color color;
    private final String description;
    private final String[] features;
    private final Permission[] perms;

    private volatile String oauthLink;

    public AboutCmd(Color color, String description, String[] features, Permission... perms)
    {
        this.color = color;
        this.description = description;
        this.features = features == null ? new String[0] : features.clone();
        this.perms = perms == null ? new Permission[0] : perms.clone();

        this.name = "about";
        this.help = "shows info about the bot";
        this.guildOnly = false;
        this.botPermissions = new Permission[]{Permission.MESSAGE_EMBED_LINKS};
    }

    @Override
    protected void execute(CommandEvent event)
    {
        String inviteLink = getInviteLink(event);
        String owner = getOwnerDisplay(event);
        String chewtilsVersion = chewtilsVersion();

        StringBuilder featureList = new StringBuilder();
        for (String feature : features)
            featureList.append('\n').append(FEATURE_ICON).append(' ').append(feature);

        EmbedBuilder builder = new EmbedBuilder()
                .setColor(event.isFromType(ChannelType.TEXT)
                        ? event.getGuild().getSelfMember().getColor()
                        : color)
                .setAuthor("All about " + event.getSelfUser().getName() + "!",
                        null, event.getSelfUser().getAvatarUrl())
                .setDescription("Hello! I am **" + event.getSelfUser().getName() + "**, "
                        + description
                        + "\n\n**Features:**" + featureList
                        + "\n\n**Version:** `" + OtherUtil.getCurrentVersion() + "`"
                        + "\n**JDA:** `" + JDAInfo.VERSION + "`"
                        + "\n**Chew's Commands Extension:** `" + chewtilsVersion + "`"
                        + "\n**Owner:** " + owner
                        + (inviteLink.isBlank() ? "" : "\n\n[Invite me](" + inviteLink + ")"));

        event.reply(builder.build());
    }

    /**
     * JDA-Chewtils 2.2.1 ships with an unresolved revision token in its info
     * class. Prefer the real value when present and fall back to the published
     * revision used by this project.
     */
    static String chewtilsVersion()
    {
        String revision = JDAUtilitiesInfo.VERSION_REVISION;
        if (revision == null || revision.isBlank() || revision.startsWith("@"))
            revision = "1";

        return JDAUtilitiesInfo.VERSION_MAJOR + "." + JDAUtilitiesInfo.VERSION_MINOR + "." + revision;
    }

    private String getInviteLink(CommandEvent event)
    {
        String cached = oauthLink;
        if (cached != null)
            return cached;

        try
        {
            ApplicationInfo info = event.getJDA().retrieveApplicationInfo().complete();
            cached = info.isBotPublic() ? info.getInviteUrl(0L, perms) : "";
        }
        catch (RuntimeException ex)
        {
            cached = "";
        }
        oauthLink = cached;
        return cached;
    }

    private static String getOwnerDisplay(CommandEvent event)
    {
        String ownerId = event.getClient().getOwnerId();
        User owner = event.getJDA().getUserById(ownerId);
        return owner == null ? "<@" + ownerId + ">" : owner.getName();
    }
}
