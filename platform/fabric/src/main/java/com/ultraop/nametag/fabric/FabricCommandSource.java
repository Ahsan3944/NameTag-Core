package com.ultraop.nametag.fabric;

import com.ultraop.nametag.api.CommandSource;
import com.ultraop.nametag.api.PermissionService;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class FabricCommandSource implements CommandSource {
    private final ServerCommandSource source;
    private final PermissionService permissions;

    public FabricCommandSource(ServerCommandSource source, PermissionService permissions) {
        this.source = Objects.requireNonNull(source, "source");
        this.permissions = Objects.requireNonNull(permissions, "permissions");
    }

    @Override
    public String name() {
        return source.getName();
    }

    @Override
    public Optional<UUID> playerUuid() {
        return Optional.ofNullable(source.getPlayer()).map(player -> player.getUuid());
    }

    @Override
    public boolean hasPermission(String permission) {
        UUID playerUuid = playerUuid().orElse(null);
        if (playerUuid != null && permissions.has(playerUuid, permission)) {
            return true;
        }
        return playerUuid == null;
    }

    @Override
    @Override
    public Collection<String> worldNames() {
        return source.getWorldKeys().stream()
                .map(key -> key.getValue().toString())
                .sorted()
                .toList();
    }

    @Override
    public void sendMessage(String message) {
        source.sendFeedback(() -> Text.literal(message), false);
    }
}
