package com.ultraop.nametag.common;

import com.ultraop.nametag.api.TagRepository;
import com.ultraop.nametag.core.model.Tag;
import com.ultraop.nametag.core.model.TagId;
import com.ultraop.nametag.core.validation.TagValidator;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public final class JdbcTagRepository implements TagRepository {
    private final JdbcDatabase database;

    JdbcTagRepository(JdbcDatabase database) {
        this.database = database;
    }

    @Override
    public synchronized Optional<Tag> find(TagId id) {
        try (Connection connection = database.open();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT payload FROM nametag_tags WHERE tag_id = ?")) {
            statement.setString(1, id.value());
            try (ResultSet result = statement.executeQuery()) {
                return result.next()
                        ? Optional.of(JdbcPayloadCodec.decodeTag(
                                id.value(), result.getString(1)))
                        : Optional.empty();
            }
        } catch (SQLException exception) {
            throw failure("Unable to read tag " + id.value(), exception);
        }
    }

    @Override
    public synchronized Collection<Tag> findAll() {
        List<Tag> result = new ArrayList<>();
        try (Connection connection = database.open();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT tag_id, payload FROM nametag_tags ORDER BY tag_id");
             ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                result.add(JdbcPayloadCodec.decodeTag(
                        rows.getString(1), rows.getString(2)));
            }
            return List.copyOf(result);
        } catch (SQLException exception) {
            throw failure("Unable to read tags", exception);
        }
    }

    @Override
    public synchronized void save(Tag tag) {
        TagValidator.validate(tag);
        String payload = JdbcPayloadCodec.encodeTag(tag);
        try (Connection connection = database.open()) {
            connection.setAutoCommit(false);
            try (PreparedStatement delete = connection.prepareStatement(
                    "DELETE FROM nametag_tags WHERE tag_id = ?");
                 PreparedStatement insert = connection.prepareStatement(
                         "INSERT INTO nametag_tags(tag_id, payload) VALUES (?, ?)")) {
                delete.setString(1, tag.id().value());
                delete.executeUpdate();
                insert.setString(1, tag.id().value());
                insert.setString(2, payload);
                insert.executeUpdate();
            }
            connection.commit();
        } catch (SQLException exception) {
            throw failure("Unable to save tag " + tag.id().value(), exception);
        }
    }

    @Override
    public synchronized void delete(TagId id) {
        try (Connection connection = database.open();
             PreparedStatement statement = connection.prepareStatement(
                     "DELETE FROM nametag_tags WHERE tag_id = ?")) {
            statement.setString(1, id.value());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw failure("Unable to delete tag " + id.value(), exception);
        }
    }

    private static IllegalStateException failure(
            String message, SQLException exception
    ) {
        return new IllegalStateException(message, exception);
    }
}
