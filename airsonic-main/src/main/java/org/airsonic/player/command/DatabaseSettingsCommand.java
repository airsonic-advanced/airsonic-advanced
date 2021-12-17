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
    private String importFolder;
    private String callback;
    private boolean backuppable;
    private int dbBackupInterval;
    private int dbBackupRetentionCount;

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

    public String getImportFolder() {
        return importFolder;
    }

    public void setImportFolder(String importFolder) {
        this.importFolder = importFolder;
    }

    public String getCallback() {
        return callback;
    }

    public void setCallback(String callback) {
        this.callback = callback;
    }

    public boolean getBackuppable() {
        return backuppable;
    }

    public void setBackuppable(boolean backuppable) {
        this.backuppable = backuppable;
    }

    public int getDbBackupInterval() {
        return dbBackupInterval;
    }

    public void setDbBackupInterval(int dbBackupInterval) {
        this.dbBackupInterval = dbBackupInterval;
    }

    public int getDbBackupRetentionCount() {
        return dbBackupRetentionCount;
    }

    public void setDbBackupRetentionCount(int dbBackupRetentionCount) {
        this.dbBackupRetentionCount = dbBackupRetentionCount;
    }

    public enum DataSourceConfigType {
        JNDI, EXTERNAL, BUILTIN
    }
}
