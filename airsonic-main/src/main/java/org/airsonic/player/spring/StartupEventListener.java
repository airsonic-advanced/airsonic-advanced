package org.airsonic.player.spring;

import org.airsonic.player.util.LegacyHsqlMigrationUtil;
import org.springframework.boot.context.event.ApplicationContextInitializedEvent;
import org.springframework.context.ApplicationListener;

/*
 * Registered via spring.factories because the event happens too early to be registered via <code>EventListener</code>
 * (the beans etc aven't been instantiated nor any component scanning done), and cannot register via <pre>Application.doConfigure(...)</pre>
 * because tests won't pick it up
 */
public class StartupEventListener implements ApplicationListener<ApplicationContextInitializedEvent> {
    @Override
    public void onApplicationEvent(ApplicationContextInitializedEvent event) {
        // Handle HSQLDB database upgrades for builtin legacy db from 1.8 to 2.x before
        // any beans are started.
        LegacyHsqlMigrationUtil.upgradeFileHsqlDbIfNeeded(event.getApplicationContext().getEnvironment());
    }
}
