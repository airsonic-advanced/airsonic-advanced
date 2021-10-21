package org.airsonic.player.ajax;

import org.airsonic.player.dao.MediaFileDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.List;

@Controller
@MessageMapping("/rate/mediafile")
public class StarWSController {
    @Autowired
    private MediaFileDao mediaFileDao;

    @MessageMapping("/star")
    public void star(Principal user, List<Integer> ids) {
        mediaFileDao.starMediaFiles(ids, user.getName());
    }

    @MessageMapping("/unstar")
    public void unstar(Principal user, List<Integer> ids) {
        mediaFileDao.unstarMediaFiles(ids, user.getName());
    }

    public void setMediaFileDao(MediaFileDao mediaFileDao) {
        this.mediaFileDao = mediaFileDao;
    }
}
