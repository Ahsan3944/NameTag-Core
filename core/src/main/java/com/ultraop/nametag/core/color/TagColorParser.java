package com.ultraop.nametag.core.color;

import com.ultraop.nametag.core.model.TagColor;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TagColorParser {
    private static final Pattern HEX_6 = Pattern.compile("^#?([0-9a-fA-F]{6})$");
    private static final Pattern HEX_3 = Pattern.compile("^#?([0-9a-fA-F]{3})$");
    private static final Pattern RGB_FUNCTION = Pattern.compile(
            "^rgb\\s*\\(\\s*(\\d{1,3})\\s*,\\s*(\\d{1,3})\\s*,\\s*(\\d{1,3})\\s*\\)$",
            Pattern.CASE_INSENSITIVE
    );

    private TagColorParser() {}

    /**
     * Parses a user-facing color value.
     *
     * Supported forms:
     * - #RRGGBB / RRGGBB
     * - #RGB / RGB
     * - 0xRRGGBB
     * - rgb(R, G, B)
     * - random
     * - named/preset colors (for example: red, aqua, gold)
     */
    public static TagColor parse(String value) {
        Objects.requireNonNull(value, "value");
        String input = value.trim();
        if (input.isEmpty()) {
            throw new IllegalArgumentException("Color value cannot be blank");
        }

        if (input.equalsIgnoreCase("random")) {
            return new TagColor.Random();
        }

        if (input.regionMatches(true, 0, "0x", 0, 2)) {
            return parseHex(input.substring(2), input);
        }

        Matcher rgb = RGB_FUNCTION.matcher(input);
        if (rgb.matches()) {
            return new TagColor.Rgb(
                    parseComponent(rgb.group(1), input),
                    parseComponent(rgb.group(2), input),
                    parseComponent(rgb.group(3), input)
            );
        }

        Matcher hex6 = HEX_6.matcher(input);
        if (hex6.matches()) {
            return fromHex(hex6.group(1));
        }

        Matcher hex3 = HEX_3.matcher(input);
        if (hex3.matches()) {
            String value3 = hex3.group(1);
            return new TagColor.Rgb(
                    Integer.parseInt("" + value3.charAt(0) + value3.charAt(0), 16),
                    Integer.parseInt("" + value3.charAt(1) + value3.charAt(1), 16),
                    Integer.parseInt("" + value3.charAt(2) + value3.charAt(2), 16)
            );
        }

        // A value that explicitly starts as hex must be rejected when malformed;
        // it must not silently become a preset color.
        if (input.startsWith("#")) {
            throw new IllegalArgumentException("Invalid hex color: " + input);
        }

        if (input.regionMatches(true, 0, "rgb", 0, 3)) {
            throw new IllegalArgumentException("Invalid RGB color: " + input);
        }

        return new TagColor.Preset(input.toLowerCase(Locale.ROOT));
    }

    private static TagColor parseHex(String value, String original) {
        if (!HEX_6.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid hex color: " + original);
        }
        return fromHex(value);
    }

    private static TagColor.Rgb fromHex(String value) {
        int packed = Integer.parseInt(value, 16);
        return new TagColor.Rgb(
                (packed >> 16) & 0xFF,
                (packed >> 8) & 0xFF,
                packed & 0xFF
        );
    }

    private static int parseComponent(String value, String original) {
        int component = Integer.parseInt(value);
        if (component < 0 || component > 255) {
            throw new IllegalArgumentException("RGB component out of range in: " + original);
        }
        return component;
    }
}
