package com.yuriscat.echowarrior.compat.menu;

import com.yuriscat.echowarrior.compat.ModContent1201;
import com.yuriscat.echowarrior.compat.EchoHeroType1201;
import com.yuriscat.echowarrior.compat.binding.EchoBindingSystem1201;
import com.yuriscat.echowarrior.compat.item.EchoAccessorySystem1201;
import com.yuriscat.echowarrior.compat.item.EchoRelicProgress1201;
import net.minecraft.util.Mth;
import com.yuriscat.echowarrior.compat.binding.EchoBindingSavedData1201;
import com.yuriscat.echowarrior.compat.item.SummonerData1201;
import com.yuriscat.echowarrior.compat.item.SummonerFuel1201;
import com.yuriscat.echowarrior.compat.item.EchoRelicItem1201;
import com.yuriscat.echowarrior.compat.item.EchoRelicState1201;
import com.yuriscat.echowarrior.compat.item.EchoSummonerAccessory1201;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import java.util.UUID;

/** Original eight-slot layout, with server-authoritative whole-equipment transactions. */
public final class SummonerMenu1201 extends AbstractContainerMenu {
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
    private int actionSequence;
    private final DataSlot spiritPresent = DataSlot.standalone();
    private final DataSlot actionFeedback = DataSlot.standalone();
    private final WideDataSlot1201 spiritHealth = new WideDataSlot1201();
    private final WideDataSlot1201 spiritMaximumHealth = new WideDataSlot1201();
    private final DataSlot activityMode = DataSlot.standalone();
    private final DataSlot alertMode = DataSlot.standalone();
    private final DataSlot enabledSkills = DataSlot.standalone();
    private final DataSlot shieldCharges = DataSlot.standalone();
    private final DataSlot activeSkillMaximumCharges = DataSlot.standalone();
    private final DataSlot shieldChargeProgress = DataSlot.standalone();
    private final DataSlot legionCooldownTicks = DataSlot.standalone();
    private final DataSlot legionActive = DataSlot.standalone();
    private final DataSlot formationActive = DataSlot.standalone();
    private final WideDataSlot1201 relicLevel = new WideDataSlot1201();
    private final WideDataSlot1201 relicExperience = new WideDataSlot1201();
    private final WideDataSlot1201 relicExperienceNeeded = new WideDataSlot1201();
    private final WideDataSlot1201 spiritAttackDamage = new WideDataSlot1201();
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
    private final Player owner;
    private final UUID summonerId;
    private final SimpleContainer contents = new SimpleContainer(8);
    private final DataSlot sourceIndex = DataSlot.standalone();
    private final DataSlot fuel = DataSlot.standalone();
    private long observedRevision = -1;

    public SummonerMenu1201(int id, Inventory inventory) { this(id, inventory, -1); }

    public SummonerMenu1201(int id, Inventory inventory, int sourceSlot) {
        super(ModContent1201.SUMMONER_MENU, id);
        owner = inventory.player;
        sourceIndex.set(sourceSlot);
        summonerId = sourceSlot < 0 ? null : SummonerData1201.summonerId(inventory.getItem(sourceSlot));
        for (int index = 0; index < 6; index++) addSlot(new AccessorySlot(contents, index, 8 + index * 29, 94));
        addSlot(new FuelSlot(contents));
        addSlot(new RelicSlot(contents));
        for (int row = 0; row < 3; row++) for (int column = 0; column < 9; column++) {
            addSlot(playerSlot(inventory, 9 + row * 9 + column, 8 + column * 18, 120 + row * 18));
        }
        for (int column = 0; column < 9; column++) addSlot(playerSlot(inventory, column, 8 + column * 18, 177));
        addDataSlot(sourceIndex);
        addDataSlot(fuel);
        this.addDataSlot(this.spiritPresent);
        this.addDataSlot(this.actionFeedback);
        this.spiritHealth.register(this::addDataSlot);
        this.spiritMaximumHealth.register(this::addDataSlot);
        this.addDataSlot(this.activityMode);
        this.addDataSlot(this.alertMode);
        this.addDataSlot(this.enabledSkills);
        this.addDataSlot(this.shieldCharges);
        this.addDataSlot(this.activeSkillMaximumCharges);
        this.addDataSlot(this.shieldChargeProgress);
        this.addDataSlot(this.legionCooldownTicks);
        this.addDataSlot(this.legionActive);
        this.addDataSlot(this.formationActive);
        this.relicLevel.register(this::addDataSlot);
        this.relicExperience.register(this::addDataSlot);
        this.relicExperienceNeeded.register(this::addDataSlot);
        this.spiritAttackDamage.register(this::addDataSlot);
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
        reloadAuthority();
        refreshData();
    }

