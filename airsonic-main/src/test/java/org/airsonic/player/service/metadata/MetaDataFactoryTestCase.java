package org.airsonic.player.service.metadata;

import org.airsonic.player.service.SettingsService;
import org.airsonic.player.util.HomeRule;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.nio.file.Path;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest
public class MetaDataFactoryTestCase {

    @ClassRule
    public static final HomeRule airsonicRule = new HomeRule();

    @ClassRule
    public static TemporaryFolder temporaryFolder = new TemporaryFolder();

    private static Path someMp3;
    private static Path someFlv;
    private static Path someJunk;

    @BeforeClass
    public static void createTestFiles() throws IOException {
        someMp3 = temporaryFolder.newFile("some.mp3").toPath();
        someFlv = temporaryFolder.newFile("some.flv").toPath();
        someJunk = temporaryFolder.newFile("some.junk").toPath();
    }

    @Autowired
    MetaDataParserFactory metaDataParserFactory;

    @Autowired
    SettingsService settingsService;

    @Test
    public void testorder() {
        MetaDataParser parser;

        settingsService.setVideoFileTypes("mp3 flv");

        parser = metaDataParserFactory.getParser(someMp3);
        assertThat(parser, instanceOf(JaudiotaggerParser.class));

        parser = metaDataParserFactory.getParser(someFlv);
        assertThat(parser, instanceOf(FFmpegParser.class));

        parser = metaDataParserFactory.getParser(someJunk);
        assertThat(parser, instanceOf(FFmpegParser.class));
    }

}
