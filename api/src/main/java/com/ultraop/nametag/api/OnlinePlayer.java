package com.ultraop.nametag.api;

import java.util.UUID;

public record OnlinePlayer(UUID uuid, String name) {
    public OnlinePlayer {
        if (uuid == null) {
            throw new IllegalArgumentException("uuid cannot be null");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name cannot be blank");
        }
    }
}
