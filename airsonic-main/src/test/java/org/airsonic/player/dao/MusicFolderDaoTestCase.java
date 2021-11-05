/*
 This file is part of Airsonic.

 Airsonic is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Airsonic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Airsonic.  If not, see <http://www.gnu.org/licenses/>.

 Copyright 2016 (C) Airsonic Authors
 Based upon Subsonic, Copyright 2009 (C) Sindre Mehus
 */
package org.airsonic.player.dao;

import org.airsonic.player.domain.MusicFolder;
import org.airsonic.player.domain.MusicFolder.Type;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit test of {@link MusicFolderDao}.
 *
 * @author Sindre Mehus
 */
public class MusicFolderDaoTestCase extends DaoTestCaseBean2 {

    @Autowired
    MusicFolderDao musicFolderDao;

    @Before
    public void setUp() {
        getJdbcTemplate().execute("delete from music_folder");
    }

    @Test
    public void testCreateMusicFolder() {
        MusicFolder musicFolder = new MusicFolder(Paths.get("path"), "name", Type.MEDIA, true, Instant.now());
        musicFolderDao.createMusicFolder(musicFolder);

        // no duplicates
        musicFolderDao.createMusicFolder(new MusicFolder(Paths.get("path"), "name", Type.MEDIA, true, Instant.now()));

        assertThat(musicFolderDao.getAllMusicFolders()).usingElementComparatorIgnoringFields("id")
                .containsExactly(musicFolder);
    }

    @Test
    public void testUpdateMusicFolder() {
        MusicFolder musicFolder = new MusicFolder(Paths.get("path"), "name", Type.MEDIA, true, Instant.now());
        musicFolderDao.createMusicFolder(musicFolder);
        musicFolder = musicFolderDao.getAllMusicFolders().get(0);

        musicFolder.setPath(Paths.get("newPath"));
        musicFolder.setName("newName");
        musicFolder.setEnabled(false);
        musicFolder.setChanged(Instant.ofEpochMilli(234234L));
        musicFolderDao.updateMusicFolder(musicFolder);

        assertThat(musicFolderDao.getAllMusicFolders()).element(0).isEqualToComparingFieldByField(musicFolder);
    }

    @Test
    public void testDeleteMusicFolder() {
        assertThat(musicFolderDao.getAllMusicFolders()).hasSize(0);

        musicFolderDao.createMusicFolder(new MusicFolder(Paths.get("path"), "name", Type.MEDIA, true, Instant.now()));

        List<MusicFolder> musicFolders = musicFolderDao.getAllMusicFolders();
        assertThat(musicFolders).hasSize(1);

        musicFolderDao.deleteMusicFolder(musicFolders.get(0).getId());
        assertThat(musicFolderDao.getAllMusicFolders()).hasSize(0);
    }
}