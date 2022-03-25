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
package org.airsonic.player.controller;

import org.airsonic.player.domain.*;
import org.airsonic.player.service.*;
import org.airsonic.player.service.search.IndexManager;
import org.airsonic.player.util.FileUtil;
import org.airsonic.player.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.support.RequestContextUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.nio.file.Path;
import java.time.Instant;
import java.util.*;

/**
 * Controller for the left index frame.
 *
 * @author Sindre Mehus
 */
@Controller
@RequestMapping("/left")
public class LeftController {

    // Update this time if you want to force a refresh in clients.
    private static final Instant LAST_COMPATIBILITY_TIME = Instant.parse("2012-03-06T00:00:00.00Z");

    @Autowired
    private MediaScannerService mediaScannerService;
    @Autowired
    private IndexManager indexManager;
    @Autowired
    private SettingsService settingsService;
    @Autowired
    private SecurityService securityService;
    @Autowired
    private MusicIndexService musicIndexService;
    @Autowired
    private PlayerService playerService;
    @Autowired
    private MediaFolderService mediaFolderService;

    /**
     * Note: This class intentionally does not implement org.springframework.web.servlet.mvc.LastModified
     * as we don't need browser-side caching of left.jsp.  This method is only used by RESTController.
     */
    long getLastModified(HttpServletRequest request) throws Exception {
        saveSelectedMusicFolder(request);

        if (mediaScannerService.isScanning()) {
            return -1L;
        }

        long lastModified = LAST_COMPATIBILITY_TIME.toEpochMilli();
        String username = securityService.getCurrentUsername(request);
        UserSettings userSettings = settingsService.getUserSettings(username);

        // When was settings last changed?
        lastModified = Math.max(lastModified, settingsService.getSettingsChanged());

        // When was music folder(s) on disk last changed?

        List<MusicFolder> allMusicFolders = mediaFolderService.getMusicFoldersForUser(username);
        MusicFolder selectedMusicFolder = allMusicFolders.stream()
                .filter(f -> f.getId().equals(userSettings.getSelectedMusicFolderId()))
                .findAny().orElse(null);
        if (selectedMusicFolder != null) {
            Path file = selectedMusicFolder.getPath();
            lastModified = Math.max(lastModified, FileUtil.lastModified(file).toEpochMilli());
        } else {
            for (MusicFolder musicFolder : allMusicFolders) {
                Path file = musicFolder.getPath();
                lastModified = Math.max(lastModified, FileUtil.lastModified(file).toEpochMilli());
            }
        }

        // When was music folder table last changed?
        for (MusicFolder musicFolder : allMusicFolders) {
            lastModified = Math.max(lastModified, musicFolder.getChanged().toEpochMilli());
        }

        // When was internet radio table last changed?
        for (InternetRadio internetRadio : settingsService.getAllInternetRadios()) {
            lastModified = Math.max(lastModified, internetRadio.getChanged().toEpochMilli());
        }

        // When was user settings last changed?
        lastModified = Math.max(lastModified, userSettings.getChanged().toEpochMilli());

        return lastModified;
    }

    @GetMapping
    protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) throws Exception {
        boolean musicFolderChanged = saveSelectedMusicFolder(request);
        Map<String, Object> map = new HashMap<>();

        MediaLibraryStatistics statistics = indexManager.getStatistics();
        Locale locale = RequestContextUtils.getLocale(request);

        boolean refresh = ServletRequestUtils.getBooleanParameter(request, "refresh", false);
        if (refresh) {
            mediaFolderService.clearMusicFolderCache();
        }

        String username = securityService.getCurrentUsername(request);
        UserSettings userSettings = settingsService.getUserSettings(username);
        List<MusicFolder> allMusicFolders = mediaFolderService.getMusicFoldersForUser(username);
        List<MusicFolder> musicFoldersToUse = mediaFolderService.getMusicFoldersForUser(username, userSettings.getSelectedMusicFolderId());
        MusicFolder selectedMusicFolder = musicFoldersToUse.stream()
                .filter(f -> f.getId().equals(userSettings.getSelectedMusicFolderId()))
                .findAny().orElse(null);
        MusicFolderContent musicFolderContent = musicIndexService.getMusicFolderContent(musicFoldersToUse, refresh);

        map.put("player", playerService.getPlayer(request, response));
        map.put("scanning", mediaScannerService.isScanning());
        map.put("musicFolders", allMusicFolders);
        map.put("selectedMusicFolder", selectedMusicFolder);
        map.put("radios", settingsService.getAllInternetRadios());
        map.put("shortcuts", musicIndexService.getShortcuts(musicFoldersToUse));
        map.put("partyMode", userSettings.getPartyModeEnabled());
        map.put("organizeByFolderStructure", settingsService.isOrganizeByFolderStructure());
        map.put("musicFolderChanged", musicFolderChanged);

        if (statistics != null) {
            map.put("statistics", statistics);
            long bytes = statistics.getTotalLengthInBytes();
            long hours = (long) (statistics.getTotalDurationInSeconds() / 3600L);
            map.put("hours", hours);
            map.put("bytes", StringUtil.formatBytes(bytes, locale));
        }

        map.put("indexedArtists", musicFolderContent.getIndexedArtists());
        map.put("singleSongs", musicFolderContent.getSingleSongs());
        map.put("indexes", musicFolderContent.getIndexedArtists().keySet());
        map.put("user", securityService.getCurrentUser(request));

        return new ModelAndView("left","model",map);
    }

    private boolean saveSelectedMusicFolder(HttpServletRequest request) throws Exception {
        Integer musicFolderId = ServletRequestUtils.getIntParameter(request, "musicFolderId");
        if (musicFolderId == null) {
            return false;
        }
        // Note: UserSettings.setChanged() is intentionally not called. This would break browser caching
        // of the left frame.
        UserSettings settings = settingsService.getUserSettings(securityService.getCurrentUsername(request));
        settings.setSelectedMusicFolderId(musicFolderId);
        settingsService.updateUserSettings(settings);

        return true;
    }
}
