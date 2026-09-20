package com.ultraop.nametag.core.model;

import java.util.List;
import java.util.Objects;

/**
 * One immutable rendered frame of a glitching NameTag.
 *
 * The frame preserves the original NameTag length. Each glyph may optionally
 * carry an RGB color, allowing platform adapters to render white or colorful
 * glitch output without putting Minecraft classes in core.
 */
public record GlitchFrame(
        String text,
        List<Glyph> glyphs
) {
    public GlitchFrame {
        Objects.requireNonNull(text, "text");
        glyphs = List.copyOf(glyphs == null ? List.of() : glyphs);
        if (glyphs.size() != text.length()) {
            throw new IllegalArgumentException("Glyph count must match text length");
        }
    }

    public record Glyph(char character, int rgb) {
        public Glyph {
            if (rgb < 0 || rgb > 0xFFFFFF) {
                throw new IllegalArgumentException("Glyph RGB must be between 0x000000 and 0xFFFFFF");
            }
        }

        public String hex() {
            return "#%06X".formatted(rgb);
        }
    }
}
