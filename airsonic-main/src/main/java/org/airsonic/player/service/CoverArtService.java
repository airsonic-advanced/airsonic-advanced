package org.airsonic.player.service;

import org.airsonic.player.dao.CoverArtDao;
import org.airsonic.player.domain.Album;
import org.airsonic.player.domain.Artist;
import org.airsonic.player.domain.CoverArt;
import org.airsonic.player.domain.CoverArt.EntityType;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.MusicFolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.Optional;

@Service
@CacheConfig(cacheNames = "coverArtCache")
public class CoverArtService {
    @Autowired
    CoverArtDao coverArtDao;
    @Autowired
    MediaFolderService mediaFolderService;

    public final static CoverArt NULL_ART = new CoverArt(-2, EntityType.NONE, null, null, false);

    public void upsert(EntityType type, int id, String path, Integer folderId, boolean overridden) {
        CoverArt art = new CoverArt(id, type, path, folderId, overridden);
        upsert(art);
    }

    @CacheEvict(key = "#art.entityType.toString().concat('-').concat(#art.entityId)")
    public void upsert(CoverArt art) {
        coverArtDao.upsert(art);
    }

    public void persistIfNeeded(MediaFile mediaFile) {
        if (mediaFile.getArt() != null && mediaFile.getArt() != NULL_ART) {
            CoverArt art = get(EntityType.MEDIA_FILE, mediaFile.getId());
            if (art == NULL_ART || !art.getOverridden()) {
                mediaFile.getArt().setEntityId(mediaFile.getId());
                upsert(mediaFile.getArt());
            }
            mediaFile.setArt(null);
        }
    }

    public void persistIfNeeded(Album album) {
        if (album.getArt() != null && album.getArt() != NULL_ART) {
            CoverArt art = get(EntityType.ALBUM, album.getId());
            if (art == NULL_ART || !art.getOverridden()) {
                album.getArt().setEntityId(album.getId());
                upsert(album.getArt());
            }
            album.setArt(null);
        }
    }

    public void persistIfNeeded(Artist artist) {
        if (artist.getArt() != null && artist.getArt() != NULL_ART) {
            CoverArt art = get(EntityType.ARTIST, artist.getId());
            if (art == NULL_ART || !art.getOverridden()) {
                artist.getArt().setEntityId(artist.getId());
                upsert(artist.getArt());
            }
            artist.setArt(null);
        }
    }

    @Cacheable(key = "#type.toString().concat('-').concat(#id)", unless = "#result == null") // 'unless' condition should never happen, because of null-object pattern
    public CoverArt get(EntityType type, int id) {
        return Optional.ofNullable(coverArtDao.get(type, id)).orElse(NULL_ART);
    }

    public Path getFullPath(EntityType type, int id) {
        CoverArt art = get(type, id);
        return getFullPath(art);
    }

    public Path getFullPath(CoverArt art) {
        if (art != null && art != NULL_ART) {
            if (art.getFolderId() == null) {
                // null folder ids mean absolute paths
                return art.getRelativePath();
            } else {
                MusicFolder folder = mediaFolderService.getMusicFolderById(art.getFolderId());
                if (folder != null) {
                    return art.getFullPath(folder.getPath());
                }
            }
        }

        return null;
    }

    @CacheEvict(key = "#type.toString().concat('-').concat(#id)")
    public void delete(EntityType type, int id) {
        coverArtDao.delete(type, id);
    }

    @CacheEvict(allEntries = true)
    public void expunge() {
        coverArtDao.expunge();
    }
}
