package com.ultraop.nametag.fabric;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.command.argument.IdentifierArgumentType;
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

            // CREATE
            // The create wizard deliberately exposes only three top-level choices:
            // name, item, and name+item. Every choice then walks through the next
            // valid value instead of dumping implementation properties into one list.

            var createName = CommandManager.literal("name");
            var createNameValue = CommandManager.argument("displayName", StringArgumentType.string());
            createNameValue.then(createStyleNode(
                    service, permissions, messages, configuration, "name"));
            createName.then(createNameValue);

            var createItem = CommandManager.literal("item");
            var createItemValue = CommandManager.argument("item", IdentifierArgumentType.identifier())
                    .suggests((context, builder) -> suggestCreateItems(context, builder, permissions));
            createItemValue.then(createSpinNode(
                    service, permissions, messages, configuration, false));
            createItem.then(createItemValue);

            var createNameItem = CommandManager.literal("name+item");
            var createNameItemValue = CommandManager.argument("item", IdentifierArgumentType.identifier())
                    .suggests((context, builder) -> suggestCreateItems(context, builder, permissions));
            var createNameItemName = CommandManager.literal("name");
            var createNameItemNameValue = CommandManager.argument("displayName", StringArgumentType.string());
            createNameItemNameValue.then(createStyleNode(
                    service, permissions, messages, configuration, "name+item"));
            createNameItemName.then(createNameItemNameValue);
            createNameItemValue.then(createNameItemName);
            createNameItem.then(createNameItemValue);

            createTag.then(createName);
            createTag.then(createItem);
            createTag.then(createNameItem);
            tagCreate.then(createTag);
            tag.then(tagCreate);

            // EDIT
            var tagEdit = CommandManager.literal("edit");
            var editTag = CommandManager.argument("tag", StringArgumentType.word())
                    .suggests((context, builder) ->
                            suggestTags(context, builder, service, permissions, configuration, "edit"));

            var editName = CommandManager.literal("name");
            editName.then(CommandManager.argument("displayName", StringArgumentType.word())
                    .executes(context -> executeEditOption(context, service, permissions, messages, configuration,
                            "name", StringArgumentType.getString(context, "displayName"))));

            var editItem = CommandManager.literal("item");
            var editItemValue = CommandManager.argument("item", StringArgumentType.word())
                    .suggests((context, builder) -> suggestEditItems(context, builder, permissions))
                    .executes(context -> executeEditOption(context, service, permissions, messages, configuration,
                            "item", IdentifierArgumentType.getIdentifier(context, "item").toString()));
            editItemValue.then(CommandManager.literal("mode")
                    .then(CommandManager.argument("mode", StringArgumentType.word())
                            .suggests((context, builder) -> CommandSource.suggestMatching(
                                    List.of("static", "rotate"), builder))
                            .executes(context -> executeEditNestedOption(context, service, permissions, messages, configuration,
                                    "item", "mode", StringArgumentType.getString(context, "mode")))));
            editItemValue.then(CommandManager.literal("speed")
                    .then(CommandManager.argument("speed", StringArgumentType.word())
                            .suggests((context, builder) -> CommandSource.suggestMatching(
                                    List.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10"), builder))
                            .executes(context -> executeEditNestedOption(context, service, permissions, messages, configuration,
                                    "item", "speed", StringArgumentType.getString(context, "speed")))));
            editItemValue.then(CommandManager.literal("clear")
                    .executes(context -> executeEditOption(context, service, permissions, messages, configuration,
                            "item", "clear")));
            editItem.then(editItemValue);

            var editAppearance = CommandManager.literal("appearance");
            editAppearance.then(createEditColorNode(service, permissions, messages, configuration));
            editAppearance.then(createEditGradientNode(service, permissions, messages, configuration));
            editAppearance.then(createEditStyleNode(service, permissions, messages, configuration));
            editAppearance.then(createEditEffectNode(service, permissions, messages, configuration));
            editAppearance.then(createEditGlitchNode(service, permissions, messages, configuration));

            var editBehavior = CommandManager.literal("behavior");
            editBehavior.then(createEditSimpleOptionNode("priority",
                    List.of("0", "10", "25", "50", "100", "1000"),
                    service, permissions, messages, configuration));
            editBehavior.then(createEditSimpleOptionNode("enabled",
                    List.of("true", "false"),
                    service, permissions, messages, configuration));
            editBehavior.then(createEditSimpleOptionNode("chat",
                    List.of("true", "false"),
                    service, permissions, messages, configuration));

            editTag.then(editName);
            editTag.then(editItem);
            editTag.then(editAppearance);
            editTag.then(editBehavior);
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

            var playerSet = CommandManager.literal("set");
            var setPlayer = CommandManager.argument("player", EntityArgumentType.player());
            var setTag = CommandManager.argument("tag", StringArgumentType.word())
                    .suggests((context, builder) ->
                            suggestTags(context, builder, service, permissions, configuration, "set"))
                    .executes(context -> executeTarget(
                            context, service, permissions, messages, configuration, true));
            var setDuration = CommandManager.argument("duration", StringArgumentType.word())
                    .suggests((context, builder) ->
                            CommandSource.suggestMatching(List.of("30m", "1h", "1d", "7d"), builder))
                    .executes(context -> executeTarget(
                            context, service, permissions, messages, configuration, true));
            setTag.then(setDuration);
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

            // DISPLAY ITEM
            var displayItem = CommandManager.literal("item");
            var itemTag = CommandManager.argument("tag", StringArgumentType.word())
                    .suggests((context, builder) ->
                            suggestTags(context, builder, service, permissions, configuration, "item"));
            var itemSet = CommandManager.literal("set");
            itemSet.then(CommandManager.argument("item", StringArgumentType.word())
                    .suggests((context, builder) -> suggestItemValues(
                            context, builder, service, permissions, configuration))
                    .executes(context -> execute(context, service, permissions, messages, configuration,
                            new String[]{
                                    "display", "item",
                                    StringArgumentType.getString(context, "tag"),
                                    "set",
                                    StringArgumentType.getString(context, "item")
                            })));
            itemTag.then(itemSet);

            var itemMode = CommandManager.literal("mode");
            itemMode.then(CommandManager.argument("mode", StringArgumentType.word())
                    .suggests((context, builder) -> CommandSource.suggestMatching(
                            List.of("static", "rotate"), builder))
                    .executes(context -> execute(context, service, permissions, messages, configuration,
                            new String[]{
                                    "display", "item",
                                    StringArgumentType.getString(context, "tag"),
                                    "mode",
                                    StringArgumentType.getString(context, "mode")
                            })));
            itemTag.then(itemMode);

            var itemSpeed = CommandManager.literal("speed");
            itemSpeed.then(CommandManager.argument("speed", StringArgumentType.word())
                    .suggests((context, builder) -> CommandSource.suggestMatching(
                            List.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10"), builder))
                    .executes(context -> execute(context, service, permissions, messages, configuration,
                            new String[]{
                                    "display", "item",
                                    StringArgumentType.getString(context, "tag"),
                                    "speed",
                                    StringArgumentType.getString(context, "speed")
                            })));
            itemTag.then(itemSpeed);

            itemTag.then(CommandManager.literal("clear")
                    .executes(context -> execute(context, service, permissions, messages, configuration,
                            new String[]{
                                    "display", "item",
                                    StringArgumentType.getString(context, "tag"),
                                    "clear"
                            })));
            displayItem.then(itemTag);
            display.then(displayItem);

            // HELP
            var help = CommandManager.literal("help");
            help.executes(context -> executeHelp(context, service, permissions, messages, configuration, null));
            help.then(CommandManager.argument("category", StringArgumentType.word())
                    .suggests((context, builder) -> CommandSource.suggestMatching(
                            List.of("tag", "player", "display", "advanced", "admin"), builder))
                    .executes(context -> executeHelp(
                            context, service, permissions, messages, configuration,
                            StringArgumentType.getString(context, "category"))));
            root.then(help);

            root.then(CommandManager.literal("info")
                    .executes(context -> executeInfo(context, service, permissions, messages, configuration)));

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


    private static java.util.concurrent.CompletableFuture<Suggestions> suggestCreateOptions(
            CommandContext<ServerCommandSource> context,
            com.mojang.brigadier.suggestion.SuggestionsBuilder builder,
            TagService service,
            PermissionService permissions,
            DefaultConfigurationService configuration
    ) {
        String tag = StringArgumentType.getString(context, "tag");
        String raw = builder.getRemaining();
        String[] tail;
        if (raw.isEmpty()) {
            tail = new String[]{""};
        } else {
            String trimmed = raw.trim();
            tail = trimmed.isEmpty() ? new String[]{""} : trimmed.split("\\s+");
            if (raw.endsWith(" ")) {
                tail = java.util.Arrays.copyOf(tail, tail.length + 1);
                tail[tail.length - 1] = "";
            }
        }

        String[] args = new String[2 + tail.length];
        args[0] = "tag";
        args[1] = "create";
        System.arraycopy(tail, 0, args, 2, tail.length);

        NameTagCommandHandler handler = handler(
                context.getSource(),
                service,
                new DefaultMessageService(),
                configuration
        );
        return CommandSource.suggestMatching(
                handler.suggest(new com.ultraop.nametag.api.CommandContext(
                        new FabricCommandSource(context.getSource(), permissions),
                        prependTag(args, tag)
                )),
                builder
        );
    }

    private static String[] prependTag(String[] createArgs, String tag) {
        String[] result = new String[createArgs.length + 1];
        result[0] = "tag";
        result[1] = "create";
        result[2] = tag;
        System.arraycopy(createArgs, 2, result, 3, createArgs.length - 2);
        return result;
    }

    private static java.util.concurrent.CompletableFuture<Suggestions> suggestItemValues(
            CommandContext<ServerCommandSource> context,
            com.mojang.brigadier.suggestion.SuggestionsBuilder builder,
            TagService service,
            PermissionService permissions,
            DefaultConfigurationService configuration
    ) {
        String tag = StringArgumentType.getString(context, "tag");
        NameTagCommandHandler handler = handler(context.getSource(), service, new DefaultMessageService(), configuration);
        return CommandSource.suggestMatching(
                handler.suggest(new com.ultraop.nametag.api.CommandContext(
                        new FabricCommandSource(context.getSource(), permissions),
                        new String[]{"display", "item", tag, "set", builder.getRemaining()}
                )),
                builder
        );
    }

    private static int executeHelp(
            CommandContext<ServerCommandSource> context,
            TagService service,
            PermissionService permissions,
            DefaultMessageService messages,
            DefaultConfigurationService configuration,
            String category
    ) {
        String[] args = category == null ? new String[]{"help"} : new String[]{"help", category};
        return execute(context, service, permissions, messages, configuration, args);
    }

    private static int executeInfo(
            CommandContext<ServerCommandSource> context,
            TagService service,
            PermissionService permissions,
            DefaultMessageService messages,
            DefaultConfigurationService configuration
    ) {
        return execute(context, service, permissions, messages, configuration, new String[]{"info"});
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

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<ServerCommandSource> createEditColorNode(
            TagService service, PermissionService permissions, DefaultMessageService messages,
            DefaultConfigurationService configuration) {
        var node = CommandManager.literal("color");
        node.then(CommandManager.argument("color", StringArgumentType.word())
                .suggests((context, builder) -> CommandSource.suggestMatching(
                        List.of("black", "dark_blue", "dark_green", "dark_aqua", "dark_red",
                                "dark_purple", "gold", "gray", "dark_gray", "blue", "green",
                                "aqua", "red", "light_purple", "yellow", "white", "random"),
                        builder))
                .executes(context -> executeEditOption(context, service, permissions, messages, configuration,
                        "color", StringArgumentType.getString(context, "color"))));
        return node;
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<ServerCommandSource> createEditGradientNode(
            TagService service, PermissionService permissions, DefaultMessageService messages,
            DefaultConfigurationService configuration) {
        var node = CommandManager.literal("gradient");
        var start = CommandManager.argument("startHex", StringArgumentType.word());
        start.then(CommandManager.argument("endHex", StringArgumentType.word())
                .executes(context -> executeEditGradient(context, service, permissions, messages, configuration)));
        node.then(start);
        return node;
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<ServerCommandSource> createEditStyleNode(
            TagService service, PermissionService permissions, DefaultMessageService messages,
            DefaultConfigurationService configuration) {
        var node = CommandManager.literal("style");
        node.then(CommandManager.argument("style", StringArgumentType.word())
                .suggests((context, builder) -> CommandSource.suggestMatching(
                        List.of("plain", "bold", "italic", "underlined", "strikethrough", "obfuscated",
                                "bold_italic", "bold_underlined", "italic_underlined"), builder))
                .executes(context -> executeEditOption(context, service, permissions, messages, configuration,
                        "style", StringArgumentType.getString(context, "style"))));
        return node;
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<ServerCommandSource> createEditEffectNode(
            TagService service, PermissionService permissions, DefaultMessageService messages,
            DefaultConfigurationService configuration) {
        var node = CommandManager.literal("effect");
        node.then(CommandManager.argument("effect", StringArgumentType.word())
                .suggests((context, builder) -> CommandSource.suggestMatching(
                        List.of("none", "rainbow", "pulse", "wave"), builder))
                .executes(context -> executeEditOption(context, service, permissions, messages, configuration,
                        "effect", StringArgumentType.getString(context, "effect"))));
        return node;
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<ServerCommandSource> createEditGlitchNode(
            TagService service, PermissionService permissions, DefaultMessageService messages,
            DefaultConfigurationService configuration) {
        var node = CommandManager.literal("glitch");
        node.then(CommandManager.argument("glitch", StringArgumentType.word())
                .suggests((context, builder) -> CommandSource.suggestMatching(
                        List.of("none", "white", "colorful"), builder))
                .executes(context -> executeEditOption(context, service, permissions, messages, configuration,
                        "glitch", StringArgumentType.getString(context, "glitch"))));
        return node;
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<ServerCommandSource> createEditSimpleOptionNode(
            String property, List<String> values, TagService service, PermissionService permissions,
            DefaultMessageService messages, DefaultConfigurationService configuration) {
        var node = CommandManager.literal(property);
        node.then(CommandManager.argument(property, StringArgumentType.word())
                .suggests((context, builder) -> CommandSource.suggestMatching(values, builder))
                .executes(context -> executeEditOption(context, service, permissions, messages, configuration,
                        property, StringArgumentType.getString(context, property))));
        return node;
    }

    private static java.util.concurrent.CompletableFuture<Suggestions> suggestEditItems(
            CommandContext<ServerCommandSource> context,
            com.mojang.brigadier.suggestion.SuggestionsBuilder builder,
            PermissionService permissions) {
        java.util.List<String> values = new java.util.ArrayList<>();
        values.add("clear");
        values.addAll(new FabricCommandSource(context.getSource(), permissions).itemNames());
        return CommandSource.suggestMatching(values, builder);
    }

    private static int executeEditOption(
            CommandContext<ServerCommandSource> context,
            TagService service,
            PermissionService permissions,
            DefaultMessageService messages,
            DefaultConfigurationService configuration,
            String property,
            String value) {
        return execute(context, service, permissions, messages, configuration, new String[]{
                "tag", "edit",
                StringArgumentType.getString(context, "tag"),
                property, value
        });
    }

    private static int executeEditNestedOption(
            CommandContext<ServerCommandSource> context,
            TagService service,
            PermissionService permissions,
            DefaultMessageService messages,
            DefaultConfigurationService configuration,
            String group,
            String property,
            String value) {
        return execute(context, service, permissions, messages, configuration, new String[]{
                "tag", "edit",
                StringArgumentType.getString(context, "tag"),
                group,
                StringArgumentType.getString(context, "item"),
                property, value
        });
    }

    private static int executeEditGradient(
            CommandContext<ServerCommandSource> context,
            TagService service,
            PermissionService permissions,
            DefaultMessageService messages,
            DefaultConfigurationService configuration) {
        return execute(context, service, permissions, messages, configuration, new String[]{
                "tag", "edit",
                StringArgumentType.getString(context, "tag"),
                "appearance", "gradient",
                StringArgumentType.getString(context, "startHex"),
                StringArgumentType.getString(context, "endHex")
        });
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<ServerCommandSource> createStyleNode(
            TagService service, PermissionService permissions, DefaultMessageService messages,
            DefaultConfigurationService configuration, String flow) {
        var node = CommandManager.literal("style");
        node.then(CommandManager.argument("style", StringArgumentType.word())
                .suggests((context, builder) -> CommandSource.suggestMatching(
                        List.of("normal", "bold", "italic", "bold_italic", "underlined",
                                "strikethrough", "obfuscated"),
                        builder))
                .then(createWizardColorNode(service, permissions, messages, configuration, flow)));
        return node;
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<ServerCommandSource> createWizardColorNode(
            TagService service, PermissionService permissions, DefaultMessageService messages,
            DefaultConfigurationService configuration, String flow) {
        var node = CommandManager.literal("color");
        node.then(CommandManager.argument("color", StringArgumentType.word())
                .suggests((context, builder) -> CommandSource.suggestMatching(
                        List.of("black", "dark_blue", "dark_green", "dark_aqua", "dark_red", "dark_purple",
                                "gold", "gray", "dark_gray", "blue", "green", "aqua", "red",
                                "light_purple", "yellow", "white", "random"),
                        builder))
                .then(createWizardEffectNode(service, permissions, messages, configuration, flow)));
        return node;
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<ServerCommandSource> createWizardEffectNode(
            TagService service, PermissionService permissions, DefaultMessageService messages,
            DefaultConfigurationService configuration, String flow) {
        var node = CommandManager.literal("effect");

        var normal = CommandManager.literal("normal");
        normal.then(CommandManager.argument("normalEffect", StringArgumentType.word())
                .suggests((context, builder) -> CommandSource.suggestMatching(
                        List.of("regular", "neon", "breath", "blink", "rgb"),
                        builder))
                .executes(context -> executeCreateWizard(
                        context, service, permissions, messages, configuration, flow,
                        "effect", StringArgumentType.getString(context, "normalEffect"))));

        var glitch = CommandManager.literal("glitch");
        glitch.then(CommandManager.argument("glitchMode", StringArgumentType.word())
                .suggests((context, builder) -> CommandSource.suggestMatching(
                        List.of("white", "colorful"),
                        builder))
                .executes(context -> executeCreateWizard(
                        context, service, permissions, messages, configuration, flow,
                        "glitch", StringArgumentType.getString(context, "glitchMode"))));

        node.then(normal);
        node.then(glitch);
        return node;
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<ServerCommandSource> createSpinNode(
            TagService service, PermissionService permissions, DefaultMessageService messages,
            DefaultConfigurationService configuration, boolean nameAndItem) {
        var node = CommandManager.literal("spin");
        var value = CommandManager.argument("spin", BoolArgumentType.bool());
        value.executes(context -> executeCreateItemWizard(
                context, service, permissions, messages, configuration, nameAndItem, false));
        var speed = CommandManager.literal("speed");
        speed.then(CommandManager.argument("itemSpeed", IntegerArgumentType.integer(1, 10))
                .suggests((context, builder) -> CommandSource.suggestMatching(
                        List.of("1","2","3","4","5","6","7","8","9","10"), builder))
                .executes(context -> executeCreateItemWizard(
                        context, service, permissions, messages, configuration, nameAndItem, true)));
        value.then(speed);
        node.then(value);
        return node;
    }

    private static int executeCreateItemWizard(
            CommandContext<ServerCommandSource> context,
            TagService service,
            PermissionService permissions,
            DefaultMessageService messages,
            DefaultConfigurationService configuration,
            boolean nameAndItem,
            boolean withSpeed) {
        boolean spin = BoolArgumentType.getBool(context, "spin");
        List<String> args = new java.util.ArrayList<>();
        args.add("tag");
        args.add("create");
        args.add(StringArgumentType.getString(context, "tag"));
        args.add("item");
        args.add(StringArgumentType.getString(context, "item"));
        args.add("item-mode");
        args.add(spin ? "rotate" : "static");
        if (withSpeed) {
            args.add("item-speed");
            args.add(Integer.toString(IntegerArgumentType.getInteger(context, "itemSpeed")));
        }
        if (nameAndItem) {
            args.add("name");
            args.add(StringArgumentType.getString(context, "displayName"));
        }
        return execute(context, service, permissions, messages, configuration, args.toArray(String[]::new));
    }

    private static int executeCreateWizard(
            CommandContext<ServerCommandSource> context,
            TagService service,
            PermissionService permissions,
            DefaultMessageService messages,
            DefaultConfigurationService configuration,
            String flow,
            String effectType,
            String effectValue) {
        List<String> args = new java.util.ArrayList<>();
        args.add("tag");
        args.add("create");
        args.add(StringArgumentType.getString(context, "tag"));

        if ("item".equals(flow)) {
            args.add("item");
            args.add(StringArgumentType.getString(context, "item"));
        } else if ("name+item".equals(flow)) {
            args.add("item");
            args.add(StringArgumentType.getString(context, "item"));
            args.add("name");
            args.add(StringArgumentType.getString(context, "displayName"));
        } else {
            args.add("name");
            args.add(StringArgumentType.getString(context, "displayName"));
        }

        args.add("style");
        args.add(StringArgumentType.getString(context, "style"));
        args.add("color");
        args.add(StringArgumentType.getString(context, "color"));
        args.add(effectType);
        args.add(effectValue);

        return execute(context, service, permissions, messages, configuration, args.toArray(String[]::new));
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<ServerCommandSource> createColorNode(
            TagService service, PermissionService permissions, DefaultMessageService messages,
            DefaultConfigurationService configuration) {
        var node = CommandManager.literal("color");
        node.then(CommandManager.argument("color", StringArgumentType.word())
                .suggests((context, builder) -> CommandSource.suggestMatching(
                        List.of("black", "dark_blue", "dark_green", "dark_aqua", "dark_red",
                                "dark_purple", "gold", "gray", "dark_gray", "blue", "green",
                                "aqua", "red", "light_purple", "yellow", "white", "random"),
                        builder))
                .executes(context -> executeCreateOption(context, service, permissions, messages, configuration,
                        "color", StringArgumentType.getString(context, "color"))));
        return node;
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<ServerCommandSource> createGradientNode(
            TagService service, PermissionService permissions, DefaultMessageService messages,
            DefaultConfigurationService configuration) {
        var node = CommandManager.literal("gradient");
        var start = CommandManager.argument("startHex", StringArgumentType.word())
                .suggests((context, builder) -> CommandSource.suggestMatching(
                        List.of("#FFFFFF", "#FF0000", "#FFD700"), builder));
        var end = CommandManager.argument("endHex", StringArgumentType.word())
                .suggests((context, builder) -> CommandSource.suggestMatching(
                        List.of("#000000", "#00FFFF", "#8A2BE2"), builder))
                .executes(context -> executeCreateGradient(context, service, permissions, messages, configuration));
        start.then(end);
        node.then(start);
        return node;
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<ServerCommandSource> createStyleNode(
            TagService service, PermissionService permissions, DefaultMessageService messages,
            DefaultConfigurationService configuration) {
        var node = CommandManager.literal("style");
        node.then(CommandManager.argument("style", StringArgumentType.word())
                .suggests((context, builder) -> CommandSource.suggestMatching(
                        List.of("plain", "bold", "italic", "underlined", "strikethrough", "obfuscated",
                                "bold_italic", "bold_underlined", "italic_underlined"),
                        builder))
                .executes(context -> executeCreateOption(context, service, permissions, messages, configuration,
                        "style", StringArgumentType.getString(context, "style"))));
        return node;
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<ServerCommandSource> createEffectNode(
            TagService service, PermissionService permissions, DefaultMessageService messages,
            DefaultConfigurationService configuration) {
        var node = CommandManager.literal("effect");
        node.then(CommandManager.argument("effect", StringArgumentType.word())
                .suggests((context, builder) -> CommandSource.suggestMatching(
                        List.of("none", "rainbow", "pulse", "wave"), builder))
                .executes(context -> executeCreateOption(context, service, permissions, messages, configuration,
                        "effect", StringArgumentType.getString(context, "effect"))));
        return node;
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<ServerCommandSource> createGlitchNode(
            TagService service, PermissionService permissions, DefaultMessageService messages,
            DefaultConfigurationService configuration) {
        var node = CommandManager.literal("glitch");
        node.then(CommandManager.argument("glitch", StringArgumentType.word())
                .suggests((context, builder) -> CommandSource.suggestMatching(
                        List.of("none", "white", "colorful"), builder))
                .executes(context -> executeCreateOption(context, service, permissions, messages, configuration,
                        "glitch", StringArgumentType.getString(context, "glitch"))));
        return node;
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<ServerCommandSource> createSimpleOptionNode(
            String property,
            List<String> values,
            TagService service,
            PermissionService permissions,
            DefaultMessageService messages,
            DefaultConfigurationService configuration) {
        var node = CommandManager.literal(property);
        node.then(CommandManager.argument(property, StringArgumentType.word())
                .suggests((context, builder) -> CommandSource.suggestMatching(values, builder))
                .executes(context -> executeCreateOption(context, service, permissions, messages, configuration,
                        property, StringArgumentType.getString(context, property))));
        return node;
    }

    private static java.util.concurrent.CompletableFuture<Suggestions> suggestCreateItems(
            CommandContext<ServerCommandSource> context,
            com.mojang.brigadier.suggestion.SuggestionsBuilder builder,
            PermissionService permissions) {
        FabricCommandSource source = new FabricCommandSource(context.getSource(), permissions);
        return CommandSource.suggestMatching(source.itemNames(), builder);
    }

    private static int executeCreateOption(
            CommandContext<ServerCommandSource> context,
            TagService service,
            PermissionService permissions,
            DefaultMessageService messages,
            DefaultConfigurationService configuration,
            String property,
            String value) {
        return execute(context, service, permissions, messages, configuration, new String[]{
                "tag", "create",
                StringArgumentType.getString(context, "tag"),
                property, value
        });
    }

    private static int executeCreateNestedOption(
            CommandContext<ServerCommandSource> context,
            TagService service,
            PermissionService permissions,
            DefaultMessageService messages,
            DefaultConfigurationService configuration,
            String group,
            String property,
            String value) {
        return execute(context, service, permissions, messages, configuration, new String[]{
                "tag", "create",
                StringArgumentType.getString(context, "tag"),
                group,
                StringArgumentType.getString(context, "item"),
                property, value
        });
    }

    private static int executeCreateGradient(
            CommandContext<ServerCommandSource> context,
            TagService service,
            PermissionService permissions,
            DefaultMessageService messages,
            DefaultConfigurationService configuration) {
        return execute(context, service, permissions, messages, configuration, new String[]{
                "tag", "create",
                StringArgumentType.getString(context, "tag"),
                "appearance", "gradient",
                StringArgumentType.getString(context, "startHex"),
                StringArgumentType.getString(context, "endHex")
        });
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

    private static int execute(
            CommandContext<ServerCommandSource> context,
            TagService service,
            PermissionService permissions,
            DefaultMessageService messages,
            DefaultConfigurationService configuration,
            String[] prefixArgs,
            String greedyTail
    ) {
        String[] tail = greedyTail == null || greedyTail.isBlank()
                ? new String[0]
                : greedyTail.trim().split("\\s+");
        String[] args = new String[prefixArgs.length + tail.length];
        System.arraycopy(prefixArgs, 0, args, 0, prefixArgs.length);
        System.arraycopy(tail, 0, args, prefixArgs.length, tail.length);
        return execute(context, service, permissions, messages, configuration, args);
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
