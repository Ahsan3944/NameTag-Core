package com.ultraop.nametag.common;

import com.ultraop.nametag.core.model.PlayerAssignment;
import com.ultraop.nametag.core.model.Tag;
import com.ultraop.nametag.api.TagResolutionContext;
import com.ultraop.nametag.core.model.TagColor;
import com.ultraop.nametag.api.PermissionService;
import com.ultraop.nametag.core.model.TagEffect;
import com.ultraop.nametag.core.model.TagId;
import com.ultraop.nametag.core.model.TagStyle;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DefaultTagServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void assigningAnItemTagReplacesExistingItemTagButKeepsNormalTags() {
        DefaultTagService service = newService();
        UUID player = UUID.randomUUID();

        service.create(tag("rank", "Rank", 0, true));
        service.create(new Tag(new TagId("iron"), "Iron", new TagColor.Preset("red"), TagStyle.plain(),
                TagEffect.none(), 0, true, true, Map.of("item", "minecraft:iron_ingot")));
        service.create(new Tag(new TagId("diamond"), "Diamond", new TagColor.Preset("red"), TagStyle.plain(),
                TagEffect.none(), 0, true, true, Map.of("item", "minecraft:diamond")));

        service.assign(player, new TagId("rank"));
        service.assign(player, new TagId("iron"));
        service.assign(player, new TagId("diamond"));

        List<Tag> assigned = service.assignedTags(player);
        assertEquals(List.of(new TagId("rank"), new TagId("diamond")),
                assigned.stream().map(Tag::id).toList());
    }

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
    void setActiveAssignsAndActivatesWhenMissing() {
        DefaultTagService service = newService();
        UUID player = UUID.randomUUID();

        service.create(tag("owner", "OWNER", 10, true));

        PlayerAssignmentSnapshot result = snapshot(service.setActive(player, new TagId("owner")));

        assertEquals(new TagId("owner"), result.active());
        assertEquals(1, result.assignedCount());
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
    void automaticRoleUsesHighestPriorityMatchingTag() {
        UUID player = UUID.randomUUID();
        PermissionService permissions = (uuid, permission) -> player.equals(uuid) && permission.equals("group.vip");

        DefaultTagService service = new DefaultTagService(
                new InMemoryTagRepository(),
                new InMemoryPlayerAssignmentRepository(),
                permissions
        );
        service.create(roleTag("member", "MEMBER", 10, "group.member"));
        service.create(roleTag("vip", "VIP", 50, "group.vip"));

        assertEquals(new TagId("vip"), service.activeTag(player).orElseThrow().id());
    }

    @Test
    void automaticRoleUsesTagIdAsDeterministicTieBreaker() {
        UUID player = UUID.randomUUID();
        PermissionService permissions = (uuid, permission) -> player.equals(uuid) && permission.equals("group.staff");

        DefaultTagService service = new DefaultTagService(
                new InMemoryTagRepository(),
                new InMemoryPlayerAssignmentRepository(),
                permissions
        );
        service.create(roleTag("alpha", "ALPHA", 50, "group.staff"));
        service.create(roleTag("omega", "OMEGA", 50, "group.staff"));

        assertEquals(new TagId("alpha"), service.activeTag(player).orElseThrow().id());
    }

    @Test
    void explicitAssignmentOverridesAutomaticRole() {
        UUID player = UUID.randomUUID();
        PermissionService permissions = (uuid, permission) -> player.equals(uuid) && permission.equals("group.vip");

        DefaultTagService service = new DefaultTagService(
                new InMemoryTagRepository(),
                new InMemoryPlayerAssignmentRepository(),
                permissions
        );
        service.create(roleTag("vip", "VIP", 100, "group.vip"));
        service.create(tag("custom", "CUSTOM", 1, true));

        service.assign(player, new TagId("custom"));

        assertEquals(new TagId("custom"), service.activeTag(player).orElseThrow().id());
    }

    @Test
    void automaticRoleResolutionReflectsPermissionChangesWithoutPersistingAssignment() {
        UUID player = UUID.randomUUID();
        java.util.Set<String> granted = new java.util.HashSet<>();
        PermissionService permissions = (uuid, permission) -> player.equals(uuid) && granted.contains(permission);

        InMemoryPlayerAssignmentRepository assignments = new InMemoryPlayerAssignmentRepository();
        DefaultTagService service = new DefaultTagService(
                new InMemoryTagRepository(),
                assignments,
                permissions
        );
        service.create(roleTag("vip", "VIP", 50, "group.vip"));
        service.create(roleTag("staff", "STAFF", 100, "group.staff"));

        granted.add("group.vip");
        assertEquals(new TagId("vip"), service.activeTag(player).orElseThrow().id());

        granted.remove("group.vip");
        granted.add("group.staff");
        assertEquals(new TagId("staff"), service.activeTag(player).orElseThrow().id());

        assertTrue(assignments.find(player).isEmpty());
    }

    @Test
    void contextualResolutionDoesNotCacheExpiringAssignments() {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        MutableClock clock = new MutableClock(now);
        InMemoryTagRepository tags = new InMemoryTagRepository();
        InMemoryPlayerAssignmentRepository assignments = new InMemoryPlayerAssignmentRepository();
        DefaultTagService service = new DefaultTagService(
                tags, assignments, 8, new com.ultraop.nametag.api.TagEventBus(), clock
        );
        UUID player = UUID.randomUUID();

        service.create(tag("member", "MEMBER", 10, true));
        service.create(tag("vip", "VIP", 50, true));
        service.assign(player, new TagId("member"));
        service.assignUntil(player, new TagId("vip"), now.plusSeconds(60));
        service.setActive(player, new TagId("vip"));

        assertEquals(List.of("vip", "member"),
                service.activeTags(player, new TagResolutionContext("world", 0, 64, 0))
                        .stream().map(Tag::id).map(TagId::value).toList());

        clock.advanceSeconds(61);

        assertEquals(List.of("member"),
                service.activeTags(player, new TagResolutionContext("world", 0, 64, 0))
                        .stream().map(Tag::id).map(TagId::value).toList());
    }

    @Test
    void contextualAutomaticRolesReflectPermissionChangesWithoutCaching() {
        UUID player = UUID.randomUUID();
        java.util.Set<String> granted = new java.util.HashSet<>();
        PermissionService permissions = (uuid, permission) ->
                player.equals(uuid) && granted.contains(permission);
        DefaultTagService service = new DefaultTagService(
                new InMemoryTagRepository(),
                new InMemoryPlayerAssignmentRepository(),
                permissions
        );
        service.create(roleTag("vip", "VIP", 10, "group.vip"));
        service.create(roleTag("staff", "STAFF", 20, "group.staff"));
        TagResolutionContext context = new TagResolutionContext("world", 0, 64, 0);

        granted.add("group.vip");
        assertEquals(List.of("vip"),
                service.activeTags(player, context).stream().map(Tag::id).map(TagId::value).toList());

        granted.remove("group.vip");
        granted.add("group.staff");
        assertEquals(List.of("staff"),
                service.activeTags(player, context).stream().map(Tag::id).map(TagId::value).toList());
    }

    @Test
    void contextualAutomaticRolesUseAscendingTagIdAsTieBreaker() {
        UUID player = UUID.randomUUID();
        PermissionService permissions = (uuid, permission) ->
                player.equals(uuid) && permission.equals("group.staff");
        DefaultTagService service = new DefaultTagService(
                new InMemoryTagRepository(),
                new InMemoryPlayerAssignmentRepository(),
                permissions
        );
        service.create(roleTag("omega", "OMEGA", 50, "group.staff"));
        service.create(roleTag("alpha", "ALPHA", 50, "group.staff"));

        assertEquals(List.of("alpha", "omega"),
                service.activeTags(player, new TagResolutionContext("world", 0, 64, 0))
                        .stream().map(Tag::id).map(TagId::value).toList());
    }

    @Test
    void temporaryAssignmentExpiresAndFallsBack() {
        Instant now=Instant.parse("2026-01-01T00:00:00Z");
        InMemoryTagRepository tags=new InMemoryTagRepository();
        InMemoryPlayerAssignmentRepository assignments=new InMemoryPlayerAssignmentRepository();
        DefaultTagService beforeExpiry=new DefaultTagService(
                tags,assignments,8,new com.ultraop.nametag.api.TagEventBus(),Clock.fixed(now,ZoneOffset.UTC));
        UUID player=UUID.randomUUID();
        beforeExpiry.create(tag("member","MEMBER",10,true)); beforeExpiry.create(tag("vip","VIP",50,true));
        beforeExpiry.assign(player,new TagId("member"));
        beforeExpiry.assignUntil(player,new TagId("vip"),now.plusSeconds(60));
        beforeExpiry.setActive(player,new TagId("vip"));

        DefaultTagService afterExpiry=new DefaultTagService(
                tags,assignments,8,new com.ultraop.nametag.api.TagEventBus(),Clock.fixed(now.plusSeconds(61),ZoneOffset.UTC));
        assertEquals("MEMBER",afterExpiry.activeTag(player).orElseThrow().displayName());
        assertTrue(assignments.find(player).orElseThrow().expirationEpochMillis().isEmpty());
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

        assertEquals(5, events.size());
        assertInstanceOf(com.ultraop.nametag.api.TagEvent.Created.class, events.get(0));
        assertInstanceOf(com.ultraop.nametag.api.TagEvent.Updated.class, events.get(1));
        assertInstanceOf(com.ultraop.nametag.api.TagEvent.AssignmentChanged.class, events.get(2));
        assertInstanceOf(com.ultraop.nametag.api.TagEvent.AssignmentChanged.class, events.get(3));
        assertInstanceOf(com.ultraop.nametag.api.TagEvent.Deleted.class, events.get(4));
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
    void assigningNewItemTagReplacesPreviousItemTagButPreservesNameTag() {
        DefaultTagService service = newService();
        UUID player = UUID.randomUUID();
        service.create(tag("vip", "VIP", 10, true));
        service.create(new Tag(new TagId("diamond"), "DIAMOND", new TagColor.Preset("aqua"), TagStyle.plain(),
                TagEffect.none(), 20, true, true, Map.of("item", "minecraft:diamond")));
        service.create(new Tag(new TagId("lapis"), "LAPIS", new TagColor.Preset("blue"), TagStyle.plain(),
                TagEffect.none(), 30, true, true, Map.of("item", "minecraft:lapis_lazuli")));

        service.assign(player, new TagId("vip"));
        service.assign(player, new TagId("diamond"));
        service.assign(player, new TagId("lapis"));

        assertEquals(List.of("vip", "lapis"),
                service.assignedTags(player).stream().map(Tag::id).map(TagId::value).toList());
    }

    @Test
    void purgeExpiredAssignmentsRemovesExpiredTagImmediately() {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        MutableClock clock = new MutableClock(now);
        InMemoryTagRepository tags = new InMemoryTagRepository();
        InMemoryPlayerAssignmentRepository assignments = new InMemoryPlayerAssignmentRepository();
        DefaultTagService service = new DefaultTagService(
                tags, assignments, 8, new com.ultraop.nametag.api.TagEventBus(), clock
        );
        UUID player = UUID.randomUUID();
        service.create(tag("vip", "VIP", 10, true));
        service.assignUntil(player, new TagId("vip"), now.plusSeconds(60));
        clock.advanceSeconds(61);

        service.purgeExpiredAssignments();

        assertTrue(assignments.find(player).isEmpty());
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

    @Test
    void assigningItemTagReplacesPreviousItemTagButKeepsNameTags() {
        DefaultTagService service = newService();
        UUID player = UUID.randomUUID();

        service.create(tag("vip", "VIP", 50, true));
        service.create(itemTag("diamond", "DIAMOND", "minecraft:diamond"));
        service.create(itemTag("ingot", "INGOT", "minecraft:iron_ingot"));

        service.assign(player, new TagId("vip"));
        service.assign(player, new TagId("diamond"));
        service.assign(player, new TagId("ingot"));

        assertEquals(List.of("vip", "ingot"),
                service.assignedTags(player).stream().map(Tag::id).map(TagId::value).toList());
        assertEquals(new TagId("ingot"), service.activeTag(player).orElseThrow().id());
    }

    @Test
    void legacyMultipleItemTagsAreNormalizedToTheActiveItemTag() {
        InMemoryPlayerAssignmentRepository assignments = new InMemoryPlayerAssignmentRepository();
        InMemoryTagRepository tags = new InMemoryTagRepository();
        DefaultTagService service = new DefaultTagService(tags, assignments);
        UUID player = UUID.randomUUID();

        service.create(itemTag("diamond", "DIAMOND", "minecraft:diamond"));
        service.create(itemTag("ingot", "INGOT", "minecraft:iron_ingot"));
        service.create(tag("vip", "VIP", 50, true));

        assignments.save(new PlayerAssignment(
                player,
                List.of(new TagId("vip"), new TagId("diamond"), new TagId("ingot")),
                new TagId("ingot")
        ));

        assertEquals(List.of("vip", "ingot"),
                service.assignedTags(player).stream().map(Tag::id).map(TagId::value).toList());
        assertEquals(List.of("vip", "ingot"),
                service.activeTags(player, new TagResolutionContext("world", 0, 64, 0))
                        .stream().map(Tag::id).map(TagId::value).toList());
    }

    @Test
    void activeTagsOrdersExplicitActiveFirstAndIncludesOtherAssignments() {
        DefaultTagService service = newService();
        UUID player = UUID.randomUUID();
        service.create(tag("owner", "OWNER", 10, true));
        service.create(tag("vip", "VIP", 50, true));
        service.assign(player, new TagId("owner"));
        service.assign(player, new TagId("vip"));
        service.setActive(player, new TagId("owner"));

        List<Tag> resolved = service.activeTags(player, new TagResolutionContext("world", 0, 64, 0));
        assertEquals(List.of(new TagId("owner"), new TagId("vip")),
                resolved.stream().map(Tag::id).toList());
    }

    @Test
    void activeTagsRespectWorldAndRegionScopes() {
        DefaultTagService service = newService();
        UUID player = UUID.randomUUID();
        service.create(new Tag(new TagId("global"), "GLOBAL", new TagColor.Preset("white"), TagStyle.plain(),
                TagEffect.none(), 1, true, true, Map.of()));
        service.create(new Tag(new TagId("world"), "WORLD", new TagColor.Preset("green"), TagStyle.plain(),
                TagEffect.none(), 20, true, true, Map.of("world", "world_nether")));
        service.create(new Tag(new TagId("spawn"), "SPAWN", new TagColor.Preset("gold"), TagStyle.plain(),
                TagEffect.none(), 30, true, true, Map.of(
                        "world", "world_nether", "region", "spawn",
                        "region.minX", "-10", "region.minY", "0", "region.minZ", "-10",
                        "region.maxX", "10", "region.maxY", "100", "region.maxZ", "10"
                )));
        service.assign(player, new TagId("global"));
        service.assign(player, new TagId("world"));
        service.assign(player, new TagId("spawn"));
        service.setActive(player, new TagId("spawn"));

        List<Tag> inside = service.activeTags(player, new TagResolutionContext("world_nether", 0, 64, 0));
        assertEquals(List.of("spawn", "world", "global"), inside.stream().map(tag -> tag.id().value()).toList());

        List<Tag> outside = service.activeTags(player, new TagResolutionContext("world_nether", 100, 64, 100));
        assertEquals(List.of("world", "global"), outside.stream().map(tag -> tag.id().value()).toList());

        List<Tag> otherWorld = service.activeTags(player, new TagResolutionContext("world", 0, 64, 0));
        assertEquals(List.of("global"), otherWorld.stream().map(tag -> tag.id().value()).toList());
    }

    @Test
    void automaticRolesCanLayerAndRespectContext() {
        UUID player = UUID.randomUUID();
        PermissionService permissions = (uuid, permission) -> player.equals(uuid) &&
                (permission.equals("group.vip") || permission.equals("group.staff"));
        DefaultTagService service = new DefaultTagService(
                new InMemoryTagRepository(), new InMemoryPlayerAssignmentRepository(), permissions);
        service.create(new Tag(new TagId("vip"), "VIP", new TagColor.Preset("red"), TagStyle.plain(),
                TagEffect.none(), 10, true, true, Map.of("auto-permission", "group.vip")));
        service.create(new Tag(new TagId("staff"), "STAFF", new TagColor.Preset("blue"), TagStyle.plain(),
                TagEffect.none(), 20, true, true, Map.of("auto-permission", "group.staff", "world", "staff_world")));

        assertEquals(List.of("vip"), service.activeTags(player, new TagResolutionContext("world", 0, 64, 0))
                .stream().map(tag -> tag.id().value()).toList());
        assertEquals(List.of("staff", "vip"), service.activeTags(player, new TagResolutionContext("staff_world", 0, 64, 0))
                .stream().map(tag -> tag.id().value()).toList());
    }

    private static DefaultTagService newService() {
        return new DefaultTagService(
                new InMemoryTagRepository(),
                new InMemoryPlayerAssignmentRepository()
        );
    }

    private static Tag itemTag(String id, String displayName, String item) {
        return new Tag(
                new TagId(id), displayName, new TagColor.Preset("white"),
                TagStyle.plain(), TagEffect.none(), 10, true, true,
                Map.of("item", item)
        );
    }

    private static Tag roleTag(String id, String displayName, int priority, String permission) {
        return new Tag(
                new TagId(id),
                displayName,
                new TagColor.Preset("red"),
                TagStyle.plain(),
                TagEffect.none(),
                priority,
                true,
                true,
                Map.of("auto-permission", permission)
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

    private static final class MutableClock extends Clock {
        private Instant current;

        private MutableClock(Instant current) {
            this.current = current;
        }

        private void advanceSeconds(long seconds) {
            current = current.plusSeconds(seconds);
        }

        @Override
        public Instant instant() {
            return current;
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }
    }

    private record PlayerAssignmentSnapshot(TagId active, int assignedCount) {}

    private static PlayerAssignmentSnapshot snapshot(com.ultraop.nametag.core.model.PlayerAssignment assignment) {
        return new PlayerAssignmentSnapshot(assignment.activeTagId(), assignment.assignedTagIds().size());
    }

}
