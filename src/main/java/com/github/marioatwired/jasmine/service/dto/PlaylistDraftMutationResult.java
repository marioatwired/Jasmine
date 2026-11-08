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
 * Result of a playlist draft mutation.
 */
public class PlaylistDraftMutationResult
{
    public final boolean success;
    public final String errorMessage;
    public final int totalItems;
    public final boolean dirty;
    public final int revision;

    private PlaylistDraftMutationResult(boolean success, String errorMessage, int totalItems,
                                        boolean dirty, int revision)
    {
        this.success = success;
        this.errorMessage = errorMessage;
        this.totalItems = totalItems;
        this.dirty = dirty;
        this.revision = revision;
    }

    public static PlaylistDraftMutationResult success(int totalItems, boolean dirty, int revision)
    {
        return new PlaylistDraftMutationResult(true, null, totalItems, dirty, revision);
    }

    public static PlaylistDraftMutationResult error(String errorMessage)
    {
        return new PlaylistDraftMutationResult(false, errorMessage, 0, false, 0);
    }
}
