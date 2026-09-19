package com.ultraop.nametag.api;

import com.ultraop.nametag.core.model.PlayerAssignment;
import com.ultraop.nametag.core.model.Tag;
import com.ultraop.nametag.core.model.TagId;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface TagService {
    Tag create(Tag tag);
    Tag update(Tag tag);
    boolean delete(TagId id);
    Optional<Tag> find(TagId id);
    Collection<Tag> list();

    PlayerAssignment assign(UUID playerUuid, TagId tagId);
    PlayerAssignment remove(UUID playerUuid, TagId tagId);
    Optional<Tag> activeTag(UUID playerUuid);
}
