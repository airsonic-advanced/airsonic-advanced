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
package org.airsonic.player.dao;

import org.airsonic.player.domain.SavedPlayQueue;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Provides database services for play queues
 *
 * @author Sindre Mehus
 */
@Repository
public class PlayQueueDao extends AbstractDao {

    private static final String INSERT_COLUMNS = "username, current_media_file_id, position_millis, changed, changed_by";
    private static final String QUERY_COLUMNS = "id, " + INSERT_COLUMNS;
    private final PlayQueueMapper rowMapper = new PlayQueueMapper();

    @PostConstruct
    public void register() throws Exception {
        registerInserts("play_queue", "id", Arrays.asList(INSERT_COLUMNS.split(", ")), SavedPlayQueue.class);
    }

    @Transactional
    public SavedPlayQueue getPlayQueue(String username) {
        SavedPlayQueue playQueue = queryOne("select " + QUERY_COLUMNS + " from play_queue where username=?", rowMapper, username);
        if (playQueue == null) {
            return null;
        }
        List<Integer> mediaFileIds = queryForInts("select media_file_id from play_queue_file where play_queue_id = ?", playQueue.getId());
        playQueue.setMediaFileIds(mediaFileIds);
        return playQueue;
    }

    @Transactional
    public void savePlayQueue(SavedPlayQueue playQueue) {
        update("delete from play_queue where username=?", playQueue.getUsername());
        Integer id = insert("play_queue", playQueue);
        playQueue.setId(id);

        batchedUpdate("insert into play_queue_file(play_queue_id, media_file_id) values (?, ?)",
                playQueue.getMediaFileIds().stream().map(x -> new Object[] { id, x }).collect(Collectors.toList()));
    }

    private static class PlayQueueMapper implements RowMapper<SavedPlayQueue> {
        @Override
        public SavedPlayQueue mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new SavedPlayQueue(rs.getInt(1),
                                      rs.getString(2),
                                      null,
                                      rs.getInt(3),
                                      rs.getLong(4),
                                      Optional.ofNullable(rs.getTimestamp(5)).map(x -> x.toInstant()).orElse(null),
                                      rs.getString(6));
        }
    }
}
