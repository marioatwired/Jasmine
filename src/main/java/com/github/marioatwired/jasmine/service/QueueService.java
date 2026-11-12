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

import com.github.marioatwired.jasmine.Bot;
import com.github.marioatwired.jasmine.audio.AudioHandler;
import com.github.marioatwired.jasmine.audio.QueuedTrack;
import com.github.marioatwired.jasmine.commands.v1.DJCommand;
import com.github.marioatwired.jasmine.queue.AbstractQueue;
import com.github.marioatwired.jasmine.utils.FormatUtil;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Queue inspection and mutation operations.
 * <p>
 * Playback control stays in {@link PlaybackControlService}; this service only
 * changes queue contents and reports the result to the caller.
 */
public class QueueService
{
    private static final Logger LOG = LoggerFactory.getLogger(QueueService.class);

    private final Bot bot;

    public QueueService(Bot bot)
    {
        this.bot = bot;
    }

    public void playNext(Guild guild, Member member, int position, OutputAdapter output)
    {
        if (position == 1)
        {
            output.replyWarning("This track is already next!");
            return;
        }
        moveTrack(guild, member, position, 1, output);
    }

    public void shuffle(Guild guild, Member member, int startIndex, OutputAdapter output)
    {
        if (!requireDJPermission(guild, member, output, "use this button"))
        {
            return;
        }

        AudioHandler handler = getHandler(guild);
        int shuffled = handler.getQueue().shuffle(startIndex);
        output.replySuccess("Shuffled " + shuffled + " tracks!");
    }

    public int shuffleUserTracks(Guild guild, long userId)
    {
        LOG.debug("Shuffling user tracks: guild={}, userId={}", guild.getId(), userId);

        AudioHandler handler = getHandler(guild);
        int shuffled = handler.getQueue().shuffle(userId);

        LOG.info("User tracks shuffled: guild={}, userId={}, count={}", guild.getId(), userId, shuffled);

        return shuffled;
    }

    public void removeTrack(Guild guild, Member member, int position, OutputAdapter output)
    {
        AudioHandler handler = getHandler(guild);

        if (!requireNonEmptyQueue(handler, output))
        {
            return;
        }
        if (!validateQueuePosition(handler, position, output))
        {
            return;
        }

        boolean isDJ = DJCommand.checkDJPermission(bot, guild, member);
        QueuedTrack queuedTrack = handler.getQueue().get(position - 1);
        String title = FormatUtil.getTrackTitle(queuedTrack.getTrack());

        if (queuedTrack.getIdentifier() == member.getIdLong())
        {
            handler.getQueue().remove(position - 1);
            output.replySuccess("Removed **" + title + "** from the queue");
            return;
        }

        if (!isDJ)
        {
            output.replyError("You cannot remove **" + title + "** because you didn't add it!");
            return;
        }

        handler.getQueue().remove(position - 1);
        User user = null;
        try
        {
            user = guild.getJDA().getUserById(queuedTrack.getIdentifier());
        }
        catch (Exception ignored)
        {
        }

        output.replySuccess("Removed **" + title + "** from the queue (requested by "
                + (user == null ? "someone" : "**" + user.getName() + "**") + ")");
    }

    public void removeAllTracks(Guild guild, Member member, OutputAdapter output)
    {
        AudioHandler handler = getHandler(guild);

        if (!requireNonEmptyQueue(handler, output))
        {
            return;
        }

        int count = handler.getQueue().removeAll(member.getIdLong());
        if (count == 0)
        {
            output.replyWarning("You don't have any songs in the queue!");
            return;
        }

        output.replySuccess("Successfully removed your " + count + " entries.");
    }

    public int removeAllTracksByUser(Guild guild, long userId)
    {
        LOG.debug("Removing all tracks by user: guild={}, userId={}", guild.getId(), userId);

        AudioHandler handler = getHandler(guild);
        int count = handler.getQueue().removeAll(userId);

        LOG.info("Removed {} tracks by user: guild={}, userId={}", count, guild.getId(), userId);

        return count;
    }

