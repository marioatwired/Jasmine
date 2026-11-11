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
package com.github.marioatwired.jasmine.audio;

import com.github.marioatwired.jasmine.BotConfig;
import dev.lavalink.youtube.YoutubeAudioSourceManager;
import dev.lavalink.youtube.YoutubeSourceOptions;
import dev.lavalink.youtube.clients.Tv;
import dev.lavalink.youtube.clients.skeleton.Client;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for YouTube source setup and OAuth token handling.
 */
@DisplayName("AudioSource YouTube Tests")
class AudioSourceOAuthTest
{
    private final Logger logger = LoggerFactory.getLogger(AudioSourceOAuthTest.class);

    @Nested
    @DisplayName("OAuth token file")
    class OAuthTokenFileTests
    {
        @Test
        @DisplayName("returns null when the token file does not exist")
        void missingTokenFileReturnsNull(@TempDir Path tempDir)
        {
            assertNull(AudioSource.readOAuthToken(tempDir.resolve("missing.txt"), logger));
        }

        @Test
        @DisplayName("reads the token from the configured path")
        void readsTokenFromPath(@TempDir Path tempDir) throws Exception
        {
            Path tokenFile = tempDir.resolve("youtubetoken.txt");
            Files.writeString(tokenFile, "refresh-token");

            assertEquals("refresh-token", AudioSource.readOAuthToken(tokenFile, logger));
        }
    }

    @Nested
    @DisplayName("OAuth application")
    class ApplyOAuthTests
    {
        @Test
        @DisplayName("passes a null token to trigger the device flow")
        void nullTokenTriggersDeviceFlow()
        {
            YoutubeAudioSourceManager manager = mock(YoutubeAudioSourceManager.class);

            AudioSource.applyOAuth(manager, (String) null, logger);

            verify(manager).useOauth2(isNull(), eq(false));
        }

        @Test
        @DisplayName("passes an existing refresh token")
        void existingTokenIsPassed()
        {
            YoutubeAudioSourceManager manager = mock(YoutubeAudioSourceManager.class);

            AudioSource.applyOAuth(manager, "refresh-token", logger);

            verify(manager).useOauth2("refresh-token", false);
        }

        @Test
        @DisplayName("handles OAuth failures without crashing startup")
        void oauthFailureIsCaught()
        {
            YoutubeAudioSourceManager manager = mock(YoutubeAudioSourceManager.class);
            doThrow(new RuntimeException("boom")).when(manager).useOauth2(isNull(), anyBoolean());

            assertDoesNotThrow(() -> AudioSource.applyOAuth(manager, (String) null, logger));
        }
    }

    @Nested
    @DisplayName("Client selection")
    class ClientSelectionTests
    {
        @Test
        @DisplayName("prefers OAuth TV playback and includes fallback clients")
        void oauthClientsIncludeFallbacks()
        {
            Client[] clients = AudioSource.buildYoutubeClients(true, true);

            assertEquals(8, clients.length);
            String[] expectedTypes = {
                    "AndroidVrWithThumbnail",
                    "MWebWithThumbnail",
                    "WebWithThumbnail",
                    "Tv",
                    "TvHtml5SimplyWithThumbnail",
                    "AndroidVrWithThumbnail",
                    "MWebWithThumbnail",
                    "WebWithThumbnail"
            };
            for (int i = 0; i < expectedTypes.length; i++)
            {
                assertEquals(expectedTypes[i], clients[i].getClass().getSimpleName(),
                        "Unexpected client at index " + i);
            }

            for (int i = 0; i < 3; i++)
            {
                assertFalse(clients[i].getOptions().getPlayback(),
                        "Metadata client " + i + " must not handle playback");
            }
            for (int i = 3; i < clients.length; i++)
            {
                assertTrue(clients[i].getOptions().getPlayback(),
                        "Playback client " + i + " must handle playback");
            }
        }

        @Test
        @DisplayName("can keep the original five OAuth clients")
        void oauthFallbacksCanBeDisabled()
        {
            Client[] clients = AudioSource.buildYoutubeClients(true, false);

            assertEquals(5, clients.length);
            assertEquals("Tv", clients[3].getClass().getSimpleName());
            assertEquals("TvHtml5SimplyWithThumbnail", clients[4].getClass().getSimpleName());
        }

