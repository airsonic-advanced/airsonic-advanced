package org.airsonic.player.spring;

import liquibase.integration.spring.SpringLiquibase;
import org.airsonic.player.util.Util;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseDataSource;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({ DataSourceProperties.class, LiquibaseProperties.class })
public class DebugConfig {
    private final LiquibaseProperties properties;

    public DebugConfig(LiquibaseProperties properties) {
        super();
        this.properties = properties;
    }

    @Bean
    public SpringLiquibase liquibase(DataSourceProperties dataSourceProperties, ObjectProvider<DataSource> dataSource,
            @LiquibaseDataSource ObjectProvider<DataSource> liquibaseDataSource) {
        System.out.println("LIQUIBASEPROPS");
        System.out.println(Util.debugObject(properties));
        System.out.println("DATAPROPS");
        System.out.println("url:" + Util.debugObject(dataSourceProperties.getUrl()));
        System.out.println("user:" + Util.debugObject(dataSourceProperties.getUsername()));
        System.out.println("data:" + Util.debugObject(dataSourceProperties.getData()));
        System.out.println("durl:" + Util.debugObject(dataSourceProperties.determineUrl()));
        System.out.println("name:" + Util.debugObject(dataSourceProperties.getName()));

        return new LiquibaseAutoConfiguration.LiquibaseConfiguration(properties).liquibase(dataSourceProperties,
                dataSource, liquibaseDataSource);
    }
}
