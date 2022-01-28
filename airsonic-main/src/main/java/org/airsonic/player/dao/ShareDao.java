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

import org.airsonic.player.domain.MusicFolder;
import org.airsonic.player.domain.Share;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Provides database services for shared media.
 *
 * @author Sindre Mehus
 */
@Repository
public class ShareDao extends AbstractDao {

    private static final String INSERT_COLUMNS = "name, description, username, created, expires, last_visited, visit_count";
    private static final String QUERY_COLUMNS = "id, " + INSERT_COLUMNS;

    private ShareRowMapper shareRowMapper = new ShareRowMapper();
    private ShareFileRowMapper shareFileRowMapper = new ShareFileRowMapper();

    @PostConstruct
    public void register() throws Exception {
        registerInserts("share", "id", Arrays.asList(INSERT_COLUMNS.split(", ")), Share.class);
    }

    /**
     * Creates a new share.
     *
     * @param share The share to create.  The ID of the share will be set by this method.
     */
    public void createShare(Share share) {
        Integer id = insert("share", share);
        share.setId(id);
    }

    /**
     * Returns all shares.
     *
     * @return Possibly empty list of all shares.
     */
    public List<Share> getAllShares() {
        String sql = "select " + QUERY_COLUMNS + " from share";
        return query(sql, shareRowMapper);
    }

    public Share getShareByName(String shareName) {
        String sql = "select " + QUERY_COLUMNS + " from share where name=?";
        return queryOne(sql, shareRowMapper, shareName);
    }

    public Share getShareById(int id) {
        String sql = "select " + QUERY_COLUMNS + " from share where id=?";
        return queryOne(sql, shareRowMapper, id);
    }

    public void updateShare(Share share) {
        String sql = "update share set name=?, description=?, username=?, created=?, expires=?, last_visited=?, visit_count=? where id=?";
        update(sql, share.getName(), share.getDescription(), share.getUsername(), share.getCreated(), share.getExpires(),
                share.getLastVisited(), share.getVisitCount(), share.getId());
    }

    @Transactional
    public void createSharedFiles(int shareId, List<Integer> mediaFileIds) {
        if (mediaFileIds == null || mediaFileIds.isEmpty()) {
            return;
        }
        String sql = "insert into share_file (share_id, media_file_id) values (?, ?)";
        batchedUpdate(sql, mediaFileIds.stream().map(x -> new Object[] { shareId, x }).collect(Collectors.toList()));
    }

    public List<Integer> getSharedFiles(final int shareId, final List<MusicFolder> musicFolders) {
        if (musicFolders.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Object> args = new HashMap<>();
        args.put("shareId", shareId);
        args.put("folders", MusicFolder.toIdList(musicFolders));
        return namedQuery("select share_file.media_file_id from share_file, media_file where share_id = :shareId and "
                + "share_file.media_file_id = media_file.id and media_file.present and media_file.folder_id in (:folders)",
                          shareFileRowMapper, args);
    }

    public void deleteShare(Integer id) {
        update("delete from share where id=?", id);
    }

    private static class ShareRowMapper implements RowMapper<Share> {
        @Override
        public Share mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new Share(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4), Optional.ofNullable(rs.getTimestamp(5)).map(x -> x.toInstant()).orElse(null),
                    Optional.ofNullable(rs.getTimestamp(6)).map(x -> x.toInstant()).orElse(null), Optional.ofNullable(rs.getTimestamp(7)).map(x -> x.toInstant()).orElse(null), rs.getInt(8));
        }
    }

    private static class ShareFileRowMapper implements RowMapper<Integer> {
        @Override
        public Integer mapRow(ResultSet rs, int rowNum) throws SQLException {
            return rs.getInt(1);
        }

    }
}
