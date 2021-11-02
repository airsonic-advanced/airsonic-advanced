package org.airsonic.player.dao;

import org.airsonic.player.domain.PlayQueue;
import org.airsonic.player.service.SettingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PlayerDaoPlayQueueFactory {

    @Autowired
    SettingsService settingsService;

    public PlayQueue createPlayQueue() {
        return new PlayQueue(id -> settingsService.getMusicFolderById(id));
    }
}
