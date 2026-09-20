package com.yuriscat.echowarrior.compat.mixin;

import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.CreativeModeTab;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(CreativeModeInventoryScreen.class)
public interface CreativeModeInventoryScreenInvoker1201 {
    @org.spongepowered.asm.mixin.gen.Accessor("destroyItemSlot")
    Slot echoWarrior$getDestroyItemSlot();
    @Invoker("slotClicked")
    void echoWarrior$invokeSlotClicked(Slot slot, int slotId, int button, ClickType clickType);

    @Invoker("selectTab")
    void echoWarrior$invokeSelectTab(CreativeModeTab tab);
}
