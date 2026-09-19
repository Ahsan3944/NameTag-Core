package com.ultraop.nametag.core.model;

import java.util.Map;
import java.util.Objects;

public record TagEffect(
        String id,
        Map<String, String> configuration
) {
    public TagEffect {
        Objects.requireNonNull(id, "id");
        configuration = Map.copyOf(configuration == null ? Map.of() : configuration);
    }

    public static TagEffect none() {
        return new TagEffect("none", Map.of());
    }
}
