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

            var tag = CommandManager.literal("tag");
            tag.then(CommandManager.literal("list")
                    .executes(context -> execute(context, service, permissions, messages, configuration, new String[]{"tag", "list"})));
            tag.then(CommandManager.literal("create")
                    .then(CommandManager.argument("tag", StringArgumentType.word())
                            .then(CommandManager.argument("displayName", StringArgumentType.greedyString())
                                    .executes(context -> execute(context, service, permissions, messages, configuration, new String[]{
                                            "tag", "create",
                                            StringArgumentType.getString(context, "tag"),
                                            StringArgumentType.getString(context, "displayName")
                                    })))));
            tag.then(CommandManager.literal("edit")
                    .then(CommandManager.argument("tag", StringArgumentType.word())
                            .suggests((context, builder) -> suggestTags(context, builder, service, permissions, configuration, "edit"))
                            .then(CommandManager.argument("property", StringArgumentType.word())
                                    .suggests((context, builder) -> CommandSource.suggestMatching(
                                            List.of("name", "color", "gradient", "style", "priority", "enabled", "chat"), builder))
                                    .then(CommandManager.argument("value", StringArgumentType.greedyString())
                                            .suggests((context, builder) -> suggestEditValues(context, builder, service, permissions, configuration))
                                            .executes(context -> execute(context, service, permissions, messages, configuration, new String[]{
                                                    "tag", "edit",
                                                    StringArgumentType.getString(context, "tag"),
                                                    StringArgumentType.getString(context, "property"),
                                                    StringArgumentType.getString(context, "value")
                                            })))));
            tag.then(CommandManager.literal("delete")
                    .then(CommandManager.argument("tag", StringArgumentType.word())
                            .suggests((context, builder) -> suggestTags(context, builder, service, permissions, configuration, "delete"))
                            .executes(context -> execute(context, service, permissions, messages, configuration, new String[]{
                                    "tag", "delete", StringArgumentType.getString(context, "tag")
                            })));
            root.then(tag);

            var player = CommandManager.literal("player");
            player.then(CommandManager.literal("give")
                    .then(CommandManager.argument("player", EntityArgumentType.player())
                            .then(CommandManager.argument("tag", StringArgumentType.word())
                                    .suggests((context, builder) -> suggestTags(context, builder, service, permissions, configuration, "give"))
                                    .executes(context -> executeTarget(context, service, permissions, messages, configuration, false))
                                    .then(CommandManager.argument("duration", StringArgumentType.word())
                                            .suggests((context, builder) -> CommandSource.suggestMatching(
                                                    List.of("30m", "1h", "1d", "7d"), builder))
                                            .executes(context -> executeTarget(context, service, permissions, messages, configuration, false))))));
            player.then(CommandManager.literal("set")
                    .then(CommandManager.argument("player", EntityArgumentType.player())
                            .then(CommandManager.argument("tag", StringArgumentType.word())
                                    .suggests((context, builder) -> suggestTags(context, builder, service, permissions, configuration, "set"))
                                    .executes(context -> executeTarget(context, service, permissions, messages, configuration, true)))));
            player.then(CommandManager.literal("remove")
                    .then(CommandManager.argument("player", EntityArgumentType.player())
                            .executes(context -> executeTargetClear(context, service, permissions, messages, configuration, "remove"))));
            player.then(CommandManager.literal("clear")
                    .then(CommandManager.argument("player", EntityArgumentType.player())
                            .executes(context -> executeTargetClear(context, service, permissions, messages, configuration, "clear"))));
            root.then(player);

            var display = CommandManager.literal("display");
            display.then(CommandManager.literal("glitch")
                    .then(CommandManager.argument("tag", StringArgumentType.word())
                            .suggests((context, builder) -> suggestTags(context, builder, service, permissions, configuration, "glitch"))
                            .then(CommandManager.argument("mode", StringArgumentType.word())
                                    .suggests((context, builder) -> CommandSource.suggestMatching(
                                            List.of("white", "colorful"), builder))
                                    .executes(context -> execute(context, service, permissions, messages, configuration, new String[]{
                                            "display", "glitch",
                                            StringArgumentType.getString(context, "tag"),
                                            StringArgumentType.getString(context, "mode")
                                    })))));
            display.then(CommandManager.literal("effect")
                    .then(CommandManager.argument("tag", StringArgumentType.word())
                            .suggests((context, builder) -> suggestTags(context, builder, service, permissions, configuration, "effect"))
                            .then(CommandManager.argument("effect", StringArgumentType.word())
                                    .suggests((context, builder) -> CommandSource.suggestMatching(
                                            List.of("none", "rainbow", "pulse", "wave"), builder))
                                    .executes(context -> execute(context, service, permissions, messages, configuration, new String[]{
                                            "display", "effect",
                                            StringArgumentType.getString(context, "tag"),
                                            StringArgumentType.getString(context, "effect")
                                    })))));
            root.then(display);

            var advanced = CommandManager.literal("advanced");
            advanced.then(CommandManager.literal("role")
                    .then(CommandManager.argument("tag", StringArgumentType.word())
                            .suggests((context, builder) -> suggestTags(context, builder, service, permissions, configuration, "role"))
                            .then(CommandManager.argument("permission", StringArgumentType.word())
                                    .suggests((context, builder) -> CommandSource.suggestMatching(
                                            List.of("clear"), builder))
                                    .executes(context -> execute(context, service, permissions, messages, configuration, new String[]{
                                            "advanced", "role",
                                            StringArgumentType.getString(context, "tag"),
                                            StringArgumentType.getString(context, "permission")
                                    })))));
            var scope = CommandManager.literal("scope")
                    .then(CommandManager.argument("tag", StringArgumentType.word())
                            .suggests((context, builder) -> suggestTags(context, builder, service, permissions, configuration, "scope"))
                            .then(CommandManager.literal("clear")
                                    .executes(context -> execute(context, service, permissions, messages, configuration, new String[]{
                                            "advanced", "scope",
                                            StringArgumentType.getString(context, "tag"),
                                            "clear"
                                    })))
                            .then(CommandManager.literal("world")
                                    .then(CommandManager.argument("world", StringArgumentType.word())
                                            .suggests((context, builder) -> CommandSource.suggestMatching(
                                                    context.getSource().getWorldKeys().stream()
                                                            .map(key -> key.getValue().toString())
                                                            .toList(),
                                                    builder))
                                            .executes(context -> execute(context, service, permissions, messages, configuration, new String[]{
                                                    "advanced", "scope",
                                                    StringArgumentType.getString(context, "tag"),
                                                    "world",
                                                    StringArgumentType.getString(context, "world")
                                            }))))
                            .then(CommandManager.literal("region")
                                    .then(CommandManager.argument("name", StringArgumentType.word())
                                            .then(CommandManager.argument("world", StringArgumentType.word())
                                                    .suggests((context, builder) -> CommandSource.suggestMatching(
                                                            context.getSource().getWorldKeys().stream()
                                                                    .map(key -> key.getValue().toString())
                                                                    .toList(),
                                                            builder))
                                                    .then(CommandManager.argument("minX", StringArgumentType.word())
                                                            .suggests((context, builder) -> CommandSource.suggestMatching(
                                                                    List.of("0", "-100", "100"), builder))
                                                            .then(CommandManager.argument("minY", StringArgumentType.word())
                                                                    .suggests((context, builder) -> CommandSource.suggestMatching(
                                                                            List.of("0", "60", "-60"), builder))
                                                                    .then(CommandManager.argument("minZ", StringArgumentType.word())
                                                                            .suggests((context, builder) -> CommandSource.suggestMatching(
                                                                                    List.of("0", "-100", "100"), builder))
                                                                            .then(CommandManager.argument("maxX", StringArgumentType.word())
                                                                                    .suggests((context, builder) -> CommandSource.suggestMatching(
                                                                                            List.of("0", "100", "1000"), builder))
                                                                                    .then(CommandManager.argument("maxY", StringArgumentType.word())
                                                                                            .suggests((context, builder) -> CommandSource.suggestMatching(
                                                                                                    List.of("60", "100", "320"), builder))
                                                                                            .then(CommandManager.argument("maxZ", StringArgumentType.word())
                                                                                                    .suggests((context, builder) -> CommandSource.suggestMatching(
                                                                                                            List.of("0", "100", "1000"), builder))
                                                                                                    .executes(context -> execute(context, service, permissions, messages, configuration, new String[]{
                                                                                                            "advanced", "scope",
                                                                                                            StringArgumentType.getString(context, "tag"),
                                                                                                            "region",
                                                                                                            StringArgumentType.getString(context, "name"),
                                                                                                            StringArgumentType.getString(context, "world"),
                                                                                                            StringArgumentType.getString(context, "minX"),
                                                                                                            StringArgumentType.getString(context, "minY"),
                                                                                                            StringArgumentType.getString(context, "minZ"),
                                                                                                            StringArgumentType.getString(context, "maxX"),
                                                                                                            StringArgumentType.getString(context, "maxY"),
                                                                                                            StringArgumentType.getString(context, "maxZ")
                                                                                                    }))))))))));
            advanced.then(scope);
            root.then(advanced);

            var admin = CommandManager.literal("admin");
            admin.then(CommandManager.literal("reload")
                    .executes(context -> execute(context, service, permissions, messages, configuration, new String[]{"admin", "reload"})));
            admin.then(CommandManager.literal("export")
                    .then(CommandManager.argument("file", StringArgumentType.word())
                            .suggests((context, builder) -> CommandSource.suggestMatching(List.of("tags.yml"), builder))
                            .executes(context -> execute(context, service, permissions, messages, configuration, new String[]{
                                    "admin", "export", StringArgumentType.getString(context, "file")
                            }))));
            admin.then(CommandManager.literal("import")
                    .then(CommandManager.argument("file", StringArgumentType.word())
                            .suggests((context, builder) -> CommandSource.suggestMatching(List.of("tags.yml"), builder))
                            .executes(context -> execute(context, service, permissions, messages, configuration, new String[]{
                                    "admin", "import", StringArgumentType.getString(context, "file")
                            }))));
            root.then(admin);

            dispatcher.register(root);
        });
    }


    private static java.util.concurrent.CompletableFuture<Suggestions> suggestEditValues(
            CommandContext<ServerCommandSource> context,
            com.mojang.brigadier.suggestion.SuggestionsBuilder builder,
            TagService service,
            PermissionService permissions,
            DefaultConfigurationService configuration
    ) {
        String tag = StringArgumentType.getString(context, "tag");
        String property = StringArgumentType.getString(context, "property");
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
                        new String[]{"tag", "edit", tag, property, prefix}
                )),
                builder
        );
    }


