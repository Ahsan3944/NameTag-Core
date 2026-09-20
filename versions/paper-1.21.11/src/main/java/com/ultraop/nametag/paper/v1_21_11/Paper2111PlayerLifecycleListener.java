package com.ultraop.nametag.paper.v1_21_11;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.entity.Player;

import java.util.Objects;

/**
 * Keeps player nameplate state synchronized with Paper join/quit lifecycle.
 *
 * <p>The periodic renderer remains the source of truth for steady-state
 * rendering; these lifecycle hooks make join and quit transitions immediate.</p>
 */
public final class Paper2111PlayerLifecycleListener implements Listener {
    private final Paper2111NameplateRenderer renderer;

    public Paper2111PlayerLifecycleListener(Paper2111NameplateRenderer renderer) {
        this.renderer = Objects.requireNonNull(renderer, "renderer");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        renderer.refreshPlayer(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        renderer.clearPlayer(player);
    }
}
