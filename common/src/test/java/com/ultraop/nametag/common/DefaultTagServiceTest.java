package com.ultraop.nametag.common;

import com.ultraop.nametag.core.model.Tag;
import com.ultraop.nametag.core.model.TagColor;
import com.ultraop.nametag.core.model.TagEffect;
import com.ultraop.nametag.core.model.TagId;
import com.ultraop.nametag.core.model.TagStyle;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DefaultTagServiceTest {

    @TempDir
    Path tempDir;

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

    
    @Test
    void emitsCreateUpdateAssignmentAndDeleteEvents() {
        DefaultTagService service = newService();
        List<com.ultraop.nametag.api.TagEvent> events = new ArrayList<>();
        service.events().register(events::add);

        UUID player = UUID.randomUUID();
        Tag owner = tag("owner", "OWNER", 10, true);
        service.create(owner);
        Tag updated = new Tag(
                owner.id(), "OWNER+", owner.color(), owner.style(), owner.effect(),
                owner.priority(), owner.enabled(), owner.chatEnabled(), owner.metadata()
        );
        service.update(updated);
        service.assign(player, owner.id());
        service.setActive(player, owner.id());
        service.delete(owner.id());

        assertEquals(4, events.size());
        assertInstanceOf(com.ultraop.nametag.api.TagEvent.Created.class, events.get(0));
        assertInstanceOf(com.ultraop.nametag.api.TagEvent.Updated.class, events.get(1));
        assertInstanceOf(com.ultraop.nametag.api.TagEvent.AssignmentChanged.class, events.get(2));
        assertInstanceOf(com.ultraop.nametag.api.TagEvent.AssignmentChanged.class, events.get(3));
    }

    @Test
    void doesNotEmitEventsForNoOpAssignmentOrMissingClear() {
        DefaultTagService service = newService();
        List<com.ultraop.nametag.api.TagEvent> events = new ArrayList<>();
        service.events().register(events::add);

        UUID player = UUID.randomUUID();
        service.clear(player);
        service.create(tag("owner", "OWNER", 10, true));
        service.assign(player, new TagId("owner"));
        service.assign(player, new TagId("owner"));
        service.setActive(player, new TagId("owner"));

        assertEquals(2, events.size());
        assertInstanceOf(com.ultraop.nametag.api.TagEvent.Created.class, events.get(0));
        assertInstanceOf(com.ultraop.nametag.api.TagEvent.AssignmentChanged.class, events.get(1));
    }

    @Test
    void yamlBackedServiceSurvivesRestart() {
        UUID player = UUID.randomUUID();

        DefaultTagService first = new DefaultTagService(
                new YamlTagRepository(tempDir.resolve("tags.yml")),
                new YamlPlayerAssignmentRepository(tempDir.resolve("assignments.yml"))
        );
        first.create(tag("owner", "OWNER", 100, true));
        first.assign(player, new TagId("owner"));

        DefaultTagService restarted = new DefaultTagService(
                new YamlTagRepository(tempDir.resolve("tags.yml")),
                new YamlPlayerAssignmentRepository(tempDir.resolve("assignments.yml"))
        );

        assertEquals("OWNER", restarted.activeTag(player).orElseThrow().displayName());
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
