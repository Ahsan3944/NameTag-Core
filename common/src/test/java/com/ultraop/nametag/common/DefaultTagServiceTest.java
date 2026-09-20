package com.ultraop.nametag.common;

import com.ultraop.nametag.core.model.Tag;
import com.ultraop.nametag.core.model.TagColor;
import com.ultraop.nametag.core.model.TagEffect;
import com.ultraop.nametag.core.model.TagId;
import com.ultraop.nametag.core.model.TagStyle;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DefaultTagServiceTest {

    @Test
    void assignPreservesExistingActiveTag() {
        DefaultTagService service = newService();
        UUID player = UUID.randomUUID();

        service.create(tag("owner", "OWNER", 10, true));
        service.create(tag("vip", "VIP", 20, true));

        service.assign(player, new TagId("owner"));
        PlayerAssignmentSnapshot first = snapshot(service.assign(player, new TagId("vip")));

        assertEquals(new TagId("owner"), first.active());
        assertEquals(2, first.assignedCount());
    }

    @Test
    void setActiveRequiresAssignment() {
        DefaultTagService service = newService();
        UUID player = UUID.randomUUID();

        service.create(tag("owner", "OWNER", 10, true));

        assertThrows(IllegalArgumentException.class,
                () -> service.setActive(player, new TagId("owner")));
    }

    @Test
    void activeTagUsesExplicitActiveBeforePriority() {
        DefaultTagService service = newService();
        UUID player = UUID.randomUUID();

        service.create(tag("owner", "OWNER", 10, true));
        service.create(tag("vip", "VIP", 50, true));

        service.assign(player, new TagId("owner"));
        service.assign(player, new TagId("vip"));
        service.setActive(player, new TagId("owner"));

        assertEquals(new TagId("owner"), service.activeTag(player).orElseThrow().id());
    }

    @Test
    void activeTagFallsBackToHighestPriorityEnabledTag() {
        DefaultTagService service = newService();
        UUID player = UUID.randomUUID();

        service.create(tag("member", "MEMBER", 10, true));
        service.create(tag("vip", "VIP", 50, true));
        service.create(tag("owner", "OWNER", 100, false));

        service.assign(player, new TagId("member"));
        service.assign(player, new TagId("vip"));
        service.assign(player, new TagId("owner"));
        service.setActive(player, new TagId("owner"));

        assertEquals(new TagId("vip"), service.activeTag(player).orElseThrow().id());
    }

    @Test
    void deletingTagRemovesItFromAssignments() {
        DefaultTagService service = newService();
        UUID player = UUID.randomUUID();

        service.create(tag("owner", "OWNER", 10, true));
        service.create(tag("vip", "VIP", 20, true));

        service.assign(player, new TagId("owner"));
        service.assign(player, new TagId("vip"));
        service.setActive(player, new TagId("vip"));

        assertTrue(service.delete(new TagId("vip")));

        assertEquals(new TagId("owner"), service.activeTag(player).orElseThrow().id());
        assertFalse(service.delete(new TagId("missing")));
    }

    private static DefaultTagService newService() {
        return new DefaultTagService(
                new InMemoryTagRepository(),
                new InMemoryPlayerAssignmentRepository()
        );
    }

    private static Tag tag(String id, String displayName, int priority, boolean enabled) {
        return new Tag(
                new TagId(id),
                displayName,
                new TagColor.Preset("red"),
                TagStyle.plain(),
                TagEffect.none(),
                priority,
                enabled,
                true,
                Map.of()
        );
    }

    private record PlayerAssignmentSnapshot(TagId active, int assignedCount) {}

    private static PlayerAssignmentSnapshot snapshot(com.ultraop.nametag.core.model.PlayerAssignment assignment) {
        return new PlayerAssignmentSnapshot(assignment.activeTagId(), assignment.assignedTagIds().size());
    }
}
