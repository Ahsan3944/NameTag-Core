package com.ultraop.nametag.api;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CommandSource {
    String name();

    Optional<UUID> playerUuid();

    boolean hasPermission(String permission);

    void sendMessage(String message);

    default Collection<String> worldNames() {
        return List.of();
    }

    default Collection<String> itemNames() {
        return List.of();
    }
}
