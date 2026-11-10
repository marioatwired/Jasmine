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
package com.github.marioatwired.jasmine.unit.utils;

import com.github.marioatwired.jasmine.utils.FileWriteUtil;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("FileWriteUtil Tests")
class FileWriteUtilTest
{
    @TempDir
    Path tempDir;

    @Test
    @DisplayName("write() creates and overwrites a writable file")
    void writeCreatesAndOverwritesFile() throws IOException
    {
        Path file = tempDir.resolve("settings.json");

        FileWriteUtil.write(file, "first".getBytes(StandardCharsets.UTF_8));
        FileWriteUtil.write(file, "second".getBytes(StandardCharsets.UTF_8));

        assertEquals("second", Files.readString(file, StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("write() replaces a read-only file through the atomic fallback")
    void writeReplacesReadOnlyFile() throws IOException
    {
        Path file = tempDir.resolve("read-only.json");
        Files.writeString(file, "old", StandardCharsets.UTF_8);
        try
        {
            Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("r--r--r--"));
        }
        catch (UnsupportedOperationException ex)
        {
            Assumptions.assumeTrue(false, "POSIX permissions are not supported");
        }

        FileWriteUtil.write(file, "new".getBytes(StandardCharsets.UTF_8));

        assertEquals("new", Files.readString(file, StandardCharsets.UTF_8));
    }
}
