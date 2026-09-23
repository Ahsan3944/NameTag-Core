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

    private static final java.util.Set<String> COMMON_ITEM_FALLBACKS = java.util.Set.of(
            "minecraft:stone",
            "minecraft:cobblestone",
            "minecraft:deepslate",
            "minecraft:coal",
            "minecraft:charcoal",
            "minecraft:iron_ingot",
            "minecraft:gold_ingot",
            "minecraft:diamond",
            "minecraft:emerald",
            "minecraft:lapis_lazuli",
            "minecraft:redstone",
            "minecraft:quartz",
            "minecraft:amethyst_shard",
            "minecraft:copper_ingot",
            "minecraft:netherite_ingot",
            "minecraft:raw_iron",
            "minecraft:raw_gold",
            "minecraft:raw_copper",
            "minecraft:iron_nugget",
            "minecraft:gold_nugget"
    );

    @Override
    public Collection<String> itemNames() {
        java.util.Set<String> values = new java.util.TreeSet<>();
        Registries.ITEM.getIds().stream()
                .map(Object::toString)
                .forEach(values::add);

        // Keep the common vanilla item choices visible even when a server-side
        // registry/suggestion provider does not expose every vanilla entry to
        // Brigadier. These are only fallbacks; they do not replace registry data.
        COMMON_ITEM_FALLBACKS.forEach(id -> {
            if (Registries.ITEM.containsId(net.minecraft.util.Identifier.of(id.split(":", 2)[0], id.split(":", 2)[1]))) {
                values.add(id);
            }
        });
        return java.util.List.copyOf(values);
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
