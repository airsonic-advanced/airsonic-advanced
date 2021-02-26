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
package org.airsonic.player.service.metadata;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;

import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.service.SettingsService;
import org.airsonic.player.service.TranscodingService;
import org.airsonic.player.util.Util;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Parses meta data from video files using FFmpeg (http://ffmpeg.org/).
 * <p/>
 * Currently duration, bitrate and dimension are supported.
 *
 * @author Sindre Mehus
 */
@Service("ffmpegParser")
@Order(100)
public class FFmpegParser extends MetaDataParser {

    private static final Logger LOG = LoggerFactory.getLogger(FFmpegParser.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String[] FFPROBE_OPTIONS = {
        "-v", "quiet", "-print_format", "json", "-show_format", "-show_streams"
    };

    @Autowired
    private TranscodingService transcodingService;
    @Autowired
    private SettingsService settingsService;

    /**
     * Parses meta data for the given music file. No guessing or reformatting is done.
     *
     *
     * @param file The music file to parse.
     * @return Meta data for the file.
     */
    @Override
    public MetaData getRawMetaData(Path file) {

        MetaData metaData = new MetaData();

        try {
            // Use `ffprobe` in the transcode directory if it exists, otherwise let the system sort it out.
            String ffprobe;
            Path inTranscodeDirectory = Util.isWindows() ?
                transcodingService.getTranscodeDirectory().resolve("ffprobe.exe") :
                transcodingService.getTranscodeDirectory().resolve("ffprobe");
            if (Files.exists(inTranscodeDirectory)) {
                ffprobe = inTranscodeDirectory.toAbsolutePath().toString();
            } else {
                ffprobe = "ffprobe";
            }

            List<String> command = new ArrayList<>();
            command.add(ffprobe);
            command.addAll(Arrays.asList(FFPROBE_OPTIONS));
            command.add(file.toAbsolutePath().toString());

            Process process = Runtime.getRuntime().exec(command.toArray(new String[0]));
            final JsonNode result = objectMapper.readTree(process.getInputStream());

            metaData.setDuration(result.at("/format/duration").asDouble());
            // Bitrate is in Kb/s
            metaData.setBitRate(result.at("/format/bit_rate").asInt() / 1000);

            metaData.setAlbumArtist(getData(result, "album_artist"));
            metaData.setArtist(getData(result, "artist"));
            metaData.setAlbumName(getData(result, "album"));
            metaData.setGenre(getData(result, "genre"));
            metaData.setTitle(getData(result, "title"));

            String data = getData(result, "track");
            if (data != null) {
                metaData.setTrackNumber(Integer.valueOf(data));
            }

            data = getData(result, "date");
            if (data != null) {
                metaData.setYear(Integer.valueOf(data));
            }

            // Find the first (if any) stream that has dimensions and use those.
            // 'width' and 'height' are display dimensions; compare to 'coded_width', 'coded_height'.
            for (JsonNode stream : result.at("/streams")) {

                // skip coverart streams
                if (stream.hasNonNull("codec_name") && stream.get("codec_name").asText().equalsIgnoreCase("mjpeg")) {
                    continue;
                }
                if (stream.has("width") && stream.has("height")) {
                    metaData.setWidth(stream.get("width").asInt());
                    metaData.setHeight(stream.get("height").asInt());
                    break;
                }
            }
        } catch (Throwable x) {
            LOG.warn("Error when parsing metadata in {}", file, x);
        }

        return metaData;
    }

    private static String getData(JsonNode node, String keyName) {
        List<String> keys = ImmutableList.of("/tags/" + keyName, "/tags/" + keyName.toUpperCase(), "/tags/" + keyName.toLowerCase());
        Optional<String> nonNullKey = keys.stream().map(k -> node.at("/format" + k).asText()).filter(StringUtils::isNotBlank).findFirst();
        if (nonNullKey.isPresent()) {
            return nonNullKey.get();
        } else {
            for (JsonNode stream : node.at("/streams")) {
                // skip coverart streams
                if (stream.hasNonNull("codec_name") && stream.get("codec_name").asText().equalsIgnoreCase("mjpeg")) {
                    continue;
                }
                nonNullKey = keys.stream().map(k -> stream.at(k).asText()).filter(StringUtils::isNotBlank).findFirst();
                if (nonNullKey.isPresent()) {
                    return nonNullKey.get();
                }
            }
        }

        return null;
    }

    /**
     * Not supported.
     */
    @Override
    public void setMetaData(MediaFile file, MetaData metaData) {
        throw new RuntimeException("setMetaData() not supported in " + getClass().getSimpleName());
    }

    /**
     * Returns whether this parser supports tag editing (using the {@link #setMetaData} method).
     *
     * @return Always false.
     */
    @Override
    public boolean isEditingSupported() {
        return false;
    }

    @Override
    SettingsService getSettingsService() {
        return settingsService;
    }

    /**
     * Returns whether this parser is applicable to the given file.
     *
     * @param path The path to file in question.
     * @return Whether this parser is applicable to the given file.
     */
    @Override
    public boolean isApplicable(Path path) {
        return Files.isRegularFile(path);
    }

    public void setTranscodingService(TranscodingService transcodingService) {
        this.transcodingService = transcodingService;
    }

    public void setSettingsService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }
}
