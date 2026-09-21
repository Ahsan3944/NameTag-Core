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
import net.minecraft.test.GameTestHelper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;

public final class NameTagFabricGameTest implements CustomTestMethodInvoker {
    @GameTest
    public void bootstrapRegistersNameTagCommands(GameTestHelper helper) {
        boolean registered = helper.getWorld()
                .getServer()
                .getCommands()
                .getDispatcher()
                .getRoot()
                .getChild("nametag") != null;

        if (!registered) {
            helper.fail("NameTag command root was not registered");
            return;
        }

        helper.succeed();
    }

    @GameTest
    public void playerLifecycleRestoresAndClearsRuntimeNameplate(GameTestHelper helper) {
        ServerWorld world = helper.getWorld();
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
            helper.fail("JOIN lifecycle did not restore the runtime nameplate team");
            return;
        }

        net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents.LEAVE.invoker().onLeave(player);

        if (server.getScoreboard().getScoreHolderTeam(player.getName().getString()) != null) {
            renderer.stop(server);
            helper.fail("LEAVE lifecycle did not clear the runtime nameplate team");
            return;
        }

        if (service.activeTag(playerUuid).isEmpty()) {
            renderer.stop(server);
            helper.fail("LEAVE lifecycle modified the persistent tag assignment");
            return;
        }

        renderer.stop(server);
        helper.succeed();
    }

    @Override
    public void invokeTestMethod(GameTestHelper helper, Method method) throws ReflectiveOperationException {
        method.invoke(this, helper);
    }
}
