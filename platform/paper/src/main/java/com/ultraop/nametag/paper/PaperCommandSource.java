package com.ultraop.nametag.paper;

import com.ultraop.nametag.api.CommandSource;
import org.bukkit.command.CommandSender;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class PaperCommandSource implements CommandSource {
    private final CommandSender sender;

    public PaperCommandSource(CommandSender sender) {
        this.sender = Objects.requireNonNull(sender, "sender");
    }

    @Override
    public String name() {
        return sender.getName();
    }

    @Override
    public Optional<UUID> playerUuid() {
        return sender instanceof Player player
                ? Optional.of(player.getUniqueId())
                : Optional.empty();
    }

    @Override
    public boolean hasPermission(String permission) {
        return sender.isOp() || sender.hasPermission(permission);
    }

    @Override
    public java.util.Collection<String> worldNames() {
        return Bukkit.getWorlds().stream()
                .map(world -> world.getName())
                .sorted()
                .toList();
    }

    @Override
    public void sendMessage(String message) {
        sender.sendMessage(message);
    }
}
