package com.ultraop.nametag.common;

import com.ultraop.nametag.api.PlayerAssignmentRepository;
import com.ultraop.nametag.api.TagRepository;
import com.ultraop.nametag.api.TagService;
import com.ultraop.nametag.core.model.PlayerAssignment;
import com.ultraop.nametag.core.model.Tag;
import com.ultraop.nametag.core.model.TagId;
import com.ultraop.nametag.core.validation.TagValidator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class DefaultTagService implements TagService {
    private final TagRepository tags;
    private final PlayerAssignmentRepository assignments;

    public DefaultTagService(TagRepository tags, PlayerAssignmentRepository assignments) {
        this.tags = Objects.requireNonNull(tags);
        this.assignments = Objects.requireNonNull(assignments);
    }

    @Override
    public Tag create(Tag tag) {
        TagValidator.validate(tag);
        if (tags.find(tag.id()).isPresent()) {
            throw new IllegalArgumentException("Tag already exists: " + tag.id().value());
        }
        tags.save(tag);
        return tag;
    }

    @Override
    public Tag update(Tag tag) {
        TagValidator.validate(tag);
        if (tags.find(tag.id()).isEmpty()) {
            throw new IllegalArgumentException("Tag does not exist: " + tag.id().value());
        }
        tags.save(tag);
        return tag;
    }

    @Override
    public boolean delete(TagId id) {
        if (tags.find(id).isEmpty()) return false;
        tags.delete(id);
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

        PlayerAssignment updated = new PlayerAssignment(playerUuid, ids, tag.id());
        TagValidator.validate(updated);
        assignments.save(updated);
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
        assignments.save(updated);
        return updated;
    }

    @Override
    public void clear(UUID playerUuid) {
        assignments.delete(playerUuid);
    }

    @Override
    public Optional<Tag> activeTag(UUID playerUuid) {
        return assignments.find(playerUuid)
                .flatMap(a -> Optional.ofNullable(a.activeTagId()))
                .flatMap(tags::find);
    }
}
