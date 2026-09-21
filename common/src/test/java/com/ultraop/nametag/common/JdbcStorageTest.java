package com.ultraop.nametag.common;

import com.ultraop.nametag.core.model.PlayerAssignment;
import com.ultraop.nametag.core.model.Tag;
import com.ultraop.nametag.core.model.TagColor;
import com.ultraop.nametag.core.model.TagEffect;
import com.ultraop.nametag.core.model.TagId;
import com.ultraop.nametag.core.model.TagStyle;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JdbcStorageTest {
    @TempDir
    Path tempDir;

    @Test
    void sqliteRoundTripPreservesTagsAndAssignmentsAcrossRestart() {
        StorageConfiguration configuration = StorageConfiguration.sqlite(
                tempDir.resolve("nametag.db"), false
        );
        StorageFactory.StorageRepositories first =
                StorageFactory.open(tempDir, configuration);

        UUID player = UUID.randomUUID();
        Tag tag = tag("vip", "VIP", 50, Map.of(
                "auto-permission", "group.vip",
                "owner", "ultra"
        ));
        PlayerAssignment assignment = new PlayerAssignment(
                player,
                List.of(tag.id()),
                tag.id(),
                Map.of(
                        tag.id(),
                        Instant.parse("2027-01-01T00:00:00Z").toEpochMilli()
                )
        );

        first.tags().save(tag);
        first.assignments().save(assignment);

        StorageFactory.StorageRepositories restarted =
                StorageFactory.open(tempDir, configuration);
        assertEquals(tag, restarted.tags().find(tag.id()).orElseThrow());
        assertEquals(
                assignment,
                restarted.assignments().find(player).orElseThrow()
        );
    }

    @Test
    void yamlMigrationRunsOnceAndPreservesExistingData() {
        UUID player = UUID.randomUUID();
        Tag tag = tag("member", "MEMBER", 10, Map.of());

        new YamlTagRepository(tempDir.resolve("tags.yml")).save(tag);
        new YamlPlayerAssignmentRepository(tempDir.resolve("assignments.yml"))
                .save(new PlayerAssignment(player, List.of(tag.id()), tag.id()));

        StorageConfiguration configuration = StorageConfiguration.sqlite(
                tempDir.resolve("nametag.db"), true
        );
        StorageFactory.StorageRepositories migrated =
                StorageFactory.open(tempDir, configuration);

        assertEquals(tag, migrated.tags().find(tag.id()).orElseThrow());
        assertEquals(
                tag.id(),
                migrated.assignments().find(player).orElseThrow().activeTagId()
        );

        new YamlTagRepository(tempDir.resolve("tags.yml")).delete(tag.id());
        StorageFactory.StorageRepositories reopened =
                StorageFactory.open(tempDir, configuration);

        assertTrue(reopened.tags().find(tag.id()).isPresent());
        assertTrue(reopened.assignments().find(player).isPresent());
    }

    @Test
    void sqliteDeleteRemovesRows() {
        StorageFactory.StorageRepositories repositories =
                StorageFactory.open(
                        tempDir,
                        StorageConfiguration.sqlite(
                                tempDir.resolve("delete.db"), false
                        )
                );
        Tag tag = tag("delete-me", "DELETE", 0, Map.of());
        UUID player = UUID.randomUUID();

        repositories.tags().save(tag);
        repositories.assignments().save(
                new PlayerAssignment(player, List.of(tag.id()), tag.id())
        );
        repositories.tags().delete(tag.id());
        repositories.assignments().delete(player);

        assertTrue(repositories.tags().findAll().isEmpty());
        assertTrue(repositories.assignments().findAll().isEmpty());
    }

    private static Tag tag(
            String id,
            String name,
            int priority,
            Map<String, String> metadata
    ) {
        return new Tag(
                new TagId(id),
                name,
                new TagColor.Gradient(
                        new TagColor.Rgb(10, 20, 30),
                        new TagColor.Rgb(200, 210, 220)
                ),
                new TagStyle(true, false, true, false, false),
                new TagEffect("pulse", Map.of("speed", "80")),
                priority,
                true,
                true,
                metadata
        );
    }
}
