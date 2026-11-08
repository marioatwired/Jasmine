package com.github.marioatwired.jasmine.commands;

import com.github.marioatwired.jasmine.audio.AudioHandler;
import com.github.marioatwired.jasmine.service.MusicService;
import com.github.marioatwired.jasmine.service.OutputAdapter;
import net.dv8tion.jda.api.entities.Message;

import java.util.function.Consumer;

/**
 * Base implementation of OutputAdapter with no-op defaults.
 * Subclasses only need to override the methods they actually use.
 */
public abstract class BaseOutputAdapter implements OutputAdapter
{
    @Override
    public void replySuccess(String content)
    {
        // No-op by default
    }

    @Override
    public void replyError(String content)
    {
        // No-op by default
    }

    @Override
    public void replyWarning(String content)
    {
        // No-op by default
    }

    @Override
    public void editMessage(String content)
    {
        // No-op by default
    }

    @Override
    public void editMessage(String content, Consumer<Message> onSuccess)
    {
        // No-op by default
    }

    @Override
    public void editNowPlaying(AudioHandler handler)
    {
        // No-op by default
    }

    @Override
    public void editNoMusic(AudioHandler handler)
    {
        // No-op by default
    }

    @Override
    public void onShowHelp()
    {
        // No-op by default
    }
}
