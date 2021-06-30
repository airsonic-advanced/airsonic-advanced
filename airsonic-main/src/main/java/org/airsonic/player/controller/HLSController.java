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

import com.auth0.jwt.interfaces.DecodedJWT;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.Player;
import org.airsonic.player.security.JWTAuthenticationToken;
import org.airsonic.player.service.JWTSecurityService;
import org.airsonic.player.service.MediaFileService;
import org.airsonic.player.service.PlayerService;
import org.airsonic.player.service.SecurityService;
import org.airsonic.player.service.hls.FFmpegHlsSession;
import org.airsonic.player.util.StringUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.awt.*;
import java.io.PrintWriter;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Controller which produces the HLS (Http Live Streaming) playlist.
 *
 * @author Sindre Mehus
 */
@Controller("hlsController")
@RequestMapping({"/hls/**", "/ext/hls/**"})
public class HLSController {

    private static final int SEGMENT_DURATION = 10;
    private static final Pattern BITRATE_PATTERN = Pattern.compile("(\\d+)(@(\\d+)x(\\d+))?");

    @Autowired
    private PlayerService playerService;
    @Autowired
    private MediaFileService mediaFileService;
    @Autowired
    private SecurityService securityService;
    @Autowired
    private JWTSecurityService jwtSecurityService;

    @GetMapping
    public void handleRequest(Authentication authentication,
            @RequestParam Integer id,
            HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        MediaFile mediaFile = mediaFileService.getMediaFile(id);
        Player player = playerService.getPlayer(request, response);
        String username = player.getUsername();

        if (mediaFile == null || mediaFile.isDirectory()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Media file not found or incorrect type: " + id);
            return;
        }

        if (!(authentication instanceof JWTAuthenticationToken)
                && !securityService.isFolderAccessAllowed(mediaFile, username)) {
            throw new AccessDeniedException("Access to file " + mediaFile.getId() + " is forbidden for user " + username);
        }

        Double duration = mediaFile.getDuration();
        if (duration == null || duration < 0.0001) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unknown duration for media file: " + id);
            return;
        }

        response.setContentType("application/vnd.apple.mpegurl");
        response.setCharacterEncoding(StringUtil.ENCODING_UTF8);
        List<Pair<Integer, Dimension>> bitRates = parseBitRates(request).stream()
                .map(b -> b.getRight() != null ? b
                        : Pair.of(b.getLeft(), FFmpegHlsSession.getSuitableVideoSize(mediaFile.getWidth(),
                                mediaFile.getHeight(), b.getLeft())))
                .collect(Collectors.toList());
        if (bitRates.isEmpty())
            bitRates = Collections.singletonList(
                    Pair.of(VideoPlayerController.DEFAULT_BIT_RATE, FFmpegHlsSession.getSuitableVideoSize(
                            mediaFile.getWidth(), mediaFile.getHeight(), VideoPlayerController.DEFAULT_BIT_RATE)));

        UriComponentsBuilder contextPath = UriComponentsBuilder.fromUriString(StringUtils.removeEndIgnoreCase(request.getRequestURI(), "hls/hls.m3u8"));
        PrintWriter writer = response.getWriter();
        if (bitRates.size() > 1) {
            generateVariantPlaylist(authentication, contextPath, id, player, bitRates, writer);
        } else {
            generateNormalPlaylist(authentication, contextPath, id, player, bitRates.get(0), Math.round(duration), writer);
        }

