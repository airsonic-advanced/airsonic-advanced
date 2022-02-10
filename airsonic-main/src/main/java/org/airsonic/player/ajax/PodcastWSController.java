package org.airsonic.player.ajax;

import org.airsonic.player.domain.PodcastChannel;
import org.airsonic.player.domain.PodcastEpisode;
import org.airsonic.player.domain.PodcastStatus;
import org.airsonic.player.service.PodcastService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

import java.util.List;

import static java.util.stream.Collectors.toList;

@Controller
@MessageMapping("/podcasts")
public class PodcastWSController {
    @Autowired
    PodcastService podcastService;

    @SubscribeMapping("all")
    public List<PodcastChannelInfo> getAllPodcastChannels() {
        return podcastService.getAllChannels().stream().map(this::wrap).collect(toList());
    }

    @MessageMapping("channel")
    @SendToUser(broadcast = false)
    public PodcastChannelInfo getPodcastChannel(Integer channelId) {
        return wrap(podcastService.getChannel(channelId));
    }

    private PodcastChannelInfo wrap(PodcastChannel channel) {
        List<PodcastEpisode> episodes = podcastService.getEpisodes(channel.getId());

        return new PodcastChannelInfo(channel, episodes.size(), (int) episodes.stream().filter(e -> e.getStatus() == PodcastStatus.COMPLETED).count());
    }

    @MessageMapping("delete")
    public void deleteChannels(List<Integer> ids) {
        ids.forEach(id -> podcastService.deleteChannel(id));
    }

    public static class PodcastChannelInfo extends PodcastChannel {
        private int fileCount;
        private int downloadedCount;

        public PodcastChannelInfo(PodcastChannel channel, int fileCount, int downloadedCount) {
            super(channel.getId(), channel.getUrl(), channel.getTitle(), channel.getDescription(),
                    channel.getImageUrl(), channel.getStatus(), channel.getErrorMessage(), channel.getMediaFileId());
            this.fileCount = fileCount;
            this.downloadedCount = downloadedCount;
        }

        public int getFileCount() {
            return fileCount;
        }

        public void setFileCount(int fileCount) {
            this.fileCount = fileCount;
        }

        public int getDownloadedCount() {
            return downloadedCount;
        }

        public void setDownloadedCount(int downloadedCount) {
            this.downloadedCount = downloadedCount;
        }

    }

}
