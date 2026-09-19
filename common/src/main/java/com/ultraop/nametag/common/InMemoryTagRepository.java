package com.ultraop.nametag.common;

import com.ultraop.nametag.api.TagRepository;
import com.ultraop.nametag.core.model.Tag;
import com.ultraop.nametag.core.model.TagId;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryTagRepository implements TagRepository {
    private final Map<TagId, Tag> values = new ConcurrentHashMap<>();

    @Override public Optional<Tag> find(TagId id) { return Optional.ofNullable(values.get(id)); }
    @Override public Collection<Tag> findAll() { return ListCopy.copy(values.values()); }
    @Override public void save(Tag tag) { values.put(tag.id(), tag); }
    @Override public void delete(TagId id) { values.remove(id); }

    private static final class ListCopy {
        private static <T> Collection<T> copy(Collection<T> source) {
            return java.util.List.copyOf(source);
        }
    }
}
