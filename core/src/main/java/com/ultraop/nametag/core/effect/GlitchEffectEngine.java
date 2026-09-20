package com.ultraop.nametag.core.effect;

import com.ultraop.nametag.core.model.GlitchFrame;
import com.ultraop.nametag.core.model.GlitchMode;
import com.ultraop.nametag.core.model.GlitchSettings;

import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;

/**
 * Platform-independent NameTag glitch frame generator.
 *
 * Unlike the old GlitchIdentity death-message generator, a NameTag must keep
 * its source text length and identity stable. The engine therefore corrupts
 * individual characters rather than replacing the whole name with a random
 * 5-7 character string.
 */
public final class GlitchEffectEngine {
    private static final char[] GLITCH_POOL = (
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789" +
            "!@#$%&*+=?<>/\\|~^"
    ).toCharArray();

    private static final int[] COLOR_PALETTE = {
            0x00FFFF, // cyan
            0xFF00FF, // magenta
            0x39FF14, // neon green
            0xFFFF00, // yellow
            0xFF4D6D, // pink/red
            0x4D7CFF, // blue
            0xFFFFFF  // white
    };

    public GlitchFrame render(String source, GlitchSettings settings, long frameIndex, long seed) {
        if (source == null) throw new IllegalArgumentException("source cannot be null");
        if (settings == null) throw new IllegalArgumentException("settings cannot be null");

        SplittableRandom random = new SplittableRandom(mixSeed(seed, frameIndex));
        List<GlitchFrame.Glyph> glyphs = new ArrayList<>(source.length());
        StringBuilder rendered = new StringBuilder(source.length());

        for (int i = 0; i < source.length(); i++) {
            char original = source.charAt(i);
            char output = original;

            if (!Character.isWhitespace(original) && random.nextInt(100) < settings.intensity()) {
                output = GLITCH_POOL[random.nextInt(GLITCH_POOL.length)];

                // Avoid a no-op corruption frame.
                if (output == original) {
                    int nextIndex = (indexOf(original) + 1 + random.nextInt(GLITCH_POOL.length - 1))
                            % GLITCH_POOL.length;
                    output = GLITCH_POOL[nextIndex];
                }
            }

            int rgb = settings.mode() == GlitchMode.WHITE
                    ? 0xFFFFFF
                    : COLOR_PALETTE[random.nextInt(COLOR_PALETTE.length)];

            rendered.append(output);
            glyphs.add(new GlitchFrame.Glyph(output, rgb));
        }

        return new GlitchFrame(rendered.toString(), glyphs);
    }

    private static int indexOf(char character) {
        for (int i = 0; i < GLITCH_POOL.length; i++) {
            if (GLITCH_POOL[i] == character) return i;
        }
        return 0;
    }

    private static long mixSeed(long seed, long frameIndex) {
        long value = seed ^ (frameIndex * 0x9E3779B97F4A7C15L);
        value ^= value >>> 30;
        value *= 0xBF58476D1CE4E5B9L;
        value ^= value >>> 27;
        value *= 0x94D049BB133111EBL;
        return value ^ (value >>> 31);
    }
}
