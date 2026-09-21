package com.ultraop.nametag.common;

import com.ultraop.nametag.core.model.Tag;
import com.ultraop.nametag.core.model.TagColor;
import com.ultraop.nametag.core.model.TagEffect;
import com.ultraop.nametag.core.model.TagId;
import com.ultraop.nametag.core.model.TagStyle;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ActiveTagCachePerformanceTest {
    private static final int CAPACITY = 1024;
    private static final int WORKLOAD = 50_000;

    @Test
    void remainsBoundedUnderHighChurn() {
        ActiveTagCache cache = new ActiveTagCache(CAPACITY);
        Tag tag = tag("stress");
        List<UUID> players = new ArrayList<>(WORKLOAD);

        for (int i = 0; i < WORKLOAD; i++) {
            UUID player = new UUID(0L, i + 1L);
            players.add(player);
            cache.put(player, Optional.of(tag));
        }

        assertEquals(CAPACITY, cache.size());
        assertTrue(cache.findCached(players.get(WORKLOAD - 1)).isPresent());
        assertEquals(Optional.empty(), cache.findCached(players.get(0)));

        for (int i = WORKLOAD; i < WORKLOAD + CAPACITY; i++) {
            cache.put(new UUID(0L, i + 1L), Optional.empty());
        }

        assertEquals(CAPACITY, cache.size());
    }

    private static Tag tag(String id) {
        return new Tag(
                new TagId(id),
                id.toUpperCase(),
                new TagColor.Preset("white"),
                TagStyle.plain(),
                TagEffect.none(),
                0,
                true,
                true,
                Map.of()
        );
    }
}
