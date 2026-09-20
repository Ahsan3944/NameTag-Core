package com.ultraop.nametag.common;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.representer.Representer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

final class YamlFileStore {
    static final int CURRENT_SCHEMA_VERSION = 1;

    private final Path file;
    private final Path backup;
    private final Yaml yaml;
    private final Map<Integer, StorageMigration> migrations;

    YamlFileStore(Path file) {
        this(file, Collections.emptyMap());
    }

    YamlFileStore(Path file, Map<Integer, StorageMigration> migrations) {
        this.file = file.toAbsolutePath().normalize();
        this.backup = this.file.resolveSibling(this.file.getFileName() + ".bak");
        this.migrations = Map.copyOf(migrations);

        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        options.setIndent(2);
        options.setWidth(120);

        LoaderOptions loaderOptions = new LoaderOptions();
        this.yaml = new Yaml(
                new SafeConstructor(loaderOptions),
                new Representer(options),
                options,
                loaderOptions
        );
    }

    Map<String, Object> load() {
        if (!Files.exists(file) && !Files.exists(backup)) return emptyDocument();

        RuntimeException primaryFailure = null;
        for (Path candidate : new Path[]{file, backup}) {
            if (!Files.exists(candidate)) continue;
            try {
                Object loaded = yaml.load(Files.readString(candidate, StandardCharsets.UTF_8));
                if (loaded == null) return emptyDocument();

                Map<String, Object> result = requireMap(loaded, candidate.toString());
                result = StorageMigrator.migrate(result, CURRENT_SCHEMA_VERSION, migrations);
                validateSchema(result, candidate);
                return result;
            } catch (IOException | RuntimeException exception) {
                if (exception instanceof RuntimeException runtime) {
                    primaryFailure = runtime;
                }
            }
        }

        throw new IllegalStateException("Unable to load storage file: " + file, primaryFailure);
    }

    void save(Map<String, Object> document) {
        validateSchema(document, file);
        try {
            Files.createDirectories(file.getParent());
            Path temp = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(temp, yaml.dump(document), StandardCharsets.UTF_8);

            if (Files.exists(file)) {
                Files.copy(file, backup, StandardCopyOption.REPLACE_EXISTING);
            }

            try {
                Files.move(
                        temp,
                        file,
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING
                );
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to save storage file: " + file, exception);
        }
    }

    private static Map<String, Object> emptyDocument() {
        Map<String, Object> document = new LinkedHashMap<>();
        document.put("schemaVersion", CURRENT_SCHEMA_VERSION);
        return document;
    }

    private static void validateSchema(Map<String, Object> document, Path source) {
        Object version = document.get("schemaVersion");
        if (!(version instanceof Number number) || number.intValue() != CURRENT_SCHEMA_VERSION) {
            throw new IllegalStateException(
                    "Unsupported storage schema in " + source + ": " + version
            );
        }
    }

    private static Map<String, Object> requireMap(Object value, String name) {
        if (!(value instanceof Map<?, ?> raw)) {
            throw new IllegalStateException("Storage root must be a YAML map: " + name);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : raw.entrySet()) {
            if (!(entry.getKey() instanceof String key)) {
                throw new IllegalStateException("Storage keys must be strings: " + name);
            }
            result.put(key, entry.getValue());
        }
        return result;
    }
}
