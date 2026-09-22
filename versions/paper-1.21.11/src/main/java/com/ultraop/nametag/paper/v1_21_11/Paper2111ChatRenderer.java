package com.ultraop.nametag.paper.v1_21_11;

import com.ultraop.nametag.api.ConfigurationService;
import com.ultraop.nametag.api.TagResolutionContext;
import com.ultraop.nametag.api.TagService;
import com.ultraop.nametag.core.model.Tag;
import com.ultraop.nametag.core.model.TagColor;
import com.ultraop.nametag.core.model.TagStyle;
import com.ultraop.nametag.core.model.TagPresentation;
import com.ultraop.nametag.core.model.TagItemSettings;
import io.papermc.paper.chat.ChatRenderer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.object.ObjectContents;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;

import java.util.Objects;
import java.util.Optional;
import java.util.List;

/**
 * Paper 1.21.11 chat renderer.
 *
 * <p>The renderer is viewer-unaware because the output does not depend on the
 * recipient. Glitch effects intentionally fall back to the stable tag text in
 * chat rather than attempting a per-tick animation.</p>
 */
public final class Paper2111ChatRenderer implements ChatRenderer.ViewerUnaware {
    private static final String TAG_PLACEHOLDER = "{tag}";
    private static final String ITEM_PLACEHOLDER = "{item}";
    private static final String PLAYER_PLACEHOLDER = "{player}";
    private static final String MESSAGE_PLACEHOLDER = "{message}";
    private static final String TAG_META_PREFIX = "{tag_meta:";

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

        TagResolutionContext context = new TagResolutionContext(
                source.getWorld().getKey().asString(),
                source.getLocation().getBlockX(),
                source.getLocation().getBlockY(),
                source.getLocation().getBlockZ()
        );
        List<Tag> active = tagService.activeTags(source.getUniqueId(), context).stream()
                .filter(Tag::enabled).filter(Tag::chatEnabled).toList();
        if (active.isEmpty()) return defaultChat(sourceDisplayName, message);

