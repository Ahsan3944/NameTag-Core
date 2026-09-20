package com.ultraop.nametag.common;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

final class StorageMigrator {
    private StorageMigrator() {}

    static Map<String, Object> migrate(
            Map<String, Object> document,
            int currentVersion,
            Map<Integer, StorageMigration> migrations
    ) {
        Objects.requireNonNull(document, "document");
        Objects.requireNonNull(migrations, "migrations");

        Object rawVersion = document.get("schemaVersion");
        if (!(rawVersion instanceof Number number)) {
            throw new IllegalStateException("Storage document has no numeric schemaVersion");
        }

        int version = number.intValue();
        if (version > currentVersion) {
            throw new IllegalStateException(
                    "Storage schema " + version + " is newer than supported schema " + currentVersion
            );
        }

        Map<String, Object> result = new LinkedHashMap<>(document);
        while (version < currentVersion) {
            StorageMigration migration = migrations.get(version);
            if (migration == null) {
                throw new IllegalStateException(
                        "No storage migration registered from schema " + version + " to " + (version + 1)
                );
            }

            Map<String, Object> migrated = Objects.requireNonNull(
                    migration.migrate(new LinkedHashMap<>(result)),
                    "Storage migration returned null"
            );
            Object migratedVersion = migrated.get("schemaVersion");
            if (!(migratedVersion instanceof Number nextNumber)
                    || nextNumber.intValue() != version + 1) {
                throw new IllegalStateException(
                        "Storage migration from schema " + version
                                + " must produce schema " + (version + 1)
                );
            }

            result = new LinkedHashMap<>(migrated);
            version++;
        }

        return result;
    }
}