package com.ultraop.nametag.common;

import com.ultraop.nametag.core.model.PlayerAssignment;
import com.ultraop.nametag.core.model.Tag;
import com.ultraop.nametag.core.model.TagColor;
import com.ultraop.nametag.core.model.TagEffect;
import com.ultraop.nametag.core.model.TagId;
import com.ultraop.nametag.core.model.TagStyle;
import com.ultraop.nametag.core.validation.TagValidator;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.representer.Representer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

final class JdbcPayloadCodec {
    private JdbcPayloadCodec() {}

    static String encodeTag(Tag tag) {
        TagValidator.validate(tag);
        return yaml().dump(encodeTagMap(tag));
    }

    static Tag decodeTag(String id, String payload) {
        return decodeTagMap(id, requireMap(yaml().load(payload), "tag payload"));
    }

    static String encodeAssignment(PlayerAssignment assignment) {
        TagValidator.validate(assignment);
        Map<String, Object> document = new LinkedHashMap<>();
        document.put("assignedTagIds",
                assignment.assignedTagIds().stream().map(TagId::value).toList());
        document.put("activeTagId",
                assignment.activeTagId() == null ? null : assignment.activeTagId().value());
        Map<String, Long> expirations = new LinkedHashMap<>();
        assignment.expirationEpochMillis()
                .forEach((id, value) -> expirations.put(id.value(), value));
        document.put("expirationEpochMillis", expirations);
        return yaml().dump(document);
    }

    static PlayerAssignment decodeAssignment(UUID playerUuid, String payload) {
        Map<String, Object> document = requireMap(yaml().load(payload), "assignment payload");
        List<TagId> assigned = new ArrayList<>();
        Object rawAssigned = document.get("assignedTagIds");
        if (rawAssigned instanceof List<?> list) {
            for (Object value : list) assigned.add(new TagId(String.valueOf(value)));
        } else if (rawAssigned != null) {
            throw new IllegalArgumentException("assignedTagIds must be a list");
        }

        TagId active = document.get("activeTagId") == null
                ? null
                : new TagId(String.valueOf(document.get("activeTagId")));

        Map<TagId, Long> expirations = new LinkedHashMap<>();
        Object rawExpirations = document.get("expirationEpochMillis");
        if (rawExpirations instanceof Map<?, ?> rawMap) {
            for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
                if (!(entry.getValue() instanceof Number number)) {
                    throw new IllegalArgumentException(
                            "expirationEpochMillis values must be numbers"
                    );
                }
                expirations.put(
                        new TagId(String.valueOf(entry.getKey())),
                        number.longValue()
                );
            }
        } else if (rawExpirations != null) {
            throw new IllegalArgumentException(
                    "expirationEpochMillis must be a map"
            );
        }

