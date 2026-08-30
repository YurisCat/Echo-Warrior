package com.yuriscat.echowarrior.menu;

import com.yuriscat.echowarrior.ModItems;
import com.yuriscat.echowarrior.ModMenus;
import com.yuriscat.echowarrior.knowledge.KnowledgeCatalog;
import com.yuriscat.echowarrior.knowledge.KnowledgeStackData;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.LinkedHashMap;
import java.util.List;

public final class KnowledgeReaderMenu extends AbstractContainerMenu {
	public static final int BUTTON_PREVIOUS = 0;
	public static final int BUTTON_NEXT = 1;
	public static final int BUTTON_EXTRACT = 2;

	private final Inventory inventory;
	private final int sourceInventorySlot;
	private final Slot sourceSlot;
	private String currentKnowledgeId;

	public KnowledgeReaderMenu(int containerId, Inventory inventory, Integer sourceInventorySlot) {
		super(ModMenus.KNOWLEDGE_READER, containerId);
		this.inventory = inventory;
		this.sourceInventorySlot = sourceInventorySlot;
		this.sourceSlot = this.addSlot(new LockedSlot(inventory, sourceInventorySlot, -1000, -1000));
		this.currentKnowledgeId = KnowledgeStackData.initialPage(this.sourceSlot.getItem());
	}

	public static InteractionResult open(Player player, InteractionHand hand) {
		if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.SUCCESS;
		int sourceSlot = hand == InteractionHand.MAIN_HAND
				? player.getInventory().getSelectedSlot()
				: Inventory.SLOT_OFFHAND;
		ItemStack stack = player.getItemInHand(hand);
		if (!isReadable(stack) || KnowledgeStackData.initialPage(stack).isEmpty()) return InteractionResult.FAIL;
		serverPlayer.openMenu(new ExtendedMenuProvider<Integer>() {
			@Override
			public Integer getScreenOpeningData(ServerPlayer openingPlayer) {
				return sourceSlot;
			}

			@Override
			public Component getDisplayName() {
				return stack.getHoverName();
			}

			@Override
			public KnowledgeReaderMenu createMenu(int containerId, Inventory inventory, Player openingPlayer) {
				return new KnowledgeReaderMenu(containerId, inventory, sourceSlot);
			}
		});
		return InteractionResult.SUCCESS;
	}

	public ItemStack sourceStack() {
		return this.sourceSlot.getItem();
	}

	public String currentKnowledgeId() {
		return this.currentKnowledgeId;
	}

	public List<String> visiblePages() {
		ItemStack stack = sourceStack();
		if (stack.is(ModItems.KNOWLEDGE_FRAGMENT)) {
			return KnowledgeStackData.fragmentId(stack).map(List::of).orElse(List.of());
		}
		return KnowledgeCatalog.presentIds(KnowledgeStackData.collectionCounts(stack));
	}

	public boolean isCollection() {
		return sourceStack().is(ModItems.KNOWLEDGE_FRAGMENT_COLLECTION);
	}

	public int pageCount(String id) {
		if (!isCollection()) {
			return KnowledgeStackData.fragmentId(sourceStack()).filter(id::equals).isPresent() ? sourceStack().getCount() : 0;
		}
		return KnowledgeStackData.collectionCounts(sourceStack()).getOrDefault(id, 0);
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
			KnowledgeStackData.setBookmark(sourceStack(), this.currentKnowledgeId);
			this.sourceSlot.setChanged();
			this.broadcastChanges();
		}
		return true;
	}

	private boolean extractCurrentPage(Player player) {
		if (!(player instanceof ServerPlayer serverPlayer) || !isCollection()) return false;
		LinkedHashMap<String, Integer> counts = KnowledgeStackData.collectionCounts(sourceStack());
		int currentCount = counts.getOrDefault(this.currentKnowledgeId, 0);
		if (currentCount <= 0) return false;

		if (currentCount == 1) counts.remove(this.currentKnowledgeId);
		else counts.put(this.currentKnowledgeId, currentCount - 1);
		long remainingTotal = KnowledgeStackData.totalCount(counts);
		ItemStack extracted = KnowledgeStackData.fragment(this.currentKnowledgeId);

		if (remainingTotal == 1) {
			String remainingId = KnowledgeCatalog.presentIds(counts).getFirst();
			this.sourceSlot.set(KnowledgeStackData.fragment(remainingId));
			this.sourceSlot.setChanged();
			deliverOrDrop(player, extracted);
			this.broadcastChanges();
			serverPlayer.closeContainer();
			return true;
		}

		if (remainingTotal < 2) return false;
		String nextPage = counts.containsKey(this.currentKnowledgeId)
				? this.currentKnowledgeId
				: KnowledgeStackData.pageAfterRemoval(counts, this.currentKnowledgeId);
		KnowledgeStackData.writeCollection(sourceStack(), counts, nextPage);
		this.currentKnowledgeId = nextPage;
		this.sourceSlot.setChanged();
		deliverOrDrop(player, extracted);
		this.broadcastChanges();
		return true;
	}

	private static void deliverOrDrop(Player player, ItemStack stack) {
		player.getInventory().add(stack);
		if (!stack.isEmpty()) player.drop(stack, false);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int slot) {
		return ItemStack.EMPTY;
	}

	@Override
	public boolean stillValid(Player player) {
		return this.inventory.player == player && isReadable(sourceStack());
	}

	private static boolean isReadable(ItemStack stack) {
		return stack.is(ModItems.KNOWLEDGE_FRAGMENT) || stack.is(ModItems.KNOWLEDGE_FRAGMENT_COLLECTION);
	}

	private static final class LockedSlot extends Slot {
		private LockedSlot(Inventory inventory, int slot, int x, int y) {
			super(inventory, slot, x, y);
		}

		@Override
		public boolean mayPlace(ItemStack stack) {
			return false;
		}

		@Override
		public boolean mayPickup(Player player) {
			return false;
		}
	}
}
