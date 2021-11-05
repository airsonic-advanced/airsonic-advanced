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

import org.airsonic.player.domain.MusicFolder.Type;
import org.airsonic.player.domain.PlayQueue.RepeatStatus;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.Files;
import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit test of {@link PlayQueue}.
 *
 * @author Sindre Mehus
 */
public class PlayQueueTestCase {
    @ClassRule
    public static TemporaryFolder temporaryFolder = new TemporaryFolder();

    @BeforeClass
    public static void setupOnce() throws IOException {
        Files.createFile(temporaryFolder.getRoot().toPath().resolve("A"));
        Files.createFile(temporaryFolder.getRoot().toPath().resolve("B"));
        Files.createFile(temporaryFolder.getRoot().toPath().resolve("C"));
        Files.createFile(temporaryFolder.getRoot().toPath().resolve("D"));
        Files.createFile(temporaryFolder.getRoot().toPath().resolve("E"));
        Files.createFile(temporaryFolder.getRoot().toPath().resolve("F"));
    }

    @Test
    public void testEmpty() {
        PlayQueue playQueue = new PlayQueue(i -> null);
        assertEquals(0, playQueue.size());
        assertTrue(playQueue.isEmpty());
        assertEquals(0, playQueue.getFiles().size());
        assertNull(playQueue.getCurrentFile());
    }

    @Test
    public void testStatus() {
        PlayQueue playQueue = new PlayQueue(i -> null);
        assertEquals(PlayQueue.Status.PLAYING, playQueue.getStatus());

        playQueue.setStatus(PlayQueue.Status.STOPPED);
        assertEquals(PlayQueue.Status.STOPPED, playQueue.getStatus());

        playQueue.addFiles(true, new TestMediaFile());
        assertEquals(PlayQueue.Status.PLAYING, playQueue.getStatus());

        playQueue.clear();
        assertEquals(PlayQueue.Status.PLAYING, playQueue.getStatus());
    }

    @Test
    public void testMoveUp() {
        PlayQueue playQueue = createPlaylist(0, "A", "B", "C", "D");
        playQueue.moveUp(0);
        assertPlaylistEquals(playQueue, 0, "A", "B", "C", "D");

        playQueue = createPlaylist(0, "A", "B", "C", "D");
        playQueue.moveUp(9999);
        assertPlaylistEquals(playQueue, 0, "A", "B", "C", "D");

        playQueue = createPlaylist(1, "A", "B", "C", "D");
        playQueue.moveUp(1);
        assertPlaylistEquals(playQueue, 0, "B", "A", "C", "D");

        playQueue = createPlaylist(3, "A", "B", "C", "D");
        playQueue.moveUp(3);
        assertPlaylistEquals(playQueue, 2, "A", "B", "D", "C");
    }

    @Test
    public void testMoveDown() {
        PlayQueue playQueue = createPlaylist(0, "A", "B", "C", "D");
        playQueue.moveDown(0);
        assertPlaylistEquals(playQueue, 1, "B", "A", "C", "D");

        playQueue = createPlaylist(0, "A", "B", "C", "D");
        playQueue.moveDown(9999);
        assertPlaylistEquals(playQueue, 0, "A", "B", "C", "D");

        playQueue = createPlaylist(1, "A", "B", "C", "D");
        playQueue.moveDown(1);
        assertPlaylistEquals(playQueue, 2, "A", "C", "B", "D");

        playQueue = createPlaylist(3, "A", "B", "C", "D");
        playQueue.moveDown(3);
        assertPlaylistEquals(playQueue, 3, "A", "B", "C", "D");
    }

    @Test
    public void testRemove() {
        PlayQueue playQueue = createPlaylist(0, "A", "B", "C", "D");
        playQueue.removeFileAt(0);
        assertPlaylistEquals(playQueue, 0, "B", "C", "D");

        playQueue = createPlaylist(1, "A", "B", "C", "D");
        playQueue.removeFileAt(0);
        assertPlaylistEquals(playQueue, 0, "B", "C", "D");

        playQueue = createPlaylist(0, "A", "B", "C", "D");
        playQueue.removeFileAt(3);
        assertPlaylistEquals(playQueue, 0, "A", "B", "C");

        playQueue = createPlaylist(1, "A", "B", "C", "D");
        playQueue.removeFileAt(1);
        assertPlaylistEquals(playQueue, 1, "A", "C", "D");

        playQueue = createPlaylist(3, "A", "B", "C", "D");
        playQueue.removeFileAt(3);
        assertPlaylistEquals(playQueue, 2, "A", "B", "C");

        playQueue = createPlaylist(0, "A");
        playQueue.removeFileAt(0);
        assertPlaylistEquals(playQueue, -1);
    }

