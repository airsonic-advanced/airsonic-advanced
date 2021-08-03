/*
 * This file is part of Airsonic.
 *
 *  Airsonic is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Airsonic is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Airsonic.  If not, see <http://www.gnu.org/licenses/>.
 *
 *  Copyright 2015 (C) Sindre Mehus
 */

package org.airsonic.player.domain;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Represents the playback of a track, possibly remote (e.g., a cached song on a
 * mobile phone). Represents a particular moment in time for a TransferStatus
 * (which, in turn, may play multiple files at different times, each
 * representing a different PlayStatus)
 *
 * @author Sindre Mehus
 * @version $Id$
 */
public class PlayStatus {

    private final UUID transferId;
    private final MediaFile mediaFile;
    private final Player player;
    private final Instant time;

    private final static long TTL_MILLIS = 6L * 60L * 60L * 1000L; // 6 hours

    public PlayStatus(UUID transferId, MediaFile mediaFile, Player player, Instant time) {
        this.transferId = transferId;
        this.mediaFile = mediaFile;
        this.player = player;
        this.time = time;
    }

    public PlayStatus(UUID transferId, MediaFile mediaFile, Player player, long millisSinceLastUpdate) {
        this(transferId, mediaFile, player, Instant.now().minusMillis(millisSinceLastUpdate));
    }

    public UUID getTransferId() {
        return transferId;
    }

    public MediaFile getMediaFile() {
        return mediaFile;
    }

    public Player getPlayer() {
        return player;
    }

    public Instant getTime() {
        return time;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(time.plusMillis(TTL_MILLIS));
    }

    public long getMinutesAgo() {
        return ChronoUnit.MINUTES.between(time, Instant.now());
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((mediaFile == null) ? 0 : mediaFile.hashCode());
        result = prime * result + ((player == null || player.getId() == null) ? 0 : player.getId().hashCode());
        result = prime * result + ((transferId == null) ? 0 : transferId.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        PlayStatus other = (PlayStatus) obj;
        if (mediaFile == null) {
            if (other.mediaFile != null)
                return false;
        } else if (!mediaFile.equals(other.mediaFile))
            return false;
        if (player == null) {
            if (other.player != null)
                return false;
        } else if (player.getId() == null) {
            if (other.player == null || other.player.getId() != null) {
                return false;
            }
        } else if (other.player == null || !player.getId().equals(other.player.getId()))
            return false;
        if (transferId == null) {
            if (other.transferId != null)
                return false;
        } else if (!transferId.equals(other.transferId))
            return false;
        return true;
    }

}
