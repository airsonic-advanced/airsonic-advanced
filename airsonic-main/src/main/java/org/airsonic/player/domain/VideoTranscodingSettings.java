/*
 This file is part of Airsonic.

 Airsonic is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Airsonic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Airsonic.  If not, see <http://www.gnu.org/licenses/>.

 Copyright 2016 (C) Airsonic Authors
 Based upon Subsonic, Copyright 2009 (C) Sindre Mehus
 */
package org.airsonic.player.domain;

/**
 * Parameters used when transcoding videos.
 *
 * @author Sindre Mehus
 * @version $Id$
 */
public class VideoTranscodingSettings {

    private final int width;
    private final int height;
    private final int timeOffset;
    private final double duration;

    private int audioTrackIndex = 1;

    // HLS stuff
    private int hlsSegmentIndex;
    private String hlsSegmentFilename;
    // TODO shouldn't be part of this file
    private String outputFilename;

    public VideoTranscodingSettings(int width, int height, int timeOffset, double duration) {
        this.width = width;
        this.height = height;
        this.timeOffset = timeOffset;
        this.duration = duration;
    }

    public VideoTranscodingSettings(int width, int height, int timeOffset, double duration, int audioTrackIndex,
            int hlsSegmentIndex, String hlsSegmentFilename, String outputFilename) {
        this(width, height, timeOffset, duration);
        this.audioTrackIndex = audioTrackIndex;
        this.hlsSegmentIndex = hlsSegmentIndex;
        this.hlsSegmentFilename = hlsSegmentFilename;
        this.outputFilename = outputFilename;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getTimeOffset() {
        return timeOffset;
    }

    public double getDuration() {
        return duration;
    }

    public int getAudioTrackIndex() {
        return audioTrackIndex;
    }

    public int getHlsSegmentIndex() {
        return hlsSegmentIndex;
    }

    public String getHlsSegmentFilename() {
        return hlsSegmentFilename;
    }

    public String getOutputFilename() {
        return outputFilename;
    }

}
