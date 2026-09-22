package com.ultraop.nametag.core.model;

import java.util.Locale;
import java.util.Map;

public record TagItemSettings(
        String itemId,
        Mode mode,
        int speed
) {
    public static final String ITEM_KEY = "item";
    public static final String MODE_KEY = "item-mode";
    public static final String SPEED_KEY = "item-speed";

    public TagItemSettings {
        if (itemId == null || itemId.isBlank()) {
            throw new IllegalArgumentException("Item ID cannot be blank");
        }
        itemId = itemId.toLowerCase(Locale.ROOT);
        if (!itemId.contains(":")) {
            itemId = "minecraft:" + itemId;
        }
        if (mode == null) {
            mode = Mode.STATIC;
        }
        if (speed < 1 || speed > 10) {
            throw new IllegalArgumentException("Item rotation speed must be between 1 and 10");
        }
    }

    public static TagItemSettings defaults(String itemId) {
        return new TagItemSettings(itemId, Mode.STATIC, 5);
    }

    public static TagItemSettings from(Tag tag) {
        Map<String, String> metadata = tag.metadata();
        String item = metadata.get(ITEM_KEY);
        if (item == null || item.isBlank()) {
            return null;
        }
        Mode mode = Mode.parse(metadata.get(MODE_KEY));
        int speed = parseSpeed(metadata.get(SPEED_KEY));
        return new TagItemSettings(item, mode, speed);
    }

    public static int parseSpeed(String value) {
        if (value == null || value.isBlank()) return 5;
        try {
            return Math.max(1, Math.min(10, Integer.parseInt(value)));
        } catch (NumberFormatException exception) {
            return 5;
        }
    }

    public enum Mode {
        STATIC,
        ROTATE;

        public static Mode parse(String value) {
            if (value == null) return STATIC;
            return switch (value.toLowerCase(Locale.ROOT)) {
                case "rotate", "rotating", "spin" -> ROTATE;
                default -> STATIC;
            };
        }

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }
    }
}
