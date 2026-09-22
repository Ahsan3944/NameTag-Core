package com.ultraop.nametag.common;

import com.ultraop.nametag.api.MessageService;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class DefaultMessageService implements MessageService {
    private final Map<String, String> messages;

    public DefaultMessageService() {
        this(defaultMessages());
    }

    public DefaultMessageService(Map<String, String> messages) {
        Objects.requireNonNull(messages, "messages");
        LinkedHashMap<String, String> copy = new LinkedHashMap<>();
        messages.forEach((key, value) -> {
            if (key == null || key.isBlank()) {
                throw new IllegalArgumentException("Message key cannot be blank");
            }
            if (value == null) {
                throw new IllegalArgumentException("Message value cannot be null: " + key);
            }
            copy.put(key, value);
        });
        this.messages = Map.copyOf(copy);
    }

    @Override
    public String message(String key) {
        Objects.requireNonNull(key, "key");
        return messages.getOrDefault(key, key);
    }

    private static Map<String, String> defaultMessages() {
        Map<String, String> defaults = new LinkedHashMap<>();
        defaults.put("error.permission", "You do not have permission to edit NameTags.");
        defaults.put("error.reload.unavailable", "Configuration reload is not available.");
        defaults.put("error.reload.failed", "Configuration reload failed: {reason}");
        defaults.put("message.reload_success", "Configuration reloaded successfully.");
        defaults.put("error.usage.create", "Usage: /nametag tag create <tag> name <displayName> [item <item>] | item <item> [name <displayName>]");
        defaults.put("error.usage.edit", "Usage: /nametag tag edit <tag> <name|color|gradient|style|priority|enabled|chat> <value>");
        defaults.put("error.usage.delete", "Usage: /nametag tag delete <tag>");
        defaults.put("error.usage.assign", "Usage: /nametag player give <player> <tag> [duration]");
        defaults.put("message.assigned_temporary", "Assigned {tag} to {player} for {duration}");
        defaults.put("message.exported", "Exported {count} tags to {file}");
        defaults.put("message.imported", "Imported {count} tags from {file}");
        defaults.put("error.usage.remove", "Usage: /nametag player remove <player>");
        defaults.put("error.usage.glitch", "Usage: /nametag display glitch <tag> <white|colorful>");
        defaults.put("error.usage.effect", "Usage: /nametag display effect <tag> <none|rainbow|pulse|wave>");
        defaults.put("error.usage.role", "Usage: /nametag advanced role <tag> <permission|clear>");
        defaults.put("error.player.offline", "Player must be online: {player}");
        defaults.put("error.tag.not_found", "Tag not found: {tag}");
        defaults.put("message.created", "Created NameTag: {tag}");
        defaults.put("message.deleted", "Deleted NameTag: {tag}");
        defaults.put("message.assigned", "Assigned {tag} to {player}");
        defaults.put("message.set_active", "Set {tag} as active for {player}");
        defaults.put("message.cleared", "Cleared NameTags from {player}");
        defaults.put("message.glitch_set", "Glitch effect set to {mode} for tag {tag}");
        defaults.put("message.effect_set", "Effect set to {effect} for tag {tag}");
        defaults.put("message.role_set", "Automatic role mapping for tag {tag}: {permission}");
        defaults.put("message.list_header", "NameTags ({count} total):");
        defaults.put("message.list_entry", "- {tag} -> {displayName} | color={color} | style={style} | enabled={enabled} | chat={chat} | effect={effect}");
        defaults.put("message.usage", "/nametag tag create <tag> <displayName> | tag edit <tag> <name|color|gradient|style|priority|enabled|chat> <value> | tag list | tag delete <tag> | player give <player> <tag> [duration] | player set <player> <tag> | player remove <player> | player clear <player> | display glitch <tag> <white|colorful> | display effect <tag> <none|rainbow|pulse|wave> | advanced role <tag> <permission|clear> | advanced scope <tag> clear|world <world>|region <name> <world> <minX> <minY> <minZ> <maxX> <maxY> <maxZ> | admin reload | admin export <file> | admin import <file>");
        return defaults;
    }
}
