package com.yuriscat.echowarrior.menu;

import com.yuriscat.echowarrior.ModItems;
import com.yuriscat.echowarrior.ModMenus;
import com.yuriscat.echowarrior.tutorial.TutorialManualCatalog;
import com.yuriscat.echowarrior.tutorial.TutorialManualStackData;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class TutorialManualMenu extends AbstractContainerMenu {
	public static final int BUTTON_PREVIOUS = 0;
	public static final int BUTTON_NEXT = 1;
	public static final int BUTTON_JUMP_START = 100;

	private final Inventory inventory;
	private final Slot sourceSlot;
	private final DataSlot currentPage = DataSlot.standalone();

	public TutorialManualMenu(int containerId, Inventory inventory, Integer sourceInventorySlot) {
		super(ModMenus.TUTORIAL_MANUAL, containerId);
		this.inventory = inventory;
		this.sourceSlot = this.addSlot(new LockedSlot(inventory, sourceInventorySlot, -1000, -1000));
		this.currentPage.set(TutorialManualStackData.bookmark(this.sourceSlot.getItem()));
		this.addDataSlot(this.currentPage);
	}

	public static InteractionResult open(Player player, InteractionHand hand) {
		if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.SUCCESS;
		int sourceSlot = hand == InteractionHand.MAIN_HAND
				? player.getInventory().getSelectedSlot()
				: Inventory.SLOT_OFFHAND;
		ItemStack stack = player.getItemInHand(hand);
		if (!stack.is(ModItems.TUTORIAL_MANUAL)) return InteractionResult.FAIL;
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
			public TutorialManualMenu createMenu(int containerId, Inventory inventory, Player openingPlayer) {
				return new TutorialManualMenu(containerId, inventory, sourceSlot);
			}
		});
		return InteractionResult.SUCCESS;
	}

	public int currentPage() {
		return Math.clamp(this.currentPage.get(), 0, TutorialManualCatalog.pageCount() - 1);
	}

	public void selectClientPage(int page) {
		this.currentPage.set(Math.clamp(page, 0, TutorialManualCatalog.pageCount() - 1));
	}

	@Override
	public boolean clickMenuButton(Player player, int buttonId) {
		int desired = switch (buttonId) {
			case BUTTON_PREVIOUS -> currentPage() - 1;
			case BUTTON_NEXT -> currentPage() + 1;
			default -> buttonId >= BUTTON_JUMP_START ? buttonId - BUTTON_JUMP_START : -1;
		};
		if (desired < 0 || desired >= TutorialManualCatalog.pageCount()) return false;
		this.currentPage.set(desired);
		TutorialManualStackData.setBookmark(this.sourceSlot.getItem(), desired);
		this.sourceSlot.setChanged();
		this.broadcastChanges();
		return true;
	}

	@Override
	public ItemStack quickMoveStack(Player player, int slot) {
		return ItemStack.EMPTY;
	}

	@Override
	public boolean stillValid(Player player) {
		return this.inventory.player == player && this.sourceSlot.getItem().is(ModItems.TUTORIAL_MANUAL);
	}

	private static final class LockedSlot extends Slot {
		private LockedSlot(Inventory inventory, int index, int x, int y) {
			super(inventory, index, x, y);
		}

		@Override
		public boolean mayPickup(Player player) {
			return false;
		}

		@Override
		public boolean mayPlace(ItemStack stack) {
			return false;
		}
	}
}
