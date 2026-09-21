package com.ultraop.nametag.common;

import com.ultraop.nametag.api.PlayerAssignmentRepository;
import com.ultraop.nametag.api.TagEvent;
import com.ultraop.nametag.api.TagEventBus;
import com.ultraop.nametag.api.TagRepository;
import com.ultraop.nametag.api.TagService;
import com.ultraop.nametag.core.model.PlayerAssignment;
import com.ultraop.nametag.core.model.Tag;
import com.ultraop.nametag.core.model.TagId;
import com.ultraop.nametag.core.validation.TagValidator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class DefaultTagService implements TagService {
    private final TagRepository tags;
    private static final int DEFAULT_CACHE_CAPACITY = ActiveTagCache.DEFAULT_CAPACITY;

    private final PlayerAssignmentRepository assignments;
    private final ActiveTagCache activeTagCache;
    private final TagEventBus events;

    public DefaultTagService(TagRepository tags, PlayerAssignmentRepository assignments) {
        this(tags, assignments, DEFAULT_CACHE_CAPACITY, new TagEventBus());
    }

    DefaultTagService(
            TagRepository tags,
            PlayerAssignmentRepository assignments,
            int cacheCapacity
    ) {
        this(tags, assignments, cacheCapacity, new TagEventBus());
    }

    public DefaultTagService(
            TagRepository tags,
            PlayerAssignmentRepository assignments,
            int cacheCapacity,
            TagEventBus events
    ) {
        this.tags = Objects.requireNonNull(tags);
        this.assignments = Objects.requireNonNull(assignments);
        this.activeTagCache = new ActiveTagCache(cacheCapacity);
        this.events = Objects.requireNonNull(events);
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
            PlayerAssignment updated = new PlayerAssignment(current.playerUuid(), remaining, active);
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
        Tag tag = tags.find(tagId).orElseThrow(() ->
                new IllegalArgumentException("Tag not found: " + tagId.value()));

        PlayerAssignment current = assignments.find(playerUuid)
                .orElse(new PlayerAssignment(playerUuid, List.of(), null));

        ArrayList<TagId> ids = new ArrayList<>(current.assignedTagIds());
        if (!ids.contains(tag.id())) ids.add(tag.id());

        TagId active = current.activeTagId() != null ? current.activeTagId() : tag.id();
        PlayerAssignment updated = new PlayerAssignment(playerUuid, ids, active);
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

        PlayerAssignment updated = new PlayerAssignment(
                playerUuid,
                current.assignedTagIds(),
                tagId
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
        PlayerAssignment updated = new PlayerAssignment(playerUuid, ids, active);
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
        if (activeTagCache.contains(playerUuid)) {
            return activeTagCache.get(playerUuid);
        }

        Optional<Tag> resolved = assignments.find(playerUuid).flatMap(this::resolveActiveTag);
        activeTagCache.put(playerUuid, resolved);
        return resolved;
    }

    private Optional<Tag> resolveActiveTag(PlayerAssignment assignment) {
        if (assignment.activeTagId() != null) {
            Optional<Tag> explicit = tags.find(assignment.activeTagId())
                    .filter(Tag::enabled);
            if (explicit.isPresent()) {
                return explicit;
            }
        }

        return assignment.assignedTagIds().stream()
                .map(tags::find)
                .flatMap(Optional::stream)
                .filter(Tag::enabled)
                .max(Comparator
                        .comparingInt(Tag::priority)
                        .thenComparing(tag -> tag.id().value(), Comparator.reverseOrder()));
    }
}
