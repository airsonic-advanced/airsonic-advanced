package org.airsonic.player.dao;

import org.airsonic.player.domain.PlayQueue;
import org.airsonic.player.domain.Player;
import org.airsonic.player.domain.PlayerTechnology;
import org.airsonic.player.domain.TranscodeScheme;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;

/**
 * Unit test of {@link PlayerDao}.
 *
 * @author Sindre Mehus
 */
public class PlayerDaoTestCase extends DaoTestCaseBean2 {

    @Autowired
    PlayerDao playerDao;

    @Before
    public void setUp() {
        getJdbcTemplate().execute("delete from player");
    }

    @Test
    public void testCreatePlayer() {
        Player player = new Player();
        player.setName("name");
        player.setType("type");
        player.setUsername("username");
        player.setIpAddress("ipaddress");
        player.setDynamicIp(false);
        player.setAutoControlEnabled(false);
        player.setTechnology(PlayerTechnology.EXTERNAL_WITH_PLAYLIST);
        player.setClientId("android");
        player.setLastSeen(Instant.now());
        player.setTranscodeScheme(TranscodeScheme.MAX_160);

        playerDao.createPlayer(player);
        Player newPlayer = playerDao.getAllPlayers().get(0);
        assertPlayerEquals(player, newPlayer);

        Player newPlayer2 = playerDao.getPlayerById(newPlayer.getId());
        assertPlayerEquals(player, newPlayer2);
    }

    @Test
    public void testDefaultValues() {
        playerDao.createPlayer(new Player());
        Player player = playerDao.getAllPlayers().get(0);

        assertTrue("Player should have dynamic IP by default.", player.getDynamicIp());
        assertTrue("Player should be auto-controlled by default.", player.getAutoControlEnabled());
        assertNull("Player client ID should be null by default.", player.getClientId());
    }

    @Test
    public void testIdentity() {
        Player player = new Player();

        playerDao.createPlayer(player);
        Integer playerId1 = player.getId();
        assertEquals("Wrong number of players.", 1, playerDao.getAllPlayers().size());

        playerDao.createPlayer(player);
        Integer playerId2 = player.getId();
        assertNotEquals("Wrong ID", playerId1, playerId2);
        assertEquals("Wrong number of players.", 2, playerDao.getAllPlayers().size());

        playerDao.deletePlayer(playerId1);
        playerDao.createPlayer(player);
        assertNotEquals("Wrong ID", playerId1, player.getId());
        assertEquals("Wrong number of players.", 2, playerDao.getAllPlayers().size());
    }

    @Test
    public void testPlaylist() {
        Player player = new Player();
        playerDao.createPlayer(player);
        PlayQueue playQueue = player.getPlayQueue();
        assertNotNull("Missing playlist.", playQueue);

        playerDao.deletePlayer(player.getId());
        playerDao.createPlayer(player);
        assertNotSame("Wrong playlist.", playQueue, player.getPlayQueue());
    }

    @Test
    public void testGetPlayersForUserAndClientId() {
        Player player = new Player();
        player.setUsername("sindre");
        playerDao.createPlayer(player);
        player = playerDao.getAllPlayers().get(0);

        List<Player> players = playerDao.getPlayersForUserAndClientId("sindre", null);
        assertFalse("Error in getPlayersForUserAndClientId().", players.isEmpty());
        assertPlayerEquals(player, players.get(0));
        assertTrue("Error in getPlayersForUserAndClientId().", playerDao.getPlayersForUserAndClientId("sindre", "foo").isEmpty());

        player.setClientId("foo");
        playerDao.updatePlayer(player);

        players = playerDao.getPlayersForUserAndClientId("sindre", null);
        assertTrue("Error in getPlayersForUserAndClientId().", players.isEmpty());
        players = playerDao.getPlayersForUserAndClientId("sindre", "foo");
        assertFalse("Error in getPlayersForUserAndClientId().", players.isEmpty());
        assertPlayerEquals(player, players.get(0));
    }

    @Test
    public void testUpdatePlayer() {
        Player player = new Player();
        playerDao.createPlayer(player);
        assertPlayerEquals(player, playerDao.getAllPlayers().get(0));

        player.setName("name");
        player.setType("Winamp");
        player.setTechnology(PlayerTechnology.WEB);
        player.setClientId("foo");
        player.setUsername("username");
        player.setIpAddress("ipaddress");
        player.setDynamicIp(true);
        player.setAutoControlEnabled(false);
        player.setLastSeen(Instant.now());
        player.setTranscodeScheme(TranscodeScheme.MAX_160);

        playerDao.updatePlayer(player);
        Player newPlayer = playerDao.getAllPlayers().get(0);
        assertPlayerEquals(player, newPlayer);
    }

    @Test
    public void testDeletePlayer() {
        assertEquals("Wrong number of players.", 0, playerDao.getAllPlayers().size());

        Player p1 = new Player();
        playerDao.createPlayer(p1);
        assertEquals("Wrong number of players.", 1, playerDao.getAllPlayers().size());

        Player p2 = new Player();
        playerDao.createPlayer(p2);
        assertEquals("Wrong number of players.", 2, playerDao.getAllPlayers().size());

        playerDao.deletePlayer(p1.getId());
        assertEquals("Wrong number of players.", 1, playerDao.getAllPlayers().size());

        playerDao.deletePlayer(p2.getId());
        assertEquals("Wrong number of players.", 0, playerDao.getAllPlayers().size());
    }

    private void assertPlayerEquals(Player expected, Player actual) {
        assertThat(expected).isEqualToComparingFieldByField(actual);
    }
}
