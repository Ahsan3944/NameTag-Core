package com.ultraop.nametag.common;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public record StorageConfiguration(
        StorageType type,
        String jdbcUrl,
        String username,
        String password,
        boolean migrateYaml
) {
    public StorageConfiguration {
        Objects.requireNonNull(type, "type");
        jdbcUrl = jdbcUrl == null ? "" : jdbcUrl.trim();
        username = username == null ? "" : username;
        password = password == null ? "" : password;
        if (type != StorageType.YAML && jdbcUrl.isBlank()) {
            throw new IllegalArgumentException("JDBC URL is required for " + type);
        }
    }

    public static StorageConfiguration yaml() {
        return new StorageConfiguration(StorageType.YAML, "", "", "", false);
    }

    public static StorageConfiguration sqlite(Path databaseFile, boolean migrateYaml) {
        Objects.requireNonNull(databaseFile, "databaseFile");
        return new StorageConfiguration(
                StorageType.SQLITE,
                "jdbc:sqlite:" + databaseFile.toAbsolutePath().normalize(),
                "",
                "",
                migrateYaml
        );
    }

    public static StorageConfiguration loadOrCreate(Path file, Path dataDirectory) {
        Objects.requireNonNull(file, "file");
        Objects.requireNonNull(dataDirectory, "dataDirectory");

        YamlFileStore store = new YamlFileStore(file);
        Map<String, Object> document = store.load();
        if (document.size() == 1 && document.containsKey("schemaVersion")) {
            Map<String, Object> defaults = defaults();
            store.save(defaults);
            return fromDocument(defaults, dataDirectory);
        }
        return fromDocument(document, dataDirectory);
    }

    private static Map<String, Object> defaults() {
        Map<String, Object> document = new LinkedHashMap<>();
        document.put("schemaVersion", YamlFileStore.CURRENT_SCHEMA_VERSION);
        document.put("type", "yaml");
        document.put("jdbcUrl", "");
        document.put("username", "");
        document.put("password", "");
        document.put("migrateYaml", true);
        return document;
    }

    private static StorageConfiguration fromDocument(Map<String, Object> document, Path dataDirectory) {
        String rawType = stringValue(document, "type", "yaml").toLowerCase(Locale.ROOT);
        StorageType type;
        try {
            type = StorageType.valueOf(rawType.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(
                    "Storage type must be yaml, sqlite, mysql, mariadb or postgresql",
                    exception
            );
        }

        String jdbcUrl = stringValue(document, "jdbcUrl", "");
        if (type == StorageType.SQLITE && jdbcUrl.isBlank()) {
            jdbcUrl = "jdbc:sqlite:" + dataDirectory.resolve("nametag.db")
                    .toAbsolutePath().normalize();
        }

        return new StorageConfiguration(
                type,
                jdbcUrl,
                stringValue(document, "username", ""),
                stringValue(document, "password", ""),
                booleanValue(document, "migrateYaml", true)
        );
    }

    private static String stringValue(Map<String, Object> document, String key, String fallback) {
        Object value = document.get(key);
        if (value == null) return fallback;
        if (value instanceof String string) return string;
        throw new IllegalStateException(
                "Storage configuration value '" + key + "' must be a string"
        );
    }

    private static boolean booleanValue(Map<String, Object> document, String key, boolean fallback) {
        Object value = document.get(key);
        if (value == null) return fallback;
        if (value instanceof Boolean booleanValue) return booleanValue;
        throw new IllegalStateException(
                "Storage configuration value '" + key + "' must be boolean"
        );
    }
}
