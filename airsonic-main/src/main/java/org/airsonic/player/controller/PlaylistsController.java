/*
 * This file is part of Airsonic.
 *
 *  Airsonic is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Airsonic is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Airsonic.  If not, see <http://www.gnu.org/licenses/>.
 *
 *  Copyright 2014 (C) Sindre Mehus
 */
package org.airsonic.player.controller;

import org.airsonic.player.domain.User;
import org.airsonic.player.domain.UserSettings;
import org.airsonic.player.service.SecurityService;
import org.airsonic.player.service.SettingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for the playlists page.
 *
 * @author Sindre Mehus
 */
@Controller
@RequestMapping("/playlists")
public class PlaylistsController {

    @Autowired
    private SecurityService securityService;

    @Autowired
    private SettingsService settingsService;

    @GetMapping
    public String doGet(HttpServletRequest request, Model model) {
        Map<String, Object> map = new HashMap<>();

        User user = securityService.getCurrentUser(request);
        UserSettings userSettings = settingsService.getUserSettings(user.getUsername());

        map.put("username", user.getUsername());
        map.put("viewAsList", userSettings.getViewAsList());
        map.put("initialPaginationSize", userSettings.getPaginationSizePlaylist());
        model.addAttribute("model", map);
        return "playlists";
    }
}
