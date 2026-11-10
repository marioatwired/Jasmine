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
package com.github.marioatwired.jasmine.service.dto;

/**
 * Result of a direct saved-playlist mutation.
 */
public class PlaylistMutationResult
{
    public final boolean success;
    public final String errorMessage;
    public final int totalItems;
    public final int addedItems;
    public final int duplicateItems;

    private PlaylistMutationResult(boolean success, String errorMessage, int totalItems,
                                   int addedItems, int duplicateItems)
    {
        this.success = success;
        this.errorMessage = errorMessage;
        this.totalItems = totalItems;
        this.addedItems = addedItems;
        this.duplicateItems = duplicateItems;
    }

    public static PlaylistMutationResult success(int totalItems)
    {
        return new PlaylistMutationResult(true, null, totalItems, 0, 0);
    }

    public static PlaylistMutationResult appendSuccess(int totalItems, int addedItems, int duplicateItems)
    {
        return new PlaylistMutationResult(true, null, totalItems, addedItems, duplicateItems);
    }

    public static PlaylistMutationResult error(String errorMessage)
    {
        return new PlaylistMutationResult(false, errorMessage, 0, 0, 0);
    }
}
