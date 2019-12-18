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

import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.PlayStatus;
import org.airsonic.player.domain.Player;
import org.airsonic.player.domain.TransferStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
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

    private final List<TransferStatus> streamStatuses = Collections.synchronizedList(new ArrayList<>());
    private final List<TransferStatus> downloadStatuses = Collections.synchronizedList(new ArrayList<>());
    private final List<TransferStatus> uploadStatuses = Collections.synchronizedList(new ArrayList<>());
    private final List<PlayStatus> remotePlays = Collections.synchronizedList(new ArrayList<>());

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
            return status;
        });
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
        List<TransferStatus> result = streamStatuses.parallelStream()
                .filter(s -> s.getPlayer().getId().equals(player.getId()))
                .collect(Collectors.toList());

        if (!result.isEmpty()) {
            return result;
        }

        return Optional.ofNullable(inactiveStreamStatuses.get(player.getId()))
                .map(Collections::singletonList)
                .orElseGet(Collections::emptyList);
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

    public void addRemotePlay(PlayStatus playStatus) {
        remotePlays.removeIf(PlayStatus::isExpired);
        remotePlays.add(playStatus);
    }

    public List<PlayStatus> getPlayStatuses() {
        List<PlayStatus> remotePlaySnapshot = new ArrayList<>(remotePlays);

        return Stream.concat(
                remotePlaySnapshot.parallelStream().filter(Predicate.not(PlayStatus::isExpired)),
                getAllStreamStatuses().parallelStream().map(streamStatus -> {
                    Path file = streamStatus.getFile();
                    if (file == null) {
                        return null;
                    }
                    Player player = streamStatus.getPlayer();
                    MediaFile mediaFile = mediaFileService.getMediaFile(file);
                    if (player == null || mediaFile == null) {
                        return null;
                    }
                    Instant time = Instant.now().minusMillis(streamStatus.getMillisSinceLastUpdate());
                    return new PlayStatus(mediaFile, player, time);
                }).filter(Objects::nonNull))
                .collect(Collectors.toList());
    }

    private TransferStatus createStatus(Player player, List<TransferStatus> statusList) {
        TransferStatus status = new TransferStatus(player);
        statusList.add(status);
        return status;
    }

    public void setMediaFileService(MediaFileService mediaFileService) {
        this.mediaFileService = mediaFileService;
    }
}
