package com.ultraop.nametag.common;

import com.ultraop.nametag.api.PlayerAssignmentRepository;
import com.ultraop.nametag.api.TagRepository;
import com.ultraop.nametag.core.model.PlayerAssignment;
import com.ultraop.nametag.core.model.Tag;
import com.ultraop.nametag.core.model.TagColor;
import com.ultraop.nametag.core.model.TagEffect;
import com.ultraop.nametag.core.model.TagId;
import com.ultraop.nametag.core.model.TagStyle;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DefaultTagServiceCacheTest {
    @Test
    void activeTagUsesCachedResolutionUntilMutationInvalidatesIt() {
        CountingAssignmentRepository assignments = new CountingAssignmentRepository();
        DefaultTagService service = new DefaultTagService(
                new InMemoryTagRepository(),
                assignments,
                8
        );
        UUID player = UUID.randomUUID();

        service.create(tag("owner", "OWNER", 100, true));
        service.assign(player, new TagId("owner"));
        assignments.findCalls = 0;

        assertEquals("OWNER", service.activeTag(player).orElseThrow().displayName());
        assertEquals("OWNER", service.activeTag(player).orElseThrow().displayName());
        assertEquals(1, assignments.findCalls);

        service.clear(player);

        assertEquals(Optional.empty(), service.activeTag(player));
        // clear() reads the current assignment before invalidating the cache; the subsequent
        // activeTag() call must perform a fresh lookup rather than reuse the cleared entry.
        assertEquals(3, assignments.findCalls);
    }

    @Test
    void updatingAnotherAssignedTagInvalidatesPriorityFallback() {
        CountingAssignmentRepository assignments = new CountingAssignmentRepository();
        DefaultTagService service = new DefaultTagService(
                new InMemoryTagRepository(),
                assignments,
                8
        );
        UUID player = UUID.randomUUID();

        service.create(tag("member", "MEMBER", 10, true));
        service.create(tag("vip", "VIP", 20, true));
        assignments.save(new PlayerAssignment(
                player,
                List.of(new TagId("member"), new TagId("vip")),
                null
        ));
        assignments.findCalls = 0;

        assertEquals("VIP", service.activeTag(player).orElseThrow().displayName());
        service.update(tag("member", "MEMBER", 50, true));

        assertEquals("MEMBER", service.activeTag(player).orElseThrow().displayName());
        assertEquals(2, assignments.findCalls);
    }

    @Test
    void tagUpdateInvalidatesPlayersUsingThatTag() {
        CountingAssignmentRepository assignments = new CountingAssignmentRepository();
        DefaultTagService service = new DefaultTagService(
                new InMemoryTagRepository(),
                assignments,
                8
        );
        UUID player = UUID.randomUUID();

        service.create(tag("owner", "OWNER", 100, true));
        service.assign(player, new TagId("owner"));
        assignments.findCalls = 0;

        service.activeTag(player);
        Tag updated = tag("owner", "UPDATED", 100, true);
        service.update(updated);

        assertEquals("UPDATED", service.activeTag(player).orElseThrow().displayName());
        assertEquals(2, assignments.findCalls);
    }

    private static Tag tag(String id, String displayName, int priority, boolean enabled) {
        return new Tag(
                new TagId(id),
                displayName,
                new TagColor.Preset("white"),
                TagStyle.plain(),
                TagEffect.none(),
                priority,
                enabled,
                true,
                Map.of()
        );
    }

    private static final class CountingAssignmentRepository implements PlayerAssignmentRepository {
        private final Map<UUID, PlayerAssignment> values = new LinkedHashMap<>();
        private int findCalls;

        @Override
        public Optional<PlayerAssignment> find(UUID playerUuid) {
            findCalls++;
            return Optional.ofNullable(values.get(playerUuid));
        }

        @Override
        public Collection<PlayerAssignment> findAll() {
            return List.copyOf(values.values());
        }

        @Override
        public void save(PlayerAssignment assignment) {
            values.put(assignment.playerUuid(), assignment);
        }

        @Override
        public void delete(UUID playerUuid) {
            values.remove(playerUuid);
        }
    }

    private static final class InMemoryTagRepository implements TagRepository {
        private final Map<TagId, Tag> values = new LinkedHashMap<>();

        @Override
        public Optional<Tag> find(TagId id) {
            return Optional.ofNullable(values.get(id));
        }

        @Override
        public Collection<Tag> findAll() {
            return List.copyOf(values.values());
        }

        @Override
        public void save(Tag tag) {
            values.put(tag.id(), tag);
        }

        @Override
        public void delete(TagId id) {
            values.remove(id);
        }
    }
}
