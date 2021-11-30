package org.airsonic.player.util;

import org.airsonic.player.service.SettingsService;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.core.env.Environment;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

public class LegacyHsqlMigrationUtil {

    private static final Logger LOG = LoggerFactory.getLogger(LegacyHsqlMigrationUtil.class);

    /**
     * Return the current version of the HSQLDB database, as reported by the database properties file.
     */
    public static String getHsqlDbVersion(String dbPath) {
        try {
            return PropertiesLoaderUtils
                    .loadProperties(new FileSystemResource(Paths.get(dbPath + ".properties")))
                    .getProperty("version");
        } catch (Exception e) {
            LOG.warn("Failed to determine HSQLDB database version", e);
            return null;
        }
    }

    /**
     * Create a new connection to the HSQLDB database.
     */
    public static Connection getHsqlDbConnection(String url, String user, String password) throws SQLException {
        Properties properties = new Properties();
        properties.put("user", user);
        properties.put("password", password);
        return DriverManager.getConnection(url, properties);
    }

    /**
     * Check if a HSQLDB database upgrade will occur and backups are needed.
     *
     * DB   Driver      Likely reason                                Decision
     * null -           new db or non-legacy                         false
     * -    null or !2  something went wrong, we better make copies  true
     * 1.x  2.x         this is the big upgrade                      true
     * 2.x  2.x         already up to date                           false
     *
     * all else true (default)
     *
     * @return true if a database backup/migration should be performed
     */
    public static boolean isHsqlDbBackupNeeded(String dbPath, String jdbcUrl) {
        // Check the current database version
        String currentVersion = getHsqlDbVersion(dbPath);
        if (currentVersion == null) {
            LOG.debug("HSQLDB database not found, won't back up");
            return false;
        }

        try {
            // as we're running before spring's datasource initialises... we need to try loading the driver ourselves
            EmbeddedDatabaseConnection conn = EmbeddedDatabaseConnection.get(null);
            if (conn != null) {
                Class.forName(conn.getDriverClassName());
            }
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Unable to load HSQLDB driver", e);
        }

        // Sanity check the database driver version (better be 2.x)
        String driverVersion = null;
        try {
            Driver driver = DriverManager.getDriver(jdbcUrl);
            driverVersion = String.format("%d.%d", driver.getMajorVersion(), driver.getMinorVersion());
            if (driver.getMajorVersion() != 2) {
                LOG.warn("HSQLDB database driver version {} is untested. Will make a copy of the DB prior to connecting. This may upgrade the database from version {}", driverVersion, currentVersion);
                return true;
            }
        } catch (SQLException e) {
            LOG.warn("HSQLDB database driver version cannot be determined. Will make a copy of the DB prior to connecting. This may upgrade the database from version {}", currentVersion, e);
            return true;
        }

        if (currentVersion.startsWith("2.")) {
            // If the database version is 2.x, it matches the driver major version, the upgrade should be relatively painless.
            LOG.debug("HSQLDB database backup not required for driver version {} connecting (and if needed, upgrading) DB version {}", currentVersion, driverVersion);
            return false;
        } else if (currentVersion.startsWith("1.")) {
            // If we're on a 1.x database, we're upgrading to 2.x and need to back up files.
            LOG.info("HSQLDB database upgrade needed, from version {} to {}", currentVersion, driverVersion);
            return true;
        } else {
            // If this happens we're on a completely untested version and we don't know what will happen.
            LOG.warn("HSQLDB database upgrade needed, from version {} to {}", currentVersion, driverVersion);
            return true;
        }
    }

    /**
     * Perform a backup of the HSQLDB database, to a timestamped directory.
     * @return the path to the backup directory
     */
    public static Path performHsqlDbBackup(String dbPath) throws IOException {
        String timestamp = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now());
        Path source = Paths.get(dbPath).getParent();
        Path destination = source.resolveSibling("backups").resolve(String.format("%s.backup.%s", source.getFileName().toString(), timestamp));

        LOG.debug("Performing HSQLDB database backup...");
        FileUtils.copyDirectory(source.toFile(), destination.toFile());
        LOG.info("HSQLDB database backed up to {}", destination.toString());

        return destination;
    }

    /**
     * Perform an in-place database upgrade from HSQLDB 1.x to 2.x.
     */
    public static void performHsqlDbUpgrade(String url, String user, String password) throws SQLException {
        LOG.debug("Performing HSQLDB database upgrade...");

        // This will upgrade HSQLDB on the first connection. This does not
        // use Spring's DataSource, as running SHUTDOWN against it will
        // prevent further connections to the database.
        try (Connection conn = getHsqlDbConnection(url, user, password)) {
            LOG.debug("Database connection established. Current version is: {}", conn.getMetaData().getDatabaseProductVersion());
            // On upgrade, the official documentation recommends that we
            // run 'SHUTDOWN SCRIPT' to compact all the database into a
            // single SQL file.
            //
            // In practice, if we don't do that, we did not observe issues
            // immediately but after the upgrade.
            LOG.debug("Shutting down database (SHUTDOWN SCRIPT)...");
            try (Statement st = conn.createStatement()) {
                st.execute("SHUTDOWN SCRIPT");
            }
        }

        LOG.info("HSQLDB database has been upgraded");
    }

    /**
     * If needed, perform an in-place database upgrade from HSQLDB 1.x to 2.x after having created backups.
     */
    public static void upgradeFileHsqlDbIfNeeded(Environment env) {
        String jdbcUrl = env.getProperty(SettingsService.KEY_DATABASE_URL);
        String dbPath = StringUtils.substringBetween(jdbcUrl, "jdbc:hsqldb:file:", ";");
        String jndi = env.getProperty(SettingsService.KEY_DATABASE_JNDI_NAME);

        if (jndi == null && dbPath != null && isHsqlDbBackupNeeded(dbPath, jdbcUrl)) {
            try {
                performHsqlDbBackup(dbPath);
            } catch (Exception e) {
                throw new RuntimeException("Failed to backup HSQLDB database before upgrade", e);
            }
            try {
                String user = env.getProperty(SettingsService.KEY_DATABASE_USERNAME);
                String password = env.getProperty(SettingsService.KEY_DATABASE_PASSWORD);
                performHsqlDbUpgrade(jdbcUrl, user, password);
                LOG.info("HSQLDB database version is now {}", getHsqlDbVersion(dbPath));
            } catch (Exception e) {
                throw new RuntimeException("Failed to upgrade HSQLDB database", e);
            }
        }
    }
}
