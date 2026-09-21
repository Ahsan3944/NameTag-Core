package com.ultraop.nametag.api;

import java.util.Objects;

public record TagResolutionContext(String world, int x, int y, int z) {
    public TagResolutionContext {
        Objects.requireNonNull(world, "world");
        if (world.isBlank()) throw new IllegalArgumentException("world cannot be blank");
    }

    public String key() {
        return world + "@" + x + "," + y + "," + z;
    }
}
