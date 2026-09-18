package com.yuriscat.echowarrior.compat.block.entity;

import com.yuriscat.echowarrior.compat.EchoWarrior1211;
import com.yuriscat.echowarrior.compat.ModContent1211;
import com.yuriscat.echowarrior.compat.ModTags1211;
import com.yuriscat.echowarrior.compat.item.EchoRelicProgress1211;
import com.yuriscat.echowarrior.compat.knowledge.KnowledgeStackData1211;
import com.yuriscat.echowarrior.compat.menu.RecyclerMenu1211;
import com.yuriscat.echowarrior.compat.recycler.RecyclerSystem1211;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public final class RecyclerChestBlockEntity1211 extends ChestBlockEntity {
    private static final Component DEFAULT_NAME = Component.translatable("container.echo_warrior.echo_recycler");
    private static final int SEAL_TICKS = 40;
    private static final int MAX_UNITS = 4_096;
    private static final long UNSET = Long.MIN_VALUE;
    private static final ResourceKey<LootTable> COMMON_POOL = lootTable("gameplay/recycler/common");
    private static final ResourceKey<LootTable> RARE_POOL = lootTable("gameplay/recycler/rare");
    private static final ResourceKey<LootTable> SUPER_POOL = lootTable("gameplay/recycler/super_rare");

    private long lastProcessedSequence = UNSET;
    private long scheduledSequence = UNSET;
    private long scheduledAtGameTime;
    private boolean pending;
    private boolean pendingNatural;
    private long pendingTargetSequence;
    private int sealTicksRemaining;
    private NonNullList<ItemStack> pendingInventory;
    private EffectTier pendingTier = EffectTier.NORMAL;
    private boolean pendingChanged;
    private boolean pendingFailure;
    private boolean spaceFailureNotice;
    private int shakeCloseTicks;

    public RecyclerChestBlockEntity1211(BlockPos pos, BlockState state) {
        super(ModContent1211.RECYCLER_CHEST, pos, state);
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, RecyclerChestBlockEntity1211 entity) {
        ChestBlockEntity.lidAnimateTick(level, pos, state, entity);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, RecyclerChestBlockEntity1211 entity) {
        if (level instanceof ServerLevel serverLevel) entity.tickServer(serverLevel);
    }

    private void tickServer(ServerLevel level) {
        if (this.shakeCloseTicks > 0 && --this.shakeCloseTicks == 0) {
            level.blockEvent(this.worldPosition, getBlockState().getBlock(), 1, 0);
        }
        if (this.pending) {
            if (this.sealTicksRemaining > 0) this.sealTicksRemaining--;
            if (this.sealTicksRemaining <= 0) finishPending(level);
            return;
        }
        long current = RecyclerSystem1211.currentMidnightSequence(level);
        if (this.lastProcessedSequence == UNSET) {
            this.lastProcessedSequence = current;
            setChanged();
            return;
        }
        if (this.scheduledSequence != UNSET) {
            if (this.lastProcessedSequence >= current) clearSchedule();
            else if (level.getGameTime() >= this.scheduledAtGameTime) {
                long target = Math.max(this.scheduledSequence, current);
                clearSchedule();
                beginTransaction(level, true, target);
            }
            return;
        }
        if (this.lastProcessedSequence < current) {
            this.scheduledSequence = current;
            this.scheduledAtGameTime = level.getGameTime() + Math.floorMod(Long.hashCode(this.worldPosition.asLong()), 21);
            setChanged();
        }
    }

    public boolean triggerManual(ServerLevel level) {
        return !this.pending && beginTransaction(level, false, UNSET);
    }

    private boolean beginTransaction(ServerLevel level, boolean natural, long targetSequence) {
        TransactionPlan plan = createPlan(level, this.worldPosition, copyInventory(getItems()), level.getRandom());
        if (!plan.hasValidInput()) {
            if (natural) this.lastProcessedSequence = Math.max(this.lastProcessedSequence, targetSequence);
            setChanged();
            return false;
        }
        this.pending = true;
        closeViewers(level);
        this.pendingNatural = natural;
        this.pendingTargetSequence = targetSequence;
        this.pendingInventory = plan.inventory();
        this.pendingTier = plan.highestTier();
        this.pendingChanged = plan.processedUnits() > 0;
        this.pendingFailure = plan.processedUnits() == 0 && plan.blockedBySpace();
        this.sealTicksRemaining = SEAL_TICKS;
        if (this.pendingChanged) playStartEffect(level);
        setChanged();
        return true;
    }

    private void finishPending(ServerLevel level) {
        if (this.pendingChanged && this.pendingInventory != null) {
            for (int slot = 0; slot < getContainerSize(); slot++) getItems().set(slot, this.pendingInventory.get(slot).copy());
            playEffect(level, this.pendingTier);
        } else if (this.pendingFailure) {
            this.spaceFailureNotice = true;
            playEffect(level, EffectTier.FAILURE);
        }
        if (this.pendingNatural) {
            this.lastProcessedSequence = Math.max(this.lastProcessedSequence,
                    Math.max(this.pendingTargetSequence, RecyclerSystem1211.currentMidnightSequence(level)));
        }
        clearPending();
        setChanged();
    }

    private void closeViewers(ServerLevel level) {
        for (ServerPlayer player : level.players()) {
            if (player.containerMenu instanceof RecyclerMenu1211 menu && menu.recyclerContainer() == this) player.closeContainer();
        }
    }

    private void clearSchedule() {
        this.scheduledSequence = UNSET;
        this.scheduledAtGameTime = 0L;
        setChanged();
    }

    private void clearPending() {
        this.pending = false;
        this.pendingNatural = false;
        this.pendingTargetSequence = 0L;
        this.sealTicksRemaining = 0;
        this.pendingInventory = null;
        this.pendingTier = EffectTier.NORMAL;
        this.pendingChanged = false;
        this.pendingFailure = false;
    }

    public boolean isSealed() { return this.pending; }

    @Override public boolean canOpen(Player player) { return !this.pending && super.canOpen(player); }
    @Override public boolean stillValid(Player player) { return !this.pending && super.stillValid(player); }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) { return !this.pending; }
    @Override public boolean canTakeItem(Container into, int slot, ItemStack stack) { return !this.pending; }
    @Override public ItemStack removeItem(int slot, int count) { return this.pending ? ItemStack.EMPTY : super.removeItem(slot, count); }
    @Override public ItemStack removeItemNoUpdate(int slot) { return this.pending ? ItemStack.EMPTY : super.removeItemNoUpdate(slot); }
    @Override public void setItem(int slot, ItemStack stack) { if (!this.pending) super.setItem(slot, stack); }
    @Override public void clearContent() { if (!this.pending) super.clearContent(); }
    @Override protected Component getDefaultName() { return DEFAULT_NAME; }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        if (!canOpen(player)) return null;
        if (this.spaceFailureNotice) {
            player.displayClientMessage(Component.translatable("message.echo_warrior.recycler.space_failure"), true);
            this.spaceFailureNotice = false;
            setChanged();
        }
        return createMenu(containerId, inventory);
    }

    @Override protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new RecyclerMenu1211(containerId, inventory, this);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.lastProcessedSequence = tag.contains("RecyclerLastSequence") ? tag.getLong("RecyclerLastSequence") : UNSET;
        this.scheduledSequence = tag.contains("RecyclerScheduledSequence") ? tag.getLong("RecyclerScheduledSequence") : UNSET;
        this.scheduledAtGameTime = tag.getLong("RecyclerScheduledAt");
        this.pending = tag.getBoolean("RecyclerPending");
        this.pendingNatural = tag.getBoolean("RecyclerPendingNatural");
        this.pendingTargetSequence = tag.getLong("RecyclerPendingSequence");
        this.sealTicksRemaining = Math.max(0, tag.getInt("RecyclerSealTicks"));
        this.pendingTier = EffectTier.byId(tag.getInt("RecyclerPendingTier"));
        this.pendingChanged = tag.getBoolean("RecyclerPendingChanged");
        this.pendingFailure = tag.getBoolean("RecyclerPendingFailure");
        this.spaceFailureNotice = tag.getBoolean("RecyclerSpaceNotice");
        this.pendingInventory = null;
        if (this.pending) {
            this.pendingInventory = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
            ContainerHelper.loadAllItems(tag.getCompound("RecyclerPendingInventory"), this.pendingInventory, registries);
            if (this.sealTicksRemaining <= 0) this.sealTicksRemaining = 1;
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("RecyclerLastSequence", this.lastProcessedSequence);
        if (this.scheduledSequence != UNSET) {
            tag.putLong("RecyclerScheduledSequence", this.scheduledSequence);
            tag.putLong("RecyclerScheduledAt", this.scheduledAtGameTime);
        }
        tag.putBoolean("RecyclerPending", this.pending);
        tag.putBoolean("RecyclerSpaceNotice", this.spaceFailureNotice);
        if (this.pending) {
            tag.putBoolean("RecyclerPendingNatural", this.pendingNatural);
            tag.putLong("RecyclerPendingSequence", this.pendingTargetSequence);
            tag.putInt("RecyclerSealTicks", this.sealTicksRemaining);
            tag.putInt("RecyclerPendingTier", this.pendingTier.ordinal());
            tag.putBoolean("RecyclerPendingChanged", this.pendingChanged);
            tag.putBoolean("RecyclerPendingFailure", this.pendingFailure);
            if (this.pendingInventory != null) {
                CompoundTag pendingTag = new CompoundTag();
                ContainerHelper.saveAllItems(pendingTag, this.pendingInventory, registries);
                tag.put("RecyclerPendingInventory", pendingTag);
            }
        }
    }

    private static TransactionPlan createPlan(ServerLevel level, BlockPos origin,
                                               NonNullList<ItemStack> original, RandomSource random) {
        NonNullList<ItemStack> working = copyInventory(original);
        boolean validInput = false;
        boolean blocked = false;
        int processed = 0;
        EffectTier highest = EffectTier.NORMAL;
        outer:
        for (int slot = 0; slot < working.size() && processed < MAX_UNITS; slot++) {
            while (processed < MAX_UNITS) {
                InputProfile profile = profile(working.get(slot));
                if (profile == null) break;
                validInput = true;
                NonNullList<ItemStack> candidate = copyInventory(working);
                if (!consumeOne(candidate, slot)) break;
                RewardRoll reward = rollRewards(level, origin, profile, random);
                if (reward == null) break outer;
                if (!insertAll(candidate, reward.rewards())) {
                    blocked = true;
                    break outer;
                }
                working = candidate;
                processed++;
                highest = EffectTier.highest(highest, reward.highestTier());
            }
        }
        return new TransactionPlan(working, validInput, blocked, processed, highest);
    }

    private static @Nullable InputProfile profile(ItemStack stack) {
        if (stack.isEmpty()) return null;
        if (stack.is(ModContent1211.KNOWLEDGE_FRAGMENT_COLLECTION)
                && KnowledgeStackData1211.totalCount(stack) <= 0) return null;
        if (stack.is(ModTags1211.RECYCLER_RELIC)) {
            int level = EchoRelicProgress1211.level(stack);
            return new InputProfile(2 + level / 5, 0.25 + 0.02 * (level - 1), 0.0025 + 0.001 * (level - 1));
        }
        if (stack.is(ModTags1211.RECYCLER_ACCESSORY_RARE)) return new InputProfile(2, 0.20, 0.0);
        if (stack.is(ModTags1211.RECYCLER_ACCESSORY_UNCOMMON)) return new InputProfile(1, 0.10, 0.0);
        if (stack.is(ModTags1211.RECYCLER_ACCESSORY_COMMON)) return new InputProfile(1, 0.05, 0.0);
        if (stack.is(ModTags1211.RECYCLER_LEGACY)) return new InputProfile(1, 0.02, 0.0);
        if (stack.is(ModTags1211.RECYCLER_KNOWLEDGE)) return new InputProfile(1, 0.0, 0.0);
        return null;
    }

    private static boolean consumeOne(NonNullList<ItemStack> inventory, int slot) {
        ItemStack source = inventory.get(slot);
        if (source.is(ModContent1211.KNOWLEDGE_FRAGMENT_COLLECTION)) {
            LinkedHashMap<String, Integer> counts = KnowledgeStackData1211.collectionCounts(source);
            String id = counts.entrySet().stream().filter(entry -> entry.getValue() > 0)
                    .map(java.util.Map.Entry::getKey).findFirst().orElse("");
            if (id.isEmpty()) return false;
            int count = counts.get(id);
            if (count <= 1) counts.remove(id); else counts.put(id, count - 1);
            long remaining = KnowledgeStackData1211.totalCount(counts);
            if (remaining <= 0) inventory.set(slot, ItemStack.EMPTY);
            else if (remaining == 1) inventory.set(slot, KnowledgeStackData1211.fragment(counts.keySet().iterator().next()));
            else {
                ItemStack remainder = source.copyWithCount(1);
                KnowledgeStackData1211.writeCollection(remainder, counts, KnowledgeStackData1211.bookmark(source));
                inventory.set(slot, remainder);
            }
            return true;
        }
        source.shrink(1);
        if (source.isEmpty()) inventory.set(slot, ItemStack.EMPTY);
        return true;
    }

    private static @Nullable RewardRoll rollRewards(ServerLevel level, BlockPos origin,
                                                     InputProfile profile, RandomSource random) {
        List<ItemStack> rewards = new ArrayList<>();
        for (int roll = 0; roll < profile.commonRolls(); roll++) {
            ItemStack reward = rollPool(level, origin, COMMON_POOL, random);
            if (reward.isEmpty()) return null;
            rewards.add(reward);
        }
        EffectTier tier = EffectTier.NORMAL;
        if (random.nextDouble() < profile.rareChance()) {
            ItemStack reward = rollPool(level, origin, RARE_POOL, random);
            if (reward.isEmpty()) return null;
            rewards.add(reward);
            tier = EffectTier.RARE;
        }
        if (random.nextDouble() < profile.superRareChance()) {
            ItemStack reward = rollPool(level, origin, SUPER_POOL, random);
            if (reward.isEmpty()) return null;
            rewards.add(reward);
            tier = EffectTier.SUPER;
        }
        return new RewardRoll(rewards, tier);
    }

    private static ItemStack rollPool(ServerLevel level, BlockPos origin,
                                      ResourceKey<LootTable> key, RandomSource random) {
        LootTable table = level.getServer().reloadableRegistries().getLootTable(key);
        LootParams params = new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(origin))
                .create(LootContextParamSets.CHEST);
        List<ItemStack> generated = table.getRandomItems(params, random.nextLong());
        if (generated.isEmpty()) {
            EchoWarrior1211.LOGGER.error("Recycler pool {} returned no item.", key.location());
            return ItemStack.EMPTY;
        }
        return generated.get(0).copy();
    }

    private static boolean insertAll(NonNullList<ItemStack> inventory, List<ItemStack> rewards) {
        for (ItemStack reward : rewards) {
            ItemStack remaining = reward.copy();
            for (int slot = 0; slot < inventory.size() && !remaining.isEmpty(); slot++) {
                ItemStack existing = inventory.get(slot);
                if (existing.isEmpty() || !ItemStack.isSameItemSameComponents(existing, remaining)) continue;
                int moved = Math.min(existing.getMaxStackSize() - existing.getCount(), remaining.getCount());
                if (moved > 0) { existing.grow(moved); remaining.shrink(moved); }
            }
            for (int slot = 0; slot < inventory.size() && !remaining.isEmpty(); slot++) {
                if (!inventory.get(slot).isEmpty()) continue;
                int moved = Math.min(remaining.getMaxStackSize(), remaining.getCount());
                ItemStack inserted = remaining.copyWithCount(moved);
                inventory.set(slot, inserted);
                remaining.shrink(moved);
            }
            if (!remaining.isEmpty()) return false;
        }
        return true;
    }

    private static NonNullList<ItemStack> copyInventory(List<ItemStack> source) {
        NonNullList<ItemStack> copy = NonNullList.withSize(source.size(), ItemStack.EMPTY);
        for (int slot = 0; slot < source.size(); slot++) copy.set(slot, source.get(slot).copy());
        return copy;
    }

    private static ResourceKey<LootTable> lootTable(String path) {
        return ResourceKey.create(Registries.LOOT_TABLE, EchoWarrior1211.id(path));
    }

    private void playStartEffect(ServerLevel level) {
        Vec3 center = this.worldPosition.getCenter();
        level.sendParticles(ParticleTypes.ENCHANT, center.x, center.y + 0.65, center.z, 5, 0.28, 0.18, 0.28, 0.02);
        level.playSound(null, this.worldPosition, SoundEvents.BUNDLE_INSERT, SoundSource.BLOCKS, 0.25F, 0.65F);
    }

    public void playDebugEffect(ServerLevel level, EffectTier tier) { playEffect(level, tier); }

    private void playEffect(ServerLevel level, EffectTier tier) {
        Vec3 center = this.worldPosition.getCenter();
        switch (tier) {
            case NORMAL -> {
                level.sendParticles(ParticleTypes.SCULK_SOUL, center.x, center.y + 0.75, center.z, 7, 0.30, 0.22, 0.30, 0.015);
                level.sendParticles(ParticleTypes.ENCHANT, center.x, center.y + 0.58, center.z, 10, 0.35, 0.16, 0.35, 0.025);
                level.playSound(null, this.worldPosition, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 0.35F, 0.70F);
            }
            case RARE -> {
                level.sendParticles(ParticleTypes.SCULK_SOUL, center.x, center.y + 0.78, center.z, 13, 0.38, 0.28, 0.38, 0.025);
                level.sendParticles(ParticleTypes.ENCHANT, center.x, center.y + 0.60, center.z, 18, 0.42, 0.20, 0.42, 0.035);
                level.playSound(null, this.worldPosition, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.75F, 0.95F);
            }
            case SUPER -> {
                level.sendParticles(ParticleTypes.SCULK_SOUL, center.x, center.y + 0.82, center.z, 20, 0.44, 0.32, 0.44, 0.035);
                level.sendParticles(ParticleTypes.WAX_ON, center.x, center.y + 0.72, center.z, 16, 0.40, 0.27, 0.40, 0.04);
                level.sendParticles(ParticleTypes.END_ROD, center.x, center.y + 0.82, center.z, 7, 0.30, 0.24, 0.30, 0.025);
                level.playSound(null, this.worldPosition, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.85F, 1.18F);
            }
            case FAILURE -> {
                level.sendParticles(ParticleTypes.SMOKE, center.x, center.y + 0.66, center.z, 3, 0.22, 0.12, 0.22, 0.01);
                level.playSound(null, this.worldPosition, SoundEvents.DISPENSER_FAIL, SoundSource.BLOCKS, 0.55F, 0.72F);
                level.blockEvent(this.worldPosition, getBlockState().getBlock(), 1, 1);
                this.shakeCloseTicks = 4;
            }
        }
    }

    public enum EffectTier {
        NORMAL, RARE, SUPER, FAILURE;
        private static EffectTier byId(int id) { return id >= 0 && id < values().length ? values()[id] : NORMAL; }
        private static EffectTier highest(EffectTier a, EffectTier b) {
            if (a == SUPER || b == SUPER) return SUPER;
            if (a == RARE || b == RARE) return RARE;
            return NORMAL;
        }
    }

    private record InputProfile(int commonRolls, double rareChance, double superRareChance) { }
    private record RewardRoll(List<ItemStack> rewards, EffectTier highestTier) { }
    private record TransactionPlan(NonNullList<ItemStack> inventory, boolean hasValidInput,
                                   boolean blockedBySpace, int processedUnits, EffectTier highestTier) { }
}
