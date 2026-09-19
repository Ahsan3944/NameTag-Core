package com.ultraop.nametag.common;

import com.ultraop.nametag.api.PlayerAssignmentRepository;
import com.ultraop.nametag.core.model.PlayerAssignment;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryPlayerAssignmentRepository implements PlayerAssignmentRepository {
    private final Map<UUID, PlayerAssignment> values = new ConcurrentHashMap<>();

    @Override public Optional<PlayerAssignment> find(UUID playerUuid) { return Optional.ofNullable(values.get(playerUuid)); }
    @Override public void save(PlayerAssignment assignment) { values.put(assignment.playerUuid(), assignment); }
    @Override public void delete(UUID playerUuid) { values.remove(playerUuid); }
}
