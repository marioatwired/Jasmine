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
package com.github.marioatwired.jasmine.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Reads credentials from container-friendly environment variables.
 * Direct values take precedence over secret files.
 */
public final class EnvironmentConfig
{
    private static final Logger LOG = LoggerFactory.getLogger(EnvironmentConfig.class);

    public static final String TOKEN_ENV = "JASMINE_TOKEN";
    public static final String TOKEN_FILE_ENV = "JASMINE_TOKEN_FILE";
    public static final String OWNER_ENV = "JASMINE_OWNER";
    public static final String OWNER_FILE_ENV = "JASMINE_OWNER_FILE";
    public static final String LEGACY_TOKEN_ENV = "DISCORD_TOKEN";
    public static final String LEGACY_OWNER_ENV = "DISCORD_OWNER";

    private EnvironmentConfig()
    {
    }

    public static Credentials from(Map<String, String> environment)
    {
        String token = firstNonBlank(environment.get(TOKEN_ENV), environment.get(LEGACY_TOKEN_ENV));
        if (token == null)
        {
            token = readSecret(environment.get(TOKEN_FILE_ENV), TOKEN_FILE_ENV);
        }

        String ownerValue = firstNonBlank(environment.get(OWNER_ENV), environment.get(LEGACY_OWNER_ENV));
        if (ownerValue == null)
        {
            ownerValue = readSecret(environment.get(OWNER_FILE_ENV), OWNER_FILE_ENV);
        }

        Long owner = parseOwner(ownerValue);
        return new Credentials(token, owner);
    }

    private static Long parseOwner(String value)
    {
        if (value == null)
        {
            return null;
        }
        try
        {
            long owner = Long.parseLong(value);
            return owner > 0 ? owner : null;
        }
        catch (NumberFormatException ex)
        {
            LOG.warn("Ignoring invalid {} value: {}", OWNER_ENV, value);
            return null;
        }
    }

    private static String readSecret(String path, String environmentName)
    {
        if (path == null || path.isBlank())
        {
            return null;
        }
        try
        {
            String value = Files.readString(Path.of(path.trim()), StandardCharsets.UTF_8).trim();
            return value.isEmpty() ? null : value;
        }
        catch (IOException ex)
        {
            LOG.warn("Could not read {} from {}: {}", environmentName, path, ex.getMessage());
            return null;
        }
    }

    private static String firstNonBlank(String... values)
    {
        for (String value : values)
        {
            if (value != null && !value.isBlank())
            {
                return value.trim();
            }
        }
        return null;
    }

    public record Credentials(String token, Long owner)
    {
        public boolean hasAny()
        {
            return token != null || owner != null;
        }

        public boolean isComplete()
        {
            return token != null && owner != null;
        }
    }
}
