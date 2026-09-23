package com.ultraop.nametag.fabric;

import com.ultraop.nametag.fabric.client.NameTagFabricClient;
import com.ultraop.nametag.fabric.network.NameTagFabricNetworking;
import net.fabricmc.api.ClientModInitializer;

public final class NameTagFabricClientEntrypoint implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        NameTagFabricNetworking.register();
        NameTagFabricClient.register();
    }
}
