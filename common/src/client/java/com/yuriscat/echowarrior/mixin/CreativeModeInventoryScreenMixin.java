package com.yuriscat.echowarrior.mixin;

import com.yuriscat.echowarrior.item.SummonerStackContents;
import com.yuriscat.echowarrior.network.CreativeSummonerDestroyPayload;
import com.yuriscat.echowarrior.platform.ClientPlatformServices;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeModeInventoryScreenMixin {
	@Shadow private @Nullable Slot destroyItemSlot;
	@Unique private List<UUID> echoWarrior$destroyedSummoners = List.of();

	@Inject(method = "slotClicked", at = @At("HEAD"))
	private void echoWarrior$captureDestroyedSummoners(
			@Nullable Slot slot,
			int slotId,
			int buttonNum,
			ContainerInput containerInput,
			CallbackInfo callback
	) {
		this.echoWarrior$destroyedSummoners = List.of();
		if (slot == null || slot != this.destroyItemSlot) return;
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null) return;

		Set<UUID> found = new LinkedHashSet<>();
		if (containerInput == ContainerInput.QUICK_MOVE) {
			for (ItemStack root : minecraft.player.inventoryMenu.getItems()) {
				found.addAll(SummonerStackContents.summonerIds(root));
			}
		} else {
			found.addAll(SummonerStackContents.summonerIds(
					((CreativeModeInventoryScreen)(Object)this).getMenu().getCarried()));
		}
		this.echoWarrior$destroyedSummoners = List.copyOf(found);
	}

	@Inject(method = "slotClicked", at = @At("RETURN"))
	private void echoWarrior$sendDestroyedSummoners(
			@Nullable Slot slot,
			int slotId,
			int buttonNum,
			ContainerInput containerInput,
			CallbackInfo callback
	) {
		List<UUID> destroyed = this.echoWarrior$destroyedSummoners;
		this.echoWarrior$destroyedSummoners = List.of();
		if (!destroyed.isEmpty()) ClientPlatformServices.sendToServer(new CreativeSummonerDestroyPayload(destroyed));
	}
}
