package com.yuriscat.echowarrior.mixin;

import com.yuriscat.echowarrior.binding.CreativeSummonerDestroyTracker;
import com.yuriscat.echowarrior.item.TestEchoSummonerItem;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerMixin {
	@Shadow public ServerPlayer player;

	@Redirect(
			method = "handleSetCreativeModeSlot",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/Slot;setByPlayer(Lnet/minecraft/world/item/ItemStack;)V")
	)
	private void echoWarrior$trackCreativeSlotReplacement(Slot slot, ItemStack replacement) {
		ItemStack previous = slot.getItem().copy();
		slot.setByPlayer(replacement);
		CreativeSummonerDestroyTracker.noteCreativeSlotUpdate(this.player, previous, replacement);
	}

	@Inject(method = "handleSetCreativeModeSlot", at = @At("TAIL"))
	private void echoWarrior$commitCreativeSummonerUpdate(
			ServerboundSetCreativeModeSlotPacket packet,
			CallbackInfo callback
	) {
		int slotNumber = packet.slotNum();
		ItemStack packetStack = packet.itemStack();
		boolean validSlot = slotNumber >= 1 && slotNumber <= 45;
		boolean validData = packetStack.isEmpty() || packetStack.getCount() <= packetStack.getMaxStackSize();
		if (!this.player.hasInfiniteMaterials() || !validSlot || !validData) return;

		ItemStack storedStack = this.player.inventoryMenu.getSlot(slotNumber).getItem();
		if (storedStack.getItem() instanceof TestEchoSummonerItem) {
			TestEchoSummonerItem.commitCreativeInventoryUpdate(this.player, storedStack);
		}
	}
}
