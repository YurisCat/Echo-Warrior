package com.yuriscat.echowarrior.compat.menu;

import com.yuriscat.echowarrior.compat.ModContent1201;
import com.yuriscat.echowarrior.compat.tutorial.TutorialManualCatalog1201;
import com.yuriscat.echowarrior.compat.tutorial.TutorialManualStackData1201;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class TutorialManualMenu1201 extends HeldBookMenu1201 {
    public static final int BUTTON_PREVIOUS = 0;
    public static final int BUTTON_NEXT = 1;
    public static final int BUTTON_JUMP_START = 100;

    private final DataSlot currentPage = DataSlot.standalone();

    public TutorialManualMenu1201(int containerId, Inventory inventory) {
        this(containerId, inventory, -1);
    }

    public TutorialManualMenu1201(int containerId, Inventory inventory, int sourceInventorySlot) {
        super(ModContent1201.TUTORIAL_MANUAL_MENU, containerId, inventory, sourceInventorySlot);
        this.currentPage.set(TutorialManualStackData1201.bookmark(this.sourceSlot.getItem()));
        this.addDataSlot(this.currentPage);
    }

    public static InteractionResultHolder<ItemStack> open(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!stack.is(ModContent1201.TUTORIAL_MANUAL)) return InteractionResultHolder.fail(stack);
        if (player instanceof ServerPlayer serverPlayer) {
            int sourceSlot = hand == InteractionHand.MAIN_HAND
                    ? player.getInventory().selected
                    : Inventory.SLOT_OFFHAND;
            serverPlayer.openMenu(new SimpleMenuProvider(
                    (containerId, inventory, ignored) -> new TutorialManualMenu1201(containerId, inventory, sourceSlot),
                    stack.getHoverName()));
        }
        return InteractionResultHolder.sidedSuccess(stack, player.level().isClientSide);
    }

    public int currentPage() {
        return net.minecraft.util.Mth.clamp(this.currentPage.get(), 0, TutorialManualCatalog1201.pageCount() - 1);
    }

    public void selectClientPage(int page) {
        this.currentPage.set(net.minecraft.util.Mth.clamp(page, 0, TutorialManualCatalog1201.pageCount() - 1));
    }

    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        if (!(player instanceof ServerPlayer) || !stillValid(player)) return false;
        int desired = buttonId == BUTTON_PREVIOUS ? currentPage() - 1
                : buttonId == BUTTON_NEXT ? currentPage() + 1
                : buttonId >= BUTTON_JUMP_START ? buttonId - BUTTON_JUMP_START : -1;
        if (desired < 0 || desired >= TutorialManualCatalog1201.pageCount()) return false;
        this.currentPage.set(desired);
        TutorialManualStackData1201.setBookmark(this.sourceSlot.getItem(), desired);
        this.sourceSlot.setChanged();
        this.broadcastChanges();
        return true;
    }

    @Override public ItemStack quickMoveStack(Player player, int slot) { return ItemStack.EMPTY; }
    @Override public boolean stillValid(Player player) {
        return this.inventory.player == player && this.sourceSlot.getItem().is(ModContent1201.TUTORIAL_MANUAL);
    }

    private static int findSourceSlot(Inventory inventory) {
        if (inventory.getSelected().is(ModContent1201.TUTORIAL_MANUAL)) return inventory.selected;
        if (inventory.getItem(Inventory.SLOT_OFFHAND).is(ModContent1201.TUTORIAL_MANUAL)) return Inventory.SLOT_OFFHAND;
        return inventory.selected;
    }

    private static final class LockedSlot extends Slot {
        private LockedSlot(net.minecraft.world.Container inventory, int slot, int x, int y) { super(inventory, slot, x, y); }
        @Override public boolean mayPickup(Player player) { return false; }
        @Override public boolean mayPlace(ItemStack stack) { return false; }
    }
}
