package com.yuriscat.echowarrior.binding;

import com.yuriscat.echowarrior.EchoWarrior;
import com.yuriscat.echowarrior.entity.EchoWarriorEntity;
import com.yuriscat.echowarrior.item.EchoRelicState;
import com.yuriscat.echowarrior.item.SummonerStackContents;
import com.yuriscat.echowarrior.item.SummonerFuel;
import com.yuriscat.echowarrior.item.TestEchoSummonerItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Runtime coordinator for persistent Echo bindings. */
public final class EchoBindingSystem {
	private static final long PERFORMANCE_WARNING_COOLDOWN_MILLIS = 60L * 60L * 1000L;
	private static final Map<UUID, PhysicalLocation> CANONICAL_PHYSICAL_LOCATIONS = new HashMap<>();

	private EchoBindingSystem() {
	}

	public static void initialize() {
		EchoBindingConfig.load();
	}

	public static void onServerStopped() {
		CANONICAL_PHYSICAL_LOCATIONS.clear();
	}

	public static void onPlayerJoin(ServerPlayer player) {
		synchronizeInventory(player);
	}

	public static void onLivingDeath(LivingEntity entity) {
		if (entity instanceof EchoWarriorEntity echo && entity.level() instanceof ServerLevel level) {
			terminate(level.getServer(), echo, "death");
		}
	}

	public static EchoBindingSavedData.Binding registerOrSynchronize(ServerLevel level, ItemStack stack) {
		EchoBindingSavedData data = EchoBindingSavedData.get(level.getServer());
		EchoBindingSavedData.Binding binding = resolvePhysicalBinding(level.getServer(), data, stack);
		copyAuthoritativeState(binding.summonerState(), stack);
		return binding;
	}

	public static void commitPhysicalStack(ServerLevel level, ItemStack stack) {
		if (!(stack.getItem() instanceof TestEchoSummonerItem)) return;
		EchoBindingSavedData data = EchoBindingSavedData.get(level.getServer());
		EchoBindingSavedData.Binding binding = resolvePhysicalBinding(level.getServer(), data, stack);
		binding.setSummonerState(stack);
		data.changedState(binding);
		synchronizeVisibleCopies(level.getServer(), binding);
	}

	public static ItemStack authoritativeSummoner(ServerLevel level, UUID summonerId) {
		EchoBindingSavedData.Binding binding = EchoBindingSavedData.get(level.getServer()).get(summonerId);
		return binding == null ? ItemStack.EMPTY : binding.summonerState();
	}

	public static EchoBindingSavedData.Binding binding(ServerLevel level, UUID summonerId) {
		return EchoBindingSavedData.get(level.getServer()).get(summonerId);
	}

	public static ItemStack relic(ServerLevel level, UUID summonerId) {
		return TestEchoSummonerItem.relicStack(authoritativeSummoner(level, summonerId));
	}

	public static List<ItemStack> accessories(ServerLevel level, UUID summonerId) {
		return TestEchoSummonerItem.accessoryStacks(authoritativeSummoner(level, summonerId));
	}

	public static void persistRelic(ServerLevel level, UUID summonerId, ItemStack relic) {
		EchoBindingSavedData data = EchoBindingSavedData.get(level.getServer());
		EchoBindingSavedData.Binding binding = data.get(summonerId);
		if (binding == null) return;
		ItemStack current = TestEchoSummonerItem.relicStack(binding.summonerState());
		if (ItemStack.matches(current, relic)) return;
		TestEchoSummonerItem.setRelicStack(binding.summonerState(), relic.copy());
		data.changedState(binding);
		synchronizeVisibleCopies(level.getServer(), binding);
	}

	public static boolean consumeFuel(ServerLevel level, UUID summonerId, int amount) {
		EchoBindingSavedData data = EchoBindingSavedData.get(level.getServer());
		EchoBindingSavedData.Binding binding = data.get(summonerId);
		if (binding == null || !SummonerFuel.consume(binding.summonerState(), amount)) return false;
		data.changedState(binding);
		synchronizeVisibleCopies(level.getServer(), binding);
		return true;
	}

	public static boolean consumeFractionalFuel(ServerLevel level, UUID summonerId, double amount) {
		EchoBindingSavedData data = EchoBindingSavedData.get(level.getServer());
		EchoBindingSavedData.Binding binding = data.get(summonerId);
		if (binding == null || !SummonerFuel.consumeFractional(binding.summonerState(), amount)) return false;
		data.changedState(binding);
		synchronizeVisibleCopies(level.getServer(), binding);
		return true;
	}

