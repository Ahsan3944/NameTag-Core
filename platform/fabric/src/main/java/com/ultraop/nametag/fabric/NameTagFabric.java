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

            // TAG GROUP
            var tag = CommandManager.literal("tag");

            var tagList = CommandManager.literal("list")
                    .executes(context -> execute(context, service, permissions, messages, configuration,
                            new String[]{"tag", "list"}));
            tag.then(tagList);

            var tagCreate = CommandManager.literal("create");
            var createTag = CommandManager.argument("tag", StringArgumentType.word());
            var createDisplayName = CommandManager.argument("displayName", StringArgumentType.greedyString())
                    .executes(context -> execute(context, service, permissions, messages, configuration,
                            new String[]{
                                    "tag", "create",
                                    StringArgumentType.getString(context, "tag"),
                                    StringArgumentType.getString(context, "displayName")
                            }));
            createTag.then(createDisplayName);
            tagCreate.then(createTag);
            tag.then(tagCreate);

            var tagEdit = CommandManager.literal("edit");
            var editTag = CommandManager.argument("tag", StringArgumentType.word())
                    .suggests((context, builder) ->
                            suggestTags(context, builder, service, permissions, configuration, "edit"));
            var editProperty = CommandManager.argument("property", StringArgumentType.word())
                    .suggests((context, builder) -> CommandSource.suggestMatching(
                            List.of("name", "color", "gradient", "style", "priority", "enabled", "chat"), builder));
            var editValue = CommandManager.argument("value", StringArgumentType.greedyString())
                    .suggests((context, builder) ->
                            suggestEditValues(context, builder, service, permissions, configuration))
                    .executes(context -> execute(context, service, permissions, messages, configuration,
                            new String[]{
                                    "tag", "edit",
                                    StringArgumentType.getString(context, "tag"),
                                    StringArgumentType.getString(context, "property"),
                                    StringArgumentType.getString(context, "value")
                            }));
            editProperty.then(editValue);
            editTag.then(editProperty);
            tagEdit.then(editTag);
            tag.then(tagEdit);

            var tagDelete = CommandManager.literal("delete");
            var deleteTag = CommandManager.argument("tag", StringArgumentType.word())
                    .suggests((context, builder) ->
                            suggestTags(context, builder, service, permissions, configuration, "delete"))
                    .executes(context -> execute(context, service, permissions, messages, configuration,
                            new String[]{"tag", "delete", StringArgumentType.getString(context, "tag")}));
            tagDelete.then(deleteTag);
            tag.then(tagDelete);

            root.then(tag);

            // PLAYER GROUP
            var player = CommandManager.literal("player");

            var playerGive = CommandManager.literal("give");
            var givePlayer = CommandManager.argument("player", EntityArgumentType.player());
            var giveTag = CommandManager.argument("tag", StringArgumentType.word())
                    .suggests((context, builder) ->
                            suggestTags(context, builder, service, permissions, configuration, "give"))
                    .executes(context -> executeTarget(
                            context, service, permissions, messages, configuration, false));
            var giveDuration = CommandManager.argument("duration", StringArgumentType.word())
                    .suggests((context, builder) ->
                            CommandSource.suggestMatching(List.of("30m", "1h", "1d", "7d"), builder))
                    .executes(context -> executeTarget(
                            context, service, permissions, messages, configuration, false));
            giveTag.then(giveDuration);
            givePlayer.then(giveTag);
            playerGive.then(givePlayer);
            player.then(playerGive);

            var playerSet = CommandManager.literal("set");
            var setPlayer = CommandManager.argument("player", EntityArgumentType.player());
            var setTag = CommandManager.argument("tag", StringArgumentType.word())
                    .suggests((context, builder) ->
                            suggestTags(context, builder, service, permissions, configuration, "set"))
                    .executes(context -> executeTarget(
                            context, service, permissions, messages, configuration, true));
            setPlayer.then(setTag);
            playerSet.then(setPlayer);
            player.then(playerSet);

            var playerRemove = CommandManager.literal("remove");
            playerRemove.then(CommandManager.argument("player", EntityArgumentType.player())
                    .executes(context -> executeTargetClear(
                            context, service, permissions, messages, configuration, "remove")));
            player.then(playerRemove);

            var playerClear = CommandManager.literal("clear");
            playerClear.then(CommandManager.argument("player", EntityArgumentType.player())
                    .executes(context -> executeTargetClear(
                            context, service, permissions, messages, configuration, "clear")));
            player.then(playerClear);

            root.then(player);

            // DISPLAY GROUP
            var display = CommandManager.literal("display");

            var displayGlitch = CommandManager.literal("glitch");
            var glitchTag = CommandManager.argument("tag", StringArgumentType.word())
                    .suggests((context, builder) ->
                            suggestTags(context, builder, service, permissions, configuration, "glitch"));
            var glitchMode = CommandManager.argument("mode", StringArgumentType.word())
                    .suggests((context, builder) ->
                            CommandSource.suggestMatching(List.of("white", "colorful"), builder))
                    .executes(context -> execute(context, service, permissions, messages, configuration,
                            new String[]{
                                    "display", "glitch",
                                    StringArgumentType.getString(context, "tag"),
                                    StringArgumentType.getString(context, "mode")
                            }));
            glitchTag.then(glitchMode);
            displayGlitch.then(glitchTag);
            display.then(displayGlitch);

            var displayEffect = CommandManager.literal("effect");
            var effectTag = CommandManager.argument("tag", StringArgumentType.word())
                    .suggests((context, builder) ->
                            suggestTags(context, builder, service, permissions, configuration, "effect"));
            var effectValue = CommandManager.argument("effect", StringArgumentType.word())
                    .suggests((context, builder) ->
                            CommandSource.suggestMatching(List.of("none", "rainbow", "pulse", "wave"), builder))
                    .executes(context -> execute(context, service, permissions, messages, configuration,
                            new String[]{
                                    "display", "effect",
                                    StringArgumentType.getString(context, "tag"),
                                    StringArgumentType.getString(context, "effect")
                            }));
            effectTag.then(effectValue);
            displayEffect.then(effectTag);
            display.then(displayEffect);

            root.then(display);

            // ADVANCED GROUP
            var advanced = CommandManager.literal("advanced");

            var role = CommandManager.literal("role");
            var roleTag = CommandManager.argument("tag", StringArgumentType.word())
                    .suggests((context, builder) ->
                            suggestTags(context, builder, service, permissions, configuration, "role"));
            var rolePermission = CommandManager.argument("permission", StringArgumentType.word())
                    .suggests((context, builder) -> CommandSource.suggestMatching(
                            List.of("clear", "nametag.admin", "nametag.chat", "group.admin", "group.vip", "group.moderator"),
                            builder))
                    .executes(context -> execute(context, service, permissions, messages, configuration,
                            new String[]{
                                    "advanced", "role",
                                    StringArgumentType.getString(context, "tag"),
                                    StringArgumentType.getString(context, "permission")
                            }));
            roleTag.then(rolePermission);
            role.then(roleTag);
            advanced.then(role);

            var scope = CommandManager.literal("scope");
            var scopeTag = CommandManager.argument("tag", StringArgumentType.word())
                    .suggests((context, builder) ->
                            suggestTags(context, builder, service, permissions, configuration, "scope"));
            scopeTag.then(CommandManager.literal("clear")
                    .executes(context -> execute(context, service, permissions, messages, configuration,
                            new String[]{
                                    "advanced", "scope",
                                    StringArgumentType.getString(context, "tag"),
                                    "clear"
                            })));

            var scopeWorld = CommandManager.literal("world");
            scopeWorld.then(CommandManager.argument("world", StringArgumentType.word())
                    .suggests((context, builder) -> CommandSource.suggestMatching(
                            context.getSource().getWorldKeys().stream()
                                    .map(key -> key.getValue().toString())
                                    .toList(),
                            builder))
                    .executes(context -> execute(context, service, permissions, messages, configuration,
                            new String[]{
                                    "advanced", "scope",
                                    StringArgumentType.getString(context, "tag"),
                                    "world",
                                    StringArgumentType.getString(context, "world")
                            })));
            scopeTag.then(scopeWorld);

            var scopeRegion = CommandManager.literal("region");
            var regionName = CommandManager.argument("name", StringArgumentType.word());
            var regionWorld = CommandManager.argument("world", StringArgumentType.word())
                    .suggests((context, builder) -> CommandSource.suggestMatching(
                            context.getSource().getWorldKeys().stream()
                                    .map(key -> key.getValue().toString())
                                    .toList(),
                            builder));
            var minX = CommandManager.argument("minX", StringArgumentType.word())
                    .suggests((context, builder) -> CommandSource.suggestMatching(
                            List.of("0", "-100", "100"), builder));
            var minY = CommandManager.argument("minY", StringArgumentType.word())
                    .suggests((context, builder) -> CommandSource.suggestMatching(
                            List.of("0", "60", "-60"), builder));
            var minZ = CommandManager.argument("minZ", StringArgumentType.word())
                    .suggests((context, builder) -> CommandSource.suggestMatching(
                            List.of("0", "-100", "100"), builder));
            var maxX = CommandManager.argument("maxX", StringArgumentType.word())
                    .suggests((context, builder) -> CommandSource.suggestMatching(
                            List.of("0", "100", "1000"), builder));
            var maxY = CommandManager.argument("maxY", StringArgumentType.word())
                    .suggests((context, builder) -> CommandSource.suggestMatching(
                            List.of("60", "100", "320"), builder));
            var maxZ = CommandManager.argument("maxZ", StringArgumentType.word())
                    .suggests((context, builder) -> CommandSource.suggestMatching(
                            List.of("0", "100", "1000"), builder))
                    .executes(context -> execute(context, service, permissions, messages, configuration,
                            new String[]{
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
                            }));
            maxY.then(maxZ);
            maxX.then(maxY);
            minZ.then(maxX);
            minY.then(minZ);
            minX.then(minY);
            regionWorld.then(minX);
            regionName.then(regionWorld);
            scopeRegion.then(regionName);
            scopeTag.then(scopeRegion);
            scope.then(scopeTag);
            advanced.then(scope);

            root.then(advanced);

            // ADMIN GROUP
            var admin = CommandManager.literal("admin");
            admin.then(CommandManager.literal("reload")
                    .executes(context -> execute(context, service, permissions, messages, configuration,
                            new String[]{"admin", "reload"})));

            var export = CommandManager.literal("export");
            export.then(CommandManager.argument("file", StringArgumentType.word())
                    .suggests((context, builder) ->
                            CommandSource.suggestMatching(List.of("tags.yml"), builder))
                    .executes(context -> execute(context, service, permissions, messages, configuration,
                            new String[]{"admin", "export", StringArgumentType.getString(context, "file")})));
            admin.then(export);

            var importCommand = CommandManager.literal("import");
            importCommand.then(CommandManager.argument("file", StringArgumentType.word())
                    .suggests((context, builder) ->
                            CommandSource.suggestMatching(List.of("tags.yml"), builder))
                    .executes(context -> execute(context, service, permissions, messages, configuration,
                            new String[]{"admin", "import", StringArgumentType.getString(context, "file")})));
            admin.then(importCommand);

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
