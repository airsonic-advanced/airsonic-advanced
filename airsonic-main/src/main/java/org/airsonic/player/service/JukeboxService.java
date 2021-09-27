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

import org.airsonic.player.domain.Player;
import org.airsonic.player.domain.PlayerTechnology;
import org.springframework.stereotype.Service;

/**
 * @author Rémi Cocula
 */
@Service
public class JukeboxService {

    private JukeboxLegacySubsonicService jukeboxLegacySubsonicService;

    public JukeboxService(JukeboxLegacySubsonicService jukeboxLegacySubsonicService) {
        this.jukeboxLegacySubsonicService = jukeboxLegacySubsonicService;
    }

    public void setGain(Player airsonicPlayer, float gain) {
        switch (airsonicPlayer.getTechnology()) {
            case JUKEBOX:
                jukeboxLegacySubsonicService.setGain(gain);
                break;
        }
    }

    public void setPosition(Player airsonicPlayer, int positionInSeconds) {
        switch (airsonicPlayer.getTechnology()) {
            case JUKEBOX:
                throw new UnsupportedOperationException();
        }
    }

    public float getGain(Player airsonicPlayer) {
        switch (airsonicPlayer.getTechnology()) {
            case JUKEBOX:
                return jukeboxLegacySubsonicService.getGain();
        }
        return 0;
    }

    /**
     * This method should be removed when the jukebox is controlled only through rest api.
     */
    @Deprecated
    public void updateJukebox(Player airsonicPlayer, int offset) {
        if (airsonicPlayer.getTechnology().equals(PlayerTechnology.JUKEBOX)) {
            jukeboxLegacySubsonicService.updateJukebox(airsonicPlayer,offset);
        }
    }

    public int getPosition(Player airsonicPlayer) {
        switch (airsonicPlayer.getTechnology()) {
            case JUKEBOX:
                return jukeboxLegacySubsonicService.getPosition();
        }
        return 0;
    }

    /**
     * Plays the playQueue of a jukebox player starting at the first item of the queue.
     */
    public void play(Player airsonicPlayer) {
        switch (airsonicPlayer.getTechnology()) {
            case JUKEBOX:
                jukeboxLegacySubsonicService.updateJukebox(airsonicPlayer,0);
                break;
        }
    }

    public void start(Player airsonicPlayer) {
        switch (airsonicPlayer.getTechnology()) {
            case JUKEBOX:
                jukeboxLegacySubsonicService.updateJukebox(airsonicPlayer,0);
                break;
        }
    }

    public void stop(Player airsonicPlayer) {
        switch (airsonicPlayer.getTechnology()) {
            case JUKEBOX:
                jukeboxLegacySubsonicService.updateJukebox(airsonicPlayer,0);
                break;
        }
    }

    public void skip(Player airsonicPlayer,int index,int offset) {
        switch (airsonicPlayer.getTechnology()) {
            case JUKEBOX:
                jukeboxLegacySubsonicService.updateJukebox(airsonicPlayer,offset);
                break;
        }
    }

    /**
     * This method is only here due to legacy considerations and should be removed
     * if the jukeboxLegacySubsonicService is removed.
     */
    @Deprecated
    public boolean canControl(Player airsonicPlayer) {
        switch (airsonicPlayer.getTechnology()) {
            case JUKEBOX:
                if (jukeboxLegacySubsonicService.getPlayer() == null) {
                    return false;
                } else {
                    return jukeboxLegacySubsonicService.getPlayer().getId().equals(airsonicPlayer.getId());
                }
        }
        return false;
    }
}
