package org.airsonic.player.spring;

import org.airsonic.player.service.ConfigurationPropertiesService;
import org.apache.commons.configuration2.spring.ConfigurationPropertySource;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.core.env.MapPropertySource;
import org.springframework.web.context.ConfigurableWebApplicationContext;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CustomPropertySourceConfigurer implements
        ApplicationContextInitializer<ConfigurableWebApplicationContext> {

    private static final Map<String, Object> DEFAULT_CONSTANTS = new ConcurrentHashMap<>();

    @Override
    public void initialize(ConfigurableWebApplicationContext ctx) {
        ctx.getEnvironment().getPropertySources().addLast(new ConfigurationPropertySource("airsonic-properties", ConfigurationPropertiesService.getInstance().getConfiguration()));
        ctx.getEnvironment().getPropertySources().addLast(new MapPropertySource("default-constants", DEFAULT_CONSTANTS));
    }

    public static Map<String, Object> getDefaultConstants() {
        return DEFAULT_CONSTANTS;
    }
}
