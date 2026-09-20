package com.ultraop.nametag.core.model;

import java.util.Map;
import java.util.Objects;

public record TagEffect(
        String id,
        Map<String, String> configuration
) {
    public static final String NONE_ID = "none";
    public static final String GLITCH_ID = "glitch";
    public static final String GLITCH_MODE_KEY = "mode";
    public static final String GLITCH_INTENSITY_KEY = "intensity";
    public static final String GLITCH_SPEED_KEY = "speed-ms";

    public TagEffect {
        Objects.requireNonNull(id, "id");
        configuration = Map.copyOf(configuration == null ? Map.of() : configuration);
    }

    public static TagEffect none() {
        return new TagEffect(NONE_ID, Map.of());
    }

    public static TagEffect glitch(GlitchMode mode) {
        return glitch(mode, GlitchSettings.DEFAULT_INTENSITY, GlitchSettings.DEFAULT_SPEED_MS);
    }

    public static TagEffect glitch(GlitchMode mode, int intensity, int speedMs) {
        GlitchSettings settings = new GlitchSettings(mode, intensity, speedMs);
        return new TagEffect(GLITCH_ID, Map.of(
                GLITCH_MODE_KEY, mode.name().toLowerCase(),
                GLITCH_INTENSITY_KEY, Integer.toString(settings.intensity()),
                GLITCH_SPEED_KEY, Integer.toString(settings.speedMs())
        ));
    }

    public boolean isGlitch() {
        return GLITCH_ID.equals(id);
    }
}
