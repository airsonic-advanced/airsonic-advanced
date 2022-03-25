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

import org.airsonic.player.domain.PodcastChannel;
import org.airsonic.player.domain.PodcastChannelRule;
import org.airsonic.player.domain.PodcastEpisode;
import org.airsonic.player.domain.PodcastStatus;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Provides database services for Podcast channels and episodes.
 *
 * @author Sindre Mehus
 */
@Repository
public class PodcastDao extends AbstractDao {

    private static final String CHANNEL_INSERT_COLUMNS = "url, title, description, image_url, status, error_message, media_file_id";
    private static final String CHANNEL_QUERY_COLUMNS = "id, " + CHANNEL_INSERT_COLUMNS;
    private static final String CHANNEL_RULES_COLUMNS = "id, check_interval, retention_count, download_count";
    private static final String EPISODE_INSERT_COLUMNS = "channel_id, episode_guid, url, media_file_id, title, description, publish_date, " +
                                                        "duration, bytes_total, bytes_downloaded, status, error_message";
    private static final String EPISODE_QUERY_COLUMNS = "id, " + EPISODE_INSERT_COLUMNS;

    private PodcastChannelRowMapper channelRowMapper = new PodcastChannelRowMapper();
    private PodcastChannelRuleRowMapper channelRuleRowMapper = new PodcastChannelRuleRowMapper();
    private PodcastEpisodeRowMapper episodeRowMapper = new PodcastEpisodeRowMapper();

    @PostConstruct
    public void register() throws Exception {
        registerInserts("podcast_channel", "id", Arrays.asList(CHANNEL_INSERT_COLUMNS.split(", ")), PodcastChannel.class);
        registerInserts("podcast_channel_rules", null, Arrays.asList(CHANNEL_RULES_COLUMNS.split(", ")), PodcastChannelRule.class);
        registerInserts("podcast_episode", "id", Arrays.asList(EPISODE_INSERT_COLUMNS.split(", ")), PodcastEpisode.class);
    }

    /**
     * Creates a new Podcast channel.
     *
     * @param channel The Podcast channel to create.
     * @return The ID of the newly created channel.
     */
    public int createChannel(PodcastChannel channel) {
        return insert("podcast_channel", channel);
    }

    /**
     * Returns all Podcast channels.
     *
     * @return Possibly empty list of all Podcast channels.
     */
    public List<PodcastChannel> getAllChannels() {
        String sql = "select " + CHANNEL_QUERY_COLUMNS + " from podcast_channel";
        return query(sql, channelRowMapper);
    }

    /**
     * Returns a single Podcast channel.
     */
    public PodcastChannel getChannel(int channelId) {
        String sql = "select " + CHANNEL_QUERY_COLUMNS + " from podcast_channel where id=?";
        return queryOne(sql, channelRowMapper, channelId);
    }

    /**
     * Updates the given Podcast channel.
     *
     * @param channel The Podcast channel to update.
     */
    public void updateChannel(PodcastChannel channel) {
        String sql = "update podcast_channel set url=?, title=?, description=?, image_url=?, status=?, error_message=?, media_file_id=? where id=?";
        update(sql, channel.getUrl(), channel.getTitle(), channel.getDescription(), channel.getImageUrl(),
                channel.getStatus().name(), channel.getErrorMessage(), channel.getMediaFileId(), channel.getId());
    }

    public void deleteChannel(int id) {
        String sql = "delete from podcast_channel where id=?";
        update(sql, id);
    }

    public int createChannelRule(PodcastChannelRule rule) {
        return update("insert into podcast_channel_rules(" + CHANNEL_RULES_COLUMNS + ") values (" + questionMarks(CHANNEL_RULES_COLUMNS) + ")", rule.getId(), rule.getCheckInterval(), rule.getRetentionCount(), rule.getDownloadCount());
    }

