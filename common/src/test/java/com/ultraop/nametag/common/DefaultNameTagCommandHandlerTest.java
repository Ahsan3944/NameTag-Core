package com.ultraop.nametag.common;

import com.ultraop.nametag.api.CommandContext;
import com.ultraop.nametag.api.CommandSource;
import com.ultraop.nametag.api.ConfigurationReloadResult;
import com.ultraop.nametag.api.ConfigurationReloadService;
import com.ultraop.nametag.api.OnlinePlayer;
import com.ultraop.nametag.api.PlayerResolver;
import com.ultraop.nametag.core.model.NameTagConfiguration;
import com.ultraop.nametag.core.model.Tag;
import com.ultraop.nametag.core.model.TagColor;
import com.ultraop.nametag.core.model.TagEffect;
import com.ultraop.nametag.core.model.TagId;
import com.ultraop.nametag.core.model.TagItemSettings;
import com.ultraop.nametag.core.model.TagStyle;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DefaultNameTagCommandHandlerTest {
    @Test
    void createsTagThroughCommonCommandContract() {
        InMemoryTagRepository tags = new InMemoryTagRepository();
        DefaultTagService service = new DefaultTagService(
                tags,
                new InMemoryPlayerAssignmentRepository()
        );
        RecordingSource source = new RecordingSource();
        DefaultNameTagCommandHandler handler = new DefaultNameTagCommandHandler(
                service,
                new EmptyPlayerResolver(),
                new DefaultMessageService()
        );

        handler.execute(new CommandContext(source, new String[]{"create", "owner", "OWNER"}));

        assertTrue(service.find(new TagId("owner")).isPresent());
        assertEquals("Created NameTag: owner", source.lastMessage);
    }

    @Test
    void givesAndSetsActiveTagUsingResolvedPlayerUuid() {
        InMemoryTagRepository tags = new InMemoryTagRepository();
        InMemoryPlayerAssignmentRepository assignments = new InMemoryPlayerAssignmentRepository();
        DefaultTagService service = new DefaultTagService(tags, assignments);
        Tag tag = new Tag(
                new TagId("owner"),
                "OWNER",
                new TagColor.Preset("white"),
                TagStyle.plain(),
                TagEffect.none(),
                0,
                true,
                true,
                Map.of()
        );
        service.create(tag);

        UUID playerUuid = UUID.randomUUID();
        RecordingSource source = new RecordingSource();
        DefaultNameTagCommandHandler handler = new DefaultNameTagCommandHandler(
                service,
                new SinglePlayerResolver(new OnlinePlayer(playerUuid, "UltraOP")),
                new DefaultMessageService()
        );

        handler.execute(new CommandContext(source, new String[]{"give", "UltraOP", "owner"}));

        assertEquals("owner", service.activeTag(playerUuid).orElseThrow().id().value());
        assertEquals("Assigned owner to UltraOP", source.lastMessage);

        Tag secondTag = new Tag(
                new TagId("vip"),
                "VIP",
                new TagColor.Preset("white"),
                TagStyle.plain(),
                TagEffect.none(),
                0,
                true,
                true,
                Map.of()
        );
        service.create(secondTag);

        handler.execute(new CommandContext(source, new String[]{"give", "UltraOP", "vip"}));

        handler.execute(new CommandContext(source, new String[]{"set", "UltraOP", "vip"}));

        assertEquals("vip", service.activeTag(playerUuid).orElseThrow().id().value());
        assertEquals("Set vip as active for UltraOP", source.lastMessage);
    }

    @Test
    void setActivatesAndAssignsTagWhenItWasNotPreviouslyGiven() {
        InMemoryTagRepository tags = new InMemoryTagRepository();
        InMemoryPlayerAssignmentRepository assignments = new InMemoryPlayerAssignmentRepository();
        DefaultTagService service = new DefaultTagService(tags, assignments);
        service.create(new Tag(
                new TagId("vip"), "VIP", new TagColor.Preset("gold"),
                TagStyle.plain(), TagEffect.none(), 0, true, true, Map.of()
        ));

        UUID playerUuid = UUID.randomUUID();
        RecordingSource source = new RecordingSource();
        DefaultNameTagCommandHandler handler = new DefaultNameTagCommandHandler(
                service,
                new SinglePlayerResolver(new OnlinePlayer(playerUuid, "UltraOP")),
                new DefaultMessageService()
        );

        handler.execute(new CommandContext(source, new String[]{"set", "UltraOP", "vip"}));

        assertEquals("vip", service.activeTag(playerUuid).orElseThrow().id().value());
        assertEquals(List.of(new TagId("vip")),
                assignments.find(playerUuid).orElseThrow().assignedTagIds());
        assertEquals("Set vip as active for UltraOP", source.lastMessage);
    }

    @Test
    void giveAcceptsTemporaryDuration() {
        InMemoryTagRepository tags=new InMemoryTagRepository();
        InMemoryPlayerAssignmentRepository assignments=new InMemoryPlayerAssignmentRepository();
        DefaultTagService service=new DefaultTagService(tags,assignments);
        service.create(new Tag(new TagId("vip"),"VIP",new TagColor.Preset("white"),TagStyle.plain(),TagEffect.none(),10,true,true,Map.of()));
        UUID playerUuid=UUID.randomUUID(); RecordingSource source=new RecordingSource();
        DefaultNameTagCommandHandler handler=new DefaultNameTagCommandHandler(service,new SinglePlayerResolver(new OnlinePlayer(playerUuid,"UltraOP")),new DefaultMessageService());
        handler.execute(new CommandContext(source,new String[]{"give","UltraOP","vip","30m"}));
        assertTrue(assignments.find(playerUuid).orElseThrow().expirationEpochMillis().containsKey(new TagId("vip")));
        assertEquals("Assigned vip to UltraOP for 30m",source.lastMessage);
    }

    @Test
    void commandHandlerUsesConfiguredTagDefaults() {
        InMemoryTagRepository tags = new InMemoryTagRepository();
        DefaultTagService service = new DefaultTagService(
                tags,
                new InMemoryPlayerAssignmentRepository()
        );
        NameTagConfiguration configuration = new NameTagConfiguration(
                true,
                false,
                "<{tag}> {player}: {message}",
                25,
                false,
                false,
                70,
                120
        );
        DefaultNameTagCommandHandler handler = new DefaultNameTagCommandHandler(
                service,
                new EmptyPlayerResolver(),
                new DefaultMessageService(),
                () -> configuration
        );
        RecordingSource source = new RecordingSource();

        handler.execute(new CommandContext(source, new String[]{"create", "vip", "VIP"}));

        Tag created = service.find(new TagId("vip")).orElseThrow();
        assertEquals(25, created.priority());
        assertFalse(created.enabled());
        assertFalse(created.chatEnabled());
    }

    @Test
    void deniesCommandWithoutPermission() {
        RecordingSource source = new RecordingSource();
        source.allowed = false;
        DefaultNameTagCommandHandler handler = new DefaultNameTagCommandHandler(
                new DefaultTagService(
                        new InMemoryTagRepository(),
                        new InMemoryPlayerAssignmentRepository()
                ),
                new EmptyPlayerResolver(),
                new DefaultMessageService()
        );

        handler.execute(new CommandContext(source, new String[]{"create", "owner", "OWNER"}));

        assertEquals("You do not have permission to edit NameTags.", source.lastMessage);
    }

    @Test
    void groupedCommandsExposeFocusedSuggestionsAndExecute() {
        InMemoryTagRepository tags = new InMemoryTagRepository();
        DefaultTagService service = new DefaultTagService(tags, new InMemoryPlayerAssignmentRepository());
        service.create(new Tag(
                new TagId("owner"), "OWNER", new TagColor.Preset("white"),
                TagStyle.plain(), TagEffect.none(), 10, true, true, Map.of()
        ));
        RecordingSource source = new RecordingSource();
        DefaultNameTagCommandHandler handler = new DefaultNameTagCommandHandler(
                service,
                new EmptyPlayerResolver(),
                new DefaultMessageService()
        );

        assertEquals(List.of("tag", "player", "display", "advanced", "admin", "help", "info"),
                handler.suggest(new CommandContext(source, new String[]{""})));
        assertEquals(List.of("create", "edit", "list", "delete"),
                handler.suggest(new CommandContext(source, new String[]{"tag", ""})));
        assertEquals(List.of(
                        "name", "item", "color", "gradient", "style", "effect", "glitch",
                        "priority", "enabled", "chat", "item-mode", "item-speed"
                ),
                handler.suggest(new CommandContext(source, new String[]{"tag", "edit", "owner", ""})));
        assertEquals(List.of("0", "10", "25", "50", "100", "1000"),
                handler.suggest(new CommandContext(source, new String[]{"tag", "edit", "owner", "priority", ""})));

        handler.execute(new CommandContext(source, new String[]{"tag", "edit", "owner", "priority", "50"}));
        assertEquals(50, service.find(new TagId("owner")).orElseThrow().priority());
    }

    @Test
    void suggestionsArePlatformIndependent() {
        DefaultTagService service = new DefaultTagService(
                new InMemoryTagRepository(),
                new InMemoryPlayerAssignmentRepository()
        );
        service.create(new Tag(
                new TagId("owner"),
                "OWNER",
                new TagColor.Preset("white"),
                TagStyle.plain(),
                TagEffect.none(),
                0,
                true,
                true,
                Map.of()
        ));

        DefaultNameTagCommandHandler handler = new DefaultNameTagCommandHandler(
                service,
                new SinglePlayerResolver(new OnlinePlayer(UUID.randomUUID(), "UltraOP")),
                new DefaultMessageService()
        );

        assertEquals(List.of("owner"),
                handler.suggest(new CommandContext(new RecordingSource(),
                        new String[]{"delete", "ow"})));
        assertEquals(List.of("white", "colorful"),
                handler.suggest(new CommandContext(new RecordingSource(),
                        new String[]{"glitch", "owner", ""})));
    }

    private static final class RecordingSource implements CommandSource {
        private boolean allowed = true;
        private String lastMessage;
        private final List<String> messages = new ArrayList<>();
        private Collection<String> items = List.of();

        @Override
        public String name() {
            return "console";
        }

        @Override
        public Optional<UUID> playerUuid() {
            return Optional.empty();
        }

        @Override
        public boolean hasPermission(String permission) {
            return allowed;
        }

        @Override
        public Collection<String> itemNames() {
            return items;
        }

        @Override
        public void sendMessage(String message) {
            lastMessage = message;
            messages.add(message);
        }
    }

    private static final class EmptyPlayerResolver implements PlayerResolver {
        @Override
        public Optional<OnlinePlayer> findOnline(String name) {
            return Optional.empty();
        }

        @Override
        public Collection<OnlinePlayer> onlinePlayers() {
            return List.of();
        }
    }

    private record SinglePlayerResolver(OnlinePlayer player) implements PlayerResolver {
        @Override
        public Optional<OnlinePlayer> findOnline(String name) {
            return player.name().equalsIgnoreCase(name) ? Optional.of(player) : Optional.empty();
        }

        @Override
        public Collection<OnlinePlayer> onlinePlayers() {
            return List.of(player);
        }
    }
    @Test
    void reloadCommandUsesReloadServiceAndPermission() {
        RecordingSource source = new RecordingSource();
        DefaultTagService service = new DefaultTagService(
                new InMemoryTagRepository(),
                new InMemoryPlayerAssignmentRepository()
        );
        ConfigurationReloadService reloadService = () -> ConfigurationReloadResult.successResult();

        DefaultNameTagCommandHandler handler = new DefaultNameTagCommandHandler(
                service,
                new EmptyPlayerResolver(),
                new DefaultMessageService(),
                NameTagConfiguration::defaults,
                reloadService
        );

        handler.execute(new CommandContext(source, new String[]{"reload"}));

        assertEquals("Configuration reloaded successfully.", source.lastMessage);
    }

    @Test
    void reloadCommandDeniesWithoutReloadPermission() {
        RecordingSource source = new RecordingSource();
        source.allowed = false;
        DefaultNameTagCommandHandler handler = new DefaultNameTagCommandHandler(
                new DefaultTagService(
                        new InMemoryTagRepository(),
                        new InMemoryPlayerAssignmentRepository()
                ),
                new EmptyPlayerResolver(),
                new DefaultMessageService(),
                NameTagConfiguration::defaults,
                () -> ConfigurationReloadResult.successResult()
        );

        handler.execute(new CommandContext(source, new String[]{"reload"}));

        assertEquals("You do not have permission to edit NameTags.", source.lastMessage);
    }

    @Test
    void editCommandUpdatesNameColorAndStyleThroughCommonContract() {
        InMemoryTagRepository tags = new InMemoryTagRepository();
        DefaultTagService service = new DefaultTagService(tags, new InMemoryPlayerAssignmentRepository());
        service.create(new Tag(
                new TagId("owner"), "OWNER", new TagColor.Preset("white"),
                TagStyle.plain(), TagEffect.none(), 0, true, true, Map.of()
        ));

        RecordingSource source = new RecordingSource();
        DefaultNameTagCommandHandler handler = new DefaultNameTagCommandHandler(
                service, new EmptyPlayerResolver(), new DefaultMessageService()
        );

        handler.execute(new CommandContext(source, new String[]{"edit", "owner", "name", "SUPER", "OWNER"}));
        handler.execute(new CommandContext(source, new String[]{"edit", "owner", "color", "#12ABEF"}));
        handler.execute(new CommandContext(source, new String[]{"edit", "owner", "style", "bold_italic"}));

        Tag updated = service.find(new TagId("owner")).orElseThrow();
        assertEquals("SUPER OWNER", updated.displayName());
        assertEquals(new TagColor.Rgb(0x12, 0xAB, 0xEF), updated.color());
        assertEquals(new TagStyle(true, true, false, false, false), updated.style());
    }

    @Test
    void editCommandUpdatesPriorityWithoutChangingOtherFields() {
        DefaultTagService service = new DefaultTagService(
                new InMemoryTagRepository(), new InMemoryPlayerAssignmentRepository()
        );
        service.create(new Tag(
                new TagId("vip"), "VIP", new TagColor.Preset("gold"),
                new TagStyle(true, false, false, false, false), TagEffect.none(),
                5, true, true, Map.of("auto-permission", "group.vip")
        ));

        RecordingSource source = new RecordingSource();
        DefaultNameTagCommandHandler handler = new DefaultNameTagCommandHandler(
                service, new EmptyPlayerResolver(), new DefaultMessageService()
        );

        handler.execute(new CommandContext(source, new String[]{"edit", "vip", "priority", "50"}));

        Tag updated = service.find(new TagId("vip")).orElseThrow();
        assertEquals(50, updated.priority());
        assertEquals("VIP", updated.displayName());
        assertEquals(new TagColor.Preset("gold"), updated.color());
        assertEquals(new TagStyle(true, false, false, false, false), updated.style());
        assertEquals("group.vip", updated.metadata().get("auto-permission"));
    }

    @Test
    void editCommandSupportsGradientAndVisibilityFlags() {
        DefaultTagService service = new DefaultTagService(
                new InMemoryTagRepository(), new InMemoryPlayerAssignmentRepository()
        );
        service.create(new Tag(
                new TagId("vip"), "VIP", new TagColor.Preset("white"),
                TagStyle.plain(), TagEffect.none(), 0, true, true, Map.of()
        ));

        RecordingSource source = new RecordingSource();
        DefaultNameTagCommandHandler handler = new DefaultNameTagCommandHandler(
                service, new EmptyPlayerResolver(), new DefaultMessageService()
        );

        handler.execute(new CommandContext(source, new String[]{"edit", "vip", "gradient", "#FF0000", "#0000FF"}));
        handler.execute(new CommandContext(source, new String[]{"edit", "vip", "enabled", "false"}));
        handler.execute(new CommandContext(source, new String[]{"edit", "vip", "chat", "false"}));

        Tag updated = service.find(new TagId("vip")).orElseThrow();
        assertEquals(new TagColor.Gradient(
                new TagColor.Rgb(255, 0, 0), new TagColor.Rgb(0, 0, 255)
        ), updated.color());
        assertFalse(updated.enabled());
        assertFalse(updated.chatEnabled());
    }

    @Test
    void listCommandReportsCountAndTagDetails() {
        DefaultTagService service = new DefaultTagService(
                new InMemoryTagRepository(), new InMemoryPlayerAssignmentRepository()
        );
        service.create(new Tag(
                new TagId("vip"), "VIP", new TagColor.Preset("gold"),
                new TagStyle(true, false, false, false, false), TagEffect.none(),
                10, true, true, Map.of()
        ));
        service.create(new Tag(
                new TagId("owner"), "OWNER", new TagColor.Preset("red"),
                TagStyle.plain(), TagEffect.none(), 20, true, true, Map.of()
        ));

        RecordingSource source = new RecordingSource();
        DefaultNameTagCommandHandler handler = new DefaultNameTagCommandHandler(
                service, new EmptyPlayerResolver(), new DefaultMessageService()
        );

        handler.execute(new CommandContext(source, new String[]{"list"}));

        assertTrue(source.messages.contains("NameTags (2 total):"));
        assertTrue(source.messages.stream().anyMatch(value ->
                value.contains("owner -> OWNER") && value.contains("color=red") && value.contains("style=plain")));
        assertTrue(source.messages.stream().anyMatch(value ->
                value.contains("vip -> VIP") && value.contains("color=gold") && value.contains("style=bold")));
    }

    @Test
    void editSuggestionsExposeCommandPropertiesAndValues() {
        DefaultTagService service = new DefaultTagService(
                new InMemoryTagRepository(), new InMemoryPlayerAssignmentRepository()
        );
        service.create(new Tag(
                new TagId("owner"), "OWNER", new TagColor.Preset("white"),
                TagStyle.plain(), TagEffect.none(), 0, true, true, Map.of()
        ));

        DefaultNameTagCommandHandler handler = new DefaultNameTagCommandHandler(
                service, new EmptyPlayerResolver(), new DefaultMessageService()
        );
        RecordingSource source = new RecordingSource();

        assertEquals(List.of("owner"),
                handler.suggest(new CommandContext(source, new String[]{"edit", "ow"})));
        assertEquals(List.of(
                        "name", "item", "color", "gradient", "style", "effect", "glitch",
                        "priority", "enabled", "chat", "item-mode", "item-speed"
                ),
                handler.suggest(new CommandContext(source, new String[]{"edit", "owner", ""})));
        List<String> styleSuggestions = handler.suggest(
                new CommandContext(source, new String[]{"edit", "owner", "style", "bold"}));
        assertTrue(styleSuggestions.contains("bold"));
        assertTrue(styleSuggestions.contains("bold_italic"));
        assertTrue(styleSuggestions.contains("bold_underlined"));
    }



    @Test
    void effectAndRoleSuggestionsExposeTagIdsBeforeTheirValues() {
        DefaultTagService service = new DefaultTagService(
                new InMemoryTagRepository(),
                new InMemoryPlayerAssignmentRepository()
        );
        service.create(new Tag(
                new TagId("owner"), "OWNER", new TagColor.Preset("white"),
                TagStyle.plain(), TagEffect.none(), 100, true, true, Map.of()
        ));
        service.create(new Tag(
                new TagId("vip"), "VIP", new TagColor.Preset("white"),
                TagStyle.plain(), TagEffect.none(), 50, true, true, Map.of()
        ));

        DefaultNameTagCommandHandler handler = new DefaultNameTagCommandHandler(
                service, new EmptyPlayerResolver(), new DefaultMessageService()
        );
        RecordingSource source = new RecordingSource();

        assertEquals(List.of("owner", "vip"),
                handler.suggest(new CommandContext(source, new String[]{"effect", ""})));
        assertEquals(List.of("vip"),
                handler.suggest(new CommandContext(source, new String[]{"effect", "v"})));
        assertEquals(List.of("owner", "vip"),
                handler.suggest(new CommandContext(source, new String[]{"role", ""})));
        assertEquals(List.of("vip"),
                handler.suggest(new CommandContext(source, new String[]{"role", "v"})));
    }

    @Test
    void helpAndInfoCommandsExposeCategoryAndVersionInformation() {
        DefaultTagService service = new DefaultTagService(
                new InMemoryTagRepository(), new InMemoryPlayerAssignmentRepository()
        );
        RecordingSource source = new RecordingSource();
        DefaultNameTagCommandHandler handler = new DefaultNameTagCommandHandler(
                service, new EmptyPlayerResolver(), new DefaultMessageService()
        );

        assertEquals(List.of("tag", "player", "display", "advanced", "admin"),
                handler.suggest(new CommandContext(source, new String[]{"help", ""})));

        handler.execute(new CommandContext(source, new String[]{"help", "display"}));
        assertTrue(source.messages.stream().anyMatch(value -> value.contains("/nametag display item <tag> set <item>")));

        handler.execute(new CommandContext(source, new String[]{"info"}));
        assertTrue(source.messages.stream().anyMatch(value -> value.contains("Version:")));
        assertTrue(source.messages.stream().anyMatch(value -> value.contains("Created by:") && value.contains("UltraOP")));
    }

    @Test
    void createSupportsNameAndItemOptionsAndItemAutocomplete() {
        DefaultTagService service = new DefaultTagService(
                new InMemoryTagRepository(), new InMemoryPlayerAssignmentRepository()
        );
        RecordingSource source = new RecordingSource();
        source.items = List.of("minecraft:diamond", "minecraft:emerald", "minecraft:iron_block");
        DefaultNameTagCommandHandler handler = new DefaultNameTagCommandHandler(
                service, new EmptyPlayerResolver(), new DefaultMessageService()
        );

        assertEquals(List.of(
                        "name", "item", "color", "gradient", "style", "effect", "glitch",
                        "priority", "enabled", "chat", "item-mode", "item-speed"
                ),
                handler.suggest(new CommandContext(source, new String[]{"create", ""})));
        assertEquals(List.of("minecraft:diamond", "minecraft:emerald", "minecraft:iron_block"),
                handler.suggest(new CommandContext(source, new String[]{"create", "vip", "item", ""})));

        handler.execute(new CommandContext(source, new String[]{
                "create", "vip", "name", "Noob", "item", "minecraft:diamond"
        }));

        Tag tag = service.find(new TagId("vip")).orElseThrow();
        assertEquals("Noob", tag.displayName());
        assertEquals("minecraft:diamond", tag.metadata().get(TagItemSettings.ITEM_KEY));
        assertEquals("static", tag.metadata().get(TagItemSettings.MODE_KEY));
        assertEquals("5", tag.metadata().get(TagItemSettings.SPEED_KEY));

        handler.execute(new CommandContext(source, new String[]{
                "create", "icon", "item", "minecraft:emerald"
        }));
        Tag iconOnly = service.find(new TagId("icon")).orElseThrow();
        assertEquals("", iconOnly.displayName());
        assertEquals("minecraft:emerald", iconOnly.metadata().get(TagItemSettings.ITEM_KEY));
    }

    @Test
    void groupedCreateSuggestionsExposeNestedOptionsAndItemValues() {
        DefaultTagService service = new DefaultTagService(
                new InMemoryTagRepository(), new InMemoryPlayerAssignmentRepository()
        );
        RecordingSource source = new RecordingSource();
        source.items = List.of("minecraft:diamond", "minecraft:emerald");
        DefaultNameTagCommandHandler handler = new DefaultNameTagCommandHandler(
                service, new EmptyPlayerResolver(), new DefaultMessageService()
        );

        assertEquals(List.of("name", "item", "appearance", "behavior"),
                handler.suggest(new CommandContext(source, new String[]{"tag", "create", "vip", ""})));
        assertEquals(List.of("color", "gradient", "style", "effect", "glitch"),
                handler.suggest(new CommandContext(source, new String[]{"tag", "create", "vip", "appearance", ""})));
        assertEquals(List.of("black", "dark_blue", "dark_green", "dark_aqua", "dark_red", "dark_purple",
                        "gold", "gray", "dark_gray", "blue", "green", "aqua", "red", "light_purple",
                        "yellow", "white", "random"),
                handler.suggest(new CommandContext(source, new String[]{"tag", "create", "vip", "appearance", "color", ""})));
        assertEquals(List.of("minecraft:diamond", "minecraft:emerald"),
                handler.suggest(new CommandContext(source, new String[]{"tag", "create", "vip", "item", ""})));
        assertEquals(List.of("mode", "speed"),
                handler.suggest(new CommandContext(source, new String[]{"tag", "create", "vip", "item", "minecraft:diamond", ""})));
        assertEquals(List.of("static", "rotate"),
                handler.suggest(new CommandContext(source, new String[]{"tag", "create", "vip", "item", "minecraft:diamond", "mode", ""})));

        assertEquals(List.of("name", "item", "appearance", "behavior"),
                handler.suggest(new CommandContext(source, new String[]{"tag", "edit", "vip", ""})));
        assertEquals(List.of("color", "gradient", "style", "effect", "glitch"),
                handler.suggest(new CommandContext(source, new String[]{"tag", "edit", "vip", "appearance", ""})));
        assertEquals(List.of("mode", "speed"),
                handler.suggest(new CommandContext(source, new String[]{"tag", "edit", "vip", "item", "minecraft:diamond", ""})));

        handler.execute(new CommandContext(source, new String[]{
                "tag", "create", "vip", "appearance", "color", "gold",
                "behavior", "priority", "100",
                "item", "minecraft:diamond", "mode", "rotate", "speed", "8"
        }));
        Tag created = service.find(new TagId("vip")).orElseThrow();
        assertEquals(new TagColor.Preset("gold"), created.color());
        assertEquals(100, created.priority());
        assertEquals("minecraft:diamond", created.metadata().get(TagItemSettings.ITEM_KEY));
        assertEquals("rotate", created.metadata().get(TagItemSettings.MODE_KEY));
        assertEquals("8", created.metadata().get(TagItemSettings.SPEED_KEY));

        handler.execute(new CommandContext(source, new String[]{
                "tag", "edit", "vip", "appearance", "style", "bold"
        }));
        assertEquals(new TagStyle(true, false, false, false, false),
                service.find(new TagId("vip")).orElseThrow().style());
    }

    @Test
    void createSupportsFullPresentationConfigurationInOneCommand() {
        DefaultTagService service = new DefaultTagService(
                new InMemoryTagRepository(), new InMemoryPlayerAssignmentRepository()
        );
        RecordingSource source = new RecordingSource();
        source.items = List.of("minecraft:diamond");
        DefaultNameTagCommandHandler handler = new DefaultNameTagCommandHandler(
                service, new EmptyPlayerResolver(), new DefaultMessageService()
        );

        handler.execute(new CommandContext(source, new String[]{
                "create", "vip", "name", "VIP", "item", "minecraft:diamond",
                "color", "gold", "style", "bold+italic+underlined",
                "effect", "rainbow", "priority", "100", "enabled", "true", "chat", "true",
                "item-mode", "rotate", "item-speed", "8"
        }));

        Tag tag = service.find(new TagId("vip")).orElseThrow();
        assertEquals("VIP", tag.displayName());
        assertEquals(new TagColor.Preset("gold"), tag.color());
        assertEquals(new TagStyle(true, true, true, false, false), tag.style());
        assertEquals(TagEffect.RAINBOW_ID, tag.effect().id());
        assertEquals(100, tag.priority());
        assertTrue(tag.enabled());
        assertTrue(tag.chatEnabled());
        assertEquals("minecraft:diamond", tag.metadata().get(TagItemSettings.ITEM_KEY));
        assertEquals("rotate", tag.metadata().get(TagItemSettings.MODE_KEY));
        assertEquals("8", tag.metadata().get(TagItemSettings.SPEED_KEY));
    }

    @Test
    void itemNametagStoresItemModeAndSpeed() {
        DefaultTagService service = new DefaultTagService(
                new InMemoryTagRepository(), new InMemoryPlayerAssignmentRepository()
        );
        service.create(new Tag(
                new TagId("dirt"), "DIRT", new TagColor.Preset("white"),
                TagStyle.plain(), TagEffect.none(), 0, true, true, Map.of()
        ));
        RecordingSource source = new RecordingSource();
        source.items = List.of("minecraft:dirt", "minecraft:iron_block", "minecraft:gold_ingot");
        DefaultNameTagCommandHandler handler = new DefaultNameTagCommandHandler(
                service, new EmptyPlayerResolver(), new DefaultMessageService()
        );

        assertEquals(List.of("minecraft:dirt", "minecraft:gold_ingot", "minecraft:iron_block"),
                handler.suggest(new CommandContext(source, new String[]{"display", "item", "dirt", "set", ""})));

        handler.execute(new CommandContext(source, new String[]{"display", "item", "dirt", "set", "minecraft:dirt"}));
        handler.execute(new CommandContext(source, new String[]{"display", "item", "dirt", "mode", "rotate"}));
        handler.execute(new CommandContext(source, new String[]{"display", "item", "dirt", "speed", "10"}));

        Tag tag = service.find(new TagId("dirt")).orElseThrow();
        assertEquals("minecraft:dirt", tag.metadata().get(TagItemSettings.ITEM_KEY));
        assertEquals("rotate", tag.metadata().get(TagItemSettings.MODE_KEY));
        assertEquals("10", tag.metadata().get(TagItemSettings.SPEED_KEY));
    }

}
