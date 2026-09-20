package com.ultraop.nametag.core.model;

import java.util.Objects;

/**
 * Immutable runtime configuration for NameTag-Core.
 *
 * Reloading and lifecycle ownership are intentionally outside this model.
 */
public record NameTagConfiguration(
        boolean nameplateEnabled,
        boolean chatEnabled,
        String chatFormat,
        int defaultTagPriority,
        boolean defaultTagEnabled,
        boolean defaultTagChatEnabled,
        int defaultGlitchIntensity,
        int defaultGlitchSpeedMs
) {
    public static final String DEFAULT_CHAT_FORMAT = "[{tag}] {player}: {message}";

    public NameTagConfiguration {
        Objects.requireNonNull(chatFormat, "chatFormat");
        if (chatFormat.isBlank()) {
            throw new IllegalArgumentException("Chat format cannot be blank");
        }
        if (defaultTagPriority < -1_000_000 || defaultTagPriority > 1_000_000) {
            throw new IllegalArgumentException("Default tag priority must be between -1000000 and 1000000");
        }
        if (defaultGlitchIntensity < GlitchSettings.MIN_INTENSITY
                || defaultGlitchIntensity > GlitchSettings.MAX_INTENSITY) {
            throw new IllegalArgumentException("Default glitch intensity must be between 0 and 100");
        }
        if (defaultGlitchSpeedMs < GlitchSettings.MIN_SPEED_MS
                || defaultGlitchSpeedMs > GlitchSettings.MAX_SPEED_MS) {
            throw new IllegalArgumentException(
                    "Default glitch speed must be between "
                            + GlitchSettings.MIN_SPEED_MS + " and "
                            + GlitchSettings.MAX_SPEED_MS + " ms"
            );
        }
    }

    public static NameTagConfiguration defaults() {
        return new NameTagConfiguration(
                true,
                true,
                DEFAULT_CHAT_FORMAT,
                0,
                true,
                true,
                GlitchSettings.DEFAULT_INTENSITY,
                GlitchSettings.DEFAULT_SPEED_MS
        );
    }
}
