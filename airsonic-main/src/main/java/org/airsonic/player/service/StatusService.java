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
package org.airsonic.player.service;

import org.airsonic.player.ajax.NowPlayingInfo;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.PlayStatus;
import org.airsonic.player.domain.Player;
import org.airsonic.player.domain.TransferStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Provides services for maintaining the list of stream, download and upload statuses.
 * <p/>
 * Note that for stream statuses, the last inactive status is also stored.
 *
 * @author Sindre Mehus
 * @see TransferStatus
 */
@Service
public class StatusService {

    @Autowired
    private MediaFileService mediaFileService;
    @Autowired
    private MediaFolderService mediaFolderService;
    @Autowired
    private SettingsService settingsService;
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    @Autowired
    private TaskSchedulingService taskService;

    @PostConstruct
    public void cleanup() {
        taskService.scheduleFixedDelayTask("remote-playstatus-cleanup", () -> cleanupRemotePlays(), Instant.now().plus(3, ChronoUnit.HOURS), Duration.ofHours(3), true);
    }

    private final List<TransferStatus> streamStatuses = Collections.synchronizedList(new ArrayList<>());
    private final List<TransferStatus> downloadStatuses = Collections.synchronizedList(new ArrayList<>());
    private final List<TransferStatus> uploadStatuses = Collections.synchronizedList(new ArrayList<>());
    private final Set<PlayStatus> remotePlays = ConcurrentHashMap.newKeySet();
    private final Set<PlayStatus> activeLocalPlays = ConcurrentHashMap.newKeySet();

    // Maps from player ID to latest inactive stream status.
    private final Map<Integer, TransferStatus> inactiveStreamStatuses = new ConcurrentHashMap<>();

    public TransferStatus createStreamStatus(Player player) {
        return createStatus(player, streamStatuses);
    }

    public void removeStreamStatus(TransferStatus status) {
        // Move it to the map of inactive statuses.
        inactiveStreamStatuses.compute(status.getPlayer().getId(), (k, v) -> {
            streamStatuses.remove(status);
            status.setActive(false);
            if (v != null) {
                broadcast(getPlayStatus(v), "recent/remove");
            }
            return status;
        });
        broadcast(getPlayStatus(status), "recent/add");
    }

    public List<TransferStatus> getAllStreamStatuses() {
        List<TransferStatus> snapshot = new ArrayList<>(streamStatuses);
        Set<Integer> playerIds = snapshot.parallelStream()
                .map(x -> x.getPlayer().getId())
                .collect(Collectors.toCollection(() -> ConcurrentHashMap.newKeySet()));
        // Add inactive status for those players that have no active status.
        return Stream.concat(
                snapshot.parallelStream(),
                inactiveStreamStatuses.values().parallelStream().filter(s -> !playerIds.contains(s.getPlayer().getId())))
                .collect(Collectors.toList());
    }

    public List<TransferStatus> getStreamStatusesForPlayer(Player player) {
        // unsynchronized stream access, but should be okay, we'll just be a bit behind
        return streamStatuses.parallelStream()
                .filter(s -> s.getPlayer().getId().equals(player.getId()))
                .collect(Collectors.toList());
    }

    public TransferStatus getInactiveStreamStatusForPlayer(Player player) {
        return inactiveStreamStatuses.get(player.getId());
    }

    public TransferStatus createDownloadStatus(Player player) {
        return createStatus(player, downloadStatuses);
    }

    public void removeDownloadStatus(TransferStatus status) {
        downloadStatuses.remove(status);
    }

    public List<TransferStatus> getAllDownloadStatuses() {
        return new ArrayList<>(downloadStatuses);
    }

    public TransferStatus createUploadStatus(Player player) {
        return createStatus(player, uploadStatuses);
    }

    public void removeUploadStatus(TransferStatus status) {
        uploadStatuses.remove(status);
    }

    public List<TransferStatus> getAllUploadStatuses() {
        return new ArrayList<>(uploadStatuses);
    }

    public void cleanupRemotePlays() {
        Set<PlayStatus> expired = remotePlays.parallelStream().filter(PlayStatus::isExpired).collect(Collectors.toSet());
        expired.forEach(e -> {
            remotePlays.remove(e);
            broadcast(e, "recent/remove");
        });
    }

    public void addRemotePlay(PlayStatus playStatus) {
        remotePlays.add(playStatus);
        broadcast(playStatus, "recent/add");
    }

    public void addActiveLocalPlay(PlayStatus status) {
        activeLocalPlays.add(status);
        broadcast(status, "current/add");
    }

    public void removeActiveLocalPlay(PlayStatus status) {
        activeLocalPlays.remove(status);
        broadcast(status, "current/remove");
    }

    public PlayStatus getPlayStatus(TransferStatus status) {
        MediaFile file = status.getMediaFile();
        if (file == null) {
            file = mediaFileService.getMediaFile(status.getExternalFile());
        }
        return new PlayStatus(status.getId(),
                file,
                status.getPlayer(),
                status.getMillisSinceLastUpdate());
    }

    private TransferStatus createStatus(Player player, List<TransferStatus> statusList) {
        TransferStatus status = new TransferStatus(player);
        statusList.add(status);
        return status;
    }

    private void broadcast(PlayStatus status, String location) {
        CompletableFuture.runAsync(() -> {
            NowPlayingInfo info = NowPlayingInfo.createForBroadcast(status, settingsService);

            if (info != null) {
                messagingTemplate.convertAndSend("/topic/nowPlaying/" + location, info);
            }
        });
    }

    public List<NowPlayingInfo> getActivePlays() {
        return activeLocalPlays.parallelStream()
                .map(s -> NowPlayingInfo.createForBroadcast(s, settingsService))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public List<NowPlayingInfo> getInactivePlays() {
        return Stream
                .concat(remotePlays.parallelStream(),
                        inactiveStreamStatuses.values().parallelStream().map(ts -> getPlayStatus(ts)))
                .map(s -> NowPlayingInfo.createForBroadcast(s, settingsService))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public void setMediaFileService(MediaFileService mediaFileService) {
        this.mediaFileService = mediaFileService;
    }

    public void setSettingsService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    public void setMessagingTemplate(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }
}
