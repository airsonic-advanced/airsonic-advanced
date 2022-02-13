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

import org.airsonic.player.domain.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Provides player-related database services.
 *
 * @author Sindre Mehus
 */
@Repository
public class PlayerDao extends AbstractDao {

    private static final Logger LOG = LoggerFactory.getLogger(PlayerDao.class);
    private static final String INSERT_COLUMNS = "name, type, username, ip_address, auto_control_enabled, m3u_bom_enabled, " +
                                                 "last_seen, transcode_scheme, dynamic_ip, technology, client_id";
    private static final String QUERY_COLUMNS = "id, " + INSERT_COLUMNS;

    @Autowired
    private PlayerDaoPlayQueueFactory playerDaoPlayQueueFactory;

    private PlayerRowMapper rowMapper = new PlayerRowMapper();
    private Map<Integer, PlayQueue> playlists = Collections.synchronizedMap(new HashMap<Integer, PlayQueue>());

    @PostConstruct
    public void register() throws Exception {
        registerInserts("player", "id", Arrays.asList(INSERT_COLUMNS.split(", ")), Player.class);
    }

    /**
     * Returns all players.
     *
     * @return Possibly empty list of all users.
     */
    public List<Player> getAllPlayers() {
        String sql = "select " + QUERY_COLUMNS + " from player";
        return query(sql, rowMapper);
    }

    /**
     * Returns all players owned by the given username and client ID.
     *
     * @param username The name of the user.
     * @param clientId The third-party client ID (used if this player is managed over the
     *                 Airsonic REST API). May be <code>null</code>.
     * @return All relevant players.
     */
    public List<Player> getPlayersForUserAndClientId(String username, String clientId) {
        if (clientId != null) {
            String sql = "select " + QUERY_COLUMNS + " from player where username=? and client_id=?";
            return query(sql, rowMapper, username, clientId);
        } else {
            String sql = "select " + QUERY_COLUMNS + " from player where username=? and client_id is null";
            return query(sql, rowMapper, username);
        }
    }

    /**
     * Returns the player with the given ID.
     *
     * @param id The unique player ID.
     * @return The player with the given ID, or <code>null</code> if no such player exists.
     */
    public Player getPlayerById(int id) {
        String sql = "select " + QUERY_COLUMNS + " from player where id=?";
        return queryOne(sql, rowMapper, id);
    }

    /**
     * Creates a new player.
     *
     * @param player The player to create.
     */
    public void createPlayer(Player player) {
        Integer id = insert("player", player);
        // never Player 0 due to odd bug cataloged in
        // https://github.com/airsonic-advanced/airsonic-advanced/issues/646
        if (id.equals(0)) {
            deletePlayer(0);
            id = insert("player", player);
        }
        player.setId(id);
        addPlaylist(player);

        LOG.info("Created player {}", id);
    }

    /**
     * Deletes the player with the given ID.
     *
     * @param id The player ID.
     */
    public void deletePlayer(Integer id) {
        String sql = "delete from player where id=?";
        update(sql, id);
        playlists.remove(id);
    }


    /**
     * Delete players that haven't been used for the given number of days, and which is not given a name
     * or is used by a REST client.
     *
     * @param days Number of days.
     */
    public void deleteOldPlayers(int days) {
        Instant lastSeen = Instant.now().minus(days, ChronoUnit.DAYS);
        String sql = "delete from player where name is null and client_id is null and (last_seen is null or last_seen < ?)";
        int n = update(sql, lastSeen);
        if (n > 0) {
            LOG.info("Deleted {} player(s) that haven't been used after {}", n, lastSeen);
        }
    }

    /**
     * Updates the given player.
     *
     * @param player The player to update.
     */
    public void updatePlayer(Player player) {
        String sql = "update player set " +
                     "name = ?," +
                     "type = ?," +
                     "username = ?," +
                     "ip_address = ?," +
                     "auto_control_enabled = ?," +
                     "m3u_bom_enabled = ?," +
                     "last_seen = ?," +
                     "transcode_scheme = ?, " +
                     "dynamic_ip = ?, " +
                     "technology = ?, " +
                     "client_id = ? " +
                     "where id = ?";
        update(sql, player.getName(), player.getType(), player.getUsername(),
               player.getIpAddress(), player.getAutoControlEnabled(), player.getM3uBomEnabled(),
               player.getLastSeen(), player.getTranscodeScheme().name(), player.getDynamicIp(),
                player.getTechnology().name(), player.getClientId(), player.getId());
    }

    private void addPlaylist(Player player) {
        PlayQueue playQueue = playlists.get(player.getId());
        if (playQueue == null) {
            playQueue = playerDaoPlayQueueFactory.createPlayQueue();
            playlists.put(player.getId(), playQueue);
        }
        player.setPlayQueue(playQueue);
    }

    private class PlayerRowMapper implements RowMapper<Player> {
        @Override
        public Player mapRow(ResultSet rs, int rowNum) throws SQLException {
            Player player = new Player();
            player.setId(rs.getInt("id"));
            player.setName(rs.getString("name"));
            player.setType(rs.getString("type"));
            player.setUsername(rs.getString("username"));
            player.setIpAddress(rs.getString("ip_address"));
            player.setAutoControlEnabled(rs.getBoolean("auto_control_enabled"));
            player.setM3uBomEnabled(rs.getBoolean("m3u_bom_enabled"));
            player.setLastSeen(Optional.ofNullable(rs.getTimestamp("last_seen")).map(x -> x.toInstant()).orElse(null));
            player.setTranscodeScheme(TranscodeScheme.valueOf(rs.getString("transcode_scheme")));
            player.setDynamicIp(rs.getBoolean("dynamic_ip"));
            player.setTechnology(PlayerTechnology.valueOf(rs.getString("technology")));
            player.setClientId(rs.getString("client_id"));

            addPlaylist(player);
            return player;
        }
    }
}
