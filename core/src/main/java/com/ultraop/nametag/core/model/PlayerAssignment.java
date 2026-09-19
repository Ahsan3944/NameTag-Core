package com.ultraop.nametag.core.model;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record PlayerAssignment(
        UUID playerUuid,
        List<TagId> assignedTagIds,
        TagId activeTagId
) {
    public PlayerAssignment {
        Objects.requireNonNull(playerUuid, "playerUuid");
        assignedTagIds = List.copyOf(assignedTagIds == null ? List.of() : assignedTagIds);
    }
}