        @Test
        @DisplayName("non-OAuth clients all support playback")
        void nonOauthClientsSupportPlayback()
        {
            Client[] clients = AudioSource.buildYoutubeClients(false, true);

            assertEquals(4, clients.length);
            for (Client client : clients)
            {
                assertTrue(client.getOptions().getPlayback());
            }
        }

        @Test
        @DisplayName("the TV client uses the PlayStation workaround")
        void tvClientUsesPlayStationUserAgent()
        {
            AudioSource.applyTvUserAgentWorkaround();

            assertTrue(Tv.BASE_CONFIG.getUserAgent().contains("PlayStation"));
        }
    }

    @Nested
    @DisplayName("Source options")
    class SourceOptionsTests
    {
        @Test
        @DisplayName("uses the configured remote cipher values")
        void remoteCipherValuesAreApplied()
        {
            BotConfig config = mockConfig(true, "https://cipher.example/", "secret", "jasmine-test");

            YoutubeSourceOptions options = AudioSource.buildYoutubeOptions(config);

            assertEquals("https://cipher.example/", options.getRemoteCipherUrl());
            assertEquals("secret", options.getRemoteCipherPassword());
            assertEquals("jasmine-test", options.getRemoteCipherUserAgent());
        }

        @Test
        @DisplayName("blank remote cipher URL disables the remote cipher")
        void blankRemoteCipherUrlDisablesRemoteCipher()
        {
            BotConfig config = mockConfig(true, "   ", "secret", "jasmine-test");

            YoutubeSourceOptions options = AudioSource.buildYoutubeOptions(config);

            assertNull(options.getRemoteCipherUrl());
            assertNull(options.getRemoteCipherPassword());
            assertNull(options.getRemoteCipherUserAgent());
        }

        @Test
        @DisplayName("invalid remote cipher URL is ignored")
        void invalidRemoteCipherUrlIsIgnored()
        {
            BotConfig config = mockConfig(true, "not a URL", "secret", "jasmine-test");

            YoutubeSourceOptions options = AudioSource.buildYoutubeOptions(config);

            assertNull(options.getRemoteCipherUrl());
        }

        @Test
        @DisplayName("blank password and user agent use safe defaults")
        void blankOptionalRemoteCipherValuesUseDefaults()
        {
            BotConfig config = mockConfig(true, "https://cipher.example/", " ", " ");

            YoutubeSourceOptions options = AudioSource.buildYoutubeOptions(config);

            assertEquals("https://cipher.example/", options.getRemoteCipherUrl());
            assertNull(options.getRemoteCipherPassword());
            assertEquals("jasmine", options.getRemoteCipherUserAgent());
        }

        @Test
        @DisplayName("remote cipher is not configured when OAuth is disabled")
        void remoteCipherRequiresOAuth()
        {
            BotConfig config = mockConfig(false, "https://cipher.example/", "secret", "jasmine-test");

            YoutubeSourceOptions options = AudioSource.buildYoutubeOptions(config);

            assertNull(options.getRemoteCipherUrl());
        }

        @Test
        @DisplayName("accepts only HTTP and HTTPS URLs")
        void httpUrlValidation()
        {
            assertTrue(AudioSource.isHttpUrl("https://cipher.example/"));
            assertTrue(AudioSource.isHttpUrl("http://cipher.example/"));
            assertFalse(AudioSource.isHttpUrl("ftp://cipher.example/"));
            assertFalse(AudioSource.isHttpUrl("cipher.example"));
            assertFalse(AudioSource.isHttpUrl("https://"));
            assertFalse(AudioSource.isHttpUrl(" "));
            assertFalse(AudioSource.isHttpUrl(null));
        }
    }

    private BotConfig mockConfig(boolean useOauth, String remoteCipherUrl,
                                 String remoteCipherPassword, String remoteCipherUserAgent)
    {
        BotConfig config = mock(BotConfig.class);
        when(config.useYouTubeOauth()).thenReturn(useOauth);
        when(config.useYouTubeFallbackClients()).thenReturn(true);
        when(config.getMaxYTPlaylistPages()).thenReturn(10);
        when(config.getYouTubeRemoteCipherUrl()).thenReturn(remoteCipherUrl);
        when(config.getYouTubeRemoteCipherPassword()).thenReturn(remoteCipherPassword);
        when(config.getYouTubeRemoteCipherUserAgent()).thenReturn(remoteCipherUserAgent);
        return config;
    }
}
