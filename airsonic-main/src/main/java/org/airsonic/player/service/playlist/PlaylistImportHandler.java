package org.airsonic.player.service.playlist;

import chameleon.playlist.SpecificPlaylist;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.MediaFile.MediaType;
import org.airsonic.player.service.MediaFileService;
import org.airsonic.player.service.SettingsService;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

public abstract class PlaylistImportHandler implements Ordered {
    @Autowired
    MediaFileService mediaFileService;

    @Autowired
    SettingsService settingsService;

    abstract public boolean canHandle(Class<? extends SpecificPlaylist> playlistClass);

    abstract public Pair<List<MediaFile>, List<String>> handle(SpecificPlaylist inputSpecificPlaylist, Path location);

    List<MediaFile> getMediaFiles(String pathInPlaylist) {
        if (StringUtils.isNotBlank(pathInPlaylist)) {
            Path path = Paths.get(pathInPlaylist);
            if (path.isAbsolute()) {
                // there's only one path to look up
                MediaFile m = mediaFileService.getMediaFile(path);
                if (m != null) {
                    return singletonList(m);
                }
            } else {
                // need to resolve the root
                List<MediaFile> possibles = new ArrayList<>();

                // look relative to playlist folder first
                Path playlistFolder = Optional.ofNullable(settingsService.getPlaylistFolder()).map(Paths::get).orElse(null);
                if (playlistFolder != null) {
                    Path resolvedFile = playlistFolder.resolve(path).normalize();
                    MediaFile mediaFile = mediaFileService.getMediaFile(resolvedFile);
                    if (mediaFile != null) {
                        possibles.add(mediaFile);
                    }
                }

                // look relative to all music folders
                possibles.addAll(mediaFileService.getMediaFilesByRelativePath(path).stream()
                        .filter(m -> !EnumSet.of(MediaType.DIRECTORY, MediaType.ALBUM).contains(m.getMediaType()))
                        .collect(toList()));

                // look relative to home
                Path resolvedFile = Paths.get(".").toAbsolutePath().resolve(path).normalize();
                MediaFile mediaFile = mediaFileService.getMediaFile(resolvedFile);
                if (mediaFile != null) {
                    possibles.add(mediaFile);
                }

                return possibles;
            }
        }

        return emptyList();
    }
}
