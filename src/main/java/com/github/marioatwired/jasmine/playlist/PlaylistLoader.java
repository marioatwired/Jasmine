/*
 * Copyright 2018 John Grosh (jagrosh).
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
package com.github.marioatwired.jasmine.playlist;

import com.github.marioatwired.jasmine.BotConfig;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Playlist model, public playlist operations, and storage facade.
 */
public class PlaylistLoader
{
    public enum PlaylistErrorType
    {
        INVALID_CONFIG,
        STORAGE_UNAVAILABLE,
        PLAYLIST_NOT_FOUND,
        PLAYLIST_ALREADY_EXISTS
    }

    public static final class PlaylistError
    {
        private final PlaylistErrorType type;
        private final String message;
        private final String configuredPath;
        private final Throwable cause;

        private PlaylistError(PlaylistErrorType type, String message, String configuredPath, Throwable cause)
        {
            this.type = type;
            this.message = message;
            this.configuredPath = configuredPath;
            this.cause = cause;
        }

        public static PlaylistError of(PlaylistErrorType type, String message, String configuredPath, Throwable cause)
        {
            return new PlaylistError(type, message, configuredPath, cause);
        }

        public PlaylistErrorType getType()
        {
            return type;
        }

        public String getMessage()
        {
            return message;
        }

        public String getConfiguredPath()
        {
            return configuredPath;
        }

        public Throwable getCause()
        {
            return cause;
        }
    }

    public static final class PlaylistResult<T>
    {
        private final T value;
        private final PlaylistError error;

        private PlaylistResult(T value, PlaylistError error)
        {
            this.value = value;
            this.error = error;
        }

        public static <T> PlaylistResult<T> success(T value)
        {
            return new PlaylistResult<>(value, null);
        }

        public static <T> PlaylistResult<T> failure(PlaylistError error)
        {
            return new PlaylistResult<>(null, error);
        }

        public boolean isSuccess()
        {
            return error == null;
        }

        public T getValue()
        {
            return value;
        }

        public PlaylistError getError()
        {
            return error;
        }
    }

    public enum AppendIfAbsentStatus
    {
        APPENDED,
        ALREADY_PRESENT
    }

    public static final class AppendIfAbsentResult
    {
        private final AppendIfAbsentStatus status;

        private AppendIfAbsentResult(AppendIfAbsentStatus status)
        {
            this.status = status;
        }

        public static AppendIfAbsentResult appended()
        {
            return new AppendIfAbsentResult(AppendIfAbsentStatus.APPENDED);
        }

        public static AppendIfAbsentResult alreadyPresent()
        {
            return new AppendIfAbsentResult(AppendIfAbsentStatus.ALREADY_PRESENT);
        }

        public AppendIfAbsentStatus getStatus()
        {
            return status;
        }
    }

    private final BotConfig config;
    private final PlaylistStorage storage;

    /**
     * Normalizes a user-provided playlist name into a safe file name.
     * Whitespace becomes underscores, path separators and reserved characters
     * are removed, and leading/trailing dots are stripped.
     *
     * @param name raw playlist name
     * @return a safe playlist name, or an empty string when no usable name remains
     */
    public static String sanitizeName(String name)
    {
        if (name == null)
            return "";

        String sanitized = name.trim().replaceAll("\\s+", "_");
        sanitized = sanitized.replaceAll("[\\\\/:*?\"<>|]", "");
        sanitized = sanitized.replaceAll("\\p{Cntrl}", "");
        sanitized = sanitized.replaceAll("^\\.+|\\.+$", "");
        return sanitized;
    }

    public PlaylistLoader(BotConfig config)
    {
        this.config = config;
        this.storage = new PlaylistStorage(config);
    }






    public PlaylistResult<Path> ensureStorageReady()
    {
        return storage.ensureStorageReady();
    }

    public PlaylistResult<Path> checkStorageReady()
    {
        return storage.checkStorageReady();
    }

    public PlaylistResult<List<String>> getPlaylistNamesResult()
    {
        return storage.getPlaylistNamesResult();
    }

    public PlaylistResult<List<String>> readPlaylistResult(String name)
    {
        return storage.readPlaylistResult(name);
    }

    public List<String> getPlaylistNames()
    {
        return storage.getPlaylistNames();
    }

    public void createFolder()
    {
        storage.createFolder();
    }

    public boolean folderExists()
    {
        return storage.folderExists();
    }

    public void createPlaylist(String name) throws IOException
    {
        storage.createPlaylist(name);
    }

    public void deletePlaylist(String name) throws IOException
    {
        storage.deletePlaylist(name);
    }

    public void writePlaylist(String name, String text) throws IOException
    {
        storage.writePlaylist(name, text);
    }

    public PlaylistResult<Void> createPlaylistResult(String name)
    {
        return storage.createPlaylistResult(name);
    }

    public PlaylistResult<Void> deletePlaylistResult(String name)
    {
        return storage.deletePlaylistResult(name);
    }

