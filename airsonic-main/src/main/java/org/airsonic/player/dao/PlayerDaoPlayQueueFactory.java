package org.airsonic.player.dao;

import org.airsonic.player.domain.PlayQueue;
import org.airsonic.player.service.MediaFolderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PlayerDaoPlayQueueFactory {

    @Autowired
    private MediaFolderService mediaFolderService;

    public PlayQueue createPlayQueue() {
        return new PlayQueue(id -> mediaFolderService.getMusicFolderById(id));
    }
}
