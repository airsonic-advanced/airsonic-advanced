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
import org.airsonic.player.domain.UserCredential;
import org.airsonic.player.domain.UserCredential.App;
import org.airsonic.player.security.GlobalSecurityConfig;
import org.airsonic.player.security.PasswordDecoder;
import org.airsonic.player.domain.UserSettings;
import org.airsonic.player.service.scrobbler.LastFMScrobbler;
import org.airsonic.player.service.scrobbler.ListenBrainzScrobbler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

/**
 * Provides services for "audioscrobbling", which is the process of
 * registering what songs are played at website.
 */
@Service
public class AudioScrobblerService {

    private LastFMScrobbler lastFMScrobbler;
    private ListenBrainzScrobbler listenBrainzScrobbler;
    @Autowired
    private SettingsService settingsService;
    @Autowired
    private SecurityService securityService;

    private static final UserCredential getDecodableCredsForLocation(List<UserCredential> creds, App location) {
        return creds.parallelStream()
                .filter(c -> c.getLocation() == location)
                .filter(c -> GlobalSecurityConfig.DECODABLE_ENCODERS.contains(c.getType()))
                .sorted(Comparator.comparing(c -> c.getUpdated(), Comparator.reverseOrder()))
                .findFirst().orElse(null);
    }

    private static final String decode(UserCredential uc) {
        PasswordDecoder decoder = (PasswordDecoder) GlobalSecurityConfig.ENCODERS.get(uc.getType());
        return decoder.decode(uc.getCredential());
    }

    /**
     * Registers the given media file at audio scrobble service.
     * This method returns immediately, the actual registration is done by separate threads.
     *
     * @param mediaFile  The media file to register.
     * @param username   The user which played the music file.
     * @param submission Whether this is a submission or a now playing notification.
     * @param time       Event time, or {@code null} to use current time.
     */

    public synchronized void register(MediaFile mediaFile, String username, boolean submission, Instant time) {
        if (mediaFile == null || mediaFile.isVideo()) {
            return;
        }

        UserSettings userSettings = settingsService.getUserSettings(username);
        List<UserCredential> creds = securityService.getCredentials(username, App.LASTFM, App.LISTENBRAINZ);

        if (userSettings.isLastFmEnabled() ) {
            if (lastFMScrobbler == null) {
                lastFMScrobbler = new LastFMScrobbler();
            }
            UserCredential cred = getDecodableCredsForLocation(creds, App.LASTFM);
            if (cred != null) {
                String decoded = decode(cred);
                if (decoded != null) {
                    lastFMScrobbler.register(mediaFile, cred.getLocationUsername(), cred.getCredential(), submission, time);
                }
            }
        }

        if (userSettings.isListenBrainzEnabled()) {
            if (listenBrainzScrobbler == null) {
                listenBrainzScrobbler = new ListenBrainzScrobbler();
            }
            UserCredential cred = getDecodableCredsForLocation(creds, App.LISTENBRAINZ);
            if (cred != null) {
                String decoded = decode(cred);
                if (decoded != null) {
                    listenBrainzScrobbler.register(mediaFile, cred.getCredential(), submission, time);
                }
            }
        }
    }

    public void setSettingsService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }
}
