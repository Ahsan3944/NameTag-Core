package com.ultraop.nametag.paper.v1_21_11;

import com.ultraop.nametag.api.ConfigurationService;
import com.ultraop.nametag.api.TagService;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.Objects;

/**
 * Installs the NameTag chat renderer on Paper's modern AsyncChatEvent.
 *
 * <p>The permission check is intentionally performed before installing the
 * renderer. NameTag-Core does not cancel or replace chat when the player lacks
 * {@code nametag.chat}; vanilla/other-plugin rendering remains untouched.</p>
 */
public final class Paper2111ChatListener implements Listener {
    private final TagService tagService;
    private final ConfigurationService configuration;
    private final Paper2111ChatRenderer renderer;

    public Paper2111ChatListener(TagService tagService, ConfigurationService configuration) {
        this.tagService = Objects.requireNonNull(tagService, "tagService");
        this.configuration = Objects.requireNonNull(configuration, "configuration");
        this.renderer = new Paper2111ChatRenderer(tagService, configuration);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        if (!configuration.current().chatEnabled()) {
            return;
        }

        Player player = event.getPlayer();
        if (!player.hasPermission("nametag.chat")) {
            return;
        }

        if (tagService.activeTag(player.getUniqueId())
                .filter(tag -> tag.enabled() && tag.chatEnabled())
                .isEmpty()) {
            return;
        }

        event.renderer(io.papermc.paper.chat.ChatRenderer.viewerUnaware(renderer));
    }
}
