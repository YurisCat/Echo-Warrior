package com.yuriscat.echowarrior.menu;

import com.yuriscat.echowarrior.ModMenus;
import com.yuriscat.echowarrior.item.EchoRelicItem;
import com.yuriscat.echowarrior.item.EchoRelicProgress;
import com.yuriscat.echowarrior.item.TestEchoSummonerItem;
import com.yuriscat.echowarrior.layout.SummonerLayout;
import com.yuriscat.echowarrior.layout.SummonerLayout.Element;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public final class SummonerMenu extends AbstractContainerMenu {
	public static final int MODULE_SLOT_COUNT = 6;
	public static final int FUEL_SLOT = 6;
	public static final int RELIC_SLOT = 7;
	public static final int CUSTOM_SLOT_COUNT = 8;
	public static final int BUTTON_SUMMON_OR_DISMISS = 0;
	public static final int ACTION_NONE = 0;
	public static final int ACTION_SUMMONED = 1;
	public static final int ACTION_DISMISSED = 2;
	public static final int ACTION_NO_RELIC = 3;
	public static final int ACTION_INVALID_SUMMONER = 4;
	public static final int ACTION_CREATE_FAILED = 5;
	private static final int PLAYER_SLOT_START = CUSTOM_SLOT_COUNT;
	private static final int PLAYER_SLOT_END = PLAYER_SLOT_START + Inventory.INVENTORY_SIZE;

	private static final int[] MODULE_SLOT_X = {8, 37, 66, 94, 123, 152};
	private static final int MODULE_SLOT_Y = 94;
	private static final int FUEL_SLOT_X = 179;
	private static final int RELIC_SLOT_X = 217;
	private static final int BOTTOM_SLOT_Y = 172;

	private final Player owner;
	private final int sourceInventorySlot;
	private final UUID summonerId;
	private final SimpleContainer summonerContainer;
	private final DataSlot spiritPresent = DataSlot.standalone();
	private final DataSlot actionFeedback = DataSlot.standalone();
	private final DataSlot relicLevel = DataSlot.standalone();
	private final DataSlot relicExperience = DataSlot.standalone();
	private final DataSlot relicExperienceNeeded = DataSlot.standalone();
	private final DataSlot spiritHealth = DataSlot.standalone();
	private final DataSlot spiritMaximumHealth = DataSlot.standalone();
	private final DataSlot spiritAttackDamage = DataSlot.standalone();
	private boolean loadingContents;
	private boolean relicWasLoaded;
	private int actionSequence;

	public SummonerMenu(int containerId, Inventory playerInventory, Integer sourceInventorySlot) {
		this(containerId, playerInventory, sourceInventorySlot, stackAt(playerInventory, sourceInventorySlot));
	}

	public SummonerMenu(
			int containerId,
			Inventory playerInventory,
			int sourceInventorySlot,
			ItemStack summonerStack
	) {
		super(ModMenus.SUMMONER, containerId);
		this.owner = playerInventory.player;
		this.sourceInventorySlot = sourceInventorySlot;
		this.summonerId = TestEchoSummonerItem.getSummonerId(summonerStack).orElse(null);
		this.summonerContainer = new SimpleContainer(CUSTOM_SLOT_COUNT) {
			@Override
			public void setChanged() {
				super.setChanged();
				SummonerMenu.this.saveContents();
			}
		};

		this.loadingContents = true;
		summonerStack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY)
				.copyInto(this.summonerContainer.getItems());
		this.loadingContents = false;
		this.relicWasLoaded = isRelic(this.summonerContainer.getItem(RELIC_SLOT));
		this.addDataSlot(this.spiritPresent);
		this.addDataSlot(this.actionFeedback);
		this.addDataSlot(this.relicLevel);
		this.addDataSlot(this.relicExperience);
		this.addDataSlot(this.relicExperienceNeeded);
		this.addDataSlot(this.spiritHealth);
		this.addDataSlot(this.spiritMaximumHealth);
		this.addDataSlot(this.spiritAttackDamage);

		for (int index = 0; index < MODULE_SLOT_COUNT; index++) {
			this.addSlot(new ModuleSlot(
					this.summonerContainer,
					index,
					layoutX(Element.MODULES, MODULE_SLOT_X[index]),
					layoutY(Element.MODULES, MODULE_SLOT_Y)
			));
		}
		this.addSlot(new FuelSlot(
				this.summonerContainer,
				FUEL_SLOT,
				layoutX(Element.FUEL_SLOT, FUEL_SLOT_X),
				layoutY(Element.FUEL_SLOT, BOTTOM_SLOT_Y)
		));
		this.addSlot(new RelicSlot(
				this.summonerContainer,
				RELIC_SLOT,
				layoutX(Element.RELIC_SLOT, RELIC_SLOT_X),
				layoutY(Element.RELIC_SLOT, BOTTOM_SLOT_Y)
		));

		addPlayerInventorySlots(playerInventory);
		if (this.owner instanceof ServerPlayer serverPlayer) {
			refreshServerData(serverPlayer);
		}
	}

	private static ItemStack stackAt(Inventory inventory, int slot) {
		if (slot < 0 || slot >= inventory.getContainerSize()) {
			return ItemStack.EMPTY;
		}
		return inventory.getItem(slot);
	}

	private void addPlayerInventorySlots(Inventory inventory) {
		for (int row = 0; row < 3; row++) {
			for (int column = 0; column < 9; column++) {
				int inventorySlot = 9 + row * 9 + column;
				this.addSlot(playerSlot(
						inventory,
						inventorySlot,
						layoutX(Element.PLAYER_INVENTORY, 8 + column * 18),
						layoutY(Element.PLAYER_INVENTORY, 120 + row * 18)
				));
			}
		}

		for (int column = 0; column < 9; column++) {
			this.addSlot(playerSlot(
					inventory,
					column,
					layoutX(Element.PLAYER_INVENTORY, 8 + column * 18),
					layoutY(Element.PLAYER_INVENTORY, 177)
			));
		}
	}

	private static int layoutX(Element element, int baseX) {
		return SummonerLayout.get().x(element, baseX);
	}

	private static int layoutY(Element element, int baseY) {
		return SummonerLayout.get().y(element, baseY);
	}

	private Slot playerSlot(Inventory inventory, int inventorySlot, int x, int y) {
		if (inventorySlot == this.sourceInventorySlot) {
			return new LockedSlot(inventory, inventorySlot, x, y);
		}
		return new Slot(inventory, inventorySlot, x, y);
	}

	private void saveContents() {
		if (this.loadingContents || this.owner.level().isClientSide()) {
			return;
		}
		ItemStack currentSummoner = currentSummonerStack();
		if (currentSummoner.isEmpty()) {
			return;
		}
		currentSummoner.set(
				DataComponents.CONTAINER,
				ItemContainerContents.fromItems(this.summonerContainer.getItems())
		);
		boolean relicLoaded = isRelic(this.summonerContainer.getItem(RELIC_SLOT));
		if (this.relicWasLoaded && !relicLoaded && this.owner instanceof ServerPlayer serverPlayer) {
			TestEchoSummonerItem.dismissBoundSpirit(serverPlayer, currentSummoner);
			reportAction(ACTION_DISMISSED);
		}
		this.relicWasLoaded = relicLoaded;
	}

	@Override
	public void broadcastChanges() {
		if (this.owner instanceof ServerPlayer serverPlayer) {
			refreshServerData(serverPlayer);
		}
		super.broadcastChanges();
	}

	private void refreshServerData(ServerPlayer serverPlayer) {
		ItemStack summoner = currentSummonerStack();
		var spirit = summoner.isEmpty() ? null : TestEchoSummonerItem.findBoundSpirit(serverPlayer.level(), summoner);
		this.spiritPresent.set(spirit != null ? 1 : 0);
		ItemStack relic = this.summonerContainer.getItem(RELIC_SLOT);
		if (relic.getItem() instanceof EchoRelicItem) {
			int level = EchoRelicProgress.level(relic);
			int maximumHealth = (int)Math.round(EchoRelicProgress.maximumHealth(level));
			this.relicLevel.set(level);
			this.relicExperience.set(EchoRelicProgress.experience(relic));
			this.relicExperienceNeeded.set(EchoRelicProgress.experienceNeeded(level));
			this.spiritMaximumHealth.set(maximumHealth);
			this.spiritAttackDamage.set((int)Math.round(EchoRelicProgress.attackDamage(level)));
			this.spiritHealth.set(spirit == null ? maximumHealth : Math.round(spirit.getHealth()));
		} else {
			this.relicLevel.set(0);
			this.relicExperience.set(0);
			this.relicExperienceNeeded.set(0);
			this.spiritHealth.set(0);
			this.spiritMaximumHealth.set(0);
			this.spiritAttackDamage.set(0);
		}
	}

	@Override
	public boolean clickMenuButton(Player player, int buttonId) {
		if (buttonId != BUTTON_SUMMON_OR_DISMISS || !(player instanceof ServerPlayer serverPlayer) || player != this.owner) {
			return false;
		}

		ItemStack summoner = currentSummonerStack();
		if (summoner.isEmpty()) {
			reportAction(ACTION_INVALID_SUMMONER);
			return true;
		}
		if (!isRelic(this.summonerContainer.getItem(RELIC_SLOT))) {
			reportAction(ACTION_NO_RELIC);
			return true;
		}

		if (TestEchoSummonerItem.hasBoundSpirit(serverPlayer.level(), summoner)) {
			if (TestEchoSummonerItem.dismissBoundSpirit(serverPlayer, summoner)) {
				this.spiritPresent.set(0);
				reportAction(ACTION_DISMISSED);
			} else {
				reportAction(ACTION_INVALID_SUMMONER);
			}
			return true;
		}

		TestEchoSummonerItem.SummonResult result = TestEchoSummonerItem.summonFromMenu(serverPlayer, summoner);
		switch (result) {
			case SUMMONED -> {
				this.spiritPresent.set(1);
				reportAction(ACTION_SUMMONED);
			}
			case ALREADY_PRESENT -> this.spiritPresent.set(1);
			case INVALID_SUMMONER -> reportAction(ACTION_INVALID_SUMMONER);
			case CREATE_FAILED -> reportAction(ACTION_CREATE_FAILED);
		}
		return true;
	}

	private void reportAction(int action) {
		this.actionSequence = (this.actionSequence + 1) & 0x07FFFFFF;
		this.actionFeedback.set((this.actionSequence << 4) | (action & 0xF));
	}

	private boolean isOriginalSummonerPresent() {
		return !currentSummonerStack().isEmpty();
	}

	private ItemStack currentSummonerStack() {
		if (this.summonerId == null) {
			return ItemStack.EMPTY;
		}
		ItemStack current = stackAt(this.owner.getInventory(), this.sourceInventorySlot);
		return TestEchoSummonerItem.hasSummoner(current, this.summonerId) ? current : ItemStack.EMPTY;
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		Slot slot = this.slots.get(index);
		if (!slot.hasItem() || !slot.mayPickup(player)) {
			return ItemStack.EMPTY;
		}

		ItemStack stack = slot.getItem();
		ItemStack original = stack.copy();
		if (index < CUSTOM_SLOT_COUNT) {
			if (!this.moveItemStackTo(stack, PLAYER_SLOT_START, PLAYER_SLOT_END, true)) {
				return ItemStack.EMPTY;
			}
		} else if (isFuel(stack)) {
			if (!this.moveItemStackTo(stack, FUEL_SLOT, FUEL_SLOT + 1, false)
					&& !this.moveItemStackTo(stack, 0, MODULE_SLOT_COUNT, false)) {
				return ItemStack.EMPTY;
			}
		} else if (isRelic(stack)) {
			if (!this.moveItemStackTo(stack, RELIC_SLOT, RELIC_SLOT + 1, false)) {
				return ItemStack.EMPTY;
			}
		} else if (!this.moveItemStackTo(stack, 0, MODULE_SLOT_COUNT, false)) {
			return ItemStack.EMPTY;
		}

		if (stack.isEmpty()) {
			slot.setByPlayer(ItemStack.EMPTY);
		} else {
			slot.setChanged();
		}
		if (stack.getCount() == original.getCount()) {
			return ItemStack.EMPTY;
		}
		slot.onTake(player, stack);
		return original;
	}

	@Override
	public boolean stillValid(Player player) {
		if (player.level().isClientSide()) {
			return stackAt(player.getInventory(), this.sourceInventorySlot).getItem() instanceof TestEchoSummonerItem;
		}
		return player == this.owner && isOriginalSummonerPresent();
	}

	@Override
	public void removed(Player player) {
		saveContents();
		super.removed(player);
	}

	public int sourceInventorySlot() {
		return this.sourceInventorySlot;
	}

	public SimpleContainer summonerContainer() {
		return this.summonerContainer;
	}

	public boolean matchesSummoner(UUID id) {
		return id != null && id.equals(this.summonerId);
	}

	public ItemStack relicStackForProgress() {
		return this.summonerContainer.getItem(RELIC_SLOT);
	}

	public void markRelicProgressChanged() {
		this.summonerContainer.setChanged();
	}

	public boolean isSpiritPresent() {
		return this.spiritPresent.get() != 0;
	}

	public int actionFeedbackValue() {
		return this.actionFeedback.get();
	}

	public int relicLevel() {
		return this.relicLevel.get();
	}

	public int relicExperience() {
		return this.relicExperience.get();
	}

	public int relicExperienceNeeded() {
		return this.relicExperienceNeeded.get();
	}

	public int spiritHealth() {
		return this.spiritHealth.get();
	}

	public int spiritMaximumHealth() {
		return this.spiritMaximumHealth.get();
	}

	public int spiritAttackDamage() {
		return this.spiritAttackDamage.get();
	}

	private static boolean isFuel(ItemStack stack) {
		return stack.is(Items.ROTTEN_FLESH) || stack.is(Items.SOUL_SAND);
	}

	private static boolean isRelic(ItemStack stack) {
		return stack.getItem() instanceof EchoRelicItem;
	}

	private static final class ModuleSlot extends Slot {
		private ModuleSlot(SimpleContainer container, int slot, int x, int y) {
			super(container, slot, x, y);
		}

		@Override
		public boolean mayPlace(ItemStack stack) {
			return !isFuel(stack) && !isRelic(stack);
		}
	}

	private static final class FuelSlot extends Slot {
		private FuelSlot(SimpleContainer container, int slot, int x, int y) {
			super(container, slot, x, y);
		}

		@Override
		public boolean mayPlace(ItemStack stack) {
			return isFuel(stack);
		}
	}

	private static final class RelicSlot extends Slot {
		private RelicSlot(SimpleContainer container, int slot, int x, int y) {
			super(container, slot, x, y);
		}

		@Override
		public boolean mayPlace(ItemStack stack) {
			return isRelic(stack);
		}
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
