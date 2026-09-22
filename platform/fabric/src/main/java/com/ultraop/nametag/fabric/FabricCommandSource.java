package com.ultraop.nametag.fabric;

import com.ultraop.nametag.api.CommandSource;
import com.ultraop.nametag.api.PermissionService;
import net.minecraft.registry.Registries;
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
    public Collection<String> worldNames() {
        return source.getWorldKeys().stream()
                .map(key -> key.getValue().toString())
                .sorted()
                .toList();
    }

    @Override
    public Collection<String> itemNames() {
        return Registries.ITEM.getIds().stream()
                .map(Object::toString)
                .sorted()
                .toList();
    }

    @Override
    public void sendMessage(String message) {
        source.sendFeedback(() -> Text.literal(message), false);
    }

    @Override
    public void sendStyledMessage(String message) {
        source.sendFeedback(() -> parseLegacyFormatting(message), false);
    }

    private static Text parseLegacyFormatting(String message) {
        net.minecraft.text.MutableText result = Text.empty();
        net.minecraft.text.Style style = net.minecraft.text.Style.EMPTY;
        StringBuilder literal = new StringBuilder();

        for (int index = 0; index < message.length(); index++) {
            char current = message.charAt(index);
            if (current == '§' && index + 1 < message.length()) {
                if (!literal.isEmpty()) {
                    result.append(Text.literal(literal.toString()).setStyle(style));
                    literal.setLength(0);
                }
                net.minecraft.util.Formatting formatting = net.minecraft.util.Formatting.byCode(message.charAt(++index));
                if (formatting == null) {
                    literal.append('§').append(message.charAt(index));
                } else if (formatting == net.minecraft.util.Formatting.RESET) {
                    style = net.minecraft.text.Style.EMPTY;
                } else {
                    style = style.withFormatting(formatting);
                }
                continue;
            }
            literal.append(current);
        }

        if (!literal.isEmpty()) {
            result.append(Text.literal(literal.toString()).setStyle(style));
        }
        return result;
    }
}
