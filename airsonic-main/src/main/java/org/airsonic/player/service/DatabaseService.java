package org.airsonic.player.service;

import liquibase.Contexts;
import liquibase.Liquibase;
import liquibase.command.CommandScope;
import liquibase.command.core.InternalExecuteSqlCommandStep;
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
import org.airsonic.player.util.LambdaUtils.ThrowingBiFunction;
import org.airsonic.player.util.LegacyHsqlMigrationUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.sql.Connection;
import java.text.MessageFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
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
    @Autowired
    private TaskSchedulingService taskService;

    @PostConstruct
    public void init() {
        try {
            schedule();
        } catch (Throwable x) {
            LOG.error("Failed to initialize DatabaseService", x);
        }
    }

    private synchronized void schedule() {
        int hoursBetween = settingsService.getDbBackupInterval();

        if (hoursBetween == -1) {
            LOG.info("Automatic DB backup disabled");
            unschedule();
            return;
        }

        long initialDelayMillis = 5L * 60L * 1000L;
        Instant firstTime = Instant.now().plusMillis(initialDelayMillis);

        taskService.scheduleAtFixedRate("db-backup", backupTask, firstTime, Duration.ofHours(hoursBetween), true);

        LOG.info("Automatic DB backup scheduled to run every {} hour(s), starting at {}", hoursBetween, firstTime);
    }

    public void unschedule() {
        taskService.unscheduleTask("db-backup");
    }

    private Runnable backupTask = () -> {
        LOG.info("Starting scheduled DB backup");
        backup();
        LOG.info("Completed scheduled DB backup");
    };

    public synchronized void backup() {
        brokerTemplate.convertAndSend("/topic/backupStatus", "started");

        if (backuppable()) {
            try {
                String dbPath = StringUtils.substringBetween(settingsService.getDatabaseUrl(), "jdbc:hsqldb:file:", ";");
                Path backupLocation = LegacyHsqlMigrationUtil.performHsqlDbBackup(dbPath);
                LOG.info("Backed up DB to location: {}", backupLocation);
                brokerTemplate.convertAndSend("/topic/backupStatus", "location: " + backupLocation);
                deleteObsoleteBackups(backupLocation);
            } catch (Exception e) {
                throw new RuntimeException("Failed to backup HSQLDB database", e);
            }
        } else {
            LOG.info("DB unable to be backed up via these means");
        }
        brokerTemplate.convertAndSend("/topic/backupStatus", "ended");
    }

    private synchronized void deleteObsoleteBackups(Path backupLocation) {
        AtomicInteger backupCount = new AtomicInteger(settingsService.getDbBackupRetentionCount());
        if (backupCount.get() == -1) {
            return;
        }

        String backupNamePattern = StringUtils.substringBeforeLast(backupLocation.getFileName().toString(), ".");
        try (Stream<Path> backups = Files.list(backupLocation.getParent());) {
            backups.filter(p -> p.getFileName().toString().startsWith(backupNamePattern))
                    .sorted(Comparator.comparing(
                            LambdaUtils.<Path, FileTime, Exception>uncheckFunction(p -> Files.readAttributes(p, BasicFileAttributes.class).creationTime()),
                            Comparator.reverseOrder()))
                    .forEach(p -> {
                        if (backupCount.getAndDecrement() <= 0) {
                            FileUtil.delete(p);
                        }
                    });
        } catch (Exception e) {
            LOG.warn("Could not clean up DB backups", e);
        }
    }

    public boolean backuppable() {
        return settingsService.getDatabaseJNDIName() == null
                && StringUtils.startsWith(settingsService.getDatabaseUrl(), "jdbc:hsqldb:file:");
    }

    ThrowingBiFunction<Path, Connection, Boolean, Exception> exportFunction = (tmpPath, connection) -> generateChangeLog(tmpPath, connection, "data", "airsonic-data", makeDiffOutputControl());

    Function<Path, Consumer<Connection>> importFunction = p -> LambdaUtils.uncheckConsumer(
        connection -> runLiquibaseUpdate(connection, p));

    public synchronized Path exportDB() throws Exception {
        brokerTemplate.convertAndSend("/topic/exportStatus", "started");
        Path fPath = getChangeLogFolder();
        Path zPath = null;
        try {
            databaseDao.exportDB(fPath, exportFunction);
            zPath = zip(fPath);
            brokerTemplate.convertAndSend("/topic/exportStatus", "Local DB extraction complete, compressing...");
        } catch (Exception e) {
            LOG.info("DB Export failed!", e);
            brokerTemplate.convertAndSend("/topic/exportStatus", "Error with local DB extraction, check logs...");
            cleanup(fPath);
        }

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
        if (Files.notExists(p) || !Files.isDirectory(p) || p.toFile().list().length == 0) {
            brokerTemplate.convertAndSend("/topic/importStatus", "Nothing imported");
        } else {
            backup();
            brokerTemplate.convertAndSend("/topic/importStatus", "Importing XML");
            databaseDao.importDB(importFunction.apply(p));
            brokerTemplate.convertAndSend("/topic/importStatus", "Import complete. Cleaning up...");
            cleanup(p);
        }
        brokerTemplate.convertAndSend("/topic/importStatus", "ended");
    }

    private void runLiquibaseUpdate(Connection connection, Path p) throws Exception {
        Database database = getDatabase(connection);
        truncateAll(database, connection);
        try (Stream<Path> files = Files.list(p)) {
            files.sorted().forEach(LambdaUtils.uncheckConsumer(f -> {
                Liquibase liquibase = new Liquibase(p.relativize(f).toString(), new FileSystemResourceAccessor(p.toFile()), database);
                liquibase.update(new Contexts());
            }));
        }
    }

    private static void truncateAll(Database db, Connection c) throws Exception {
        String sql = TABLE_ORDER.stream().flatMap(t -> t.stream())
                .map(t -> "delete from " + t).collect(joining("; "));
        CommandScope commandScope = new CommandScope("internalExecuteSql");
        commandScope.addArgumentValue(InternalExecuteSqlCommandStep.DATABASE_ARG, db);
        commandScope.addArgumentValue(InternalExecuteSqlCommandStep.SQL_ARG, sql);
        commandScope.addArgumentValue(InternalExecuteSqlCommandStep.DELIMITER_ARG, ";");

        commandScope.execute();
    }

    private static List<List<String>> TABLE_ORDER = Arrays.asList(
            Arrays.asList("users", "music_folder", "transcoding", "player"),
            Arrays.asList("music_folder_user", "user_credentials", "user_settings", "player_transcoding"),
            Arrays.asList("media_file", "music_file_info"),
            Arrays.asList("playlist", "play_queue", "internet_radio"),
            Arrays.asList("playlist_file", "playlist_user", "play_queue_file"),
            Arrays.asList("album", "artist", "genre"),
            Arrays.asList("podcast_channel", "share"),
            Arrays.asList("cover_art"),
            Arrays.asList("podcast_channel_rules", "podcast_episode", "bookmark", "share_file", "sonoslink"),
            Arrays.asList("starred_album", "starred_artist", "starred_media_file", "user_rating", "custom_avatar"));

    private boolean generateChangeLog(Path fPath, Connection connection, String snapshotTypes, String author, DiffOutputControl diffOutputControl) throws Exception {
        Database database = getDatabase(connection);
        Files.createDirectories(fPath);
        for (int i = 0; i < TABLE_ORDER.size(); i++) {
            setTableFilter(diffOutputControl, TABLE_ORDER.get(i));
            CommandLineUtils.doGenerateChangeLog(fPath.resolve(i + ".xml").toString(), database, null, null,
                    snapshotTypes, author, null, null, diffOutputControl);
        }

        return true;
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
