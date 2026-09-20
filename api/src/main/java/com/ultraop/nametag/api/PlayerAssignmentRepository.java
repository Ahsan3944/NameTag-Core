package com.ultraop.nametag.api;

import com.ultraop.nametag.core.model.PlayerAssignment;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface PlayerAssignmentRepository {
    Optional<PlayerAssignment> find(UUID playerUuid);
    Collection<PlayerAssignment> findAll();
    void save(PlayerAssignment assignment);
    void delete(UUID playerUuid);
}
