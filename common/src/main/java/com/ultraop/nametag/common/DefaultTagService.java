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
        events.publish(new TagEvent.Updated(previous, tag));
        return tag;
    }

    @Override
    public boolean delete(TagId id) {
        Tag deleted = tags.find(id).orElse(null);
        if (deleted == null) return false;

        tags.delete(id);
        activeTagCache.invalidateTag(id);

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
        if (!ids.contains(tag.id())) ids.add(tag.id());

        Map<TagId, Long> expirations = new HashMap<>(current.expirationEpochMillis());
        if (expiresAt == null) expirations.remove(tag.id());
        else expirations.put(tag.id(), expiresAt.toEpochMilli());

        TagId active = current.activeTagId() != null ? current.activeTagId() : tag.id();
        PlayerAssignment updated = new PlayerAssignment(playerUuid, ids, active, expirations);
        TagValidator.validate(updated);
        assignments.save(updated);
        activeTagCache.invalidatePlayer(playerUuid);
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
                .orElseThrow(() -> new IllegalArgumentException(
                        "Player has no assigned tags: " + playerUuid));

        if (!current.assignedTagIds().contains(tagId)) {
            throw new IllegalArgumentException(
                    "Tag is not assigned to player: " + tagId.value());
        }
        if (current.isExpired(tagId, clock.millis())) {
            throw new IllegalArgumentException("Tag assignment has expired: " + tagId.value());
        }

        PlayerAssignment updated = new PlayerAssignment(
                playerUuid,
                current.assignedTagIds(),
                tagId,
                current.expirationEpochMillis()
        );
        TagValidator.validate(updated);
        assignments.save(updated);
        activeTagCache.invalidatePlayer(playerUuid);
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
        if (!updated.equals(current)) {
            events.publish(new TagEvent.AssignmentChanged(playerUuid, current, updated));
        }
        return updated;
    }

    @Override
    public void clear(UUID playerUuid) {
        Optional<PlayerAssignment> current = assignments.find(playerUuid);
        assignments.delete(playerUuid);
        activeTagCache.put(playerUuid, Optional.empty());
        current.ifPresent(previous ->
                events.publish(new TagEvent.AssignmentChanged(
                        playerUuid, previous, new PlayerAssignment(playerUuid, List.of(), null))));
    }

    @Override
    public Optional<Tag> activeTag(UUID playerUuid) {
        Optional<Optional<Tag>> cached = activeTagCache.findCached(playerUuid);
        if (cached.isPresent()) return cached.orElseThrow();

        Optional<PlayerAssignment> stored = assignments.find(playerUuid);
        if (stored.isEmpty()) {
            return automaticRoleTag(playerUuid);
        }

        PlayerAssignment assignment = removeExpired(playerUuid, stored.get());
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
