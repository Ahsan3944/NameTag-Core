package com.ultraop.nametag.fabric.v1_21_11;

import com.ultraop.nametag.api.TagService;
import net.minecraft.server.MinecraftServer;

import java.util.Objects;

public final class Fabric2111Adapter {
    private final Fabric2111NameplateRenderer renderer;

    public Fabric2111Adapter(TagService tagService) {
        Objects.requireNonNull(tagService, "tagService");
        this.renderer = new Fabric2111NameplateRenderer(tagService);
    }

    public void stop(MinecraftServer server) {
        renderer.stop(server);
    }
}
