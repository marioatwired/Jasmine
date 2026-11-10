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
package com.github.marioatwired.jasmine.commands.v2.music;

import com.jagrosh.jdautilities.command.SlashCommand;
import com.jagrosh.jdautilities.command.SlashCommandEvent;
import com.github.marioatwired.jasmine.Bot;
import com.github.marioatwired.jasmine.playlist.PlaylistLoader;
import com.github.marioatwired.jasmine.service.dto.PlaylistMutationResult;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.ArrayList;
import java.util.List;

/**
 * Slash command group for viewing and editing saved playlists.
 */
public class PlaylistSlashCmd extends SlashCommand
{
    private final Bot bot;

    public PlaylistSlashCmd(Bot bot)
    {
        this.bot = bot;
        this.name = "playlist";
        this.help = "view and edit saved playlists";
        this.guildOnly = true;
        this.category = new Category("Playlist");
        this.children = new SlashCommand[]{
            new ViewCmd(),
            new AppendCmd(),
            new RemoveCmd(),
            new MoveCmd(),
            new ReplaceCmd()
        };
    }

    @Override
    protected void execute(SlashCommandEvent event)
    {
        event.reply("Use `/playlist view`, `/playlist append`, `/playlist remove`, "
                + "`/playlist move`, or `/playlist replace`.")
                .setEphemeral(true)
                .queue();
    }

    private abstract class EditorCommand extends SlashCommand
    {
        protected EditorCommand()
        {
            this.guildOnly = true;
            this.category = new Category("Playlist");
        }

        @Override
        protected void execute(SlashCommandEvent event)
        {
            if (event.getGuild() == null || event.getMember() == null
                    || !bot.getMusicService().canEditPlaylistEntries(event.getGuild(), event.getMember()))
            {
                event.reply(event.getClient().getError()
                        + " You need to be a DJ or the bot owner to edit playlists!")
                        .setEphemeral(true)
                        .queue();
                return;
            }
            doEdit(event);
        }

        protected String playlistName(SlashCommandEvent event)
        {
            String raw = event.getOption("name").getAsString();
            String name = PlaylistLoader.sanitizeName(raw);
            if (name.isEmpty())
            {
                event.reply(event.getClient().getError() + " Please include a playlist name.")
                        .setEphemeral(true)
                        .queue();
                return null;
            }
            return name;
        }

        protected abstract void doEdit(SlashCommandEvent event);
    }

    public class ViewCmd extends SlashCommand
    {
        public ViewCmd()
        {
            this.name = "view";
            this.help = "view and edit a saved playlist";
            this.guildOnly = true;
            this.category = new Category("Playlist");
            this.options = List.of(
                    new OptionData(OptionType.STRING, "name", "the playlist name", true),
                    new OptionData(OptionType.INTEGER, "page", "page number", false).setMinValue(1)
            );
        }

        @Override
        protected void execute(SlashCommandEvent event)
        {
            String name = event.getOption("name").getAsString();
            int page = (int) event.optLong("page", 1);
            PlaylistViewSupport.open(bot.getMusicService(), event.getGuild(), event.getMember(),
                    event.getUser().getIdLong(), name, page, result ->
                    {
                        if (result.hasError())
                        {
                            event.reply(event.getClient().getError() + " " + result.errorMessage())
                                    .setEphemeral(true)
                                    .queue();
                            return;
                        }
                        if (result.hasComponents())
                        {
                            event.replyEmbeds(result.embed())
                                    .setComponents(result.components())
                                    .queue();
                        }
                        else
                        {
                            event.replyEmbeds(result.embed()).queue();
                        }
                    });
        }
    }

    public class AppendCmd extends EditorCommand
    {
        public AppendCmd()
        {
            this.name = "append";
            this.help = "append songs to a saved playlist";
            this.options = List.of(
                    new OptionData(OptionType.STRING, "name", "the playlist name", true),
                    new OptionData(OptionType.STRING, "urls",
                            "URL or URLs separated by |", true)
            );
        }

