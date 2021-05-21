package org.airsonic.player.domain;

public class MediaFileWithUrlInfo {

    private final MediaFile file;
    private final boolean streamable;
    private final String coverArtUrl;
    private final String streamUrl;
    private final String captionsUrl;
    private final String contentType;

    public MediaFileWithUrlInfo(MediaFile file, boolean streamable, String coverArtUrl, String streamUrl,
            String captionsUrl, String contentType) {
        this.file = file;
        this.streamable = streamable;
        this.coverArtUrl = coverArtUrl;
        this.streamUrl = streamUrl;
        this.captionsUrl = captionsUrl;
        this.contentType = contentType;
    }

    public String getCaptionsUrl() {
        return captionsUrl;
    }

    public boolean getStreamable() {
        return streamable;
    }

    public String getStreamUrl() {
        return streamUrl;
    }

    public String getCoverArtUrl() {
        return coverArtUrl;
    }

    public MediaFile getFile() {
        return file;
    }

    public String getContentType() {
        return contentType;
    }

}
