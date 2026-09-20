package com.ultraop.nametag.common;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class StorageMigrationTest {
    @Test
    void migratesSequentiallyToCurrentSchema() {
        Map<String, Object> legacy = new LinkedHashMap<>();
        legacy.put("schemaVersion", 0);
        legacy.put("legacy", "value");

        Map<String, StorageMigration> unused = Map.of();
        Map<Integer, StorageMigration> migrations = new LinkedHashMap<>();
        migrations.put(0, document -> {
            document.put("migrated", document.remove("legacy"));
            document.put("schemaVersion", 1);
            return document;
        });

        Map<String, Object> result = StorageMigrator.migrate(legacy, 1, migrations);

        assertEquals(1, result.get("schemaVersion"));
        assertEquals("value", result.get("migrated"));
        assertFalse(result.containsKey("legacy"));
    }

    @Test
    void rejectsNewerSchema() {
        Map<String, Object> document = Map.of("schemaVersion", 2);
        assertThrows(
                IllegalStateException.class,
                () -> StorageMigrator.migrate(document, 1, Map.of())
        );
    }

    @Test
    void rejectsMissingMigration() {
        Map<String, Object> document = Map.of("schemaVersion", 0);
        assertThrows(
                IllegalStateException.class,
                () -> StorageMigrator.migrate(document, 1, Map.of())
        );
    }
}