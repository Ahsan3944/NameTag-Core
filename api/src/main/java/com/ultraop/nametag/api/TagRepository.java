package com.ultraop.nametag.api;

import com.ultraop.nametag.core.model.Tag;
import com.ultraop.nametag.core.model.TagId;

import java.util.Collection;
import java.util.Optional;

public interface TagRepository {
    Optional<Tag> find(TagId id);
    Collection<Tag> findAll();
    void save(Tag tag);
    void delete(TagId id);
}
