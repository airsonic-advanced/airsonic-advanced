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

import org.airsonic.player.controller.MusicFolderSettingsController;
import org.airsonic.player.domain.MusicFolder;
import org.airsonic.player.domain.MusicFolder.Type;
import org.apache.commons.lang.StringUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;

/**
 * Command used in {@link MusicFolderSettingsController}.
 *
 * @author Sindre Mehus
 */
public class MusicFolderSettingsCommand {

    private String interval;
    private String hour;
    private boolean scanning;
    private boolean fastCache;
    private boolean organizeByFolderStructure;
    private List<MusicFolderInfo> musicFolders;
    private MusicFolderInfo newMusicFolder;
    private List<MusicFolderInfo> deletedMusicFolders;
    private EnumSet<Type> musicFolderTypes = EnumSet.allOf(MusicFolder.Type.class);
    private String uploadsFolder;
    private String excludePatternString;
    private boolean ignoreSymLinks;
    private Boolean fullScan;
    private Boolean clearFullScanSettingAfterScan;

    public String getInterval() {
        return interval;
    }

    public void setInterval(String interval) {
        this.interval = interval;
    }

    public String getHour() {
        return hour;
    }

    public void setHour(String hour) {
        this.hour = hour;
    }

    public boolean isScanning() {
        return scanning;
    }

    public void setScanning(boolean scanning) {
        this.scanning = scanning;
    }

    public boolean isFastCache() {
        return fastCache;
    }

    public List<MusicFolderInfo> getMusicFolders() {
        return musicFolders;
    }

    public void setMusicFolders(List<MusicFolderInfo> musicFolders) {
        this.musicFolders = musicFolders;
    }

    public void setFastCache(boolean fastCache) {
        this.fastCache = fastCache;
    }

    public MusicFolderInfo getNewMusicFolder() {
        return newMusicFolder;
    }

    public void setNewMusicFolder(MusicFolderInfo newMusicFolder) {
        this.newMusicFolder = newMusicFolder;
    }

    public List<MusicFolderInfo> getDeletedMusicFolders() {
        return deletedMusicFolders;
    }

    public void setDeletedMusicFolders(List<MusicFolderInfo> deletedMusicFolders) {
        this.deletedMusicFolders = deletedMusicFolders;
    }

    public EnumSet<Type> getMusicFolderTypes() {
        return musicFolderTypes;
    }

    public void setMusicFolderTypes(EnumSet<Type> musicFolderTypes) {
        this.musicFolderTypes = musicFolderTypes;
    }

    public boolean isOrganizeByFolderStructure() {
        return organizeByFolderStructure;
    }

    public void setOrganizeByFolderStructure(boolean organizeByFolderStructure) {
        this.organizeByFolderStructure = organizeByFolderStructure;
    }

    public String getExcludePatternString() {
        return excludePatternString;
    }

    public void setExcludePatternString(String excludePatternString) {
        this.excludePatternString = excludePatternString;
    }

    public boolean getIgnoreSymLinks() {
        return ignoreSymLinks;
    }

    public void setIgnoreSymLinks(boolean ignoreSymLinks) {
        this.ignoreSymLinks = ignoreSymLinks;
    }

    public Boolean getFullScan() {
        return fullScan;
    }

    public void setFullScan(Boolean fullScan) {
        this.fullScan = fullScan;
    }

    public Boolean getClearFullScanSettingAfterScan() {
        return clearFullScanSettingAfterScan;
    }

    public void setClearFullScanSettingAfterScan(Boolean clearFullScanSettingAfterScan) {
        this.clearFullScanSettingAfterScan = clearFullScanSettingAfterScan;
    }

    public String getUploadsFolder() {
        return uploadsFolder;
    }

    public void setUploadsFolder(String uploadsFolder) {
        this.uploadsFolder = uploadsFolder;
    }

    public static class MusicFolderInfo {

        private Integer id;
        private String path;
        private String name;
        private String type = Type.MEDIA.name();
        private Boolean enabled;
        private boolean delete;
        private boolean existing;
        private boolean overlap;
        private String overlapStatus;

        public MusicFolderInfo(MusicFolder musicFolder, boolean overlap, String overlapStatus) {
            id = musicFolder.getId();
            path = musicFolder.getPath().toString();
            name = musicFolder.getName();
            type = musicFolder.getType().name();
            enabled = musicFolder.isEnabled();
            existing = Files.exists(musicFolder.getPath()) && Files.isDirectory(musicFolder.getPath());
            this.overlap = overlap;
            this.overlapStatus = overlapStatus;
        }

        public MusicFolderInfo() {
            enabled = true;
        }

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Boolean getEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }

        public boolean getDelete() {
            return delete;
        }

        public void setDelete(boolean delete) {
            this.delete = delete;
        }

        public boolean getOverlap() {
            return overlap;
        }

        public String getOverlapStatus() {
            return overlapStatus;
        }

        public boolean getExisting() {
            return existing;
        }

        public MusicFolder toMusicFolder() {
            String path = StringUtils.trimToNull(this.path);
            if (path == null) {
                return null;
            }
            Path file = Paths.get(path);
            String name = StringUtils.trimToNull(this.name);
            if (name == null) {
                name = file.getFileName().toString();
            }
            return new MusicFolder(id, file, name, MusicFolder.Type.valueOf(type), enabled != null && enabled, Instant.now());
        }
    }
}
