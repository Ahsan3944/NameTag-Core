package com.ultraop.nametag.common;

import com.ultraop.nametag.api.PermissionService;
import com.ultraop.nametag.api.PlayerAssignmentRepository;
import com.ultraop.nametag.api.TagEvent;
import com.ultraop.nametag.api.TagEventBus;
import com.ultraop.nametag.api.TagRepository;
import com.ultraop.nametag.api.TagService;
import com.ultraop.nametag.core.model.PlayerAssignment;
import com.ultraop.nametag.core.model.Tag;
import com.ultraop.nametag.core.model.TagId;
import com.ultraop.nametag.core.model.TagItemSettings;
import com.ultraop.nametag.core.validation.TagValidator;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class DefaultTagService implements TagService {
    private final TagRepository tags;
    private static final int DEFAULT_CACHE_CAPACITY = ActiveTagCache.DEFAULT_CAPACITY;

    private final PlayerAssignmentRepository assignments;
    private final ActiveTagCache activeTagCache;
    private final ContextualTagCache contextualTagCache = new ContextualTagCache();
    private final TagEventBus events;
    private final Clock clock;
    private final PermissionService permissions;

    public DefaultTagService(TagRepository tags, PlayerAssignmentRepository assignments) {
        this(tags, assignments, null, DEFAULT_CACHE_CAPACITY, new TagEventBus(), Clock.systemUTC());
    }

    public DefaultTagService(TagRepository tags, PlayerAssignmentRepository assignments, PermissionService permissions) {
        this(tags, assignments, permissions, DEFAULT_CACHE_CAPACITY, new TagEventBus(), Clock.systemUTC());
    }

    DefaultTagService(
            TagRepository tags,
            PlayerAssignmentRepository assignments,
            int cacheCapacity
    ) {
        this(tags, assignments, null, cacheCapacity, new TagEventBus(), Clock.systemUTC());
    }

    public DefaultTagService(
            TagRepository tags,
            PlayerAssignmentRepository assignments,
            int cacheCapacity,
            TagEventBus events
    ) {
        this(tags, assignments, null, cacheCapacity, events, Clock.systemUTC());
    }

    DefaultTagService(
            TagRepository tags,
            PlayerAssignmentRepository assignments,
            int cacheCapacity,
            TagEventBus events,
            Clock clock
    ) {
        this(tags, assignments, null, cacheCapacity, events, clock);
    }

    DefaultTagService(
            TagRepository tags,
            PlayerAssignmentRepository assignments,
            PermissionService permissions,
            int cacheCapacity,
            TagEventBus events,
            Clock clock
    ) {
        this.tags = Objects.requireNonNull(tags);
        this.assignments = Objects.requireNonNull(assignments);
        this.activeTagCache = new ActiveTagCache(cacheCapacity);
        this.events = Objects.requireNonNull(events);
        this.clock = Objects.requireNonNull(clock);
        this.permissions = permissions;
    }

    @Override
    public TagEventBus events() {
        return events;
    }

    @Override
    public Tag create(Tag tag) {
        TagValidator.validate(tag);
        if (tags.find(tag.id()).isPresent()) {
            throw new IllegalArgumentException("Tag already exists: " + tag.id().value());
        }
        tags.save(tag);
        activeTagCache.clear();
        contextualTagCache.clear();
        events.publish(new TagEvent.Created(tag));
        return tag;
    }

    @Override
    public Tag update(Tag tag) {
        TagValidator.validate(tag);
        Tag previous = tags.find(tag.id()).orElseThrow(() ->
                new IllegalArgumentException("Tag does not exist: " + tag.id().value()));
        tags.save(tag);
        // Any tag update can change priority/enabled resolution for players using other tags.
        activeTagCache.clear();
        contextualTagCache.clear();
        events.publish(new TagEvent.Updated(previous, tag));
        return tag;
    }

    @Override
    public boolean delete(TagId id) {
        Tag deleted = tags.find(id).orElse(null);
        if (deleted == null) return false;

        tags.delete(id);
        activeTagCache.invalidateTag(id);
        contextualTagCache.invalidateTag(id.value());

        List<TagEvent.AssignmentChanged> assignmentEvents = new ArrayList<>();

        for (PlayerAssignment current : assignments.findAll()) {
            if (!current.assignedTagIds().contains(id)) {
                continue;
            }

            ArrayList<TagId> remaining = new ArrayList<>(current.assignedTagIds());
            remaining.remove(id);

            TagId active = Objects.equals(current.activeTagId(), id) ? null : current.activeTagId();
            Map<TagId, Long> expirations = new HashMap<>(current.expirationEpochMillis());
            expirations.remove(id);
            PlayerAssignment updated = new PlayerAssignment(current.playerUuid(), remaining, active, expirations);
            TagValidator.validate(updated);

            if (remaining.isEmpty()) {
                assignments.delete(current.playerUuid());
            } else {
                assignments.save(updated);
            }
            activeTagCache.invalidatePlayer(current.playerUuid());
            contextualTagCache.invalidatePlayer(current.playerUuid());
            assignmentEvents.add(new TagEvent.AssignmentChanged(
                    current.playerUuid(), current, updated));
        }

        for (TagEvent.AssignmentChanged event : assignmentEvents) {
            events.publish(event);
        }
        events.publish(new TagEvent.Deleted(deleted));
        return true;
    }

    @Override
    public Optional<Tag> find(TagId id) {
        return tags.find(id);
    }

    @Override
    public Collection<Tag> list() {
        return List.copyOf(tags.findAll());
    }

    @Override
    public PlayerAssignment assign(UUID playerUuid, TagId tagId) {
        return assignUntil(playerUuid, tagId, null);
    }

    @Override
    public PlayerAssignment assignUntil(UUID playerUuid, TagId tagId, Instant expiresAt) {
        Tag tag = tags.find(tagId).orElseThrow(() ->
                new IllegalArgumentException("Tag not found: " + tagId.value()));
        if (expiresAt != null && !expiresAt.isAfter(clock.instant())) {
            throw new IllegalArgumentException("Expiration must be in the future");
        }

        PlayerAssignment current = assignments.find(playerUuid)
                .orElse(new PlayerAssignment(playerUuid, List.of(), null));
        ArrayList<TagId> ids = new ArrayList<>(current.assignedTagIds());
        boolean newTagHasItem = TagItemSettings.from(tag) != null;
        if (newTagHasItem) {
            ids.removeIf(existingId -> !existingId.equals(tag.id())
                    && tags.find(existingId)
                    .map(existing -> TagItemSettings.from(existing) != null)
                    .orElse(false));
        }
        if (!ids.contains(tag.id())) ids.add(tag.id());

        Map<TagId, Long> expirations = new HashMap<>(current.expirationEpochMillis());
        if (newTagHasItem) {
            expirations.keySet().removeIf(existingId -> !ids.contains(existingId));
        }
        if (expiresAt == null) expirations.remove(tag.id());
        else expirations.put(tag.id(), expiresAt.toEpochMilli());

        TagId active = current.activeTagId() != null ? current.activeTagId() : tag.id();
        if (newTagHasItem && current.activeTagId() != null
                && !ids.contains(current.activeTagId())
                && ids.contains(tag.id())) {
            active = tag.id();
        }
        PlayerAssignment updated = new PlayerAssignment(playerUuid, ids, active, expirations);
        TagValidator.validate(updated);
        assignments.save(updated);
        activeTagCache.invalidatePlayer(playerUuid);
        contextualTagCache.invalidatePlayer(playerUuid);
        if (!updated.equals(current)) {
            events.publish(new TagEvent.AssignmentChanged(playerUuid, current, updated));
        }
        return updated;
    }

    @Override
    public PlayerAssignment setActive(UUID playerUuid, TagId tagId) {
        tags.find(tagId).orElseThrow(() ->
                new IllegalArgumentException("Tag not found: " + tagId.value()));

        PlayerAssignment current = assignments.find(playerUuid)
                .orElse(new PlayerAssignment(playerUuid, List.of(), null));

        if (current.isExpired(tagId, clock.millis())) {
            throw new IllegalArgumentException("Tag assignment has expired: " + tagId.value());
        }

        ArrayList<TagId> ids = new ArrayList<>(current.assignedTagIds());
        if (!ids.contains(tagId)) {
            ids.add(tagId);
        }

        PlayerAssignment updated = new PlayerAssignment(
                playerUuid,
                ids,
                tagId,
                current.expirationEpochMillis()
        );
        TagValidator.validate(updated);
        assignments.save(updated);
        activeTagCache.invalidatePlayer(playerUuid);
        contextualTagCache.invalidatePlayer(playerUuid);
        if (!updated.equals(current)) {
            events.publish(new TagEvent.AssignmentChanged(playerUuid, current, updated));
        }
        return updated;
    }

    @Override
    public PlayerAssignment remove(UUID playerUuid, TagId tagId) {
        PlayerAssignment current = assignments.find(playerUuid)
                .orElse(new PlayerAssignment(playerUuid, List.of(), null));

        ArrayList<TagId> ids = new ArrayList<>(current.assignedTagIds());
        ids.remove(tagId);

        TagId active = Objects.equals(current.activeTagId(), tagId) ? null : current.activeTagId();
        Map<TagId, Long> expirations = new HashMap<>(current.expirationEpochMillis());
        expirations.remove(tagId);
        PlayerAssignment updated = new PlayerAssignment(playerUuid, ids, active, expirations);
        TagValidator.validate(updated);

        if (ids.isEmpty()) {
            assignments.delete(playerUuid);
        } else {
            assignments.save(updated);
        }

        activeTagCache.invalidatePlayer(playerUuid);
        contextualTagCache.invalidatePlayer(playerUuid);
        if (!updated.equals(current)) {
            events.publish(new TagEvent.AssignmentChanged(playerUuid, current, updated));
        }
        return updated;
    }

    @Override
    public void clear(UUID playerUuid) {
        Optional<PlayerAssignment> current = assignments.find(playerUuid);
        assignments.delete(playerUuid);
        activeTagCache.invalidatePlayer(playerUuid);
        contextualTagCache.invalidatePlayer(playerUuid);
        current.ifPresent(previous ->
                events.publish(new TagEvent.AssignmentChanged(
                        playerUuid, previous, new PlayerAssignment(playerUuid, List.of(), null))));
    }

    @Override
    public List<Tag> activeTags(UUID playerUuid, com.ultraop.nametag.api.TagResolutionContext context) {
        Objects.requireNonNull(context, "context");
        List<Tag> cached = contextualTagCache.find(playerUuid, context);
        if (cached != null) return cached;

        Optional<PlayerAssignment> stored = assignments.find(playerUuid);
        List<Tag> resolved;
        boolean cacheable = false;
        if (stored.isPresent()) {
            PlayerAssignment assignment = normalizeItemAssignments(
                    playerUuid, removeExpired(playerUuid, stored.get()));
            resolved = resolveActiveTags(assignment, context);
            cacheable = !assignment.hasExpirations() && !resolved.isEmpty();
            if (resolved.isEmpty()) {
                // Permission-derived roles are intentionally not cached because permission
                // providers can change independently of NameTag-Core mutations.
                resolved = automaticRoleTags(playerUuid, context);
                cacheable = false;
            }
        } else {
            // Automatic roles are intentionally resolved on every request so external
            // permission changes are reflected without a cache invalidation hook.
            resolved = automaticRoleTags(playerUuid, context);
        }

        if (cacheable) {
            contextualTagCache.put(playerUuid, context, resolved);
        }
        return resolved;
    }

    private List<Tag> resolveActiveTags(PlayerAssignment assignment, com.ultraop.nametag.api.TagResolutionContext context) {
        long now = clock.millis();
        List<Tag> candidates = assignment.assignedTagIds().stream()
                .filter(tagId -> !assignment.isExpired(tagId, now))
                .map(tags::find)
                .flatMap(Optional::stream)
                .filter(Tag::enabled)
                .filter(tag -> matchesContext(tag, context))
                .sorted(Comparator.comparingInt(Tag::priority).reversed()
                        .thenComparing(tag -> tag.id().value()))
                .toList();

        if (candidates.isEmpty()) return List.of();
        if (assignment.activeTagId() == null) return candidates;

        Optional<Tag> explicit = candidates.stream()
                .filter(tag -> tag.id().equals(assignment.activeTagId()))
                .findFirst();
        if (explicit.isEmpty()) return candidates;

        ArrayList<Tag> ordered = new ArrayList<>();
        ordered.add(explicit.get());
        candidates.stream()
                .filter(tag -> !tag.id().equals(explicit.get().id()))
                .forEach(ordered::add);
        return List.copyOf(ordered);
    }

    private List<Tag> automaticRoleTags(UUID playerUuid, com.ultraop.nametag.api.TagResolutionContext context) {
        if (permissions == null) return List.of();
        return tags.findAll().stream()
                .filter(Tag::enabled)
                .filter(tag -> tag.metadata().get("auto-permission") != null)
                .filter(tag -> permissions.has(playerUuid, tag.metadata().get("auto-permission")))
                .filter(tag -> matchesContext(tag, context))
                .sorted(Comparator.comparingInt(Tag::priority).reversed()
                        .thenComparing(tag -> tag.id().value()))
                .toList();
    }

    private static boolean matchesContext(Tag tag, com.ultraop.nametag.api.TagResolutionContext context) {
        Map<String, String> metadata = tag.metadata();
        String world = metadata.get("world");
        if (world != null && !world.equals(context.world())) return false;

        String region = metadata.get("region");
        if (region == null) return true;

        try {
            int minX = Integer.parseInt(metadata.get("region.minX"));
            int maxX = Integer.parseInt(metadata.get("region.maxX"));
            int minY = Integer.parseInt(metadata.get("region.minY"));
            int maxY = Integer.parseInt(metadata.get("region.maxY"));
            int minZ = Integer.parseInt(metadata.get("region.minZ"));
            int maxZ = Integer.parseInt(metadata.get("region.maxZ"));
            return context.x() >= Math.min(minX, maxX) && context.x() <= Math.max(minX, maxX)
                    && context.y() >= Math.min(minY, maxY) && context.y() <= Math.max(minY, maxY)
                    && context.z() >= Math.min(minZ, maxZ) && context.z() <= Math.max(minZ, maxZ);
        } catch (NumberFormatException | NullPointerException ignored) {
            return false;
        }
    }

    @Override
    public List<Tag> assignedTags(UUID playerUuid) {
        Optional<PlayerAssignment> stored = assignments.find(playerUuid);
        if (stored.isEmpty()) return List.of();

        PlayerAssignment assignment = normalizeItemAssignments(
                playerUuid, removeExpired(playerUuid, stored.get()));
        return assignment.assignedTagIds().stream()
                .filter(tagId -> !assignment.isExpired(tagId, clock.millis()))
                .map(tags::find)
                .flatMap(Optional::stream)
                .toList();
    }

    @Override
    public Optional<Tag> activeTag(UUID playerUuid) {
        Optional<Optional<Tag>> cached = activeTagCache.findCached(playerUuid);
        if (cached.isPresent()) return cached.orElseThrow();

        Optional<PlayerAssignment> stored = assignments.find(playerUuid);
        if (stored.isEmpty()) {
            return automaticRoleTag(playerUuid);
        }

        PlayerAssignment assignment = normalizeItemAssignments(
                playerUuid, removeExpired(playerUuid, stored.get()));
        if (assignment.hasExpirations()) return resolveActiveTag(assignment);

        Optional<Tag> resolved = resolveActiveTag(assignment);
        if (resolved.isPresent()) {
            activeTagCache.put(playerUuid, resolved);
            return resolved;
        }
        return automaticRoleTag(playerUuid);
    }

    private Optional<Tag> automaticRoleTag(UUID playerUuid) {
        if (permissions == null) return Optional.empty();
        return tags.findAll().stream()
                .filter(Tag::enabled)
                .filter(tag -> tag.metadata().get("auto-permission") != null)
                .filter(tag -> permissions.has(playerUuid, tag.metadata().get("auto-permission")))
                .max(Comparator.comparingInt(Tag::priority)
                        .thenComparing(tag -> tag.id().value(), Comparator.reverseOrder()));
    }

    @Override
    public void purgeExpiredAssignments() {
        long now = clock.millis();
        for (PlayerAssignment assignment : assignments.findAll()) {
            if (assignment.assignedTagIds().stream().anyMatch(id -> assignment.isExpired(id, now))) {
                removeExpired(assignment.playerUuid(), assignment);
            }
        }
    }

    private PlayerAssignment normalizeItemAssignments(UUID playerUuid, PlayerAssignment assignment) {
        List<TagId> itemIds = assignment.assignedTagIds().stream()
                .filter(id -> tags.find(id)
                        .map(tag -> TagItemSettings.from(tag) != null)
                        .orElse(false))
                .toList();
        if (itemIds.size() <= 1) return assignment;

        TagId keep = itemIds.stream()
                .filter(id -> Objects.equals(id, assignment.activeTagId()))
                .findFirst()
                .orElse(itemIds.get(itemIds.size() - 1));

        List<TagId> remaining = assignment.assignedTagIds().stream()
                .filter(id -> !itemIds.contains(id) || id.equals(keep))
                .toList();

        Map<TagId, Long> expirations = new HashMap<>();
        for (Map.Entry<TagId, Long> entry : assignment.expirationEpochMillis().entrySet()) {
            if (remaining.contains(entry.getKey())) expirations.put(entry.getKey(), entry.getValue());
        }

        TagId active = remaining.contains(assignment.activeTagId())
                ? assignment.activeTagId()
                : keep;
        PlayerAssignment updated = new PlayerAssignment(
                playerUuid, remaining, active, expirations
        );
        assignments.save(updated);
        activeTagCache.invalidatePlayer(playerUuid);
        contextualTagCache.invalidatePlayer(playerUuid);
        return updated;
    }

    private PlayerAssignment removeExpired(UUID playerUuid, PlayerAssignment assignment) {
        long now = clock.millis();
        List<TagId> remaining = assignment.assignedTagIds().stream()
                .filter(tagId -> !assignment.isExpired(tagId, now))
                .toList();
        if (remaining.size() == assignment.assignedTagIds().size()) return assignment;

        Map<TagId, Long> expirations = new HashMap<>();
        for (Map.Entry<TagId, Long> entry : assignment.expirationEpochMillis().entrySet()) {
            if (remaining.contains(entry.getKey()) && entry.getValue() > now) expirations.put(entry.getKey(), entry.getValue());
        }
        TagId active = remaining.contains(assignment.activeTagId()) ? assignment.activeTagId() : null;
        PlayerAssignment updated = new PlayerAssignment(playerUuid, remaining, active, expirations);
        if (remaining.isEmpty()) assignments.delete(playerUuid);
        else assignments.save(updated);
        activeTagCache.invalidatePlayer(playerUuid);
        contextualTagCache.invalidatePlayer(playerUuid);
        events.publish(new TagEvent.AssignmentChanged(playerUuid, assignment, updated));
        return updated;
    }

    private Optional<Tag> resolveActiveTag(PlayerAssignment assignment) {
        long now = clock.millis();
        if (assignment.activeTagId() != null && !assignment.isExpired(assignment.activeTagId(), now)) {
            Optional<Tag> explicit = tags.find(assignment.activeTagId()).filter(Tag::enabled);
            if (explicit.isPresent()) return explicit;
        }
        return assignment.assignedTagIds().stream()
                .filter(tagId -> !assignment.isExpired(tagId, now))
                .map(tags::find).flatMap(Optional::stream).filter(Tag::enabled)
                .max(Comparator.comparingInt(Tag::priority).thenComparing(tag -> tag.id().value(), Comparator.reverseOrder()));
    }
}
