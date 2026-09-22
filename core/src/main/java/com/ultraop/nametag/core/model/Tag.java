package com.ultraop.nametag.core.model;

import java.util.Map;
import java.util.Objects;

public record Tag(
        TagId id,
        String displayName,
        TagColor color,
        TagStyle style,
        TagEffect effect,
        int priority,
        boolean enabled,
        boolean chatEnabled,
        Map<String, String> metadata
) {
    public Tag {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(color, "color");
        Objects.requireNonNull(style, "style");
        Objects.requireNonNull(effect, "effect");
        // An icon-only tag is valid when the tag has no text label. The item/icon
        // metadata is resolved by the platform chat renderer.
        metadata = Map.copyOf(metadata == null ? Map.of() : metadata);
    }
}
