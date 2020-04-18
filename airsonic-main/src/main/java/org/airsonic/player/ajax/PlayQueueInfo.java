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
package org.airsonic.player.ajax;

import org.airsonic.player.domain.PlayQueue.RepeatStatus;
import org.airsonic.player.domain.PlayQueue.Status;

import java.util.List;

/**
 * The playlist of a player.
 *
 * @author Sindre Mehus
 */
public class PlayQueueInfo {
    private final List<MediaFileEntry> entries;
    private final Status playStatus;
    private final RepeatStatus repeatStatus;
    private final boolean shuffleRadioEnabled;
    private final boolean internetRadioEnabled;
    private final float gain;
    private int startPlayerAt = -1;
    private long startPlayerAtPosition; // millis

    public PlayQueueInfo(List<MediaFileEntry> entries, Status playStatus, RepeatStatus repeatStatus,
            boolean shuffleRadioEnabled, boolean internetRadioEnabled, float gain) {
        this.entries = entries;
        this.playStatus = playStatus;
        this.repeatStatus = repeatStatus;
        this.shuffleRadioEnabled = shuffleRadioEnabled;
        this.internetRadioEnabled = internetRadioEnabled;
        this.gain = gain;
    }

    public List<MediaFileEntry> getEntries() {
        return entries;
    }

    public Status getPlayStatus() {
        return playStatus;
    }

    public RepeatStatus getRepeatStatus() {
        return repeatStatus;
    }

    public boolean isShuffleRadioEnabled() {
        return shuffleRadioEnabled;
    }

    public boolean isInternetRadioEnabled() {
        return internetRadioEnabled;
    }

    public float getGain() {
        return gain;
    }

    public int getStartPlayerAt() {
        return startPlayerAt;
    }

    public PlayQueueInfo setStartPlayerAt(int startPlayerAt) {
        this.startPlayerAt = startPlayerAt;
        return this;
    }

    public long getStartPlayerAtPosition() {
        return startPlayerAtPosition;
    }

    public PlayQueueInfo setStartPlayerAtPosition(long startPlayerAtPosition) {
        this.startPlayerAtPosition = startPlayerAtPosition;
        return this;
    }
}
