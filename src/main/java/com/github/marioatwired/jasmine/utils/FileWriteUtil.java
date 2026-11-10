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
package com.github.marioatwired.jasmine.utils;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

/**
 * Writes files that may be read-only while still allowing the containing
 * directory to be writable, such as bind-mounted files in Docker.
 */
public final class FileWriteUtil
{
    private FileWriteUtil()
    {
    }

    public static void write(Path path, byte[] bytes) throws IOException
    {
        try
        {
            Files.write(path, bytes);
        }
        catch (AccessDeniedException ex)
        {
            replaceFile(path, bytes);
        }
    }

    private static void replaceFile(Path path, byte[] bytes) throws IOException
    {
        Path target = path.toAbsolutePath().normalize();
        Path parent = target.getParent();
        if (parent == null)
        {
            throw new IOException("File has no parent directory: " + target);
        }

        Path temporary = Files.createTempFile(parent, target.getFileName().toString(), ".tmp");
        try
        {
            Files.write(temporary, bytes);
            copyPosixPermissions(target, temporary);
            try
            {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            }
            catch (AtomicMoveNotSupportedException ex)
            {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        finally
        {
            Files.deleteIfExists(temporary);
        }
    }

    private static void copyPosixPermissions(Path source, Path target)
    {
        try
        {
            if (!Files.exists(source))
            {
                return;
            }
            Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(source);
            Files.setPosixFilePermissions(target, permissions);
        }
        catch (IOException | UnsupportedOperationException ignored)
        {
            // POSIX permissions are not available on every platform/filesystem.
        }
    }
}
