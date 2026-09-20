package com.ultraop.nametag.core.model;

import java.util.Map;

/**
 * Validated configuration for the built-in NameTag glitch effect.
 *
 * Speed is an animation interval in milliseconds. Scheduling is owned by the
 * platform adapter; the core only describes the desired cadence.
 */
public record GlitchSettings(
        GlitchMode mode,
        int intensity,
        int speedMs
) {
    public static final int DEFAULT_INTENSITY = 45;
    public static final int DEFAULT_SPEED_MS = 80;
    public static final int MIN_INTENSITY = 0;
    public static final int MAX_INTENSITY = 100;
    public static final int MIN_SPEED_MS = 30;
    public static final int MAX_SPEED_MS = 2000;

    public GlitchSettings {
        if (mode == null) throw new IllegalArgumentException("Glitch mode cannot be null");
        if (intensity < MIN_INTENSITY || intensity > MAX_INTENSITY) {
            throw new IllegalArgumentException("Glitch intensity must be between 0 and 100");
        }
        if (speedMs < MIN_SPEED_MS || speedMs > MAX_SPEED_MS) {
            throw new IllegalArgumentException(
                    "Glitch speed must be between " + MIN_SPEED_MS + " and " + MAX_SPEED_MS + " ms"
            );
        }
    }

    public static GlitchSettings defaults(GlitchMode mode) {
        return new GlitchSettings(mode, DEFAULT_INTENSITY, DEFAULT_SPEED_MS);
    }

    public static GlitchSettings from(TagEffect effect) {
        if (!TagEffect.GLITCH_ID.equals(effect.id())) {
            throw new IllegalArgumentException("Effect is not glitch: " + effect.id());
        }

        Map<String, String> config = effect.configuration();
        GlitchMode mode = GlitchMode.from(config.getOrDefault(TagEffect.GLITCH_MODE_KEY, "white"));
        int intensity = parseInt(config, TagEffect.GLITCH_INTENSITY_KEY, DEFAULT_INTENSITY);
        int speedMs = parseInt(config, TagEffect.GLITCH_SPEED_KEY, DEFAULT_SPEED_MS);
        return new GlitchSettings(mode, intensity, speedMs);
    }

    private static int parseInt(Map<String, String> config, String key, int fallback) {
        String value = config.get(key);
        if (value == null || value.isBlank()) return fallback;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid glitch " + key + ": " + value, exception);
        }
    }
}
