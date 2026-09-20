package com.ultraop.nametag.paper.v1_21_11;

import com.ultraop.nametag.api.ConfigurationService;
import com.ultraop.nametag.api.TagService;
import com.ultraop.nametag.core.model.Tag;
import com.ultraop.nametag.core.model.TagColor;
import com.ultraop.nametag.core.model.TagStyle;
import io.papermc.paper.chat.ChatRenderer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;

import java.util.Objects;
import java.util.Optional;

/**
 * Paper 1.21.11 chat renderer.
 *
 * <p>The renderer is viewer-unaware because the output does not depend on the
 * recipient. Glitch effects intentionally fall back to the stable tag text in
 * chat rather than attempting a per-tick animation.</p>
 */
public final class Paper2111ChatRenderer implements ChatRenderer.ViewerUnaware {
    private static final String TAG_PLACEHOLDER = "{tag}";
    private static final String PLAYER_PLACEHOLDER = "{player}";
    private static final String MESSAGE_PLACEHOLDER = "{message}";

    private final TagService tagService;
    private final ConfigurationService configuration;

    public Paper2111ChatRenderer(TagService tagService, ConfigurationService configuration) {
        this.tagService = Objects.requireNonNull(tagService, "tagService");
        this.configuration = Objects.requireNonNull(configuration, "configuration");
    }

    @Override
    public Component render(Player source, Component sourceDisplayName, Component message) {
        if (!configuration.current().chatEnabled()) {
            return defaultChat(sourceDisplayName, message);
        }

        Optional<Tag> active = tagService.activeTag(source.getUniqueId())
                .filter(Tag::enabled)
                .filter(Tag::chatEnabled);
        if (active.isEmpty()) {
            return defaultChat(sourceDisplayName, message);
        }

        return renderFormat(
                configuration.current().chatFormat(),
                active.get(),
                sourceDisplayName,
                message
        );
    }

    static Component renderFormat(
            String format,
            Tag tag,
            Component sourceDisplayName,
            Component message
    ) {
        Component result = Component.empty();
        int cursor = 0;

        while (cursor < format.length()) {
            PlaceholderMatch match = nextPlaceholder(format, cursor);
            if (match == null) {
                result = result.append(Component.text(format.substring(cursor)));
                break;
            }

            if (match.start() > cursor) {
                result = result.append(Component.text(format.substring(cursor, match.start())));
            }

            Component replacement = switch (match.placeholder()) {
                case TAG_PLACEHOLDER -> styledTag(tag);
                case PLAYER_PLACEHOLDER -> sourceDisplayName;
                case MESSAGE_PLACEHOLDER -> message;
                default -> Component.text(match.placeholder());
            };
            result = result.append(replacement);
            cursor = match.end();
        }

        return result;
    }

    private static PlaceholderMatch nextPlaceholder(String format, int fromIndex) {
        int tag = format.indexOf(TAG_PLACEHOLDER, fromIndex);
        int player = format.indexOf(PLAYER_PLACEHOLDER, fromIndex);
        int message = format.indexOf(MESSAGE_PLACEHOLDER, fromIndex);

        int start = Integer.MAX_VALUE;
        String placeholder = null;
        if (tag >= 0 && tag < start) {
            start = tag;
            placeholder = TAG_PLACEHOLDER;
        }
        if (player >= 0 && player < start) {
            start = player;
            placeholder = PLAYER_PLACEHOLDER;
        }
        if (message >= 0 && message < start) {
            start = message;
            placeholder = MESSAGE_PLACEHOLDER;
        }

        return placeholder == null
                ? null
                : new PlaceholderMatch(start, start + placeholder.length(), placeholder);
    }

    static Component styledTag(Tag tag) {
        Component component = Component.text(tag.displayName());
        TagStyle style = tag.style();
        component = component
                .decoration(TextDecoration.BOLD, style.bold())
                .decoration(TextDecoration.ITALIC, style.italic())
                .decoration(TextDecoration.UNDERLINED, style.underlined())
                .decoration(TextDecoration.STRIKETHROUGH, style.strikethrough())
                .decoration(TextDecoration.OBFUSCATED, style.obfuscated());

        TextColor color = colorOf(tag.color());
        return color == null ? component : component.color(color);
    }

    private static TextColor colorOf(TagColor color) {
        if (color instanceof TagColor.Rgb rgb) {
            return TextColor.color(rgb.red(), rgb.green(), rgb.blue());
        }
        if (color instanceof TagColor.Preset preset) {
            return presetColor(preset.name());
        }
        if (color instanceof TagColor.Gradient gradient) {
            return TextColor.color(
                    gradient.start().red(),
                    gradient.start().green(),
                    gradient.start().blue()
            );
        }
        return null;
    }

    private static TextColor presetColor(String name) {
        return switch (name.trim().toLowerCase()) {
            case "black" -> TextColor.color(0x000000);
            case "dark_blue" -> TextColor.color(0x0000AA);
            case "dark_green" -> TextColor.color(0x00AA00);
            case "dark_aqua" -> TextColor.color(0x00AAAA);
            case "dark_red" -> TextColor.color(0xAA0000);
            case "dark_purple" -> TextColor.color(0xAA00AA);
            case "gold" -> TextColor.color(0xFFAA00);
            case "gray", "grey" -> TextColor.color(0xAAAAAA);
            case "dark_gray", "dark_grey" -> TextColor.color(0x555555);
            case "blue" -> TextColor.color(0x5555FF);
            case "green" -> TextColor.color(0x55FF55);
            case "aqua" -> TextColor.color(0x55FFFF);
            case "red" -> TextColor.color(0xFF5555);
            case "light_purple" -> TextColor.color(0xFF55FF);
            case "yellow" -> TextColor.color(0xFFFF55);
            case "white" -> TextColor.color(0xFFFFFF);
            default -> null;
        };
    }

    private static Component defaultChat(Component sourceDisplayName, Component message) {
        return Component.translatable("chat.type.text", sourceDisplayName, message);
    }

    private record PlaceholderMatch(int start, int end, String placeholder) {
    }
}
