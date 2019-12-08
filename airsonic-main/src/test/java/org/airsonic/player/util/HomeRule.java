package org.airsonic.player.util;

import org.airsonic.player.TestCaseUtils;
import org.airsonic.player.service.ConfigurationPropertiesService;
import org.junit.rules.ExternalResource;

import java.nio.file.Paths;

/**
 *
 * This must be a ClassRule for Spring tests, because it needs to be executed
 * prior to the context initiation. Context picks up home from property set by
 * this class. If home is set after context init, the context won't see it, and
 * point to the wrong home.
 *
 */
public class HomeRule extends ExternalResource {
    @Override
    protected void before() throws Throwable {
        super.before();
        System.setProperty("airsonic.home", TestCaseUtils.airsonicHomePathForTest());
        TestCaseUtils.cleanAirsonicHomeForTest();
        ConfigurationPropertiesService.reset();
    }

    @Override
    protected void after() {
        FileUtil.delete(Paths.get(TestCaseUtils.airsonicHomePathForTest()));
    }
}
