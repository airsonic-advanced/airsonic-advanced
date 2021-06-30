package org.airsonic.player.domain;

import org.airsonic.player.controller.CaptionsController;
import org.airsonic.player.controller.CaptionsController.CaptionInfo;

import java.util.List;

public class MediaFileWithUrlInfo {

    private final MediaFile file;
    private final boolean streamable;
    private final String coverArtUrl;
    private final String streamUrl;
    private final List<CaptionsController.CaptionInfo> captions;
    private final String contentType;

    public MediaFileWithUrlInfo(MediaFile file, boolean streamable, String coverArtUrl, String streamUrl,
            List<CaptionInfo> captions, String contentType) {
        this.file = file;
        this.streamable = streamable;
        this.coverArtUrl = coverArtUrl;
        this.streamUrl = streamUrl;
        this.captions = captions;
        this.contentType = contentType;
    }

    public List<CaptionsController.CaptionInfo> getCaptions() {
        return captions;
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
