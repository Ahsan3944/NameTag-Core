package com.ultraop.nametag.common;

import com.ultraop.nametag.core.model.Tag;
import com.ultraop.nametag.core.model.TagId;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Bounded LRU cache for resolved active tags.
 *
 * <p>Empty results are cached as well, avoiding repeated resolution scans for
 * players that currently have no active tag.</p>
 */
final class ActiveTagCache {
    static final int DEFAULT_CAPACITY = 1024;

    private final int capacity;
    private final LinkedHashMap<UUID, Optional<Tag>> values;

    ActiveTagCache() {
        this(DEFAULT_CAPACITY);
    }

    ActiveTagCache(int capacity) {
        if (capacity < 1 || capacity > 100_000) {
            throw new IllegalArgumentException("Cache capacity must be between 1 and 100000");
        }
        this.capacity = capacity;
        this.values = new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<UUID, Optional<Tag>> eldest) {
                return size() > ActiveTagCache.this.capacity;
            }
        };
    }

    synchronized Optional<Optional<Tag>> findCached(UUID playerUuid) {
        if (!values.containsKey(playerUuid)) {
            return Optional.empty();
        }
        return Optional.of(values.get(playerUuid));
    }

    synchronized void put(UUID playerUuid, Optional<Tag> tag) {
        values.put(playerUuid, tag);
    }

    synchronized void invalidatePlayer(UUID playerUuid) {
        values.remove(playerUuid);
    }

    synchronized void invalidateTag(TagId tagId) {
        values.entrySet().removeIf(entry ->
                entry.getValue().map(tag -> tag.id().equals(tagId)).orElse(false)
        );
    }

    synchronized void clear() {
        values.clear();
    }

    synchronized int size() {
        return values.size();
    }
}
