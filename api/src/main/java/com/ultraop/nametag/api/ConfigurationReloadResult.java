package com.ultraop.nametag.api;

import java.util.Objects;

public record ConfigurationReloadResult(boolean success, String message) {
    public ConfigurationReloadResult {
        Objects.requireNonNull(message, "message");
        if (message.isBlank()) {
            throw new IllegalArgumentException("message cannot be blank");
        }
    }

    public static ConfigurationReloadResult successResult() {
        return new ConfigurationReloadResult(true, "Configuration reloaded successfully.");
    }

    public static ConfigurationReloadResult failure(String message) {
        return new ConfigurationReloadResult(false, message);
    }
}
