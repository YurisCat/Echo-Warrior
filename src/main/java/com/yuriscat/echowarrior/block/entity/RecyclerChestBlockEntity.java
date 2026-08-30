package com.yuriscat.echowarrior.block.entity;

import com.yuriscat.echowarrior.EchoWarrior;
import com.yuriscat.echowarrior.ModBlockEntities;
import com.yuriscat.echowarrior.ModItems;
import com.yuriscat.echowarrior.ModTags;
import com.yuriscat.echowarrior.item.EchoRelicProgress;
import com.yuriscat.echowarrior.knowledge.KnowledgeStackData;
import com.yuriscat.echowarrior.menu.RecyclerMenu;
import com.yuriscat.echowarrior.recycler.RecyclerSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public final class RecyclerChestBlockEntity extends ChestBlockEntity {
	private static final Component DEFAULT_NAME = Component.translatable("container.echo_warrior.echo_recycler");
	private static final int SEAL_TICKS = 40;
	private static final int MAX_UNITS_PER_TRANSACTION = 4_096;
	private static final long UNINITIALIZED_SEQUENCE = Long.MIN_VALUE;
	private static final ResourceKey<LootTable> COMMON_POOL = lootTable("gameplay/recycler/common");
	private static final ResourceKey<LootTable> RARE_POOL = lootTable("gameplay/recycler/rare");
	private static final ResourceKey<LootTable> SUPER_RARE_POOL = lootTable("gameplay/recycler/super_rare");

	private long lastProcessedSequence = UNINITIALIZED_SEQUENCE;
	private long scheduledSequence = UNINITIALIZED_SEQUENCE;
	private long scheduledAtGameTime;
	private boolean pendingActive;
	private boolean pendingNatural;
	private long pendingTargetSequence;
	private int sealTicksRemaining;
	private NonNullList<ItemStack> pendingInventory;
	private EffectTier pendingTier = EffectTier.NORMAL;
	private boolean pendingChanged;
	private boolean pendingFailure;
	private boolean spaceFailureNotice;
	private int shakeCloseTicks;

	public RecyclerChestBlockEntity(BlockPos worldPosition, BlockState blockState) {
		super(ModBlockEntities.RECYCLER_CHEST, worldPosition, blockState);
	}

	public static void clientTick(Level level, BlockPos pos, BlockState state, RecyclerChestBlockEntity entity) {
		ChestBlockEntity.lidAnimateTick(level, pos, state, entity);
	}

	public static void serverTick(Level level, BlockPos pos, BlockState state, RecyclerChestBlockEntity entity) {
		if (!(level instanceof ServerLevel serverLevel)) return;
		entity.tickServer(serverLevel);
	}

	private void tickServer(ServerLevel level) {
		if (this.shakeCloseTicks > 0 && --this.shakeCloseTicks == 0) {
			level.blockEvent(this.worldPosition, this.getBlockState().getBlock(), 1, 0);
		}

		if (this.pendingActive) {
			if (this.sealTicksRemaining > 0) this.sealTicksRemaining--;
			if (this.sealTicksRemaining <= 0) finishPending(level);
			return;
		}

		long currentSequence = RecyclerSystem.currentMidnightSequence(level);
		if (this.lastProcessedSequence == UNINITIALIZED_SEQUENCE) {
			this.lastProcessedSequence = currentSequence;
			this.scheduledSequence = UNINITIALIZED_SEQUENCE;
			setChanged();
			return;
		}

		if (this.scheduledSequence != UNINITIALIZED_SEQUENCE) {
			if (this.lastProcessedSequence >= currentSequence) {
				clearSchedule();
				return;
			}
			if (level.getGameTime() >= this.scheduledAtGameTime) {
				long target = Math.max(this.scheduledSequence, currentSequence);
				clearSchedule();
				beginTransaction(level, true, target);
			}
			return;
		}

		if (this.lastProcessedSequence < currentSequence) {
			this.scheduledSequence = currentSequence;
			this.scheduledAtGameTime = level.getGameTime() + positionDelay(this.worldPosition);
			setChanged();
		}
	}

	public boolean triggerManual(ServerLevel level) {
		if (this.pendingActive) return false;
		return beginTransaction(level, false, UNINITIALIZED_SEQUENCE);
	}

	private boolean beginTransaction(ServerLevel level, boolean natural, long targetSequence) {
		if (this.pendingActive) return false;
		TransactionPlan plan = createPlan(level, this.worldPosition, copyInventory(this.getItems()), level.getRandom());
		if (!plan.hasValidInput()) {
			if (natural) this.lastProcessedSequence = Math.max(this.lastProcessedSequence, targetSequence);
			setChanged();
			return false;
		}

		this.pendingActive = true;
		closeViewers();
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
			for (int slot = 0; slot < this.getContainerSize(); slot++) {
				this.getItems().set(slot, this.pendingInventory.get(slot).copy());
			}
			playEffect(level, this.pendingTier);
		} else if (this.pendingFailure) {
			this.spaceFailureNotice = true;
			playEffect(level, EffectTier.FAILURE);
		}

		if (this.pendingNatural) {
			long current = RecyclerSystem.currentMidnightSequence(level);
			this.lastProcessedSequence = Math.max(this.lastProcessedSequence, Math.max(this.pendingTargetSequence, current));
		}
		clearPending();
		setChanged();
	}

	private void clearSchedule() {
		this.scheduledSequence = UNINITIALIZED_SEQUENCE;
		this.scheduledAtGameTime = 0L;
		setChanged();
	}

	private void clearPending() {
		this.pendingActive = false;
		this.pendingNatural = false;
		this.pendingTargetSequence = 0L;
		this.sealTicksRemaining = 0;
		this.pendingInventory = null;
		this.pendingTier = EffectTier.NORMAL;
		this.pendingChanged = false;
		this.pendingFailure = false;
	}

	private void closeViewers() {
		List<ContainerUser> viewers = List.copyOf(this.getEntitiesWithContainerOpen());
		for (ContainerUser viewer : viewers) {
			if (viewer.getLivingEntity() instanceof ServerPlayer player) player.closeContainer();
		}
	}

	public boolean isSealed() {
		return this.pendingActive;
	}

	@Override
	public boolean canOpen(Player player) {
		return !this.pendingActive && super.canOpen(player);
	}

	@Override
	public boolean stillValid(Player player) {
		return !this.pendingActive && super.stillValid(player);
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		return !this.pendingActive;
	}

	@Override
	public boolean canTakeItem(Container into, int slot, ItemStack stack) {
		return !this.pendingActive;
	}

	@Override
	public ItemStack removeItem(int slot, int count) {
		return this.pendingActive ? ItemStack.EMPTY : super.removeItem(slot, count);
	}

	@Override
	public ItemStack removeItemNoUpdate(int slot) {
		return this.pendingActive ? ItemStack.EMPTY : super.removeItemNoUpdate(slot);
	}

	@Override
	public void setItem(int slot, ItemStack stack) {
		if (!this.pendingActive) super.setItem(slot, stack);
	}

	@Override
	public void clearContent() {
		if (!this.pendingActive) super.clearContent();
	}

	@Override
	protected Component getDefaultName() {
		return DEFAULT_NAME;
	}

	@Override
	public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
		if (!this.canOpen(player)) {
			BaseContainerBlockEntity.sendChestLockedNotifications(this.getBlockPos().getCenter(), player, this.getDisplayName());
			return null;
		}
		if (this.spaceFailureNotice && player instanceof ServerPlayer serverPlayer) {
			serverPlayer.sendOverlayMessage(Component.translatable("message.echo_warrior.recycler.space_failure"));
			this.spaceFailureNotice = false;
			setChanged();
		}
		return createMenu(containerId, inventory);
	}

	@Override
	protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
		return new RecyclerMenu(containerId, inventory, this);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		this.lastProcessedSequence = input.getLongOr("RecyclerLastSequence", UNINITIALIZED_SEQUENCE);
		this.scheduledSequence = input.getLongOr("RecyclerScheduledSequence", UNINITIALIZED_SEQUENCE);
		this.scheduledAtGameTime = input.getLongOr("RecyclerScheduledAt", 0L);
		this.pendingActive = input.getBooleanOr("RecyclerPending", false);
		this.pendingNatural = input.getBooleanOr("RecyclerPendingNatural", false);
		this.pendingTargetSequence = input.getLongOr("RecyclerPendingSequence", 0L);
		this.sealTicksRemaining = Math.max(0, input.getIntOr("RecyclerSealTicks", 0));
		this.pendingTier = EffectTier.byId(input.getIntOr("RecyclerPendingTier", 0));
		this.pendingChanged = input.getBooleanOr("RecyclerPendingChanged", false);
		this.pendingFailure = input.getBooleanOr("RecyclerPendingFailure", false);
		this.spaceFailureNotice = input.getBooleanOr("RecyclerSpaceNotice", false);
		this.pendingInventory = null;
		if (this.pendingActive) {
			this.pendingInventory = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
			ContainerHelper.loadAllItems(input.childOrEmpty("RecyclerPendingInventory"), this.pendingInventory);
			if (this.sealTicksRemaining <= 0) this.sealTicksRemaining = 1;
		}
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putLong("RecyclerLastSequence", this.lastProcessedSequence);
		if (this.scheduledSequence != UNINITIALIZED_SEQUENCE) {
			output.putLong("RecyclerScheduledSequence", this.scheduledSequence);
			output.putLong("RecyclerScheduledAt", this.scheduledAtGameTime);
		}
		output.putBoolean("RecyclerPending", this.pendingActive);
		if (this.pendingActive) {
			output.putBoolean("RecyclerPendingNatural", this.pendingNatural);
			output.putLong("RecyclerPendingSequence", this.pendingTargetSequence);
			output.putInt("RecyclerSealTicks", this.sealTicksRemaining);
			output.putInt("RecyclerPendingTier", this.pendingTier.ordinal());
			output.putBoolean("RecyclerPendingChanged", this.pendingChanged);
			output.putBoolean("RecyclerPendingFailure", this.pendingFailure);
			if (this.pendingInventory != null) {
				ContainerHelper.saveAllItems(output.child("RecyclerPendingInventory"), this.pendingInventory);
			}
		}
		output.putBoolean("RecyclerSpaceNotice", this.spaceFailureNotice);
	}

	private static TransactionPlan createPlan(
			ServerLevel level,
			BlockPos origin,
			NonNullList<ItemStack> original,
			RandomSource random
	) {
		NonNullList<ItemStack> working = copyInventory(original);
		boolean validInput = false;
		boolean blockedBySpace = false;
		int processed = 0;
		EffectTier highestTier = EffectTier.NORMAL;

		outer:
		for (int slot = 0; slot < working.size() && processed < MAX_UNITS_PER_TRANSACTION; slot++) {
			while (processed < MAX_UNITS_PER_TRANSACTION) {
				ItemStack source = working.get(slot);
				InputProfile profile = profile(source);
				if (profile == null) break;
				validInput = true;

				NonNullList<ItemStack> candidate = copyInventory(working);
				if (!consumeOne(candidate, slot)) break;
				RewardRoll rewardRoll = rollRewards(level, origin, profile, random);
				if (rewardRoll == null) break outer;
				if (!insertAll(candidate, rewardRoll.rewards())) {
					blockedBySpace = true;
					break outer;
				}

				working = candidate;
				processed++;
				highestTier = EffectTier.highest(highestTier, rewardRoll.highestTier());
			}
		}

		return new TransactionPlan(working, validInput, blockedBySpace, processed, highestTier);
	}

	private static @Nullable InputProfile profile(ItemStack stack) {
		if (stack.isEmpty()) return null;
		if (stack.is(ModItems.KNOWLEDGE_FRAGMENT_COLLECTION)
				&& KnowledgeStackData.totalCount(KnowledgeStackData.collectionCounts(stack)) <= 0) {
			return null;
		}
		if (stack.is(ModTags.RECYCLER_RELIC)) {
			int level = EchoRelicProgress.level(stack);
			return new InputProfile(2 + level / 5, 0.25 + 0.02 * (level - 1), 0.0025 + 0.001 * (level - 1));
		}
		if (stack.is(ModTags.RECYCLER_ACCESSORY_RARE)) return new InputProfile(2, 0.20, 0.0);
		if (stack.is(ModTags.RECYCLER_ACCESSORY_UNCOMMON)) return new InputProfile(1, 0.10, 0.0);
		if (stack.is(ModTags.RECYCLER_ACCESSORY_COMMON)) return new InputProfile(1, 0.05, 0.0);
		if (stack.is(ModTags.RECYCLER_LEGACY)) return new InputProfile(1, 0.02, 0.0);
		if (stack.is(ModTags.RECYCLER_KNOWLEDGE)) return new InputProfile(1, 0.0, 0.0);
		return null;
	}

	private static boolean consumeOne(NonNullList<ItemStack> inventory, int slot) {
		ItemStack source = inventory.get(slot);
		if (source.is(ModItems.KNOWLEDGE_FRAGMENT_COLLECTION)) {
			LinkedHashMap<String, Integer> counts = KnowledgeStackData.collectionCounts(source);
			String consumedId = counts.entrySet().stream()
					.filter(entry -> entry.getValue() > 0)
					.map(java.util.Map.Entry::getKey)
					.findFirst()
					.orElse("");
			if (consumedId.isEmpty()) return false;
			int count = counts.get(consumedId);
			if (count <= 1) counts.remove(consumedId);
			else counts.put(consumedId, count - 1);
			long remaining = KnowledgeStackData.totalCount(counts);
			if (remaining <= 0) {
				inventory.set(slot, ItemStack.EMPTY);
			} else if (remaining == 1) {
				inventory.set(slot, KnowledgeStackData.fragment(counts.keySet().iterator().next()));
			} else {
				ItemStack remainder = source.copy();
				remainder.setCount(1);
				KnowledgeStackData.writeCollection(remainder, counts, KnowledgeStackData.bookmark(source));
				inventory.set(slot, remainder);
			}
			return true;
		}

		source.shrink(1);
		if (source.isEmpty()) inventory.set(slot, ItemStack.EMPTY);
		return true;
	}

	private static @Nullable RewardRoll rollRewards(
			ServerLevel level,
			BlockPos origin,
			InputProfile profile,
			RandomSource random
	) {
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
			ItemStack reward = rollPool(level, origin, SUPER_RARE_POOL, random);
			if (reward.isEmpty()) return null;
			rewards.add(reward);
			tier = EffectTier.SUPER;
		}
		return new RewardRoll(rewards, tier);
	}

	private static ItemStack rollPool(
			ServerLevel level,
			BlockPos origin,
			ResourceKey<LootTable> key,
			RandomSource random
	) {
		LootTable table = level.getServer().reloadableRegistries().getLootTable(key);
		LootParams params = new LootParams.Builder(level)
				.withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(origin))
				.create(LootContextParamSets.CHEST);
		List<ItemStack> generated = table.getRandomItems(params, random.nextLong());
		if (generated.isEmpty()) {
			EchoWarrior.LOGGER.error("Recycler pool {} returned no item.", key.identifier());
			return ItemStack.EMPTY;
		}
		if (generated.size() > 1) {
			EchoWarrior.LOGGER.warn("Recycler pool {} returned {} stacks; only the first is used.", key.identifier(), generated.size());
		}
		return generated.getFirst().copy();
	}

	private static boolean insertAll(NonNullList<ItemStack> inventory, List<ItemStack> rewards) {
		for (ItemStack reward : rewards) {
			ItemStack remaining = reward.copy();
			for (int slot = 0; slot < inventory.size() && !remaining.isEmpty(); slot++) {
				ItemStack existing = inventory.get(slot);
				if (existing.isEmpty() || !ItemStack.isSameItemSameComponents(existing, remaining)) continue;
				int room = existing.getMaxStackSize() - existing.getCount();
				if (room <= 0) continue;
				int moved = Math.min(room, remaining.getCount());
				existing.grow(moved);
				remaining.shrink(moved);
			}
			for (int slot = 0; slot < inventory.size() && !remaining.isEmpty(); slot++) {
				if (!inventory.get(slot).isEmpty()) continue;
				int moved = Math.min(remaining.getMaxStackSize(), remaining.getCount());
				ItemStack inserted = remaining.copy();
				inserted.setCount(moved);
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

	private static int positionDelay(BlockPos pos) {
		return Math.floorMod(Long.hashCode(pos.asLong()), 21);
	}

	private static ResourceKey<LootTable> lootTable(String path) {
		return ResourceKey.create(Registries.LOOT_TABLE, EchoWarrior.id(path));
	}

	private void playStartEffect(ServerLevel level) {
		Vec3 center = this.worldPosition.getCenter();
		level.sendParticles(ParticleTypes.ENCHANT, center.x(), center.y() + 0.65, center.z(), 5, 0.28, 0.18, 0.28, 0.02);
		level.playSound(null, this.worldPosition, SoundEvents.BUNDLE_INSERT, SoundSource.BLOCKS, 0.25F, 0.65F);
	}

	public void playDebugEffect(ServerLevel level, EffectTier tier) {
		playEffect(level, tier);
	}

	private void playEffect(ServerLevel level, EffectTier tier) {
		Vec3 center = this.worldPosition.getCenter();
		switch (tier) {
			case NORMAL -> {
				level.sendParticles(ParticleTypes.SCULK_SOUL, center.x(), center.y() + 0.75, center.z(), 7, 0.30, 0.22, 0.30, 0.015);
				level.sendParticles(ParticleTypes.ENCHANT, center.x(), center.y() + 0.58, center.z(), 10, 0.35, 0.16, 0.35, 0.025);
				level.playSound(null, this.worldPosition, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 0.35F, 0.70F);
			}
			case RARE -> {
				level.sendParticles(ParticleTypes.SCULK_SOUL, center.x(), center.y() + 0.78, center.z(), 13, 0.38, 0.28, 0.38, 0.025);
				level.sendParticles(ParticleTypes.ENCHANT, center.x(), center.y() + 0.60, center.z(), 18, 0.42, 0.20, 0.42, 0.035);
				level.playSound(null, this.worldPosition, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.75F, 0.95F);
			}
			case SUPER -> {
				level.sendParticles(ParticleTypes.SCULK_SOUL, center.x(), center.y() + 0.82, center.z(), 20, 0.44, 0.32, 0.44, 0.035);
				level.sendParticles(ParticleTypes.WAX_ON, center.x(), center.y() + 0.72, center.z(), 16, 0.40, 0.27, 0.40, 0.04);
				level.sendParticles(ParticleTypes.END_ROD, center.x(), center.y() + 0.82, center.z(), 7, 0.30, 0.24, 0.30, 0.025);
				level.playSound(null, this.worldPosition, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.85F, 1.18F);
			}
			case FAILURE -> {
				level.sendParticles(ParticleTypes.SMOKE, center.x(), center.y() + 0.66, center.z(), 3, 0.22, 0.12, 0.22, 0.01);
				level.playSound(null, this.worldPosition, SoundEvents.DISPENSER_FAIL, SoundSource.BLOCKS, 0.55F, 0.72F);
				level.blockEvent(this.worldPosition, this.getBlockState().getBlock(), 1, 1);
				this.shakeCloseTicks = 4;
			}
		}
	}

	public enum EffectTier {
		NORMAL,
		RARE,
		SUPER,
		FAILURE;

		private static EffectTier byId(int id) {
			return id >= 0 && id < values().length ? values()[id] : NORMAL;
		}

		private static EffectTier highest(EffectTier first, EffectTier second) {
			if (first == SUPER || second == SUPER) return SUPER;
			if (first == RARE || second == RARE) return RARE;
			return NORMAL;
		}
	}

	private record InputProfile(int commonRolls, double rareChance, double superRareChance) {
	}

	private record RewardRoll(List<ItemStack> rewards, EffectTier highestTier) {
	}

	private record TransactionPlan(
			NonNullList<ItemStack> inventory,
			boolean hasValidInput,
			boolean blockedBySpace,
			int processedUnits,
			EffectTier highestTier
	) {
	}
}