        PlayerAssignment assignment = new PlayerAssignment(
                playerUuid, assigned, active, expirations
        );
        TagValidator.validate(assignment);
        return assignment;
    }

    private static Map<String, Object> encodeTagMap(Tag tag) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("displayName", tag.displayName());
        result.put("color", encodeColor(tag.color()));

        Map<String, Object> style = new LinkedHashMap<>();
        style.put("bold", tag.style().bold());
        style.put("italic", tag.style().italic());
        style.put("underlined", tag.style().underlined());
        style.put("strikethrough", tag.style().strikethrough());
        style.put("obfuscated", tag.style().obfuscated());
        result.put("style", style);

        Map<String, Object> effect = new LinkedHashMap<>();
        effect.put("id", tag.effect().id());
        effect.put("configuration", tag.effect().configuration());
        result.put("effect", effect);

        result.put("priority", tag.priority());
        result.put("enabled", tag.enabled());
        result.put("chatEnabled", tag.chatEnabled());
        result.put("metadata", tag.metadata());
        return result;
    }

    private static Tag decodeTagMap(String id, Map<String, Object> map) {
        Map<String, Object> styleMap = requireMap(map.get("style"), "style");
        TagStyle style = new TagStyle(
                bool(styleMap, "bold"),
                bool(styleMap, "italic"),
                bool(styleMap, "underlined"),
                bool(styleMap, "strikethrough"),
                bool(styleMap, "obfuscated")
        );

        Map<String, Object> effectMap =
                requireMap(map.get("effect"), "effect");
        Map<String, Object> rawConfig =
                requireMap(effectMap.get("configuration"), "effect.configuration");
        Map<String, String> config = new LinkedHashMap<>();
        rawConfig.forEach((key, value) -> config.put(key, String.valueOf(value)));

        Map<String, Object> rawMetadata =
                requireMap(map.get("metadata"), "metadata");
        Map<String, String> metadata = new LinkedHashMap<>();
        rawMetadata.forEach((key, value) -> metadata.put(key, String.valueOf(value)));

        Tag tag = new Tag(
                new TagId(id),
                string(map, "displayName"),
                decodeColor(requireMap(map.get("color"), "color")),
                style,
                new TagEffect(string(effectMap, "id"), config),
                integer(map, "priority"),
                bool(map, "enabled"),
                bool(map, "chatEnabled"),
                metadata
        );
        TagValidator.validate(tag);
        return tag;
    }

    private static TagColor decodeColor(Map<String, Object> map) {
        return switch (string(map, "type").toLowerCase(Locale.ROOT)) {
            case "preset" -> new TagColor.Preset(string(map, "name"));
            case "rgb" -> new TagColor.Rgb(
                    integer(map, "red"),
                    integer(map, "green"),
                    integer(map, "blue")
            );
            case "random" -> new TagColor.Random();
            case "gradient" -> new TagColor.Gradient(
                    requireRgb(map.get("start")),
                    requireRgb(map.get("end"))
            );
            default -> throw new IllegalArgumentException(
                    "Unknown color type: " + map.get("type")
            );
        };
    }

    private static Map<String, Object> encodeColor(TagColor color) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (color instanceof TagColor.Preset preset) {
            result.put("type", "preset");
            result.put("name", preset.name());
        } else if (color instanceof TagColor.Rgb rgb) {
            result.put("type", "rgb");
            result.put("red", rgb.red());
            result.put("green", rgb.green());
            result.put("blue", rgb.blue());
        } else if (color instanceof TagColor.Random) {
            result.put("type", "random");
        } else if (color instanceof TagColor.Gradient gradient) {
            result.put("type", "gradient");
            result.put("start", encodeColor(gradient.start()));
            result.put("end", encodeColor(gradient.end()));
        } else {
            throw new IllegalArgumentException(
                    "Unsupported color type: " + color.getClass()
            );
        }
        return result;
    }

    private static TagColor.Rgb requireRgb(Object value) {
        TagColor color = decodeColor(requireMap(value, "gradient stop"));
        if (!(color instanceof TagColor.Rgb rgb)) {
            throw new IllegalArgumentException("Gradient stop must be RGB");
        }
        return rgb;
    }

    private static boolean bool(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (!(value instanceof Boolean booleanValue)) {
            throw new IllegalArgumentException("Expected boolean: " + key);
        }
        return booleanValue;
    }

    private static int integer(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (!(value instanceof Number number)) {
            throw new IllegalArgumentException("Expected number: " + key);
        }
        return number.intValue();
    }

    private static String string(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            throw new IllegalArgumentException("Missing value: " + key);
        }
        return String.valueOf(value);
    }

    private static Map<String, Object> requireMap(Object value, String name) {
        if (!(value instanceof Map<?, ?> raw)) {
            throw new IllegalArgumentException("Expected map: " + name);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : raw.entrySet()) {
            if (!(entry.getKey() instanceof String key)) {
                throw new IllegalArgumentException(
                        "Expected string key: " + name
                );
            }
            result.put(key, entry.getValue());
        }
        return result;
    }

    private static Yaml yaml() {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        options.setIndent(2);
        options.setWidth(120);
        LoaderOptions loaderOptions = new LoaderOptions();
        return new Yaml(
                new SafeConstructor(loaderOptions),
                new Representer(options),
                options,
                loaderOptions
        );
    }
}
