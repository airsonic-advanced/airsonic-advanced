package org.airsonic.player.spring;

import liquibase.database.DatabaseFactory;
import liquibase.integration.spring.SpringLiquibase;
import org.airsonic.player.service.SettingsService;
import org.airsonic.player.util.Util;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.datasource.lookup.JndiDataSourceLookup;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableTransactionManagement
public class DatabaseConfiguration {
    @Value("${DatabaseConfigEmbedUrl:#{T(org.airsonic.player.service.SettingsService).getDefaultJDBCUrl()}}")
    private String url;
    @Value("${DatabaseConfigEmbedUsername:#{T(org.airsonic.player.service.SettingsService).getDefaultJDBCUsername()}}")
    private String user;
    @Value("${DatabaseConfigEmbedPassword:#{T(org.airsonic.player.service.SettingsService).getDefaultJDBCPassword()}}")
    private String password;
    @Value("${DatabaseConfigEmbedDriver:org.hsqldb.jdbc.JDBCDriver}")
    private String driver;

    @Bean
    @Profile("!jndi")
    public DataSource dataSource() {
        return DataSourceBuilder.create()
                //find connection pool automatically
                .username(user)
                .password(password)
                .driverClassName(driver)
                .url(url)
                .build();
    }

    @Bean
    @Profile("jndi")
    public DataSource jndiDataSource(@Value("${DatabaseConfigJNDIName}") String jndiName) {
        JndiDataSourceLookup jndiLookup = new JndiDataSourceLookup();
        return jndiLookup.getDataSource(jndiName);
    }

    @Bean
    public SpringLiquibase liquibase(DataSource dataSource,
                                     @Value("${DatabaseMysqlMaxlength:512}")
                                     String mysqlVarcharLimit,
                                     @Value("${DatabaseUsertableQuote:}")
                                     String userTableQuote) {
        // add support for our hqldb that doesn't support schemas
        DatabaseFactory.getInstance().register(new HsqlDatabase());
        
        SpringLiquibase springLiquibase = new SpringLiquibase();
        springLiquibase.setDataSource(dataSource);
        springLiquibase.setChangeLog("classpath:liquibase/db-changelog.xml");
        springLiquibase.setRollbackFile(SettingsService.getAirsonicHome().resolve("rollback.sql").toFile());
        Map<String, String> parameters = new HashMap<>();
        parameters.put("defaultMusicFolder", Util.getDefaultMusicFolder());
        parameters.put("mysqlVarcharLimit", mysqlVarcharLimit);
        parameters.put("userTableQuote", userTableQuote);
        springLiquibase.setChangeLogParameters(parameters);
        return springLiquibase;
    }

}
