package org.airsonic.player.service.playlist;

import chameleon.playlist.SpecificPlaylist;
import chameleon.playlist.xspf.Playlist;
import chameleon.playlist.xspf.StringContainer;
import org.airsonic.player.domain.MediaFile;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Component
public class XspfPlaylistImportHandler extends PlaylistImportHandler {
    @Override
    public boolean canHandle(Class<? extends SpecificPlaylist> playlistClass) {
        return Playlist.class.equals(playlistClass);
    }

    @Override
    public Pair<List<MediaFile>, List<String>> handle(SpecificPlaylist inputSpecificPlaylist, Path location) {
        List<MediaFile> mediaFiles = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        Playlist xspfPlaylist = (Playlist) inputSpecificPlaylist;

        xspfPlaylist.getTracks().forEach(track -> {
            for (StringContainer sc : track.getStringContainers()) {
                try {
                    String uri = sc.getText();
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
        return Pair.of(mediaFiles, errors);
    }

    @Override
    public int getOrder() {
        return 40;
    }
}
