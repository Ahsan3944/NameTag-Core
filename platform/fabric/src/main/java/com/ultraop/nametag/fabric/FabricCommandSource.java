package com.ultraop.nametag.fabric;

import com.ultraop.nametag.api.CommandSource;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class FabricCommandSource implements CommandSource {
    private final ServerCommandSource source;

    public FabricCommandSource(ServerCommandSource source) {
        this.source = Objects.requireNonNull(source, "source");
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
        // The native /nametag root command already enforces the gamemaster permission level.
        return true;
    }

    @Override
    public void sendMessage(String message) {
        source.sendFeedback(() -> Text.literal(message), false);
    }
}
