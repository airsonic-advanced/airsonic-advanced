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

import com.google.common.collect.ImmutableMap;

import org.airsonic.player.domain.*;
import org.airsonic.player.domain.UserCredential.App;
import org.airsonic.player.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Provides user-related database services.
 *
 * @author Sindre Mehus
 */
@Repository
@Transactional
public class UserDao extends AbstractDao {

    private static final Logger LOG = LoggerFactory.getLogger(UserDao.class);
    private static final String USER_COLUMNS = "username, email, ldap_authenticated, bytes_streamed, bytes_downloaded, bytes_uploaded";
    private static final String USER_SETTINGS_COLUMNS = "username, locale, theme_id, final_version_notification, beta_version_notification, " +
            "song_notification, main_track_number, main_artist, main_album, main_genre, " +
            "main_year, main_bit_rate, main_duration, main_format, main_file_size, " +
            "playlist_track_number, playlist_artist, playlist_album, playlist_genre, " +
            "playlist_year, playlist_bit_rate, playlist_duration, playlist_format, playlist_file_size, " +
            "last_fm_enabled, listenbrainz_enabled, " +
            "transcode_scheme, show_now_playing, selected_music_folder_id, " +
            "party_mode_enabled, now_playing_allowed, avatar_scheme, system_avatar_id, changed, show_artist_info, auto_hide_play_queue, " +
            "view_as_list, default_album_list, queue_following_songs, show_side_bar, list_reload_delay, " +
            "keyboard_shortcuts_enabled, pagination_size";
    private static final String USER_CREDENTIALS_COLUMNS = "username, app_username, credential, encoder, app, created, updated, expiration, comment";

    private static final Integer ROLE_ID_ADMIN = 1;
    private static final Integer ROLE_ID_DOWNLOAD = 2;
    private static final Integer ROLE_ID_UPLOAD = 3;
    private static final Integer ROLE_ID_PLAYLIST = 4;
    private static final Integer ROLE_ID_COVER_ART = 5;
    private static final Integer ROLE_ID_COMMENT = 6;
    private static final Integer ROLE_ID_PODCAST = 7;
    private static final Integer ROLE_ID_STREAM = 8;
    private static final Integer ROLE_ID_SETTINGS = 9;
    private static final Integer ROLE_ID_JUKEBOX = 10;
    private static final Integer ROLE_ID_SHARE = 11;

    private UserRowMapper userRowMapper = new UserRowMapper();
    private UserSettingsRowMapper userSettingsRowMapper = new UserSettingsRowMapper();
    private UserCredentialRowMapper userCredentialRowMapper = new UserCredentialRowMapper();

    private final String userTable;

    @Autowired
    public UserDao(@Value("${DatabaseUsertableQuote:}") String userTableQuote) {
        this.userTable = userTableQuote + "user" + userTableQuote;
    }

    /**
     * Returns the user with the given username.
     *
     * @param username The username used when logging in.
     * @param caseSensitive If false, perform a case-insensitive search
     * @return The user, or <code>null</code> if not found.
     */
    public User getUserByName(String username, boolean caseSensitive) {
        String sql;
        if (caseSensitive) {
            sql = "select " + USER_COLUMNS + " from " + getUserTable() + " where username=?";
        } else {
            sql = "select " + USER_COLUMNS + " from " + getUserTable() + " where UPPER(username)=UPPER(?)";
        }
        List<User> users = query(sql, userRowMapper, username);
        User user = null;
        if (users.size() == 1) {
            user = users.iterator().next();
        } else if (users.size() > 1) {
            throw new RuntimeException("Too many matching users");
        }
        if (user != null) {
            readRoles(user);
        }
        return user;
    }

    public List<UserCredential> getCredentials(String username, App... apps) {
        if (apps.length == 0) {
            return Collections.emptyList();
        }
        String sql = "select " + USER_CREDENTIALS_COLUMNS + " from user_credentials where username=:user and app in (:apps)";
        return namedQuery(sql, userCredentialRowMapper, ImmutableMap.of("user", username, "apps", Arrays.asList(apps).stream().map(App::name).collect(Collectors.toList())));
    }

