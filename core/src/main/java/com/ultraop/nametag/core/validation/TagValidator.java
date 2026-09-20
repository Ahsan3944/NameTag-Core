package com.ultraop.nametag.core.validation;

import com.ultraop.nametag.core.model.PlayerAssignment;
import com.ultraop.nametag.core.model.Tag;

import java.util.HashSet;
import java.util.Objects;

public final class TagValidator {
    private TagValidator() {}

    public static void validate(Tag tag) {
        Objects.requireNonNull(tag, "tag");
        if (tag.priority() < 0) {
            throw new IllegalArgumentException("Tag priority cannot be negative");
        }
    }

    public static void validate(PlayerAssignment assignment) {
        Objects.requireNonNull(assignment, "assignment");

        HashSet<?> unique = new HashSet<>(assignment.assignedTagIds());
        if (unique.size() != assignment.assignedTagIds().size()) {
            throw new IllegalArgumentException("Player assignment cannot contain duplicate tag IDs");
        }

        if (assignment.activeTagId() != null
                && !assignment.assignedTagIds().contains(assignment.activeTagId())) {
            throw new IllegalArgumentException("Active tag must be assigned to the player");
        }
    }
}
