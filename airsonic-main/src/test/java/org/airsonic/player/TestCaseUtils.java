package org.airsonic.player;

import com.google.common.io.MoreFiles;
import com.google.common.io.RecursiveDeleteOption;

import org.airsonic.player.controller.JAXBWriter;
import org.airsonic.player.dao.AbstractDao;
import org.airsonic.player.service.MediaScannerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.TestConfiguration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TestCaseUtils {

    private static final Logger LOG = LoggerFactory.getLogger(TestCaseUtils.class);

    private static Path airsonicHomeDirForTest = null;

    /**
     * Returns the path of the AIRSONIC_HOME directory to use for tests.
     * This will create a temporary directory.
     *
     * @return AIRSONIC_HOME directory path.
     * @throws RuntimeException if it fails to create the temp directory.
     */
    public static String airsonicHomePathForTest() {

        if (airsonicHomeDirForTest == null) {
            try {
                airsonicHomeDirForTest = Files.createTempDirectory("airsonic_test_");
            } catch (IOException e) {
                throw new RuntimeException("Error while creating temporary AIRSONIC_HOME directory for tests");
            }
            LOG.info("AIRSONIC_HOME directory will be {}", airsonicHomeDirForTest.toAbsolutePath().toString());
        }
        return airsonicHomeDirForTest.toAbsolutePath().toString();
    }

    /**
     * @return current REST api version.
     */
    public static String restApiVersion() {
        return new JAXBWriter().getRestProtocolVersion();
    }

    /**
     * Cleans the AIRSONIC_HOME directory used for tests. (Does not delete the folder itself)
     */
    public static void cleanAirsonicHomeForTest() throws IOException {
        Path airsonicHomeDir = Paths.get(airsonicHomePathForTest());
        MoreFiles.deleteDirectoryContents(airsonicHomeDir, RecursiveDeleteOption.ALLOW_INSECURE);
    }

    /**
     * Constructs a map of records count per table.
     *
     * @param daoHelper DaoHelper object
     * @return Map table name -> records count
     */
    public static Map<String, Integer> recordsInAllTables(AbstractDao dao) {
        List<String> tableNames = dao.queryForStrings("" +
                      "select table_name " +
                      "from information_schema.system_tables " +
                      "where table_type <> 'SYSTEM TABLE'");

        return tableNames.parallelStream()
                .collect(Collectors.toConcurrentMap(table -> table, table -> recordsInTable(table, dao)));
    }

    /**
     * Counts records in a table.
     */
    public static Integer recordsInTable(String tableName, AbstractDao dao) {
        return dao.queryForInt("select count(*) from " + tableName, -1);
    }

    public static void waitForScanFinish(MediaScannerService mediaScannerService) {
        while (mediaScannerService.isScanning()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Scans the music library
     * @param mediaScannerService
     */
    public static void execScan(MediaScannerService mediaScannerService) {
        // TODO create a synchronous scan
        mediaScannerService.scanLibrary();

        waitForScanFinish(mediaScannerService);
    }

    @TestConfiguration
    public static class TestDao extends AbstractDao {

    }

}
