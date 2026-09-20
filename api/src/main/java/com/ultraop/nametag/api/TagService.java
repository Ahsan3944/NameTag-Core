package com.ultraop.nametag.api;

import com.ultraop.nametag.core.model.GlitchMode;
import com.ultraop.nametag.core.model.GlitchSettings;
import com.ultraop.nametag.core.model.PlayerAssignment;
import com.ultraop.nametag.core.model.Tag;
import com.ultraop.nametag.core.model.TagEffect;
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
    void clear(UUID playerUuid);
    Optional<Tag> activeTag(UUID playerUuid);

    /**
     * Applies or replaces the built-in glitch effect on an existing tag.
     * The tag's other appearance settings remain unchanged.
     */
    default Tag setGlitch(TagId tagId, GlitchSettings settings) {
        Tag tag = find(tagId).orElseThrow(() ->
                new IllegalArgumentException("Tag not found: " + tagId.value()));

        return update(new Tag(
                tag.id(),
                tag.displayName(),
                tag.color(),
                tag.style(),
                TagEffect.glitch(settings.mode(), settings.intensity(), settings.speedMs()),
                tag.priority(),
                tag.enabled(),
                tag.chatEnabled(),
                tag.metadata()
        ));
    }

    default Tag setGlitch(TagId tagId, GlitchMode mode) {
        return setGlitch(tagId, GlitchSettings.defaults(mode));
    }

    /**
     * Removes the glitch effect while keeping all other tag properties.
     */
    default Tag clearGlitch(TagId tagId) {
        Tag tag = find(tagId).orElseThrow(() ->
                new IllegalArgumentException("Tag not found: " + tagId.value()));

        return update(new Tag(
                tag.id(),
                tag.displayName(),
                tag.color(),
                tag.style(),
                TagEffect.none(),
                tag.priority(),
                tag.enabled(),
                tag.chatEnabled(),
                tag.metadata()
        ));
    }
}
