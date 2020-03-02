package org.airsonic.player.service.search;

import org.airsonic.player.TestCaseUtils;
import org.airsonic.player.dao.DaoHelper;
import org.airsonic.player.dao.MusicFolderDao;
import org.airsonic.player.service.MediaScannerService;
import org.airsonic.player.service.SettingsService;
import org.airsonic.player.util.HomeRule;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.concurrent.atomic.AtomicBoolean;

@RunWith(SpringRunner.class)
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
/*
 * Abstract class for scanning MusicFolder.
 */
public abstract class AbstractAirsonicHomeTest implements AirsonicHomeTest {

    @ClassRule
    public static final HomeRule airsonicRule = new HomeRule();

    /*
     * Currently, Maven is executing test classes in series,
     * so this class can hold the state.
     * When executing in parallel, subclasses should override this.
     */
    private static AtomicBoolean dataBasePopulated = new AtomicBoolean();

    // Above.
    private static AtomicBoolean dataBaseReady = new AtomicBoolean();

    @Autowired
    protected DaoHelper daoHelper;

    @Autowired
    protected MediaScannerService mediaScannerService;

    @Autowired
    protected MusicFolderDao musicFolderDao;

    @Autowired
    protected SettingsService settingsService;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Override
    public AtomicBoolean dataBasePopulated() {
        return dataBasePopulated;
    }

    @Override
    public AtomicBoolean dataBaseReady() {
        return dataBaseReady;
    }

    @Override
    public final void populateDatabaseOnlyOnce() {
        if (!dataBasePopulated().get()) {
            dataBasePopulated().set(true);
            getMusicFolders().forEach(musicFolderDao::createMusicFolder);
            settingsService.clearMusicFolderCache();
            // wait for previous startup scan to finish
            while (mediaScannerService.isScanning()) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            // scan again
            TestCaseUtils.execScan(mediaScannerService);
            dataBaseReady().set(true);
        } else {
            while (!dataBaseReady().get()) {
                try {
                    // The subsequent test method waits while reading DB data.
                    for (int i = 0; i < 10; i++) {
                        Thread.sleep(100);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}