package com.yuriscat.echowarrior.compat.menu;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import java.util.List;

/** Slot contents and the authoritative held-slot index may arrive in either order. */
public abstract class HeldBookMenu1201 extends AbstractContainerMenu {
    protected final Inventory inventory;
    protected final Slot sourceSlot;
    private final DataSlot sourceIndex = DataSlot.standalone();
    private boolean receivedSnapshot;

    protected HeldBookMenu1201(MenuType<?> type, int id, Inventory inventory, int sourceInventorySlot) {
        super(type, id);
        this.inventory = inventory;
        sourceIndex.set(sourceInventorySlot);
        addDataSlot(sourceIndex);
        sourceSlot = addSlot(new LockedSlot(sourceInventorySlot < 0 ? new SimpleContainer(1) : inventory,
                sourceInventorySlot < 0 ? 0 : sourceInventorySlot));
    }
    @Override public void setItem(int slot, int stateId, ItemStack stack) {
        super.setItem(slot, stateId, stack);
        if (slot == 0) receivedSnapshot = true;
        mirrorConfirmedSource();
    }
    @Override public void initializeContents(int stateId, List<ItemStack> stacks, ItemStack carried) {
        super.initializeContents(stateId, stacks, carried);
        receivedSnapshot = !stacks.isEmpty();
        mirrorConfirmedSource();
    }
    @Override public void setData(int index, int value) {
        super.setData(index, value);
        mirrorConfirmedSource();
    }
    private void mirrorConfirmedSource() {
        int index = sourceIndex.get();
        if (inventory.player.level().isClientSide && receivedSnapshot && index >= 0 && index < inventory.getContainerSize()) {
            inventory.setItem(index, sourceSlot.getItem().copy());
        }
    }
    private static final class LockedSlot extends Slot {
        private LockedSlot(Container container, int slot) { super(container, slot, -1000, -1000); }
        @Override public boolean mayPlace(ItemStack stack) { return false; }
        @Override public boolean mayPickup(Player player) { return false; }
    }
}
