package com.ultraop.nametag.fabric;
import net.fabricmc.api.DedicatedServerModInitializer;
public final class NameTagFabricEntrypoint implements DedicatedServerModInitializer {
    @Override public void onInitializeServer() { NameTagFabric.bootstrap(); }
}
