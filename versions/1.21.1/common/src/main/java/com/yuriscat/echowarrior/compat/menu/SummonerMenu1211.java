package com.yuriscat.echowarrior.compat.menu;

import com.yuriscat.echowarrior.compat.EchoHeroType1211;
import com.yuriscat.echowarrior.compat.ModContent1211;
import com.yuriscat.echowarrior.compat.binding.EchoBindingSavedData1211;
import com.yuriscat.echowarrior.compat.binding.EchoBindingSystem1211;
import com.yuriscat.echowarrior.compat.item.EchoRelicItem1211;
import com.yuriscat.echowarrior.compat.item.EchoAccessorySystem1211;
import com.yuriscat.echowarrior.compat.item.EchoSummonerAccessory1211;
import com.yuriscat.echowarrior.compat.item.EchoRelicProgress1211;
import com.yuriscat.echowarrior.compat.item.EchoRelicState1211;
import com.yuriscat.echowarrior.compat.item.EchoSummonerItem1211;
import com.yuriscat.echowarrior.compat.item.SummonerFuel1211;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

import java.util.UUID;

public final class SummonerMenu1211 extends AbstractContainerMenu {
    public static final int MODULE_SLOT_COUNT = 6;
    public static final int FUEL_SLOT = 6;
    public static final int RELIC_SLOT = 7;
    public static final int CUSTOM_SLOT_COUNT = 8;
    public static final int BUTTON_SUMMON_OR_DISMISS = 0;
    public static final int BUTTON_ACTIVITY_START = 10;
    public static final int BUTTON_ALERT_START = 20;
    public static final int BUTTON_SKILL_START = 30;
    public static final int ACTION_SUMMONED = 1;
    public static final int ACTION_DISMISSED = 2;
    public static final int ACTION_NO_RELIC = 3;
    public static final int ACTION_INVALID_SUMMONER = 4;
    public static final int ACTION_CREATE_FAILED = 5;
    public static final int ACTION_NOT_ENOUGH_FUEL = 6;
    public static final int ACTION_NO_SAFE_POSITION = 7;
    public static final int ACTION_MODE_CHANGED = 8;
    public static final int ACTION_SKILL_CHANGED = 9;
    public static final int ACTION_FUEL_ROTTEN_FLESH = 11;
    public static final int ACTION_FUEL_SOUL_SAND = 12;
    public static final int ACTION_LIMIT_REACHED = 13;
    public static final int ACTION_STALE_STATE = 14;
    private static final int PLAYER_SLOT_START = CUSTOM_SLOT_COUNT;
    private static final int PLAYER_SLOT_END = PLAYER_SLOT_START + Inventory.INVENTORY_SIZE;

    private final Player owner;
    private final int sourceInventorySlot;
    private final UUID summonerId;
    private final SimpleContainer summonerContainer;
    private final DataSlot fuelAmount = DataSlot.standalone();
    private final DataSlot spiritPresent = DataSlot.standalone();
    private final DataSlot actionFeedback = DataSlot.standalone();
    private final DataSlot spiritHealth = DataSlot.standalone();
    private final DataSlot spiritMaximumHealth = DataSlot.standalone();
    private final DataSlot activityMode = DataSlot.standalone();
    private final DataSlot alertMode = DataSlot.standalone();
    private final DataSlot enabledSkills = DataSlot.standalone();
    private final DataSlot shieldCharges = DataSlot.standalone();
    private final DataSlot activeSkillMaximumCharges = DataSlot.standalone();
    private final DataSlot shieldChargeProgress = DataSlot.standalone();
    private final DataSlot legionCooldownTicks = DataSlot.standalone();
    private final DataSlot legionActive = DataSlot.standalone();
    private final DataSlot formationActive = DataSlot.standalone();
    private final DataSlot relicLevel = DataSlot.standalone();
    private final DataSlot relicExperience = DataSlot.standalone();
    private final DataSlot relicExperienceNeeded = DataSlot.standalone();
    private final DataSlot spiritAttackDamage = DataSlot.standalone();
    private final DataSlot spiritArmor = DataSlot.standalone();
    private final DataSlot spiritMovement = DataSlot.standalone();
    private final DataSlot spiritAttackSpeed = DataSlot.standalone();
    private final DataSlot summonCostPercent = DataSlot.standalone();
    private final DataSlot spiritAlertRange = DataSlot.standalone();
    private final DataSlot traitMaskLow = DataSlot.standalone();
    private final DataSlot traitMaskHigh = DataSlot.standalone();
    private final DataSlot biomeAffinity = DataSlot.standalone();
    private final DataSlot heroType = DataSlot.standalone();
    private final DataSlot skillCount = DataSlot.standalone();
    private final DataSlot egyptianArrowMode = DataSlot.standalone();
    private boolean loading;
    private int actionSequence;
    private long observedStateRevision = -1L;

