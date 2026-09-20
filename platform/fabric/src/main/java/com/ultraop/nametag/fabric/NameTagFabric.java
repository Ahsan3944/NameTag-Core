package com.ultraop.nametag.fabric;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.ultraop.nametag.api.ConfigurationService;
import com.ultraop.nametag.api.NameTagCommandHandler;
import com.ultraop.nametag.api.TagService;
import com.ultraop.nametag.common.DefaultMessageService;
import com.ultraop.nametag.common.DefaultNameTagCommandHandler;
import com.ultraop.nametag.common.DefaultTagService;
import com.ultraop.nametag.common.YamlPlayerAssignmentRepository;
import com.ultraop.nametag.common.YamlTagRepository;
import com.ultraop.nametag.fabric.v1_21_11.Fabric2111Adapter;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.List;

public final class NameTagFabric {
    private NameTagFabric() {}

    public static void bootstrap() {
        java.nio.file.Path dataDirectory = FabricLoader.getInstance()
                .getConfigDir()
                .resolve("nametag-core");
        ConfigurationService configuration = new com.ultraop.nametag.common.DefaultConfigurationService(
                dataDirectory.resolve("configuration.yml")
        );
        TagService service = new DefaultTagService(
                new YamlTagRepository(dataDirectory.resolve("tags.yml")),
                new YamlPlayerAssignmentRepository(dataDirectory.resolve("assignments.yml"))
        );

        new Fabric2111Adapter(service);
        registerCommands(service, configuration);
    }

    private static void registerCommands(TagService service, ConfigurationService configuration) {
        DefaultMessageService messages = new DefaultMessageService();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            var root = CommandManager.literal("nametag")
                    .requires(CommandManager.requirePermissionLevel(CommandManager.GAMEMASTERS_CHECK));

            root.then(CommandManager.literal("list")
                    .executes(context -> execute(context, service, messages, configuration, new String[]{"list"})));

            root.then(CommandManager.literal("create")
                    .then(CommandManager.argument("tag", StringArgumentType.word())
                            .then(CommandManager.argument("displayName", StringArgumentType.greedyString())
                                    .executes(context -> execute(context, service, messages, configuration, new String[]{
                                            "create",
                                            StringArgumentType.getString(context, "tag"),
                                            StringArgumentType.getString(context, "displayName")
                                    })))));

            root.then(CommandManager.literal("delete")
                    .then(CommandManager.argument("tag", StringArgumentType.word())
                            .suggests((context, builder) ->
                                    suggestTags(context, builder, service, configuration, "delete"))
                            .executes(context -> execute(context, service, messages, configuration, new String[]{
                                    "delete",
                                    StringArgumentType.getString(context, "tag")
                            }))));

            root.then(CommandManager.literal("give")
                    .then(CommandManager.argument("player", EntityArgumentType.player())
                            .then(CommandManager.argument("tag", StringArgumentType.word())
                                    .suggests((context, builder) ->
                                            suggestTags(context, builder, service, configuration, "give"))
                                    .executes(context -> executeTarget(
                                            context, service, messages, configuration, false
                                    )))));

            root.then(CommandManager.literal("set")
                    .then(CommandManager.argument("player", EntityArgumentType.player())
                            .then(CommandManager.argument("tag", StringArgumentType.word())
                                    .suggests((context, builder) ->
                                            suggestTags(context, builder, service, configuration, "set"))
                                    .executes(context -> executeTarget(
                                            context, service, messages, configuration, true
                                    )))));

            root.then(CommandManager.literal("remove")
                    .then(CommandManager.argument("player", EntityArgumentType.player())
                            .executes(context -> executeTargetClear(
                                    context, service, messages, configuration, "remove"
                            ))));

            root.then(CommandManager.literal("clear")
                    .then(CommandManager.argument("player", EntityArgumentType.player())
                            .executes(context -> executeTargetClear(
                                    context, service, messages, configuration, "clear"
                            ))));

            root.then(CommandManager.literal("glitch")
                    .then(CommandManager.argument("tag", StringArgumentType.word())
                            .suggests((context, builder) ->
                                    suggestTags(context, builder, service, configuration, "glitch"))
                            .then(CommandManager.argument("mode", StringArgumentType.word())
                                    .suggests((context, builder) ->
                                            CommandSource.suggestMatching(
                                                    List.of("white", "colorful"),
                                                    builder
                                            ))
                                    .executes(context -> execute(context, service, messages, configuration, new String[]{
                                            "glitch",
                                            StringArgumentType.getString(context, "tag"),
                                            StringArgumentType.getString(context, "mode")
                                    })))));

            dispatcher.register(root);
        });
    }

    private static int execute(
            CommandContext<ServerCommandSource> context,
            TagService service,
            DefaultMessageService messages,
            ConfigurationService configuration,
            String[] args
    ) {
        handler(context.getSource(), service, messages, configuration)
                .execute(new com.ultraop.nametag.api.CommandContext(
                        new FabricCommandSource(context.getSource()),
                        args
                ));
        return 1;
    }

    private static int executeTarget(
            CommandContext<ServerCommandSource> context,
            TagService service,
            DefaultMessageService messages,
            ConfigurationService configuration,
            boolean setActive
    ) {
        try {
            ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
            String subcommand = setActive ? "set" : "give";
            String tag = StringArgumentType.getString(context, "tag");
            return execute(
                    context,
                    service,
                    messages,
                    configuration,
                    new String[]{subcommand, player.getName().getString(), tag}
            );
        } catch (CommandSyntaxException exception) {
            context.getSource().sendError(Text.literal(exception.getMessage()));
            return 0;
        }
    }

    private static int executeTargetClear(
            CommandContext<ServerCommandSource> context,
            TagService service,
            DefaultMessageService messages,
            ConfigurationService configuration,
            String subcommand
    ) {
        try {
            ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
            return execute(
                    context,
                    service,
                    messages,
                    configuration,
                    new String[]{subcommand, player.getName().getString()}
            );
        } catch (CommandSyntaxException exception) {
            context.getSource().sendError(Text.literal(exception.getMessage()));
            return 0;
        }
    }

    private static java.util.concurrent.CompletableFuture<Suggestions> suggestTags(
            CommandContext<ServerCommandSource> context,
            com.mojang.brigadier.suggestion.SuggestionsBuilder builder,
            TagService service,
            ConfigurationService configuration,
            String subcommand
    ) {
        String prefix = builder.getRemaining();
        NameTagCommandHandler handler = handler(
                context.getSource(),
                service,
                new DefaultMessageService(),
                configuration
        );
        return CommandSource.suggestMatching(
                handler.suggest(new com.ultraop.nametag.api.CommandContext(
                        new FabricCommandSource(context.getSource()),
                        subcommand.equals("give") || subcommand.equals("set")
                                ? new String[]{subcommand, "", prefix}
                                : new String[]{subcommand, prefix}
                )),
                builder
        );
    }

    private static NameTagCommandHandler handler(
            ServerCommandSource source,
            TagService service,
            DefaultMessageService messages,
            ConfigurationService configuration
    ) {
        return new DefaultNameTagCommandHandler(
                service,
                new FabricPlayerResolver(source.getServer()),
                messages,
                configuration
        );
    }
}
