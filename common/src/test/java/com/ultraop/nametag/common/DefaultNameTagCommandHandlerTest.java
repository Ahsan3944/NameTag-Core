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
import com.ultraop.nametag.core.model.TagStyle;
import org.junit.jupiter.api.Test;

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
        public void sendMessage(String message) {
            lastMessage = message;
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


}
