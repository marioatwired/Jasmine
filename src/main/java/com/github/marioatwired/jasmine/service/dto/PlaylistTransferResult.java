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
package com.github.marioatwired.jasmine.service.dto;

/**
 * Result of importing or exporting a playlist file.
 *
 * @param success      whether the operation succeeded
 * @param message      user-facing result message
 * @param playlistName sanitized playlist name
 * @param entries      number of imported/exported entries
 * @param content      exported file bytes, or {@code null} for imports
 */
public record PlaylistTransferResult(
        boolean success,
        String message,
        String playlistName,
        int entries,
        byte[] content)
{
    public static PlaylistTransferResult success(String message, String playlistName, int entries,
                                                 byte[] content)
    {
        return new PlaylistTransferResult(true, message, playlistName, entries, content);
    }

    public static PlaylistTransferResult failure(String message)
    {
        return new PlaylistTransferResult(false, message, null, 0, null);
    }
}
