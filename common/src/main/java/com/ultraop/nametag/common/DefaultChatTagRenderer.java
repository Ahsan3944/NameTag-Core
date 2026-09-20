package com.ultraop.nametag.common;

import com.ultraop.nametag.api.ChatTagRenderer;
import com.ultraop.nametag.api.ConfigurationService;
import com.ultraop.nametag.core.model.Tag;

import java.util.Objects;
import java.util.UUID;

/**
 * Platform-neutral chat formatting fallback.
 *
 * <p>Platform adapters may render the same format with native text components so
 * tag color and style can be preserved without coupling the API to a Minecraft
 * text implementation.</p>
 */
public final class DefaultChatTagRenderer implements ChatTagRenderer {
    private final ConfigurationService configuration;

    public DefaultChatTagRenderer(ConfigurationService configuration) {
        this.configuration = Objects.requireNonNull(configuration, "configuration");
    }

    @Override
    public String render(UUID playerUuid, String playerName, String message, Tag activeTag) {
        Objects.requireNonNull(playerUuid, "playerUuid");
        Objects.requireNonNull(playerName, "playerName");
        Objects.requireNonNull(message, "message");
        Objects.requireNonNull(activeTag, "activeTag");

        String format = configuration.current().chatFormat();
        return format
                .replace("{tag}", activeTag.displayName())
                .replace("{player}", playerName)
                .replace("{message}", message);
    }
}
