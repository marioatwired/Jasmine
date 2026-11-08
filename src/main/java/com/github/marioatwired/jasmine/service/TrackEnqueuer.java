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

import com.github.marioatwired.jasmine.service.dto.TrackAddResult;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

/**
 * Narrow interface for adding tracks to a guild queue.
 * <p>
 * Playback and audio-loading code depend on this interface instead of the
 * {@link MusicService} facade. This keeps the queue-writing API explicit and
 * avoids a dependency on the entire music-service surface.
 */
public interface TrackEnqueuer
{
    TrackAddResult addTrackToQueue(Guild guild, Member member, AudioTrack track,
                                   String queryArgs, TextChannel channel);

    TrackAddResult addTrackToFront(Guild guild, Member member, AudioTrack track,
                                   String queryArgs, TextChannel channel);

    boolean isTooLong(AudioTrack track);

    String formatTooLongError(AudioTrack track);

    String formatTrackAddedMessage(String title, long duration, int position);
}