    public PlaylistResult<Void> writePlaylistResult(String name, String text)
    {
        return storage.writePlaylistResult(name, text);
    }

    public PlaylistResult<Void> renamePlaylistResult(String oldName, String newName)
    {
        return storage.renamePlaylistResult(oldName, newName);
    }

    public PlaylistResult<AppendIfAbsentResult> appendItemIfAbsentResult(String name, String item)
    {
        return storage.appendItemIfAbsentResult(name, item);
    }

    public Optional<PlaylistError> getLastStorageError()
    {
        return storage.getLastStorageError();
    }

    public Playlist getPlaylist(String name)
    {
        PlaylistResult<Playlist> result = getPlaylistResult(name);
        return result.isSuccess() ? result.getValue() : null;
    }

    public PlaylistResult<Playlist> getPlaylistResult(String name)
    {
        PlaylistResult<List<String>> readResult = storage.readPlaylistResult(name);
        if (!readResult.isSuccess())
        {
            return PlaylistResult.failure(readResult.getError());
        }

        ParsedPlaylist parsed = PlaylistParser.parse(readResult.getValue(), true);
        return PlaylistResult.success(new Playlist(name, parsed.items(), parsed.shuffle()));
    }



    public class Playlist
    {
        private final String name;
        private final List<String> items;
        private final boolean shuffle;
        private final List<AudioTrack> tracks = new LinkedList<>();
        private final List<PlaylistLoadError> errors = new LinkedList<>();
        private boolean loaded = false;

        private Playlist(String name, List<String> items, boolean shuffle)
        {
            this.name = name;
            this.items = items;
            this.shuffle = shuffle;
        }

        public void loadTracks(AudioPlayerManager manager, Consumer<AudioTrack> consumer, Runnable callback)
        {
            if(loaded)
                return;
            loaded = true;
            if(items.isEmpty())
            {
                if(callback != null)
                    callback.run();
                return;
            }
            AtomicInteger pendingItems = new AtomicInteger(items.size());
            for(int i=0; i<items.size(); i++)
            {
                int index = i;
                manager.loadItemOrdered(name, items.get(i), new AudioLoadResultHandler()
                {
                    private void done()
                    {
                        if(pendingItems.decrementAndGet() == 0)
                        {
                            if(shuffle)
                                shuffleTracks();
                            if(callback != null)
                                callback.run();
                        }
                    }

                    @Override
                    public void trackLoaded(AudioTrack at)
                    {
                        if(config.isTooLong(at))
                            errors.add(new PlaylistLoadError(index, items.get(index), "This track is longer than the allowed maximum"));
                        else
                        {
                            at.setUserData(0L);
                            tracks.add(at);
                            consumer.accept(at);
                        }
                        done();
                    }

                    @Override
                    public void playlistLoaded(AudioPlaylist ap)
                    {
                        if(ap.isSearchResult())
                        {
                            trackLoaded(ap.getTracks().get(0));
                        }
                        else if(ap.getSelectedTrack()!=null)
                        {
                            trackLoaded(ap.getSelectedTrack());
                        }
                        else
                        {
                            List<AudioTrack> loaded = new ArrayList<>(ap.getTracks());
                            if(shuffle)
                                for(int first =0; first<loaded.size(); first++)
                                {
                                    int second = (int)(Math.random()*loaded.size());
                                    AudioTrack tmp = loaded.get(first);
                                    loaded.set(first, loaded.get(second));
                                    loaded.set(second, tmp);
                                }
                            loaded.removeIf(track -> config.isTooLong(track));
                            loaded.forEach(at -> at.setUserData(0L));
                            tracks.addAll(loaded);
                            loaded.forEach(at -> consumer.accept(at));
                        }
                        done();
                    }

                    @Override
                    public void noMatches()
                    {
                        errors.add(new PlaylistLoadError(index, items.get(index), "No matches found."));
                        done();
                    }

                    @Override
                    public void loadFailed(FriendlyException fe)
                    {
                        errors.add(new PlaylistLoadError(index, items.get(index), "Failed to load track: "+fe.getLocalizedMessage()));
                        done();
                    }
                });
            }
        }

        public void shuffleTracks()
        {
            PlaylistParser.shuffle(tracks);
        }

        public String getName()
        {
            return name;
        }

        public List<String> getItems()
        {
            return items;
        }

        public boolean isShuffle()
        {
            return shuffle;
        }

        public List<AudioTrack> getTracks()
        {
            return tracks;
        }

        public List<PlaylistLoadError> getErrors()
        {
            return errors;
        }
    }

    public class PlaylistLoadError
    {
        private final int number;
        private final String item;
        private final String reason;

        private PlaylistLoadError(int number, String item, String reason)
        {
            this.number = number;
            this.item = item;
            this.reason = reason;
        }

        public int getIndex()
        {
            return number;
        }

        public String getItem()
        {
            return item;
        }

        public String getReason()
        {
            return reason;
        }
    }
}