        @Override
        protected void doEdit(SlashCommandEvent event)
        {
            String name = playlistName(event);
            if (name == null)
            {
                return;
            }

            List<String> items = new ArrayList<>();
            for (String value : event.getOption("urls").getAsString().split("\\|"))
            {
                String item = value.trim();
                if (item.startsWith("<") && item.endsWith(">"))
                {
                    item = item.substring(1, item.length() - 1);
                }
                if (!item.isEmpty())
                {
                    items.add(item);
                }
            }

            PlaylistMutationResult result = bot.getMusicService()
                    .appendPlaylistItems(name, items);
            replyResult(event, result, "Added " + items.size()
                    + " item(s) to playlist `" + name + "`.");
        }
    }

    public class RemoveCmd extends EditorCommand
    {
        public RemoveCmd()
        {
            this.name = "remove";
            this.help = "remove a song from a saved playlist";
            this.options = List.of(
                    new OptionData(OptionType.STRING, "name", "the playlist name", true),
                    new OptionData(OptionType.INTEGER, "position", "song position", true).setMinValue(1)
            );
        }

        @Override
        protected void doEdit(SlashCommandEvent event)
        {
            String name = playlistName(event);
            if (name == null)
            {
                return;
            }

            int position = (int) event.getOption("position").getAsLong();
            PlaylistMutationResult result = bot.getMusicService()
                    .removePlaylistItem(name, position);
            replyResult(event, result, "Removed item #" + position
                    + " from playlist `" + name + "`.");
        }
    }

    public class MoveCmd extends EditorCommand
    {
        public MoveCmd()
        {
            this.name = "move";
            this.help = "move a song within a saved playlist";
            this.options = List.of(
                    new OptionData(OptionType.STRING, "name", "the playlist name", true),
                    new OptionData(OptionType.INTEGER, "from", "current song position", true).setMinValue(1),
                    new OptionData(OptionType.INTEGER, "to", "new song position", true).setMinValue(1)
            );
        }

        @Override
        protected void doEdit(SlashCommandEvent event)
        {
            String name = playlistName(event);
            if (name == null)
            {
                return;
            }

            int from = (int) event.getOption("from").getAsLong();
            int to = (int) event.getOption("to").getAsLong();
            PlaylistMutationResult result = bot.getMusicService()
                    .movePlaylistItem(name, from, to);
            replyResult(event, result, "Moved item #" + from + " to #" + to
                    + " in playlist `" + name + "`.");
        }
    }

    public class ReplaceCmd extends EditorCommand
    {
        public ReplaceCmd()
        {
            this.name = "replace";
            this.help = "replace a song URL in a saved playlist";
            this.options = List.of(
                    new OptionData(OptionType.STRING, "name", "the playlist name", true),
                    new OptionData(OptionType.INTEGER, "position", "song position", true).setMinValue(1),
                    new OptionData(OptionType.STRING, "url", "replacement URL", true)
            );
        }

        @Override
        protected void doEdit(SlashCommandEvent event)
        {
            String name = playlistName(event);
            if (name == null)
            {
                return;
            }

            int position = (int) event.getOption("position").getAsLong();
            String item = event.getOption("url").getAsString().trim();
            if (item.startsWith("<") && item.endsWith(">"))
            {
                item = item.substring(1, item.length() - 1);
            }
            PlaylistMutationResult result = bot.getMusicService()
                    .replacePlaylistItem(name, position, item);
            replyResult(event, result, "Replaced item #" + position
                    + " in playlist `" + name + "`.");
        }
    }

    private void replyResult(SlashCommandEvent event, PlaylistMutationResult result, String successMessage)
    {
        if (!result.success)
        {
            event.reply(event.getClient().getError() + " " + result.errorMessage)
                    .setEphemeral(true)
                    .queue();
            return;
        }
        event.reply(event.getClient().getSuccess() + " " + successMessage).queue();
    }
}
