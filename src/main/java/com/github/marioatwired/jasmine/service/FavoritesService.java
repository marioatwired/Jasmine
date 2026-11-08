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
import com.github.marioatwired.jasmine.commands.v1.DJCommand;
import com.github.marioatwired.jasmine.playlist.PlaylistLoader;
import com.github.marioatwired.jasmine.utils.FormatUtil;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;

import java.nio.file.Path;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Adds the currently playing track to the shared favorites playlist.
 */
public class FavoritesService
{
    private static final String FAVORITES_PLAYLIST_NAME = "favorites";
    private static final Pattern WINDOWS_DRIVE_ABSOLUTE_PATH = Pattern.compile("^[A-Za-z]:[\\\\/].*");
    private static final boolean WINDOWS_RUNTIME =
            System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");

    private final Bot bot;

    public FavoritesService(Bot bot)
    {
        this.bot = bot;
    }

    public void addCurrentTrackToFavorites(Guild guild, Member member, OutputAdapter output)
    {
        if (!requireDJPermission(guild, member, output, "add the current track to favorites"))
        {
            return;
        }

        AudioHandler handler = getHandler(guild);
        if (handler == null)
        {
            output.replyError("There is no player in this server!");
            return;
        }

        AudioTrack currentTrack = handler.getPlayer().getPlayingTrack();
        if (currentTrack == null)
        {
            output.replyError("There is no track currently playing!");
            return;
        }

        String favoriteEntry = currentTrack.getInfo().uri;
        if (favoriteEntry == null || favoriteEntry.isBlank())
        {
            output.replyError("The current track does not have a valid URI or absolute path to save.");
            return;
        }
        if (!isSupportedFavoriteEntry(favoriteEntry))
        {
            output.replyError("Only URI references or absolute file paths for this OS can be added to favorites.");
            return;
        }

        PlaylistLoader.PlaylistResult<PlaylistLoader.AppendIfAbsentResult> appendResult =
                bot.getPlaylistLoader().appendItemIfAbsentResult(FAVORITES_PLAYLIST_NAME, favoriteEntry);
        if (!appendResult.isSuccess())
        {
            output.replyError(PlaylistErrorMessages.toMessage(appendResult.getError()));
            return;
        }

        if (appendResult.getValue().getStatus() == PlaylistLoader.AppendIfAbsentStatus.ALREADY_PRESENT)
        {
            handler.markCurrentTrackFavorited(favoriteEntry);
            output.editNowPlaying(handler);
            output.replyWarning("This track is already favorited");
            return;
        }

        handler.markCurrentTrackFavorited(favoriteEntry);
        output.editNowPlaying(handler);
        output.replySuccess("Added " + FormatUtil.filter(FormatUtil.getTrackTitle(currentTrack)) + " to Favorites");
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

    private static boolean isSupportedFavoriteEntry(String value)
    {
        String candidate = value == null ? "" : value.trim();
        if (candidate.isEmpty())
        {
            return false;
        }
        return isUriLikeReference(candidate) || isOsNativeAbsolutePath(candidate);
    }

    private static boolean isUriLikeReference(String value)
    {
        // Avoid treating Windows drive paths (e.g. C:\music\song.mp3) as URI schemes.
        if (isWindowsAbsolutePathSyntax(value))
        {
            return false;
        }
        int colonIndex = value.indexOf(':');
        if (colonIndex <= 0)
        {
            return false;
        }
        for (int i = 0; i < colonIndex; i++)
        {
            char c = value.charAt(i);
            if (!Character.isLetterOrDigit(c) && c != '+' && c != '-' && c != '.')
            {
                return false;
            }
        }
        return true;
    }

    private static boolean isOsNativeAbsolutePath(String value)
    {
        try
        {
            if (!Path.of(value).isAbsolute())
            {
                return false;
            }
        }
        catch (RuntimeException ex)
        {
            return false;
        }

        if (WINDOWS_RUNTIME)
        {
            return isWindowsAbsolutePathSyntax(value);
        }
        return value.startsWith("/");
    }

    private static boolean isWindowsAbsolutePathSyntax(String value)
    {
        return WINDOWS_DRIVE_ABSOLUTE_PATH.matcher(value).matches() || value.startsWith("\\\\");
    }
}
