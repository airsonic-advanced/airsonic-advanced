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

import org.airsonic.player.command.PersonalSettingsCommand;
import org.airsonic.player.domain.*;
import org.airsonic.player.domain.UserCredential.App;
import org.airsonic.player.service.SecurityService;
import org.airsonic.player.service.SettingsService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;

import java.time.Instant;
import java.util.Locale;
import java.util.Map;

/**
 * Controller for the page used to administrate per-user settings.
 *
 * @author Sindre Mehus
 */
@Controller
@RequestMapping("/personalSettings")
public class PersonalSettingsController {

    @Autowired
    private SettingsService settingsService;
    @Autowired
    private SecurityService securityService;

    @ModelAttribute
    protected void formBackingObject(HttpServletRequest request,Model model) {
        PersonalSettingsCommand command = new PersonalSettingsCommand();

        User user = securityService.getCurrentUser(request);
        UserSettings userSettings = settingsService.getUserSettings(user.getUsername());

        command.setUser(user);
        command.setLocaleIndex("-1");
        command.setThemeIndex("-1");
        command.setAlbumLists(AlbumListType.values());
        command.setAlbumListId(userSettings.getDefaultAlbumList().getId());
        command.setAvatars(settingsService.getAllSystemAvatars());
        command.setCustomAvatar(settingsService.getCustomAvatar(user.getUsername()));
        command.setAvatarId(getAvatarId(userSettings));
        command.setPartyModeEnabled(userSettings.getPartyModeEnabled());
        command.setQueueFollowingSongs(userSettings.getQueueFollowingSongs());
        command.setShowNowPlayingEnabled(userSettings.getShowNowPlayingEnabled());
        command.setShowArtistInfoEnabled(userSettings.getShowArtistInfoEnabled());
        command.setNowPlayingAllowed(userSettings.getNowPlayingAllowed());
        command.setMainVisibility(userSettings.getMainVisibility());
        command.setPlaylistVisibility(userSettings.getPlaylistVisibility());
        command.setPlayqueueVisibility(userSettings.getPlayqueueVisibility());
        command.setFinalVersionNotificationEnabled(userSettings.getFinalVersionNotificationEnabled());
        command.setBetaVersionNotificationEnabled(userSettings.getBetaVersionNotificationEnabled());
        command.setSongNotificationEnabled(userSettings.getSongNotificationEnabled());
        command.setAutoHidePlayQueue(userSettings.getAutoHidePlayQueue());
        command.setKeyboardShortcutsEnabled(userSettings.getKeyboardShortcutsEnabled());
        command.setLastFmEnabled(userSettings.getLastFmEnabled());
        command.setListenBrainzEnabled(userSettings.getListenBrainzEnabled());
        command.setListenBrainzUrl(userSettings.getListenBrainzUrl());
        command.setPodcastIndexEnabled(userSettings.getPodcastIndexEnabled());
        command.setPodcastIndexUrl(userSettings.getPodcastIndexUrl());
        command.setPaginationSizeFiles(userSettings.getPaginationSizeFiles());
        command.setPaginationSizeFolders(userSettings.getPaginationSizeFolders());
        command.setPaginationSizePlaylist(userSettings.getPaginationSizePlaylist());
        command.setPaginationSizePlayqueue(userSettings.getPaginationSizePlayqueue());
        command.setPaginationSizeBookmarks(userSettings.getPaginationSizeBookmarks());
        command.setAutoBookmark(userSettings.getAutoBookmark());
        command.setAudioBookmarkFrequency(userSettings.getAudioBookmarkFrequency());
        command.setVideoBookmarkFrequency(userSettings.getVideoBookmarkFrequency());
        command.setSearchCount(userSettings.getSearchCount());

        Locale currentLocale = userSettings.getLocale();
        Locale[] locales = settingsService.getAvailableLocales();
        String[] localeStrings = new String[locales.length];
        for (int i = 0; i < locales.length; i++) {
            localeStrings[i] = locales[i].getDisplayName(locales[i]);
            if (locales[i].equals(currentLocale)) {
                command.setLocaleIndex(String.valueOf(i));
            }
        }
        command.setLocales(localeStrings);

        String currentThemeId = userSettings.getThemeId();
        Theme[] themes = settingsService.getAvailableThemes();
        command.setThemes(themes);
        for (int i = 0; i < themes.length; i++) {
            if (themes[i].getId().equals(currentThemeId)) {
                command.setThemeIndex(String.valueOf(i));
                break;
            }
        }

        model.addAttribute("command", command);

        Map<App, UserCredential> thirdPartyCreds = securityService.getDecodableCredsForApps(user.getUsername(), App.LASTFM, App.LISTENBRAINZ, App.PODCASTINDEX);

        model.addAttribute("lastfmCredsAbsent", thirdPartyCreds.get(App.LASTFM) == null);
        model.addAttribute("listenBrainzCredsAbsent", thirdPartyCreds.get(App.LISTENBRAINZ) == null);
        model.addAttribute("podcastIndexCredsAbsent", thirdPartyCreds.get(App.PODCASTINDEX) == null);
    }