	public static UUID controllerId(ServerLevel level, UUID summonerId) {
		EchoBindingSavedData.Binding binding = EchoBindingSavedData.get(level.getServer()).get(summonerId);
		return binding == null ? null : binding.controllerId();
	}

	public static long generation(ServerLevel level, UUID summonerId) {
		EchoBindingSavedData.Binding binding = EchoBindingSavedData.get(level.getServer()).get(summonerId);
		return binding == null ? -1L : binding.generation();
	}

	public static boolean isActive(ServerLevel level, UUID summonerId) {
		EchoBindingSavedData.Binding binding = EchoBindingSavedData.get(level.getServer()).get(summonerId);
		return binding != null && binding.active();
	}

	public static long stateRevision(ServerLevel level, UUID summonerId) {
		EchoBindingSavedData.Binding binding = EchoBindingSavedData.get(level.getServer()).get(summonerId);
		return binding == null ? -1L : binding.stateRevision();
	}

	public static boolean canAddControllerEcho(MinecraftServer server, UUID controllerId, UUID excludingSummoner) {
		int maximum = EchoBindingConfig.maxLivingEchoesPerController();
		if (maximum <= 0) return true;
		return countActive(server, controllerId, excludingSummoner) < maximum;
	}

	public static int countActive(MinecraftServer server, UUID controllerId, UUID excludingSummoner) {
		int count = 0;
		for (EchoBindingSavedData.Binding binding : EchoBindingSavedData.get(server).bindings()) {
			if (binding.active() && controllerId.equals(binding.controllerId())
					&& !binding.summonerId().equals(excludingSummoner)) count++;
		}
		return count;
	}

	public static long activate(ServerLevel level, ItemStack physicalStack, ServerPlayer controller, LivingEntity entity) {
		EchoBindingSavedData.Binding binding = registerOrSynchronize(level, physicalStack);
		EchoBindingSavedData data = EchoBindingSavedData.get(level.getServer());
		binding.setSummonerState(physicalStack);
		binding.setControllerId(controller.getUUID());
		binding.setGeneration(binding.generation() + 1L);
		binding.setEntityId(entity.getUUID());
		binding.setActive(true);
		binding.setSnapshot(snapshot(entity));
		data.changedState(binding);
		synchronizeVisibleCopies(level.getServer(), binding);
		return binding.generation();
	}

	public static void attachReconstructedEntity(ServerPlayer controller, EchoBindingSavedData.Binding binding,
			LivingEntity entity) {
		EchoBindingSavedData data = EchoBindingSavedData.get(controller.level().getServer());
		binding.setEntityId(entity.getUUID());
		binding.setSnapshot(snapshot(entity));
		data.changed(binding);
	}

	public static boolean validateAndSnapshot(EchoWarriorEntity echo, ServerLevel level) {
		UUID summonerId = echo.getSummonerUuid();
		if (summonerId == null) return false;
		EchoBindingSavedData data = EchoBindingSavedData.get(level.getServer());
		EchoBindingSavedData.Binding binding = data.get(summonerId);
		LivingEntity entity = echo.livingEntity();
		if (binding == null || !binding.active() || binding.entityId() == null
				|| !binding.entityId().equals(entity.getUUID())
				|| binding.generation() != echo.getBindingGeneration()) return false;
		if (entity.tickCount % 20 == 0) {
			binding.setSnapshot(snapshot(entity));
			data.changed(binding);
		}
		return true;
	}

	public static EchoWarriorEntity findLoadedSpirit(MinecraftServer server, UUID summonerId) {
		EchoBindingSavedData.Binding binding = EchoBindingSavedData.get(server).get(summonerId);
		if (binding == null || !binding.active() || binding.entityId() == null) return null;
		for (ServerLevel level : server.getAllLevels()) {
			Entity entity = level.getEntity(binding.entityId());
			if (entity instanceof EchoWarriorEntity echo && echo.livingEntity().isAlive()
					&& echo.getBindingGeneration() == binding.generation()) return echo;
		}
		return null;
	}

	public static boolean dismiss(MinecraftServer server, UUID summonerId, String reason) {
		EchoBindingSavedData data = EchoBindingSavedData.get(server);
		EchoBindingSavedData.Binding binding = data.get(summonerId);
		if (binding == null || !binding.active()) return false;
		EchoWarriorEntity loaded = findLoadedSpirit(server, summonerId);
		binding.setGeneration(binding.generation() + 1L);
		binding.setActive(false);
		binding.setEntityId(null);
		data.changedState(binding);
		if (loaded != null && !loaded.livingEntity().isRemoved()) loaded.dismiss();
		EchoWarrior.LOGGER.debug("Dismissed Echo binding {} ({})", summonerId, reason);
		return true;
	}

