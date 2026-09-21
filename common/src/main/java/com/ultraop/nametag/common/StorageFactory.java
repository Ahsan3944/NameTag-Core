package com.ultraop.nametag.common;

import com.ultraop.nametag.api.PlayerAssignmentRepository;
import com.ultraop.nametag.api.TagRepository;
import com.ultraop.nametag.core.model.PlayerAssignment;
import com.ultraop.nametag.core.model.Tag;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Objects;

public final class StorageFactory {
    private StorageFactory() {}

    public static StorageRepositories open(
            Path dataDirectory,
            StorageConfiguration configuration
    ) {
        Objects.requireNonNull(dataDirectory, "dataDirectory");
        Objects.requireNonNull(configuration, "configuration");

        if (configuration.type() == StorageType.YAML) {
            return new StorageRepositories(
                    new YamlTagRepository(dataDirectory.resolve("tags.yml")),
                    new YamlPlayerAssignmentRepository(
                            dataDirectory.resolve("assignments.yml"))
            );
        }

        JdbcDatabase database = new JdbcDatabase(configuration);
        database.initialize();

        JdbcTagRepository tags = new JdbcTagRepository(database);
        JdbcPlayerAssignmentRepository assignments =
                new JdbcPlayerAssignmentRepository(database);

        if (configuration.migrateYaml() && !database.migrationComplete()) {
            migrateYamlIfDatabaseIsEmpty(
                    dataDirectory, database, tags, assignments);
            database.markMigrationComplete();
        }

        return new StorageRepositories(tags, assignments);
    }

    private static void migrateYamlIfDatabaseIsEmpty(
            Path dataDirectory,
            JdbcDatabase database,
            TagRepository tags,
            PlayerAssignmentRepository assignments
    ) {
        if (database.count("nametag_tags") != 0
                || database.count("nametag_player_assignments") != 0) {
            return;
        }

        YamlTagRepository yamlTags =
                new YamlTagRepository(dataDirectory.resolve("tags.yml"));
        YamlPlayerAssignmentRepository yamlAssignments =
                new YamlPlayerAssignmentRepository(
                        dataDirectory.resolve("assignments.yml"));

        Collection<Tag> storedTags = yamlTags.findAll();
        Collection<PlayerAssignment> storedAssignments =
                yamlAssignments.findAll();

        for (Tag tag : storedTags) tags.save(tag);
        for (PlayerAssignment assignment : storedAssignments) {
            assignments.save(assignment);
        }
    }

    public record StorageRepositories(
            TagRepository tags,
            PlayerAssignmentRepository assignments
    ) {}
}
