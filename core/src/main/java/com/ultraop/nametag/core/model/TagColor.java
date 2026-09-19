package com.ultraop.nametag.core.model;

import java.util.Objects;

public sealed interface TagColor permits TagColor.Preset, TagColor.Rgb, TagColor.Random, TagColor.Gradient {
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
