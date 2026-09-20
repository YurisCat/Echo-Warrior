package com.yuriscat.echowarrior.compat.client;

import com.yuriscat.echowarrior.compat.item.SummonerStackContents1201;
import com.yuriscat.echowarrior.compat.network.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import java.util.*;

/** Reports possible permanent loss; never removes world bindings on the client. */
public final class CreativeDestructionClient1201 {
    private Set<UUID> before = Set.of();
    private Set<UUID> explicit = Set.of();
    private static Set<UUID> hotbarBefore = Set.of();

    public void reset() { before = Set.of(); explicit = Set.of(); }
    public void before(CreativeModeInventoryScreen screen, Slot slot, Slot trash, ClickType type) {
        reset();
        if (slot == null || type == ClickType.THROW) return;
        before = visible();
        if (slot == trash) {
            if (type == ClickType.PICKUP) explicit = ids(screen.getMenu().getCarried());
            else if (type == ClickType.QUICK_MOVE) explicit = before;
        } else if (type == ClickType.QUICK_MOVE) explicit = ids(slot.getItem());
    }
    public void after() {
        var candidates = new HashSet<>(before);
        candidates.removeAll(visible());
        candidates.addAll(explicit);
        reset();
        send(candidates);
    }
    public static void closed(CreativeModeInventoryScreen screen) { send(ids(screen.getMenu().getCarried())); }
    public static void beforePreset(boolean load) { hotbarBefore = load ? visible() : Set.of(); }
    public static void afterPreset(boolean load) {
        var candidates = new HashSet<>(hotbarBefore);
        hotbarBefore = Set.of();
        if (!load) return;
        candidates.removeAll(visible());
        send(candidates);
    }
    private static Set<UUID> ids(ItemStack stack) {
        var scan = SummonerStackContents1201.scan(stack);
        return scan.complete() ? scan.ids() : Set.of();
    }
    private static Set<UUID> visible() {
        var client = Minecraft.getInstance();
        if (client.player == null) return Set.of();
        var scan = new SummonerStackContents1201.VisibilityScan();
        client.player.inventoryMenu.getItems().forEach(scan::accept);
        if (client.screen instanceof CreativeModeInventoryScreen screen) scan.accept(screen.getMenu().getCarried());
        var result = scan.result();
        return result.complete() ? result.ids() : Set.of();
    }
    private static void send(Set<UUID> candidates) {
        if (candidates.isEmpty() || InventoryNetwork1201.destructionSender == null) return;
        var ordered = List.copyOf(candidates);
        for (int index = 0; index < ordered.size(); index += CreativeDestructionRequest1201.MAX_IDS) {
            InventoryNetwork1201.destructionSender.accept(new CreativeDestructionRequest1201(
                    ordered.subList(index, Math.min(ordered.size(), index + CreativeDestructionRequest1201.MAX_IDS))));
        }
    }
}
