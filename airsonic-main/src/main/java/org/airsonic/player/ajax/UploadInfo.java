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

import java.util.UUID;

/**
 * Contains status for a file upload.
 *
 * @author Sindre Mehus
 */
public class UploadInfo {
    private final UUID transferId;
    private final long bytesUploaded;
    private final long bytesTotal;

    public UploadInfo(UUID transferId, long bytesUploaded, long bytesTotal) {
        this.transferId = transferId;
        this.bytesUploaded = bytesUploaded;
        this.bytesTotal = bytesTotal;
    }

    public UUID getTransferId() {
        return transferId;
    }

    /**
     * Returns the number of bytes uploaded.
     * @return The number of bytes uploaded.
     */
    public long getBytesUploaded() {
        return bytesUploaded;
    }

    /**
    * Returns the total number of bytes.
    * @return The total number of bytes.
    */
    public long getBytesTotal() {
        return bytesTotal;
    }

}
