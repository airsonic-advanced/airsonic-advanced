package org.airsonic.player.service;

import org.airsonic.player.TestCaseUtils;
import org.airsonic.player.util.EmbeddedTestCategory;
import org.airsonic.player.util.FileUtils;
import org.airsonic.player.util.HomeRule;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.sql.DataSource;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

@Category(EmbeddedTestCategory.class)
@RunWith(SpringRunner.class)
@SpringBootTest
public class LegacyDatabaseStartupTestCase {

    @ClassRule
    public static final HomeRule airsonicRule = new HomeRule();

    @BeforeClass
    public static void setupOnce() throws IOException, URISyntaxException {
        String homeParent = TestCaseUtils.airsonicHomePathForTest();
        Path dbDirectory = Paths.get(homeParent, "db");
        FileUtils.copyRecursively(LegacyDatabaseStartupTestCase.class.getResource("/db/pre-liquibase/db"), dbDirectory);
        // have to change the url here because old db files are libresonic
        System.setProperty(SettingsService.KEY_DATABASE_URL,
                SettingsService.getDefaultJDBCUrl().replaceAll("airsonic;", "libresonic;"));
        System.setProperty(SettingsService.KEY_DATABASE_USERNAME, "sa");
        System.setProperty(SettingsService.KEY_DATABASE_PASSWORD, "");
    }

    @Autowired
    DataSource dataSource;

    @Test
    public void testStartup() {
        assertThat(dataSource).isNotNull();
    }
}