    @GetMapping
    protected String displayForm() {
        return "personalSettings";
    }

    @PostMapping
    protected String doSubmitAction(@ModelAttribute("command") PersonalSettingsCommand command, RedirectAttributes redirectAttributes) {

        int localeIndex = Integer.parseInt(command.getLocaleIndex());
        Locale locale = null;
        if (localeIndex != -1) {
            locale = settingsService.getAvailableLocales()[localeIndex];
        }

        int themeIndex = Integer.parseInt(command.getThemeIndex());
        String themeId = null;
        if (themeIndex != -1) {
            themeId = settingsService.getAvailableThemes()[themeIndex].getId();
        }

        String username = command.getUser().getUsername();
        UserSettings settings = settingsService.getUserSettings(username);

        settings.setLocale(locale);
        settings.setThemeId(themeId);
        settings.setDefaultAlbumList(AlbumListType.fromId(command.getAlbumListId()));
        settings.setPartyModeEnabled(command.isPartyModeEnabled());
        settings.setQueueFollowingSongs(command.isQueueFollowingSongs());
        settings.setShowNowPlayingEnabled(command.isShowNowPlayingEnabled());
        settings.setShowArtistInfoEnabled(command.isShowArtistInfoEnabled());
        settings.setNowPlayingAllowed(command.isNowPlayingAllowed());
        settings.setMainVisibility(command.getMainVisibility());
        settings.setPlaylistVisibility(command.getPlaylistVisibility());
        settings.setPlayqueueVisibility(command.getPlayqueueVisibility());
        settings.setFinalVersionNotificationEnabled(command.isFinalVersionNotificationEnabled());
        settings.setBetaVersionNotificationEnabled(command.isBetaVersionNotificationEnabled());
        settings.setSongNotificationEnabled(command.isSongNotificationEnabled());
        settings.setAutoHidePlayQueue(command.isAutoHidePlayQueue());
        settings.setKeyboardShortcutsEnabled(command.isKeyboardShortcutsEnabled());
        settings.setLastFmEnabled(command.getLastFmEnabled());
        settings.setListenBrainzEnabled(command.getListenBrainzEnabled());
        settings.setListenBrainzUrl(StringUtils.trimToNull(command.getListenBrainzUrl()));
        settings.setPodcastIndexEnabled(command.getPodcastIndexEnabled());
        settings.setPodcastIndexUrl(StringUtils.trimToNull(command.getPodcastIndexUrl()));
        settings.setSystemAvatarId(getSystemAvatarId(command));
        settings.setAvatarScheme(getAvatarScheme(command));
        settings.setPaginationSizeFiles(command.getPaginationSizeFiles());
        settings.setPaginationSizeFolders(command.getPaginationSizeFolders());
        settings.setPaginationSizePlaylist(command.getPaginationSizePlaylist());
        settings.setPaginationSizePlayqueue(command.getPaginationSizePlayqueue());
        settings.setPaginationSizeBookmarks(command.getPaginationSizeBookmarks());
        settings.setAutoBookmark(command.getAutoBookmark());
        settings.setAudioBookmarkFrequency(command.getAudioBookmarkFrequency());
        settings.setVideoBookmarkFrequency(command.getVideoBookmarkFrequency());
        settings.setSearchCount(command.getSearchCount());

        settings.setChanged(Instant.now());
        settingsService.updateUserSettings(settings);

        redirectAttributes.addFlashAttribute("settings_reload", true);
        redirectAttributes.addFlashAttribute("settings_toast", true);

        return "redirect:personalSettings.view";
    }

    private int getAvatarId(UserSettings userSettings) {
        AvatarScheme avatarScheme = userSettings.getAvatarScheme();
        return avatarScheme == AvatarScheme.SYSTEM ? userSettings.getSystemAvatarId() : avatarScheme.getCode();
    }

    private AvatarScheme getAvatarScheme(PersonalSettingsCommand command) {
        if (command.getAvatarId() == AvatarScheme.NONE.getCode()) {
            return AvatarScheme.NONE;
        }
        if (command.getAvatarId() == AvatarScheme.CUSTOM.getCode()) {
            return AvatarScheme.CUSTOM;
        }
        return AvatarScheme.SYSTEM;
    }

    private Integer getSystemAvatarId(PersonalSettingsCommand command) {
        int avatarId = command.getAvatarId();
        if (avatarId == AvatarScheme.NONE.getCode() ||
            avatarId == AvatarScheme.CUSTOM.getCode()) {
            return null;
        }
        return avatarId;
    }

}
