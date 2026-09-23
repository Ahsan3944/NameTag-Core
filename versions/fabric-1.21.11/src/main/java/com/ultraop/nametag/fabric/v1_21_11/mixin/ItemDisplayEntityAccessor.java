package com.ultraop.nametag.fabric.v1_21_11.mixin;

import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.item.ItemDisplayContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(DisplayEntity.ItemDisplayEntity.class)
public interface ItemDisplayEntityAccessor {
    @Invoker("setItemDisplayContext")
    void nametagCore$setItemDisplayContext(ItemDisplayContext context);
}
