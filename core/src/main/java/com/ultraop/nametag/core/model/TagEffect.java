package com.ultraop.nametag.core.model;

import java.util.Map;
import java.util.Objects;

public record TagEffect(
        String id,
        Map<String, String> configuration
) {
    public static final String NONE_ID = "none";
    public static final String GLITCH_ID = "glitch";
    public static final String RAINBOW_ID = "rainbow";
    public static final String PULSE_ID = "pulse";
    public static final String WAVE_ID = "wave";
    public static final String NEON_ID = "neon";
    public static final String BREATH_ID = "breath";
    public static final String BLINK_ID = "blink";
    public static final String RGB_ID = "rgb";
    public static final String GLITCH_MODE_KEY = "mode";
    public static final String GLITCH_INTENSITY_KEY = "intensity";
    public static final String GLITCH_SPEED_KEY = "speed-ms";
    public static final String ANIMATION_INTENSITY_KEY = "intensity";
    public static final String ANIMATION_SPEED_KEY = "speed-ms";

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

    public static TagEffect rainbow() { return rainbow(45, 80); }
    public static TagEffect rainbow(int intensity, int speedMs) { return animated(RAINBOW_ID, intensity, speedMs); }
    public static TagEffect pulse() { return pulse(45, 80); }
    public static TagEffect pulse(int intensity, int speedMs) { return animated(PULSE_ID, intensity, speedMs); }
    public static TagEffect wave() { return wave(45, 80); }
    public static TagEffect wave(int intensity, int speedMs) { return animated(WAVE_ID, intensity, speedMs); }
    public static TagEffect neon() { return animated(NEON_ID, 65, 70); }
    public static TagEffect breath() { return animated(BREATH_ID, 55, 110); }
    public static TagEffect blink() { return animated(BLINK_ID, 100, 180); }
    public static TagEffect rgb() { return animated(RGB_ID, 100, 45); }
    private static TagEffect animated(String id, int intensity, int speedMs) {
        if (intensity < 0 || intensity > 100) throw new IllegalArgumentException("Animation intensity must be between 0 and 100");
        if (speedMs < 30 || speedMs > 2000) throw new IllegalArgumentException("Animation speed must be between 30 and 2000 ms");
        return new TagEffect(id, Map.of(ANIMATION_INTENSITY_KEY, Integer.toString(intensity), ANIMATION_SPEED_KEY, Integer.toString(speedMs)));
    }
    public boolean isGlitch() { return GLITCH_ID.equals(id); }
    public boolean isAnimated() { return isAnimated(id); }
    public static boolean isAnimated(String id) { return RAINBOW_ID.equals(id) || PULSE_ID.equals(id) || WAVE_ID.equals(id)
                || NEON_ID.equals(id) || BREATH_ID.equals(id) || BLINK_ID.equals(id) || RGB_ID.equals(id); }
}
