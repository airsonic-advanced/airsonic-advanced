package org.airsonic.player.spring;

import org.airsonic.player.service.ConfigurationPropertiesService;
import org.airsonic.player.service.SettingsService;
import org.apache.commons.configuration2.spring.ConfigurationPropertySource;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.context.logging.LoggingApplicationListener;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.core.env.MapPropertySource;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CustomPropertySourceConfigurer implements
        ApplicationListener<ApplicationEnvironmentPreparedEvent>, Ordered {

    private static final Map<String, Object> DEFAULT_CONSTANTS = new ConcurrentHashMap<>();
    private static final Map<String, Object> MIGRATED = new ConcurrentHashMap<>();

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        // vars are searched in order: spring > jvm > env > app.prop > airsonic.prop > default consts
        // spring: java -jar pkg.jar --var=foo
        // jvm: java -jar -Dvar=foo pkg.jar
        // env: SET var=foo; java -jar pkg.jar

        Map<String, String> migratedProps = SettingsService.getMigratedPropertyKeys();

        // Migrate each property source key to its latest name
        // PropertySource precedence matters. Higher migrates first, lower will skip migration if key has already migrated
        // At lookup time, migrated properties have lower precedence (so more specific properties can override migrated properties)
        // Example for same start: env (higher precedence) and file (lower) need to migrate A -> B and both have A
        //  - env migrates A -> B, file skips chain (migration keys already present)
        //  - Lookup(A) will find env[A] (higher precedence)
        //  - Lookup(B) will find migrated[env[A]] since B does not exist in env and file, and migrated has env[A] (skipped file)
        // Example 1 for in-the-middle chain migration: env has C and file has A in migration A -> B -> C -> D
        //  - env migrates C -> D, file migrates A -> B (skips rest of the chain)
        //  - Lookup(A) finds file[A]
        //  - Lookup(B) finds migrated[file[A]]
        //  - Lookup(C) finds env[C]
        //  - Lookup(D) finds migrated[env[C]]
        // Example 2 for in-the-middle chain migration: env has A and file has C in migration A -> B -> C -> D
        //  - env migrates A -> B -> C -> D, file skips chain (migration keys already present)
        //  - Lookup(A) finds env[A]
        //  - Lookup(B) finds migrated[env[A]]
        //  - Lookup(C) finds file[C] (higher precedence than migrated[env[C]])
        //  - Lookup(D) finds migrated[env[A]]
        event.getEnvironment().getPropertySources().forEach(ps -> SettingsService.migratePropertySourceKeys(migratedProps, ps, MIGRATED));
        event.getEnvironment().getPropertySources().addLast(new MapPropertySource("migrated-properties", MIGRATED));

        // Migrate external property file
        SettingsService.migratePropFileKeys(migratedProps, ConfigurationPropertiesService.getInstance());
        event.getEnvironment().getPropertySources().addLast(new ConfigurationPropertySource("airsonic-properties", ConfigurationPropertiesService.getInstance().getConfiguration()));

        // Set default constants - only set if vars are blank so their PS need to be set first (or blank vars will get picked up first on look up)
        SettingsService.setDefaultConstants(event.getEnvironment(), DEFAULT_CONSTANTS);
        event.getEnvironment().getPropertySources().addFirst(new MapPropertySource("default-constants", DEFAULT_CONSTANTS));
    }

    @Override
    public int getOrder() {
        return LoggingApplicationListener.DEFAULT_ORDER - 1;
    }
}
