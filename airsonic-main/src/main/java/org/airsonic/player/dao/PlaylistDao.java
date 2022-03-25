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
package org.airsonic.player.dao;

import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.Playlist;
import org.airsonic.player.util.LambdaUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Provides database services for playlists.
 *
 * @author Sindre Mehus
 */
@Repository
public class PlaylistDao extends AbstractDao {
    private static final String INSERT_COLUMNS = "username, is_public, name, comment, file_count, duration, " +
                                                "created, changed, imported_from";
    private static final String QUERY_COLUMNS = "id, " + INSERT_COLUMNS;
    private final PlaylistMapper rowMapper = new PlaylistMapper();
    private final static Comparator<Playlist> sorter = Comparator.comparing(p -> p.getName());

    @PostConstruct
    public void register() throws Exception {
        registerInserts("playlist", "id", Arrays.asList(INSERT_COLUMNS.split(", ")), Playlist.class);
    }

    public List<Playlist> getReadablePlaylistsForUser(String username) {

        List<Playlist> result1 = getWritablePlaylistsForUser(username);
        List<Playlist> result2 = query("select " + QUERY_COLUMNS + " from playlist where is_public", rowMapper);
        List<Playlist> result3 = query("select " + prefix(QUERY_COLUMNS, "playlist") + " from playlist, playlist_user where " +
                                       "playlist.id = playlist_user.playlist_id and " +
                                       "playlist.username != ? and " +
                                       "playlist_user.username = ?", rowMapper, username, username);

        // Remove duplicates.
        return Stream.of(result1, result2, result3)
                .flatMap(r -> r.parallelStream())
                .filter(LambdaUtils.distinctByKey(p -> p.getId()))
                .sorted(sorter)
                .collect(Collectors.toList());
    }

    public List<Playlist> getWritablePlaylistsForUser(String username) {
        return query("select " + QUERY_COLUMNS + " from playlist where username=?", rowMapper, username)
                .stream()
                .sorted(sorter)
                .collect(Collectors.toList());
    }

    public Playlist getPlaylist(int id) {
        return queryOne("select " + QUERY_COLUMNS + " from playlist where id=?", rowMapper, id);
    }

    public List<Playlist> getAllPlaylists() {
        return query("select " + QUERY_COLUMNS + " from playlist", rowMapper)
                .stream()
                .sorted(sorter)
                .collect(Collectors.toList());
    }

    public void createPlaylist(Playlist playlist) {
        Integer id = insert("playlist", playlist);
        playlist.setId(id);
    }

    @Transactional
    public void setFilesInPlaylist(int id, List<MediaFile> files) {
        update("delete from playlist_file where playlist_id=?", id);
        batchedUpdate("insert into playlist_file (playlist_id, media_file_id) values (?, ?)",
                files.stream().map(x -> new Object[] { id, x.getId() }).collect(Collectors.toList()));
    }

    public Pair<Integer, Double> getPlaylistFileStats(int id) {
        return queryOne("select count(*), sum(duration) from media_file m, playlist_file p where p.media_file_id = m.id and p.playlist_id=?", (rs, i) -> Pair.of(rs.getInt(1), rs.getDouble(2)), id);
    }

    public List<String> getPlaylistUsers(int playlistId) {
        return queryForStrings("select username from playlist_user where playlist_id=?", playlistId);
    }

    public void addPlaylistUser(int playlistId, String username) {
        if (!getPlaylistUsers(playlistId).contains(username)) {
            update("insert into playlist_user(playlist_id,username) values (?,?)", playlistId, username);
        }
    }

    public void deletePlaylistUser(int playlistId, String username) {
        update("delete from playlist_user where playlist_id=? and username=?", playlistId, username);
    }

    public void deletePlaylist(int id) {
        update("delete from playlist where id=?", id);
    }

    public void updatePlaylist(Playlist playlist) {
        update("update playlist set username=?, is_public=?, name=?, comment=?, changed=?, imported_from=?, file_count=?, duration=? where id=?",
                playlist.getUsername(), playlist.getShared(), playlist.getName(), playlist.getComment(),
                Instant.now(), playlist.getImportedFrom(), playlist.getFileCount(), playlist.getDuration(),
                playlist.getId());
    }

    private static class PlaylistMapper implements RowMapper<Playlist> {
        @Override
        public Playlist mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new Playlist(
                    rs.getInt(1),
                    rs.getString(2),
                    rs.getBoolean(3),
                    rs.getString(4),
                    rs.getString(5),
                    rs.getInt(6),
                    rs.getDouble(7),
                    Optional.ofNullable(rs.getTimestamp(8)).map(x -> x.toInstant()).orElse(null),
                    Optional.ofNullable(rs.getTimestamp(9)).map(x -> x.toInstant()).orElse(null),
                    rs.getString(10));
        }
    }
}
