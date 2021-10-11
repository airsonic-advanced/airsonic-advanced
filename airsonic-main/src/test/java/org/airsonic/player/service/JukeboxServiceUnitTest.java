package org.airsonic.player.service;

import org.airsonic.player.domain.Player;
import org.airsonic.player.domain.PlayerTechnology;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class JukeboxServiceUnitTest {

    private JukeboxService jukeboxService;
    @Mock
    private JukeboxLegacySubsonicService jukeboxLegacySubsonicService;
    private Player legacyJukeboxPlayer;
    private Player nonJukeboxPlayer;

    @Before
    public void setUp() {
        jukeboxService = new JukeboxService(jukeboxLegacySubsonicService);
        legacyJukeboxPlayer = generateLegacyJukeboxPlayer();
        nonJukeboxPlayer = generateNonJukeboxPlayer();
    }

    private Player generateNonJukeboxPlayer() {
        Player player = new Player();
        player.setId(0);
        player.setTechnology(PlayerTechnology.WEB);
        return player;
    }

    private Player generateLegacyJukeboxPlayer() {
        Player player = new Player();
        player.setId(1);
        player.setTechnology(PlayerTechnology.JUKEBOX);
        return player;
    }

    @Test(expected = UnsupportedOperationException.class)
    public void setPositionWithLegacyJukeboxPlayer() {
        // When
        jukeboxService.setPosition(legacyJukeboxPlayer, 0);
    }

    @Test
    public void getGainWithLegacyJukeboxPlayer() {
        // When
        jukeboxService.getGain(legacyJukeboxPlayer);
        // Then
        verify(jukeboxLegacySubsonicService).getGain();
    }

    @Test
    public void getGainWithNonJukeboxPlayer() {
        // When
        float gain = jukeboxService.getGain(nonJukeboxPlayer);
        // Then
        assertThat(gain).isEqualTo(0);
    }

    @Test
    public void updateJukebox() {
        // When
        jukeboxService.updateJukebox(legacyJukeboxPlayer, 0);
        // Then
        verify(jukeboxLegacySubsonicService).updateJukebox(legacyJukeboxPlayer, 0);
    }

    @Test
    public void getPositionWithLegacyJukeboxPlayer() {
        // When
        jukeboxService.getPosition(legacyJukeboxPlayer);
        // Then
        verify(jukeboxLegacySubsonicService).getPosition();
    }

    @Test
    public void getPositionWithNonJukeboxPlayer() {
        // When
        int position = jukeboxService.getPosition(nonJukeboxPlayer);
        // Then
        assertThat(position).isEqualTo(0);
    }

    @Test
    public void setGainWithLegacyJukeboxPlayer() {
        // When
        jukeboxService.setGain(legacyJukeboxPlayer, 0.5f);
        // Then
        verify(jukeboxLegacySubsonicService).setGain(0.5f);
    }

    @Test
    public void startWithLegacyJukeboxPlayer() {
        // When
        jukeboxService.start(legacyJukeboxPlayer);

        // Then
        verify(jukeboxLegacySubsonicService).updateJukebox(legacyJukeboxPlayer, 0);
    }

    @Test
    public void playWithLegacyJukeboxPlayer() {
        // When
        jukeboxService.play(legacyJukeboxPlayer);
        // Then
        verify(jukeboxLegacySubsonicService).updateJukebox(legacyJukeboxPlayer, 0);
    }

    @Test
    public void stopWithLegacyJukeboxPlayer() {
        // When
        jukeboxService.stop(legacyJukeboxPlayer);
        // Then
        verify(jukeboxLegacySubsonicService).updateJukebox(legacyJukeboxPlayer, 0);
    }

    @Test
    public void skipWithLegacyJukeboxPlayer() {
        // When
        jukeboxService.skip(legacyJukeboxPlayer, 0, 1);
        // Then
        verify(jukeboxLegacySubsonicService).updateJukebox(legacyJukeboxPlayer, 1);
    }

    @Test
    public void canControlWithLegacyJukeboxPlayer() {
        // When
        when(jukeboxLegacySubsonicService.getPlayer()).thenReturn(legacyJukeboxPlayer);
        boolean canControl = jukeboxService.canControl(legacyJukeboxPlayer);
        // Then
        assertThat(canControl).isEqualTo(true);
    }

    @Test
    public void canControlWithLegacyJukeboxPlayerWrongPlayer() {
        // When
        when(jukeboxLegacySubsonicService.getPlayer()).thenReturn(nonJukeboxPlayer);
        boolean canControl = jukeboxService.canControl(legacyJukeboxPlayer);
        // Then
        assertThat(canControl).isEqualTo(false);
    }
}