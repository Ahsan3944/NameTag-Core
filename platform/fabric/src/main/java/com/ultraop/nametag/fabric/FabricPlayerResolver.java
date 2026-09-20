package com.ultraop.nametag.fabric;

import com.ultraop.nametag.api.OnlinePlayer;
import com.ultraop.nametag.api.PlayerResolver;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public final class FabricPlayerResolver implements PlayerResolver {
    private final MinecraftServer server;

    public FabricPlayerResolver(MinecraftServer server) {
        this.server = server;
    }

    @Override
    public Optional<OnlinePlayer> findOnline(String name) {
        ServerPlayerEntity player = server.getPlayerManager().getPlayer(name);
        return player == null
                ? Optional.empty()
                : Optional.of(new OnlinePlayer(player.getUuid(), player.getName().getString()));
    }

    @Override
    public Collection<OnlinePlayer> onlinePlayers() {
        return server.getPlayerManager().getPlayerList().stream()
                .map(player -> new OnlinePlayer(player.getUuid(), player.getName().getString()))
                .toList();
    }
}
