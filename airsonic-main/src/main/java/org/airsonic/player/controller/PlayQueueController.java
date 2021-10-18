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

import com.google.common.collect.ImmutableMap;
import org.airsonic.player.domain.Player;
import org.airsonic.player.domain.User;
import org.airsonic.player.domain.UserSettings;
import org.airsonic.player.service.PlayerService;
import org.airsonic.player.service.SecurityService;
import org.airsonic.player.service.SettingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for the playlist frame.
 *
 * @author Sindre Mehus
 */
@Controller
@RequestMapping("/playQueue")
public class PlayQueueController {

    @Autowired
    private PlayerService playerService;
    @Autowired
    private SecurityService securityService;
    @Autowired
    private SettingsService settingsService;

    @GetMapping
    protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) throws Exception {

        User user = securityService.getCurrentUser(request);
        UserSettings userSettings = settingsService.getUserSettings(user.getUsername());

        Map<String, Object> map = new HashMap<>();
        map.put("user", user);
        map.put("players", playerService.getPlayersForUserAndClientId(user.getUsername(), null));
        map.put("visibility", userSettings.getPlayqueueVisibility());
        map.put("partyMode", userSettings.getPartyModeEnabled());
        map.put("notify", userSettings.getSongNotificationEnabled());
        map.put("autoHide", userSettings.getAutoHidePlayQueue());
        map.put("initialPaginationSize", userSettings.getPaginationSizePlayqueue());
        map.put("autoBookmark", userSettings.getAutoBookmark());
        map.put("audioBookmarkFrequency", userSettings.getAudioBookmarkFrequency());
        return new ModelAndView("playQueue", "model", map);
    }

    @GetMapping("/player")
    @ResponseBody
    public Map<String, Object> getPlayer(HttpServletRequest request, HttpServletResponse response) throws Exception {
        Player player = playerService.getPlayer(request, response);
        return ImmutableMap.of("id", player.getId(), "description", player.getShortDescription(), "tech", player.getTechnology());
    }
}
