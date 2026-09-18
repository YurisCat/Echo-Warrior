package com.yuriscat.echowarrior.compat.mixin;

import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen$SlotWrapper")
public interface CreativeModeSlotWrapperAccessor1211 {
    @Accessor("target")
    Slot echoWarrior$getTarget();
}
