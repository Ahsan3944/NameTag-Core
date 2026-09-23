package com.ultraop.nametag.fabric.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public final class NameTagFabricNetworking {
    private static boolean registered;

    private NameTagFabricNetworking() {}

    public static synchronized void register() {
        if (registered) return;
        PayloadTypeRegistry.playS2C().register(NameTagItemPayload.TYPE, NameTagItemPayload.CODEC);
        registered = true;
    }
}
