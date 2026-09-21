package com.ultraop.nametag.fabric.v1_21_11;

import com.ultraop.nametag.api.ConfigurationService;
import com.ultraop.nametag.api.TagService;
import net.minecraft.server.MinecraftServer;

import java.util.Objects;

public final class Fabric2111Adapter {
    private final Fabric2111NameplateRenderer renderer;
    private final Fabric2111ChatRenderer chatRenderer;

    public Fabric2111Adapter(TagService tagService, ConfigurationService configuration) {
        Objects.requireNonNull(tagService, "tagService");
        Objects.requireNonNull(configuration, "configuration");
        this.renderer = new Fabric2111NameplateRenderer(tagService);
        this.chatRenderer = new Fabric2111ChatRenderer(tagService, configuration);
        this.chatRenderer.register();
    }

    public void stop(MinecraftServer server) {
        renderer.stop(server);
    }
}
