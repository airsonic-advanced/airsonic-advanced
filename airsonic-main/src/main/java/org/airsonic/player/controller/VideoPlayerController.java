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
package org.airsonic.player.controller;

import com.google.common.collect.ImmutableSet;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.User;
import org.airsonic.player.service.MediaFileService;
import org.airsonic.player.service.NetworkService;
import org.airsonic.player.service.PlayerService;
import org.airsonic.player.service.SecurityService;
import org.airsonic.player.service.metadata.MetaData;
import org.airsonic.player.service.metadata.MetaDataParser;
import org.airsonic.player.service.metadata.MetaDataParserFactory;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Controller for the page used to play videos.
 *
 * @author Sindre Mehus
 */
@Controller
@RequestMapping("/videoPlayer")
public class VideoPlayerController {

    public static final int DEFAULT_BIT_RATE = 2000;
    public static final int[] BIT_RATES = {200, 300, 400, 500, 700, 1000, 1200, 1500, 2000, 3000, 5000};
    private static Set<String> NONSTREAMABLE_FORMATS = ImmutableSet.of("mp4", "m4v");
    private static Set<String> CASTABLE_FORMATS = ImmutableSet.of("mp4", "m4v", "mkv");

    @Autowired
    private MediaFileService mediaFileService;
    @Autowired
    private PlayerService playerService;
    @Autowired
    private SecurityService securityService;
    @Autowired
    private MetaDataParserFactory metaDataParserFactory;
    @Autowired
    private CaptionsController captionsController;

    @GetMapping
    protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) throws Exception {

        User user = securityService.getCurrentUser(request);
        Map<String, Object> map = new HashMap<String, Object>();
        int id = ServletRequestUtils.getRequiredIntParameter(request, "id");
        MediaFile file = mediaFileService.getMediaFile(id);
        mediaFileService.populateStarredDate(file, user.getUsername());

        Integer playerId = playerService.getPlayer(request, response).getId();
        String url = NetworkService.getBaseUrl(request);
        String streamUrl = url + "stream?id=" + file.getId() + "&player=" + playerId + "&format=mp4";
        String coverArtUrl = url + "coverArt.view?id=" + file.getId();
        boolean streamable = isStreamable(file);
        boolean castable = isCastable(file);
        List<CaptionsController.CaptionInfo> captions = captionsController.listCaptions(file);

        map.put("video", file);
        map.put("streamable", streamable);
        map.put("castable", castable);
        map.put("captions", captions);
        map.put("streamUrl", streamUrl);
        map.put("remoteStreamUrl", streamUrl);
        map.put("remoteCoverArtUrl", coverArtUrl);
        map.put("bitRates", BIT_RATES);
        map.put("defaultBitRate", DEFAULT_BIT_RATE);
        map.put("user", user);

        return new ModelAndView("videoPlayer", "model", map);
    }

    public MetaData getVideoMetaData(MediaFile video) {
        MetaDataParser parser = this.metaDataParserFactory.getParser(video.getFile());
        return (parser != null) ? parser.getMetaData(video.getFile()) : null;
    }

    public boolean isStreamable(MediaFile file) {
        if (NONSTREAMABLE_FORMATS.contains(StringUtils.lowerCase(file.getFormat())))
            return false;
        MetaData metaData = getVideoMetaData(file);
        if (metaData == null)
            return true;
        if (!metaData.getVideoTracks().isEmpty() && !(metaData.getVideoTracks().get(0)).isStreamable())
            return false;
        if (!metaData.getAudioTracks().isEmpty() && !(metaData.getAudioTracks().get(0)).isStreamable())
            return false;
        return true;
    }

    public static boolean isCastable(MediaFile file) {
        return CASTABLE_FORMATS.contains(StringUtils.lowerCase(file.getFormat()));
    }

}
