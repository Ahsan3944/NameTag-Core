package com.ultraop.nametag.fabric.v1_21_11;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.ultraop.nametag.api.TagService;
import com.ultraop.nametag.core.model.GlitchMode;
import com.ultraop.nametag.core.model.Tag;
import com.ultraop.nametag.core.model.TagColor;
import com.ultraop.nametag.core.model.TagEffect;
import com.ultraop.nametag.core.model.TagId;
import com.ultraop.nametag.core.model.TagStyle;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class Fabric2111Adapter {
    private final TagService tagService;
    private final Fabric2111NameplateRenderer renderer;

    public Fabric2111Adapter(TagService tagService) {
        this.tagService = Objects.requireNonNull(tagService, "tagService");
        this.renderer = new Fabric2111NameplateRenderer(tagService);
    }

    public TagService tagService() {
        return tagService;
    }

    public void stop(MinecraftServer server) {
        renderer.stop(server);
    }

    public void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(
                        CommandManager.literal("nametag")
                                .requires(CommandManager.requirePermissionLevel(CommandManager.GAMEMASTERS_CHECK))
                                .then(CommandManager.literal("list")
                                        .executes(context -> {
                                            ServerCommandSource source = context.getSource();
                                            tagService.list().stream()
                                                    .sorted(java.util.Comparator.comparing(tag -> tag.id().value()))
                                                    .forEach(tag -> source.sendFeedback(
                                                            () -> Text.literal(
                                                                    tag.id().value() + " -> " + tag.displayName() +
                                                                            (tag.effect().isGlitch() ? " [glitch]" : "")
                                                            ),
                                                            false
                                                    ));
                                            return 1;
                                        }))
                                .then(CommandManager.literal("create")
                                        .then(CommandManager.argument("tag", StringArgumentType.word())
                                                .then(CommandManager.argument("displayName", StringArgumentType.greedyString())
                                                        .executes(context -> {
                                                            String tagName = StringArgumentType.getString(context, "tag").toLowerCase(Locale.ROOT);
                                                            String displayName = StringArgumentType.getString(context, "displayName");
                                                            Tag tag = new Tag(
                                                                    new TagId(tagName),
                                                                    displayName,
                                                                    new TagColor.Preset("white"),
                                                                    TagStyle.plain(),
                                                                    TagEffect.none(),
                                                                    0,
                                                                    true,
                                                                    true,
                                                                    java.util.Map.of()
                                                            );
                                                            tagService.create(tag);
                                                            context.getSource().sendFeedback(
                                                                    () -> Text.literal("Created NameTag: " + tagName),
                                                                    true
                                                            );
                                                            return 1;
                                                        }))))
                                .then(CommandManager.literal("delete")
                                        .then(CommandManager.argument("tag", StringArgumentType.word())
                                                .suggests((context, builder) -> suggestTags(builder, tagService))
                                                .executes(context -> {
                                                    TagId id = new TagId(StringArgumentType.getString(context, "tag").toLowerCase(Locale.ROOT));
                                                    if (!tagService.delete(id)) {
                                                        context.getSource().sendError(Text.literal("Tag not found: " + id.value()));
                                                        return 0;
                                                    }
                                                    context.getSource().sendFeedback(
                                                            () -> Text.literal("Deleted NameTag: " + id.value()),
                                                            true
                                                    );
                                                    return 1;
                                                })))
                                .then(CommandManager.literal("give")
                                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                                .then(CommandManager.argument("tag", StringArgumentType.word())
                                                        .suggests((context, builder) -> suggestTags(builder, tagService))
                                                        .executes(context -> assign(context, false)))))
                                .then(CommandManager.literal("set")
                                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                                .then(CommandManager.argument("tag", StringArgumentType.word())
                                                        .suggests((context, builder) -> suggestTags(builder, tagService))
                                                        .executes(context -> assign(context, true)))))
                                .then(CommandManager.literal("remove")
                                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                                .executes(context -> clear(context))))
                                .then(CommandManager.literal("clear")
                                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                                .executes(context -> clear(context))))
                                .then(CommandManager.literal("glitch")
                                        .then(CommandManager.argument("tag", StringArgumentType.word())
                                                .suggests((context, builder) -> suggestTags(builder, tagService))
                                                .then(CommandManager.argument("mode", StringArgumentType.word())
                                                        .suggests((context, builder) ->
                                                                CommandSource.suggestMatching(
                                                                        List.of("white", "colorful"),
                                                                        builder
                                                                ))
                                                        .executes(context -> {
                                                            TagId tagId = new TagId(
                                                                    StringArgumentType.getString(context, "tag").toLowerCase(Locale.ROOT)
                                                            );
                                                            GlitchMode mode = GlitchMode.from(
                                                                    StringArgumentType.getString(context, "mode")
                                                            );
                                                            Tag updated = tagService.setGlitch(tagId, mode);
                                                            context.getSource().sendFeedback(
                                                                    () -> Text.literal(
                                                                            "Glitch effect set to " +
                                                                                    mode.name().toLowerCase(Locale.ROOT) +
                                                                                    " for tag " + updated.id().value()
                                                                    ),
                                                                    true
                                                            );
                                                            return 1;
                                                        })))
                )
        );
    }

    private int assign(com.mojang.brigadier.context.CommandContext<ServerCommandSource> context, boolean replace) {
        ServerPlayerEntity player;
        try {
            player = EntityArgumentType.getPlayer(context, "player");
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException exception) {
            context.getSource().sendError(Text.literal(exception.getMessage()));
            return 0;
        }

        TagId tagId = new TagId(
                StringArgumentType.getString(context, "tag").toLowerCase(Locale.ROOT)
        );
        tagService.assign(player.getUuid(), tagId);
        context.getSource().sendFeedback(
                () -> Text.literal(
                        (replace ? "Set " : "Assigned ") + tagId.value() + " to " + player.getName().getString()
                ),
                true
        );
        return 1;
    }

    private int clear(com.mojang.brigadier.context.CommandContext<ServerCommandSource> context) {
        ServerPlayerEntity player;
        try {
            player = EntityArgumentType.getPlayer(context, "player");
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException exception) {
            context.getSource().sendError(Text.literal(exception.getMessage()));
            return 0;
        }

        tagService.clear(player.getUuid());
        context.getSource().sendFeedback(
                () -> Text.literal("Cleared NameTags from " + player.getName().getString()),
                true
        );
        return 1;
    }

    private static java.util.concurrent.CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> suggestTags(
            com.mojang.brigadier.suggestion.SuggestionsBuilder builder,
            TagService service
    ) {
        return CommandSource.suggestMatching(
                service.list().stream()
                        .map(Tag::id)
                        .map(TagId::value)
                        .sorted()
                        .toList(),
                builder
        );
    }
}
