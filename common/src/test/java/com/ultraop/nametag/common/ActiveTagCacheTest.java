package com.ultraop.nametag.common;

import com.ultraop.nametag.core.model.Tag;
import com.ultraop.nametag.core.model.TagColor;
import com.ultraop.nametag.core.model.TagEffect;
import com.ultraop.nametag.core.model.TagId;
import com.ultraop.nametag.core.model.TagStyle;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ActiveTagCacheTest {
    @Test
    void cachesSuccessfulAndEmptyResults() {
        ActiveTagCache cache = new ActiveTagCache(2);
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        Tag tag = tag("owner");

        cache.put(first, Optional.of(tag));
        cache.put(second, Optional.empty());

        assertEquals(Optional.of(tag), cache.get(first));
        assertEquals(Optional.empty(), cache.get(second));
        assertEquals(Optional.of(Optional.of(tag)), cache.findCached(first));
        assertEquals(Optional.of(Optional.empty()), cache.findCached(second));
        assertEquals(Optional.empty(), cache.findCached(UUID.randomUUID()));
        assertEquals(2, cache.size());
    }

    @Test
    void evictsLeastRecentlyUsedEntryWhenCapacityIsExceeded() {
        ActiveTagCache cache = new ActiveTagCache(2);
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        UUID third = UUID.randomUUID();

        cache.put(first, Optional.of(tag("first")));
        cache.put(second, Optional.of(tag("second")));
        cache.get(first);
        cache.put(third, Optional.of(tag("third")));

        assertNotNull(cache.get(first));
        assertNull(cache.get(second));
        assertNotNull(cache.get(third));
    }

    @Test
    void invalidatesEntriesByPlayerAndTag() {
        ActiveTagCache cache = new ActiveTagCache(4);
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        Tag owner = tag("owner");
        Tag vip = tag("vip");

        cache.put(first, Optional.of(owner));
        cache.put(second, Optional.of(vip));
        cache.invalidateTag(new TagId("owner"));

        assertNull(cache.get(first));
        assertEquals(Optional.of(vip), cache.get(second));

        cache.invalidatePlayer(second);
        assertNull(cache.get(second));
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
