package com.yuriscat.echowarrior.compat.mixin;

import com.yuriscat.echowarrior.compat.client.InventoryInsertionClient1201;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.CreativeModeTab;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeModeInventoryScreenMixin1201 {
    @Redirect(method = "slotClicked", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/inventory/Slot;getItem()Lnet/minecraft/world/item/ItemStack;"))
    private net.minecraft.world.item.ItemStack echoWarrior$stableDropSnapshot(Slot readSlot, Slot clickedSlot,
            int slotId, int button, ClickType type) {
        var stack = readSlot.getItem();
        // 1.20.1's catalogue hotbar caches the live stack before menu.clicked(THROW).
        // Removing its last item empties that same object, suppressing the later creative drop packet.
        // Preserve only read snapshots of our bound items (including their containers); vanilla still owns the drop.
        return type == ClickType.THROW && !com.yuriscat.echowarrior.compat.item.SummonerStackContents1201.scan(stack).ids().isEmpty()
                ? stack.copy() : stack;
    }

    @org.spongepowered.asm.mixin.Shadow private Slot destroyItemSlot;
    @org.spongepowered.asm.mixin.Unique private final com.yuriscat.echowarrior.compat.client.CreativeDestructionClient1201 echoWarrior$destruction =
            new com.yuriscat.echowarrior.compat.client.CreativeDestructionClient1201();
    @Inject(method = "slotClicked", at = @At("HEAD"), cancellable = true)
    private void echoWarrior$insert(Slot slot, int slotId, int button, ClickType type, CallbackInfo callback) {
        echoWarrior$destruction.reset();
        if (InventoryInsertionClient1201.click((CreativeModeInventoryScreen)(Object)this, slot, button, type)) {
            callback.cancel();
            return;
        }
        echoWarrior$destruction.before((CreativeModeInventoryScreen)(Object)this, slot, destroyItemSlot, type);
    }

    @Inject(method = "slotClicked", at = @At("RETURN"))
    private void echoWarrior$confirmLoss(Slot slot, int slotId, int button, ClickType type, CallbackInfo callback) {
        echoWarrior$destruction.after();
    }
    @Inject(method = "removed", at = @At("HEAD"))
    private void echoWarrior$discardCursor(CallbackInfo callback) {
        com.yuriscat.echowarrior.compat.client.CreativeDestructionClient1201.closed((CreativeModeInventoryScreen)(Object)this);
    }
    @Inject(method = "handleHotbarLoadOrSave", at = @At("HEAD"))
    private static void echoWarrior$beforePreset(net.minecraft.client.Minecraft client, int index, boolean load, boolean save, CallbackInfo callback) {
        com.yuriscat.echowarrior.compat.client.CreativeDestructionClient1201.beforePreset(load);
    }
    @Inject(method = "handleHotbarLoadOrSave", at = @At("RETURN"))
    private static void echoWarrior$afterPreset(net.minecraft.client.Minecraft client, int index, boolean load, boolean save, CallbackInfo callback) {
        com.yuriscat.echowarrior.compat.client.CreativeDestructionClient1201.afterPreset(load);
    }

    @Inject(method = "selectTab", at = @At("HEAD"), cancellable = true)
    private void echoWarrior$waitForInsertion(CreativeModeTab tab, CallbackInfo callback) {
        if (InventoryInsertionClient1201.busy((CreativeModeInventoryScreen)(Object)this)) callback.cancel();
    }
}
