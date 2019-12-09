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

    public void setDriver(String driver) {
        this.driver = driver;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
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
