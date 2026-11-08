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
package com.github.marioatwired.jasmine.queue;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.marioatwired.jasmine.audio.RequestMetadata;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Serializable snapshot of the playback queues.
 * <p>
 * Track objects cannot be serialized directly, so only their URI, position,
 * and request metadata are stored. Tracks are reloaded through Lavaplayer when
 * the queue is restored.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
final class QueueState
{
    static final int CURRENT_VERSION = 1;

    public final int version;
    public final Map<Long, GuildQueue> guilds;

    @JsonCreator
    QueueState(@JsonProperty("version") Integer version,
               @JsonProperty("guilds") Map<Long, GuildQueue> guilds)
    {
        this.version = version == null ? CURRENT_VERSION : version;
        this.guilds = guilds == null ? Map.of() : guilds;
    }

    QueueState(Map<Long, GuildQueue> guilds)
    {
        this(CURRENT_VERSION, guilds);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static final class GuildQueue
    {
        public final StoredTrack current;
        public final List<StoredTrack> queue;

        @JsonCreator
        GuildQueue(@JsonProperty("current") StoredTrack current,
                   @JsonProperty("queue") List<StoredTrack> queue)
        {
            this.current = current;
            this.queue = queue == null
                    ? List.of()
                    : queue.stream().filter(Objects::nonNull).toList();
        }

        boolean isEmpty()
        {
            return current == null && queue.isEmpty();
        }

        List<StoredTrack> orderedTracks()
        {
            if (current == null)
                return queue;

            List<StoredTrack> ordered = new ArrayList<>(queue.size() + 1);
            ordered.add(current);
            ordered.addAll(queue);
            return Collections.unmodifiableList(ordered);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static final class StoredTrack
    {
        public final String uri;
        public final long position;
        public final RequestMetadata metadata;

        @JsonCreator
        StoredTrack(@JsonProperty("uri") String uri,
                    @JsonProperty("position") Long position,
                    @JsonProperty("metadata") RequestMetadata metadata)
        {
            this.uri = uri;
            this.position = position == null ? 0L : Math.max(0L, position);
            this.metadata = metadata;
        }

        StoredTrack(String uri, long position, RequestMetadata metadata)
        {
            this(uri, Long.valueOf(position), metadata);
        }

        boolean hasUri()
        {
            return uri != null && !uri.isBlank();
        }
    }
}
