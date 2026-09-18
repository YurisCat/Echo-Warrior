package com.yuriscat.echowarrior.compat.mixin;

import com.yuriscat.echowarrior.compat.binding.CreativeSummonerDestroyTracker1211;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerMixin1211 {
    @Shadow public ServerPlayer player;

    @Redirect(
            method = "handleSetCreativeModeSlot",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/Slot;setByPlayer(Lnet/minecraft/world/item/ItemStack;)V")
    )
    private void echoWarrior$trackCreativeSlotReplacement(Slot slot, ItemStack replacement) {
        ItemStack previous = slot.getItem().copy();
        slot.setByPlayer(replacement);
        CreativeSummonerDestroyTracker1211.noteCreativeSlotUpdate(this.player, previous, replacement);
    }
}
