package com.ultraop.nametag.fabric.client;

import com.ultraop.nametag.fabric.v1_21_11.network.NameTagItemPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.client.util.math.MatrixStack;

import java.util.HashMap;
import java.util.Map;

public final class NameTagFabricClient {
    private static final Map<Integer, NameTagItemPayload> ITEMS = new HashMap<>();

    private NameTagFabricClient() {}

    public static void register() {
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ITEMS.clear());
        ClientPlayNetworking.registerGlobalReceiver(
                NameTagItemPayload.ID,
                (payload, context) -> context.client().execute(() -> {
                    if (payload.active()) ITEMS.put(payload.entityId(), payload);
                    else ITEMS.remove(payload.entityId());
                })
        );
    }

    public static NameTagItemPayload itemFor(PlayerEntityRenderState state) {
        return ITEMS.get(state.id);
    }

    public static void renderItem(
            PlayerEntityRenderState state,
            MatrixStack matrices,
            OrderedRenderCommandQueue queue,
            int light,
            float textWidth
    ) {
        NameTagItemPayload payload = itemFor(state);
        if (payload == null) return;

        long now = System.nanoTime();
        if ((payload.effects() & NameTagItemPayload.EFFECT_BLINK) != 0
                && ((now / 350_000_000L) & 1L) == 1L) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return;

        Identifier id = Identifier.tryParse(payload.itemId());
        if (id == null || !Registries.ITEM.containsId(id)) return;

        ItemStack stack = new ItemStack(Registries.ITEM.get(id));
        ItemRenderState itemState = new ItemRenderState();
        client.getItemModelManager().update(
                itemState, stack, ItemDisplayContext.GUI, client.world, null, 0
        );

        float wave = 0.0f;
        if ((payload.effects() & NameTagItemPayload.EFFECT_WAVE) != 0) {
            wave = (float) Math.sin(now / 250_000_000.0) * 1.5f;
        }

        matrices.push();
        matrices.translate(-textWidth / 2.0f - 10.0f, wave - 7.0f, 0.0f);

        if (payload.rotate()) {
            float degrees = (now / 1_000_000L) * payload.speed() * 0.18f % 360.0f;
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(degrees));
        }

        matrices.scale(0.72f, 0.72f, 0.72f);
        itemState.render(matrices, queue, light, 0, 0);
        matrices.pop();
    }
}