    public int updateChannelRule(PodcastChannelRule rule) {
        String sql = "update podcast_channel_rules set check_interval=?, retention_count=?, download_count=? where id=?";
        return update(sql, rule.getCheckInterval(), rule.getRetentionCount(), rule.getDownloadCount(), rule.getId());
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void createOrUpdateChannelRule(PodcastChannelRule rule) {
        int updated = updateChannelRule(rule);
        if (updated == 0) {
            createChannelRule(rule);
        }
    }

    public void deleteChannelRule(int id) {
        String sql = "delete from podcast_channel_rules where id=?";
        update(sql, id);
    }

    public PodcastChannelRule getChannelRule(int id) {
        String sql = "select " + CHANNEL_RULES_COLUMNS + " from podcast_channel_rules where id=?";
        return queryOne(sql, channelRuleRowMapper, id);
    }

    public List<PodcastChannelRule> getAllChannelRules() {
        String sql = "select " + CHANNEL_RULES_COLUMNS + " from podcast_channel_rules";
        return query(sql, channelRuleRowMapper);
    }

    /**
     * Creates a new Podcast episode.
     *
     * @param episode The Podcast episode to create.
     */
    public void createEpisode(PodcastEpisode episode) {
        insert("podcast_episode", episode);
    }

    /**
     * Returns all Podcast episodes for a given channel.
     *
     * @return Possibly empty list of all Podcast episodes for the given channel, sorted in
     *         reverse chronological order (newest episode first).
     */
    public List<PodcastEpisode> getEpisodes(int channelId) {
        String sql = "select " + EPISODE_QUERY_COLUMNS + " from podcast_episode where channel_id = ? and status != ?";
        List<PodcastEpisode> result = query(sql, episodeRowMapper, channelId, PodcastStatus.DELETED.name());
        result.sort(Comparator.comparing(PodcastEpisode::getPublishDate, Comparator.nullsLast(Comparator.reverseOrder())));
        return result;
    }

    /**
     * Returns the N newest episodes.
     *
     * @return Possibly empty list of the newest Podcast episodes, sorted in
     *         reverse chronological order (newest episode first).
     */
    public List<PodcastEpisode> getNewestEpisodes(int count) {
        String sql = "select " + EPISODE_QUERY_COLUMNS +
                     " from podcast_episode where status = ? and publish_date is not null and media_file_id is not null" +
                     " order by publish_date desc, id limit ?";
        return query(sql, episodeRowMapper, PodcastStatus.COMPLETED.name(), count);
    }

    /**
     * Returns the Podcast episode with the given ID.
     *
     * @param episodeId The Podcast episode ID.
     * @return The episode or <code>null</code> if not found.
     */
    public PodcastEpisode getEpisode(int episodeId) {
        String sql = "select " + EPISODE_QUERY_COLUMNS + " from podcast_episode where id=?";
        return queryOne(sql, episodeRowMapper, episodeId);
    }

    public PodcastEpisode getEpisodeByUrl(Integer channelId, String url) {
        String sql = "select " + EPISODE_QUERY_COLUMNS + " from podcast_episode where channel_id=? and url=?";
        return queryOne(sql, episodeRowMapper, channelId, url);
    }

    public PodcastEpisode getEpisodeByGuid(Integer channelId, String guid) {
        String sql = "select " + EPISODE_QUERY_COLUMNS + " from podcast_episode where channel_id=? and episode_guid=?";
        return queryOne(sql, episodeRowMapper, channelId, guid);
    }

    public PodcastEpisode getEpisodeByTitleAndDate(Integer channelId, String title, Instant pubDate) {
        String sql = "select " + EPISODE_QUERY_COLUMNS + " from podcast_episode where channel_id=? and title=? and publish_date=?";
        return queryOne(sql, episodeRowMapper, channelId, title, pubDate);
    }

    /**
     * Updates the given Podcast episode.
     *
     * @param episode The Podcast episode to update.
     * @return The number of episodes updated (zero or one).
     */
    public int updateEpisode(PodcastEpisode episode) {
        String sql = "update podcast_episode set episode_guid=?, url=?, media_file_id=?, title=?, description=?, publish_date=?, duration=?, " +
                "bytes_total=?, bytes_downloaded=?, status=?, error_message=? where id=?";
        return update(sql, episode.getEpisodeGuid(), episode.getUrl(), episode.getMediaFileId(), episode.getTitle(),
                episode.getDescription(), episode.getPublishDate(), episode.getDuration(),
                episode.getBytesTotal(), episode.getBytesDownloaded(), episode.getStatus().name(),
                episode.getErrorMessage(), episode.getId());
    }

    /**
     * Deletes the Podcast episode with the given ID.
     *
     * @param id The Podcast episode ID.
     */
    public void deleteEpisode(int id) {
        String sql = "delete from podcast_episode where id=?";
        update(sql, id);
    }

    private static class PodcastChannelRowMapper implements RowMapper<PodcastChannel> {
        @Override
        public PodcastChannel mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new PodcastChannel(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5),
                    PodcastStatus.valueOf(rs.getString(6)), rs.getString(7), (Integer) rs.getObject(8));
        }
    }

    private static class PodcastChannelRuleRowMapper implements RowMapper<PodcastChannelRule> {
        @Override
        public PodcastChannelRule mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new PodcastChannelRule(rs.getInt("id"), rs.getInt("check_interval"), rs.getInt("retention_count"), rs.getInt("download_count"));
        }
    }

    private static class PodcastEpisodeRowMapper implements RowMapper<PodcastEpisode> {
        @Override
        public PodcastEpisode mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new PodcastEpisode(rs.getInt(1), rs.getInt(2), rs.getString(3), rs.getString(4), (Integer) rs.getObject(5),
                    rs.getString(6), rs.getString(7), Optional.ofNullable(rs.getTimestamp(8)).map(x -> x.toInstant()).orElse(null), rs.getString(9), (Long) rs.getObject(10),
                    (Long) rs.getObject(11), PodcastStatus.valueOf(rs.getString(12)), rs.getString(13));
        }
    }
}