    private Slot playerSlot(Inventory inventory, int index, int x, int y) {
        return new PlayerSlot(inventory, index, x, y);
    }

    public int fuelAmount() { return fuel.get(); }
    public int sourceInventorySlot() { return sourceIndex.get(); }

    public boolean matches(ItemStack stack) {
        return summonerId != null && summonerId.equals(SummonerData1201.summonerId(stack));
    }
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
        return EchoAccessorySystem1201.maximumHealthBonus(this.contents);
    }
    public double accessoryArmorChange() { return EchoAccessorySystem1201.armorBonus(this.contents); }
    public double accessoryMovementChange() {
        return EchoAccessorySystem1201.movementMultiplier(this.contents) - 1.0;
    }
    public double accessoryAlertRangeChange() { return this.spiritAlertRange() - 16.0; }
    public int traitMask() {
        return this.traitMaskLow.get() & 0xFFFF | (this.traitMaskHigh.get() & 0xFFFF) << 16;
    }
    public int biomeAffinity() { return this.biomeAffinity.get(); }
    public int heroType() { return this.heroType.get(); }
    public int skillCount() { return this.skillCount.get(); }
    public int egyptianArrowMode() { return this.egyptianArrowMode.get(); }
    private void refreshData() {
        if (!(this.owner instanceof ServerPlayer player)) return;
        ItemStack source = sourceStack();
        if (!source.is(ModContent1201.ECHO_SUMMONER)) return;
        var binding = binding();
        if (binding == null || !stillValid(owner)) return;
        this.fuel.set(binding.fuel());
        this.spiritPresent.set(binding.active() ? 1 : 0);
        var spirit = EchoBindingSystem1201.findLoaded(player.server, binding.spiritId());
        ItemStack relic = binding.relic();
        EchoHeroType1201 currentHero = EchoHeroType1201.fromRelic(relic);
        this.spiritMaximumHealth.set(spirit == null ? Math.round((float)currentHero.baseMaximumHealth())
                : Math.round(spirit.livingEntity().getMaxHealth()));
        long now = player.level().getGameTime();
        this.activityMode.set(binding.activityMode());
        this.alertMode.set(binding.alertMode());
        this.enabledSkills.set(binding.enabledSkills());
        this.shieldCharges.set(EchoRelicState1201.activeSkillCharges(relic, now));
        this.activeSkillMaximumCharges.set(EchoRelicState1201.activeSkillMaximumCharges(relic));
        this.shieldChargeProgress.set(EchoRelicState1201.activeSkillChargeProgress(relic, now));
        long auxiliaryCooldown = currentHero == EchoHeroType1201.JAPANESE_SAMURAI
                ? EchoRelicState1201.samuraiStabCooldownEnd(relic) - now
                : EchoRelicState1201.legionCooldownEnd(relic) - now;
        this.legionCooldownTicks.set((int)Math.max(0L, Math.min(
                currentHero == EchoHeroType1201.JAPANESE_SAMURAI ? 200L : 400L, auxiliaryCooldown)));
        this.legionActive.set(spirit != null && spirit.isLegionEnduresActive() ? 1 : 0);
        this.formationActive.set(spirit != null && spirit.isFormationActive() ? 1 : 0);
        if (relic.getItem() instanceof EchoRelicItem1201 relicItem) {
            int levelValue = EchoRelicProgress1201.level(relic);
            this.relicLevel.set(levelValue);
            this.relicExperience.set(EchoRelicProgress1201.experience(relic));
            this.relicExperienceNeeded.set(EchoRelicProgress1201.experienceNeeded(levelValue));
            this.spiritMaximumHealth.set(Math.round((float)(EchoRelicState1201.maximumHealth(relic)
                    + EchoAccessorySystem1201.maximumHealthBonus(this.contents))));
            this.spiritAttackDamage.set((int)Math.round((EchoRelicState1201.attackDamage(relic)
                    + EchoAccessorySystem1201.attackBonus(this.contents)) * 10.0));
            this.spiritArmor.set((int)Math.round((EchoRelicState1201.armor(relic)
                    + EchoAccessorySystem1201.armorBonus(this.contents)) * 10.0));
            this.spiritMovement.set((int)Math.round(EchoRelicState1201.movementSpeed(relic)
                    / relicItem.heroType().movementSpeed()
                    * EchoAccessorySystem1201.movementMultiplier(this.contents) * 100.0));
            this.spiritAlertRange.set((int)Math.round(EchoAccessorySystem1201.proactiveRange(
                    this.contents, 16.0, false)));
            this.spiritAttackSpeed.set(EchoRelicState1201.attackSpeedPercent(relic));
            this.summonCostPercent.set(EchoRelicState1201.summonCostPercent(relic));
            int mask = EchoRelicState1201.traitMask(relic);
            this.traitMaskLow.set(mask & 0xFFFF);
            this.traitMaskHigh.set(mask >>> 16);
            this.biomeAffinity.set(EchoRelicState1201.biomeAffinity(relic).ordinal());
            this.heroType.set(relicItem.heroType().ordinal());
            this.skillCount.set(relicItem.heroType().skillCount());
            this.egyptianArrowMode.set(EchoRelicState1201.egyptianArrowMode(relic).ordinal());
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
                ? Math.round(binding.snapshot().health())
                : Math.round(spirit.livingEntity().getHealth());
        if (spirit == null && currentHealth <= 0 && !relic.isEmpty()) currentHealth = maximumHealth;
        this.spiritHealth.set(Mth.clamp(currentHealth, 0, maximumHealth));
    }

    private ItemStack sourceStack() {
        int slot = sourceIndex.get();
        return slot >= 0 && (slot < 36 || slot == 40) ? owner.getInventory().getItem(slot) : ItemStack.EMPTY;
    }

    private EchoBindingSavedData1201.Binding binding() {
        return owner instanceof ServerPlayer player && summonerId != null
                ? EchoBindingSavedData1201.get(player.server).get(summonerId) : null;
    }

    @Override public boolean stillValid(Player player) {
        return player == owner && player.isAlive() && !player.isSpectator()
                && (owner.level().isClientSide || summonerId != null
                && summonerId.equals(SummonerData1201.summonerId(sourceStack())) && binding() != null);
    }

    private void reloadAuthority() {
        var binding = binding();
        if (binding == null || !stillValid(owner)) return;
        var authoritative = binding.contents();
        for (int index = 0; index < 8; index++) contents.setItem(index, authoritative.get(index));
        observedRevision = binding.stateRevision();
        fuel.set(binding.fuel());
        binding.mirrorTo(sourceStack());
    }

    @Override public void broadcastChanges() {
        var binding = binding();
        if (binding != null && binding.stateRevision() != observedRevision) reloadAuthority();
        refreshData();
        super.broadcastChanges();
    }

    @Override public boolean clickMenuButton(Player player, int id) {
        if (!(player instanceof ServerPlayer serverPlayer) || player != owner || !stillValid(player)) return false;
        var binding = binding();
        if (binding.stateRevision() != observedRevision) {
            reloadAuthority();
            reportAction(ACTION_STALE_STATE);
            broadcastChanges();
            return true;
        }
        int action;
        var level = serverPlayer.serverLevel();
        if (!(binding.relic().getItem() instanceof EchoRelicItem1201)) action = ACTION_NO_RELIC;
        else if (id >= BUTTON_ACTIVITY_START && id < BUTTON_ACTIVITY_START + 3) {
            if (id == BUTTON_ACTIVITY_START && binding.active() && !player.getUUID().equals(binding.controllerId())) {
                if (!EchoBindingSystem1201.canAddControllerEcho(serverPlayer.server, player.getUUID(), summonerId)) action = ACTION_LIMIT_REACHED;
                else action = EchoBindingSystem1201.transferToFollowing(serverPlayer, sourceStack(), binding)
                        ? ACTION_MODE_CHANGED : ACTION_NO_SAFE_POSITION;
            } else {
                EchoBindingSystem1201.setActivityMode(level, binding, id - BUTTON_ACTIVITY_START);
                action = ACTION_MODE_CHANGED;
            }
        } else if (id >= BUTTON_ALERT_START && id < BUTTON_ALERT_START + 3) {
            EchoBindingSystem1201.setAlertMode(level, binding, id - BUTTON_ALERT_START);
            action = ACTION_MODE_CHANGED;
        } else if (id >= BUTTON_SKILL_START && id < BUTTON_SKILL_START + EchoHeroType1201.fromRelic(binding.relic()).skillCount()) {
            EchoBindingSystem1201.toggleSkill(level, binding, id - BUTTON_SKILL_START);
            action = ACTION_SKILL_CHANGED;
        } else if (id == BUTTON_SUMMON_OR_DISMISS) {
            if (binding.active()) action = EchoBindingSystem1201.dismiss(level, sourceStack()) ? ACTION_DISMISSED : ACTION_INVALID_SUMMONER;
            else action = switch (EchoBindingSystem1201.summonNew(level, player, sourceStack()).failure()) {
                case NONE -> ACTION_SUMMONED;
                case NO_RELIC -> ACTION_NO_RELIC;
                case NO_SAFE_POSITION -> ACTION_NO_SAFE_POSITION;
                case LIMIT_REACHED -> ACTION_LIMIT_REACHED;
                case NOT_ENOUGH_FUEL -> ACTION_NOT_ENOUGH_FUEL;
                default -> ACTION_CREATE_FAILED;
            };
        } else return false;
        reloadAuthority();
        reportAction(action);
        broadcastChanges();
        return true;
    }

    public void reportFuelConsumed(ItemStack consumed) {
        reportAction(consumed.is(net.minecraft.world.item.Items.SOUL_SAND) || consumed.is(net.minecraft.world.item.Items.SOUL_SOIL)
                ? ACTION_FUEL_SOUL_SAND : ACTION_FUEL_ROTTEN_FLESH);
    }

    private void reportAction(int action) {
        // Vanilla 1.20.1 container data packets carry signed shorts, not arbitrary ints.
        actionSequence = (actionSequence + 1) & 0x7F;
        actionFeedback.set((actionSequence << 8) | (action & 0xFF));
    }

    /** Vanilla otherwise executes a stale click then resyncs; this menu must reject it instead. */
    public boolean acceptRemoteState(int remoteState) {
        var binding = binding();
        return stillValid(owner) && binding != null && remoteState == getStateId()
                && binding.stateRevision() == observedRevision;
    }

    public void rejectRemoteClick() {
        reloadAuthority();
        sendAllDataToRemote();
    }

    @Override public void clicked(int slotId, int button, ClickType type, Player player) {
        // Server-only mutation: no client-side copies of the summoner are ever saved.
        if (!(owner instanceof ServerPlayer) || player != owner || !stillValid(player)) return;
        var binding = binding();
        if (binding.stateRevision() != observedRevision) { rejectRemoteClick(); return; }
        if (type == ClickType.SWAP && button == sourceIndex.get()) return;
        ItemStack insertionFeedback = ItemStack.EMPTY;
        if (slotId >= 8 && slotId < slots.size()
                && slots.get(slotId).getContainerSlot() == sourceIndex.get()) {
            if (type == ClickType.PICKUP && button == 0) {
                ItemStack before = getCarried().copy();
                insertCarried();
                if (getCarried().getCount() < before.getCount()) insertionFeedback = before;
            }
            else return;
        } else {
            super.clicked(slotId, button, type, player);
        }
        // All vanilla slot changes from one click (including shift transfer) commit together.
        EchoRelicState1201.ensureInitialized(contents.getItem(7), owner.getRandom(), owner.level().getGameTime());
        if (!binding.commitEquipment(observedRevision,
                java.util.stream.IntStream.range(0, 8).mapToObj(contents::getItem).toList())) {
            throw new IllegalStateException("Summoner authority changed during a server-thread click");
        }
        observedRevision = binding.stateRevision();
        binding.mirrorTo(sourceStack());
        owner.getInventory().setChanged();
        broadcastChanges();
        if (!insertionFeedback.isEmpty()) {
            com.yuriscat.echowarrior.compat.network.InventoryNetwork1201.feedback(
                    (ServerPlayer)owner, new com.yuriscat.echowarrior.compat.network.InsertionFeedback1201(containerId, slotId, insertionFeedback));
        }
    }

    private void insertCarried() {
        ItemStack carried = getCarried();
        var planned = new java.util.ArrayList<>(java.util.stream.IntStream.range(0, 8).mapToObj(contents::getItem).toList());
        int amount = SummonerTransfer1201.insert(planned, carried);
        if (amount <= 0) return;
        for (int index = 0; index < 8; index++) contents.setItem(index, planned.get(index));
        carried.shrink(amount);
    }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot source = slots.get(index);
        if (!source.hasItem() || !source.mayPickup(player)) return ItemStack.EMPTY;
        ItemStack stack = source.getItem();
        ItemStack original = stack.copy();
        if (index < 8) {
            if (!moveItemStackTo(stack, 8, slots.size(), true)) return ItemStack.EMPTY;
        } else if (stack.getItem() instanceof EchoRelicItem1201) {
            if (!moveItemStackTo(stack, 7, 8, false)) return ItemStack.EMPTY;
        } else if (stack.getItem() instanceof EchoSummonerAccessory1201) {
            if (!moveItemStackTo(stack, 0, 6, false)) return ItemStack.EMPTY;
        } else if (index >= 8 && SummonerFuel1201.value(stack) > 0) {
            if (!moveItemStackTo(stack, FUEL_SLOT, FUEL_SLOT + 1, false)) return ItemStack.EMPTY;
        } else if (index >= 8 && index < 35) {
            if (!moveItemStackTo(stack, 35, 44, false)) return ItemStack.EMPTY;
        } else if (index >= 35) {
            if (!moveItemStackTo(stack, 8, 35, false)) return ItemStack.EMPTY;
        } else return ItemStack.EMPTY;
        if (stack.isEmpty()) source.set(ItemStack.EMPTY);
        else source.setChanged();
        source.onTake(player, stack);
        return original;
    }

    @Override public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return slot.mayPickup(owner) && super.canTakeItemForPickAll(stack, slot);
    }

    // removed() intentionally uses vanilla cursor return only. Never flush an obsolete snapshot on close.

    private static final class FuelSlot extends Slot {
        FuelSlot(SimpleContainer container) { super(container, FUEL_SLOT, 179, 172); }
        @Override public boolean mayPlace(ItemStack stack) { return SummonerFuel1201.value(stack) > 0; }
    }

    private static final class RelicSlot extends Slot {
        RelicSlot(SimpleContainer container) { super(container, 7, 217, 172); }
        @Override public boolean mayPlace(ItemStack stack) { return stack.getItem() instanceof EchoRelicItem1201; }
        @Override public int getMaxStackSize() { return 1; }
    }

    private static final class AccessorySlot extends Slot {
        AccessorySlot(SimpleContainer contents, int index, int x, int y) { super(contents, index, x, y); }
        @Override public boolean mayPlace(ItemStack stack) {
            return EchoSummonerAccessory1201.canInstall(container, getContainerSlot(), stack);
        }
        @Override public int getMaxStackSize() { return 1; }
    }

    private final class PlayerSlot extends Slot {
        PlayerSlot(Inventory inventory, int index, int x, int y) { super(inventory, index, x, y); }
        @Override public boolean mayPlace(ItemStack stack) { return getContainerSlot() != sourceIndex.get(); }
        @Override public boolean mayPickup(Player player) { return getContainerSlot() != sourceIndex.get(); }
    }
}
