package org.airsonic.player.ajax;

import com.google.common.collect.ImmutableMap;

import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.MediaFileComparator;
import org.airsonic.player.service.MediaFileService;
import org.airsonic.player.service.RatingService;
import org.airsonic.player.service.SecurityService;
import org.airsonic.player.service.SettingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Controller
@MessageMapping("/mediafile")
public class MediaFileWSController {
    @Autowired
    private MediaFileService mediaFileService;
    @Autowired
    private RatingService ratingService;
    @Autowired
    private SecurityService securityService;
    @Autowired
    private SettingsService settingsService;

    @MessageMapping("/directory/get")
    @SendToUser(broadcast = false)
    public Object get(Principal user, MediaDirectoryRequest req, SimpMessageHeaderAccessor headers) throws Exception {
        List<MediaFile> mediaFiles = getMediaFiles(req.getIds(), req.getPaths());

        if (mediaFiles.isEmpty()) {
            return ImmutableMap.of("contentType", "notFound");
        }

        MediaFile dir = mediaFiles.get(0);
        if (dir.isFile()) {
            dir = mediaFileService.getParentOf(dir);
        }

        // Redirect if root directory.
        if (mediaFileService.isRoot(dir)) {
            return ImmutableMap.of("contentType", "home");
        }

        if (!securityService.isFolderAccessAllowed(dir, user.getName())) {
            return ImmutableMap.of("contentType", "accessDenied");
        }

        List<MediaFile> children = getChildren(mediaFiles);

        List<MediaFile> files = new ArrayList<>();
        List<MediaFile> subDirs = new ArrayList<>();
        for (MediaFile child : children) {
            if (child.isFile()) {
                files.add(child);
            } else {
                subDirs.add(child);
            }
        }
        MediaFileDirectoryEntry entry = new MediaFileDirectoryEntry(mediaFileService.toMediaFileEntryList(Collections.singletonList(dir), user.getName(), true, true, null, null, null).get(0));
        entry.setFiles(mediaFileService.toMediaFileEntryList(files, user.getName(), true, false, null, null, null));
        entry.setSubDirs(mediaFileService.toMediaFileEntryList(subDirs, user.getName(), false, false, null, null, null));
        entry.setAncestors(mediaFileService.toMediaFileEntryList(getAncestors(dir), user.getName(), false, false, null, null, null));
        entry.setLastPlayed(dir.getLastPlayed());
        entry.setPlayCount(dir.getPlayCount());
        entry.setComment(dir.getComment());
        if (dir.isAlbum()) {
            List<MediaFile> siblingAlbums = getSiblingAlbums(dir);
            entry.setSiblingAlbums(mediaFileService.toMediaFileEntryList(siblingAlbums, user.getName(), false, false, null, null, null));
            entry.setAlbum(guessAlbum(children));
            entry.setArtist(guessArtist(children));
            entry.setMusicBrainzReleaseId(guessMusicBrainzReleaseId(children));
        }

        Integer userRating = Optional.ofNullable(ratingService.getRatingForUser(user.getName(), dir)).orElse(0);
        Double averageRating = Optional.ofNullable(ratingService.getAverageRating(dir)).orElse(0.0D);

        entry.setUserRating(10 * userRating);
        entry.setAverageRating(10.0D * averageRating);

        if (isVideoOnly(children)) {
            entry.setContentType("video");
        } else if (dir.isAlbum()) {
            entry.setContentType("album");
        } else {
            entry.setContentType("artist");
        }

        return entry;
    }

    private boolean isVideoOnly(List<MediaFile> children) {
        boolean videoFound = false;
        for (MediaFile child : children) {
            if (child.isAudio()) {
                return false;
            }
            if (child.isVideo()) {
                videoFound = true;
            }
        }
        return videoFound;
    }