    @Test
    public void testNext() {
        PlayQueue playQueue = createPlaylist(0, "A", "B", "C");
        assertThat(playQueue.getRepeatStatus()).isEqualTo(RepeatStatus.OFF);
        playQueue.next();
        assertPlaylistEquals(playQueue, 1, "A", "B", "C");
        playQueue.next();
        assertPlaylistEquals(playQueue, 2, "A", "B", "C");
        playQueue.next();
        assertPlaylistEquals(playQueue, -1, "A", "B", "C");

        playQueue = createPlaylist(0, "A", "B", "C");
        playQueue.setRepeatStatus(RepeatStatus.QUEUE);
        assertThat(playQueue.getRepeatStatus()).isEqualTo(RepeatStatus.QUEUE);
        playQueue.next();
        assertPlaylistEquals(playQueue, 1, "A", "B", "C");
        playQueue.next();
        assertPlaylistEquals(playQueue, 2, "A", "B", "C");
        playQueue.next();
        assertPlaylistEquals(playQueue, 0, "A", "B", "C");

        playQueue = createPlaylist(0, "A", "B", "C");
        playQueue.setRepeatStatus(RepeatStatus.TRACK);
        assertThat(playQueue.getRepeatStatus()).isEqualTo(RepeatStatus.TRACK);
        playQueue.next();
        assertPlaylistEquals(playQueue, 0, "A", "B", "C");
        playQueue.next();
        assertPlaylistEquals(playQueue, 0, "A", "B", "C");
        playQueue.next();
        assertPlaylistEquals(playQueue, 0, "A", "B", "C");
    }

    @Test
    public void testPlayAfterEndReached() {
        PlayQueue playQueue = createPlaylist(2, "A", "B", "C");
        playQueue.setStatus(PlayQueue.Status.PLAYING);
        playQueue.next();
        assertNull(playQueue.getCurrentFile());
        assertEquals(PlayQueue.Status.STOPPED, playQueue.getStatus());

        playQueue.setStatus(PlayQueue.Status.PLAYING);
        assertEquals(PlayQueue.Status.PLAYING, playQueue.getStatus());
        assertEquals(0, playQueue.getIndex());
        assertEquals("A", playQueue.getCurrentFile().getName());
    }

    @Test
    public void testPlayLast() {
        PlayQueue playQueue = createPlaylist(1, "A", "B", "C");

        playQueue.addFiles(true, new TestMediaFile("D"));
        assertPlaylistEquals(playQueue, 1, "A", "B", "C", "D");

        playQueue.addFiles(false, new TestMediaFile("E"));
        assertPlaylistEquals(playQueue, 0, "E");
    }

    public void testAddFilesAt() {
        PlayQueue playQueue = createPlaylist(0);

        playQueue.addFilesAt(Arrays.<MediaFile>asList(new TestMediaFile("A"), new TestMediaFile("B"), new TestMediaFile("C")), 0);
        assertPlaylistEquals(playQueue, 0, "A", "B", "C");

        playQueue.addFilesAt(Arrays.<MediaFile>asList(new TestMediaFile("D"), new TestMediaFile("E")), 1);
        assertPlaylistEquals(playQueue, 0, "A", "D", "E", "B", "C");

        playQueue.addFilesAt(Arrays.<MediaFile>asList(new TestMediaFile("F")), 0);
        assertPlaylistEquals(playQueue, 0, "F", "A", "D", "E", "B", "C");

    }

