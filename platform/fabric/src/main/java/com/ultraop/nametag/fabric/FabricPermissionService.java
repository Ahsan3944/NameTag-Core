package com.ultraop.nametag.fabric;

import com.ultraop.nametag.api.PermissionService;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import java.util.UUID;

public final class FabricPermissionService implements PermissionService {
    private volatile MinecraftServer server;

    public FabricPermissionService() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> this.server = server);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            if (this.server == server) this.server = null;
        });
    }

    @Override
    public boolean has(UUID playerUuid, String permission) {
        MinecraftServer current = server;
        if (current == null) return false;
        ServerPlayerEntity player = current.getPlayerManager().getPlayer(playerUuid);
        if (player == null) return false;
        if (current.getPlayerManager().isOperator(player.getPlayerConfigEntry())) return true;
        if (!FabricLoader.getInstance().isModLoaded("luckperms")) return false;
        try {
            var user = net.luckperms.api.LuckPermsProvider.get().getUserManager().getUser(playerUuid);
            return user != null && user.getCachedData().getPermissionData().checkPermission(permission).asBoolean();
        } catch (IllegalStateException | LinkageError ignored) {
            return false;
        }
    }
}
