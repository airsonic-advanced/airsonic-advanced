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

import org.airsonic.player.domain.Player;
import org.airsonic.player.domain.TransferStatus;
import org.airsonic.player.service.MediaFileService;
import org.airsonic.player.service.PlayerService;
import org.airsonic.player.service.StatusService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.Optional;

/**
 * Controller for showing what's currently playing.
 *
 * @author Sindre Mehus
 */
@Controller
@RequestMapping("/nowPlaying")
public class NowPlayingController {

    @Autowired
    private PlayerService playerService;
    @Autowired
    private StatusService statusService;
    @Autowired
    private MediaFileService mediaFileService;

    @GetMapping
    protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) throws Exception {

        Player player = playerService.getPlayer(request, response);
        TransferStatus status = statusService.getStreamStatusesForPlayer(player).stream().findFirst()
                .orElseGet(() -> statusService.getInactiveStreamStatusForPlayer(player));

        String url = Optional.ofNullable(status)
                .map(s -> {
                    if (s.getMediaFile() != null) {
                        return s.getMediaFile();
                    }
                    return mediaFileService.getMediaFile(s.getExternalFile());
                })
                .map(mediaFileService::getParentOf)
                .filter(dir -> !mediaFileService.isRoot(dir))
                .map(dir -> "main.view?id=" + dir.getId())
                .orElse("home.view");

        return new ModelAndView(new RedirectView(url));
    }
}
