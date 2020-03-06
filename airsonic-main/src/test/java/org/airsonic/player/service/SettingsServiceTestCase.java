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
package org.airsonic.player.service;

import org.airsonic.player.TestCaseUtils;
import org.airsonic.player.util.HomeRule;
import org.apache.commons.configuration2.spring.ConfigurationPropertySource;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit test of {@link SettingsService}.
 *
 * @author Sindre Mehus
 */
@RunWith(SpringRunner.class)
public class SettingsServiceTestCase {

    @ClassRule
    public static HomeRule home = new HomeRule();

    private SettingsService settingsService;

    @Autowired
    StandardEnvironment env;

    @Before
    public void setUp() throws IOException {
        TestCaseUtils.cleanAirsonicHomeForTest();
        ConfigurationPropertiesService.reset();

        settingsService = newSettingsService();
    }

    private SettingsService newSettingsService() {
        SettingsService settingsService = new SettingsService();
        env.getPropertySources().addFirst(new ConfigurationPropertySource("airsonic-pre-init-configs", ConfigurationPropertiesService.getInstance().getConfiguration()));
        settingsService.setEnvironment(env);
        return settingsService;
    }

    @Test
    public void testAirsonicHome() {
        assertEquals("Wrong Airsonic home.", TestCaseUtils.airsonicHomePathForTest(), SettingsService.getAirsonicHome().toString());
    }

    @Test
    public void testDefaultValues() {
        assertEquals("Wrong default language.", "en", settingsService.getLocale().getLanguage());
        assertEquals("Wrong default index creation interval.", 1, settingsService.getIndexCreationInterval());
        assertEquals("Wrong default index creation hour.", 3, settingsService.getIndexCreationHour());
        assertTrue("Wrong default playlist folder.", settingsService.getPlaylistFolder().endsWith("playlists"));
        assertEquals("Wrong default theme.", "default", settingsService.getThemeId());
        assertEquals("Wrong default Podcast episode retention count.", 10, settingsService.getPodcastEpisodeRetentionCount());
        assertEquals("Wrong default Podcast episode download count.", 1, settingsService.getPodcastEpisodeDownloadCount());
        assertTrue("Wrong default Podcast folder.", settingsService.getPodcastFolder().endsWith("podcast"));
        assertEquals("Wrong default Podcast update interval.", 24, settingsService.getPodcastUpdateInterval());
        assertEquals("Wrong default LDAP enabled.", false, settingsService.isLdapEnabled());
        assertEquals("Wrong default LDAP URL.", "ldap://host.domain.com:389/cn=Users,dc=domain,dc=com", settingsService.getLdapUrl());
        assertNull("Wrong default LDAP manager DN.", settingsService.getLdapManagerDn());
        assertNull("Wrong default LDAP manager password.", settingsService.getLdapManagerPassword());
        assertEquals("Wrong default LDAP search filter.", "(sAMAccountName={0})", settingsService.getLdapSearchFilter());
        assertEquals("Wrong default LDAP auto-shadowing.", false, settingsService.isLdapAutoShadowing());
    }

    @Test
    public void testChangeSettings() {
        settingsService.setIndexString("indexString");
        settingsService.setIgnoredArticles("a the foo bar");
        settingsService.setShortcuts("new incoming \"rock 'n' roll\"");
        settingsService.setPlaylistFolder("playlistFolder");
        settingsService.setMusicFileTypes("mp3 ogg  aac");
        settingsService.setCoverArtFileTypes("jpeg gif  png");
        settingsService.setWelcomeMessage("welcomeMessage");
        settingsService.setLoginMessage("loginMessage");
        settingsService.setLocale(Locale.CANADA_FRENCH);
        settingsService.setThemeId("dark");
        settingsService.setIndexCreationInterval(4);
        settingsService.setIndexCreationHour(9);
        settingsService.setPodcastEpisodeRetentionCount(5);
        settingsService.setPodcastEpisodeDownloadCount(-1);
        settingsService.setPodcastFolder("d:/podcasts");
        settingsService.setPodcastUpdateInterval(-1);
        settingsService.setLdapEnabled(true);
        settingsService.setLdapUrl("newLdapUrl");
        settingsService.setLdapManagerDn("admin");
        settingsService.setLdapManagerPassword("secret");
        settingsService.setLdapSearchFilter("newLdapSearchFilter");
        settingsService.setLdapAutoShadowing(true);

        verifySettings(settingsService);

        settingsService.save();
        verifySettings(settingsService);

        verifySettings(newSettingsService());
    }

