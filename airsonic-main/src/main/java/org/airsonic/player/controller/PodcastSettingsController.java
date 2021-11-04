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

import org.airsonic.player.command.PodcastSettingsCommand;
import org.airsonic.player.domain.MusicFolder;
import org.airsonic.player.domain.MusicFolder.Type;
import org.airsonic.player.service.PodcastService;
import org.airsonic.player.service.SettingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.file.Paths;
import java.time.Instant;

/**
 * Controller for the page used to administrate the Podcast receiver.
 *
 * @author Sindre Mehus
 */
@Controller
@RequestMapping("/podcastSettings")
public class PodcastSettingsController {
    private static final Logger LOG = LoggerFactory.getLogger(PodcastSettingsController.class);

    @Autowired
    private SettingsService settingsService;
    @Autowired
    private PodcastService podcastService;

    @GetMapping
    protected String formBackingObject(Model model) {
        PodcastSettingsCommand command = new PodcastSettingsCommand();

        command.setInterval(String.valueOf(settingsService.getPodcastUpdateInterval()));
        command.setEpisodeRetentionCount(String.valueOf(settingsService.getPodcastEpisodeRetentionCount()));
        command.setEpisodeDownloadCount(String.valueOf(settingsService.getPodcastEpisodeDownloadCount()));
        command.setFolder(settingsService.getAllMusicFolders(true, true).stream()
                .filter(f -> f.getType() == Type.PODCAST)
                .map(MusicFolder::getPath)
                .findAny()
                .map(p -> p.toString())
                .orElse(null));

        model.addAttribute("command", command);
        return "podcastSettings";
    }

    @PostMapping
    protected String doSubmitAction(@ModelAttribute PodcastSettingsCommand command, RedirectAttributes redirectAttributes) {
        settingsService.setPodcastUpdateInterval(Integer.parseInt(command.getInterval()));
        settingsService.setPodcastEpisodeRetentionCount(Integer.parseInt(command.getEpisodeRetentionCount()));
        settingsService.setPodcastEpisodeDownloadCount(Integer.parseInt(command.getEpisodeDownloadCount()));
        settingsService.save();
        boolean success = true;
        MusicFolder podcastFolder = settingsService.getAllMusicFolders(true, true).stream()
                .filter(f -> f.getType() == Type.PODCAST).findAny().orElse(null);
        if (podcastFolder != null) {
            podcastFolder.setPath(Paths.get(command.getFolder()));
            try {
                settingsService.updateMusicFolder(podcastFolder);
            } catch (Exception e) {
                LOG.warn("Could not update music folder id {} ({})", podcastFolder.getId(), podcastFolder.getName(), e);
                success = false;
            }
        } else {
            podcastFolder = new MusicFolder(Paths.get(command.getFolder()), "Podcasts", Type.PODCAST, true, Instant.now());
            try {
                settingsService.createMusicFolder(podcastFolder);
            } catch (Exception e) {
                LOG.warn("Could not create music folder {}", podcastFolder.getName(), e);
                success = false;
            }
        }

        podcastService.schedule();
        redirectAttributes.addFlashAttribute("settings_toast", success);
        return "redirect:podcastSettings.view";
    }

}
