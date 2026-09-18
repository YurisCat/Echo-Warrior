package com.yuriscat.echowarrior.compat.mixin;

import com.yuriscat.echowarrior.compat.EchoWarrior1211;
import com.yuriscat.echowarrior.compat.client.CreativeSummonerDestructionSender1211;
import com.yuriscat.echowarrior.compat.client.CreativeSummonerInsertionSender1211;
import com.yuriscat.echowarrior.compat.item.EchoSummonerItem1211;
import com.yuriscat.echowarrior.compat.item.SummonerStackContents1211;
import com.yuriscat.echowarrior.compat.item.SummonerFuel1211;
import com.yuriscat.echowarrior.compat.item.SummonerFuelInsertFeedback1211;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.spongepowered.asm.mixin.Unique;

@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeModeInventoryScreenMixin1211 {
    @Shadow private Slot destroyItemSlot;
    @Unique private Set<UUID> echoWarrior$visibleSummonersBefore = Set.of();
    @Unique private Set<UUID> echoWarrior$explicitDestructionCandidates = Set.of();
    @Unique private static Set<UUID> echoWarrior$hotbarSummonersBefore = Set.of();

    @Inject(method = "slotClicked", at = @At("HEAD"), cancellable = true)
    private void echoWarrior$handleDirectSummonerInsertion(
            Slot slot, int slotId, int button, ClickType clickType, CallbackInfo callback) {
        this.echoWarrior$visibleSummonersBefore = Set.of();
        this.echoWarrior$explicitDestructionCandidates = Set.of();
        if (slot != null && clickType != ClickType.THROW) {
            this.echoWarrior$visibleSummonersBefore = echoWarrior$visibleClientSummoners();
        }
        if (slot == this.destroyItemSlot) {
            CreativeModeInventoryScreen screen = (CreativeModeInventoryScreen)(Object)this;
            Set<UUID> found = new LinkedHashSet<>();
            if (clickType == ClickType.PICKUP) {
                found.addAll(SummonerStackContents1211.summonerIds(screen.getMenu().getCarried()));
            } else if (clickType == ClickType.QUICK_MOVE && Minecraft.getInstance().player != null) {
                for (ItemStack inventoryStack : Minecraft.getInstance().player.inventoryMenu.getItems()) {
                    found.addAll(SummonerStackContents1211.summonerIds(inventoryStack));
                }
            }
            this.echoWarrior$explicitDestructionCandidates = Set.copyOf(found);
            return;
        }
        if (slot != null && clickType == ClickType.QUICK_MOVE) {
            Set<UUID> summonerIds = Set.copyOf(SummonerStackContents1211.summonerIds(slot.getItem()));
            this.echoWarrior$explicitDestructionCandidates = summonerIds;
            if (!summonerIds.isEmpty()) {
                CreativeModeInventoryScreen screen = (CreativeModeInventoryScreen)(Object)this;
                EchoWarrior1211.LOGGER.info(
                        "[CreativeSummonerDelete] client captured quick move: slotId={} slotIndex={} inventoryTab={} ids={}",
                        slotId, slot.index, screen.isInventoryOpen(), summonerIds);
            }
        }
        if (slot == null || clickType != ClickType.PICKUP || button != 0) return;
        CreativeModeInventoryScreen screen = (CreativeModeInventoryScreen)(Object)this;
        ItemStack summoner = slot.getItem();
        ItemStack carried = screen.getMenu().getCarried();
        if (!(summoner.getItem() instanceof EchoSummonerItem1211)
                || carried.isEmpty()
                || !EchoSummonerItem1211.isDirectInsertionCandidate(carried)) return;
        if (Boolean.getBoolean("echo_warrior.auto_pause_after_quick_play")) {
            EchoWarrior1211.LOGGER.info(
                    "Automated creative click reached summoner mixin: slotId={} slotIndex={} inventoryTab={} carried={}",
                    slotId, slot.index, screen.isInventoryOpen(), carried);
        }

        int inventorySlot;
        if (screen.isInventoryOpen()) {
            if (!((Object)slot instanceof CreativeModeSlotWrapperAccessor1211 wrapper)) return;
            inventorySlot = wrapper.echoWarrior$getTarget().index;
        } else {
            int firstHotbarSlotId = screen.getMenu().slots.size() - 9;
            if (slotId < firstHotbarSlotId) return;
            inventorySlot = slot.index - screen.getMenu().slots.size() + 45;
        }
        if (inventorySlot < 0 || inventorySlot > 45) return;

        ItemStack summonerAfter = summoner.copy();
        ItemStack carriedAfter = carried.copy();
        boolean fuel = SummonerFuel1211.isFuel(carried);
        ItemStack fuelForFeedback = fuel ? carried.copyWithCount(1) : ItemStack.EMPTY;
        Minecraft minecraft = Minecraft.getInstance();
        if (!EchoSummonerItem1211.insertIntoInternalSlotForCreative(summonerAfter, carriedAfter)) {
            if (minecraft.player != null) {
                minecraft.player.playSound(SoundEvents.DISPENSER_FAIL, 0.7F, 1.0F);
            }
            callback.cancel();
            return;
        }

        CreativeSummonerInsertionSender1211.send(inventorySlot, carried);
        screen.getMenu().setCarried(carriedAfter);
        slot.setByPlayer(summonerAfter);
        slot.setChanged();
        screen.getMenu().broadcastChanges();
        if (Boolean.getBoolean("echo_warrior.auto_pause_after_quick_play")) {
            EchoWarrior1211.LOGGER.info(
                    "Automated creative click committed summoner: packetSlot={} summoner={} remaining={}",
                    inventorySlot, summonerAfter, carriedAfter);
        }
        if (minecraft.player != null) {
            minecraft.player.playSound(SoundEvents.BUNDLE_INSERT, 0.8F, 1.0F);
            if (fuel) SummonerFuelInsertFeedback1211.playFuel(slot, fuelForFeedback);
            else SummonerFuelInsertFeedback1211.playPolish(slot);
        }
        callback.cancel();
    }

    @Inject(method = "slotClicked", at = @At("RETURN"))
    private void echoWarrior$sendDestroyedSummoners(
            Slot slot, int slotId, int button, ClickType clickType, CallbackInfo callback) {
        Set<UUID> destroyed = new LinkedHashSet<>(this.echoWarrior$explicitDestructionCandidates);
        Set<UUID> visibleBefore = this.echoWarrior$visibleSummonersBefore;
        if (!visibleBefore.isEmpty()) {
            Set<UUID> visibleAfter = echoWarrior$visibleClientSummoners();
            visibleBefore.stream().filter(id -> !visibleAfter.contains(id)).forEach(destroyed::add);
        }
        this.echoWarrior$visibleSummonersBefore = Set.of();
        this.echoWarrior$explicitDestructionCandidates = Set.of();
        if (!destroyed.isEmpty()) {
            EchoWarrior1211.LOGGER.info(
                    "[CreativeSummonerDelete] client sending destruction request: slotId={} clickType={} ids={}",
                    slotId, clickType, destroyed);
            CreativeSummonerDestructionSender1211.send(List.copyOf(destroyed));
        }
    }

    @Inject(method = "removed", at = @At("HEAD"))
    private void echoWarrior$destroyCarriedSummonerWhenScreenCloses(CallbackInfo callback) {
        CreativeModeInventoryScreen screen = (CreativeModeInventoryScreen)(Object)this;
        List<UUID> destroyed = List.copyOf(SummonerStackContents1211.summonerIds(screen.getMenu().getCarried()));
        if (destroyed.isEmpty()) return;
        EchoWarrior1211.LOGGER.info(
                "[CreativeSummonerDelete] client discarded carried summoner while closing creative screen: ids={}",
                destroyed);
        CreativeSummonerDestructionSender1211.send(destroyed);
    }

    @Inject(method = "handleHotbarLoadOrSave", at = @At("HEAD"))
    private static void echoWarrior$captureHotbarBeforePresetLoad(
            Minecraft minecraft, int hotbarIndex, boolean load, boolean save, CallbackInfo callback) {
        echoWarrior$hotbarSummonersBefore = load ? echoWarrior$visibleClientSummoners() : Set.of();
    }

    @Inject(method = "handleHotbarLoadOrSave", at = @At("RETURN"))
    private static void echoWarrior$destroySummonersOverwrittenByPresetLoad(
            Minecraft minecraft, int hotbarIndex, boolean load, boolean save, CallbackInfo callback) {
        Set<UUID> visibleBefore = echoWarrior$hotbarSummonersBefore;
        echoWarrior$hotbarSummonersBefore = Set.of();
        if (!load || visibleBefore.isEmpty()) return;
        Set<UUID> visibleAfter = echoWarrior$visibleClientSummoners();
        List<UUID> destroyed = visibleBefore.stream()
                .filter(id -> !visibleAfter.contains(id))
                .toList();
        if (destroyed.isEmpty()) return;
        EchoWarrior1211.LOGGER.info(
                "[CreativeSummonerDelete] client detected hotbar preset overwrite: preset={} ids={}",
                hotbarIndex, destroyed);
        CreativeSummonerDestructionSender1211.send(destroyed);
    }

    @Unique
    private static Set<UUID> echoWarrior$visibleClientSummoners() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return Set.of();
        Set<UUID> visible = new LinkedHashSet<>();
        for (ItemStack root : minecraft.player.inventoryMenu.getItems()) {
            visible.addAll(SummonerStackContents1211.summonerIds(root));
        }
        if (minecraft.screen instanceof CreativeModeInventoryScreen creativeScreen) {
            visible.addAll(SummonerStackContents1211.summonerIds(creativeScreen.getMenu().getCarried()));
        }
        return visible;
    }
}
