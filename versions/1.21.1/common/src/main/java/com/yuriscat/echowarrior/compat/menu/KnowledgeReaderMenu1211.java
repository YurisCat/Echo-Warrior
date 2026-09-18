package com.yuriscat.echowarrior.compat.menu;

import com.yuriscat.echowarrior.compat.ModContent1211;
import com.yuriscat.echowarrior.compat.knowledge.KnowledgeCatalog1211;
import com.yuriscat.echowarrior.compat.knowledge.KnowledgeStackData1211;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.LinkedHashMap;
import java.util.List;

public final class KnowledgeReaderMenu1211 extends AbstractContainerMenu {
    public static final int BUTTON_PREVIOUS = 0;
    public static final int BUTTON_NEXT = 1;
    public static final int BUTTON_EXTRACT = 2;

    private final Inventory inventory;
    private final Slot sourceSlot;
    private String currentKnowledgeId;

    public KnowledgeReaderMenu1211(int containerId, Inventory inventory) {
        this(containerId, inventory, findSourceSlot(inventory));
    }

    public KnowledgeReaderMenu1211(int containerId, Inventory inventory, int sourceInventorySlot) {
        super(ModContent1211.KNOWLEDGE_READER_MENU, containerId);
        this.inventory = inventory;
        this.sourceSlot = this.addSlot(new LockedSlot(inventory, sourceInventorySlot, -1000, -1000));
        this.currentKnowledgeId = KnowledgeStackData1211.initialPage(this.sourceSlot.getItem());
    }

    public static InteractionResultHolder<ItemStack> open(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!isReadable(stack) || KnowledgeStackData1211.initialPage(stack).isEmpty()) {
            return InteractionResultHolder.fail(stack);
        }
        if (player instanceof ServerPlayer serverPlayer) {
            int sourceSlot = hand == InteractionHand.MAIN_HAND
                    ? player.getInventory().selected
                    : Inventory.SLOT_OFFHAND;
            serverPlayer.openMenu(new SimpleMenuProvider(
                    (containerId, inventory, ignored) -> new KnowledgeReaderMenu1211(containerId, inventory, sourceSlot),
                    stack.getHoverName()));
        }
        return InteractionResultHolder.sidedSuccess(stack, player.level().isClientSide);
    }

    public ItemStack sourceStack() { return this.sourceSlot.getItem(); }
    public String currentKnowledgeId() { return this.currentKnowledgeId; }

    public List<String> visiblePages() {
        ItemStack stack = sourceStack();
        if (stack.is(ModContent1211.KNOWLEDGE_FRAGMENT)) {
            return KnowledgeStackData1211.fragmentId(stack).map(List::of).orElse(List.of());
        }
        return KnowledgeCatalog1211.presentIds(KnowledgeStackData1211.collectionCounts(stack));
    }

    public boolean isCollection() { return sourceStack().is(ModContent1211.KNOWLEDGE_FRAGMENT_COLLECTION); }

    public int pageCount(String id) {
        if (!isCollection()) {
            return KnowledgeStackData1211.fragmentId(sourceStack()).filter(id::equals).isPresent()
                    ? sourceStack().getCount() : 0;
        }
        return KnowledgeStackData1211.collectionCounts(sourceStack()).getOrDefault(id, 0);
    }

    public void selectClientPage(String id) {
        if (visiblePages().contains(id)) this.currentKnowledgeId = id;
    }

    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        if (buttonId == BUTTON_EXTRACT) return extractCurrentPage(player);
        List<String> pages = visiblePages();
        int index = pages.indexOf(this.currentKnowledgeId);
        if (index < 0 && !pages.isEmpty()) index = 0;
        int desired = buttonId == BUTTON_PREVIOUS ? index - 1 : buttonId == BUTTON_NEXT ? index + 1 : -1;
        if (desired < 0 || desired >= pages.size()) return false;
        this.currentKnowledgeId = pages.get(desired);
        if (isCollection()) {
            KnowledgeStackData1211.setBookmark(sourceStack(), this.currentKnowledgeId);
            this.sourceSlot.setChanged();
            this.broadcastChanges();
        }
        return true;
    }

    private boolean extractCurrentPage(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer) || !isCollection()) return false;
        LinkedHashMap<String, Integer> counts = KnowledgeStackData1211.collectionCounts(sourceStack());
        int currentCount = counts.getOrDefault(this.currentKnowledgeId, 0);
        if (currentCount <= 0) return false;
        if (currentCount == 1) counts.remove(this.currentKnowledgeId);
        else counts.put(this.currentKnowledgeId, currentCount - 1);

        long remainingTotal = KnowledgeStackData1211.totalCount(counts);
        ItemStack extracted = KnowledgeStackData1211.fragment(this.currentKnowledgeId);
        if (remainingTotal == 1) {
            String remainingId = KnowledgeCatalog1211.presentIds(counts).get(0);
            this.sourceSlot.set(KnowledgeStackData1211.fragment(remainingId));
            this.sourceSlot.setChanged();
            player.getInventory().placeItemBackInInventory(extracted);
            this.broadcastChanges();
            serverPlayer.closeContainer();
            return true;
        }
        if (remainingTotal < 2) return false;
        String nextPage = counts.containsKey(this.currentKnowledgeId)
                ? this.currentKnowledgeId
                : KnowledgeStackData1211.pageAfterRemoval(counts, this.currentKnowledgeId);
        KnowledgeStackData1211.writeCollection(sourceStack(), counts, nextPage);
        this.currentKnowledgeId = nextPage;
        this.sourceSlot.setChanged();
        player.getInventory().placeItemBackInInventory(extracted);
        this.broadcastChanges();
        return true;
    }

    @Override public ItemStack quickMoveStack(Player player, int slot) { return ItemStack.EMPTY; }
    @Override public boolean stillValid(Player player) {
        return this.inventory.player == player && isReadable(sourceStack());
    }

    private static int findSourceSlot(Inventory inventory) {
        if (isReadable(inventory.getSelected())) return inventory.selected;
        if (isReadable(inventory.getItem(Inventory.SLOT_OFFHAND))) return Inventory.SLOT_OFFHAND;
        return inventory.selected;
    }

    private static boolean isReadable(ItemStack stack) {
        return stack.is(ModContent1211.KNOWLEDGE_FRAGMENT)
                || stack.is(ModContent1211.KNOWLEDGE_FRAGMENT_COLLECTION);
    }

    private static final class LockedSlot extends Slot {
        private LockedSlot(Inventory inventory, int slot, int x, int y) { super(inventory, slot, x, y); }
        @Override public boolean mayPlace(ItemStack stack) { return false; }
        @Override public boolean mayPickup(Player player) { return false; }
    }
}
