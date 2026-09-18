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
	@Unique private Set<UUID> echoWarrior$visibleSummonersBefore = Set.of();
	@Unique private Set<UUID> echoWarrior$explicitDestructionCandidates = Set.of();
	@Unique private static Set<UUID> echoWarrior$hotbarSummonersBefore = Set.of();

	@Inject(method = "slotClicked", at = @At("HEAD"))
	private void echoWarrior$captureDestroyedSummoners(
			@Nullable Slot slot,
			int slotId,
			int buttonNum,
			ContainerInput containerInput,
			CallbackInfo callback
	) {
		this.echoWarrior$visibleSummonersBefore = Set.of();
		this.echoWarrior$explicitDestructionCandidates = Set.of();
		if (slot != null && containerInput != ContainerInput.THROW) {
			this.echoWarrior$visibleSummonersBefore = echoWarrior$visibleClientSummoners();
		}
		if (slot == null) return;
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null) return;

		Set<UUID> found = new LinkedHashSet<>();
		if (slot == this.destroyItemSlot && containerInput == ContainerInput.QUICK_MOVE) {
			for (ItemStack root : minecraft.player.inventoryMenu.getItems()) {
				found.addAll(SummonerStackContents.summonerIds(root));
			}
		} else if (slot == this.destroyItemSlot) {
			found.addAll(SummonerStackContents.summonerIds(
					((CreativeModeInventoryScreen)(Object)this).getMenu().getCarried()));
		} else if (containerInput == ContainerInput.QUICK_MOVE) {
			found.addAll(SummonerStackContents.summonerIds(slot.getItem()));
		}
		this.echoWarrior$explicitDestructionCandidates = Set.copyOf(found);
	}

	@Inject(method = "slotClicked", at = @At("RETURN"))
	private void echoWarrior$sendDestroyedSummoners(
			@Nullable Slot slot,
			int slotId,
			int buttonNum,
			ContainerInput containerInput,
			CallbackInfo callback
	) {
		Set<UUID> destroyed = new LinkedHashSet<>(this.echoWarrior$explicitDestructionCandidates);
		Set<UUID> visibleBefore = this.echoWarrior$visibleSummonersBefore;
		if (!visibleBefore.isEmpty()) {
			Set<UUID> visibleAfter = echoWarrior$visibleClientSummoners();
			visibleBefore.stream().filter(id -> !visibleAfter.contains(id)).forEach(destroyed::add);
		}
		this.echoWarrior$visibleSummonersBefore = Set.of();
		this.echoWarrior$explicitDestructionCandidates = Set.of();
		if (!destroyed.isEmpty()) {
			ClientPlatformServices.sendToServer(new CreativeSummonerDestroyPayload(List.copyOf(destroyed)));
		}
	}

	@Inject(method = "removed", at = @At("HEAD"))
	private void echoWarrior$destroyCarriedSummonerWhenScreenCloses(CallbackInfo callback) {
		CreativeModeInventoryScreen screen = (CreativeModeInventoryScreen)(Object)this;
		List<UUID> destroyed = List.copyOf(SummonerStackContents.summonerIds(screen.getMenu().getCarried()));
		if (!destroyed.isEmpty()) {
			ClientPlatformServices.sendToServer(new CreativeSummonerDestroyPayload(destroyed));
		}
	}

	@Inject(method = "handleHotbarLoadOrSave", at = @At("HEAD"))
	private static void echoWarrior$captureHotbarBeforePresetLoad(
			Minecraft minecraft, int hotbarIndex, boolean load, boolean save, CallbackInfo callback
	) {
		echoWarrior$hotbarSummonersBefore = load ? echoWarrior$visibleClientSummoners() : Set.of();
	}

	@Inject(method = "handleHotbarLoadOrSave", at = @At("RETURN"))
	private static void echoWarrior$destroySummonersOverwrittenByPresetLoad(
			Minecraft minecraft, int hotbarIndex, boolean load, boolean save, CallbackInfo callback
	) {
		Set<UUID> visibleBefore = echoWarrior$hotbarSummonersBefore;
		echoWarrior$hotbarSummonersBefore = Set.of();
		if (!load || visibleBefore.isEmpty()) return;
		Set<UUID> visibleAfter = echoWarrior$visibleClientSummoners();
		List<UUID> destroyed = visibleBefore.stream().filter(id -> !visibleAfter.contains(id)).toList();
		if (!destroyed.isEmpty()) {
			ClientPlatformServices.sendToServer(new CreativeSummonerDestroyPayload(destroyed));
		}
	}

	@Unique
	private static Set<UUID> echoWarrior$visibleClientSummoners() {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null) return Set.of();
		Set<UUID> visible = new LinkedHashSet<>();
		for (ItemStack root : minecraft.player.inventoryMenu.getItems()) {
			visible.addAll(SummonerStackContents.summonerIds(root));
		}
		if (minecraft.screen instanceof CreativeModeInventoryScreen creativeScreen) {
			visible.addAll(SummonerStackContents.summonerIds(creativeScreen.getMenu().getCarried()));
		}
		return visible;
	}
}
