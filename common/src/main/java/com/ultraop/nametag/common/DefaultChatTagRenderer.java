package com.ultraop.nametag.common;

import com.ultraop.nametag.api.ChatTagRenderer;
import com.ultraop.nametag.api.ConfigurationService;
import com.ultraop.nametag.core.model.Tag;
import com.ultraop.nametag.core.model.TagPresentation;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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

        String format = configuration.current().chatFormat()
                .replace("{item}", "")
                .replace("{tag}", TagPresentation.displayText(activeTag))
                .replace("{tag_id}", activeTag.id().value())
                .replace("{tag_priority}", String.valueOf(activeTag.priority()))
                .replace("{tag_prefix}", TagPresentation.prefix(activeTag))
                .replace("{tag_suffix}", TagPresentation.suffix(activeTag))
                .replace("{player}", playerName)
                .replace("{message}", message);
        Matcher matcher = Pattern.compile("\\{tag_meta:([^{}]+)\\}").matcher(format);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String value = activeTag.metadata().get(matcher.group(1));
            matcher.appendReplacement(result, Matcher.quoteReplacement(value == null ? matcher.group() : value));
        }
        matcher.appendTail(result);
        return result.toString();
    }
}