    public void clearQueue(Guild guild, Member member, OutputAdapter output)
    {
        if (!requireDJPermission(guild, member, output, "clear the queue"))
        {
            return;
        }

        AudioHandler handler = getHandler(guild);
        if (handler == null)
        {
            output.replyError("There is no player in this server!");
            return;
        }

        int count = handler.clearQueue();
        if (count == 0)
        {
            output.replyWarning("The queue is already empty!");
            return;
        }

        output.replySuccess("Cleared " + count + " track" + (count == 1 ? "" : "s") + " from the queue.");
    }

    public boolean isQueueEmpty(Guild guild)
    {
        AudioHandler handler = getHandler(guild);
        return handler == null || handler.getQueue().isEmpty();
    }

    public void moveTrack(Guild guild, Member member, int from, int to, OutputAdapter output)
    {
        if (!requireDJPermission(guild, member, output, "move tracks"))
        {
            return;
        }
        if (from == to)
        {
            output.replyError("Can't move a track to the same position.");
            return;
        }

        AudioHandler handler = getHandler(guild);
        AbstractQueue<QueuedTrack> queue = handler.getQueue();

        if (isInvalidPosition(queue, from))
        {
            output.replyError("`" + from + "` is not a valid position in the queue!");
            return;
        }
        if (isInvalidPosition(queue, to))
        {
            output.replyError("`" + to + "` is not a valid position in the queue!");
            return;
        }

        QueuedTrack track = queue.moveItem(from - 1, to - 1);
        String title = FormatUtil.getTrackTitle(track.getTrack());
        output.replySuccess("Moved **" + title + "** from position `" + from + "` to `" + to + "`.");
    }

    public String moveTrackPosition(Guild guild, int from, int to)
    {
        LOG.debug("Moving track: guild={}, from={}, to={}", guild.getId(), from, to);

        AudioHandler handler = getHandler(guild);
        AbstractQueue<QueuedTrack> queue = handler.getQueue();

        if (isInvalidPosition(queue, from) || isInvalidPosition(queue, to))
        {
            LOG.debug("Move rejected: invalid position(s) - from={}, to={}, queueSize={}",
                    from, to, queue.size());
            return null;
        }

        QueuedTrack track = queue.moveItem(from - 1, to - 1);
        String title = FormatUtil.getTrackTitle(track.getTrack());

        LOG.info("Track moved: guild={}, track=\"{}\", from={}, to={}",
                guild.getId(), title, from, to);

        return title;
    }

    public boolean isValidQueuePosition(Guild guild, int position)
    {
        AudioHandler handler = getHandler(guild);
        return handler != null && position >= 1 && position <= handler.getQueue().size();
    }

    public int getQueueSize(Guild guild)
    {
        AudioHandler handler = getHandler(guild);
        return handler != null ? handler.getQueue().size() : 0;
    }

    private AudioHandler getHandler(Guild guild)
    {
        return (AudioHandler) guild.getAudioManager().getSendingHandler();
    }

    private boolean requireDJPermission(Guild guild, Member member, OutputAdapter output, String action)
    {
        if (!DJCommand.checkDJPermission(bot, guild, member))
        {
            output.replyError("You need to be a DJ to " + action + "!");
            return false;
        }
        return true;
    }

    private boolean isInvalidPosition(AbstractQueue<QueuedTrack> queue, int position)
    {
        return position < 1 || position > queue.size();
    }

    private boolean validateQueuePosition(AudioHandler handler, int position, OutputAdapter output)
    {
        int size = handler.getQueue().size();
        if (position < 1 || position > size)
        {
            output.replyError("Position must be a valid integer between 1 and " + size + "!");
            return false;
        }
        return true;
    }

    private boolean requireNonEmptyQueue(AudioHandler handler, OutputAdapter output)
    {
        if (handler.getQueue().isEmpty())
        {
            output.replyError("There is nothing in the queue!");
            return false;
        }
        return true;
    }
}
