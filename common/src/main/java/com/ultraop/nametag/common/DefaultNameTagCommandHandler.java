package com.ultraop.nametag.common;

import com.ultraop.nametag.api.CommandContext;
import com.ultraop.nametag.api.CommandSource;
import com.ultraop.nametag.api.MessageService;
import com.ultraop.nametag.api.NameTagCommandHandler;
import com.ultraop.nametag.api.OnlinePlayer;
import com.ultraop.nametag.api.PlayerResolver;
import com.ultraop.nametag.api.TagService;
import com.ultraop.nametag.core.model.GlitchMode;
import com.ultraop.nametag.core.model.Tag;
import com.ultraop.nametag.core.model.TagColor;
import com.ultraop.nametag.core.model.TagEffect;
import com.ultraop.nametag.core.model.TagId;
import com.ultraop.nametag.core.model.TagStyle;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class DefaultNameTagCommandHandler implements NameTagCommandHandler {
    private static final List<String> SUBCOMMANDS =
            List.of("create", "list", "give", "set", "remove", "clear", "delete", "glitch");

    private final TagService tagService;
    private final PlayerResolver playerResolver;
    private final MessageService messages;

    public DefaultNameTagCommandHandler(
            TagService tagService,
            PlayerResolver playerResolver,
            MessageService messages
    ) {
        this.tagService = Objects.requireNonNull(tagService, "tagService");
        this.playerResolver = Objects.requireNonNull(playerResolver, "playerResolver");
        this.messages = Objects.requireNonNull(messages, "messages");
    }

    @Override
    public void execute(CommandContext context) {
        String[] args = context.args();
        if (args.length == 0) {
            sendUsage(context.source());
            return;
        }

        String subcommand = args[0].toLowerCase(Locale.ROOT);
        if ("list".equals(subcommand)) {
            if (allowed(context.source(), "nametag.use")) {
                list(context.source());
            }
            return;
        }

        String permission = permissionFor(subcommand);
        if (permission == null) {
            sendUsage(context.source());
            return;
        }
        if (!allowed(context.source(), permission)) {
            return;
        }

        try {
            switch (subcommand) {
                case "create" -> create(context.source(), args);
                case "delete" -> delete(context.source(), args);
                case "give", "set" -> assign(context.source(), args);
                case "remove", "clear" -> clear(context.source(), args);
                case "glitch" -> glitch(context.source(), args);
                default -> sendUsage(context.source());
            }
        } catch (IllegalArgumentException exception) {
            context.source().sendMessage(exception.getMessage());
        }
    }

    @Override
    public Collection<String> suggest(CommandContext context) {
        String[] args = context.args();
        if (args.length == 0) {
            return SUBCOMMANDS;
        }

        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            return SUBCOMMANDS.stream()
                    .filter(value -> value.startsWith(prefix))
                    .toList();
        }

        String subcommand = args[0].toLowerCase(Locale.ROOT);
        if (args.length == 2 && ("delete".equals(subcommand) || "glitch".equals(subcommand))) {
            return tagNames(args[1]);
        }

        if (args.length == 2 && List.of("give", "set", "remove", "clear").contains(subcommand)) {
            return playerNames(args[1]);
        }

        if (args.length == 3 && List.of("give", "set").contains(subcommand)) {
            return tagNames(args[2]);
        }

        if (args.length == 3 && "glitch".equals(subcommand)) {
            String prefix = args[2].toLowerCase(Locale.ROOT);
            return List.of("white", "colorful").stream()
                    .filter(value -> value.startsWith(prefix))
                    .toList();
        }

        return List.of();
    }

    private void create(CommandSource source, String[] args) {
        if (args.length < 3) {
            throw new IllegalArgumentException(messages.message("error.usage.create"));
        }

        TagId id = new TagId(args[1].toLowerCase(Locale.ROOT));
        String displayName = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        Tag tag = new Tag(
                id,
                displayName,
                new TagColor.Preset("white"),
                TagStyle.plain(),
                TagEffect.none(),
                0,
                true,
                true,
                Map.of()
        );
        tagService.create(tag);
        source.sendMessage(messages.format("message.created", Map.of("tag", id.value())));
    }

    private void delete(CommandSource source, String[] args) {
        if (args.length != 2) {
            throw new IllegalArgumentException(messages.message("error.usage.delete"));
        }

        TagId id = new TagId(args[1].toLowerCase(Locale.ROOT));
        if (!tagService.delete(id)) {
            throw new IllegalArgumentException(
                    messages.format("error.tag.not_found", Map.of("tag", id.value()))
            );
        }
        source.sendMessage(messages.format("message.deleted", Map.of("tag", id.value())));
    }

    private void assign(CommandSource source, String[] args) {
        if (args.length != 3) {
            throw new IllegalArgumentException(messages.message("error.usage.assign"));
        }

        OnlinePlayer player = playerResolver.findOnline(args[1]).orElseThrow(() ->
                new IllegalArgumentException(
                        messages.format("error.player.offline", Map.of("player", args[1]))
                )
        );

        TagId id = new TagId(args[2].toLowerCase(Locale.ROOT));
        if ("set".equalsIgnoreCase(args[0])) {
            tagService.setActive(player.uuid(), id);
        } else {
            tagService.assign(player.uuid(), id);
        }
        source.sendMessage(messages.format(
                "message.assigned",
                Map.of("tag", id.value(), "player", player.name())
        ));
    }

    private void clear(CommandSource source, String[] args) {
        if (args.length != 2) {
            throw new IllegalArgumentException(messages.message("error.usage.remove"));
        }

        OnlinePlayer player = playerResolver.findOnline(args[1]).orElseThrow(() ->
                new IllegalArgumentException(
                        messages.format("error.player.offline", Map.of("player", args[1]))
                )
        );
        tagService.clear(player.uuid());
        source.sendMessage(messages.format(
                "message.cleared",
                Map.of("player", player.name())
        ));
    }

    private void glitch(CommandSource source, String[] args) {
        if (args.length != 3) {
            throw new IllegalArgumentException(messages.message("error.usage.glitch"));
        }

        TagId id = new TagId(args[1].toLowerCase(Locale.ROOT));
        GlitchMode mode = GlitchMode.from(args[2]);
        Tag updated = tagService.setGlitch(id, mode);
        source.sendMessage(messages.format(
                "message.glitch_set",
                Map.of(
                        "mode", mode.name().toLowerCase(Locale.ROOT),
                        "tag", updated.id().value()
                )
        ));
    }

    private void list(CommandSource source) {
        tagService.list().stream()
                .sorted(Comparator.comparing(tag -> tag.id().value()))
                .forEach(tag -> source.sendMessage(messages.format(
                        "message.list_entry",
                        Map.of(
                                "tag", tag.id().value(),
                                "displayName", tag.displayName(),
                                "glitch", tag.effect().isGlitch() ? " [glitch]" : ""
                        )
                )));
    }

    private boolean allowed(CommandSource source, String permission) {
        if (source.hasPermission(permission) || source.hasPermission("nametag.admin")) {
            return true;
        }
        source.sendMessage(messages.message("error.permission"));
        return false;
    }

    private static String permissionFor(String subcommand) {
        return switch (subcommand) {
            case "create" -> "nametag.create";
            case "delete" -> "nametag.delete";
            case "give", "set" -> "nametag.give";
            case "remove", "clear" -> "nametag.remove";
            case "glitch" -> "nametag.edit";
            default -> null;
        };
    }

    private List<String> tagNames(String prefix) {
        String normalized = prefix.toLowerCase(Locale.ROOT);
        return tagService.list().stream()
                .map(Tag::id)
                .map(TagId::value)
                .filter(value -> value.startsWith(normalized))
                .sorted()
                .toList();
    }

    private List<String> playerNames(String prefix) {
        String normalized = prefix.toLowerCase(Locale.ROOT);
        return playerResolver.onlinePlayers().stream()
                .map(OnlinePlayer::name)
                .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(normalized))
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    private void sendUsage(CommandSource source) {
        source.sendMessage(messages.message("message.usage"));
    }
}
