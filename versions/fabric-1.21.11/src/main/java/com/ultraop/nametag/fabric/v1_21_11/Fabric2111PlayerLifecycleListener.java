package com.ultraop.nametag.fabric.v1_21_11;

import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Objects;

public final class Fabric2111PlayerLifecycleListener {
    private final Fabric2111NameplateRenderer renderer;

    public Fabric2111PlayerLifecycleListener(Fabric2111NameplateRenderer renderer) {
        this.renderer = Objects.requireNonNull(renderer, "renderer");
    }

    public void register() {
        ServerPlayerEvents.JOIN.register(this::onJoin);
        ServerPlayerEvents.LEAVE.register(this::onLeave);
    }

    private void onJoin(ServerPlayerEntity player) {
        renderer.refreshPlayer(player);
    }

    private void onLeave(ServerPlayerEntity player) {
        renderer.clearPlayer(player);
    }
}
