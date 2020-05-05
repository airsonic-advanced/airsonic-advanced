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
package org.airsonic.player.ajax;

import org.airsonic.player.domain.AvatarScheme;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.PlayStatus;
import org.airsonic.player.domain.Player;
import org.airsonic.player.domain.UserSettings;
import org.airsonic.player.service.SettingsService;
import org.airsonic.player.util.StringUtil;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

import java.util.UUID;

/**
 * Details about what a user is currently listening to.
 *
 * @author Sindre Mehus
 */
public class NowPlayingInfo {

    private final UUID transferId;
    private final Integer playerId;
    private final Integer mediaFileId;
    private final String username;
    private final String artist;
    private final String title;
    private final String tooltip;
    private final String streamUrl;
    private final String albumUrl;
    private final String lyricsUrl;
    private final String coverArtUrl;
    private final String avatarUrl;
    private final long minutesAgo;
    private final PlayStatus playStatus;

    public NowPlayingInfo(Integer playerId, String user, String artist, String title, String tooltip, String streamUrl, String albumUrl,
            String lyricsUrl, String coverArtUrl, String avatarUrl, long minutesAgo) {
        this(null, playerId, null, user, artist, title, tooltip, streamUrl, albumUrl, lyricsUrl, coverArtUrl, avatarUrl,
                minutesAgo, null);
    }

    public NowPlayingInfo(UUID transferId, Integer playerId, Integer mediaFileId, String user, String artist, String title,
            String tooltip, String streamUrl, String albumUrl, String lyricsUrl, String coverArtUrl, String avatarUrl,
            long minutesAgo, PlayStatus playStatus) {
        this.transferId = transferId;
        this.playerId = playerId;
        this.mediaFileId = mediaFileId;
        this.username = user;
        this.artist = artist;
        this.title = title;
        this.tooltip = tooltip;
        this.streamUrl = streamUrl;
        this.albumUrl = albumUrl;
        this.lyricsUrl = lyricsUrl;
        this.coverArtUrl = coverArtUrl;
        this.avatarUrl = avatarUrl;
        this.minutesAgo = minutesAgo;
        this.playStatus = playStatus;
    }

    public UUID getTransferId() {
        return transferId;
    }

    public Integer getPlayerId() {
        return playerId;
    }

    public Integer getMediaFileId() {
        return mediaFileId;
    }

    public String getUsername() {
        return username;
    }

    public String getArtist() {
        return artist;
    }

    public String getTitle() {
        return title;
    }

    public String getTooltip() {
        return tooltip;
    }

    public String getStreamUrl() {
        return streamUrl;
    }

    public String getAlbumUrl() {
        return albumUrl;
    }

    public String getLyricsUrl() {
        return lyricsUrl;
    }

    public String getCoverArtUrl() {
        return coverArtUrl;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public long getMinutesAgo() {
        return minutesAgo;
    }

    // deliberately do not make it a getter so it doesn't serialize
    public PlayStatus fromPlayStatus() {
        return playStatus;
    }

    public static NowPlayingInfo createForBroadcast(PlayStatus status, SettingsService settingsService) {
        String url = "";// NetworkService.getBaseUrl(request);

        Player player = status.getPlayer();
        MediaFile mediaFile = status.getMediaFile();
        if (mediaFile == null) {
            return null;
        }
        String username = player.getUsername();
        if (username == null) {
            return null;
        }
        long minutesAgo = status.getMinutesAgo();
        if (minutesAgo > 60) {
            return null;
        }
        UserSettings userSettings = settingsService.getUserSettings(username);
        if (!userSettings.getNowPlayingAllowed()) {
            return null;
        }

        String artist = mediaFile.getArtist();
        String title = mediaFile.getTitle();
        String streamUrl = url + "stream?player=" + player.getId() + "&id=" + mediaFile.getId();
        String albumUrl = url + "main.view?id=" + mediaFile.getId();
        String lyricsUrl = null;
        if (!mediaFile.isVideo()) {
            lyricsUrl = url + "lyrics.view?artistUtf8Hex=" + StringUtil.utf8HexEncode(artist) + "&songUtf8Hex="
                    + StringUtil.utf8HexEncode(title);
        }
        String coverArtUrl = url + "coverArt.view?size=60&id=" + mediaFile.getId();

        String avatarUrl = null;
        if (userSettings.getAvatarScheme() == AvatarScheme.SYSTEM) {
            avatarUrl = url + "avatar.view?id=" + userSettings.getSystemAvatarId();
        } else if (userSettings.getAvatarScheme() == AvatarScheme.CUSTOM
                && settingsService.getCustomAvatar(username) != null) {
            avatarUrl = url + "avatar.view?usernameUtf8Hex=" + StringUtil.utf8HexEncode(username);
        }

        String tooltip = StringEscapeUtils.escapeHtml(artist) + " &ndash; " + StringEscapeUtils.escapeHtml(title);

        if (StringUtils.isNotBlank(player.getName())) {
            username += "@" + player.getName();
        }
        artist = StringEscapeUtils.escapeHtml(StringUtils.abbreviate(artist, 25));
        title = StringEscapeUtils.escapeHtml(StringUtils.abbreviate(title, 25));
        username = StringEscapeUtils.escapeHtml(StringUtils.abbreviate(username, 25));

        return new NowPlayingInfo(status.getTransferId(), player.getId(), mediaFile.getId(), username, artist, title,
                tooltip, streamUrl, albumUrl, lyricsUrl, coverArtUrl, avatarUrl, minutesAgo, status);
    }
}