    private void verifySettings(SettingsService ss) {
        assertEquals("Wrong index string.", "indexString", ss.getIndexString());
        assertEquals("Wrong ignored articles.", "a the foo bar", ss.getIgnoredArticles());
        assertEquals("Wrong shortcuts.", "new incoming \"rock 'n' roll\"", ss.getShortcuts());
        assertTrue("Wrong ignored articles array.", Arrays.equals(new String[] {"a", "the", "foo", "bar"}, ss.getIgnoredArticlesAsArray()));
        assertTrue("Wrong shortcut array.", Arrays.equals(new String[] {"new", "incoming", "rock 'n' roll"}, ss.getShortcutsAsArray()));
        assertEquals("Wrong playlist folder.", "playlistFolder", ss.getPlaylistFolder());
        assertEquals("Wrong music mask.", "mp3 ogg  aac", ss.getMusicFileTypes());
        assertThat(ss.getMusicFileTypesSet()).containsOnly("mp3", "ogg", "aac");
        assertEquals("Wrong cover art mask.", "jpeg gif  png", ss.getCoverArtFileTypes());
        assertThat(ss.getCoverArtFileTypesSet()).containsOnly("jpeg", "gif", "png");
        assertEquals("Wrong welcome message.", "welcomeMessage", ss.getWelcomeMessage());
        assertEquals("Wrong login message.", "loginMessage", ss.getLoginMessage());
        assertEquals("Wrong locale.", Locale.CANADA_FRENCH, ss.getLocale());
        assertEquals("Wrong theme.", "dark", ss.getThemeId());
        assertEquals("Wrong index creation interval.", 4, ss.getIndexCreationInterval());
        assertEquals("Wrong index creation hour.", 9, ss.getIndexCreationHour());
        assertEquals("Wrong Podcast episode retention count.", 5, settingsService.getPodcastEpisodeRetentionCount());
        assertEquals("Wrong Podcast episode download count.", -1, settingsService.getPodcastEpisodeDownloadCount());
        assertEquals("Wrong Podcast folder.", "d:/podcasts", settingsService.getPodcastFolder());
        assertEquals("Wrong Podcast update interval.", -1, settingsService.getPodcastUpdateInterval());
        assertTrue("Wrong LDAP enabled.", settingsService.isLdapEnabled());
        assertEquals("Wrong LDAP URL.", "newLdapUrl", settingsService.getLdapUrl());
        assertEquals("Wrong LDAP manager DN.", "admin", settingsService.getLdapManagerDn());
        assertEquals("Wrong LDAP manager password.", "secret", settingsService.getLdapManagerPassword());
        assertEquals("Wrong LDAP search filter.", "newLdapSearchFilter", settingsService.getLdapSearchFilter());
        assertTrue("Wrong LDAP auto-shadowing.", settingsService.isLdapAutoShadowing());
    }

    @Test
    public void migrateKeys_noKeys() {
        Map<String, String> keyMaps = new LinkedHashMap<>();
        keyMaps.put("bla", "bla2");
        keyMaps.put("bla2", "bla3");
        SettingsService.migrateKeys(keyMaps);

        assertNull(env.getProperty("bla"));
        assertNull(env.getProperty("bla2"));
        assertNull(env.getProperty("bla3"));
    }

