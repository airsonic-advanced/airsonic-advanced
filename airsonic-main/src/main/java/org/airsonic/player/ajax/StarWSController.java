package org.airsonic.player.ajax;

import org.airsonic.player.dao.MediaFileDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@MessageMapping("/rate")
public class StarWSController {
    @Autowired
    private MediaFileDao mediaFileDao;

    @MessageMapping("/star")
    public void star(Principal user, int id) {
        mediaFileDao.starMediaFile(id, user.getName());
    }

    @MessageMapping("/unstar")
    public void unstar(Principal user, int id) {
        mediaFileDao.unstarMediaFile(id, user.getName());
    }

    public void setMediaFileDao(MediaFileDao mediaFileDao) {
        this.mediaFileDao = mediaFileDao;
    }
}
