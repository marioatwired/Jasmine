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
package com.github.marioatwired.jasmine.commands.v1.music;

import com.github.marioatwired.jasmine.Bot;
import com.github.marioatwired.jasmine.audio.AudioHandler;
import com.github.marioatwired.jasmine.audio.QueuedTrack;
import com.github.marioatwired.jasmine.audio.RequestMetadata;
import com.github.marioatwired.jasmine.commands.v1.MusicCommand;
import com.github.marioatwired.jasmine.utils.FormatUtil;
import com.github.marioatwired.jasmine.utils.TimeUtil;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author John Grosh <john.a.grosh@gmail.com>
 */
public class SearchCmd extends MusicCommand 
{
    protected String searchPrefix = "ytsearch:";
    private final String searchingEmoji;
    
    public SearchCmd(Bot bot)
    {
        super(bot);
        this.searchingEmoji = bot.getConfig().getSearching();
        this.name = "search";
        this.aliases = bot.getConfig().getAliases(this.name);
        this.arguments = "<query>";
        this.help = "searches Youtube for a provided query";
        this.beListening = true;
        this.bePlaying = false;
        this.botPermissions = new Permission[]{Permission.MESSAGE_EMBED_LINKS};
    }
    @Override
    public void doCommand(CommandEvent event) 
    {
        if(event.getArgs().isEmpty())
        {
            event.replyError("Please include a query.");
            return;
        }
        String query = searchPrefix + event.getArgs();
        event.reply(searchingEmoji+" Searching... `["+event.getArgs()+"]`", 
                m -> bot.getPlayerManager().loadItemOrdered(event.getGuild(), query, 
                    bot.getAudioLoadWrapper().wrap(query, new ResultHandler(m,event))));
    }
    
    private class ResultHandler implements AudioLoadResultHandler 
    {
        private final Message m;
        private final CommandEvent event;
        
        private ResultHandler(Message m, CommandEvent event)
        {
            this.m = m;
            this.event = event;
        }
        
        @Override
        public void trackLoaded(AudioTrack track)
        {
            if(bot.getConfig().isTooLong(track))
            {
                String trackTitle = FormatUtil.getTrackTitle(track);
                m.editMessage(FormatUtil.filter(event.getClient().getWarning()+" This track (**"+trackTitle+"**) is longer than the allowed maximum: `"
                        + TimeUtil.formatTime(track.getDuration())+"` > `"+bot.getConfig().getMaxTime()+"`")).queue();
                return;
            }
            AudioHandler handler = (AudioHandler)event.getGuild().getAudioManager().getSendingHandler();
            int pos = handler.addTrack(new QueuedTrack(track, RequestMetadata.fromResultHandler(track, event)))+1;
            String trackTitle = FormatUtil.getTrackTitle(track);
            m.editMessage(FormatUtil.filter(event.getClient().getSuccess()+" Added **"+trackTitle
                    +"** (`"+ TimeUtil.formatTime(track.getDuration())+"`) "+(pos==0 ? "to begin playing"
                        : " to the queue at position "+pos))).queue();
        }

        @Override
        public void playlistLoaded(AudioPlaylist playlist)
        {
            List<AudioTrack> tracks = playlist.getTracks();
            if (tracks.isEmpty())
            {
                m.editMessage(FormatUtil.filter(event.getClient().getWarning()
                        + " No results found for `" + event.getArgs() + "`.")).queue();
                return;
            }

            int limit = Math.min(4, tracks.size());
            StringSelectMenu.Builder menu = StringSelectMenu.create("search_select_" + event.getAuthor().getId())
                    .setPlaceholder("Select a track")
                    .setMinValues(1)
                    .setMaxValues(1);
            StringBuilder description = new StringBuilder();
            for (int i = 0; i < limit; i++)
            {
                AudioTrack track = tracks.get(i);
                String trackTitle = FormatUtil.getTrackTitle(track);
                String label = trackTitle == null ? "" : trackTitle;
                if (label.length() > 80)
                {
                    label = label.substring(0, 77) + "...";
                }
                menu.addOption(label, String.valueOf(i),
                        "Duration: " + TimeUtil.formatTime(track.getDuration()));
                description.append("`").append(i + 1).append(".` ")
                        .append("[**").append(FormatUtil.filter(label)).append("**](")
                        .append(track.getInfo().uri).append(") `[")
                        .append(TimeUtil.formatTime(track.getDuration())).append("]`\n");
            }

            EmbedBuilder embed = new EmbedBuilder()
                    .setTitle("Search results for `" + event.getArgs() + "`")
                    .setDescription(description.toString())
                    .setColor(event.getSelfMember().getColors().getPrimary());
            m.editMessageEmbeds(embed.build())
                    .setContent("")
                    .setComponents(ActionRow.of(menu.build()))
                    .queue(msg -> bot.getWaiter().waitForEvent(
                            StringSelectInteractionEvent.class,
                            e -> e.getMessageId().equals(msg.getId())
                                    && e.getUser().getIdLong() == event.getAuthor().getIdLong(),
                            e ->
                            {
                                int selected;
                                try
                                {
                                    selected = Integer.parseInt(e.getValues().get(0));
                                }
                                catch (NumberFormatException ex)
                                {
                                    e.editMessage(event.getClient().getError() + " Invalid selection.")
                                            .setEmbeds().setComponents().queue();
                                    return;
                                }

                                AudioTrack track = tracks.get(selected);
                                String trackTitle = FormatUtil.getTrackTitle(track);
                                if (bot.getConfig().isTooLong(track))
                                {
                                    e.editMessage(FormatUtil.filter(event.getClient().getWarning()
                                            + " This track (**" + trackTitle + "**) is longer than the allowed maximum: `"
                                            + TimeUtil.formatTime(track.getDuration()) + "` > `"
                                            + bot.getConfig().getMaxTime() + "`"))
                                            .setEmbeds().setComponents().queue();
                                    return;
                                }

                                AudioHandler handler = (AudioHandler) event.getGuild().getAudioManager().getSendingHandler();
                                int pos = handler.addTrack(new QueuedTrack(track,
                                        RequestMetadata.fromResultHandler(track, event))) + 1;
                                e.editMessage(FormatUtil.filter(event.getClient().getSuccess()
                                        + " Added **" + FormatUtil.filter(trackTitle) + "** (`"
                                        + TimeUtil.formatTime(track.getDuration()) + "`) "
                                        + (pos == 0 ? "to begin playing" : " to the queue at position " + pos)))
                                        .setEmbeds().setComponents().queue();
                            },
                            1, TimeUnit.MINUTES,
                            () -> m.editMessage("Search timed out.")
                                    .setEmbeds().setComponents().queue()));
        }

        @Override
        public void noMatches() 
        {
            m.editMessage(FormatUtil.filter(event.getClient().getWarning()+" No results found for `"+event.getArgs()+"`.")).queue();
        }

        @Override
        public void loadFailed(FriendlyException throwable) 
        {
            if(throwable.severity==Severity.COMMON)
                m.editMessage(event.getClient().getError()+" Error loading: "+throwable.getMessage()).queue();
            else
                m.editMessage(event.getClient().getError()+" Error loading track.").queue();
        }
    }
}
