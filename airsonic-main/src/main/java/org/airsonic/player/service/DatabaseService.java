package org.airsonic.player.service;

import liquibase.Contexts;
import liquibase.Liquibase;
import liquibase.command.core.ExecuteSqlCommand;
import liquibase.database.Database;
import liquibase.database.DatabaseConnection;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.diff.output.DiffOutputControl;
import liquibase.diff.output.StandardObjectChangeFilter;
import liquibase.integration.commandline.CommandLineUtils;
import liquibase.resource.FileSystemResourceAccessor;
import org.airsonic.player.dao.DatabaseDao;
import org.airsonic.player.util.FileUtil;
import org.airsonic.player.util.LambdaUtils;
import org.airsonic.player.util.LegacyHsqlMigrationUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static java.util.stream.Collectors.joining;

@Service
public class DatabaseService {
    private static final Logger LOG = LoggerFactory.getLogger(DatabaseService.class);

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Autowired
    SettingsService settingsService;
    @Autowired
    DatabaseDao databaseDao;
    @Autowired
    private SimpMessagingTemplate brokerTemplate;

    public synchronized void backup() {
        brokerTemplate.convertAndSend("/topic/backupStatus", "started");

        if (backuppable()) {
            try {
                String dbPath = StringUtils.substringBetween(settingsService.getDatabaseUrl(), "jdbc:hsqldb:file:", ";");
                Path backupLocation = LegacyHsqlMigrationUtil.performHsqlDbBackup(dbPath);
                LOG.info("Backed up DB to location: {}", backupLocation);
                brokerTemplate.convertAndSend("/topic/backupStatus", "location: " + backupLocation);
            } catch (Exception e) {
                throw new RuntimeException("Failed to backup HSQLDB database", e);
            }
        } else {
            LOG.info("DB unable to be backed up via these means");
        }
        brokerTemplate.convertAndSend("/topic/backupStatus", "ended");
    }

    public boolean backuppable() {
        return settingsService.getDatabaseJNDIName() == null
                && StringUtils.startsWith(settingsService.getDatabaseUrl(), "jdbc:hsqldb:file:");
    }

    Function<Connection, Path> exportFunction = LambdaUtils.uncheckFunction(
        connection -> generateChangeLog(connection, "data", "airsonic-data", makeDiffOutputControl()));

    Function<Path, Consumer<Connection>> importFunction = p -> LambdaUtils.uncheckConsumer(
        connection -> runLiquibaseUpdate(connection, p));

    public synchronized Path exportDB() throws Exception {
        brokerTemplate.convertAndSend("/topic/exportStatus", "started");
        Path fPath = databaseDao.exportDB(exportFunction);
        brokerTemplate.convertAndSend("/topic/exportStatus", "Local DB extraction complete, compressing...");
        Path zPath = zip(fPath);
        brokerTemplate.convertAndSend("/topic/exportStatus", "ended");
        return zPath;
    }

    public void cleanup(Path p) {
        if (Files.isDirectory(p)) {
            FileUtil.delete(p);
        } else {
            FileUtil.delete(p.getParent());
        }
    }

    private Path zip(Path folder) throws Exception {
        Path zipName = folder.resolve(folder.getFileName().toString() + ".zip");
        try (OutputStream fos = Files.newOutputStream(zipName);
                ZipOutputStream zipOut = new ZipOutputStream(fos);
                Stream<Path> files = Files.list(folder);) {
            files.filter(f -> !f.equals(zipName)).forEach(LambdaUtils.uncheckConsumer(f -> {
                ZipEntry zipEntry = new ZipEntry(f.getFileName().toString());
                zipOut.putNextEntry(zipEntry);
                Files.copy(f, zipOut);
                zipOut.closeEntry();
            }));
        }

        return zipName;
    }

