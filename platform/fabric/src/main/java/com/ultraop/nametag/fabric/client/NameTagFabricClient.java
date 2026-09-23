package com.ultraop.nametag.fabric.client;

import com.ultraop.nametag.fabric.v1_21_11.network.NameTagItemPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.scoreboard.Team;
import net.minecraft.entity.Entity;
import net.minecraft.client.texture.Sprite;

import java.util.HashMap;
import java.util.Map;

public final class NameTagFabricClient {
    private static final Map<Integer, NameTagItemPayload> ITEMS = new HashMap<>();

    private static final float ICON_SIZE = 9.0f;
    private static final float ICON_GAP = 2.0f;

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

    /**
     * Returns the width of the actual vanilla team prefix rendered before the
     * player's name. This keeps the icon aligned with the NameTag text instead
     * of incorrectly anchoring it from the player-name width.
     */
    public static float tagPrefixWidth(PlayerEntityRenderState state) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return 0.0f;

        Entity entity = client.world.getEntityById(state.id);
        if (entity == null) return 0.0f;

        Team team = entity.getScoreboardTeam();
        if (team == null) return 0.0f;

        Text prefix = team.getPrefix();
        return prefix == null ? 0.0f : client.textRenderer.getWidth(prefix);
    }

    /**
     * Renders the item's atlas sprite as a single camera-facing 2D quad.
     * No ItemDisplay, model transform, rotation, interpolation or world-space
     * item entity is involved.
     */
    public static void renderItem(
            PlayerEntityRenderState state,
            MatrixStack matrices,
            OrderedRenderCommandQueue queue,
            int light,
            float textWidth,
            float prefixWidth
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

        // Resolve only the vanilla item texture/sprite. The resulting render
        // state is used solely to locate its particle/icon sprite; it is never
        // rendered as an item model.
        ItemStack stack = new ItemStack(Registries.ITEM.get(id));
        ItemRenderState itemState = new ItemRenderState();
        client.getItemModelManager().update(
                itemState, stack, ItemDisplayContext.GUI, client.world, null, 0
        );

        Sprite sprite = itemState.getParticleSprite(Random.create(0L));
        if (sprite == null) return;

        // The icon occupies the same horizontal "tag" area as the team prefix.
        // For text+icon: [ICON] [TAG] Player
        // For icon-only: [ICON] Player
        float tagStart = prefixWidth > 0.0f
                ? -textWidth / 2.0f - prefixWidth
                : -textWidth / 2.0f;
        float x0 = tagStart - ICON_SIZE - ICON_GAP;
        float x1 = x0 + ICON_SIZE;
        float y0 = -ICON_SIZE + 1.0f;
        float y1 = y0 + ICON_SIZE;

        RenderLayer layer = RenderLayers.entityTranslucentEmissive(sprite.getAtlasId());

        matrices.push();
        queue.submitCustom(matrices, layer, (entry, consumer) ->
                drawSprite(entry, consumer, sprite, x0, y0, x1, y1, light)
        );
        matrices.pop();
    }

    private static void drawSprite(
            MatrixStack.Entry entry,
            VertexConsumer consumer,
            Sprite sprite,
            float x0,
            float y0,
            float x1,
            float y1,
            int light
    ) {
        float minU = sprite.getMinU();
        float maxU = sprite.getMaxU();
        float minV = sprite.getMinV();
        float maxV = sprite.getMaxV();

        int color = 0xFFFFFFFF;
        int overlay = 0;

        consumer.vertex(entry, x0, y1, 0.0f, color, minU, maxV, overlay, light, 0.0f, 0.0f, 1.0f);
        consumer.vertex(entry, x1, y1, 0.0f, color, maxU, maxV, overlay, light, 0.0f, 0.0f, 1.0f);
        consumer.vertex(entry, x1, y0, 0.0f, color, maxU, minV, overlay, light, 0.0f, 0.0f, 1.0f);
        consumer.vertex(entry, x0, y0, 0.0f, color, minU, minV, overlay, light, 0.0f, 0.0f, 1.0f);
    }
}
