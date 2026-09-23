package com.ultraop.nametag.fabric.v1_21_11;

import com.ultraop.nametag.api.ConfigurationService;
import com.ultraop.nametag.api.TagResolutionContext;
import com.ultraop.nametag.api.TagService;
import com.ultraop.nametag.core.model.Tag;
import com.ultraop.nametag.core.model.TagColor;
import com.ultraop.nametag.core.model.TagPresentation;
import com.ultraop.nametag.core.model.TagItemSettings;
import com.ultraop.nametag.core.model.TagStyle;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.command.DefaultPermissions;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.util.Atlases;
import net.minecraft.util.Identifier;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.object.AtlasTextObjectContents;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;

import java.util.List;
import java.util.Objects;

public final class Fabric2111ChatRenderer {
    private static final String TAG_PLACEHOLDER = "{tag}";
    private static final String ITEM_PLACEHOLDER = "{item}";
    private static final String PLAYER_PLACEHOLDER = "{player}";
    private static final String MESSAGE_PLACEHOLDER = "{message}";
    private static final String TAG_META_PREFIX = "{tag_meta:";

    private final TagService tagService;
    private final ConfigurationService configuration;

    public Fabric2111ChatRenderer(TagService tagService, ConfigurationService configuration) {
        this.tagService = Objects.requireNonNull(tagService, "tagService");
        this.configuration = Objects.requireNonNull(configuration, "configuration");
    }

    public void register() {
        /*
         * Fabric's message decorator can only replace message content. Vanilla
         * then adds the sender name around that content, which makes an
         * item/rank prefix appear after the player name. For NameTag-Core's
         * contract ([rank] Player: Message), the tagged message must therefore
         * be rendered as a complete system-chat component after the allow phase.
         * Returning false prevents vanilla from emitting the duplicate/default
         * chat line; the original signed message is still validated by the
         * allow event before we render the final component.
         */
        ServerMessageEvents.ALLOW_CHAT_MESSAGE.register(this::allowChatMessage);
    }

    private boolean allowChatMessage(
            net.minecraft.network.message.SignedMessage signedMessage,
            ServerPlayerEntity sender,
            net.minecraft.network.message.MessageType.Parameters params
    ) {
        if (sender == null || !configuration.current().chatEnabled()) return true;
        if (!sender.getPermissions().hasPermission(DefaultPermissions.GAMEMASTERS)) return true;

        TagResolutionContext context = new TagResolutionContext(
                sender.getEntityWorld().getRegistryKey().getValue().toString(),
                sender.getBlockPos().getX(), sender.getBlockPos().getY(), sender.getBlockPos().getZ()
        );
        List<Tag> active = tagService.activeTags(sender.getUuid(), context).stream()
                .filter(Tag::enabled).filter(Tag::chatEnabled).toList();
        if (active.isEmpty()) return true;

        Text rendered = renderFormat(
                ensureItemPlaceholder(configuration.current().chatFormat()),
                active,
                sender.getDisplayName(),
                signedMessage.getContent()
        );
        for (ServerPlayerEntity recipient : sender.getEntityWorld().getServer().getPlayerManager().getPlayerList()) {
            recipient.sendMessage(rendered);
        }
        return false;
    }

    static Text renderFormat(String format, Tag tag, Text playerName, Text message) {
        return renderFormat(format, List.of(tag), playerName, message);
    }

    static String ensureItemPlaceholder(String format) {
        // The native team/nameplate already carries the icon and tag. Never inject
        // another copy into chat, and remove legacy square-bracket wrappers so
        // old configuration files cannot leave a stray [] in chat.
        return format.replace("{item}", "").replace("{tag}", "").replace("{tags}", "")
                .replaceAll("\\[\\s*\\]\\s*", "")
                .stripLeading();
    }

    static Text renderContentFormat(String format, List<Tag> tags, Text message) {
        return renderFormat(removePlayerPlaceholderForContent(format), tags, Text.empty(), message);
    }

    private static String removePlayerPlaceholderForContent(String format) {
        int playerIndex = format.indexOf(PLAYER_PLACEHOLDER);
        if (playerIndex < 0) {
            return format;
        }

        String before = format.substring(0, playerIndex);
        String after = format.substring(playerIndex + PLAYER_PLACEHOLDER.length());

        // Fabric's message decorator only replaces message content. Vanilla
        // applies the sender decoration afterward, so rendering {player} here
        // would display the player name twice. Remove the sender placeholder
        // together with its following separator when present.
        after = after.replaceFirst("^\\s*[:|>-]\\s*", "");
        return before + after;
    }

    static Text renderFormat(String format, List<Tag> tags, Text playerName, Text message) {
        MutableText result = Text.empty();
        int cursor = 0;
        while (cursor < format.length()) {
            PlaceholderMatch match = nextPlaceholder(format, cursor);
            if (match == null) {
                result.append(Text.literal(format.substring(cursor)));
                break;
            }
            if (match.start() > cursor) result.append(Text.literal(format.substring(cursor, match.start())));

            Text replacement = switch (match.placeholder()) {
                case TAG_PLACEHOLDER -> styledTag(tags.get(0));
                case ITEM_PLACEHOLDER -> itemIcon(tags.get(0));
                case "{tags}" -> styledTags(tags);
                case "{tag_id}" -> Text.literal(tags.get(0).id().value());
                case "{tag_priority}" -> Text.literal(String.valueOf(tags.get(0).priority()));
                case "{tag_prefix}" -> Text.literal(TagPresentation.prefix(tags.get(0)));
                case "{tag_suffix}" -> Text.literal(TagPresentation.suffix(tags.get(0)));
                case PLAYER_PLACEHOLDER -> playerName;
                case MESSAGE_PLACEHOLDER -> message;
                default -> Text.literal(metadataValue(tags.get(0), match.placeholder()));
            };
            result.append(replacement);
            cursor = match.end();
        }
        return result;
    }

