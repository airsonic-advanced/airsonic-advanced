package org.airsonic.player.dao;

import org.airsonic.player.domain.AvatarScheme;
import org.airsonic.player.domain.TranscodeScheme;
import org.airsonic.player.domain.User;
import org.airsonic.player.domain.User.Role;
import org.airsonic.player.domain.UserCredential;
import org.airsonic.player.domain.UserCredential.App;
import org.airsonic.player.domain.UserSettings;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.comparator.NullSafeComparator;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.Locale;
import java.util.Set;

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
        getJdbcTemplate().execute("delete from user_credentials");
        getJdbcTemplate().execute("delete from users");
    }

    @Test
    public void testCreateUser() {
        User user = new User("sindre", "sindre@activeobjects.no", false, 1000L, 2000L, 3000L, Set.of(Role.ADMIN, Role.COMMENT, Role.COVERART, Role.PLAYLIST, Role.PODCAST, Role.STREAM, Role.JUKEBOX, Role.SETTINGS));
        UserCredential uc = new UserCredential("sindre", "sindre", "secret", "noop", App.AIRSONIC);
        userDao.createUser(user, uc);

        User newUser = userDao.getAllUsers().get(0);
        assertThat(newUser).isEqualToComparingFieldByField(user);
        assertThat(userDao.getCredentials("sindre", App.AIRSONIC).get(0)).isEqualToComparingFieldByField(uc);
    }

    @Test
    public void testCreateUserTransactionalError() {
        User user = new User("muff1nman5", "noemail") {
            @Override
            public Set<Role> getRoles() {
                throw new RuntimeException();
            }
        };

        user.setRoles(Set.of(Role.ADMIN));
        UserCredential uc = new UserCredential("muff1nman5", "muff1nman5", "secret", "noop", App.AIRSONIC);

        assertThatExceptionOfType(RuntimeException.class).isThrownBy(() -> createTestUser(user, uc));
        assertThat(userDao.getUserByName("muff1nman5", true)).isNull();

        User user2 = new User("muff1nman6", "noemail");
        UserCredential uc2 = new UserCredential("muff1nman6", "muff1nman6", "secret", "noop", App.AIRSONIC) {
            @Override
            public String getCredential() {
                throw new RuntimeException();
            }
        };

        assertThatExceptionOfType(RuntimeException.class).isThrownBy(() -> createTestUser(user2, uc2));
        assertThat(userDao.getUserByName("muff1nman6", true)).isNull();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private void createTestUser(User user, UserCredential uc) {
        userDao.createUser(user, uc);
    }

    @Test
    public void testUpdateUser() {
        User user = new User("sindre", null);
        user.setRoles(Set.of(Role.ADMIN, Role.COMMENT, Role.COVERART, Role.PLAYLIST, Role.PODCAST, Role.STREAM, Role.JUKEBOX, Role.SETTINGS));
        UserCredential uc = new UserCredential("sindre", "sindre", "secret", "noop", App.AIRSONIC);
        userDao.createUser(user, uc);

        user.setEmail("sindre@foo.bar");
        user.setLdapAuthenticated(true);
        user.setBytesStreamed(1);
        user.setBytesDownloaded(2);
        user.setBytesUploaded(3);
        user.setRoles(Set.of(Role.DOWNLOAD, Role.UPLOAD));

        userDao.updateUser(user);

        assertThat(userDao.getAllUsers().get(0)).isEqualToComparingFieldByField(user);
        assertThat(userDao.getCredentials("sindre", App.AIRSONIC).get(0)).isEqualToComparingFieldByField(uc);
    }

    @Test
    public void testUpdateCredential() {
        User user = new User("sindre", null);
        user.setRoles(Set.of(Role.ADMIN, Role.COMMENT, Role.COVERART));
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
        // assertNull("Error in getUserByName().", userDao.getUserByName("Sindre ", true)); // depends on the collation of the DB
        assertNull("Error in getUserByName().", userDao.getUserByName("bente", true));
        assertNull("Error in getUserByName().", userDao.getUserByName("", true));
        assertNull("Error in getUserByName().", userDao.getUserByName(null, true));
    }

    @Test
    public void testDeleteUser() {
        assertEquals("Wrong number of users.", 0, userDao.getAllUsers().size());

        userDao.createUser(new User("sindre", null), new UserCredential("sindre", "sindre", "secret", "noop", App.AIRSONIC));
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
        user.setRoles(Set.of(Role.ADMIN, Role.COMMENT, Role.PODCAST, Role.STREAM, Role.SETTINGS));
        userDao.createUser(user, new UserCredential("sindre", "sindre", "secret", "noop", App.AIRSONIC));

        Set<Role> roles = userDao.getUserByName("sindre", true).getRoles();
        assertThat(roles).containsOnly(Role.ADMIN, Role.COMMENT, Role.PODCAST, Role.STREAM, Role.SETTINGS);
    }

    @Test
    public void testUserSettings() {
        assertNull("Error in getUserSettings.", userDao.getUserSettings("sindre"));

        assertThatExceptionOfType(DataIntegrityViolationException.class)
                .isThrownBy(() -> updateUserSettings(new UserSettings("sindre")));

        userDao.createUser(new User("sindre", null), new UserCredential("sindre", "sindre", "secret", "noop", App.AIRSONIC));
        assertNull("Error in getUserSettings.", userDao.getUserSettings("sindre"));

        UserSettings settings = new UserSettings("sindre");
        userDao.updateUserSettings(settings);
        UserSettings userSettings = userDao.getUserSettings("sindre");
        assertThat(userSettings).usingComparatorForType(new NullSafeComparator<Instant>(new Comparator<Instant>() {
            // use a custom comparator to account for micro second differences
            // (Mysql only stores floats in json to a certain value)
            @Override
            public int compare(Instant o1, Instant o2) {
                if (o1.equals(o2) || Math.abs(ChronoUnit.MICROS.between(o1, o2)) <= 2) {
                    return 0;
                }
                return o1.compareTo(o2);
            }
        }, true), Instant.class)
                .usingRecursiveComparison().isEqualTo(settings);

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
        settings.setPaginationSizeFiles(120);
        settings.setPaginationSizeFolders(9);
        settings.setPaginationSizePlayqueue(121);
        settings.setPaginationSizePlaylist(122);

        userDao.updateUserSettings(settings);
        userSettings = userDao.getUserSettings("sindre");
        assertThat(userSettings).usingRecursiveComparison().isEqualTo(settings);

        userDao.deleteUser("sindre");
        assertNull("Error in cascading delete.", userDao.getUserSettings("sindre"));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private void updateUserSettings(UserSettings settings) {
        userDao.updateUserSettings(settings);
    }
}
