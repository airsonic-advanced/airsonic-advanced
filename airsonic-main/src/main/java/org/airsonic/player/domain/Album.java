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

import com.google.common.util.concurrent.AtomicDouble;

import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Sindre Mehus
 * @version $Id$
 */
public class Album {

    private int id;
    private String path;
    private String name;
    private String artist;
    private final AtomicInteger songCount = new AtomicInteger(0);
    private final AtomicDouble duration = new AtomicDouble(0);
    private Integer year;
    private String genre;
    private final AtomicInteger playCount = new AtomicInteger(0);
    private Instant lastPlayed;
    private String comment;
    private Instant created;
    private Instant lastScanned;
    private boolean present;
    private Integer folderId;
    private String musicBrainzReleaseId;
    private Set<Integer> mediaFileIds = ConcurrentHashMap.newKeySet();
    private Set<Genre> genres = ConcurrentHashMap.newKeySet();

    public Album() {
    }

    public Album(int id, String path, String name, String artist, int songCount, double duration,
            Integer year, String genre, int playCount, Instant lastPlayed, String comment, Instant created, Instant lastScanned,
            boolean present, Integer folderId, String musicBrainzReleaseId) {
        this.id = id;
        this.path = path;
        this.name = name;
        this.artist = artist;
        this.songCount.set(songCount);
        this.duration.set(duration);
        this.year = year;
        this.genre = genre;
        this.playCount.set(playCount);
        this.lastPlayed = lastPlayed;
        this.comment = comment;
        this.created = created;
        this.lastScanned = lastScanned;
        this.folderId = folderId;
        this.present = present;
        this.musicBrainzReleaseId = musicBrainzReleaseId;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public int getSongCount() {
        return songCount.get();
    }

    public void setSongCount(int songCount) {
        this.songCount.set(songCount);
    }

    public void incrementSongCount() {
        this.songCount.incrementAndGet();
    }

    public double getDuration() {
        return duration.get();
    }

    public void setDuration(double duration) {
        this.duration.set(duration);
    }

    public void incrementDuration(double duration) {
        this.duration.addAndGet(duration);
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public int getPlayCount() {
        return playCount.get();
    }

    public void setPlayCount(int playCount) {
        this.playCount.set(playCount);
    }

    public void incrementPlayCount() {
        this.playCount.incrementAndGet();
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

    public Instant getCreated() {
        return created;
    }

    public void setCreated(Instant created) {
        this.created = created;
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

    public void setFolderId(Integer folderId) {
        this.folderId = folderId;
    }

    public Integer getFolderId() {
        return folderId;
    }

    public String getMusicBrainzReleaseId() {
        return musicBrainzReleaseId;
    }

    public void setMusicBrainzReleaseId(String musicBrainzReleaseId) {
        this.musicBrainzReleaseId = musicBrainzReleaseId;
    }

    public Set<Integer> getMediaFileIds() {
        return mediaFileIds;
    }

    public void setMediaFileIds(Set<Integer> mediaFileIds) {
        this.mediaFileIds = mediaFileIds;
    }

    public Set<Genre> getGenres() {
        return genres;
    }

    public void setGenres(Set<Genre> genres) {
        this.genres = genres;
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
