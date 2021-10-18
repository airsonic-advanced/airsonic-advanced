package org.airsonic.player.ajax;

import org.airsonic.player.domain.Bookmark;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.service.BookmarkService;
import org.airsonic.player.service.MediaFileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@MessageMapping("/bookmarks")
public class BookmarksWSController {
    @Autowired
    private BookmarkService bookmarkService;
    @Autowired
    private MediaFileService mediaFileService;

    @SubscribeMapping("/list")
    private Map<Integer, BookmarkInfo> getBookmarks(Principal user) {
        return bookmarkService.getBookmarks(user.getName()).stream()
                .map(b -> bookmarkToBookmarkInfo(b, user.getName()))
                .filter(bi -> bi != null)
                .collect(Collectors.toMap(bi -> bi.getMediaFileEntry().getId(), bi -> bi));
    }

    private BookmarkInfo bookmarkToBookmarkInfo(Bookmark bookmark, String user) {
        MediaFile mediaFile = mediaFileService.getMediaFile(bookmark.getMediaFileId());
        if (mediaFile == null) {
            return null;
        }
        return new BookmarkInfo(
                bookmark.getId(),
                mediaFileService.toMediaFileEntryList(Collections.singletonList(mediaFile), user, true, false, null, null, null).get(0),
                bookmark.getChanged(),
                bookmark.getCreated(),
                bookmark.getComment(),
                bookmark.getPositionMillis());
    }

    @MessageMapping("/set")
    public boolean create(Principal user, BookmarkCreateReq bookmark) {
        return bookmarkService.setBookmark(user.getName(), bookmark.getMediaFileId(), bookmark.getPositionMillis(), bookmark.getComment());
    }

    @MessageMapping("/delete")
    public int delete(Principal user, int mediaFileId) {
        bookmarkService.deleteBookmark(user.getName(), mediaFileId);
        return mediaFileId;
    }

    @MessageMapping("/get")
    @SendToUser(broadcast = false)
    public BookmarkInfo getBookmark(Principal user, int mediaFileId) {
        return bookmarkToBookmarkInfo(bookmarkService.getBookmark(user.getName(), mediaFileId), user.getName());
    }

    public static class BookmarkInfo {
        private final int id;
        private final MediaFileEntry mediaFileEntry;
        private final Instant changed;
        private final Instant created;
        private final String comment;
        private final long positionMillis;

        public BookmarkInfo(int id, MediaFileEntry mediaFileEntry, Instant changed, Instant created, String comment, long positionMillis) {
            this.id = id;
            this.mediaFileEntry = mediaFileEntry;
            this.changed = changed;
            this.created = created;
            this.comment = comment;
            this.positionMillis = positionMillis;
        }

        public int getId() {
            return id;
        }

        public MediaFileEntry getMediaFileEntry() {
            return mediaFileEntry;
        }

        public Instant getChanged() {
            return changed;
        }

        public Instant getCreated() {
            return created;
        }

        public String getComment() {
            return comment;
        }

        public long getPositionMillis() {
            return positionMillis;
        }
    }

    public static class BookmarkCreateReq {
        private long positionMillis;
        private String comment;
        private int mediaFileId;

        public long getPositionMillis() {
            return positionMillis;
        }

        public void setPositionMillis(long positionMillis) {
            this.positionMillis = positionMillis;
        }

        public String getComment() {
            return comment;
        }

        public void setComment(String comment) {
            this.comment = comment;
        }

        public int getMediaFileId() {
            return mediaFileId;
        }

        public void setMediaFileId(int mediaFileId) {
            this.mediaFileId = mediaFileId;
        }
    }

}
