package com.ultraop.nametag.paper.v1_21_11;

import com.ultraop.nametag.api.ConfigurationService;
import com.ultraop.nametag.api.TagService;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;

import java.util.Objects;

public final class Paper2111Adapter {
    private final Plugin plugin;
    private final TagService tagService;
    private final ConfigurationService configuration;
    private final Paper2111NameplateRenderer renderer;
    private final Paper2111ChatListener chatListener;

    public Paper2111Adapter(
            Plugin plugin,
            TagService tagService,
            ConfigurationService configuration
    ) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.tagService = Objects.requireNonNull(tagService, "tagService");
        this.configuration = Objects.requireNonNull(configuration, "configuration");
        this.renderer = new Paper2111NameplateRenderer(plugin, tagService);
        this.chatListener = new Paper2111ChatListener(tagService, configuration);
    }

    public TagService tagService() {
        return tagService;
    }

    public void start() {
        renderer.start();
        plugin.getServer().getPluginManager().registerEvents(chatListener, plugin);
    }

    public void stop() {
        HandlerList.unregisterAll(chatListener);
        renderer.stop();
    }
}
