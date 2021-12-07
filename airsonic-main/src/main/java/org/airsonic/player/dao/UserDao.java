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

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.ImmutableMap;
import org.airsonic.player.domain.*;
import org.airsonic.player.domain.UserCredential.App;
import org.airsonic.player.util.Util;
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
import java.util.Set;
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

    private static final String USER_COLUMNS = "username, email, ldap_authenticated, bytes_streamed, bytes_downloaded, bytes_uploaded, roles";
    private static final String USER_SETTINGS_COLUMNS = "username, settings";
    private static final String USER_CREDENTIALS_COLUMNS = "username, app_username, credential, encoder, app, created, updated, expiration, comment";

    private UserRowMapper userRowMapper = new UserRowMapper();
    private UserSettingsRowMapper userSettingsRowMapper = new UserSettingsRowMapper();
    private UserCredentialRowMapper userCredentialRowMapper = new UserCredentialRowMapper();

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
            sql = "select " + USER_COLUMNS + " from users where username=?";
        } else {
            sql = "select " + USER_COLUMNS + " from users where UPPER(username)=UPPER(?)";
        }
        List<User> users = query(sql, userRowMapper, username);
        User user = null;
        if (users.size() == 1) {
            user = users.iterator().next();
        } else if (users.size() > 1) {
            throw new RuntimeException("Too many matching users");
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

    /**
     * Returns the user with the given email address.
     *
     * @param email The email address.
     * @return The user, or <code>null</code> if not found.
     */
    public User getUserByEmail(String email) {
        String sql = "select " + USER_COLUMNS + " from users where email=?";
        User user = queryOne(sql, userRowMapper, email);
        return user;
    }

    /**
     * Returns all users.
     *
     * @return Possibly empty array of all users.
     */
    public List<User> getAllUsers() {
        String sql = "select " + USER_COLUMNS + " from users";
        List<User> users = query(sql, userRowMapper);
        return users;
    }

    /**
     * Creates a new user.
     *
     * @param user The user to create.
     */
    public void createUser(User user, UserCredential credential) {
        String sql = "insert into users (" + USER_COLUMNS + ") values (" + questionMarks(USER_COLUMNS) + ")";
        update(sql, user.getUsername(), user.getEmail(), user.isLdapAuthenticated(),
                user.getBytesStreamed(), user.getBytesDownloaded(), user.getBytesUploaded(),
                Util.toJson(user.getRoles()));
        createCredential(credential);
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

        update("delete from player where username=?", username);
        update("delete from user_credentials where username=?", username);
        update("delete from users where username=?", username);
    }

    /**
     * Updates the given user.
     *
     * @param user The user to update.
     */
    public void updateUser(User user) {
        String sql = "update users set email=?, ldap_authenticated=?, bytes_streamed=?, bytes_downloaded=?, bytes_uploaded=?, roles=? where username=?";
        update(sql, user.getEmail(), user.isLdapAuthenticated(),
                user.getBytesStreamed(), user.getBytesDownloaded(), user.getBytesUploaded(),
                Util.toJson(user.getRoles()),
                user.getUsername());
    }

    public void updateUserByteCounts(String user, long bytesStreamedDelta, long bytesDownloadedDelta, long bytesUploadedDelta) {
        String sql = "update users set bytes_streamed=bytes_streamed+?, bytes_downloaded=bytes_downloaded+?, bytes_uploaded=bytes_uploaded+? where username=?";
        update(sql, bytesStreamedDelta, bytesDownloadedDelta, bytesUploadedDelta, user);
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

        return update(sql, settings.getUsername(), Util.toJson(settings)) == 1;
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
            return new User(rs.getString("username"),
                    rs.getString("email"),
                    rs.getBoolean("ldap_authenticated"),
                    rs.getLong("bytes_streamed"),
                    rs.getLong("bytes_downloaded"),
                    rs.getLong("bytes_uploaded"),
                    Optional.ofNullable(Util.fromJson(rs.getString("roles"), new TypeReference<Set<User.Role>>() {})).orElse(Collections.emptySet()));
        }
    }

    private static class UserSettingsRowMapper implements RowMapper<UserSettings> {
        @Override
        public UserSettings mapRow(ResultSet rs, int rowNum) throws SQLException {
            return Util.fromJson(rs.getString("settings"), UserSettings.class);
        }
    }
}
