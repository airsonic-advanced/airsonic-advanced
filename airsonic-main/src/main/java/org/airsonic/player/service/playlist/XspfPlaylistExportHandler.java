package org.airsonic.player.service.playlist;

import chameleon.playlist.SpecificPlaylist;
import chameleon.playlist.SpecificPlaylistProvider;
import chameleon.playlist.xspf.Location;
import chameleon.playlist.xspf.Track;
import chameleon.playlist.xspf.XspfProvider;
import org.airsonic.player.dao.MediaFileDao;
import org.airsonic.player.dao.PlaylistDao;
import org.airsonic.player.domain.CoverArt.EntityType;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.MusicFolder;
import org.airsonic.player.domain.Playlist;
import org.airsonic.player.service.CoverArtService;
import org.airsonic.player.service.MediaFolderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Component
public class XspfPlaylistExportHandler implements PlaylistExportHandler {

    @Autowired
    MediaFileDao mediaFileDao;

    @Autowired
    PlaylistDao playlistDao;

    @Autowired
    MediaFolderService mediaFolderService;

    @Autowired
    CoverArtService coverArtService;

    @Override
    public boolean canHandle(Class<? extends SpecificPlaylistProvider> providerClass) {
        return XspfProvider.class.equals(providerClass);
    }

    @Override
    public SpecificPlaylist handle(int id, SpecificPlaylistProvider provider) {
        return createXsfpPlaylistFromDBId(id);
    }

    chameleon.playlist.xspf.Playlist createXsfpPlaylistFromDBId(int id) {
        chameleon.playlist.xspf.Playlist newPlaylist = new chameleon.playlist.xspf.Playlist();
        Playlist playlist = playlistDao.getPlaylist(id);
        newPlaylist.setTitle(playlist.getName());
        newPlaylist.setCreator("Airsonic user " + playlist.getUsername());
        newPlaylist.setDate(Date.from(Instant.now())); //TODO switch to Instant upstream
        List<MediaFile> files = mediaFileDao.getFilesInPlaylist(id);

        files.stream().map(mediaFile -> {
            MusicFolder folder = mediaFolderService.getMusicFolderById(mediaFile.getFolderId());
            Track track = new Track();
            track.setTrackNumber(mediaFile.getTrackNumber());
            track.setCreator(mediaFile.getArtist());
            track.setTitle(mediaFile.getTitle());
            track.setAlbum(mediaFile.getAlbumName());
            track.setDuration((int) Math.round(mediaFile.getDuration())); // TODO switch to Double upstream
            track.setImage(Optional.ofNullable(coverArtService.getFullPath(EntityType.MEDIA_FILE, mediaFile.getId())).map(p -> p.toString()).orElse(null));
            Location location = new Location();
            location.setText(mediaFile.getFullPath(folder.getPath()).toString());
            track.getStringContainers().add(location);
            return track;
        }).forEach(newPlaylist::addTrack);

        return newPlaylist;
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
