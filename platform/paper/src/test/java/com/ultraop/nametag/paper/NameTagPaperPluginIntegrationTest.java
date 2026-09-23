package com.ultraop.nametag.paper;

import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.bukkit.scoreboard.Team;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NameTagPaperPluginIntegrationTest {
    private ServerMock server;
    private NameTagPaperPlugin plugin;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(NameTagPaperPlugin.class);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void pluginBootstrapsCommandsAndRendersAssignedTag() {
        assertTrue(plugin.isEnabled());
        assertNotNull(server.getPluginManager().getPlugin("NameTag-Core"));

        assertTrue(server.dispatchCommand(
                server.getConsoleSender(),
                "nametag create owner OWNER"
        ));

        var player = server.addPlayer("UltraOP");

        assertTrue(server.dispatchCommand(
                server.getConsoleSender(),
                "nametag player set UltraOP owner"
        ));

        server.getScheduler().performTicks(1);

        Team team = server.getScoreboardManager()
                .getMainScoreboard()
                .getEntryTeam(player.getName());

        assertNotNull(team);
        assertTrue(team.getName().startsWith("nametag_core_"));
    }

    @Test
    void playerRemoveTargetsOnlyTheSelectedTag() {
        assertTrue(server.dispatchCommand(
                server.getConsoleSender(),
                "nametag tag create owner OWNER"
        ));
        assertTrue(server.dispatchCommand(
                server.getConsoleSender(),
                "nametag tag create vip VIP"
        ));

        var player = server.addPlayer("UltraOP");
        assertTrue(server.dispatchCommand(
                server.getConsoleSender(),
                "nametag player set UltraOP owner"
        ));
        assertTrue(server.dispatchCommand(
                server.getConsoleSender(),
                "nametag player set UltraOP vip"
        ));

        assertTrue(server.dispatchCommand(
                server.getConsoleSender(),
                "nametag player remove UltraOP owner"
        ));

        assertNotNull(server.getScoreboardManager()
                .getMainScoreboard()
                .getEntryTeam(player.getName()));
    }

    @Test
    void quitLifecycleClearsRuntimeTeamMembershipWithoutDeletingAssignment() {
        assertTrue(server.dispatchCommand(
                server.getConsoleSender(),
                "nametag create owner OWNER"
        ));

        var player = server.addPlayer("UltraOP");

        assertTrue(server.dispatchCommand(
                server.getConsoleSender(),
                "nametag player set UltraOP owner"
        ));

        server.getScheduler().performTicks(1);

        assertNotNull(server.getScoreboardManager()
                .getMainScoreboard()
                .getEntryTeam(player.getName()));

        player.disconnect();

        assertNull(server.getScoreboardManager()
                .getMainScoreboard()
                .getEntryTeam(player.getName()));

        assertTrue(player.reconnect());
        var rejoined = server.getPlayerExact("UltraOP");
        assertNotNull(rejoined);

        // Join lifecycle must restore the nameplate without waiting for the periodic renderer.
        assertNotNull(server.getScoreboardManager()
                .getMainScoreboard()
                .getEntryTeam(rejoined.getName()));
    }
}
