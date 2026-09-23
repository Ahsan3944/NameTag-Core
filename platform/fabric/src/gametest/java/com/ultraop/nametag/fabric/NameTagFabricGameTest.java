package com.ultraop.nametag.fabric;

import com.mojang.authlib.GameProfile;
import com.ultraop.nametag.common.DefaultTagService;
import com.ultraop.nametag.common.InMemoryPlayerAssignmentRepository;
import com.ultraop.nametag.common.InMemoryTagRepository;
import com.ultraop.nametag.core.model.Tag;
import com.ultraop.nametag.core.model.TagColor;
import com.ultraop.nametag.core.model.TagEffect;
import com.ultraop.nametag.core.model.TagId;
import com.ultraop.nametag.core.model.TagStyle;
import com.ultraop.nametag.fabric.v1_21_11.Fabric2111NameplateRenderer;
import com.ultraop.nametag.fabric.v1_21_11.Fabric2111PlayerLifecycleListener;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.test.TestContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;

public final class NameTagFabricGameTest implements CustomTestMethodInvoker {
    @GameTest
    public void bootstrapRegistersNameTagCommands(TestContext context) {
        boolean registered = context.getWorld()
                .getServer()
                .getCommandManager()
                .getDispatcher()
                .getRoot()
                .getChild("nametag") != null;

        if (!registered) {
            context.throwGameTestException("NameTag command root was not registered");
            return;
        }

        context.complete();
    }

    @GameTest
    public void namespacedItemCreateCommandParsesColonIds(TestContext context) {
        var server = context.getWorld().getServer();
        int result = server.getCommandManager().getDispatcher().execute(
                "nametag tag create parserfix item minecraft:netherite_ingot spin false",
                server.getCommandSource()
        );

        if (result <= 0) {
            context.throwGameTestException("Namespaced item ID command did not execute");
            return;
        }

        context.complete();
    }

    @GameTest
    public void playerLifecycleRestoresAndClearsRuntimeNameplate(TestContext context) {
        ServerWorld world = context.getWorld();
        var server = world.getServer();

        DefaultTagService service = new DefaultTagService(
                new InMemoryTagRepository(),
                new InMemoryPlayerAssignmentRepository()
        );

        Tag tag = new Tag(
                new TagId("owner"),
                "OWNER",
                new TagColor.Rgb(255, 170, 0),
                TagStyle.plain(),
                TagEffect.none(),
                0,
                true,
                true,
                Map.of()
        );
        service.create(tag);

        UUID playerUuid = UUID.randomUUID();
        service.assign(playerUuid, tag.id());

        Fabric2111NameplateRenderer renderer = new Fabric2111NameplateRenderer(service);
        Fabric2111PlayerLifecycleListener lifecycle = new Fabric2111PlayerLifecycleListener(renderer);
        lifecycle.register();

        ServerPlayerEntity player = FakePlayer.get(
                world,
                new GameProfile(playerUuid, "UltraOP")
        );

        net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents.JOIN.invoker().onJoin(player);

        if (server.getScoreboard().getScoreHolderTeam(player.getName().getString()) == null) {
            renderer.stop(server);
            context.throwGameTestException("JOIN lifecycle did not restore the runtime nameplate team");
            return;
        }

        net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents.LEAVE.invoker().onLeave(player);

        if (server.getScoreboard().getScoreHolderTeam(player.getName().getString()) != null) {
            renderer.stop(server);
            context.throwGameTestException("LEAVE lifecycle did not clear the runtime nameplate team");
            return;
        }

        if (service.activeTag(playerUuid).isEmpty()) {
            renderer.stop(server);
            context.throwGameTestException("LEAVE lifecycle modified the persistent tag assignment");
            return;
        }

        renderer.stop(server);
        context.complete();
    }

    @GameTest
    public void nameplatePrefixRecoversIfScoreboardStateIsChanged(TestContext context) {
        ServerWorld world = context.getWorld();
        var server = world.getServer();
        DefaultTagService service = new DefaultTagService(
                new InMemoryTagRepository(),
                new InMemoryPlayerAssignmentRepository()
        );
        Tag tag = new Tag(
                new TagId("owner"),
                "OWNER",
                new TagColor.Preset("gold"),
                TagStyle.plain(),
                TagEffect.none(),
                0,
                true,
                true,
                Map.of()
        );
        service.create(tag);

        UUID playerUuid = UUID.randomUUID();
        service.assign(playerUuid, tag.id());

        Fabric2111NameplateRenderer renderer = new Fabric2111NameplateRenderer(service);
        ServerPlayerEntity player = FakePlayer.get(
                world,
                new GameProfile(playerUuid, "UltraOP")
        );

        renderer.refreshPlayer(player);
        var team = server.getScoreboard().getScoreHolderTeam(player.getName().getString());
        if (team == null) {
            renderer.stop(server);
            context.throwGameTestException("Nameplate team was not created");
            return;
        }

        team.setPrefix(net.minecraft.text.Text.empty());
        renderer.refreshPlayer(player);

        if (!team.getPrefix().getString().contains("OWNER")) {
            renderer.stop(server);
            context.throwGameTestException("Nameplate prefix was not restored after scoreboard state changed");
            return;
        }

        renderer.stop(server);
        context.complete();
    }

    @GameTest
    public void foreignScoreboardTeamIsNotHijacked(TestContext context) {
        ServerWorld world = context.getWorld();
        var server = world.getServer();
        var scoreboard = server.getScoreboard();

        String foreignTeamName = "nametag_core_owner";
        var foreignTeam = scoreboard.getTeam(foreignTeamName);
        if (foreignTeam != null) {
            scoreboard.removeTeam(foreignTeam);
        }
        foreignTeam = scoreboard.addTeam(foreignTeamName);
        foreignTeam.setPrefix(net.minecraft.text.Text.literal("FOREIGN "));
        scoreboard.addScoreHolderToTeam("ForeignPlayer", foreignTeam);

        DefaultTagService service = new DefaultTagService(
                new InMemoryTagRepository(),
                new InMemoryPlayerAssignmentRepository()
        );
        Tag tag = new Tag(
                new TagId("owner"),
                "OWNER",
                new TagColor.Rgb(255, 170, 0),
                TagStyle.plain(),
                TagEffect.none(),
                0,
                true,
                true,
                Map.of()
        );
        service.create(tag);
        UUID playerUuid = UUID.randomUUID();
        service.assign(playerUuid, tag.id());

        Fabric2111NameplateRenderer renderer = new Fabric2111NameplateRenderer(service);
        ServerPlayerEntity player = FakePlayer.get(world, new GameProfile(playerUuid, "UltraOP"));
        renderer.refreshPlayer(player);

        if (scoreboard.getTeam(foreignTeamName) == null
                || !"FOREIGN ".equals(scoreboard.getTeam(foreignTeamName).getPrefix().getString())) {
            renderer.stop(server);
            context.throwGameTestException("NameTag renderer hijacked the pre-existing scoreboard team");
            return;
        }

        if (scoreboard.getScoreHolderTeam(player.getName().getString()) == scoreboard.getTeam(foreignTeamName)) {
            renderer.stop(server);
            context.throwGameTestException("Player was assigned to the foreign scoreboard team");
            return;
        }

        renderer.stop(server);
        scoreboard.removeTeam(foreignTeam);
        context.complete();
    }

    @Override
    public void invokeTestMethod(TestContext context, Method method) throws ReflectiveOperationException {
        method.invoke(this, context);
    }
}