    public SummonerMenu1211(int containerId, Inventory inventory) {
        this(containerId, inventory, -1, ItemStack.EMPTY);
    }

    public SummonerMenu1211(int containerId, Inventory inventory, int sourceInventorySlot, ItemStack summoner) {
        super(ModContent1211.SUMMONER_MENU, containerId);
        this.owner = inventory.player;
        this.sourceInventorySlot = sourceInventorySlot;
        this.summonerId = summoner.is(ModContent1211.ECHO_SUMMONER)
                ? EchoSummonerItem1211.getOrCreateSummonerId(summoner)
                : null;
        if (this.owner instanceof ServerPlayer player && this.summonerId != null) {
            this.observedStateRevision = EchoBindingSystem1211.synchronize(
                    (net.minecraft.server.level.ServerLevel)player.level(), summoner).stateRevision();
        }
        this.summonerContainer = new SimpleContainer(CUSTOM_SLOT_COUNT) {
            @Override
            public void setChanged() {
                super.setChanged();
                SummonerMenu1211.this.saveContents();
            }
        };
        this.loading = true;
        summoner.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY)
                .copyInto(this.summonerContainer.getItems());
        this.loading = false;

        for (int index = 0; index < MODULE_SLOT_COUNT; index++) {
            int accessorySlot = index;
            this.addSlot(new Slot(this.summonerContainer, index, 8 + index * 29, 94) {
                @Override public boolean mayPlace(ItemStack stack) {
                    return EchoSummonerAccessory1211.canInstall(
                            stack, SummonerMenu1211.this.sourceStack(), accessorySlot,
                            SummonerMenu1211.this.summonerContainer);
                }
                @Override public int getMaxStackSize() { return 1; }
            });
        }
        this.addSlot(new Slot(this.summonerContainer, FUEL_SLOT, 179, 172) {
            @Override public boolean mayPlace(ItemStack stack) { return SummonerFuel1211.isFuel(stack); }
        });
        this.addSlot(new Slot(this.summonerContainer, RELIC_SLOT, 217, 172) {
            @Override public boolean mayPlace(ItemStack stack) { return stack.getItem() instanceof EchoRelicItem1211; }
            @Override public int getMaxStackSize() { return 1; }
        });

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                int inventorySlot = 9 + row * 9 + column;
                this.addSlot(playerSlot(inventory, inventorySlot, 8 + column * 18, 120 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            this.addSlot(playerSlot(inventory, column, 8 + column * 18, 177));
        }

        this.addDataSlot(this.fuelAmount);
        this.addDataSlot(this.spiritPresent);
        this.addDataSlot(this.actionFeedback);
        this.addDataSlot(this.spiritHealth);
        this.addDataSlot(this.spiritMaximumHealth);
        this.addDataSlot(this.activityMode);
        this.addDataSlot(this.alertMode);
        this.addDataSlot(this.enabledSkills);
        this.addDataSlot(this.shieldCharges);
        this.addDataSlot(this.activeSkillMaximumCharges);
        this.addDataSlot(this.shieldChargeProgress);
        this.addDataSlot(this.legionCooldownTicks);
        this.addDataSlot(this.legionActive);
        this.addDataSlot(this.formationActive);
        this.addDataSlot(this.relicLevel);
        this.addDataSlot(this.relicExperience);
        this.addDataSlot(this.relicExperienceNeeded);
        this.addDataSlot(this.spiritAttackDamage);
        this.addDataSlot(this.spiritArmor);
        this.addDataSlot(this.spiritMovement);
        this.addDataSlot(this.spiritAttackSpeed);
        this.addDataSlot(this.summonCostPercent);
        this.addDataSlot(this.spiritAlertRange);
        this.addDataSlot(this.traitMaskLow);
        this.addDataSlot(this.traitMaskHigh);
        this.addDataSlot(this.biomeAffinity);
        this.addDataSlot(this.heroType);
        this.addDataSlot(this.skillCount);
        this.addDataSlot(this.egyptianArrowMode);
        refreshData();
    }

