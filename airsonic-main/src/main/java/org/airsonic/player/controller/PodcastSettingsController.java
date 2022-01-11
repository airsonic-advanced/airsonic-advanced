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
import org.airsonic.player.command.PodcastSettingsCommand.PodcastRule;
import org.airsonic.player.domain.PodcastChannel;
import org.airsonic.player.domain.PodcastChannelRule;
import org.airsonic.player.service.PodcastService;
import org.airsonic.player.service.SettingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;

/**
 * Controller for the page used to administrate the Podcast receiver.
 *
 * @author Sindre Mehus
 */
@Controller
@RequestMapping("/podcastSettings")
public class PodcastSettingsController {

    @Autowired
    private SettingsService settingsService;
    @Autowired
    private PodcastService podcastService;

    @GetMapping
    protected String formBackingObject(Model model) {
        PodcastSettingsCommand command = new PodcastSettingsCommand();

        List<PodcastChannel> channels = podcastService.getAllChannels();
        List<PodcastChannelRule> rules = podcastService.getAllChannelRules();
        command.setRules(rules.stream()
                .map(cr -> new PodcastRule(
                        cr,
                        channels.stream()
                            .filter(c -> c.getId().equals(cr.getId()))
                            .findFirst()
                            .map(c -> c.getTitle()).orElse(null)))
                .collect(toList()));
        command.getRules()
                .add(new PodcastRule(
                        new PodcastChannelRule(-1, settingsService.getPodcastUpdateInterval(), settingsService.getPodcastEpisodeRetentionCount(), settingsService.getPodcastEpisodeDownloadCount()),
                        "DEFAULT"));

        command.setFolder(settingsService.getPodcastFolder());
        command.setNewRule(new PodcastRule());
        command.setNoRuleChannels(channels.parallelStream()
                .filter(c -> rules.stream().noneMatch(r -> r.getId().equals(c.getId())))
                .map(c -> new PodcastRule(c.getId(), c.getTitle()))
                .collect(toList()));

        model.addAttribute("command",command);
        return "podcastSettings";
    }

    @PostMapping
    protected String doSubmitAction(@ModelAttribute PodcastSettingsCommand command, RedirectAttributes redirectAttributes) {
        PodcastRule defaultRule = command.getRules().stream().filter(r -> r.getId().equals(-1)).findFirst().get();
        settingsService.setPodcastUpdateInterval(defaultRule.getInterval());
        settingsService.setPodcastEpisodeRetentionCount(defaultRule.getEpisodeRetentionCount());
        settingsService.setPodcastEpisodeDownloadCount(defaultRule.getEpisodeDownloadCount());
        settingsService.setPodcastFolder(command.getFolder());
        settingsService.save();
        podcastService.scheduleDefault();

        command.getRules().stream().filter(r -> !r.getId().equals(-1)).forEach(r -> {
            if (r.getDelete()) {
                podcastService.deleteChannelRule(r.getId());
            } else {
                podcastService.createOrUpdateChannelRule(r.toPodcastChannelRule());
            }
        });

        Optional.ofNullable(command.getNewRule().toPodcastChannelRule())
                .ifPresent(podcastService::createOrUpdateChannelRule);

        redirectAttributes.addFlashAttribute("settings_toast", true);
        return "redirect:podcastSettings.view";
    }

}