    private List<MediaFile> getMediaFiles(List<Integer> ids, List<String> paths) {
        return Stream
                .concat(Optional.ofNullable(paths).orElse(Collections.emptyList()).parallelStream().map(mediaFileService::getMediaFile),
                        Optional.ofNullable(ids).orElse(Collections.emptyList()).parallelStream().map(mediaFileService::getMediaFile))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private String guessArtist(List<MediaFile> children) {
        for (MediaFile child : children) {
            if (child.isFile() && child.getArtist() != null) {
                return child.getArtist();
            }
        }
        return null;
    }

    private String guessAlbum(List<MediaFile> children) {
        for (MediaFile child : children) {
            if (child.isFile() && child.getArtist() != null) {
                return child.getAlbumName();
            }
        }
        return null;
    }

    private String guessMusicBrainzReleaseId(List<MediaFile> children) {
        for (MediaFile child : children) {
            if (child.isFile() && child.getMusicBrainzReleaseId() != null) {
                return child.getMusicBrainzReleaseId();
            }
        }
        return null;
    }

    private List<MediaFile> getChildren(List<MediaFile> mediaFiles) {
        SortedSet<MediaFile> result = new TreeSet<>(new MediaFileComparator(settingsService.isSortAlbumsByYear()));
        for (MediaFile mediaFile : mediaFiles) {
            if (mediaFile.isFile()) {
                mediaFile = mediaFileService.getParentOf(mediaFile);
            }
            result.addAll(mediaFileService.getChildrenOf(mediaFile, true, true, true));
        }
        return new ArrayList<>(result);
    }

    private List<MediaFile> getAncestors(MediaFile dir) {
        ArrayList<MediaFile> result = new ArrayList<>();

        try {
            MediaFile parent = mediaFileService.getParentOf(dir);
            while (parent != null) {
                result.add(parent);
                if (mediaFileService.isRoot(parent)) {
                    break;
                }
                parent = mediaFileService.getParentOf(parent);
            }
        } catch (SecurityException x) {
            // Happens if Podcast directory is outside music folder.
        }
        return result;
    }

    private List<MediaFile> getSiblingAlbums(MediaFile dir) {
        List<MediaFile> result = new ArrayList<>();

        MediaFile parent = mediaFileService.getParentOf(dir);
        if (!mediaFileService.isRoot(parent)) {
            List<MediaFile> siblings = mediaFileService.getChildrenOf(parent, false, true, true);
            result.addAll(siblings.stream().filter(sibling -> sibling.isAlbum() && !sibling.equals(dir))
                    .collect(Collectors.toList()));
        }
        return result;
    }

    public static class MediaDirectoryRequest {
        List<Integer> ids;
        List<String> paths;

        public List<Integer> getIds() {
            return ids;
        }

        public void setIds(List<Integer> ids) {
            this.ids = ids;
        }

        public List<String> getPaths() {
            return paths;
        }

        public void setPaths(List<String> paths) {
            this.paths = paths;
        }

    }

    public static class MediaFileDirectoryEntry extends MediaFileEntry {
        private List<MediaFileEntry> files;
        private List<MediaFileEntry> subDirs;
        private List<MediaFileEntry> ancestors;
        private Integer userRating;
        private Double averageRating;
        private long playCount;
        private Instant lastPlayed;
        private String comment;

        // Albums only
        private List<MediaFileEntry> siblingAlbums;
        private String musicBrainzReleaseId;

        public MediaFileDirectoryEntry(MediaFileEntry mfe) {
            super(mfe.getId(), mfe.getTrackNumber(), mfe.getTitle(), mfe.getArtist(), mfe.getAlbum(), mfe.getGenre(),
                    mfe.getYear(), mfe.getBitRate(), mfe.getDimensions(), mfe.getDuration(),
                    mfe.getFormat(), mfe.getContentType(), mfe.getEntryType(), mfe.getFileSize(), mfe.getStarred(), mfe.getPresent(),
                    mfe.getAlbumUrl(), mfe.getStreamUrl(), mfe.getRemoteStreamUrl(), mfe.getCoverArtUrl(), mfe.getRemoteCoverArtUrl());
        }

        public List<MediaFileEntry> getFiles() {
            return files;
        }

        public void setFiles(List<MediaFileEntry> files) {
            this.files = files;
        }

        public Integer getUserRating() {
            return userRating;
        }

        public void setUserRating(Integer userRating) {
            this.userRating = userRating;
        }

        public Double getAverageRating() {
            return averageRating;
        }

        public void setAverageRating(Double averageRating) {
            this.averageRating = averageRating;
        }

        public long getPlayCount() {
            return playCount;
        }

        public void setPlayCount(long playCount) {
            this.playCount = playCount;
        }

        public Instant getLastPlayed() {
            return lastPlayed;
        }

        public void setLastPlayed(Instant lastPlayed) {
            this.lastPlayed = lastPlayed;
        }

        public String getComment() {
            return comment;
        }

        public void setComment(String comment) {
            this.comment = comment;
        }

        public String getMusicBrainzReleaseId() {
            return musicBrainzReleaseId;
        }

        public void setMusicBrainzReleaseId(String musicBrainzReleaseId) {
            this.musicBrainzReleaseId = musicBrainzReleaseId;
        }

        public List<MediaFileEntry> getSubDirs() {
            return subDirs;
        }

        public void setSubDirs(List<MediaFileEntry> subDirs) {
            this.subDirs = subDirs;
        }

        public List<MediaFileEntry> getAncestors() {
            return ancestors;
        }

        public void setAncestors(List<MediaFileEntry> ancestors) {
            this.ancestors = ancestors;
        }

        public List<MediaFileEntry> getSiblingAlbums() {
            return siblingAlbums;
        }

        public void setSiblingAlbums(List<MediaFileEntry> siblingAlbums) {
            this.siblingAlbums = siblingAlbums;
        }
    }
}