    private Slot playerSlot(Inventory inventory, int inventorySlot, int x, int y) {
        return inventorySlot == this.sourceInventorySlot
                ? new LockedSlot(inventory, inventorySlot, x, y)
                : new Slot(inventory, inventorySlot, x, y);
    }

    public boolean matches(ItemStack stack) {
        return this.summonerId != null && this.summonerId.equals(EchoSummonerItem1211.getSummonerId(stack));
    }

    public int fuelAmount() { return this.fuelAmount.get(); }
    public boolean spiritPresent() { return this.spiritPresent.get() != 0; }
    public int actionFeedbackValue() { return this.actionFeedback.get(); }
    public int spiritHealth() { return this.spiritHealth.get(); }
    public int spiritMaximumHealth() { return this.spiritMaximumHealth.get(); }
    public int activityMode() { return this.activityMode.get(); }
    public int alertMode() { return this.alertMode.get(); }
    public int enabledSkills() { return this.enabledSkills.get(); }
    public int shieldCharges() { return this.shieldCharges.get(); }
    public int activeSkillMaximumCharges() { return this.activeSkillMaximumCharges.get(); }
    public int shieldChargeProgress() { return this.shieldChargeProgress.get(); }
    public int legionCooldownTicks() { return this.legionCooldownTicks.get(); }
    public boolean legionActive() { return this.legionActive.get() != 0; }
    public boolean formationActive() { return this.formationActive.get() != 0; }
    public int relicLevel() { return this.relicLevel.get(); }
    public int relicExperience() { return this.relicExperience.get(); }
    public int relicExperienceNeeded() { return this.relicExperienceNeeded.get(); }
    public int spiritAttackDamage() { return this.spiritAttackDamage.get(); }
    public int spiritArmor() { return this.spiritArmor.get(); }
    public int spiritMovement() { return this.spiritMovement.get(); }
    public int spiritAttackSpeed() { return this.spiritAttackSpeed.get(); }
    public int summonCostPercent() { return this.summonCostPercent.get(); }
    public int spiritAlertRange() { return this.spiritAlertRange.get(); }
    public double accessoryMaximumHealthChange() {
        return EchoAccessorySystem1211.maximumHealthBonus(this.summonerContainer);
    }
    public double accessoryArmorChange() { return EchoAccessorySystem1211.armorBonus(this.summonerContainer); }
    public double accessoryMovementChange() {
        return EchoAccessorySystem1211.movementMultiplier(this.summonerContainer) - 1.0;
    }
    public double accessoryAlertRangeChange() { return this.spiritAlertRange() - 16.0; }
    public int traitMask() {
        return this.traitMaskLow.get() & 0xFFFF | (this.traitMaskHigh.get() & 0xFFFF) << 16;
    }
    public int biomeAffinity() { return this.biomeAffinity.get(); }
    public int heroType() { return this.heroType.get(); }
    public int skillCount() { return this.skillCount.get(); }
    public int egyptianArrowMode() { return this.egyptianArrowMode.get(); }

