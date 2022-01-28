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
import org.airsonic.player.util.Util;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.*;

/**
 * Controller for the main page.
 *
 * @author Sindre Mehus
 */
@Controller
@RequestMapping("/main")
public class MainController {

    @Autowired
    private SecurityService securityService;
    @Autowired
    private SettingsService settingsService;
    @Autowired
    private MediaFileService mediaFileService;
    @Autowired
    private PodcastService podcastService;

    @GetMapping
    protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) throws Exception {
        Map<String, Object> map = new HashMap<>();

        String username = securityService.getCurrentUsername(request);
        UserSettings userSettings = settingsService.getUserSettings(username);
        int[] requestParameterIds = ServletRequestUtils.getIntParameters(request, "id");

        if (requestParameterIds.length == 1 && isPodcast(requestParameterIds[0])) {
            return new ModelAndView(new RedirectView("/podcastChannel.view?id=" + getPodcastChannelId(requestParameterIds[0])));
        }

        map.put("coverArtSizeMedium", CoverArtScheme.MEDIUM.getSize());
        map.put("coverArtSizeLarge", CoverArtScheme.LARGE.getSize());
        map.put("user", securityService.getCurrentUser(request));
        map.put("visibility", userSettings.getMainVisibility());
        map.put("showAlbumYear", settingsService.isSortAlbumsByYear());
        map.put("showArtistInfo", userSettings.getShowArtistInfoEnabled());
        map.put("partyMode", userSettings.getPartyModeEnabled());
        map.put("brand", settingsService.getBrand());
        map.put("viewAsList", userSettings.getViewAsList());
        map.put("initialPaginationSizeFiles", userSettings.getPaginationSizeFiles());
        map.put("initialPaginationSizeFolders", userSettings.getPaginationSizeFolders());
        map.put("initialPathsJSON", Util.toJson(ServletRequestUtils.getStringParameters(request, "path")));
        map.put("initialIdsJSON", Util.toJson(requestParameterIds));

        return new ModelAndView("mediaMain", "model", map);
    }

    private boolean isPodcast(int id) {
        MediaFile mediaFile = mediaFileService.getMediaFile(id);
        return mediaFile.isPodcast();
    }

    private int getPodcastChannelId(int id) {
        MediaFile mediaFile = mediaFileService.getMediaFile(id);
        return podcastService.getChannelIdByMediaFile(mediaFile);
    }
}
