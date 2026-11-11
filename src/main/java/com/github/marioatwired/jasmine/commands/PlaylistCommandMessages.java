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
package com.github.marioatwired.jasmine.commands;

import com.github.marioatwired.jasmine.service.dto.PlaylistMutationResult;

/**
 * Shared user-facing messages for playlist append operations.
 */
public final class PlaylistCommandMessages
{
    private PlaylistCommandMessages()
    {
    }

    public static String appendSummary(String playlistName, PlaylistMutationResult result)
    {
        String message = "Added " + count(result.addedItems, "item", "items")
                + " to playlist `" + playlistName + "`.";
        if (result.duplicateItems > 0)
        {
            message += " " + count(result.duplicateItems, "duplicate", "duplicates") + " skipped.";
        }
        return message;
    }

    public static String allDuplicates(String playlistName, int duplicates)
    {
        if (duplicates == 1)
        {
            return "The item is already in playlist `" + playlistName + "`.";
        }
        return "All " + duplicates + " items are already in playlist `" + playlistName + "`.";
    }

    private static String count(int amount, String singular, String plural)
    {
        return amount + " " + (amount == 1 ? singular : plural);
    }
}
