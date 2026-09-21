package com.ultraop.nametag.fabric.v1_21_11;

import com.ultraop.nametag.api.ConfigurationService;
import com.ultraop.nametag.api.TagService;
import com.ultraop.nametag.core.model.Tag;
import com.ultraop.nametag.core.model.TagColor;
import com.ultraop.nametag.core.model.TagStyle;
import net.fabricmc.fabric.api.message.v1.ServerMessageDecoratorEvent;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public final class Fabric2111ChatRenderer {
    private static final String TAG_PLACEHOLDER = "{tag}";
    private static final String PLAYER_PLACEHOLDER = "{player}";
    private static final String MESSAGE_PLACEHOLDER = "{message}";

    private final TagService tagService;
    private final ConfigurationService configuration;

    public Fabric2111ChatRenderer(TagService tagService, ConfigurationService configuration) {
        this.tagService = Objects.requireNonNull(tagService, "tagService");
        this.configuration = Objects.requireNonNull(configuration, "configuration");
    }

    public void register() {
        ServerMessageDecoratorEvent.EVENT.register(
                ServerMessageDecoratorEvent.CONTENT_PHASE,
                this::decorate
        );
    }

    private CompletableFuture<Text> decorate(ServerPlayerEntity sender, Text message) {
        if (sender == null || !configuration.current().chatEnabled()) {
            return CompletableFuture.completedFuture(message);
        }

        if (!sender.getServer().getPlayerManager().isOperator(sender.getGameProfile())) {
            return CompletableFuture.completedFuture(message);
        }

        Optional<Tag> active = tagService.activeTag(sender.getUuid())
                .filter(Tag::enabled)
                .filter(Tag::chatEnabled);
        if (active.isEmpty()) {
            return CompletableFuture.completedFuture(message);
        }

        return CompletableFuture.completedFuture(renderFormat(
                configuration.current().chatFormat(),
                active.get(),
                Text.literal(sender.getName().getString()),
                message
        ));
    }

    static Text renderFormat(String format, Tag tag, Text playerName, Text message) {
        MutableText result = Text.empty();
        int cursor = 0;

        while (cursor < format.length()) {
            PlaceholderMatch match = nextPlaceholder(format, cursor);
            if (match == null) {
                result.append(Text.literal(format.substring(cursor)));
                break;
            }

            if (match.start() > cursor) {
                result.append(Text.literal(format.substring(cursor, match.start())));
            }

            Text replacement = switch (match.placeholder()) {
                case TAG_PLACEHOLDER -> styledTag(tag);
                case PLAYER_PLACEHOLDER -> playerName;
                case MESSAGE_PLACEHOLDER -> message;
                default -> Text.literal(match.placeholder());
            };
            result.append(replacement);
            cursor = match.end();
        }

        return result;
    }

    static Text styledTag(Tag tag) {
        MutableText component = Text.literal(tag.displayName());
        TagStyle style = tag.style();

        component.setStyle(applyStyle(component.getStyle(), style));

        TextColor color = colorOf(tag.color());
        if (color != null) {
            component.setStyle(component.getStyle().withColor(color));
        }
        return component;
    }

    private static TextColor colorOf(TagColor color) {
        if (color instanceof TagColor.Rgb rgb) {
            return TextColor.fromRgb(rgb.red() << 16 | rgb.green() << 8 | rgb.blue());
        }
        if (color instanceof TagColor.Preset preset) {
            return presetColor(preset.name());
        }
        if (color instanceof TagColor.Gradient gradient) {
            return TextColor.fromRgb(
                    gradient.start().red() << 16
                            | gradient.start().green() << 8
                            | gradient.start().blue()
            );
        }
        return null;
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
        return style
                .withBold(tagStyle.bold())
                .withItalic(tagStyle.italic())
                .withUnderline(tagStyle.underlined())
                .withStrikethrough(tagStyle.strikethrough())
                .withObfuscated(tagStyle.obfuscated());
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

    private record PlaceholderMatch(int start, int end, String placeholder) {
    }
}
