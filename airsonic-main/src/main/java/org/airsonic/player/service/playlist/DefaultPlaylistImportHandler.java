package org.airsonic.player.service.playlist;

import chameleon.playlist.*;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.service.MediaFileService;
import org.airsonic.player.service.SettingsService;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class DefaultPlaylistImportHandler implements PlaylistImportHandler {

    @Autowired
    MediaFileService mediaFileService;

    @Autowired
    SettingsService settingsService;

    @Override
    public boolean canHandle(Class<? extends SpecificPlaylist> playlistClass) {
        return true;
    }

    @Override
    public Pair<List<MediaFile>, List<String>> handle(SpecificPlaylist inputSpecificPlaylist, Path location) {
        List<MediaFile> mediaFiles = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        if (location == null) {
            location = Optional.ofNullable(settingsService.getPlaylistFolder()).map(Paths::get).orElse(null);
        }
        if (location == null) {
            location = Paths.get(".").toAbsolutePath().normalize();
        }
        Path playlistFolder = location;
        try {
            inputSpecificPlaylist.toPlaylist().acceptDown(new BasePlaylistVisitor() {
                @Override
                public void beginVisitMedia(Media media) {
                    try {
                        // Cannot use uri directly because it resolves against war root
                        // URI uri = media.getSource().getURI();
                        String uri = media.getSource().toString();
                        Path file = Paths.get(uri);
                        Path resolvedFile = playlistFolder.resolve(file).normalize();
                        MediaFile mediaFile = mediaFileService.getMediaFile(resolvedFile);
                        if (mediaFile != null) {
                            mediaFiles.add(mediaFile);
                        } else {
                            errors.add("Cannot find media file " + file + "[" + resolvedFile + "]");
                        }
                    } catch (Exception e) {
                        errors.add(e.getMessage());
                    }
                }
            });
        } catch (Exception e) {
            errors.add(e.getMessage());
        }

        return Pair.of(mediaFiles, errors);
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
