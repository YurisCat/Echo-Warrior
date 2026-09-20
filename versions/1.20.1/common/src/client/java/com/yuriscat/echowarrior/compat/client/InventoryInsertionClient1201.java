package com.yuriscat.echowarrior.compat.client;

import com.yuriscat.echowarrior.compat.item.SummonerData1201;
import com.yuriscat.echowarrior.compat.item.SummonerFuel1201;
import com.yuriscat.echowarrior.compat.item.SummonerFuelInsertFeedback1201;
import com.yuriscat.echowarrior.compat.menu.SummonerTransfer1201;
import com.yuriscat.echowarrior.compat.mixin.CreativeModeSlotWrapperAccessor1201;
import com.yuriscat.echowarrior.compat.network.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Creative cursor is virtual; only an accepted server transaction may consume it. */
public final class InventoryInsertionClient1201 {
    private static int sequence;
    private static Pending pending;
    private record Pending(int id, CreativeModeInventoryScreen screen, Slot slot, ItemStack cursor) {}
    private InventoryInsertionClient1201() {}

    public static void initialize() {
        InventoryNetwork1201.creativeReceiver = InventoryInsertionClient1201::receive;
        InventoryNetwork1201.feedbackReceiver = InventoryInsertionClient1201::feedback;
    }

    public static boolean busy(CreativeModeInventoryScreen screen) {
        return pending != null && pending.screen == screen;
    }

    public static boolean click(CreativeModeInventoryScreen screen, Slot slot, int button, ClickType type) {
        if (busy(screen)) return true;
        var player = Minecraft.getInstance().player;
        if (player == null || slot == null || button != 0 || type != ClickType.PICKUP) return false;
        ItemStack carried = screen.getMenu().getCarried();
        if (!slot.getItem().is(com.yuriscat.echowarrior.compat.ModContent1201.ECHO_SUMMONER) || !SummonerTransfer1201.candidate(carried)) return false;
        int inventorySlot;
        if (slot instanceof CreativeModeSlotWrapperAccessor1201 wrapper) {
            inventorySlot = wrapper.echoWarrior$getTarget().index;
        } else {
            int hotbarStart = screen.getMenu().slots.size() - 9;
            if (slot.index < hotbarStart) return false;
            inventorySlot = slot.index - screen.getMenu().slots.size() + 45;
        }
        var id = SummonerData1201.summonerId(slot.getItem());
        if (id == null || InventoryNetwork1201.creativeSender == null) return true;
        pending = new Pending(++sequence, screen, slot, carried.copy());
        InventoryNetwork1201.creativeSender.accept(new CreativeInsertRequest1201(
                sequence, inventorySlot, id, SummonerData1201.revision(slot.getItem()), carried));
        return true;
    }

    private static void receive(CreativeInsertReply1201 reply) {
        Pending current = pending;
        if (current == null || current.id != reply.requestId()) return;
        pending = null;
        var client = Minecraft.getInstance();
        // Never restore a closed screen's virtual cursor into a new inventory.
        if (client.screen != current.screen || reply.inserted() <= 0 || reply.inserted() > current.cursor.getCount()) return;
        ItemStack cursor = current.screen.getMenu().getCarried();
        if (!ItemStack.matches(cursor, current.cursor)) return;
        ItemStack remaining = current.cursor.copy();
        remaining.shrink(reply.inserted());
        current.screen.getMenu().setCarried(remaining);
        play(current.slot, current.cursor);
    }

    private static void feedback(InsertionFeedback1201 message) {
        var client = Minecraft.getInstance();
        if (AutomatedTestController1201.ENABLED) com.yuriscat.echowarrior.compat.EchoWarrior1201.LOGGER.info(
                "[Compat1201] Insertion feedback received menu={} slot={} item={} screen={}",
                message.menuId(), message.slot(), message.item(), client.screen == null ? "none" : client.screen.getClass().getSimpleName());
        if (!(client.screen instanceof AbstractContainerScreen<?> screen)) return;
        var menu = screen.getMenu();
        if (menu.containerId != message.menuId() || message.slot() < 0 || message.slot() >= menu.slots.size()) return;
        play(menu.getSlot(message.slot()), message.item());
    }

    private static void play(Slot slot, ItemStack item) {
        // Both loaders acknowledge the server transaction here; do not sound on prediction or rejection.
        Minecraft.getInstance().getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                net.minecraft.sounds.SoundEvents.BUNDLE_INSERT, 1.0F, 0.8F));
        if (SummonerFuel1201.value(item) > 0) SummonerFuelInsertFeedback1201.playFuel(slot, item);
        else SummonerFuelInsertFeedback1201.playPolish(slot);
    }
}