    public List<UserCredential> getCredentialsByEncoder(String encoderPatternMatcher) {
        String sql = "select " + USER_CREDENTIALS_COLUMNS + " from user_credentials where encoder like ?";
        return query(sql, userCredentialRowMapper, encoderPatternMatcher);
    }

    public Integer getCredentialCountByEncoder(String encoderPatternMatcher) {
        String sql = "select count(*) from user_credentials where encoder like ?";
        return queryForInt(sql, 0, encoderPatternMatcher);
    }

    public boolean updateCredential(UserCredential oldCreds, UserCredential newCreds) {
        String sql = "update user_credentials set app_username=?, credential=?, encoder=?, app=?, updated=?, expiration=? where username=? and app_username=? and credential=? and encoder=? and app=? and created=? and updated=?";
        return update(sql, newCreds.getAppUsername(), newCreds.getCredential(), newCreds.getEncoder(),
                newCreds.getApp().name(), newCreds.getUpdated(), newCreds.getExpiration(), oldCreds.getUsername(),
                oldCreds.getAppUsername(), oldCreds.getCredential(), oldCreds.getEncoder(), oldCreds.getApp().name(),
                oldCreds.getCreated(), oldCreds.getUpdated()) == 1;
    }

    public boolean createCredential(UserCredential credential) {
        String sql = "insert into user_credentials (" + USER_CREDENTIALS_COLUMNS + ") values (" + questionMarks(USER_CREDENTIALS_COLUMNS) + ")";
        return update(sql,
                credential.getUsername(),
                credential.getAppUsername(),
                credential.getCredential(),
                credential.getEncoder(),
                credential.getApp().name(),
                credential.getCreated(),
                credential.getUpdated(),
                credential.getExpiration(),
                credential.getComment()) == 1;
    }

    public boolean deleteCredential(UserCredential credential, Predicate<UserCredential> postDeletionCheck) {
        String sql = "delete from user_credentials where username=:username and app_username=:app_username and credential=:credential and encoder=:encoder and app=:app and created=:created and updated=:updated";
        Map<String, Object> args = new HashMap<>();
        args.put("username", credential.getUsername());
        args.put("app_username", credential.getAppUsername());
        args.put("credential", credential.getCredential());
        args.put("encoder", credential.getEncoder());
        args.put("app", credential.getApp().name());
        args.put("created", credential.getCreated());
        args.put("updated", credential.getUpdated());
        if (credential.getExpiration() != null) {
            sql = sql + " and expiration=:expiration";
            args.put("expiration", credential.getExpiration());
        }

        boolean deleteSuccess = namedUpdate(sql, args) == 1;

        if (!postDeletionCheck.test(credential)) {
            throw new RuntimeException("Cannot delete a credential due to failed post deletion check");
        }

        return deleteSuccess;
    }

    public boolean checkCredentialsStoredInLegacyTables() {
        String sql = "select count(*) from " + getUserTable() + " where password!=?";
        if (queryForInt(sql, 0, "") > 0) {
            return true;
        }

        sql = "select count(*) from user_settings where last_fm_password is not null or listenbrainz_token is not null";
        if (queryForInt(sql, 0) > 0) {
            return true;
        }

        return false;
    }

    public boolean purgeCredentialsStoredInLegacyTables() {
        String sql = "update " + getUserTable() + " set password=''";
        int updated = update(sql);

        sql = "update user_settings set last_fm_username=NULL, last_fm_password=NULL, listenbrainz_token=NULL";
        updated += update(sql);

        return updated != 0;
    }

    /**
     * Returns the user with the given email address.
     *
     * @param email The email address.
     * @return The user, or <code>null</code> if not found.
     */
    public User getUserByEmail(String email) {
        String sql = "select " + USER_COLUMNS + " from " + getUserTable() + " where email=?";
        User user = queryOne(sql, userRowMapper, email);
        if (user != null) {
            readRoles(user);
        }
        return user;
    }

    /**
     * Returns all users.
     *
     * @return Possibly empty array of all users.
     */
    public List<User> getAllUsers() {
        String sql = "select " + USER_COLUMNS + " from " + getUserTable();
        List<User> users = query(sql, userRowMapper);
        users.forEach(this::readRoles);
        return users;
    }

