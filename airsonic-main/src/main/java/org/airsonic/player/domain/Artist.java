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

import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Sindre Mehus
 * @version $Id$
 */
public class Artist {

    private int id;
    private String name;
    private Instant lastScanned;
    private boolean present;
    private Set<Integer> albumIds = ConcurrentHashMap.newKeySet();

    public Artist() {
    }

    public Artist(int id, String name, Instant lastScanned, boolean present) {
        this.id = id;
        this.name = name;
        this.lastScanned = lastScanned;
        this.present = present;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Instant getLastScanned() {
        return lastScanned;
    }

    public void setLastScanned(Instant lastScanned) {
        this.lastScanned = lastScanned;
    }

    public boolean isPresent() {
        return present;
    }

    public void setPresent(boolean present) {
        this.present = present;
    }

    public Set<Integer> getAlbumIds() {
        return albumIds;
    }

    public void setAlbumIds(Set<Integer> albumIds) {
        this.albumIds = albumIds;
    }


    // placeholder for persistence later
    private CoverArt art;

    public CoverArt getArt() {
        return art;
    }

    public void setArt(CoverArt art) {
        this.art = art;
    }
}
