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

import org.airsonic.player.dao.ShareDao;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.MusicFolder;
import org.airsonic.player.domain.Share;
import org.airsonic.player.domain.User;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;

import static java.util.stream.Collectors.toList;

/**
 * Provides services for sharing media.
 *
 * @author Sindre Mehus
 * @see Share
 */
@Service
public class ShareService {

    private static final Logger LOG = LoggerFactory.getLogger(ShareService.class);

    @Autowired
    private ShareDao shareDao;
    @Autowired
    private SecurityService securityService;
    @Autowired
    private MediaFileService mediaFileService;
    @Autowired
    private JWTSecurityService jwtSecurityService;

    public List<Share> getAllShares() {
        return shareDao.getAllShares();
    }

    public List<Share> getSharesForUser(User user) {
        return getAllShares().stream().filter(share -> user.isAdminRole() || ObjectUtils.equals(user.getUsername(), share.getUsername())).collect(toList());
    }

    public Share getShareById(int id) {
        return shareDao.getShareById(id);
    }

    public Share getShareByName(String name) {
        return shareDao.getShareByName(name);
    }

    public List<MediaFile> getSharedFiles(int id, List<MusicFolder> musicFolders) {
        return shareDao.getSharedFiles(id, musicFolders).stream().map(mediaFileService::getMediaFile).filter(Objects::nonNull).collect(toList());
    }

    public Share createShare(HttpServletRequest request, List<MediaFile> files) {

        Share share = new Share();
        share.setName(RandomStringUtils.random(5, "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"));
        share.setCreated(Instant.now());
        share.setUsername(securityService.getCurrentUsername(request));

        share.setExpires(Instant.now().plus(ChronoUnit.YEARS.getDuration()));

        shareDao.createShare(share);
        shareDao.createSharedFiles(share.getId(), files.stream().map(f -> f.getId()).collect(toList()));
        LOG.info("Created share '{}' with {} file(s).", share.getName(), files.size());

        return share;
    }

    public void updateShare(Share share) {
        shareDao.updateShare(share);
    }

    public void deleteShare(int id) {
        shareDao.deleteShare(id);
    }

    public String getShareUrl(HttpServletRequest request, Share share) {
        String shareUrl = "ext/share/" + share.getName();
        return NetworkService.getBaseUrl(request) + jwtSecurityService
                .addJWTToken(User.USERNAME_ANONYMOUS, UriComponentsBuilder.fromUriString(shareUrl), share.getExpires())
                .build().toUriString();
    }

    public void setSecurityService(SecurityService securityService) {
        this.securityService = securityService;
    }

    public void setShareDao(ShareDao shareDao) {
        this.shareDao = shareDao;
    }

    public void setMediaFileService(MediaFileService mediaFileService) {
        this.mediaFileService = mediaFileService;
    }

    public void setJwtSecurityService(JWTSecurityService jwtSecurityService) {
        this.jwtSecurityService = jwtSecurityService;
    }
}