    /**
     * Creates a new user.
     *
     * @param user The user to create.
     */
    public void createUser(User user, UserCredential credential) {
        String sql = "insert into " + getUserTable() + " (" + USER_COLUMNS + ", password) values (" + questionMarks(USER_COLUMNS) + ", ?)";
        update(sql, user.getUsername(), user.getEmail(), user.isLdapAuthenticated(),
                user.getBytesStreamed(), user.getBytesDownloaded(), user.getBytesUploaded(), "");
        createCredential(credential);
        writeRoles(user);
    }

    /**
     * Deletes the user with the given username.
     *
     * @param username The username.
     */
    public void deleteUser(String username) {
        if (User.USERNAME_ADMIN.equals(username)) {
            throw new IllegalArgumentException("Can't delete admin user.");
        }

        update("delete from user_role where username=?", username);
        update("delete from player where username=?", username);
        update("delete from user_credentials where username=?", username);
        update("delete from " + getUserTable() + " where username=?", username);
    }

    /**
     * Updates the given user.
     *
     * @param user The user to update.
     */
    public void updateUser(User user) {
        String sql = "update " + getUserTable() + " set email=?, ldap_authenticated=?, bytes_streamed=?, bytes_downloaded=?, bytes_uploaded=? " + "where username=?";
        update(sql, user.getEmail(), user.isLdapAuthenticated(),
                user.getBytesStreamed(), user.getBytesDownloaded(), user.getBytesUploaded(),
                user.getUsername());
        writeRoles(user);
    }

    public void updateUserByteCounts(String user, long bytesStreamedDelta, long bytesDownloadedDelta, long bytesUploadedDelta) {
        String sql = "update " + getUserTable() + " set bytes_streamed=bytes_streamed+?, bytes_downloaded=bytes_downloaded+?, bytes_uploaded=bytes_uploaded+? where username=?";
        update(sql, bytesStreamedDelta, bytesDownloadedDelta, bytesUploadedDelta, user);
    }

    /**
     * Returns the name of the roles for the given user.
     *
     * @param username The user name.
     * @return Roles the user is granted.
     */
    public List<String> getRolesForUser(String username) {
        String sql = "select r.name from role r, user_role ur " +
                "where ur.username=? and ur.role_id=r.id";
        return queryForStrings(sql, username);
    }

    /**
     * Returns settings for the given user.
     *
     * @param username The username.
     * @return User-specific settings, or <code>null</code> if no such settings exist.
     */
    public UserSettings getUserSettings(String username) {
        String sql = "select " + USER_SETTINGS_COLUMNS + " from user_settings where username=?";
        return queryOne(sql, userSettingsRowMapper, username);
    }

    /**
     * Updates settings for the given username, creating it if necessary.
     *
     * @param settings The user-specific settings.
     */
    public boolean updateUserSettings(UserSettings settings) {
        update("delete from user_settings where username=?", settings.getUsername());

        String sql = "insert into user_settings (" + USER_SETTINGS_COLUMNS + ") values (" + questionMarks(USER_SETTINGS_COLUMNS) + ")";
        String locale = settings.getLocale() == null ? null : settings.getLocale().toString();
        UserSettings.Visibility main = settings.getMainVisibility();
        UserSettings.Visibility playlist = settings.getPlaylistVisibility();
        return update(sql, settings.getUsername(), locale, settings.getThemeId(),
                settings.isFinalVersionNotificationEnabled(), settings.isBetaVersionNotificationEnabled(),
                settings.isSongNotificationEnabled(), main.isTrackNumberVisible(),
                main.isArtistVisible(), main.isAlbumVisible(), main.isGenreVisible(), main.isYearVisible(),
                main.isBitRateVisible(), main.isDurationVisible(), main.isFormatVisible(), main.isFileSizeVisible(),
                playlist.isTrackNumberVisible(), playlist.isArtistVisible(), playlist.isAlbumVisible(),
                playlist.isGenreVisible(), playlist.isYearVisible(), playlist.isBitRateVisible(), playlist.isDurationVisible(),
                playlist.isFormatVisible(), playlist.isFileSizeVisible(),
                settings.isLastFmEnabled(), settings.isListenBrainzEnabled(),
                settings.getTranscodeScheme().name(), settings.isShowNowPlayingEnabled(),
                settings.getSelectedMusicFolderId(), settings.isPartyModeEnabled(), settings.isNowPlayingAllowed(),
                settings.getAvatarScheme().name(), settings.getSystemAvatarId(), settings.getChanged(),
                settings.isShowArtistInfoEnabled(), settings.isAutoHidePlayQueue(),
                settings.isViewAsList(), settings.getDefaultAlbumList().getId(), settings.isQueueFollowingSongs(),
                settings.isShowSideBar(), 60 /* Unused listReloadDelay */, settings.isKeyboardShortcutsEnabled(),
                settings.getPaginationSize()) == 1;
    }

