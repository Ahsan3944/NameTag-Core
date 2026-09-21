package com.ultraop.nametag.core.model;

import java.util.Objects;

public sealed interface TagColor permits TagColor.Preset, TagColor.Rgb, TagColor.Random, TagColor.Gradient {
    /**
     * Resolves a renderable RGB value for one glyph. Preset colors stay
     * platform-owned; random colors are deterministic for the supplied seed.
     */
    static Integer resolve(TagColor color, int index, int length, long seed) {
        Objects.requireNonNull(color, "color");

        if (color instanceof Rgb rgb) {
            return rgb.red() << 16 | rgb.green() << 8 | rgb.blue();
        }
        if (color instanceof Random) {
            return randomColor(seed);
        }
        if (color instanceof Gradient gradient) {
            return gradient(gradient.start(), gradient.end(), index, length);
        }
        return null;
    }

    private static int gradient(Rgb start, Rgb end, int index, int length) {
        if (length <= 1) {
            return start.red() << 16 | start.green() << 8 | start.blue();
        }
        double progress = Math.max(0.0, Math.min(1.0, (double) index / (length - 1)));
        int red = interpolate(start.red(), end.red(), progress);
        int green = interpolate(start.green(), end.green(), progress);
        int blue = interpolate(start.blue(), end.blue(), progress);
        return red << 16 | green << 8 | blue;
    }

    private static int interpolate(int start, int end, double progress) {
        return (int) Math.round(start + (end - start) * progress);
    }

    private static int randomColor(long seed) {
        long value = seed ^ 0x9E3779B97F4A7C15L;
        value ^= value >>> 30;
        value *= 0xBF58476D1CE4E5B9L;
        value ^= value >>> 27;
        value *= 0x94D049BB133111EBL;
        value ^= value >>> 31;
        return (int) value & 0xFFFFFF;
    }
    record Preset(String name) implements TagColor {
        public Preset {
            Objects.requireNonNull(name, "name");
            if (name.isBlank()) throw new IllegalArgumentException("Preset color name cannot be blank");
        }
    }

    record Rgb(int red, int green, int blue) implements TagColor {
        public Rgb {
            validate(red);
            validate(green);
            validate(blue);
        }

        private static void validate(int value) {
            if (value < 0 || value > 255) {
                throw new IllegalArgumentException("RGB component must be between 0 and 255");
            }
        }

        public String hex() {
            return "#%02X%02X%02X".formatted(red, green, blue);
        }
    }

    record Random() implements TagColor {}

    record Gradient(Rgb start, Rgb end) implements TagColor {
        public Gradient {
            Objects.requireNonNull(start, "start");
            Objects.requireNonNull(end, "end");
        }
    }
}
