package com.github.marioatwired.jasmine.commands.v2.music;

import com.github.marioatwired.jasmine.Bot;

/**
 * Slash alias for now-playing output.
 */
public class NpSlashCmd extends NowPlayingSlashCmd
{
    public NpSlashCmd(Bot bot)
    {
        super(bot, "np", "shows the song that is currently playing");
    }
}
