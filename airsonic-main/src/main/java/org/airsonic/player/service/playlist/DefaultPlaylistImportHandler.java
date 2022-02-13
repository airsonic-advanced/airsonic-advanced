package org.airsonic.player.service.playlist;

import chameleon.playlist.*;
import org.airsonic.player.domain.MediaFile;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Component
public class DefaultPlaylistImportHandler extends PlaylistImportHandler {
    @Override
    public boolean canHandle(Class<? extends SpecificPlaylist> playlistClass) {
        return true;
    }

    @Override
    public Pair<List<MediaFile>, List<String>> handle(SpecificPlaylist inputSpecificPlaylist, Path location) {
        List<MediaFile> mediaFiles = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        try {
            inputSpecificPlaylist.toPlaylist().acceptDown(new BasePlaylistVisitor() {
                @Override
                public void beginVisitMedia(Media media) {
                    try {
                        // Cannot use uri directly because it resolves against war root
                        // URI uri = media.getSource().getURI();
                        String uri = media.getSource().toString();
                        List<MediaFile> possibles = getMediaFiles(uri);
                        if (possibles.isEmpty()) {
                            errors.add("Cannot find media file " + uri);
                        } else {
                            mediaFiles.addAll(possibles);
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
