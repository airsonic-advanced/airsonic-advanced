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
package org.airsonic.player.service.scrobbler;

import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.util.Util;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Provides services for "audioscrobbling" at listenbrainz.org.
 * <br/>
 * See https://listenbrainz.readthedocs.io/
 */
public class ListenBrainzScrobbler {

    private static final Logger LOG = LoggerFactory.getLogger(ListenBrainzScrobbler.class);
    private static final int MAX_PENDING_REGISTRATION = 2000;

    private RegistrationThread thread;
    private final LinkedBlockingQueue<RegistrationData> queue = new LinkedBlockingQueue<RegistrationData>();
    private final RequestConfig requestConfig = RequestConfig.custom()
            .setConnectTimeout(15000)
            .setSocketTimeout(15000)
            .build();

    /**
     * Registers the given media file at listenbrainz.org. This method returns
     * immediately, the actual registration is done by a separate thread.
     *
     * @param mediaFile  The media file to register.
     * @param url        The ListenBrainz URL (null for default)
     * @param token      The token to authentication user on ListenBrainz.
     * @param submission Whether this is a submission or a now playing notification.
     * @param time       Event time, or {@code null} to use current time.
     */
    public synchronized void register(MediaFile mediaFile, String url, String token, boolean submission, Instant time) {
        if (thread == null) {
            thread = new RegistrationThread();
            thread.start();
        }

        if (queue.size() >= MAX_PENDING_REGISTRATION) {
            LOG.warn("ListenBrainz scrobbler queue is full. Ignoring '{}'", mediaFile.getTitle());
            return;
        }

        RegistrationData registrationData = createRegistrationData(mediaFile, url, token, submission, time);
        if (registrationData == null) {
            return;
        }

        try {
            queue.put(registrationData);
        } catch (InterruptedException x) {
            LOG.warn("Interrupted while queuing ListenBrainz scrobble", x);
        }
    }

    private RegistrationData createRegistrationData(MediaFile mediaFile, String url, String token, boolean submission, Instant time) {
        RegistrationData reg = new RegistrationData();
        reg.url = url == null ? "https://api.listenbrainz.org/1/submit-listens" : url;
        reg.token = token;
        reg.artist = mediaFile.getArtist();
        reg.album = mediaFile.getAlbumName();
        reg.title = mediaFile.getTitle();
        reg.musicBrainzReleaseId = mediaFile.getMusicBrainzReleaseId();
        reg.musicBrainzRecordingId = mediaFile.getMusicBrainzRecordingId();
        reg.trackNumber = mediaFile.getTrackNumber();
        reg.duration = mediaFile.getDuration() == null ? 0 : (int) Math.round(mediaFile.getDuration());
        reg.time = time == null ? Instant.now() : time;
        reg.submission = submission;

        return reg;
    }

    /**
     * Scrobbles the given song data at listenbrainz.org, using the protocol defined at https://listenbrainz.readthedocs.io/en/latest/dev/api.html.
     *
     * @param registrationData Registration data for the song.
     */
    private void scrobble(RegistrationData registrationData) throws ClientProtocolException, IOException {
        if (registrationData == null || registrationData.token == null) {
            return;
        }

        if (!submit(registrationData)) {
            LOG.warn("Failed to scrobble song '{}' at ListenBrainz ({}).", registrationData.title, registrationData.url);
        } else {
            LOG.info("Successfully registered {} for song '{}' at ListenBrainz ({}): {}",
                    (registrationData.submission ? "submission" : "now playing"), registrationData.title,
                    registrationData.url, registrationData.time);
        }
    }
    /**
     * Returns if submission succeeds.
     */
    private boolean submit(RegistrationData registrationData) throws ClientProtocolException, IOException {
        Map<String, Object> additional_info = new HashMap<String, Object>();
        additional_info.computeIfAbsent("release_mbid", k -> registrationData.musicBrainzReleaseId);
        additional_info.computeIfAbsent("recording_mbid", k -> registrationData.musicBrainzRecordingId);
        additional_info.computeIfAbsent("tracknumber", k -> registrationData.trackNumber);

        Map<String, Object> track_metadata = new HashMap<String, Object>();
        if (additional_info.size() > 0) {
            track_metadata.put("additional_info", additional_info);
        }
        track_metadata.computeIfAbsent("artist_name", k -> registrationData.artist);
        track_metadata.computeIfAbsent("track_name", k -> registrationData.title);
        track_metadata.computeIfAbsent("release_name", k -> registrationData.album);

        Map<String, Object> payload = new HashMap<String, Object>();
        if (track_metadata.size() > 0) {
            payload.put("track_metadata", track_metadata);
        }

        Map<String, Object> content = new HashMap<String, Object>();

        if (registrationData.submission) {
            payload.put("listened_at", Long.valueOf(registrationData.time.getEpochSecond()));
            content.put("listen_type", "single");
        } else {
            content.put("listen_type", "playing_now");
        }

        List<Map<String, Object>> payloads = new ArrayList<Map<String, Object>>();
        payloads.add(payload);
        content.put("payload", payloads);

        String json = Util.toJson(content);

        return executeJsonPostRequest(registrationData.url, registrationData.token, json);
    }

    private boolean executeJsonPostRequest(String url, String token, String json) throws ClientProtocolException, IOException {
        HttpPost request = new HttpPost(url);
        request.setEntity(new StringEntity(json, "UTF-8"));
        request.setHeader("Authorization", "token " + token);
        request.setHeader("Content-type", "application/json; charset=utf-8");

        return executeRequest(request);
    }

    private boolean executeRequest(HttpUriRequest request) throws ClientProtocolException, IOException {
        try (CloseableHttpClient client = HttpClients.createDefault();
                CloseableHttpResponse resp = client.execute(request);) {
            boolean ok = resp.getStatusLine().getStatusCode() == 200;
            if (!ok) {
                LOG.warn("Failed to execute ListenBrainz request: {}", resp.getEntity().toString());
            }

            return ok;
        }
    }

    private class RegistrationThread extends Thread {
        private RegistrationThread() {
            super("ListenBrainzScrobbler Registration");
        }

        @Override
        public void run() {
            while (true) {
                RegistrationData registrationData = null;
                try {
                    registrationData = queue.take();
                    scrobble(registrationData);
                } catch (ClientProtocolException x) {
                } catch (IOException x) {
                    handleNetworkError(registrationData, x);
                } catch (Exception x) {
                    LOG.warn("Error in ListenBrainz registration: " + x.toString());
                }
            }
        }

        private void handleNetworkError(RegistrationData registrationData, Exception error) {
            try {
                queue.put(registrationData);
                LOG.info("ListenBrainz registration for '{}' encountered network error. Will try again later. In queue: {}", registrationData.title, queue.size(), error);
            } catch (InterruptedException x) {
                LOG.error("Failed to reschedule ListenBrainz registration for '{}'", registrationData.title, x);
            }
            try {
                sleep(60L * 1000L);  // Wait 60 seconds.
            } catch (InterruptedException x) {
                LOG.error("Failed to sleep after ListenBrainz registration failure for '{}'", registrationData.title, x);
            }
        }
    }

    private static class RegistrationData {
        private String url;
        private String token;
        private String artist;
        private String album;
        private String title;
        private String musicBrainzReleaseId;
        private String musicBrainzRecordingId;
        private Integer trackNumber;
        private int duration;
        private Instant time;
        public boolean submission;
    }

}
