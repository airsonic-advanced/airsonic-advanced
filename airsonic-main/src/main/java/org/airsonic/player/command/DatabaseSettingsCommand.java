package org.airsonic.player.command;

import javax.validation.constraints.NotNull;

public class DatabaseSettingsCommand {

    @NotNull
    private DataSourceConfigType configType;
    private String driver;
    private String password;
    private String url;
    private String username;
    private String JNDIName;
    private int mysqlVarcharMaxlength;
    private String usertableQuote;

    public DataSourceConfigType getConfigType() {
        return configType;
    }

    public void setConfigType(DataSourceConfigType configType) {
        this.configType = configType;
    }

    public String getDriver() {
        return driver;
    }

    public void setDriver(String embedDriver) {
        this.driver = embedDriver;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String embedPassword) {
        this.password = embedPassword;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String embedUrl) {
        this.url = embedUrl;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String embedUsername) {
        this.username = embedUsername;
    }

    public String getJNDIName() {
        return JNDIName;
    }

    public void setJNDIName(String JNDIName) {
        this.JNDIName = JNDIName;
    }

    public int getMysqlVarcharMaxlength() {
        return mysqlVarcharMaxlength;
    }

    public void setMysqlVarcharMaxlength(int mysqlVarcharMaxlength) {
        this.mysqlVarcharMaxlength = mysqlVarcharMaxlength;
    }

    public String getUsertableQuote() {
        return usertableQuote;
    }

    public void setUsertableQuote(String usertableQuote) {
        this.usertableQuote = usertableQuote;
    }

    public enum DataSourceConfigType {
        JNDI, EXTERNAL, BUILTIN
    }
}
