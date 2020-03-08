package org.airsonic.player.util;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.stream.Stream;

public class FileUtils {
    public static void copyRecursively(Path from, Path to) throws IOException {
        try (final Stream<Path> sources = Files.walk(from)) {
            sources.forEach(src -> {
                final Path dest = to.resolve(from.relativize(src));
                try {
                    if (Files.isDirectory(src)) {
                        if (Files.notExists(dest)) {
                            Files.createDirectories(dest);
                        }
                    } else {
                        Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
                    }
                } catch (IOException e) {
                    throw new RuntimeException("Failed to unzip file.", e);
                }
            });
        }
    }

    public static void copyRecursively(final URL originUrl, final Path destination)
            throws IOException, URISyntaxException {
        copyRecursively(Paths.get(originUrl.toURI()), destination);
    }
}
