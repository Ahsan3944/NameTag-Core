package com.ultraop.nametag.common;

import com.ultraop.nametag.api.ConfigurationReloadResult;
import com.ultraop.nametag.api.ConfigurationReloadService;
import com.ultraop.nametag.api.ConfigurationService;
import com.ultraop.nametag.core.model.NameTagConfiguration;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class DefaultConfigurationService implements ConfigurationService, ConfigurationReloadService {
    private final YamlFileStore store;
    private volatile NameTagConfiguration current;

    public DefaultConfigurationService(Path file) {
        this.store = new YamlFileStore(file);
        this.current = loadOrCreate();
    }

    @Override
    public NameTagConfiguration current() {
        return current;
    }

    /**
     * Loads and validates the replacement snapshot completely before publishing it.
     * The volatile reference is changed only after every validation step succeeds.
     */
    @Override
    public synchronized ConfigurationReloadResult reload() {
        try {
            NameTagConfiguration replacement = loadFromDisk();
            current = replacement;
            return ConfigurationReloadResult.success();
        } catch (RuntimeException exception) {
            return ConfigurationReloadResult.failure(
                    "Configuration reload failed: " + safeMessage(exception)
            );
        }
    }

    private NameTagConfiguration loadOrCreate() {
        Map<String, Object> document = store.load();

        if (document.size() == 1 && document.containsKey("schemaVersion")) {
            NameTagConfiguration defaults = NameTagConfiguration.defaults();
            store.save(toDocument(defaults));
            return defaults;
        }

        return fromDocument(document);
    }

    private NameTagConfiguration loadFromDisk() {
        if (!store.hasAnyFile()) {
            throw new IllegalStateException("Configuration file is missing");
        }
        return fromDocument(store.load());
    }

    private static String safeMessage(RuntimeException exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank()
                ? exception.getClass().getSimpleName()
                : message;
    }

    private static NameTagConfiguration fromDocument(Map<String, Object> document) {
        return new NameTagConfiguration(
                booleanValue(document, "nameplateEnabled", true),
                booleanValue(document, "chatEnabled", true),
                stringValue(document, "chatFormat", NameTagConfiguration.DEFAULT_CHAT_FORMAT),
                intValue(document, "defaultTagPriority", 0),
                booleanValue(document, "defaultTagEnabled", true),
                booleanValue(document, "defaultTagChatEnabled", true),
                intValue(document, "defaultGlitchIntensity", 45),
                intValue(document, "defaultGlitchSpeedMs", 80)
        );
    }

    private static Map<String, Object> toDocument(NameTagConfiguration configuration) {
        Map<String, Object> document = new LinkedHashMap<>();
        document.put("schemaVersion", YamlFileStore.CURRENT_SCHEMA_VERSION);
        document.put("nameplateEnabled", configuration.nameplateEnabled());
        document.put("chatEnabled", configuration.chatEnabled());
        document.put("chatFormat", configuration.chatFormat());
        document.put("defaultTagPriority", configuration.defaultTagPriority());
        document.put("defaultTagEnabled", configuration.defaultTagEnabled());
        document.put("defaultTagChatEnabled", configuration.defaultTagChatEnabled());
        document.put("defaultGlitchIntensity", configuration.defaultGlitchIntensity());
        document.put("defaultGlitchSpeedMs", configuration.defaultGlitchSpeedMs());
        return document;
    }

    private static boolean booleanValue(Map<String, Object> document, String key, boolean fallback) {
        Object value = document.get(key);
        if (value == null) return fallback;
        if (value instanceof Boolean booleanValue) return booleanValue;
        throw new IllegalStateException("Configuration value '" + key + "' must be boolean");
    }

    private static String stringValue(Map<String, Object> document, String key, String fallback) {
        Object value = document.get(key);
        if (value == null) return fallback;
        if (value instanceof String stringValue && !stringValue.isBlank()) return stringValue;
        throw new IllegalStateException("Configuration value '" + key + "' must be a non-blank string");
    }

    private static int intValue(Map<String, Object> document, String key, int fallback) {
        Object value = document.get(key);
        if (value == null) return fallback;
        if (value instanceof Byte || value instanceof Short
                || value instanceof Integer || value instanceof Long) {
            return ((Number) value).intValue();
        }
        throw new IllegalStateException("Configuration value '" + key + "' must be an integer");
    }
}
