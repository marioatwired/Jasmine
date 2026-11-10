/*
 * Copyright 2016 John Grosh <john.a.grosh@gmail.com>.
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
package com.github.marioatwired.jasmine.commands.v1.owner;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.github.marioatwired.jasmine.Bot;
import com.github.marioatwired.jasmine.commands.v1.OwnerCommand;
import com.github.marioatwired.jasmine.commands.v1.TextOutputAdapters.SimpleOutputAdapter;
import com.github.marioatwired.jasmine.commands.v2.music.PlaylistViewSupport;
import com.github.marioatwired.jasmine.playlist.PlaylistLoader;
import com.github.marioatwired.jasmine.service.PlaylistTransferService;
import com.github.marioatwired.jasmine.service.dto.PlaylistMutationResult;
import com.github.marioatwired.jasmine.service.dto.PlaylistTransferResult;
import net.dv8tion.jda.api.entities.Message.Attachment;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
@SuppressWarnings("deprecation") // JDA-Chewtils has no DM context equivalent for text commands yet.
public class PlaylistCmd extends Command
{
    private final Bot bot;

    public PlaylistCmd(Bot bot)
    {
        this.bot = bot;
        this.guildOnly = false;
        this.name = "playlist";
        this.arguments = "<all|view|append|addcurrent|rename|remove|move|replace|make|delete|export|import|setdefault>";
        this.help = "playlist management";
        this.aliases = bot.getConfig().getAliases(this.name);
        this.category = new Category("Playlist");
        this.children = new Command[]{
            new ListCmd(),
            new ViewCmd(),
            new AppendlistCmd(),
            new AddCurrentCmd(),
            new RenameCmd(),
            new RemovelistCmd(),
            new MovelistCmd(),
            new ReplacelistCmd(),
            new MakelistCmd(),
            new DeletelistCmd(),
            new ExportCmd(),
            new ImportCmd(),
            new DefaultlistCmd(bot)
        };
    }

    @Override
    public void execute(CommandEvent event)
    {
        StringBuilder builder = new StringBuilder(event.getClient().getWarning()+" Playlist Management Commands:\n");
        for(Command cmd: this.children)
        {
            if(cmd.isOwnerCommand() && !event.isOwner())
                continue;
            builder.append("\n`").append(event.getClient().getPrefix()).append(name).append(" ").append(cmd.getName())
                    .append(" ").append(cmd.getArguments()==null ? "" : cmd.getArguments()).append("` - ").append(cmd.getHelp());
        }
        event.reply(builder.toString());
    }

    private boolean requireEditor(CommandEvent event)
    {
        if(event.getGuild()==null || event.getMember()==null)
        {
            event.replyError("This command can only be used in a server.");
            return false;
        }
        if(!bot.getMusicService().canEditPlaylistEntries(event.getGuild(), event.getMember()))
        {
            event.replyError("You need to be a DJ or the bot owner to edit playlists!");
            return false;
        }
        return true;
    }

    private static int parsePosition(String value)
    {
        try
        {
            return Integer.parseInt(value);
        }
        catch(NumberFormatException e)
        {
            return -1;
        }
    }

    public class MakelistCmd extends OwnerCommand
    {
        public MakelistCmd()
        {
            this.name = "make";
            this.aliases = new String[]{"create"};
            this.help = "makes a new playlist";
            this.arguments = "<name>";
            this.guildOnly = false;
        }

        @Override
        protected void execute(CommandEvent event)
        {
            String pname = PlaylistLoader.sanitizeName(event.getArgs());
            if(pname == null || pname.isEmpty())
            {
                event.replyError("Please provide a name for the playlist!");
            }
            else if(bot.getPlaylistLoader().getPlaylist(pname) == null)
            {
                try
                {
                    bot.getPlaylistLoader().createPlaylist(pname);
                    event.reply(event.getClient().getSuccess()+" Successfully created playlist `"+pname+"`!");
                }
                catch(IOException e)
                {
                    event.reply(event.getClient().getError()+" I was unable to create the playlist: "+e.getLocalizedMessage());
                }
            }
            else
                event.reply(event.getClient().getError()+" Playlist `"+pname+"` already exists!");
        }
    }

    public class DeletelistCmd extends OwnerCommand
    {
        public DeletelistCmd()
        {
            this.name = "delete";
            this.aliases = new String[]{"remove"};
            this.help = "deletes an existing playlist";
            this.arguments = "<name>";
            this.guildOnly = false;
        }

        @Override
        protected void execute(CommandEvent event)
        {
            String pname = PlaylistLoader.sanitizeName(event.getArgs());
            if(bot.getPlaylistLoader().getPlaylist(pname)==null)
                event.reply(event.getClient().getError()+" Playlist `"+pname+"` doesn't exist!");
            else
            {
                try
                {
                    bot.getPlaylistLoader().deletePlaylist(pname);
                    event.reply(event.getClient().getSuccess()+" Successfully deleted playlist `"+pname+"`!");
                }
                catch(IOException e)
                {
                    event.reply(event.getClient().getError()+" I was unable to delete the playlist: "+e.getLocalizedMessage());
                }
            }
        }
    }

    public class AppendlistCmd extends Command
    {
        public AppendlistCmd()
        {
            this.name = "append";
            this.aliases = new String[]{"add", "amend", "ammend"};
            this.help = "appends songs to an existing playlist (DJ)";
            this.arguments = "<name> <URL> | <URL> | ...";
            this.guildOnly = false;
        }

        @Override
        protected void execute(CommandEvent event)
        {
            if(!requireEditor(event))
                return;

            String[] parts = event.getArgs().trim().split("\\s+", 2);
            if(parts.length<2)
            {
                event.replyError("Please include a playlist name and URLs to add!");
                return;
            }
            String pname = PlaylistLoader.sanitizeName(parts[0]);
            List<String> items = new ArrayList<>();
            for(String url: parts[1].split("\\|"))
            {
                String item = url.trim();
                if(item.startsWith("<") && item.endsWith(">"))
                    item = item.substring(1, item.length()-1);
                if(!item.isEmpty())
                    items.add(item);
            }
            if(items.isEmpty())
            {
                event.replyError("Please include at least one URL to add!");
                return;
            }

            PlaylistMutationResult result = bot.getMusicService()
                    .appendPlaylistItems(pname, items);
            if(!result.success)
            {
                event.replyError(result.errorMessage);
                return;
            }
            if(result.addedItems == 0)
            {
                event.replyWarning("All "+result.duplicateItems
                        +" item(s) are already in playlist `"+pname+"`.");
                return;
            }
            String message = "Successfully added "+result.addedItems
                    +" item(s) to playlist `"+pname+"`!";
            if(result.duplicateItems > 0)
                message += " "+result.duplicateItems+" duplicate(s) skipped.";
            event.replySuccess(message);
        }
    }

    public class AddCurrentCmd extends Command
    {
        public AddCurrentCmd()
        {
            this.name = "addcurrent";
            this.aliases = new String[]{"savecurrent", "addplaying"};
            this.help = "adds the current track to a playlist (DJ)";
            this.arguments = "<name>";
            this.guildOnly = false;
        }

        @Override
        protected void execute(CommandEvent event)
        {
            if(!requireEditor(event))
                return;

            String name = event.getArgs().trim();
            if(name.isEmpty())
            {
                event.replyError("Please include a playlist name.");
                return;
            }
            bot.getMusicService().addCurrentTrackToPlaylist(event.getGuild(), event.getMember(),
                    name, new SimpleOutputAdapter(event));
        }
    }

    public class RenameCmd extends Command
    {
        public RenameCmd()
        {
            this.name = "rename";
            this.aliases = new String[]{"renamelist"};
            this.help = "renames a playlist (DJ)";
            this.arguments = "<old> <new>";
            this.guildOnly = false;
        }

        @Override
        protected void execute(CommandEvent event)
        {
            if(!requireEditor(event))
                return;

            String[] parts = event.getArgs().trim().split("\\s+", 2);
            if(parts.length != 2)
            {
                event.replyError("Usage: `playlist rename <old> <new>`");
                return;
            }

            PlaylistMutationResult result = bot.getMusicService()
                    .renamePlaylist(parts[0], parts[1]);
            if(!result.success)
            {
                event.replyError(result.errorMessage);
                return;
            }
            event.replySuccess("Renamed playlist `"+PlaylistLoader.sanitizeName(parts[0])
                    +"` to `"+PlaylistLoader.sanitizeName(parts[1])+"`.");
        }
    }

    public class RemovelistCmd extends Command
    {
        public RemovelistCmd()
        {
            this.name = "remove";
            this.aliases = new String[]{"rm"};
            this.help = "removes a song from a playlist by position (DJ)";
            this.arguments = "<name> <position>";
            this.guildOnly = false;
        }

        @Override
        protected void execute(CommandEvent event)
        {
            if(!requireEditor(event))
                return;

            String[] parts = event.getArgs().trim().split("\\s+");
            if(parts.length!=2)
            {
                event.replyError("Usage: `playlist remove <name> <position>`");
                return;
            }
            String pname = PlaylistLoader.sanitizeName(parts[0]);
            int position = parsePosition(parts[1]);
            PlaylistMutationResult result = bot.getMusicService()
                    .removePlaylistItem(pname, position);
            if(!result.success)
            {
                event.replyError(result.errorMessage);
                return;
            }
            event.replySuccess("Removed item #"+position+" from playlist `"+pname+"`.");
        }
    }

    public class MovelistCmd extends Command
    {
        public MovelistCmd()
        {
            this.name = "move";
            this.help = "moves a song within a playlist (DJ)";
            this.arguments = "<name> <from> <to>";
            this.guildOnly = false;
        }

        @Override
        protected void execute(CommandEvent event)
        {
            if(!requireEditor(event))
                return;

            String[] parts = event.getArgs().trim().split("\\s+");
            if(parts.length!=3)
            {
                event.replyError("Usage: `playlist move <name> <from> <to>`");
                return;
            }
            String pname = PlaylistLoader.sanitizeName(parts[0]);
            int from = parsePosition(parts[1]);
            int to = parsePosition(parts[2]);
            PlaylistMutationResult result = bot.getMusicService()
                    .movePlaylistItem(pname, from, to);
            if(!result.success)
            {
                event.replyError(result.errorMessage);
                return;
            }
            event.replySuccess("Moved item #"+from+" to #"+to+" in playlist `"+pname+"`.");
        }
    }

    public class ReplacelistCmd extends Command
    {
        public ReplacelistCmd()
        {
            this.name = "replace";
            this.aliases = new String[]{"set", "editurl"};
            this.help = "replaces a song URL in a playlist by position (DJ)";
            this.arguments = "<name> <position> <URL>";
            this.guildOnly = false;
        }

        @Override
        protected void execute(CommandEvent event)
        {
            if(!requireEditor(event))
                return;

            String[] parts = event.getArgs().trim().split("\\s+", 3);
            if(parts.length!=3)
            {
                event.replyError("Usage: `playlist replace <name> <position> <URL>`");
                return;
            }
            String pname = PlaylistLoader.sanitizeName(parts[0]);
            int position = parsePosition(parts[1]);
            String item = parts[2].trim();
            if(item.startsWith("<") && item.endsWith(">"))
                item = item.substring(1, item.length()-1);
            PlaylistMutationResult result = bot.getMusicService()
                    .replacePlaylistItem(pname, position, item);
            if(!result.success)
            {
                event.replyError(result.errorMessage);
                return;
            }
            event.replySuccess("Replaced item #"+position+" in playlist `"+pname+"`.");
        }
    }

    public class ExportCmd extends OwnerCommand
    {
        public ExportCmd()
        {
            this.name = "export";
            this.aliases = new String[]{"download"};
            this.help = "exports a playlist as a .txt file";
            this.arguments = "<name>";
            this.guildOnly = false;
        }

        @Override
        protected void execute(CommandEvent event)
        {
            PlaylistTransferResult result = bot.getMusicService().exportPlaylist(event.getArgs());
            if (!result.success())
            {
                event.replyError(result.message());
                return;
            }

            MessageCreateData message = new MessageCreateBuilder()
                    .setContent(event.getClient().getSuccess() + " " + result.message())
                    .setFiles(List.of(FileUpload.fromData(result.content(),
                            result.playlistName() + ".txt")))
                    .build();
            event.getChannel().sendMessage(message).queue();
        }
    }

    public class ImportCmd extends OwnerCommand
    {
        public ImportCmd()
        {
            this.name = "import";
            this.aliases = new String[]{"upload"};
            this.help = "imports a playlist from an attached .txt file";
            this.arguments = "<name> (attach .txt)";
            this.guildOnly = false;
        }

        @Override
        protected void execute(CommandEvent event)
        {
            String name = event.getArgs().trim();
            if (name.isEmpty())
            {
                event.replyError("Please provide a name for the imported playlist!");
                return;
            }

            List<Attachment> attachments = event.getMessage().getAttachments();
            if (attachments.isEmpty())
            {
                event.replyError("Attach a `.txt` playlist file to the command message!");
                return;
            }

            Attachment attachment = attachments.get(0);
            if (!attachment.getFileName().toLowerCase(Locale.ROOT).endsWith(".txt"))
            {
                event.replyError("The attached file must be a `.txt` playlist file!");
                return;
            }
            if (attachment.getSize() > PlaylistTransferService.MAX_IMPORT_BYTES)
            {
                event.replyError("The attached playlist is too large (maximum 1 MiB).");
                return;
            }

            event.reply("Importing playlist...", message ->
                    attachment.getProxy().download().whenComplete((stream, error) ->
                    {
                        if (error != null || stream == null)
                        {
                            message.editMessage(event.getClient().getError()
                                    + " Could not download the playlist file.").queue();
                            return;
                        }

                        byte[] content;
                        try (InputStream input = stream)
                        {
                            content = input.readAllBytes();
                        }
                        catch (IOException ex)
                        {
                            message.editMessage(event.getClient().getError()
                                    + " Could not read the playlist file.").queue();
                            return;
                        }

                        PlaylistTransferResult result = bot.getMusicService().importPlaylist(name, content);
                        String prefix = result.success()
                                ? event.getClient().getSuccess()
                                : event.getClient().getError();
                        message.editMessage(prefix + " " + result.message()).queue();
                    }));
        }
    }

    public class DefaultlistCmd extends AutoplaylistCmd
    {
        public DefaultlistCmd(Bot bot)
        {
            super(bot);
            this.name = "setdefault";
            this.aliases = new String[]{"default"};
            this.arguments = "<playlistname|NONE>";
        }
    }

    public class ListCmd extends Command
    {
        public ListCmd()
        {
            this.name = "all";
            this.aliases = new String[]{"available","list"};
            this.help = "lists all available playlists";
        }

        @Override
        protected void execute(CommandEvent event)
        {
            if(!bot.getPlaylistLoader().folderExists())
                bot.getPlaylistLoader().createFolder();
            if(!bot.getPlaylistLoader().folderExists())
            {
                event.reply(event.getClient().getWarning()+" Playlists folder does not exist and could not be created!");
                return;
            }
            List<String> list = bot.getPlaylistLoader().getPlaylistNames();
            if(list==null)
                event.reply(event.getClient().getError()+" Failed to load available playlists!");
            else if(list.isEmpty())
                event.reply(event.getClient().getWarning()+" There are no playlists in the Playlists folder!");
            else
            {
                StringBuilder builder = new StringBuilder(event.getClient().getSuccess()+" Available playlists:\n");
                list.forEach(str -> builder.append("`").append(str).append("` "));
                event.reply(builder.toString());
            }
        }
    }

    public class ViewCmd extends Command
    {
        public ViewCmd()
        {
            this.name = "view";
            this.aliases = new String[]{"show", "songs", "edit"};
            this.help = "shows a playlist's songs and opens the editor";
            this.arguments = "<name> [page]";
            this.guildOnly = false;
        }

        @Override
        protected void execute(CommandEvent event)
        {
            String args = event.getArgs().trim();
            if(args.isEmpty())
            {
                event.replyError("Please include a playlist name.");
                return;
            }

            String[] parts = args.split("\\s+");
            int requestedPage = 1;
            String namePart = args;
            if(parts.length>1 && parts[parts.length-1].matches("\\d+"))
            {
                requestedPage = parsePosition(parts[parts.length-1]);
                namePart = args.substring(0, args.length()-parts[parts.length-1].length()).trim();
            }

            PlaylistViewSupport.open(bot.getMusicService(), event.getGuild(), event.getMember(),
                    event.getAuthor().getIdLong(), namePart, requestedPage, result ->
                    {
                        if(result.hasError())
                        {
                            event.replyError(result.errorMessage());
                            return;
                        }
                        if(result.hasComponents())
                        {
                            event.reply(new MessageCreateBuilder()
                                    .addEmbeds(result.embed())
                                    .setComponents(result.components())
                                    .build());
                        }
                        else
                        {
                            event.reply(result.embed());
                        }
                    });
        }
    }
}
