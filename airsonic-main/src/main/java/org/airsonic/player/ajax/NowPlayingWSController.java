package org.airsonic.player.ajax;

import org.airsonic.player.service.StatusService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
public class NowPlayingWSController {
    @Autowired
    private StatusService statusService;

    @SubscribeMapping("/nowPlaying/current")
    public List<NowPlayingInfo> getActivePlays() {
        return statusService.getActivePlays();
    }

    @SubscribeMapping("/nowPlaying/recent")
    public List<NowPlayingInfo> getInactivePlays() {
        return statusService.getInactivePlays();
    }
}
