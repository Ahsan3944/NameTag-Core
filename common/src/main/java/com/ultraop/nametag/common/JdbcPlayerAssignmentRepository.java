package com.ultraop.nametag.common;

import com.ultraop.nametag.api.PlayerAssignmentRepository;
import com.ultraop.nametag.core.model.PlayerAssignment;
import com.ultraop.nametag.core.validation.TagValidator;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class JdbcPlayerAssignmentRepository implements PlayerAssignmentRepository {
    private final JdbcDatabase database;

    JdbcPlayerAssignmentRepository(JdbcDatabase database) {
        this.database = database;
    }

    @Override
    public synchronized Optional<PlayerAssignment> find(UUID playerUuid) {
        try (Connection connection = database.open();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT payload FROM nametag_player_assignments WHERE player_uuid = ?")) {
            statement.setString(1, playerUuid.toString());
            try (ResultSet result = statement.executeQuery()) {
                return result.next()
                        ? Optional.of(JdbcPayloadCodec.decodeAssignment(
                                playerUuid, result.getString(1)))
                        : Optional.empty();
            }
        } catch (SQLException exception) {
            throw failure(
                    "Unable to read player assignment " + playerUuid, exception);
        }
    }

    @Override
    public synchronized Collection<PlayerAssignment> findAll() {
        List<PlayerAssignment> result = new ArrayList<>();
        try (Connection connection = database.open();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT player_uuid, payload " +
                             "FROM nametag_player_assignments ORDER BY player_uuid");
             ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                result.add(JdbcPayloadCodec.decodeAssignment(
                        UUID.fromString(rows.getString(1)), rows.getString(2)));
            }
            return List.copyOf(result);
        } catch (SQLException exception) {
            throw failure("Unable to read player assignments", exception);
        }
    }

    @Override
    public synchronized void save(PlayerAssignment assignment) {
        TagValidator.validate(assignment);
        String payload = JdbcPayloadCodec.encodeAssignment(assignment);
        try (Connection connection = database.open()) {
            connection.setAutoCommit(false);
            try (PreparedStatement delete = connection.prepareStatement(
                    "DELETE FROM nametag_player_assignments WHERE player_uuid = ?");
                 PreparedStatement insert = connection.prepareStatement(
                         "INSERT INTO nametag_player_assignments(player_uuid, payload) " +
                                 "VALUES (?, ?)")) {
                delete.setString(1, assignment.playerUuid().toString());
                delete.executeUpdate();
                insert.setString(1, assignment.playerUuid().toString());
                insert.setString(2, payload);
                insert.executeUpdate();
            }
            connection.commit();
        } catch (SQLException exception) {
            throw failure(
                    "Unable to save player assignment " +
                            assignment.playerUuid(), exception);
        }
    }

    @Override
    public synchronized void delete(UUID playerUuid) {
        try (Connection connection = database.open();
             PreparedStatement statement = connection.prepareStatement(
                     "DELETE FROM nametag_player_assignments WHERE player_uuid = ?")) {
            statement.setString(1, playerUuid.toString());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw failure(
                    "Unable to delete player assignment " + playerUuid, exception);
        }
    }

    private static IllegalStateException failure(
            String message, SQLException exception
    ) {
        return new IllegalStateException(message, exception);
    }
}
