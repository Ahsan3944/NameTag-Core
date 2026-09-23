package com.ultraop.nametag.fabric.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import com.ultraop.nametag.fabric.v1_21_11.network.NameTagItemPayload;

public final class NameTagFabricNetworking {
    private static boolean registered;

    private NameTagFabricNetworking() {}

    public static synchronized void register() {
        if (registered) return;
        PayloadTypeRegistry.playS2C().register(NameTagItemPayload.ID, NameTagItemPayload.CODEC);
        registered = true;
    }
}
