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
    TagEventBus events();

    Tag create(Tag tag);
    Tag update(Tag tag);
    boolean delete(TagId id);
    Optional<Tag> find(TagId id);
    Collection<Tag> list();

    /**
     * Assigns a tag without replacing an existing explicit active tag.
     * If the player has no active tag, the newly assigned tag becomes active.
     */
    PlayerAssignment assign(UUID playerUuid, TagId tagId);

    /**
     * Assigns a tag until the supplied instant. The expiration is stored per tag.
     */
    PlayerAssignment assignUntil(UUID playerUuid, TagId tagId, java.time.Instant expiresAt);

    /**
     * Makes an already-assigned tag the player's explicit active tag.
     */
    PlayerAssignment setActive(UUID playerUuid, TagId tagId);

    PlayerAssignment remove(UUID playerUuid, TagId tagId);
    void clear(UUID playerUuid);
    Optional<Tag> activeTag(UUID playerUuid);

    /**
     * Resolves all simultaneously visible tags for the supplied player context.
     * The first tag is the explicit active tag when one exists; remaining tags are
     * ordered deterministically by priority and id.
     */
    default List<Tag> activeTags(UUID playerUuid, TagResolutionContext context) {
        return activeTag(playerUuid).map(List::of).orElseGet(List::of);
    }

    /** Applies or replaces an effect while preserving all other tag properties. */
    default Tag setEffect(TagId tagId, TagEffect effect) {
        Tag tag = find(tagId).orElseThrow(() -> new IllegalArgumentException("Tag not found: " + tagId.value()));
        if (effect == null) throw new IllegalArgumentException("Effect cannot be null");
        return update(new Tag(tag.id(), tag.displayName(), tag.color(), tag.style(), effect,
                tag.priority(), tag.enabled(), tag.chatEnabled(), tag.metadata()));
    }

    /**
     * Applies or replaces the built-in glitch effect on an existing tag.
     * The tag's other appearance settings remain unchanged.
     */
    default Tag setGlitch(TagId tagId, GlitchSettings settings) {
        return setEffect(tagId, TagEffect.glitch(settings.mode(), settings.intensity(), settings.speedMs()));
    }

    default Tag setGlitch(TagId tagId, GlitchMode mode) {
        return setGlitch(tagId, GlitchSettings.defaults(mode));
    }

    /**
     * Removes the glitch effect while keeping all other tag properties.
     */
    default Tag clearGlitch(TagId tagId) {
        return setEffect(tagId, TagEffect.none());
    }
}
