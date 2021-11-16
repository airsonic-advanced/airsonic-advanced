/*
 This file is part of Airsonic.

 Airsonic is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Airsonic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Airsonic.  If not, see <http://www.gnu.org/licenses/>.

 Copyright 2016 (C) Airsonic Authors
 Based upon Subsonic, Copyright 2009 (C) Sindre Mehus
 */
package org.airsonic.player.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Comparator;
import java.util.stream.Stream;

import static org.airsonic.player.util.LambdaUtils.uncheckConsumer;

/**
 * Miscellaneous file utility methods.
 *
 * @author Sindre Mehus
 */
public final class FileUtil {

    private static final Logger LOG = LoggerFactory.getLogger(FileUtil.class);

    /**
     * Disallow external instantiation.
     */
    private FileUtil() {
    }

    public static Instant lastModified(final Path file) {
        try {
            return Files.getLastModifiedTime(file).toInstant();
        } catch (IOException e) {
            LOG.warn("Could not get file modify date for {}", file.toString(), e);
            return Instant.now();
        }
    }

    public static long size(final Path file) {
        try {
            return Files.size(file);
        } catch (IOException e) {
            LOG.warn("Could not get file size for {}", file.toString(), e);
            return 0;
        }
    }

    public static boolean delete(Path fileOrFolder) {
        try (Stream<Path> walk = Files.walk(fileOrFolder)) {
            walk.sorted(Comparator.reverseOrder())
                .forEach(uncheckConsumer(Files::deleteIfExists));

            return true;
        } catch (NoSuchFileException e) {
            LOG.debug("Could not delete file/folder {}", fileOrFolder, e);
            return false;
        } catch (Exception e) {
            LOG.warn("Could not delete file/folder {}", fileOrFolder, e);
            return false;
        }
    }

    /**
     * Returns a short path for the given file.  The path consists of the name
     * of the parent directory and the given file.
     */
    public static String getShortPath(Path file) {
        if (file == null) {
            return null;
        }

        Path parent = file.getParent();
        if (parent == null) {
            return file.getFileName().toString();
        }
        return parent.getFileName().toString() + File.separator + file.getFileName().toString();
    }

    /**
     * Closes the "closable", ignoring any excepetions.
     *
     * @param closeable The Closable to close, may be {@code null}.
     */
    public static void closeQuietly(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                // Ignored
            }
        }
    }
}