    private void readRoles(User user) {
        String sql = "select role_id from user_role where username=?";
        List<Integer> roles = queryForInts(sql, user.getUsername());
        for (Object role : roles) {
            if (ROLE_ID_ADMIN.equals(role)) {
                user.setAdminRole(true);
            } else if (ROLE_ID_DOWNLOAD.equals(role)) {
                user.setDownloadRole(true);
            } else if (ROLE_ID_UPLOAD.equals(role)) {
                user.setUploadRole(true);
            } else if (ROLE_ID_PLAYLIST.equals(role)) {
                user.setPlaylistRole(true);
            } else if (ROLE_ID_COVER_ART.equals(role)) {
                user.setCoverArtRole(true);
            } else if (ROLE_ID_COMMENT.equals(role)) {
                user.setCommentRole(true);
            } else if (ROLE_ID_PODCAST.equals(role)) {
                user.setPodcastRole(true);
            } else if (ROLE_ID_STREAM.equals(role)) {
                user.setStreamRole(true);
            } else if (ROLE_ID_SETTINGS.equals(role)) {
                user.setSettingsRole(true);
            } else if (ROLE_ID_JUKEBOX.equals(role)) {
                user.setJukeboxRole(true);
            } else if (ROLE_ID_SHARE.equals(role)) {
                user.setShareRole(true);
            } else {
                LOG.warn("Unknown role: {}", role);
            }
        }
    }

    private void writeRoles(User user) {
        String sql = "delete from user_role where username=?";
        update(sql, user.getUsername());
        sql = "insert into user_role (username, role_id) values(?, ?)";
        if (user.isAdminRole()) {
            update(sql, user.getUsername(), ROLE_ID_ADMIN);
        }
        if (user.isDownloadRole()) {
            update(sql, user.getUsername(), ROLE_ID_DOWNLOAD);
        }
        if (user.isUploadRole()) {
            update(sql, user.getUsername(), ROLE_ID_UPLOAD);
        }
        if (user.isPlaylistRole()) {
            update(sql, user.getUsername(), ROLE_ID_PLAYLIST);
        }
        if (user.isCoverArtRole()) {
            update(sql, user.getUsername(), ROLE_ID_COVER_ART);
        }
        if (user.isCommentRole()) {
            update(sql, user.getUsername(), ROLE_ID_COMMENT);
        }
        if (user.isPodcastRole()) {
            update(sql, user.getUsername(), ROLE_ID_PODCAST);
        }
        if (user.isStreamRole()) {
            update(sql, user.getUsername(), ROLE_ID_STREAM);
        }
        if (user.isJukeboxRole()) {
            update(sql, user.getUsername(), ROLE_ID_JUKEBOX);
        }
        if (user.isSettingsRole()) {
            update(sql, user.getUsername(), ROLE_ID_SETTINGS);
        }
        if (user.isShareRole()) {
            update(sql, user.getUsername(), ROLE_ID_SHARE);
        }
    }

