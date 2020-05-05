package org.airsonic.player.ajax;

import org.airsonic.player.domain.UserSettings;
import org.airsonic.player.service.SettingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.Instant;

@Controller
@MessageMapping("/settings")
public class UserSettingsWSController {
    @Autowired
    private SettingsService settingsService;

    @MessageMapping("/sidebar")
    @SendToUser
    public boolean setShowSideBar(Principal p, boolean show) {
        UserSettings userSettings = settingsService.getUserSettings(p.getName());
        if (show != userSettings.getShowSideBar()) {
            userSettings.setShowSideBar(show);
            userSettings.setChanged(Instant.now());
            settingsService.updateUserSettings(userSettings);
        }
        return show;
    }

    @MessageMapping("/viewAsList")
    @SendToUser
    public boolean setViewAsList(Principal p, boolean viewAsList) {
        UserSettings userSettings = settingsService.getUserSettings(p.getName());
        if (viewAsList != userSettings.getViewAsList()) {
            userSettings.setViewAsList(viewAsList);
            userSettings.setChanged(Instant.now());
            settingsService.updateUserSettings(userSettings);
        }
        return viewAsList;
    }

    public void setSettingsService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }
}
