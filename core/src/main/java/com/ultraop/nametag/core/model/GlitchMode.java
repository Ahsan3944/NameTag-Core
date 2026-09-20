package com.ultraop.nametag.core.model;

/**
 * Visual mode used by the built-in NameTag glitch effect.
 */
public enum GlitchMode {
    WHITE,
    COLORFUL;

    public static GlitchMode from(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Glitch mode cannot be null");
        }
        return switch (value.trim().toLowerCase()) {
            case "white" -> WHITE;
            case "colorful", "colourful", "color" -> COLORFUL;
            default -> throw new IllegalArgumentException(
                    "Unknown glitch mode: " + value + ". Use white or colorful."
            );
        };
    }
}
