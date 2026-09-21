package com.ultraop.nametag.core.model;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public record PlayerAssignment(
        UUID playerUuid,
        List<TagId> assignedTagIds,
        TagId activeTagId,
        Map<TagId, Long> expirationEpochMillis
) {
    public PlayerAssignment(UUID playerUuid, List<TagId> assignedTagIds, TagId activeTagId) {
        this(playerUuid, assignedTagIds, activeTagId, Map.of());
    }

    public PlayerAssignment {
        Objects.requireNonNull(playerUuid, "playerUuid");
        assignedTagIds = List.copyOf(assignedTagIds == null ? List.of() : assignedTagIds);
        expirationEpochMillis = Map.copyOf(expirationEpochMillis == null ? Map.of() : expirationEpochMillis);
        for (Map.Entry<TagId, Long> entry : expirationEpochMillis.entrySet()) {
            Objects.requireNonNull(entry.getKey(), "expiration tag id");
            Long expiresAt = Objects.requireNonNull(entry.getValue(), "expiration timestamp");
            if (expiresAt <= 0) throw new IllegalArgumentException("Expiration timestamp must be positive");
            if (!assignedTagIds.contains(entry.getKey())) {
                throw new IllegalArgumentException("Expiration can only be defined for an assigned tag: " + entry.getKey().value());
            }
        }
    }
    public boolean hasExpirations() { return !expirationEpochMillis.isEmpty(); }
    public boolean isExpired(TagId tagId, long nowEpochMillis) {
        Long expiresAt = expirationEpochMillis.get(tagId);
        return expiresAt != null && expiresAt <= nowEpochMillis;
    }
}
