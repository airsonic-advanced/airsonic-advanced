package org.airsonic.player.dao;

import org.airsonic.player.domain.AvatarScheme;
import org.airsonic.player.domain.TranscodeScheme;
import org.airsonic.player.domain.User;
import org.airsonic.player.domain.UserCredential;
import org.airsonic.player.domain.UserCredential.App;
import org.airsonic.player.domain.UserSettings;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;


/**
 * Unit test of {@link UserDao}.
 *
 * @author Sindre Mehus
 */
public class UserDaoTestCase extends DaoTestCaseBean2 {

    @Autowired
    UserDao userDao;

    @Before
    public void setUp() {
        getJdbcTemplate().execute("delete from user_role");
        getJdbcTemplate().execute("delete from user_credentials");
        getJdbcTemplate().execute("delete from user");
    }

    @Test
    public void testCreateUser() {
        User user = new User("sindre", "sindre@activeobjects.no", false, 1000L, 2000L, 3000L);
        user.setAdminRole(true);
        user.setCommentRole(true);
        user.setCoverArtRole(true);
        user.setDownloadRole(false);
        user.setPlaylistRole(true);
        user.setUploadRole(false);
        user.setPodcastRole(true);
        user.setStreamRole(true);
        user.setJukeboxRole(true);
        user.setSettingsRole(true);
        UserCredential uc = new UserCredential("sindre", "sindre", "secret", "noop", App.AIRSONIC);
        userDao.createUser(user, uc);

        User newUser = userDao.getAllUsers().get(0);
        assertThat(newUser).isEqualToComparingFieldByField(user);
        assertThat(userDao.getCredentials("sindre", App.AIRSONIC).get(0)).isEqualToComparingFieldByField(uc);
    }

    @Test
    public void testCreateUserTransactionalError() {
        User user = new User("muff1nman", "noemail") {
            @Override
            public boolean isPlaylistRole() {
                throw new RuntimeException();
            }
        };

        user.setAdminRole(true);

        UserCredential uc = new UserCredential("muff1nman", "muff1nman", "secret", "noop", App.AIRSONIC);
        int beforeSize = userDao.getAllUsers().size();

        assertThatExceptionOfType(RuntimeException.class).isThrownBy(() -> userDao.createUser(user, uc));
        assertEquals(beforeSize, userDao.getAllUsers().size());

        User user2 = new User("muff1nman", "noemail");
        UserCredential uc2 = new UserCredential("muff1nman", "muff1nman", "secret", "noop", App.AIRSONIC) {
            @Override
            public String getCredential() {
                throw new RuntimeException();
            }
        };

        assertThatExceptionOfType(RuntimeException.class).isThrownBy(() -> userDao.createUser(user2, uc2));
        assertEquals(beforeSize, userDao.getAllUsers().size());
    }

    @Test
    public void testUpdateUser() {
        User user = new User("sindre", null);
        user.setAdminRole(true);
        user.setCommentRole(true);
        user.setCoverArtRole(true);
        user.setDownloadRole(false);
        user.setPlaylistRole(true);
        user.setUploadRole(false);
        user.setPodcastRole(true);
        user.setStreamRole(true);
        user.setJukeboxRole(true);
        user.setSettingsRole(true);
        UserCredential uc = new UserCredential("sindre", "sindre", "secret", "noop", App.AIRSONIC);
        userDao.createUser(user, uc);

        user.setEmail("sindre@foo.bar");
        user.setLdapAuthenticated(true);
        user.setBytesStreamed(1);
        user.setBytesDownloaded(2);
        user.setBytesUploaded(3);
        user.setAdminRole(false);
        user.setCommentRole(false);
        user.setCoverArtRole(false);
        user.setDownloadRole(true);
        user.setPlaylistRole(false);
        user.setUploadRole(true);
        user.setPodcastRole(false);
        user.setStreamRole(false);
        user.setJukeboxRole(false);
        user.setSettingsRole(false);
        userDao.updateUser(user);

        assertThat(userDao.getAllUsers().get(0)).isEqualToComparingFieldByField(user);
        assertThat(userDao.getCredentials("sindre", App.AIRSONIC).get(0)).isEqualToComparingFieldByField(uc);
    }

