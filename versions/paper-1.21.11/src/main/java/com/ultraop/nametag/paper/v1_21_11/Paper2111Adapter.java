package com.ultraop.nametag.paper.v1_21_11;

import com.ultraop.nametag.api.TagService;
import org.bukkit.plugin.Plugin;

import java.util.Objects;

public final class Paper2111Adapter {
    private final TagService tagService;
    private final Paper2111NameplateRenderer renderer;

    public Paper2111Adapter(Plugin plugin, TagService tagService) {
        Objects.requireNonNull(plugin, "plugin");
        this.tagService = Objects.requireNonNull(tagService, "tagService");
        this.renderer = new Paper2111NameplateRenderer(plugin, tagService);
    }

    public TagService tagService() {
        return tagService;
    }

    public void start() {
        renderer.start();
    }

    public void stop() {
        renderer.stop();
    }
}
