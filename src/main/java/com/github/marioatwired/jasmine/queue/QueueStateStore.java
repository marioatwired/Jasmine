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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Map;

/**
 * File-system persistence for {@link QueueState}.
 * <p>
 * Writes use a temporary file in the same directory followed by an atomic move,
 * so a crash cannot leave a half-written queue file.
 */
final class QueueStateStore
{
    private static final Logger LOG = LoggerFactory.getLogger(QueueStateStore.class);

    private final Path file;
    private final ObjectMapper mapper = new ObjectMapper()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    QueueStateStore(Path file)
    {
        this.file = file.toAbsolutePath().normalize();
    }

    Path getFile()
    {
        return file;
    }

    QueueState load()
    {
        byte[] raw = readRaw();
        if (raw == null || raw.length == 0)
            return new QueueState(Map.of());

        try
        {
            QueueState state = mapper.readValue(raw, QueueState.class);
            return state != null ? state : new QueueState(Map.of());
        }
        catch (IOException ex)
        {
            LOG.warn("Could not read queue state from {}: {}", file, ex.getMessage());
            return new QueueState(Map.of());
        }
    }

    byte[] readRaw()
    {
        if (!Files.exists(file))
            return null;

        try
        {
            return Files.readAllBytes(file);
        }
        catch (IOException ex)
        {
            LOG.warn("Could not read queue state from {}: {}", file, ex.getMessage());
            return null;
        }
    }

    byte[] serialize(QueueState state) throws JsonProcessingException
    {
        byte[] json = mapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(state);
        byte[] withNewline = Arrays.copyOf(json, json.length + 1);
        withNewline[withNewline.length - 1] = '\n';
        return withNewline;
    }

    void write(byte[] bytes) throws IOException
    {
        Path parent = file.getParent();
        if (parent != null)
            Files.createDirectories(parent);

        Path temporary = Files.createTempFile(parent, file.getFileName().toString(), ".tmp");
        try
        {
            Files.write(temporary, bytes);
            try
            {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            }
            catch (AtomicMoveNotSupportedException ex)
            {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        finally
        {
            Files.deleteIfExists(temporary);
        }
    }
}