	public static void terminate(MinecraftServer server, EchoWarriorEntity echo, String reason) {
		UUID summonerId = echo.getSummonerUuid();
		if (summonerId == null) return;
		EchoBindingSavedData data = EchoBindingSavedData.get(server);
		EchoBindingSavedData.Binding binding = data.get(summonerId);
		if (binding == null || !binding.active() || binding.generation() != echo.getBindingGeneration()) return;
		binding.setSnapshot(snapshot(echo.livingEntity()));
		binding.setGeneration(binding.generation() + 1L);
		binding.setActive(false);
		binding.setEntityId(null);
		data.changedState(binding);
		EchoWarrior.LOGGER.debug("Terminated Echo binding {} ({})", summonerId, reason);
	}

	public static boolean transferToFollowing(ServerPlayer newController, UUID summonerId) {
		MinecraftServer server = newController.level().getServer();
		EchoBindingSavedData data = EchoBindingSavedData.get(server);
		EchoBindingSavedData.Binding binding = data.get(summonerId);
		if (binding == null || !binding.active()) return false;
		if (!canAddControllerEcho(server, newController.getUUID(), summonerId)) return false;
		EchoWarriorEntity loaded = findLoadedSpirit(server, summonerId);
		if (loaded != null) binding.setSnapshot(snapshot(loaded.livingEntity()));
		ItemStack relic = TestEchoSummonerItem.relicStack(binding.summonerState());
		EchoRelicState.setActivityMode(relic, EchoRelicState.ActivityMode.FOLLOW);
		TestEchoSummonerItem.setRelicStack(binding.summonerState(), relic);
		binding.setControllerId(newController.getUUID());
		binding.setGeneration(binding.generation() + 1L);
		binding.setEntityId(null);
		data.changedState(binding);
		if (loaded != null && !loaded.livingEntity().isRemoved()) loaded.dismiss();
		synchronizeVisibleCopies(server, binding);
		TestEchoSummonerItem.reconstructFromBinding(newController, binding);
		return true;
	}

	/** Operator recovery path for an active FOLLOW binding whose live entity is missing or corrupt. */
	public static boolean forceReconstruct(MinecraftServer server, UUID summonerId) {
		EchoBindingSavedData data = EchoBindingSavedData.get(server);
		EchoBindingSavedData.Binding binding = data.get(summonerId);
		if (binding == null || !binding.active() || binding.controllerId() == null) return false;
		ItemStack relic = TestEchoSummonerItem.relicStack(binding.summonerState());
		if (relic.isEmpty() || EchoRelicState.activityMode(relic) != EchoRelicState.ActivityMode.FOLLOW) return false;
		ServerPlayer controller = server.getPlayerList().getPlayer(binding.controllerId());
		if (controller == null || !controller.isAlive() || controller.isSpectator()) return false;
		EchoWarriorEntity loaded = findLoadedSpirit(server, summonerId);
		if (loaded != null) {
			binding.setSnapshot(snapshot(loaded.livingEntity()));
			loaded.dismiss();
		}
		binding.setGeneration(binding.generation() + 1L);
		binding.setEntityId(null);
		data.changed(binding);
		return TestEchoSummonerItem.reconstructFromBinding(controller, binding);
	}

	public static void noteNewSummon(ServerPlayer controller) {
		MinecraftServer server = controller.level().getServer();
		int count = countActive(server, controller.getUUID(), null);
		if (count <= 8) return;
		EchoBindingSavedData data = EchoBindingSavedData.get(server);
		long now = System.currentTimeMillis();
		if (now - data.lastWarning(controller.getUUID()) < PERFORMANCE_WARNING_COOLDOWN_MILLIS) return;
		data.noteWarning(controller.getUUID(), now);
		controller.sendSystemMessage(Component.translatable("message.echo_warrior.echo_count_performance_warning", count));
	}

	public static int destroySummonersIn(MinecraftServer server, ItemStack root, String reason) {
		int destroyed = 0;
		for (UUID summonerId : SummonerStackContents.summonerIds(root)) {
			if (dismiss(server, summonerId, reason)) destroyed++;
		}
		return destroyed;
	}

