package org.airsonic.player.domain;

public class PodcastChannelRule {
    private Integer id;
    private Integer checkInterval;
    private Integer retentionCount;
    private Integer downloadCount;

    public PodcastChannelRule(Integer id, Integer checkInterval, Integer retentionCount, Integer downloadCount) {
        super();
        this.id = id;
        this.checkInterval = checkInterval;
        this.retentionCount = retentionCount;
        this.downloadCount = downloadCount;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getCheckInterval() {
        return checkInterval;
    }

    public void setCheckInterval(Integer checkInterval) {
        this.checkInterval = checkInterval;
    }

    public Integer getRetentionCount() {
        return retentionCount;
    }

    public void setRetentionCount(Integer retentionCount) {
        this.retentionCount = retentionCount;
    }

    public Integer getDownloadCount() {
        return downloadCount;
    }

    public void setDownloadCount(Integer downloadCount) {
        this.downloadCount = downloadCount;
    }

}
