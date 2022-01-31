package org.airsonic.player.service.playlist;

import chameleon.playlist.SpecificPlaylist;
import chameleon.playlist.xspf.Playlist;
import chameleon.playlist.xspf.StringContainer;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.service.MediaFileService;
import org.airsonic.player.service.SettingsService;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class XspfPlaylistImportHandler implements PlaylistImportHandler {

    @Autowired
    MediaFileService mediaFileService;

    @Autowired
    SettingsService settingsService;

    @Override
    public boolean canHandle(Class<? extends SpecificPlaylist> playlistClass) {
        return Playlist.class.equals(playlistClass);
    }

    @Override
    public Pair<List<MediaFile>, List<String>> handle(SpecificPlaylist inputSpecificPlaylist, Path location) {
        List<MediaFile> mediaFiles = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        Playlist xspfPlaylist = (Playlist) inputSpecificPlaylist;
        if (location == null) {
            location = Optional.ofNullable(settingsService.getPlaylistFolder()).map(Paths::get).orElse(null);
        }
        if (location == null) {
            location = Paths.get(".").toAbsolutePath().normalize();
        }
        Path playlistFolder = location;
        xspfPlaylist.getTracks().forEach(track -> {
            MediaFile mediaFile = null;
            for (StringContainer sc : track.getStringContainers()) {
                try {
                    Path file = Paths.get(sc.getText());
                    Path resolvedFile = playlistFolder.resolve(file).normalize();
                    mediaFile = mediaFileService.getMediaFile(resolvedFile);
                } catch (Exception ignored) {
                }
            }
            if (mediaFile != null) {
                mediaFiles.add(mediaFile);
            } else {
                String errorMsg = "Could not find media file matching ";
                try {
                    errorMsg += track.getStringContainers().stream().map(StringContainer::getText).collect(Collectors.joining(","));
                } catch (Exception ignored) {}
                errors.add(errorMsg);
            }
        });
        return Pair.of(mediaFiles, errors);
    }

    @Override
    public int getOrder() {
        return 40;
    }
}