    @Override
    public void broadcastChanges() {
        if (this.owner instanceof ServerPlayer player) {
            reloadAuthoritativeStateIfChanged(player, false);
            if (this.owner.tickCount % 5 == 0) convertOneFuel();
        }
        refreshData();
        super.broadcastChanges();
    }

    private void convertOneFuel() {
        ItemStack source = sourceStack();
        if (!source.is(ModContent1211.ECHO_SUMMONER)) return;
        ItemStack fuel = this.summonerContainer.getItem(FUEL_SLOT);
        int value = SummonerFuel1211.value(fuel);
        if (value <= 0 || SummonerFuel1211.amount(source) + value > SummonerFuel1211.CAPACITY) return;
        boolean soul = fuel.is(net.minecraft.world.item.Items.SOUL_SAND)
                || fuel.is(net.minecraft.world.item.Items.SOUL_SOIL);
        fuel.shrink(1);
        EchoBindingSystem1211.addFuel((net.minecraft.server.level.ServerLevel)this.owner.level(), source, value);
        this.observedStateRevision = EchoBindingSystem1211.stateRevision(
                (net.minecraft.server.level.ServerLevel)this.owner.level(), this.summonerId);
        saveContents();
        reportAction(soul ? ACTION_FUEL_SOUL_SAND : ACTION_FUEL_ROTTEN_FLESH);
    }

