package com.ultraop.nametag.fabric;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.ultraop.nametag.api.TagService;
import com.ultraop.nametag.common.DefaultTagService;
import com.ultraop.nametag.common.InMemoryPlayerAssignmentRepository;
import com.ultraop.nametag.common.InMemoryTagRepository;
import com.ultraop.nametag.core.model.GlitchMode;
import com.ultraop.nametag.core.model.Tag;
import com.ultraop.nametag.core.model.TagId;
import com.ultraop.nametag.fabric.v1_21_11.Fabric2111Adapter;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.util.List;

public final class NameTagFabric {
    private NameTagFabric() {}

    public static void bootstrap() {
        TagService service = new DefaultTagService(
                new InMemoryTagRepository(),
                new InMemoryPlayerAssignmentRepository()
        );
        new Fabric2111Adapter(service);
        registerCommands(service);
    }

    private static void registerCommands(TagService service) {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(
                        CommandManager.literal("nametag")
                                .requires(source -> source.hasPermissionLevel(2))
                                .then(CommandManager.literal("glitch")
                                        .then(CommandManager.argument("tag", StringArgumentType.word())
                                                .suggests((context, builder) ->
                                                        net.minecraft.command.CommandSource.suggestMatching(
                                                                service.list().stream()
                                                                        .map(Tag::id)
                                                                        .map(TagId::value)
                                                                        .sorted()
                                                                        .toList(),
                                                                builder
                                                        )
                                                )
                                                .then(CommandManager.argument("mode", StringArgumentType.word())
                                                        .suggests((context, builder) ->
                                                                net.minecraft.command.CommandSource.suggestMatching(
                                                                        List.of("white", "colorful"),
                                                                        builder
                                                                )
                                                        )
                                                        .executes(context -> {
                                                            ServerCommandSource source = context.getSource();
                                                            String tagName = StringArgumentType.getString(context, "tag");
                                                            String modeName = StringArgumentType.getString(context, "mode");

                                                            try {
                                                                TagId tagId = new TagId(tagName.toLowerCase());
                                                                GlitchMode mode = GlitchMode.from(modeName);
                                                                Tag updated = service.setGlitch(tagId, mode);
                                                                source.sendFeedback(
                                                                        () -> Text.literal(
                                                                                "Glitch effect set to " +
                                                                                        mode.name().toLowerCase() +
                                                                                        " for tag " +
                                                                                        updated.id().value()
                                                                        ),
                                                                        true
                                                                );
                                                                return 1;
                                                            } catch (IllegalArgumentException exception) {
                                                                source.sendError(Text.literal(exception.getMessage()));
                                                                return 0;
                                                            }
                                                        })
                                                )
                                        )
                                )
                )
        );
    }
}
