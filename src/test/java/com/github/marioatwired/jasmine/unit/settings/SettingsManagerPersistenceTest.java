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
package com.github.marioatwired.jasmine.unit.settings;

import com.github.marioatwired.jasmine.settings.SettingsManager;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;

import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("SettingsManager Persistence Tests")
class SettingsManagerPersistenceTest
{
    @TempDir
    Path tempDir;

    @Test
    @DisplayName("writeSettings() replaces a read-only serversettings.json")
    void writeSettingsReplacesReadOnlyFile() throws Exception
    {
        Path settingsFile = tempDir.resolve("serversettings.json");
        Files.writeString(settingsFile, "{}", StandardCharsets.UTF_8);
        try
        {
            Files.setPosixFilePermissions(settingsFile, PosixFilePermissions.fromString("r--r--r--"));
        }
        catch (UnsupportedOperationException ex)
        {
            Assumptions.assumeTrue(false, "POSIX permissions are not supported");
        }

        Constructor<SettingsManager> constructor = SettingsManager.class.getDeclaredConstructor(Path.class);
        constructor.setAccessible(true);
        SettingsManager manager = constructor.newInstance(settingsFile);
        manager.getSettings(123L).setVolume(42);

        String content = Files.readString(settingsFile, StandardCharsets.UTF_8);
        assertTrue(content.contains("\"123\""));
        assertTrue(content.contains("\"volume\""));
        assertTrue(content.contains("42"));
    }
}