    @Test
    public void migrateKeys_deleteKeys_BackwardsCompatibilitySet() {
        ConfigurationPropertiesService.getInstance().setProperty("bla", "hello");

        Map<String, String> keyMaps = new LinkedHashMap<>();
        keyMaps.put("bla", "bla2");
        keyMaps.put("bla2", "bla3");
        keyMaps.put("bla3", null);
        SettingsService.migrateKeys(keyMaps);

        assertEquals("hello", env.getProperty("bla"));
        assertEquals("hello", env.getProperty("bla2"));
        assertNull(env.getProperty("bla3"));
    }

    @Test
    public void migrateKeys_deleteKeys_NonBackwardsCompatible() {
        ConfigurationPropertiesService.getInstance().setProperty(SettingsService.KEY_PROPERTIES_FILE_UPGRADE_RETAIN_COMPATIBILITY, "false");
        ConfigurationPropertiesService.getInstance().setProperty("bla", "hello");

        Map<String, String> keyMaps = new LinkedHashMap<>();
        keyMaps.put("bla", "bla2");
        keyMaps.put("bla2", "bla3");
        keyMaps.put("bla3", null);
        SettingsService.migrateKeys(keyMaps);

        assertNull(env.getProperty("bla"));
        assertNull(env.getProperty("bla2"));
        assertNull(env.getProperty("bla3"));
    }

    @Test
    public void migrateKeys_withKeys_ExplicitlyBackwardsCompatible() {
        ConfigurationPropertiesService.getInstance().setProperty(SettingsService.KEY_PROPERTIES_FILE_UPGRADE_RETAIN_COMPATIBILITY, "true");
        ConfigurationPropertiesService.getInstance().setProperty("bla", "hello");
        ConfigurationPropertiesService.getInstance().setProperty("bla3", "hello2");

        Map<String, String> keyMaps = new LinkedHashMap<>();
        keyMaps.put("bla", "bla2");
        keyMaps.put("bla2", "bla3");
        keyMaps.put("bla3", "bla4");
        keyMaps.put("bla4", "bla5");
        SettingsService.migrateKeys(keyMaps);

        assertEquals("hello", env.getProperty("bla"));
        assertEquals("hello", env.getProperty("bla2"));
        assertEquals("hello2", env.getProperty("bla3"));
        assertEquals("hello2", env.getProperty("bla4"));
        assertEquals("hello2", env.getProperty("bla5"));
    }

    @Test
    public void migrateKeys_withKeys_ImplicitlyBackwardsCompatible() {
        ConfigurationPropertiesService.getInstance().setProperty("bla", "hello");
        ConfigurationPropertiesService.getInstance().setProperty("bla3", "hello2");

        Map<String, String> keyMaps = new LinkedHashMap<>();
        keyMaps.put("bla", "bla2");
        keyMaps.put("bla2", "bla3");
        keyMaps.put("bla3", "bla4");
        keyMaps.put("bla4", "bla5");
        SettingsService.migrateKeys(keyMaps);

        assertEquals("hello", env.getProperty("bla"));
        assertEquals("hello", env.getProperty("bla2"));
        assertEquals("hello2", env.getProperty("bla3"));
        assertEquals("hello2", env.getProperty("bla4"));
        assertEquals("hello2", env.getProperty("bla5"));
    }

    @Test
    public void migrateKeys_withKeys_NonBackwardsCompatible() {
        ConfigurationPropertiesService.getInstance().setProperty(SettingsService.KEY_PROPERTIES_FILE_UPGRADE_RETAIN_COMPATIBILITY, "false");
        ConfigurationPropertiesService.getInstance().setProperty("bla", "hello");
        ConfigurationPropertiesService.getInstance().setProperty("bla3", "hello2");

        Map<String, String> keyMaps = new LinkedHashMap<>();
        keyMaps.put("bla", "bla2");
        keyMaps.put("bla2", "bla3");
        keyMaps.put("bla3", "bla4");
        keyMaps.put("bla4", "bla5");
        SettingsService.migrateKeys(keyMaps);

        assertNull(env.getProperty("bla"));
        assertNull(env.getProperty("bla2"));
        assertNull(env.getProperty("bla3"));
        assertNull(env.getProperty("bla4"));
        assertEquals("hello2", env.getProperty("bla5"));
    }
}