    @Test
    public void testUpdateCredential() {
        User user = new User("sindre", null);
        user.setAdminRole(true);
        user.setCommentRole(true);
        user.setCoverArtRole(true);
        user.setDownloadRole(false);
        UserCredential uc = new UserCredential("sindre", "sindre", "secret", "noop", App.AIRSONIC);
        userDao.createUser(user, uc);

        UserCredential newCreds = new UserCredential(uc);
        newCreds.setCredential("foo");

        userDao.updateCredential(uc, newCreds);

        assertThat(userDao.getAllUsers().get(0)).isEqualToComparingFieldByField(user);
        assertThat(userDao.getCredentials("sindre", App.AIRSONIC).get(0)).isEqualToComparingFieldByField(newCreds);
    }

    @Test
    public void testGetUserByName() {
        User user = new User("sindre", null);
        userDao.createUser(user, new UserCredential("sindre", "sindre", "secret", "noop", App.AIRSONIC));

        assertThat(userDao.getUserByName("sindre", true)).isEqualToComparingFieldByField(user);

        assertNull("Error in getUserByName().", userDao.getUserByName("sindre2", true));
        assertNull("Error in getUserByName().", userDao.getUserByName("sindre ", true));
        assertNull("Error in getUserByName().", userDao.getUserByName("bente", true));
        assertNull("Error in getUserByName().", userDao.getUserByName("", true));
        assertNull("Error in getUserByName().", userDao.getUserByName(null, true));
    }

    @Test
    public void testDeleteUser() {
        assertEquals("Wrong number of users.", 0, userDao.getAllUsers().size());

        userDao.createUser(new User("sindre", null),
                new UserCredential("sindre", "sindre", "secret", "noop", App.AIRSONIC));
        assertEquals("Wrong number of users.", 1, userDao.getAllUsers().size());

        userDao.createUser(new User("bente", null), new UserCredential("bente", "bente", "secret", "noop", App.AIRSONIC));
        assertEquals("Wrong number of users.", 2, userDao.getAllUsers().size());

        userDao.deleteUser("sindre");
        assertEquals("Wrong number of users.", 1, userDao.getAllUsers().size());

        userDao.deleteUser("bente");
        assertEquals("Wrong number of users.", 0, userDao.getAllUsers().size());
    }

    @Test
    public void testGetRolesForUser() {
        User user = new User("sindre", null);
        user.setAdminRole(true);
        user.setCommentRole(true);
        user.setPodcastRole(true);
        user.setStreamRole(true);
        user.setSettingsRole(true);
        userDao.createUser(user, new UserCredential("sindre", "sindre", "secret", "noop", App.AIRSONIC));

        List<String> roles = userDao.getRolesForUser("sindre");
        assertThat(roles).containsExactly("admin", "comment", "podcast", "stream", "settings");
    }

    @Test
    public void testUserSettings() {
        assertNull("Error in getUserSettings.", userDao.getUserSettings("sindre"));

        assertThatExceptionOfType(DataIntegrityViolationException.class)
                .isThrownBy(() -> userDao.updateUserSettings(new UserSettings("sindre")));

        userDao.createUser(new User("sindre", null),
                new UserCredential("sindre", "sindre", "secret", "noop", App.AIRSONIC));
        assertNull("Error in getUserSettings.", userDao.getUserSettings("sindre"));

        UserSettings settings = new UserSettings("sindre");
        userDao.updateUserSettings(settings);
        UserSettings userSettings = userDao.getUserSettings("sindre");
        assertThat(userSettings).usingRecursiveComparison().isEqualTo(settings);

        settings = new UserSettings("sindre");
        settings.setLocale(Locale.SIMPLIFIED_CHINESE);
        settings.setThemeId("midnight");
        settings.setBetaVersionNotificationEnabled(true);
        settings.setSongNotificationEnabled(false);
        settings.setShowSideBar(true);
        settings.getMainVisibility().setBitRateVisible(true);
        settings.getPlaylistVisibility().setYearVisible(true);
        settings.setLastFmEnabled(true);
        settings.setListenBrainzEnabled(true);
        settings.setTranscodeScheme(TranscodeScheme.MAX_192);
        settings.setShowNowPlayingEnabled(false);
        settings.setSelectedMusicFolderId(3);
        settings.setPartyModeEnabled(true);
        settings.setNowPlayingAllowed(true);
        settings.setAvatarScheme(AvatarScheme.SYSTEM);
        settings.setSystemAvatarId(1);
        settings.setChanged(Instant.ofEpochMilli(9412L));
        settings.setKeyboardShortcutsEnabled(true);
        settings.setPaginationSize(120);

        userDao.updateUserSettings(settings);
        userSettings = userDao.getUserSettings("sindre");
        assertThat(userSettings).usingRecursiveComparison().isEqualTo(settings);

        userDao.deleteUser("sindre");
        assertNull("Error in cascading delete.", userDao.getUserSettings("sindre"));
    }
}
