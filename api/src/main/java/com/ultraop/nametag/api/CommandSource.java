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

    /**
     * Sends a message intended for human-readable, styled command output.
     * Platform adapters may translate the legacy section-code markup to native
     * text components; the default implementation remains plain-text safe.
     */
    default void sendStyledMessage(String message) {
        sendMessage(message);
    }

    default Collection<String> worldNames() {
        return List.of();
    }

    default Collection<String> itemNames() {
        return List.of();
    }
}