    @Test
    public void testUndo() {
        PlayQueue playQueue = createPlaylist(0, "A", "B", "C");
        playQueue.setIndex(2);
        playQueue.undo();
        assertPlaylistEquals(playQueue, 0, "A", "B", "C");

        playQueue.removeFileAt(2);
        playQueue.undo();
        assertPlaylistEquals(playQueue, 0, "A", "B", "C");

        playQueue.clear();
        playQueue.undo();
        assertPlaylistEquals(playQueue, 0, "A", "B", "C");

        playQueue.addFiles(true, new TestMediaFile());
        playQueue.undo();
        assertPlaylistEquals(playQueue, 0, "A", "B", "C");

        playQueue.moveDown(1);
        playQueue.undo();
        assertPlaylistEquals(playQueue, 0, "A", "B", "C");

        playQueue.moveUp(1);
        playQueue.undo();
        assertPlaylistEquals(playQueue, 0, "A", "B", "C");
    }

    @Test
    public void testOrder() {
        PlayQueue playQueue = createPlaylist(2);
        playQueue.addFiles(true, new TestMediaFile(2, "A", "Album B"));
        playQueue.addFiles(true, new TestMediaFile(1, "C", "Album C"));
        playQueue.addFiles(true, new TestMediaFile(3, "B", "Album A"));
        playQueue.addFiles(true, new TestMediaFile(null, "D", "Album D"));
        playQueue.setIndex(2);
        assertThat(playQueue.getCurrentFile().getTrackNumber()).isEqualTo(3);

        // Order by track.
        playQueue.sort(PlayQueue.SortOrder.TRACK);
        assertThat(playQueue.getFile(0).getTrackNumber()).isNull();
        assertThat(playQueue.getFile(1).getTrackNumber()).isEqualTo(1);
        assertThat(playQueue.getFile(2).getTrackNumber()).isEqualTo(2);
        assertThat(playQueue.getFile(3).getTrackNumber()).isEqualTo(3);
        assertThat(playQueue.getCurrentFile().getTrackNumber()).isEqualTo(3);

        // Order by artist.
        playQueue.sort(PlayQueue.SortOrder.ARTIST);
        assertEquals("A", playQueue.getFile(0).getArtist());
        assertEquals("B", playQueue.getFile(1).getArtist());
        assertEquals("C", playQueue.getFile(2).getArtist());
        assertEquals("D", playQueue.getFile(3).getArtist());
        assertThat(playQueue.getCurrentFile().getTrackNumber()).isEqualTo(3);

        // Order by album.
        playQueue.sort(PlayQueue.SortOrder.ALBUM);
        assertEquals("Album A", playQueue.getFile(0).getAlbumName());
        assertEquals("Album B", playQueue.getFile(1).getAlbumName());
        assertEquals("Album C", playQueue.getFile(2).getAlbumName());
        assertEquals("Album D", playQueue.getFile(3).getAlbumName());
        assertThat(playQueue.getCurrentFile().getTrackNumber()).isEqualTo(3);
    }

    private void assertPlaylistEquals(PlayQueue playQueue, int index, String... songs) {
        assertEquals(songs.length, playQueue.size());
        for (int i = 0; i < songs.length; i++) {
            assertEquals(songs[i], playQueue.getFiles().get(i).getName());
        }

        if (index == -1) {
            assertNull(playQueue.getCurrentFile());
        } else {
            assertEquals(songs[index], playQueue.getCurrentFile().getName());
        }
    }

    private PlayQueue createPlaylist(int index, String... songs) {
        PlayQueue playQueue = new PlayQueue(i -> new MusicFolder(i, temporaryFolder.getRoot().toPath(), "meh", Type.MEDIA, true, Instant.now()));
        for (String song : songs) {
            playQueue.addFiles(true, new TestMediaFile(song));
        }
        playQueue.setIndex(index);
        return playQueue;
    }

    private static class TestMediaFile extends MediaFile {

        private String name;
        private Integer track;
        private String album;
        private String artist;

        TestMediaFile() {
        }

        TestMediaFile(String name) {
            this.name = name;
            setPath(name);
            setFolderId(0);
        }

        TestMediaFile(Integer track, String artist, String album) {
            this.track = track;
            this.album = album;
            this.artist = artist;
            setPath(artist);
            setFolderId(track);
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public boolean isFile() {
            return true;
        }

        @Override
        public Integer getTrackNumber() {
            return track;
        }

        @Override
        public String getArtist() {
            return artist;
        }

        @Override
        public String getAlbumName() {
            return album;
        }

        @Override
        public boolean isDirectory() {
            return false;
        }

        @Override
        public boolean equals(Object o) {
            return this == o;
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, track, album, artist);
        }
    }
}