    static Text itemIcon(Tag tag) {
        TagItemSettings settings = TagItemSettings.from(tag);
        if (settings == null) return Text.empty();
        Identifier sprite = itemSpriteId(tag);
        if (sprite == null) return Text.empty();
        Identifier itemId = Identifier.tryParse(TagItemSettings.from(tag).itemId());
        if (itemId == null || !Registries.ITEM.containsId(itemId)) return Text.empty();
        MutableText icon = Text.object(new AtlasTextObjectContents(Atlases.ITEMS, sprite));
        if (!TagPresentation.displayText(tag).isBlank()) icon.append(Text.literal(" "));
        return icon;
    }

    static Identifier itemSpriteId(Tag tag) {
        TagItemSettings settings = TagItemSettings.from(tag);
        if (settings == null) return null;
        Identifier itemId = Identifier.tryParse(settings.itemId());
        if (itemId == null) return null;
        return Identifier.of(itemId.getNamespace(), "item/" + itemId.getPath());
    }

    static Text styledTags(List<Tag> tags) {
        MutableText result = Text.empty();
        for (int index = 0; index < tags.size(); index++) {
            if (index > 0) result.append(Text.literal(" "));
            result.append(styledTag(tags.get(index)));
        }
        return result;
    }

    static Text styledTag(Tag tag) {
        TagStyle style = tag.style();
        TagColor color = tag.color();
        if (color instanceof TagColor.Preset) {
            MutableText component = Text.literal(TagPresentation.displayText(tag));
            component.setStyle(applyStyle(component.getStyle(), style));
            TextColor presetColor = presetColor(((TagColor.Preset) color).name());
            if (presetColor != null) component.setStyle(component.getStyle().withColor(presetColor));
            return component;
        }

        MutableText result = Text.empty();
        int[] codePoints = TagPresentation.displayText(tag).codePoints().toArray();
        long seed = tag.id().value().hashCode();
        for (int index = 0; index < codePoints.length; index++) {
            MutableText glyph = Text.literal(new String(Character.toChars(codePoints[index])));
            Style glyphStyle = applyStyle(Style.EMPTY, style);
            Integer rgb = TagColor.resolve(color, index, codePoints.length, seed);
            if (rgb != null) glyphStyle = glyphStyle.withColor(rgb);
            glyph.setStyle(glyphStyle);
            result.append(glyph);
        }
        return result;
    }

    private static TextColor presetColor(String name) {
        return switch (name.trim().toLowerCase()) {
            case "black" -> TextColor.fromRgb(0x000000);
            case "dark_blue" -> TextColor.fromRgb(0x0000AA);
            case "dark_green" -> TextColor.fromRgb(0x00AA00);
            case "dark_aqua" -> TextColor.fromRgb(0x00AAAA);
            case "dark_red" -> TextColor.fromRgb(0xAA0000);
            case "dark_purple" -> TextColor.fromRgb(0xAA00AA);
            case "gold" -> TextColor.fromRgb(0xFFAA00);
            case "gray", "grey" -> TextColor.fromRgb(0xAAAAAA);
            case "dark_gray", "dark_grey" -> TextColor.fromRgb(0x555555);
            case "blue" -> TextColor.fromRgb(0x5555FF);
            case "green" -> TextColor.fromRgb(0x55FF55);
            case "aqua" -> TextColor.fromRgb(0x55FFFF);
            case "red" -> TextColor.fromRgb(0xFF5555);
            case "light_purple" -> TextColor.fromRgb(0xFF55FF);
            case "yellow" -> TextColor.fromRgb(0xFFFF55);
            case "white" -> TextColor.fromRgb(0xFFFFFF);
            default -> null;
        };
    }

    private static Style applyStyle(Style style, TagStyle tagStyle) {
        return style.withBold(tagStyle.bold()).withItalic(tagStyle.italic())
                .withUnderline(tagStyle.underlined()).withStrikethrough(tagStyle.strikethrough())
                .withObfuscated(tagStyle.obfuscated());
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
        if (tags >= 0 && tags < start) { start = tags; placeholder = "{tags}"; }
        if (item >= 0 && item < start) { start = item; placeholder = ITEM_PLACEHOLDER; }
        if (tag >= 0 && tag < start) { start = tag; placeholder = TAG_PLACEHOLDER; }
        if (player >= 0 && player < start) { start = player; placeholder = PLAYER_PLACEHOLDER; }
        if (message >= 0 && message < start) { start = message; placeholder = MESSAGE_PLACEHOLDER; }
        return placeholder == null ? null : new PlaceholderMatch(start, start + placeholder.length(), placeholder);
    }

    private record PlaceholderMatch(int start, int end, String placeholder) {}
}
