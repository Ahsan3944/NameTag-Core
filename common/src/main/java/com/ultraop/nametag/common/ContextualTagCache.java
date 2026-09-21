package com.ultraop.nametag.common;

import com.ultraop.nametag.api.TagResolutionContext;
import com.ultraop.nametag.core.model.Tag;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

final class ContextualTagCache {
    private static final int DEFAULT_CAPACITY = 2048;
    private final int capacity;
    private final LinkedHashMap<Key, List<Tag>> values;

    ContextualTagCache() {
        this(DEFAULT_CAPACITY);
    }

    ContextualTagCache(int capacity) {
        if (capacity < 1 || capacity > 100_000) {
            throw new IllegalArgumentException("Cache capacity must be between 1 and 100000");
        }
        this.capacity = capacity;
        this.values = new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<Key, List<Tag>> eldest) {
                return size() > ContextualTagCache.this.capacity;
            }
        };
    }

    synchronized List<Tag> find(UUID playerUuid, TagResolutionContext context) {
        List<Tag> result = values.get(new Key(playerUuid, context));
        return result == null ? null : List.copyOf(result);
    }

    synchronized void put(UUID playerUuid, TagResolutionContext context, List<Tag> tags) {
        values.put(new Key(playerUuid, context), List.copyOf(tags));
    }

    synchronized void invalidatePlayer(UUID playerUuid) {
        values.keySet().removeIf(key -> key.playerUuid().equals(playerUuid));
    }

    synchronized void invalidateTag(String tagId) {
        values.entrySet().removeIf(entry ->
                entry.getValue().stream().anyMatch(tag -> tag.id().value().equals(tagId)));
    }

    synchronized void clear() {
        values.clear();
    }

    private record Key(UUID playerUuid, TagResolutionContext context) {}
}
