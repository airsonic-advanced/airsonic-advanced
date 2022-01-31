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
package org.airsonic.player.command;

import org.airsonic.player.controller.PodcastSettingsController;
import org.airsonic.player.domain.PodcastChannelRule;

import java.util.List;
import java.util.Map;

/**
 * Command used in {@link PodcastSettingsController}.
 *
 * @author Sindre Mehus
 */
public class PodcastSettingsCommand {

    private Integer folderId;
    private Map<Integer, String> folders;
    private List<PodcastRule> rules;
    private PodcastRule newRule;
    private List<PodcastRule> noRuleChannels;

    public Integer getFolderId() {
        return folderId;
    }

    public void setFolderId(Integer folderId) {
        this.folderId = folderId;
    }

    public Map<Integer, String> getFolders() {
        return folders;
    }

    public void setFolders(Map<Integer, String> folders) {
        this.folders = folders;
    }

    public List<PodcastRule> getRules() {
        return rules;
    }

    public void setRules(List<PodcastRule> rules) {
        this.rules = rules;
    }

    public PodcastRule getNewRule() {
        return newRule;
    }

    public void setNewRule(PodcastRule newRule) {
        this.newRule = newRule;
    }

    public List<PodcastRule> getNoRuleChannels() {
        return noRuleChannels;
    }

    public void setNoRuleChannels(List<PodcastRule> noRuleChannels) {
        this.noRuleChannels = noRuleChannels;
    }

    public static class PodcastRule {
        private Integer id;
        private String name;
        private Integer interval;
        private Integer episodeRetentionCount;
        private Integer episodeDownloadCount;
        private boolean delete;

        public PodcastRule(PodcastChannelRule rule, String name) {
            this.id = rule.getId();
            this.name = name;
            this.interval = rule.getCheckInterval();
            this.episodeRetentionCount = rule.getRetentionCount();
            this.episodeDownloadCount = rule.getDownloadCount();
        }

        public PodcastRule() {
        }

        public PodcastRule(Integer id, String name) {
            this.id = id;
            this.name = name;
        }

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Integer getInterval() {
            return interval;
        }

        public void setInterval(Integer interval) {
            this.interval = interval;
        }

        public Integer getEpisodeRetentionCount() {
            return episodeRetentionCount;
        }

        public void setEpisodeRetentionCount(Integer episodeRetentionCount) {
            this.episodeRetentionCount = episodeRetentionCount;
        }

        public Integer getEpisodeDownloadCount() {
            return episodeDownloadCount;
        }

        public void setEpisodeDownloadCount(Integer episodeDownloadCount) {
            this.episodeDownloadCount = episodeDownloadCount;
        }

        public boolean getDelete() {
            return delete;
        }

        public void setDelete(boolean delete) {
            this.delete = delete;
        }

        public PodcastChannelRule toPodcastChannelRule() {
            if (id == null || interval == null || episodeRetentionCount == null || episodeDownloadCount == null) {
                return null;
            }
            return new PodcastChannelRule(id, interval, episodeRetentionCount, episodeDownloadCount);
        }
    }
}