        return renderFormat(configuration.current().chatFormat(), active, sourceDisplayName, message);
    }

    static Component renderFormat(
            String format,
            Tag tag,
            Component sourceDisplayName,
            Component message
    ) {
        return renderFormat(format, List.of(tag), sourceDisplayName, message);
    }

    static Component renderFormat(
            String format,
            List<Tag> tags,
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
                case TAG_PLACEHOLDER -> styledTag(tags.get(0));
                case ITEM_PLACEHOLDER -> itemIcon(tags.get(0));
                case "{tags}" -> styledTags(tags);
                case "{tag_id}" -> Component.text(tags.get(0).id().value());
                case "{tag_priority}" -> Component.text(String.valueOf(tags.get(0).priority()));
                case "{tag_prefix}" -> Component.text(TagPresentation.prefix(tags.get(0)));
                case "{tag_suffix}" -> Component.text(TagPresentation.suffix(tags.get(0)));
                case PLAYER_PLACEHOLDER -> sourceDisplayName;
                case MESSAGE_PLACEHOLDER -> message;
                default -> Component.text(metadataValue(tags.get(0), match.placeholder()));
            };
            result = result.append(replacement);
            cursor = match.end();
        }

        return result;
    }

    static Component itemIcon(Tag tag) {
        TagItemSettings settings = TagItemSettings.from(tag);
        if (settings == null) return Component.empty();
        String[] parts = settings.itemId().split(":", 2);
        if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) return Component.empty();
        Component icon = Component.object(ObjectContents.sprite(
                Key.key("minecraft", "items"),
                Key.key(parts[0], "item/" + parts[1])
        ));
        if (!TagPresentation.displayText(tag).isBlank()) icon = icon.append(Component.text(" "));
        return icon;
    }

    private static String metadataValue(Tag tag, String placeholder) {
        if (placeholder.startsWith(TAG_META_PREFIX) && placeholder.endsWith("}")) {
            String key = placeholder.substring(TAG_META_PREFIX.length(), placeholder.length() - 1);
            String value = tag.metadata().get(key);
            return value == null ? placeholder : value;
        }
        return placeholder;
    }

    private static PlaceholderMatch nextPlaceholder(String format, int fromIndex) {
        int tag = format.indexOf(TAG_PLACEHOLDER, fromIndex);
        int item = format.indexOf(ITEM_PLACEHOLDER, fromIndex);
        int tags = format.indexOf("{tags}", fromIndex);
        int player = format.indexOf(PLAYER_PLACEHOLDER, fromIndex);
        int message = format.indexOf(MESSAGE_PLACEHOLDER, fromIndex);
        int tagId = format.indexOf("{tag_id}", fromIndex);
        int tagPriority = format.indexOf("{tag_priority}", fromIndex);
        int tagPrefix = format.indexOf("{tag_prefix}", fromIndex);
        int tagSuffix = format.indexOf("{tag_suffix}", fromIndex);
        int tagMeta = format.indexOf(TAG_META_PREFIX, fromIndex);

        int start = Integer.MAX_VALUE;
        String placeholder = null;
        if (tagId >= 0 && tagId < start) { start = tagId; placeholder = "{tag_id}"; }
        if (tagPriority >= 0 && tagPriority < start) { start = tagPriority; placeholder = "{tag_priority}"; }
        if (tagPrefix >= 0 && tagPrefix < start) { start = tagPrefix; placeholder = "{tag_prefix}"; }
        if (tagSuffix >= 0 && tagSuffix < start) { start = tagSuffix; placeholder = "{tag_suffix}"; }
        if (tagMeta >= 0 && tagMeta < start) {
            int end = format.indexOf("}", tagMeta + TAG_META_PREFIX.length());
            if (end >= 0) { start = tagMeta; placeholder = format.substring(tagMeta, end + 1); }
        }
        // {tag_id}, {tag_priority}, {tag_prefix}, and {tag_suffix} all begin with {tag}.
        // Resolve the longer placeholders first so the generic {tag} token cannot consume them.
        if (tags >= 0 && tags < start) { start = tags; placeholder = "{tags}"; }
        if (item >= 0 && item < start) { start = item; placeholder = ITEM_PLACEHOLDER; }
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

    static Component styledTags(List<Tag> tags) {
        Component result = Component.empty();
        for (int index = 0; index < tags.size(); index++) {
            if (index > 0) result = result.append(Component.text(" "));
            result = result.append(styledTag(tags.get(index)));
        }
        return result;
    }

    static Component styledTag(Tag tag) {
        TagStyle style = tag.style();
        TagColor color = tag.color();
        if (color instanceof TagColor.Preset) {
            return applyStyle(
                    Component.text(TagPresentation.displayText(tag)).color(presetColor(((TagColor.Preset) color).name())),
                    style
            );
        }

        Component result = Component.empty();
        int[] codePoints = TagPresentation.displayText(tag).codePoints().toArray();
        long seed = tag.id().value().hashCode();
        for (int index = 0; index < codePoints.length; index++) {
            Component glyph = Component.text(new String(Character.toChars(codePoints[index])));
            Integer rgb = TagColor.resolve(color, index, codePoints.length, seed);
            if (rgb != null) {
                glyph = glyph.color(TextColor.color(rgb));
            }
            result = result.append(applyStyle(glyph, style));
        }
        return result;
    }

    private static Component applyStyle(Component component, TagStyle style) {
        return component
                .decoration(TextDecoration.BOLD, style.bold())
                .decoration(TextDecoration.ITALIC, style.italic())
                .decoration(TextDecoration.UNDERLINED, style.underlined())
                .decoration(TextDecoration.STRIKETHROUGH, style.strikethrough())
                .decoration(TextDecoration.OBFUSCATED, style.obfuscated());
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