	public static void tick(MinecraftServer server) {
		if (server.getTickCount() % 10 != 0) return;
		for (EchoBindingSavedData.Binding binding : EchoBindingSavedData.get(server).bindings()) {
			if (!binding.active() || binding.controllerId() == null) continue;
			ItemStack relic = TestEchoSummonerItem.relicStack(binding.summonerState());
			if (relic.isEmpty() || EchoRelicState.activityMode(relic) != EchoRelicState.ActivityMode.FOLLOW) continue;
			ServerPlayer controller = server.getPlayerList().getPlayer(binding.controllerId());
			if (controller == null || !controller.isAlive() || controller.isSpectator()) continue;
			EchoWarriorEntity loaded = findLoadedSpirit(server, binding.summonerId());
			if (loaded == null || loaded.livingEntity().level() != controller.level()) {
				if (loaded != null) {
					binding.setSnapshot(snapshot(loaded.livingEntity()));
					loaded.dismiss();
				}
				binding.setGeneration(binding.generation() + 1L);
				binding.setEntityId(null);
				EchoBindingSavedData.get(server).changed(binding);
				TestEchoSummonerItem.reconstructFromBinding(controller, binding);
			}
		}
	}

	private static void synchronizeInventory(ServerPlayer player) {
		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			ItemStack stack = player.getInventory().getItem(slot);
			if (stack.getItem() instanceof TestEchoSummonerItem) registerOrSynchronize(player.level(), stack);
		}
	}

	private static void synchronizeVisibleCopies(MinecraftServer server, EchoBindingSavedData.Binding binding) {
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
				ItemStack candidate = player.getInventory().getItem(slot);
				if (TestEchoSummonerItem.hasSummoner(candidate, binding.summonerId())) {
					copyAuthoritativeState(binding.summonerState(), candidate);
				}
			}
		}
	}

	private static EchoBindingSavedData.Binding resolvePhysicalBinding(
			MinecraftServer server,
			EchoBindingSavedData data,
			ItemStack physicalStack
	) {
		UUID id = TestEchoSummonerItem.getOrCreateSummonerId(physicalStack);
		PhysicalLocation observed = locatePhysicalStack(server, physicalStack);
		PhysicalLocation canonical = CANONICAL_PHYSICAL_LOCATIONS.get(id);
		if (observed != null && (canonical == null || canonical.equals(observed)
				|| !locationStillContains(server, canonical, id))) {
			// ItemStack Java identities are not stable across menu/inventory synchronization.
			// A changed object at the same slot, or a move from an emptied slot, is still
			// the same physical summoner and must keep its UUID.
			CANONICAL_PHYSICAL_LOCATIONS.put(id, observed);
		} else if (observed != null && canonical != null && !canonical.equals(observed)) {
			// Only call something a duplicate when two distinct, currently loaded player
			// inventory slots can both be proven to hold the same UUID at the same time.
			UUID duplicateId = TestEchoSummonerItem.replaceSummonerIdForDuplicate(physicalStack);
			EchoWarrior.LOGGER.warn("Detected duplicate Echo Summoner UUID {}; reassigned later copy to {}",
					id, duplicateId);
			id = duplicateId;
			CANONICAL_PHYSICAL_LOCATIONS.put(id, observed);
		}
		return data.getOrCreate(id, physicalStack);
	}

	private static PhysicalLocation locatePhysicalStack(MinecraftServer server, ItemStack physicalStack) {
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
				if (player.getInventory().getItem(slot) == physicalStack) {
					return new PhysicalLocation(player.getUUID(), slot);
				}
			}
		}
		return null;
	}

	private static boolean locationStillContains(
			MinecraftServer server,
			PhysicalLocation location,
			UUID summonerId
	) {
		ServerPlayer player = server.getPlayerList().getPlayer(location.playerId());
		if (player == null || location.slot() < 0
				|| location.slot() >= player.getInventory().getContainerSize()) return false;
		return TestEchoSummonerItem.hasSummoner(player.getInventory().getItem(location.slot()), summonerId);
	}

	private static void copyAuthoritativeState(ItemStack source, ItemStack target) {
		if (source.isEmpty() || target.isEmpty()) return;
		target.set(DataComponents.CONTAINER,
				source.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY));
		SummonerFuel.copyState(source, target);
	}

	private static EchoBindingSavedData.Snapshot snapshot(LivingEntity entity) {
		CompoundTag migrationState = new CompoundTag();
		if (entity instanceof EchoWarriorEntity echo) echo.writeMigrationState(migrationState);
		return new EchoBindingSavedData.Snapshot(
				entity.level().dimension().identifier().toString(),
				entity.getX(), entity.getY(), entity.getZ(), entity.getHealth(), entity.getAbsorptionAmount(),
				entity.getRemainingFireTicks(), entity.getTicksFrozen(), entity.getAirSupply(), migrationState);
	}

	private record PhysicalLocation(UUID playerId, int slot) {
	}
}
