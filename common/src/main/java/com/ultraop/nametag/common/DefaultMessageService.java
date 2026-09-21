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
        defaults.put("error.usage.create", "Usage: /nametag create <tag> <displayName>");
        defaults.put("error.usage.delete", "Usage: /nametag delete <tag>");
        defaults.put("error.usage.assign", "Usage: /nametag give <player> <tag> [duration]");
        defaults.put("message.assigned_temporary", "Assigned {tag} to {player} for {duration}");
        defaults.put("message.exported", "Exported {count} tags to {file}");
        defaults.put("message.imported", "Imported {count} tags from {file}");
        defaults.put("error.usage.remove", "Usage: /nametag remove <player>");
        defaults.put("error.usage.glitch", "Usage: /nametag glitch <tag> <white|colorful>");
        defaults.put("error.usage.effect", "Usage: /nametag effect <tag> <none|rainbow|pulse|wave>");
        defaults.put("error.player.offline", "Player must be online: {player}");
        defaults.put("error.tag.not_found", "Tag not found: {tag}");
        defaults.put("message.created", "Created NameTag: {tag}");
        defaults.put("message.deleted", "Deleted NameTag: {tag}");
        defaults.put("message.assigned", "Assigned {tag} to {player}");
        defaults.put("message.set_active", "Set {tag} as active for {player}");
        defaults.put("message.cleared", "Cleared NameTags from {player}");
        defaults.put("message.glitch_set", "Glitch effect set to {mode} for tag {tag}");
        defaults.put("message.effect_set", "Effect set to {effect} for tag {tag}");
        defaults.put("message.list_entry", "{tag} -> {displayName}{glitch}");
        defaults.put("message.usage", "/nametag create <tag> <displayName> | list | give <player> <tag> | set <player> <tag> | remove <player> | clear <player> | delete <tag> | reload | glitch <tag> <white|colorful> | effect <tag> <none|rainbow|pulse|wave>");
        return defaults;
    }
}