    public synchronized void importDB(Path p) {
        brokerTemplate.convertAndSend("/topic/importStatus", "started");
        backup();
        brokerTemplate.convertAndSend("/topic/importStatus", "Importing XML");
        databaseDao.importDB(importFunction.apply(p));
        brokerTemplate.convertAndSend("/topic/importStatus", "Import complete. Cleaning up...");
        cleanup(p);
        brokerTemplate.convertAndSend("/topic/importStatus", "ended");
    }

    private void runLiquibaseUpdate(Connection connection, Path p) throws Exception {
        Database database = getDatabase(connection);
        truncateAll(database, connection);
        try (Stream<Path> files = Files.list(p)) {
            files.forEach(LambdaUtils.uncheckConsumer(f -> {
                Liquibase liquibase = new Liquibase(p.relativize(f).toString(), new FileSystemResourceAccessor(p.toFile()), database);
                liquibase.update(new Contexts());
            }));
        }
    }

    private static void truncateAll(Database db, Connection c) throws Exception {
        String sql = TABLE_ORDER.stream().flatMap(t -> t.stream())
                .map(t -> "delete from " + t).collect(joining("; "));
        ExecuteSqlCommand esc = new ExecuteSqlCommand();
        esc.setDatabase(db);
        esc.setSql(sql);
        esc.execute();
    }

    private static List<List<String>> TABLE_ORDER = Arrays.asList(
            Arrays.asList("user", "user_credentials", "user_settings"),
            Arrays.asList("music_folder", "music_folder_user", "transcoding"),
            Arrays.asList("media_file", "music_file_info", "player"),
            Arrays.asList("player_transcoding", "playlist", "playlist_file", "playlist_user", "play_queue", "play_queue_file", "internet_radio"),
            Arrays.asList("album", "artist", "genre"),
            Arrays.asList("podcast_channel", "podcast_episode"),
            Arrays.asList("bookmark", "share", "share_file", "sonoslink"),
            Arrays.asList("starred_album", "starred_artist", "starred_media_file", "user_rating", "custom_avatar"));

    private Path generateChangeLog(Connection connection, String snapshotTypes, String author, DiffOutputControl diffOutputControl) throws Exception {
        Database database = getDatabase(connection);
        Path fPath = getChangeLogFolder();
        Files.createDirectories(fPath);
        for (int i = 0; i < TABLE_ORDER.size(); i++) {
            setTableFilter(diffOutputControl, TABLE_ORDER.get(i));
            CommandLineUtils.doGenerateChangeLog(fPath.resolve(i + ".xml").toString(), database, null, null,
                    snapshotTypes, author, null, null, diffOutputControl);
        }

        return fPath;
    }

    private Database getDatabase(Connection connection) throws Exception {
        DatabaseConnection databaseConnection = new JdbcConnection(connection);
        return DatabaseFactory.getInstance().findCorrectDatabaseImplementation(databaseConnection);
    }

    private static Path getChangeLogFolder() {
        String timestamp = LocalDateTime.now().format(DATE_TIME_FORMATTER);
        return SettingsService.getAirsonicHome().resolve("backups")
                .resolve(String.format("airsonic.exportDB.%s", timestamp));
    }

    public static Path getImportDBFolder() {
        String timestamp = LocalDateTime.now().format(DATE_TIME_FORMATTER);
        return SettingsService.getAirsonicHome().resolve("backups")
                .resolve(String.format("airsonic.importDB.%s", timestamp));
    }

    private DiffOutputControl makeDiffOutputControl() {
        return new DiffOutputControl(false, false, false, null);
    }

    private void setTableFilter(DiffOutputControl diffOutputControl, List<String> tables) {
        StandardObjectChangeFilter filter = new StandardObjectChangeFilter(
                StandardObjectChangeFilter.FilterType.INCLUDE,
                tables.stream().map(t -> MessageFormat.format("table:(?i){0}", t)).collect(joining(",")));
        diffOutputControl.setObjectChangeFilter(filter);
    }
}
