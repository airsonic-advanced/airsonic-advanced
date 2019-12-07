package org.airsonic.player.spring;

import com.google.common.collect.Lists;
import org.airsonic.player.service.ConfigurationPropertiesService;
import org.apache.commons.configuration2.spring.ConfigurationPropertySource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.web.context.ConfigurableWebApplicationContext;

import java.util.List;

public class CustomPropertySourceConfigurer implements
        ApplicationContextInitializer<ConfigurableWebApplicationContext> {

    public static final String DATASOURCE_CONFIG_TYPE = "DatabaseConfigType";

    public void initialize(ConfigurableWebApplicationContext ctx) {
        ctx.getEnvironment().getPropertySources().addLast(new ConfigurationPropertySource("airsonic-pre-init-configs", ConfigurationPropertiesService.getInstance().getConfiguration()));

        addDataSourceProfile(ctx);
    }

    private void addDataSourceProfile(ConfigurableWebApplicationContext ctx) {
        DataSourceConfigType dataSourceConfigType;
        String rawType = ctx.getEnvironment().getProperty(DATASOURCE_CONFIG_TYPE);
        if (StringUtils.isNotBlank(rawType)) {
            dataSourceConfigType = DataSourceConfigType.valueOf(StringUtils.upperCase(rawType));
        } else {
            dataSourceConfigType = DataSourceConfigType.LEGACY;
        }
        String dataSourceTypeProfile = StringUtils.lowerCase(dataSourceConfigType.name());
        List<String> existingProfiles = Lists.newArrayList(ctx.getEnvironment().getActiveProfiles());
        existingProfiles.add(dataSourceTypeProfile);
        ctx.getEnvironment().setActiveProfiles(existingProfiles.toArray(new String[0]));
    }
}
