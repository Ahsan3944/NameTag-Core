package com.ultraop.nametag.api;

import java.util.Optional;
import java.util.UUID;

public interface CommandSource {
    String name();

    Optional<UUID> playerUuid();

    boolean hasPermission(String permission);

    void sendMessage(String message);
}
