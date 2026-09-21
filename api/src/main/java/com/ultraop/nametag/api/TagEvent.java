package com.ultraop.nametag.api;

import com.ultraop.nametag.core.model.PlayerAssignment;
import com.ultraop.nametag.core.model.Tag;

import java.util.UUID;

/**
 * Immutable domain events emitted by TagService mutations.
 */
public sealed interface TagEvent
        permits TagEvent.Created, TagEvent.Updated, TagEvent.Deleted, TagEvent.AssignmentChanged {

    record Created(Tag tag) implements TagEvent {}
    record Updated(Tag previous, Tag current) implements TagEvent {}
    record Deleted(Tag tag) implements TagEvent {}
    record AssignmentChanged(
            UUID playerUuid,
            PlayerAssignment previous,
            PlayerAssignment current
    ) implements TagEvent {}
}