    private void refreshData() {
        if (!(this.owner instanceof ServerPlayer player)) return;
        ItemStack source = sourceStack();
        if (!source.is(ModContent1211.ECHO_SUMMONER)) return;
        EchoBindingSavedData1211.Binding binding = EchoBindingSystem1211.synchronize(
                (net.minecraft.server.level.ServerLevel)player.level(), source);
        this.observedStateRevision = binding.stateRevision();
        syncAuthoritativeRelic(binding);
        this.fuelAmount.set(SummonerFuel1211.amount(source));
        this.spiritPresent.set(binding.active() ? 1 : 0);
        var spirit = EchoBindingSystem1211.findLoaded(player.server, binding.spiritId());
        ItemStack relic = binding.relic();
        EchoHeroType1211 currentHero = EchoHeroType1211.fromRelic(relic);
        this.spiritMaximumHealth.set(spirit == null ? Math.round((float)currentHero.maximumHealth())
                : Math.round(spirit.livingEntity().getMaxHealth()));
        long now = player.level().getGameTime();
        this.activityMode.set(binding.activityMode());
        this.alertMode.set(binding.alertMode());
        this.enabledSkills.set(binding.enabledSkills());
        this.shieldCharges.set(EchoRelicState1211.activeSkillCharges(relic, now));
        this.activeSkillMaximumCharges.set(EchoRelicState1211.activeSkillMaximumCharges(relic));
        this.shieldChargeProgress.set(EchoRelicState1211.activeSkillChargeProgress(relic, now));
        long auxiliaryCooldown = currentHero == EchoHeroType1211.JAPANESE_SAMURAI
                ? EchoRelicState1211.samuraiStabCooldownEnd(relic) - now
                : EchoRelicState1211.legionCooldownEnd(relic) - now;
        this.legionCooldownTicks.set((int)Math.max(0L, Math.min(
                currentHero == EchoHeroType1211.JAPANESE_SAMURAI ? 200L : 400L, auxiliaryCooldown)));
        this.legionActive.set(spirit != null && spirit.isLegionEnduresActive() ? 1 : 0);
        this.formationActive.set(spirit != null && spirit.isFormationActive() ? 1 : 0);
        if (relic.getItem() instanceof EchoRelicItem1211 relicItem) {
            int levelValue = EchoRelicProgress1211.level(relic);
            this.relicLevel.set(levelValue);
            this.relicExperience.set(EchoRelicProgress1211.experience(relic));
            this.relicExperienceNeeded.set(EchoRelicProgress1211.experienceNeeded(levelValue));
            this.spiritMaximumHealth.set(Math.round((float)(EchoRelicState1211.maximumHealth(relic)
                    + EchoAccessorySystem1211.maximumHealthBonus(this.summonerContainer))));
            this.spiritAttackDamage.set((int)Math.round((EchoRelicState1211.attackDamage(relic)
                    + EchoAccessorySystem1211.attackBonus(this.summonerContainer)) * 10.0));
            this.spiritArmor.set((int)Math.round((EchoRelicState1211.armor(relic)
                    + EchoAccessorySystem1211.armorBonus(this.summonerContainer)) * 10.0));
            this.spiritMovement.set((int)Math.round(EchoRelicState1211.movementSpeed(relic)
                    / relicItem.heroType().movementSpeed()
                    * EchoAccessorySystem1211.movementMultiplier(this.summonerContainer) * 100.0));
            this.spiritAlertRange.set((int)Math.round(EchoAccessorySystem1211.proactiveRange(
                    this.summonerContainer, 16.0, false)));
            this.spiritAttackSpeed.set(EchoRelicState1211.attackSpeedPercent(relic));
            this.summonCostPercent.set(EchoRelicState1211.summonCostPercent(relic));
            int mask = EchoRelicState1211.traitMask(relic);
            this.traitMaskLow.set(mask & 0xFFFF);
            this.traitMaskHigh.set(mask >>> 16);
            this.biomeAffinity.set(EchoRelicState1211.biomeAffinity(relic).ordinal());
            this.heroType.set(relicItem.heroType().ordinal());
            this.skillCount.set(relicItem.heroType().skillCount());
            this.egyptianArrowMode.set(EchoRelicState1211.egyptianArrowMode(relic).ordinal());
        } else {
            this.relicLevel.set(0);
            this.relicExperience.set(0);
            this.relicExperienceNeeded.set(0);
            this.spiritAttackDamage.set(0);
            this.spiritArmor.set(0);
            this.spiritMovement.set(0);
            this.spiritAttackSpeed.set(0);
            this.summonCostPercent.set(100);
            this.spiritAlertRange.set(0);
            this.traitMaskLow.set(0);
            this.traitMaskHigh.set(0);
            this.biomeAffinity.set(0);
            this.heroType.set(0);
            this.skillCount.set(0);
            this.egyptianArrowMode.set(0);
            this.shieldCharges.set(0);
            this.activeSkillMaximumCharges.set(0);
            this.shieldChargeProgress.set(0);
            this.legionCooldownTicks.set(0);
        }
        int maximumHealth = Math.max(0, this.spiritMaximumHealth.get());
        int currentHealth = spirit == null
                ? Math.round(binding.health())
                : Math.round(spirit.livingEntity().getHealth());
        if (spirit == null && currentHealth <= 0 && !relic.isEmpty()) currentHealth = maximumHealth;
        this.spiritHealth.set(Mth.clamp(currentHealth, 0, maximumHealth));
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!(player instanceof ServerPlayer serverPlayer)) return false;
        if (!reloadAuthoritativeStateIfChanged(serverPlayer, true)) return true;
        ItemStack source = sourceStack();
        if (!source.is(ModContent1211.ECHO_SUMMONER)) {
            reportAction(ACTION_INVALID_SUMMONER);
            return true;
        }
        saveContents();
        net.minecraft.server.level.ServerLevel level = (net.minecraft.server.level.ServerLevel)serverPlayer.level();
        EchoBindingSavedData1211.Binding binding = EchoBindingSystem1211.synchronize(level, source);
        ItemStack relic = this.summonerContainer.getItem(RELIC_SLOT);
        if (!(relic.getItem() instanceof EchoRelicItem1211)) {
            reportAction(ACTION_NO_RELIC);
            return true;
        }
        if (id >= BUTTON_ACTIVITY_START && id < BUTTON_ACTIVITY_START + 3) {
            EchoBindingSystem1211.setActivityMode(level, binding, id - BUTTON_ACTIVITY_START);
            refreshData();
            reportAction(ACTION_MODE_CHANGED);
            return true;
        }
        if (id >= BUTTON_ALERT_START && id < BUTTON_ALERT_START + 3) {
            EchoBindingSystem1211.setAlertMode(level, binding, id - BUTTON_ALERT_START);
            refreshData();
            reportAction(ACTION_MODE_CHANGED);
            return true;
        }
        if (id >= BUTTON_SKILL_START && id < BUTTON_SKILL_START + binding.skillCount()) {
            EchoBindingSystem1211.toggleSkill(level, binding, id - BUTTON_SKILL_START);
            refreshData();
            reportAction(ACTION_SKILL_CHANGED);
            return true;
        }
        if (id != BUTTON_SUMMON_OR_DISMISS) return false;
        if (binding.active()) {
            reportAction(EchoBindingSystem1211.dismiss(level, source)
                    ? ACTION_DISMISSED : ACTION_INVALID_SUMMONER);
            refreshData();
            return true;
        }
        int summonCost = SummonerFuel1211.summonCost(relic);
        if (binding.fuel() < summonCost) {
            reportAction(ACTION_NOT_ENOUGH_FUEL);
            return true;
        }
        if (!EchoBindingSystem1211.canAddControllerEcho(
                level.getServer(), serverPlayer.getUUID(), binding.summonerId())) {
            reportAction(ACTION_LIMIT_REACHED);
            return true;
        }
        EchoSummonerItem1211.SummonResult result = EchoSummonerItem1211.summonFromMenu(serverPlayer, source);
        reportAction(switch (result) {
            case SUMMONED -> ACTION_SUMMONED;
            case NO_RELIC -> ACTION_NO_RELIC;
            case NOT_ENOUGH_FUEL -> ACTION_NOT_ENOUGH_FUEL;
            case NO_SAFE_POSITION -> ACTION_NO_SAFE_POSITION;
            case LIMIT_REACHED -> ACTION_LIMIT_REACHED;
            default -> ACTION_CREATE_FAILED;
        });
        refreshData();
        return true;
    }

    private void reportAction(int action) {
        this.actionSequence = (this.actionSequence + 1) & 0x07FFFFFF;
        this.actionFeedback.set((this.actionSequence << 8) | (action & 0xFF));
    }

    private boolean reloadAuthoritativeStateIfChanged(ServerPlayer player, boolean reportStale) {
        if (this.summonerId == null) return false;
        ItemStack source = sourceStack();
        if (!source.is(ModContent1211.ECHO_SUMMONER) || !matches(source)) return false;
        EchoBindingSavedData1211.Binding binding = EchoBindingSystem1211.synchronize(
                (net.minecraft.server.level.ServerLevel)player.level(), source);
        long currentRevision = binding.stateRevision();
        if (currentRevision == this.observedStateRevision) return true;
        this.loading = true;
        for (int slot = 0; slot < this.summonerContainer.getContainerSize(); slot++) {
            this.summonerContainer.setItem(slot, ItemStack.EMPTY);
        }
        source.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY)
                .copyInto(this.summonerContainer.getItems());
        this.loading = false;
        this.observedStateRevision = currentRevision;
        if (reportStale) reportAction(ACTION_STALE_STATE);
        return !reportStale;
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (player instanceof ServerPlayer serverPlayer
                && !reloadAuthoritativeStateIfChanged(serverPlayer, true)) {
            broadcastChanges();
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    private ItemStack sourceStack() {
        return this.sourceInventorySlot >= 0 && this.sourceInventorySlot < this.owner.getInventory().getContainerSize()
                ? this.owner.getInventory().getItem(this.sourceInventorySlot)
                : ItemStack.EMPTY;
    }

    private void saveContents() {
        if (this.loading || this.owner.level().isClientSide) return;
        ItemStack source = sourceStack();
        if (!source.is(ModContent1211.ECHO_SUMMONER)) return;
        ServerPlayer player = (ServerPlayer)this.owner;
        if (!reloadAuthoritativeStateIfChanged(player, true)) return;
        net.minecraft.server.level.ServerLevel level = (net.minecraft.server.level.ServerLevel)player.level();
        EchoBindingSavedData1211.Binding binding = EchoBindingSystem1211.synchronize(level, source);
        ItemStack menuRelic = this.summonerContainer.getItem(RELIC_SLOT);
        ItemStack reconciledRelic = EchoBindingSystem1211.reconcileMenuRelicForSave(menuRelic, binding.relic());
        if (!ItemStack.matches(menuRelic, reconciledRelic)) {
            this.loading = true;
            this.summonerContainer.setItem(RELIC_SLOT, reconciledRelic);
            this.loading = false;
        }
        source.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(this.summonerContainer.getItems()));
        EchoBindingSystem1211.commitMenuContents(level, source);
        this.observedStateRevision = EchoBindingSystem1211.stateRevision(level, this.summonerId);
    }

    private void syncAuthoritativeRelic(EchoBindingSavedData1211.Binding binding) {
        ItemStack authoritativeRelic = binding.relic();
        if (ItemStack.matches(this.summonerContainer.getItem(RELIC_SLOT), authoritativeRelic)) return;
        this.loading = true;
        this.summonerContainer.setItem(RELIC_SLOT, authoritativeRelic);
        this.loading = false;
    }

    public boolean allowsDirectInsertionIntoSource(Slot slot, Player player) {
        boolean lockedSource = player == this.owner
                && slot instanceof LockedSlot
                && slot.container == this.owner.getInventory()
                && slot.getContainerSlot() == this.sourceInventorySlot;
        if (!lockedSource) return false;
        return this.owner.level().isClientSide
                ? slot.getItem().is(ModContent1211.ECHO_SUMMONER)
                : this.matches(sourceStack());
    }

    public boolean insertIntoOpenSummoner(ItemStack carried) {
        if (carried.isEmpty() || (!this.owner.level().isClientSide && !this.matches(sourceStack()))) return false;
        boolean inserted = false;
        if (SummonerFuel1211.isFuel(carried)) {
            inserted = insertIntoMenuSlot(FUEL_SLOT, carried, carried.getCount());
        } else if (carried.getItem() instanceof EchoRelicItem1211) {
            inserted = insertIntoMenuSlot(RELIC_SLOT, carried, 1);
        } else if (EchoSummonerAccessory1211.isAccessory(carried)) {
            for (int slot = 0; slot < MODULE_SLOT_COUNT; slot++) {
                if (insertIntoMenuSlot(slot, carried, 1)) {
                    inserted = true;
                    break;
                }
            }
        }
        if (inserted && !this.owner.level().isClientSide) saveContents();
        return inserted;
    }

    public ItemStack directInsertionRelic() {
        return this.summonerContainer.getItem(RELIC_SLOT);
    }

    private boolean insertIntoMenuSlot(int slot, ItemStack carried, int maximumAmount) {
        int previousCount = carried.getCount();
        this.slots.get(slot).safeInsert(carried, maximumAmount);
        return carried.getCount() < previousCount;
    }

    @Override
    public void removed(Player player) {
        saveContents();
        super.removed(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        Slot slot = this.slots.get(slotIndex);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (slotIndex < CUSTOM_SLOT_COUNT) {
            if (!this.moveItemStackTo(stack, PLAYER_SLOT_START, PLAYER_SLOT_END, true)) return ItemStack.EMPTY;
        } else if (SummonerFuel1211.isFuel(stack)) {
            if (!this.moveItemStackTo(stack, FUEL_SLOT, FUEL_SLOT + 1, false)) return ItemStack.EMPTY;
        } else if (EchoSummonerAccessory1211.isAccessory(stack)) {
            if (!this.moveItemStackTo(stack, 0, MODULE_SLOT_COUNT, false)) return ItemStack.EMPTY;
        } else if (stack.getItem() instanceof EchoRelicItem1211) {
            if (!this.moveItemStackTo(stack, RELIC_SLOT, RELIC_SLOT + 1, false)) return ItemStack.EMPTY;
        } else {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.sourceInventorySlot < 0 || this.matches(sourceStack());
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
