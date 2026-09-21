package com.ultraop.nametag.fabric;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.ultraop.nametag.api.NameTagCommandHandler;
import com.ultraop.nametag.api.PermissionService;
import com.ultraop.nametag.api.TagService;
import com.ultraop.nametag.common.DefaultConfigurationService;
import com.ultraop.nametag.common.DefaultMessageService;
import com.ultraop.nametag.common.DefaultNameTagCommandHandler;
import com.ultraop.nametag.common.DefaultTagService;
import com.ultraop.nametag.common.FileTagAuditLogger;
import com.ultraop.nametag.common.StorageConfiguration;
import com.ultraop.nametag.common.StorageFactory;
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
        DefaultConfigurationService configuration = new DefaultConfigurationService(
                dataDirectory.resolve("configuration.yml")
        );
        StorageConfiguration storageConfiguration = StorageConfiguration.loadOrCreate(
                dataDirectory.resolve("storage.yml"), dataDirectory);
        StorageFactory.StorageRepositories storage = StorageFactory.open(
                dataDirectory, storageConfiguration);
        PermissionService permissions = new FabricPermissionService();
        TagService service = new DefaultTagService(
                storage.tags(),
                storage.assignments(),
                permissions
        );

        new Fabric2111Adapter(service, configuration);
        FileTagAuditLogger.register(service.events(), dataDirectory.resolve("audit.log"));
        registerCommands(service, permissions, configuration);
    }

    private static void registerCommands(
            TagService service,
            PermissionService permissions,
            DefaultConfigurationService configuration
    ) {
        DefaultMessageService messages = new DefaultMessageService();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            var root = CommandManager.literal("nametag");

            root.then(CommandManager.literal("list")
                    .executes(context -> execute(context, service, permissions, messages, configuration, new String[]{"list"})));

            root.then(CommandManager.literal("reload")
                    .executes(context -> execute(context, service, permissions, messages, configuration, new String[]{"reload"})));

            root.then(CommandManager.literal("create")
                    .then(CommandManager.argument("tag", StringArgumentType.word())
                            .then(CommandManager.argument("displayName", StringArgumentType.greedyString())
                                    .executes(context -> execute(context, service, permissions, messages, configuration, new String[]{
                                            "create",
                                            StringArgumentType.getString(context, "tag"),
                                            StringArgumentType.getString(context, "displayName")
                                    })))));

            root.then(CommandManager.literal("edit")
                    .then(CommandManager.argument("tag", StringArgumentType.word())
                            .suggests((context, builder) ->
                                    suggestTags(context, builder, service, permissions, configuration, "edit"))
                            .then(CommandManager.argument("property", StringArgumentType.word())
                                    .suggests((context, builder) ->
                                            CommandSource.suggestMatching(
                                                    List.of("name", "color", "gradient", "style", "priority", "enabled", "chat"),
                                                    builder
                                            ))
                                    .then(CommandManager.argument("value", StringArgumentType.greedyString())
                                            .suggests((context, builder) -> suggestEditValues(context, builder))
                                            .executes(context -> execute(context, service, permissions, messages, configuration, new String[]{
                                                    "edit",
                                                    StringArgumentType.getString(context, "tag"),
                                                    StringArgumentType.getString(context, "property"),
                                                    StringArgumentType.getString(context, "value")
                                            }))))));

            root.then(CommandManager.literal("delete")
                    .then(CommandManager.argument("tag", StringArgumentType.word())
                            .suggests((context, builder) ->
                                    suggestTags(context, builder, service, permissions, configuration, "delete"))
                            .executes(context -> execute(context, service, permissions, messages, configuration, new String[]{
                                    "delete",
                                    StringArgumentType.getString(context, "tag")
                            }))));

            root.then(CommandManager.literal("give")
                    .then(CommandManager.argument("player", EntityArgumentType.player())
                            .then(CommandManager.argument("tag", StringArgumentType.word())
                                    .suggests((context, builder) ->
                                            suggestTags(context, builder, service, permissions, configuration, "give"))
                                    .executes(context -> executeTarget(
                                            context, service, permissions, messages, configuration, false
                                    ))
                                    .then(CommandManager.argument("duration", StringArgumentType.word())
                                            .executes(context -> executeTarget(
                                                    context, service, permissions, messages, configuration, false
                                            ))))));

            root.then(CommandManager.literal("set")
                    .then(CommandManager.argument("player", EntityArgumentType.player())
                            .then(CommandManager.argument("tag", StringArgumentType.word())
                                    .suggests((context, builder) ->
                                            suggestTags(context, builder, service, permissions, configuration, "set"))
                                    .executes(context -> executeTarget(
                                            context, service, permissions, messages, configuration, true
                                    )))));

            root.then(CommandManager.literal("remove")
                    .then(CommandManager.argument("player", EntityArgumentType.player())
                            .executes(context -> executeTargetClear(
                                    context, service, permissions, messages, configuration, "remove"
                            ))));

            root.then(CommandManager.literal("clear")
                    .then(CommandManager.argument("player", EntityArgumentType.player())
                            .executes(context -> executeTargetClear(
                                    context, service, permissions, messages, configuration, "clear"
                            ))));

            root.then(CommandManager.literal("export")
                    .then(CommandManager.argument("file", StringArgumentType.word())
                            .suggests((context, builder) -> CommandSource.suggestMatching(List.of("tags.yml"), builder))
                            .executes(context -> execute(context, service, permissions, messages, configuration, new String[]{
                                    "export", StringArgumentType.getString(context, "file")
                            }))));

            root.then(CommandManager.literal("import")
                    .then(CommandManager.argument("file", StringArgumentType.word())
                            .suggests((context, builder) -> CommandSource.suggestMatching(List.of("tags.yml"), builder))
                            .executes(context -> execute(context, service, permissions, messages, configuration, new String[]{
                                    "import", StringArgumentType.getString(context, "file")
                            }))));

            root.then(CommandManager.literal("glitch")
                    .then(CommandManager.argument("tag", StringArgumentType.word())
                            .suggests((context, builder) ->
                                    suggestTags(context, builder, service, permissions, configuration, "glitch"))
                            .then(CommandManager.argument("mode", StringArgumentType.word())
                                    .suggests((context, builder) ->
                                            CommandSource.suggestMatching(List.of("white", "colorful"), builder))
                                    .executes(context -> execute(context, service, permissions, messages, configuration, new String[]{
                                            "glitch",
                                            StringArgumentType.getString(context, "tag"),
                                            StringArgumentType.getString(context, "mode")
                                    })))));

            root.then(CommandManager.literal("effect")
                    .then(CommandManager.argument("tag", StringArgumentType.word())
                            .suggests((context, builder) ->
                                    suggestTags(context, builder, service, permissions, configuration, "effect"))
                            .then(CommandManager.argument("effect", StringArgumentType.word())
                                    .suggests((context, builder) ->
                                            CommandSource.suggestMatching(List.of("none", "rainbow", "pulse", "wave"), builder))
                                    .executes(context -> execute(context, service, permissions, messages, configuration, new String[]{
                                            "effect",
                                            StringArgumentType.getString(context, "tag"),
                                            StringArgumentType.getString(context, "effect")
                                    })))));

            root.then(CommandManager.literal("scope")
                    .then(CommandManager.argument("tag", StringArgumentType.word())
                            .suggests((context, builder) ->
                                    suggestTags(context, builder, service, permissions, configuration, "scope"))
                            .then(CommandManager.argument("settings", StringArgumentType.greedyString())
                                    .suggests((context, builder) -> CommandSource.suggestMatching(
                                            List.of("clear", "world", "region"), builder))
                                    .executes(context -> execute(context, service, permissions, messages, configuration, new String[]{
                                            "scope",
                                            StringArgumentType.getString(context, "tag"),
                                            StringArgumentType.getString(context, "settings")
                                    })))));
 
            root.then(CommandManager.literal("role")
                    .then(CommandManager.argument("tag", StringArgumentType.word())
                            .suggests((context, builder) ->
                                    suggestTags(context, builder, service, permissions, configuration, "role"))
                            .then(CommandManager.argument("permission", StringArgumentType.word())
                                    .suggests((context, builder) ->
                                            CommandSource.suggestMatching(List.of("clear"), builder))
                                    .executes(context -> execute(context, service, permissions, messages, configuration, new String[]{
                                            "role",
                                            StringArgumentType.getString(context, "tag"),
                                            StringArgumentType.getString(context, "permission")
                                    })))));

            dispatcher.register(root);
        });
    }

    private static java.util.concurrent.CompletableFuture<Suggestions> suggestEditValues(
            CommandContext<ServerCommandSource> context,
            com.mojang.brigadier.suggestion.SuggestionsBuilder builder
    ) {
        String property = StringArgumentType.getString(context, "property").toLowerCase(java.util.Locale.ROOT);
        return switch (property) {
            case "color" -> CommandSource.suggestMatching(
                    List.of("black", "dark_blue", "dark_green", "dark_aqua", "dark_red", "dark_purple",
                            "gold", "gray", "dark_gray", "blue", "green", "aqua", "red", "light_purple",
                            "yellow", "white", "random"),
                    builder
            );
            case "style" -> CommandSource.suggestMatching(
                    List.of("plain", "bold", "italic", "bold_italic"),
                    builder
            );
            case "enabled", "chat" -> CommandSource.suggestMatching(
                    List.of("true", "false"),
                    builder
            );
            default -> Suggestions.empty();
        };
    }

    private static int execute(
            CommandContext<ServerCommandSource> context,
            TagService service,
            PermissionService permissions,
            DefaultMessageService messages,
            DefaultConfigurationService configuration,
            String[] args
    ) {
        handler(context.getSource(), service, messages, configuration)
                .execute(new com.ultraop.nametag.api.CommandContext(
                        new FabricCommandSource(context.getSource(), permissions),
                        args
                ));
        return 1;
    }

    private static int executeTarget(
            CommandContext<ServerCommandSource> context,
            TagService service,
            PermissionService permissions,
            DefaultMessageService messages,
            DefaultConfigurationService configuration,
            boolean setActive
    ) {
        try {
            ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
            String subcommand = setActive ? "set" : "give";
            String tag = StringArgumentType.getString(context, "tag");
            boolean hasDuration = context.getNodes().stream()
                    .anyMatch(node -> node.getNode().getName().equals("duration"));
            String[] args = hasDuration
                    ? new String[]{subcommand, player.getName().getString(), tag, StringArgumentType.getString(context, "duration")}
                    : new String[]{subcommand, player.getName().getString(), tag};
            return execute(context, service, permissions, messages, configuration, args);
        } catch (CommandSyntaxException exception) {
            context.getSource().sendError(Text.literal(exception.getMessage()));
            return 0;
        }
    }

    private static int executeTargetClear(
            CommandContext<ServerCommandSource> context,
            TagService service,
            PermissionService permissions,
            DefaultMessageService messages,
            DefaultConfigurationService configuration,
            String subcommand
    ) {
        try {
            ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
            return execute(
                    context,
                    service,
                    permissions,
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
            PermissionService permissions,
            DefaultConfigurationService configuration,
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
                        new FabricCommandSource(context.getSource(), permissions),
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
            DefaultConfigurationService configuration
    ) {
        java.nio.file.Path dataDirectory = FabricLoader.getInstance()
                .getConfigDir()
                .resolve("nametag-core");
        return new DefaultNameTagCommandHandler(
                service,
                new FabricPlayerResolver(source.getServer()),
                messages,
                configuration,
                configuration,
                dataDirectory
        );
    }
}
