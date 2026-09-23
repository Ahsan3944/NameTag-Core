package com.ultraop.nametag.fabric.v1_21_11.mixin;

import com.ultraop.nametag.fabric.client.NameTagFabricClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerNameplateItemMixin {
    @Inject(method = "renderLabelIfPresent", at = @At("TAIL"))
    private void nametagCore$renderItemNameplate(
            PlayerEntityRenderState state,
            MatrixStack matrices,
            OrderedRenderCommandQueue queue,
            net.minecraft.client.render.state.CameraRenderState cameraRenderState,
            CallbackInfo ci
    ) {
        if (NameTagFabricClient.itemFor(state) == null) return;
        if (state.displayName == null || state.nameLabelPos == null) return;

        MinecraftClient client = MinecraftClient.getInstance();
        float textWidth = client.textRenderer.getWidth(state.displayName);
        float prefixWidth = NameTagFabricClient.tagPrefixWidth(state);

        matrices.push();
        matrices.translate(
                state.nameLabelPos.x,
                state.nameLabelPos.y,
                state.nameLabelPos.z
        );
        Quaternionf rotation = client.getEntityRenderDispatcher().camera.getRotation();
        matrices.multiply(rotation);
        matrices.scale(-0.025f, -0.025f, 0.025f);

        NameTagFabricClient.renderItem(
                state, matrices, queue, state.light, textWidth, prefixWidth
        );
        matrices.pop();
    }
}
