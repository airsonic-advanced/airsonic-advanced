package org.airsonic.player.spring.migrations;

import liquibase.change.custom.CustomSqlChange;
import liquibase.database.Database;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.CustomChangeException;
import liquibase.exception.DatabaseException;
import liquibase.exception.SetupException;
import liquibase.exception.ValidationErrors;
import liquibase.resource.ResourceAccessor;
import liquibase.statement.SqlStatement;
import liquibase.statement.core.InsertStatement;
import org.airsonic.player.domain.AlbumListType;
import org.airsonic.player.domain.AvatarScheme;
import org.airsonic.player.domain.TranscodeScheme;
import org.airsonic.player.domain.UserSettings;
import org.airsonic.player.util.StringUtil;
import org.airsonic.player.util.Util;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserSettingsJsonTablePopulation implements CustomSqlChange {

    @Override
    public String getConfirmationMessage() {
        return "Table user_settings2 (for json) populated from user_settings";
    }

    @Override
    public void setUp() throws SetupException {
    }

    @Override
    public void setFileOpener(ResourceAccessor resourceAccessor) {
    }

    @Override
    public ValidationErrors validate(Database database) {
        return new ValidationErrors();
    }

    @Override
    public SqlStatement[] generateStatements(Database database) throws CustomChangeException {
        List<UserSettings> userSettingRows = new ArrayList<>();
        JdbcConnection conn = null;
        if (database.getConnection() instanceof JdbcConnection) {
            conn = (JdbcConnection) database.getConnection();
        }

        if (conn != null) {
            try (Statement st = conn.createStatement();
                    ResultSet result = st.executeQuery("SELECT * FROM user_settings");) {

                while (result.next()) {
                    userSettingRows.add(mapRow(result));
                }
            } catch (DatabaseException | SQLException e) {
                throw new CustomChangeException(e);
            }
        }

        return userSettingRows.parallelStream()
                .map(usr -> new InsertStatement(database.getDefaultCatalogName(), database.getDefaultSchemaName(), "user_settings2")
                        .addColumnValue("username", usr.getUsername())
                        .addColumnValue("settings", Util.toJson(usr)))
                .toArray(SqlStatement[]::new);
    }

    private static UserSettings mapRow(ResultSet rs) throws SQLException {
        // These fields are frozen at time of migration of UserSettings to JSON in
        // user-settings-json.xml
        UserSettings settings = new UserSettings(rs.getString("username"));
        settings.setLocale(StringUtil.parseLocale(rs.getString("locale")));
        settings.setThemeId(rs.getString("theme_id"));
        settings.setFinalVersionNotificationEnabled(rs.getBoolean("final_version_notification"));
        settings.setBetaVersionNotificationEnabled(rs.getBoolean("beta_version_notification"));
        settings.setSongNotificationEnabled(rs.getBoolean("song_notification"));

        settings.getMainVisibility().setTrackNumberVisible(rs.getBoolean("main_track_number"));
        settings.getMainVisibility().setArtistVisible(rs.getBoolean("main_artist"));
        settings.getMainVisibility().setAlbumVisible(rs.getBoolean("main_album"));
        settings.getMainVisibility().setGenreVisible(rs.getBoolean("main_genre"));
        settings.getMainVisibility().setYearVisible(rs.getBoolean("main_year"));
        settings.getMainVisibility().setBitRateVisible(rs.getBoolean("main_bit_rate"));
        settings.getMainVisibility().setDurationVisible(rs.getBoolean("main_duration"));
        settings.getMainVisibility().setFormatVisible(rs.getBoolean("main_format"));
        settings.getMainVisibility().setFileSizeVisible(rs.getBoolean("main_file_size"));

        settings.getPlaylistVisibility().setTrackNumberVisible(rs.getBoolean("playlist_track_number"));
        settings.getPlaylistVisibility().setArtistVisible(rs.getBoolean("playlist_artist"));
        settings.getPlaylistVisibility().setAlbumVisible(rs.getBoolean("playlist_album"));
        settings.getPlaylistVisibility().setGenreVisible(rs.getBoolean("playlist_genre"));
        settings.getPlaylistVisibility().setYearVisible(rs.getBoolean("playlist_year"));
        settings.getPlaylistVisibility().setBitRateVisible(rs.getBoolean("playlist_bit_rate"));
        settings.getPlaylistVisibility().setDurationVisible(rs.getBoolean("playlist_duration"));
        settings.getPlaylistVisibility().setFormatVisible(rs.getBoolean("playlist_format"));
        settings.getPlaylistVisibility().setFileSizeVisible(rs.getBoolean("playlist_file_size"));

        settings.setLastFmEnabled(rs.getBoolean("last_fm_enabled"));
        settings.setListenBrainzEnabled(rs.getBoolean("listenbrainz_enabled"));

        settings.setTranscodeScheme(TranscodeScheme.valueOf(rs.getString("transcode_scheme")));
        settings.setShowNowPlayingEnabled(rs.getBoolean("show_now_playing"));
        settings.setSelectedMusicFolderId(rs.getInt("selected_music_folder_id"));
        settings.setPartyModeEnabled(rs.getBoolean("party_mode_enabled"));
        settings.setNowPlayingAllowed(rs.getBoolean("now_playing_allowed"));
        settings.setAvatarScheme(AvatarScheme.valueOf(rs.getString("avatar_scheme")));
        settings.setSystemAvatarId((Integer) rs.getObject("system_avatar_id"));
        settings.setChanged(Optional.ofNullable(rs.getTimestamp("changed")).map(x -> x.toInstant()).orElse(null));
        settings.setShowArtistInfoEnabled(rs.getBoolean("show_artist_info"));
        settings.setAutoHidePlayQueue(rs.getBoolean("auto_hide_play_queue"));
        settings.setViewAsList(rs.getBoolean("view_as_list"));
        settings.setDefaultAlbumList(AlbumListType.fromId(rs.getString("default_album_list")));
        settings.setQueueFollowingSongs(rs.getBoolean("queue_following_songs"));
        settings.setShowSideBar(rs.getBoolean("show_side_bar"));
        settings.setKeyboardShortcutsEnabled(rs.getBoolean("keyboard_shortcuts_enabled"));
        settings.setPaginationSizeFiles(rs.getInt("pagination_size"));
        settings.setPaginationSizeFolders(rs.getInt("pagination_size"));
        settings.setPaginationSizePlaylist(rs.getInt("pagination_size"));
        settings.setPaginationSizePlayqueue(rs.getInt("pagination_size"));

        return settings;
    }

}
