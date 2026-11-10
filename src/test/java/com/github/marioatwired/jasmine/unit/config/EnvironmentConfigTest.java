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
package com.github.marioatwired.jasmine.unit.config;

import com.github.marioatwired.jasmine.config.EnvironmentConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("EnvironmentConfig Tests")
class EnvironmentConfigTest
{
    @TempDir
    Path tempDir;

    @Test
    @DisplayName("reads direct token and owner variables")
    void readsDirectVariables()
    {
        EnvironmentConfig.Credentials credentials = EnvironmentConfig.from(Map.of(
                EnvironmentConfig.TOKEN_ENV, "token-value",
                EnvironmentConfig.OWNER_ENV, "123456789"
        ));

        assertEquals("token-value", credentials.token());
        assertEquals(123456789L, credentials.owner());
        assertTrue(credentials.isComplete());
    }

    @Test
    @DisplayName("supports DISCORD_TOKEN and DISCORD_OWNER aliases")
    void supportsLegacyAliases()
    {
        EnvironmentConfig.Credentials credentials = EnvironmentConfig.from(Map.of(
                EnvironmentConfig.LEGACY_TOKEN_ENV, "legacy-token",
                EnvironmentConfig.LEGACY_OWNER_ENV, "42"
        ));

        assertEquals("legacy-token", credentials.token());
        assertEquals(42L, credentials.owner());
    }

    @Test
    @DisplayName("reads token and owner from secret files")
    void readsSecretFiles() throws IOException
    {
        Path tokenFile = tempDir.resolve("token");
        Path ownerFile = tempDir.resolve("owner");
        Files.writeString(tokenFile, "file-token\n");
        Files.writeString(ownerFile, "987654321\n");

        Map<String, String> environment = new HashMap<>();
        environment.put(EnvironmentConfig.TOKEN_FILE_ENV, tokenFile.toString());
        environment.put(EnvironmentConfig.OWNER_FILE_ENV, ownerFile.toString());

        EnvironmentConfig.Credentials credentials = EnvironmentConfig.from(environment);

        assertEquals("file-token", credentials.token());
        assertEquals(987654321L, credentials.owner());
    }

    @Test
    @DisplayName("direct variables take precedence over secret files")
    void directValuesTakePrecedence() throws IOException
    {
        Path tokenFile = tempDir.resolve("token");
        Files.writeString(tokenFile, "file-token");

        Map<String, String> environment = new HashMap<>();
        environment.put(EnvironmentConfig.TOKEN_ENV, "direct-token");
        environment.put(EnvironmentConfig.TOKEN_FILE_ENV, tokenFile.toString());

        assertEquals("direct-token", EnvironmentConfig.from(environment).token());
    }

    @Test
    @DisplayName("invalid owner values are ignored")
    void invalidOwnerIsIgnored()
    {
        EnvironmentConfig.Credentials credentials = EnvironmentConfig.from(Map.of(
                EnvironmentConfig.OWNER_ENV, "not-a-number"
        ));

        assertNull(credentials.owner());
        assertFalse(credentials.hasAny());
    }

    @Test
    @DisplayName("missing secret file is ignored")
    void missingSecretFileIsIgnored()
    {
        EnvironmentConfig.Credentials credentials = EnvironmentConfig.from(Map.of(
                EnvironmentConfig.TOKEN_FILE_ENV, tempDir.resolve("missing").toString()
        ));

        assertNull(credentials.token());
    }
}