        return;
    }

    private List<Pair<Integer, Dimension>> parseBitRates(HttpServletRequest request) throws IllegalArgumentException {
        List<Pair<Integer, Dimension>> result = new ArrayList<>();
        String[] bitRates = request.getParameterValues("maxBitRate");
        String globalSize = request.getParameter("size");
        if (bitRates != null) {
            for (String bitRate : bitRates) {
                result.add(parseBitRate(bitRate, globalSize));
            }
        }
        return result;
    }

    /**
     * Parses a string containing the bitrate and an optional width/height, e.g., 1200@640x480
     */
    protected Pair<Integer, Dimension> parseBitRate(String bitRate, String defaultSize) throws IllegalArgumentException {

        Matcher matcher = BITRATE_PATTERN.matcher(bitRate);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid bitrate specification: " + bitRate);
        }
        int kbps = Integer.parseInt(matcher.group(1));
        if (matcher.group(3) == null) {
            if (defaultSize == null) {
                return Pair.of(kbps, null);
            }
            String[] dims = StringUtils.split(defaultSize, "x");
            int width = (Integer.parseInt(dims[0]) / 2) * 2;
            int height = (Integer.parseInt(dims[1]) / 2) * 2;
            return Pair.of(kbps, new Dimension(width, height));
        } else {
            int width = (Integer.parseInt(matcher.group(3)) / 2) * 2;
            int height = (Integer.parseInt(matcher.group(4)) / 2) * 2;
            return Pair.of(kbps, new Dimension(width, height));
        }
    }

    private void generateVariantPlaylist(Authentication authentication, UriComponentsBuilder contextPath, int id,
            Player player, List<Pair<Integer, Dimension>> bitRates, PrintWriter writer) {
        writer.println("#EXTM3U");
        writer.println("#EXT-X-VERSION:1");
//        writer.println("#EXT-X-TARGETDURATION:" + SEGMENT_DURATION);

        for (Pair<Integer, Dimension> bitRate : bitRates) {
            Integer kbps = bitRate.getLeft();
            int averageVideoBitRate = FFmpegHlsSession.getAverageVideoBitRate(kbps);
            writer.println("#EXT-X-STREAM-INF:PROGRAM-ID=1,BANDWIDTH=" + (kbps * 1000L) + ",AVERAGE-BANDWIDTH=" + (averageVideoBitRate * 1000L));
            UriComponentsBuilder url = contextPath.cloneBuilder()
                    .pathSegment("hls", "hls.m3u8")
                    .queryParam("id", id)
                    .queryParam("player", player.getId())
                    .queryParam("maxBitRate", kbps + Optional.ofNullable(bitRate.getRight()).map(d -> "@" + d.width + "x" + d.height).orElse(""));
            if (authentication instanceof JWTAuthenticationToken) {
                DecodedJWT token = (DecodedJWT) authentication.getDetails();
                Instant expires = Optional.ofNullable(token).map(x -> x.getExpiresAt()).map(x -> x.toInstant()).orElse(null);
                url = jwtSecurityService.addJWTToken(authentication.getName(), url, expires);
            }

            writer.print(url.toUriString());
            writer.println();
        }
//        writer.println("#EXT-X-ENDLIST");
    }

    private void generateNormalPlaylist(Authentication authentication, UriComponentsBuilder contextPath, int id,
            Player player, Pair<Integer, Dimension> bitRate, long totalDuration, PrintWriter writer) {
        writer.println("#EXTM3U");
        writer.println("#EXT-X-VERSION:1");
        writer.println("#EXT-X-TARGETDURATION:" + SEGMENT_DURATION);

        for (int i = 0; i < totalDuration / SEGMENT_DURATION; i++) {
            writer.println("#EXTINF:" + SEGMENT_DURATION + ",");
            writer.println(createStreamUrl(authentication, contextPath, player, id, i, SEGMENT_DURATION, bitRate));
        }

        long remainder = totalDuration % SEGMENT_DURATION;
        if (remainder > 0) {
            writer.println("#EXTINF:" + remainder + ",");
            writer.println(
                    createStreamUrl(authentication, contextPath, player, id, (int) (totalDuration / SEGMENT_DURATION),
                            remainder, bitRate));
        }
        writer.println("#EXT-X-ENDLIST");
    }

    private String createStreamUrl(Authentication authentication,
            UriComponentsBuilder contextPath,
            Player player,
            int id,
            int segmentIndex,
            long duration,
            Pair<Integer, Dimension> bitRate) {
        UriComponentsBuilder builder = contextPath.cloneBuilder()
                .pathSegment("segment", "segment.ts")
                .queryParam("id", id)
                .queryParam("hls", "true")
                .queryParam("segmentIndex", segmentIndex)
                .queryParam("player", player.getId())
                .queryParam("duration", duration)
                .queryParam("maxBitRate", bitRate.getLeft())
                .queryParam("size", bitRate.getRight().width + "x" + bitRate.getRight().height);

        if (authentication instanceof JWTAuthenticationToken) {
            DecodedJWT token = (DecodedJWT) authentication.getDetails();
            Instant expires = Optional.ofNullable(token).map(x -> x.getExpiresAt()).map(x -> x.toInstant()).orElse(null);
            builder = jwtSecurityService.addJWTToken(authentication.getName(), builder, expires);
        }
        return builder.toUriString();
    }

}
