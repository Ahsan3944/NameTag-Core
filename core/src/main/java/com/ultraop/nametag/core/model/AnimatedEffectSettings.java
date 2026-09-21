package com.ultraop.nametag.core.model;

import java.util.Map;

public record AnimatedEffectSettings(String effectId, int intensity, int speedMs) {
    public static final int DEFAULT_INTENSITY = 45;
    public static final int DEFAULT_SPEED_MS = 80;
    public static final int MIN_INTENSITY = 0;
    public static final int MAX_INTENSITY = 100;
    public static final int MIN_SPEED_MS = 30;
    public static final int MAX_SPEED_MS = 2000;

    public AnimatedEffectSettings {
        if (!TagEffect.isAnimated(effectId)) throw new IllegalArgumentException("Effect is not animated: " + effectId);
        if (intensity < MIN_INTENSITY || intensity > MAX_INTENSITY) throw new IllegalArgumentException("Animation intensity must be between 0 and 100");
        if (speedMs < MIN_SPEED_MS || speedMs > MAX_SPEED_MS) throw new IllegalArgumentException("Animation speed must be between 30 and 2000 ms");
    }

    public static AnimatedEffectSettings from(TagEffect effect) {
        if (effect == null) throw new IllegalArgumentException("Effect cannot be null");
        Map<String, String> config = effect.configuration();
        return new AnimatedEffectSettings(effect.id(),
                parseInt(config, TagEffect.ANIMATION_INTENSITY_KEY, DEFAULT_INTENSITY),
                parseInt(config, TagEffect.ANIMATION_SPEED_KEY, DEFAULT_SPEED_MS));
    }

    private static int parseInt(Map<String, String> config, String key, int fallback) {
        String value = config.get(key);
        if (value == null || value.isBlank()) return fallback;
        try { return Integer.parseInt(value); }
        catch (NumberFormatException exception) { throw new IllegalArgumentException("Invalid animation " + key + ": " + value, exception); }
    }
}