    private class UserCredentialRowMapper implements RowMapper<UserCredential> {
        @Override
        public UserCredential mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new UserCredential(
                    rs.getString("username"),
                    rs.getString("app_username"),
                    rs.getString("credential"),
                    rs.getString("encoder"),
                    App.valueOf(rs.getString("app")),
                    rs.getString("comment"),
                    Optional.ofNullable(rs.getTimestamp("expiration")).map(x -> x.toInstant()).orElse(null),
                    rs.getTimestamp("created").toInstant(),
                    rs.getTimestamp("updated").toInstant());
        }
    }

    private class UserRowMapper implements RowMapper<User> {
        @Override
        public User mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new User(rs.getString(1),
                    rs.getString(2),
                    rs.getBoolean(3),
                    rs.getLong(4),
                    rs.getLong(5),
                    rs.getLong(6));
        }
    }

    private static class UserSettingsRowMapper implements RowMapper<UserSettings> {
        @Override
        public UserSettings mapRow(ResultSet rs, int rowNum) throws SQLException {
            int col = 1;
            UserSettings settings = new UserSettings(rs.getString(col++));
            settings.setLocale(StringUtil.parseLocale(rs.getString(col++)));
            settings.setThemeId(rs.getString(col++));
            settings.setFinalVersionNotificationEnabled(rs.getBoolean(col++));
            settings.setBetaVersionNotificationEnabled(rs.getBoolean(col++));
            settings.setSongNotificationEnabled(rs.getBoolean(col++));

            settings.getMainVisibility().setTrackNumberVisible(rs.getBoolean(col++));
            settings.getMainVisibility().setArtistVisible(rs.getBoolean(col++));
            settings.getMainVisibility().setAlbumVisible(rs.getBoolean(col++));
            settings.getMainVisibility().setGenreVisible(rs.getBoolean(col++));
            settings.getMainVisibility().setYearVisible(rs.getBoolean(col++));
            settings.getMainVisibility().setBitRateVisible(rs.getBoolean(col++));
            settings.getMainVisibility().setDurationVisible(rs.getBoolean(col++));
            settings.getMainVisibility().setFormatVisible(rs.getBoolean(col++));
            settings.getMainVisibility().setFileSizeVisible(rs.getBoolean(col++));

            settings.getPlaylistVisibility().setTrackNumberVisible(rs.getBoolean(col++));
            settings.getPlaylistVisibility().setArtistVisible(rs.getBoolean(col++));
            settings.getPlaylistVisibility().setAlbumVisible(rs.getBoolean(col++));
            settings.getPlaylistVisibility().setGenreVisible(rs.getBoolean(col++));
            settings.getPlaylistVisibility().setYearVisible(rs.getBoolean(col++));
            settings.getPlaylistVisibility().setBitRateVisible(rs.getBoolean(col++));
            settings.getPlaylistVisibility().setDurationVisible(rs.getBoolean(col++));
            settings.getPlaylistVisibility().setFormatVisible(rs.getBoolean(col++));
            settings.getPlaylistVisibility().setFileSizeVisible(rs.getBoolean(col++));

            settings.setLastFmEnabled(rs.getBoolean(col++));
            settings.setListenBrainzEnabled(rs.getBoolean(col++));

            settings.setTranscodeScheme(TranscodeScheme.valueOf(rs.getString(col++)));
            settings.setShowNowPlayingEnabled(rs.getBoolean(col++));
            settings.setSelectedMusicFolderId(rs.getInt(col++));
            settings.setPartyModeEnabled(rs.getBoolean(col++));
            settings.setNowPlayingAllowed(rs.getBoolean(col++));
            settings.setAvatarScheme(AvatarScheme.valueOf(rs.getString(col++)));
            settings.setSystemAvatarId((Integer) rs.getObject(col++));
            settings.setChanged(Optional.ofNullable(rs.getTimestamp(col++)).map(x -> x.toInstant()).orElse(null));
            settings.setShowArtistInfoEnabled(rs.getBoolean(col++));
            settings.setAutoHidePlayQueue(rs.getBoolean(col++));
            settings.setViewAsList(rs.getBoolean(col++));
            settings.setDefaultAlbumList(AlbumListType.fromId(rs.getString(col++)));
            settings.setQueueFollowingSongs(rs.getBoolean(col++));
            settings.setShowSideBar(rs.getBoolean(col++));
            col++;  // Skip the now unused listReloadDelay
            settings.setKeyboardShortcutsEnabled(rs.getBoolean(col++));
            settings.setPaginationSize(rs.getInt(col++));

            return settings;
        }
    }

    String getUserTable() {
        return userTable;
    }
}
