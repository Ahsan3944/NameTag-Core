package com.ultraop.nametag.fabric.v1_21_11.mixin;

import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.util.math.AffineTransformation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(DisplayEntity.class)
public interface DisplayEntityAccessor {
    @Invoker("setTransformation")
    void nametagCore$setTransformation(AffineTransformation transformation);

    @Invoker("setBillboardMode")
    void nametagCore$setBillboardMode(DisplayEntity.BillboardMode billboardMode);

    @Invoker("setInterpolationDuration")
    void nametagCore$setInterpolationDuration(int interpolationDuration);

    @Invoker("setTeleportDuration")
    void nametagCore$setTeleportDuration(int teleportDuration);

    @Accessor("ITEM_DISPLAY")
    static TrackedData<Byte> nametagCore$getItemDisplayData() {
        throw new AssertionError();
    }
}
