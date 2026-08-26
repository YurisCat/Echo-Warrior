package com.yuriscat.echowarrior.menu;

import com.yuriscat.echowarrior.ModMenus;
import com.yuriscat.echowarrior.item.EchoRelicItem;
import com.yuriscat.echowarrior.item.EchoRelicProgress;
import com.yuriscat.echowarrior.item.EchoRelicState;
import com.yuriscat.echowarrior.item.EchoHeroType;
import com.yuriscat.echowarrior.item.EchoSummonerModule;
import com.yuriscat.echowarrior.item.SummonerFuel;
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
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public final class SummonerMenu extends AbstractContainerMenu {
	public static final int MODULE_SLOT_COUNT = 6;
	public static final int FUEL_SLOT = 6;
	public static final int RELIC_SLOT = 7;
	public static final int CUSTOM_SLOT_COUNT = 8;
	public static final int BUTTON_SUMMON_OR_DISMISS = 0;
	public static final int BUTTON_ACTIVITY_START = 10;
	public static final int BUTTON_ALERT_START = 20;
	public static final int BUTTON_SKILL_START = 30;
	public static final int ACTION_NONE = 0;
	public static final int ACTION_SUMMONED = 1;
	public static final int ACTION_DISMISSED = 2;
	public static final int ACTION_NO_RELIC = 3;
	public static final int ACTION_INVALID_SUMMONER = 4;
	public static final int ACTION_CREATE_FAILED = 5;
	public static final int ACTION_NOT_ENOUGH_FUEL = 6;
	public static final int ACTION_NO_SAFE_POSITION = 7;
	public static final int ACTION_MODE_CHANGED = 8;
	public static final int ACTION_SKILL_CHANGED = 9;
	public static final int ACTION_RELIC_CHANGED = 10;
	public static final int ACTION_FUEL_ROTTEN_FLESH = 11;
	public static final int ACTION_FUEL_SOUL_SAND = 12;
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
	private final DataSlot spiritAttackSpeed = DataSlot.standalone();
	private final DataSlot spiritArmor = DataSlot.standalone();
	private final DataSlot spiritMovement = DataSlot.standalone();
	private final DataSlot summonCostPercent = DataSlot.standalone();
	private final DataSlot fuelAmount = DataSlot.standalone();
	private final DataSlot traitMask = DataSlot.standalone();
	private final DataSlot relicSyncToken = DataSlot.standalone();
	private final DataSlot activityMode = DataSlot.standalone();
	private final DataSlot alertMode = DataSlot.standalone();
	private final DataSlot enabledSkills = DataSlot.standalone();
	private final DataSlot heroType = DataSlot.standalone();
	private final DataSlot skillCount = DataSlot.standalone();
	private final DataSlot egyptianArrowMode = DataSlot.standalone();
	private final DataSlot shieldCharges = DataSlot.standalone();
	private final DataSlot shieldChargeProgress = DataSlot.standalone();
	private final DataSlot legionCooldownTicks = DataSlot.standalone();
	private final DataSlot formationActive = DataSlot.standalone();
	private final DataSlot shieldBondActive = DataSlot.standalone();
	private final DataSlot legionActive = DataSlot.standalone();
	private boolean loadingContents;
	private String previousRelicId;
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
		if (this.owner instanceof ServerPlayer serverPlayer) {
			EchoRelicState.ensureInitialized(this.summonerContainer.getItem(RELIC_SLOT), serverPlayer.getRandom(), serverPlayer.level().getGameTime());
		}
		this.previousRelicId = relicIdentity(this.summonerContainer.getItem(RELIC_SLOT));
		this.addDataSlot(this.spiritPresent);
		this.addDataSlot(this.actionFeedback);
		this.addDataSlot(this.relicLevel);
		this.addDataSlot(this.relicExperience);
		this.addDataSlot(this.relicExperienceNeeded);
		this.addDataSlot(this.spiritHealth);
		this.addDataSlot(this.spiritMaximumHealth);
		this.addDataSlot(this.spiritAttackDamage);
		this.addDataSlot(this.spiritAttackSpeed);
		this.addDataSlot(this.spiritArmor);
		this.addDataSlot(this.spiritMovement);
		this.addDataSlot(this.summonCostPercent);
		this.addDataSlot(this.fuelAmount);
		this.addDataSlot(this.traitMask);
		this.addDataSlot(this.relicSyncToken);
		this.addDataSlot(this.activityMode);
		this.addDataSlot(this.alertMode);
		this.addDataSlot(this.enabledSkills);
		this.addDataSlot(this.heroType);
		this.addDataSlot(this.skillCount);
		this.addDataSlot(this.egyptianArrowMode);
		this.addDataSlot(this.shieldCharges);
		this.addDataSlot(this.shieldChargeProgress);
		this.addDataSlot(this.legionCooldownTicks);
		this.addDataSlot(this.formationActive);
		this.addDataSlot(this.shieldBondActive);
		this.addDataSlot(this.legionActive);

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
		ItemStack relic = this.summonerContainer.getItem(RELIC_SLOT);
		if (this.owner instanceof ServerPlayer serverPlayer) {
			EchoRelicState.ensureInitialized(relic, serverPlayer.getRandom(), serverPlayer.level().getGameTime());
		}
		currentSummoner.set(
				DataComponents.CONTAINER,
				ItemContainerContents.fromItems(this.summonerContainer.getItems())
		);
		String currentRelicId = relicIdentity(relic);
		if (!this.previousRelicId.isEmpty() && !this.previousRelicId.equals(currentRelicId)
				&& this.owner instanceof ServerPlayer serverPlayer) {
			TestEchoSummonerItem.dismissBoundSpirit(serverPlayer, currentSummoner);
			reportAction(ACTION_RELIC_CHANGED);
		}
		this.previousRelicId = currentRelicId;
	}

	@Override
	public void broadcastChanges() {
		if (this.owner instanceof ServerPlayer serverPlayer) {
			tickFuelConversion(serverPlayer);
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
			EchoRelicState.ensureInitialized(relic, serverPlayer.getRandom(), serverPlayer.level().getGameTime());
			int level = EchoRelicProgress.level(relic);
			int maximumHealth = (int)Math.round(EchoRelicState.maximumHealth(relic) * 10.0);
			this.relicLevel.set(level);
			this.relicExperience.set(EchoRelicProgress.experience(relic));
			this.relicExperienceNeeded.set(EchoRelicProgress.experienceNeeded(level));
			this.spiritMaximumHealth.set(maximumHealth);
			this.spiritAttackDamage.set((int)Math.round(EchoRelicState.attackDamage(relic) * 10.0));
			this.spiritHealth.set(spirit == null ? maximumHealth : Math.round(spirit.livingEntity().getHealth() * 10.0F));
			this.spiritAttackSpeed.set(EchoRelicState.attackSpeedPercent(relic));
			this.spiritArmor.set((int)Math.round(EchoRelicState.armor(relic) * 10.0));
			this.spiritMovement.set(EchoRelicState.movementPercent(relic));
			this.summonCostPercent.set(EchoRelicState.summonCostPercent(relic));
			this.traitMask.set(EchoRelicState.traitMask(relic));
			this.relicSyncToken.set(relicSyncToken(relic));
			this.activityMode.set(EchoRelicState.activityMode(relic).ordinal());
			this.alertMode.set(EchoRelicState.alertMode(relic).ordinal());
			this.enabledSkills.set(EchoRelicState.enabledSkills(relic));
			EchoHeroType currentHero = EchoHeroType.fromRelic(relic);
			this.heroType.set(currentHero.ordinal());
			this.skillCount.set(currentHero.skillCount());
			this.egyptianArrowMode.set(EchoRelicState.egyptianArrowMode(relic).ordinal());
			this.shieldCharges.set(EchoRelicState.activeSkillCharges(relic, serverPlayer.level().getGameTime()));
			this.shieldChargeProgress.set(EchoRelicState.activeSkillChargeProgress(relic, serverPlayer.level().getGameTime()));
			if (!summoner.isEmpty()) {
				summoner.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(this.summonerContainer.getItems()));
			}
			long auxiliaryCooldown = currentHero == EchoHeroType.JAPANESE_SAMURAI
					? EchoRelicState.samuraiStabCooldownEnd(relic) - serverPlayer.level().getGameTime()
					: EchoRelicState.legionCooldownEnd(relic) - serverPlayer.level().getGameTime();
			this.legionCooldownTicks.set((int)Math.clamp(auxiliaryCooldown, 0L,
					currentHero == EchoHeroType.JAPANESE_SAMURAI ? 200L : 400L));
			this.formationActive.set(spirit != null && spirit.isFormationActive() ? 1 : 0);
			this.shieldBondActive.set(spirit != null && spirit.isShieldBondActive() ? 1 : 0);
			this.legionActive.set(spirit != null && spirit.isLegionEnduresActive() ? 1 : 0);
		} else {
			this.relicLevel.set(0);
			this.relicExperience.set(0);
			this.relicExperienceNeeded.set(0);
			this.spiritHealth.set(0);
			this.spiritMaximumHealth.set(0);
			this.spiritAttackDamage.set(0);
			this.spiritAttackSpeed.set(0);
			this.spiritArmor.set(0);
			this.spiritMovement.set(0);
			this.summonCostPercent.set(100);
			this.traitMask.set(0);
			this.relicSyncToken.set(0);
			this.activityMode.set(0);
			this.alertMode.set(1);
			this.enabledSkills.set(EchoRelicState.ALL_SKILLS_ENABLED);
			this.heroType.set(0);
			this.skillCount.set(0);
			this.egyptianArrowMode.set(0);
			this.shieldCharges.set(0);
			this.shieldChargeProgress.set(0);
			this.legionCooldownTicks.set(0);
			this.formationActive.set(0);
			this.shieldBondActive.set(0);
			this.legionActive.set(0);
		}
		this.fuelAmount.set(summoner.isEmpty() ? 0 : SummonerFuel.amount(summoner));
	}

	private void tickFuelConversion(ServerPlayer player) {
		if (player.tickCount % 5 != 0) {
			return;
		}
		ItemStack summoner = currentSummonerStack();
		ItemStack input = this.summonerContainer.getItem(FUEL_SLOT);
		int value = SummonerFuel.value(input);
		if (summoner.isEmpty() || value <= 0 || SummonerFuel.amount(summoner) + value > SummonerFuel.CAPACITY) {
			return;
		}
		boolean soulSand = input.is(net.minecraft.world.item.Items.SOUL_SAND)
				|| input.is(net.minecraft.world.item.Items.SOUL_SOIL);
		input.shrink(1);
		SummonerFuel.setAmount(summoner, SummonerFuel.amount(summoner) + value);
		this.summonerContainer.setChanged();
		reportAction(soulSand ? ACTION_FUEL_SOUL_SAND : ACTION_FUEL_ROTTEN_FLESH);
	}

	@Override
	public boolean clickMenuButton(Player player, int buttonId) {
		if (!(player instanceof ServerPlayer serverPlayer) || player != this.owner) {
			return false;
		}

		ItemStack summoner = currentSummonerStack();
		if (summoner.isEmpty()) {
			reportAction(ACTION_INVALID_SUMMONER);
			return true;
		}
		ItemStack relic = this.summonerContainer.getItem(RELIC_SLOT);
		if (!isRelic(relic)) {
			reportAction(ACTION_NO_RELIC);
			return true;
		}
		if (buttonId >= BUTTON_ACTIVITY_START && buttonId < BUTTON_ACTIVITY_START + 3) {
			EchoRelicState.setActivityMode(relic, EchoRelicState.ActivityMode.byOrdinal(buttonId - BUTTON_ACTIVITY_START));
			var spirit = TestEchoSummonerItem.findBoundSpirit(serverPlayer.level(), summoner);
			if (spirit != null) spirit.applyRelicState(relic, true);
			this.summonerContainer.setChanged();
			reportAction(ACTION_MODE_CHANGED);
			return true;
		}
		if (buttonId >= BUTTON_ALERT_START && buttonId < BUTTON_ALERT_START + 3) {
			EchoRelicState.setAlertMode(relic, EchoRelicState.AlertMode.byOrdinal(buttonId - BUTTON_ALERT_START));
			var spirit = TestEchoSummonerItem.findBoundSpirit(serverPlayer.level(), summoner);
			if (spirit != null) spirit.applyRelicState(relic, false);
			this.summonerContainer.setChanged();
			reportAction(ACTION_MODE_CHANGED);
			return true;
		}
		if (buttonId >= BUTTON_SKILL_START && buttonId < BUTTON_SKILL_START + EchoHeroType.fromRelic(relic).skillCount()) {
			int skill = buttonId - BUTTON_SKILL_START;
			if (EchoHeroType.fromRelic(relic) == EchoHeroType.EGYPTIAN_ARCHER && skill == 1) {
				if (!EchoRelicState.cycleEgyptianArrowMode(relic, serverPlayer.level().getGameTime())) return true;
			} else {
				EchoRelicState.toggleSkill(relic, skill);
			}
			var spirit = TestEchoSummonerItem.findBoundSpirit(serverPlayer.level(), summoner);
			if (spirit != null) spirit.applyRelicState(relic, false);
			this.summonerContainer.setChanged();
			reportAction(ACTION_SKILL_CHANGED);
			return true;
		}
		if (buttonId != BUTTON_SUMMON_OR_DISMISS) {
			return false;
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
			case NO_RELIC -> reportAction(ACTION_NO_RELIC);
			case NOT_ENOUGH_FUEL -> reportAction(ACTION_NOT_ENOUGH_FUEL);
			case NO_SAFE_POSITION -> reportAction(ACTION_NO_SAFE_POSITION);
		}
		return true;
	}

	private void reportAction(int action) {
		this.actionSequence = (this.actionSequence + 1) & 0x07FFFFFF;
		this.actionFeedback.set((this.actionSequence << 8) | (action & 0xFF));
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

	public int spiritAttackSpeed() { return this.spiritAttackSpeed.get(); }
	public int spiritArmor() { return this.spiritArmor.get(); }
	public int spiritMovement() { return this.spiritMovement.get(); }
	public int summonCostPercent() { return this.summonCostPercent.get(); }
	public int fuelAmount() { return this.fuelAmount.get(); }
	public int traitMask() { return this.traitMask.get(); }
	public int relicSyncToken() { return this.relicSyncToken.get(); }
	public int activityMode() { return this.activityMode.get(); }
	public int alertMode() { return this.alertMode.get(); }
	public int enabledSkills() { return this.enabledSkills.get(); }
	public int heroType() { return this.heroType.get(); }
	public int skillCount() { return this.skillCount.get(); }
	public int egyptianArrowMode() { return this.egyptianArrowMode.get(); }
	public int shieldCharges() { return this.shieldCharges.get(); }
	public int shieldChargeProgress() { return this.shieldChargeProgress.get(); }
	public int legionCooldownTicks() { return this.legionCooldownTicks.get(); }
	public boolean formationActive() { return this.formationActive.get() != 0; }
	public boolean shieldBondActive() { return this.shieldBondActive.get() != 0; }
	public boolean legionActive() { return this.legionActive.get() != 0; }

	private static boolean isFuel(ItemStack stack) {
		return SummonerFuel.isFuel(stack);
	}

	private static String relicIdentity(ItemStack stack) {
		return isRelic(stack) ? EchoRelicState.relicId(stack) : "";
	}

	public static int relicSyncToken(ItemStack stack) {
		String id = relicIdentity(stack);
		return id.isEmpty() ? 0 : (id.hashCode() & 0x7FFF) + 1;
	}

	private static boolean isRelic(ItemStack stack) {
		return stack.getItem() instanceof EchoRelicItem;
	}

	private final class ModuleSlot extends Slot {
		private final int moduleSlot;

		private ModuleSlot(SimpleContainer container, int slot, int x, int y) {
			super(container, slot, x, y);
			this.moduleSlot = slot;
		}

		@Override
		public boolean mayPlace(ItemStack stack) {
			return EchoSummonerModule.canInstall(stack, currentSummonerStack(), this.moduleSlot, summonerContainer);
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
