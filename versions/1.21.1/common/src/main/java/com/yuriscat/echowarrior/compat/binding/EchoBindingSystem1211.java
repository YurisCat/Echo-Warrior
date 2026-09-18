package com.yuriscat.echowarrior.compat.binding;

import com.yuriscat.echowarrior.compat.EchoHeroType1211;
import com.yuriscat.echowarrior.compat.EchoWarrior1211;
import com.yuriscat.echowarrior.compat.ModContent1211;
import com.yuriscat.echowarrior.compat.entity.EchoWarriorEntity1211;
import com.yuriscat.echowarrior.compat.entity.behavior.EchoSafeTeleport1211;
import com.yuriscat.echowarrior.compat.item.EchoAccessorySystem1211;
import com.yuriscat.echowarrior.compat.item.EchoSummonerItem1211;
import com.yuriscat.echowarrior.compat.item.EchoRelicItem1211;
import com.yuriscat.echowarrior.compat.item.EchoRelicState1211;
import com.yuriscat.echowarrior.compat.item.SummonerFuel1211;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public final class EchoBindingSystem1211 {
    private static final long PERFORMANCE_WARNING_COOLDOWN_MILLIS = 60L * 60L * 1000L;
    private static final Map<UUID, PhysicalLocation> CANONICAL_PHYSICAL_LOCATIONS = new HashMap<>();

    private EchoBindingSystem1211() {
    }

    public static void onServerStopped() {
        CANONICAL_PHYSICAL_LOCATIONS.clear();
    }

    public static void onPlayerJoin(net.minecraft.server.level.ServerPlayer player) {
        synchronizeInventory(player);
    }

    public static EchoBindingSavedData1211.Binding synchronize(ServerLevel level, ItemStack summoner) {
        EchoBindingSavedData1211 data = EchoBindingSavedData1211.get(level.getServer());
        UUID summonerId = resolvePhysicalSummonerId(level.getServer(), summoner);
        EchoBindingSavedData1211.Binding binding = data.getOrCreate(summonerId);
        if (!binding.fuelInitialized()) {
            binding.initializeFuel(SummonerFuel1211.amount(summoner));
            data.setDirty();
        }
        ItemStack physicalRelic = EchoSummonerItem1211.relicStack(summoner).copy();
        if (physicalRelic.getItem() instanceof EchoRelicItem1211
                && EchoRelicState1211.ensureInitialized(physicalRelic, level.random, level.getGameTime())) {
            EchoSummonerItem1211.setRelicStack(summoner, physicalRelic);
        }
        if (!binding.relicInitialized()) {
            binding.initializeRelic(physicalRelic);
            data.setDirty();
        } else if (!ItemStack.matches(physicalRelic, binding.relic())) {
            EchoSummonerItem1211.setRelicStack(summoner, binding.relic());
        }
        List<ItemStack> physicalAccessories = EchoSummonerItem1211.accessoryStacks(summoner);
        if (!binding.accessoriesInitialized()) {
            binding.initializeAccessories(physicalAccessories);
            data.setDirty();
        } else if (!matchingStacks(physicalAccessories, binding.accessories())) {
            EchoSummonerItem1211.setAccessoryStacks(summoner, binding.accessories());
        }
        SummonerFuel1211.setAmount(summoner, binding.fuel());
        EchoSummonerItem1211.setSpiritId(summoner, binding.active() ? binding.spiritId() : null);
        return binding;
    }

    public static void updateRelic(ServerLevel level, ItemStack summoner) {
        UUID summonerId = resolvePhysicalSummonerId(level.getServer(), summoner);
        ItemStack relic = EchoSummonerItem1211.relicStack(summoner).copy();
        if (relic.getItem() instanceof EchoRelicItem1211
                && EchoRelicState1211.ensureInitialized(relic, level.random, level.getGameTime())) {
            EchoSummonerItem1211.setRelicStack(summoner, relic);
        }
        EchoBindingSavedData1211 data = EchoBindingSavedData1211.get(level.getServer());
        EchoBindingSavedData1211.Binding binding = data.getOrCreate(summonerId);
        if (!binding.fuelInitialized()) binding.initializeFuel(SummonerFuel1211.amount(summoner));
        if (!binding.accessoriesInitialized()) {
            binding.initializeAccessories(EchoSummonerItem1211.accessoryStacks(summoner));
        }
        ItemStack previousRelic = binding.relic();
        boolean replaced = binding.relicInitialized() && !sameRelicIdentity(previousRelic, relic);
        if (replaced && binding.active()) {
            EchoWarriorEntity1211 loaded = findLoaded(level.getServer(), binding.spiritId());
            binding.deactivate();
            EchoSummonerItem1211.setSpiritId(summoner, null);
            if (loaded != null) loaded.livingEntity().discard();
        }
        binding.setRelic(relic);
        data.setDirty();
        synchronizeVisibleCopies(level.getServer(), summonerId, relic, binding.accessories());
        EchoWarriorEntity1211 spirit = findLoaded(level.getServer(), binding.spiritId());
        if (spirit != null) {
            spirit.applyRelicState(relic, false);
            EchoAccessorySystem1211.apply(spirit, binding.accessories());
        }
    }

    public static void updateAccessories(ServerLevel level, ItemStack summoner) {
        UUID summonerId = resolvePhysicalSummonerId(level.getServer(), summoner);
        List<ItemStack> accessories = EchoSummonerItem1211.accessoryStacks(summoner);
        EchoBindingSavedData1211 data = EchoBindingSavedData1211.get(level.getServer());
        EchoBindingSavedData1211.Binding binding = data.getOrCreate(summonerId);
        if (!binding.fuelInitialized()) binding.initializeFuel(SummonerFuel1211.amount(summoner));
        if (!binding.relicInitialized()) binding.initializeRelic(EchoSummonerItem1211.relicStack(summoner));
        binding.setAccessories(accessories);
        data.setDirty();
        synchronizeVisibleCopies(level.getServer(), summonerId, binding.relic(), accessories);
        EchoWarriorEntity1211 spirit = findLoaded(level.getServer(), binding.spiritId());
        if (spirit != null) EchoAccessorySystem1211.apply(spirit, accessories);
    }

    /**
     * Commits every player-editable summoner equipment slot as one transaction.
     * Updating relic and accessories separately is unsafe because each legacy
     * update mirrors the other authoritative value back into the physical stack.
     */
    public static void commitMenuContents(ServerLevel level, ItemStack summoner) {
        UUID summonerId = resolvePhysicalSummonerId(level.getServer(), summoner);
        ItemStack relic = EchoSummonerItem1211.relicStack(summoner).copy();
        if (relic.getItem() instanceof EchoRelicItem1211
                && EchoRelicState1211.ensureInitialized(relic, level.random, level.getGameTime())) {
            EchoSummonerItem1211.setRelicStack(summoner, relic);
        }
        List<ItemStack> accessories = EchoSummonerItem1211.accessoryStacks(summoner);
        EchoBindingSavedData1211 data = EchoBindingSavedData1211.get(level.getServer());
        EchoBindingSavedData1211.Binding binding = data.getOrCreate(summonerId);
        if (!binding.fuelInitialized()) binding.initializeFuel(SummonerFuel1211.amount(summoner));

        ItemStack previousRelic = binding.relic();
        boolean replaced = binding.relicInitialized() && !sameRelicIdentity(previousRelic, relic);
        if (replaced && binding.active()) {
            EchoWarriorEntity1211 loaded = findLoaded(level.getServer(), binding.spiritId());
            binding.deactivate();
            EchoSummonerItem1211.setSpiritId(summoner, null);
            if (loaded != null) loaded.livingEntity().discard();
        }

        binding.setRelic(relic);
        binding.setAccessories(accessories);
        data.setDirty();
        synchronizeVisibleCopies(level.getServer(), summonerId, relic, accessories);
        EchoWarriorEntity1211 spirit = findLoaded(level.getServer(), binding.spiritId());
        if (spirit != null) {
            spirit.applyRelicState(relic, false);
            EchoAccessorySystem1211.apply(spirit, accessories);
        }
    }

    public static ItemStack relic(ServerLevel level, UUID summonerId) {
        EchoBindingSavedData1211.Binding binding = EchoBindingSavedData1211.get(level.getServer()).get(summonerId);
        return binding == null ? ItemStack.EMPTY : binding.relic();
    }

    public static List<ItemStack> accessories(ServerLevel level, UUID summonerId) {
        EchoBindingSavedData1211.Binding binding = EchoBindingSavedData1211.get(level.getServer()).get(summonerId);
        return binding == null ? List.of() : binding.accessories();
    }

    public static UUID controllerId(ServerLevel level, UUID summonerId) {
        EchoBindingSavedData1211.Binding binding = EchoBindingSavedData1211.get(level.getServer()).get(summonerId);
        return binding == null ? null : binding.controllerId();
    }

    public static boolean isActive(ServerLevel level, UUID summonerId) {
        EchoBindingSavedData1211.Binding binding = EchoBindingSavedData1211.get(level.getServer()).get(summonerId);
        return binding != null && binding.active();
    }

    public static long generation(ServerLevel level, UUID summonerId) {
        EchoBindingSavedData1211.Binding binding = EchoBindingSavedData1211.get(level.getServer()).get(summonerId);
        return binding == null ? Long.MIN_VALUE : binding.generation();
    }

    public static long stateRevision(ServerLevel level, UUID summonerId) {
        EchoBindingSavedData1211.Binding binding = EchoBindingSavedData1211.get(level.getServer()).get(summonerId);
        return binding == null ? -1L : binding.stateRevision();
    }

    public static boolean canAddControllerEcho(MinecraftServer server, UUID controllerId, UUID excludingSummoner) {
        int maximum = EchoBindingConfig1211.maxLivingEchoesPerController();
        if (maximum <= 0) return true;
        return countActive(server, controllerId, excludingSummoner) < maximum;
    }

    public static int countActive(MinecraftServer server, UUID controllerId, UUID excludingSummoner) {
        int count = 0;
        for (EchoBindingSavedData1211.Binding binding : EchoBindingSavedData1211.get(server).bindings()) {
            if (binding.active() && controllerId.equals(binding.controllerId())
                    && (excludingSummoner == null || !binding.summonerId().equals(excludingSummoner))) {
                count++;
            }
        }
        return count;
    }

    public static void noteNewSummon(net.minecraft.server.level.ServerPlayer controller) {
        MinecraftServer server = controller.level().getServer();
        int count = countActive(server, controller.getUUID(), null);
        if (count <= 8) return;
        EchoBindingSavedData1211 data = EchoBindingSavedData1211.get(server);
        long now = System.currentTimeMillis();
        if (now - data.lastWarning(controller.getUUID()) < PERFORMANCE_WARNING_COOLDOWN_MILLIS) return;
        data.noteWarning(controller.getUUID(), now);
        controller.sendSystemMessage(Component.translatable(
                "message.echo_warrior.echo_count_performance_warning", count));
    }

    public static void persistRelic(ServerLevel level, UUID summonerId, ItemStack relic) {
        EchoBindingSavedData1211 data = EchoBindingSavedData1211.get(level.getServer());
        EchoBindingSavedData1211.Binding binding = data.get(summonerId);
        if (binding == null) return;
        binding.setRelic(relic);
        data.setDirty();
        synchronizeVisibleCopies(level.getServer(), summonerId, relic, binding.accessories());
    }

    private static void synchronizeVisibleCopies(MinecraftServer server, UUID summonerId, ItemStack relic,
                                                 List<ItemStack> accessories) {
        for (net.minecraft.server.level.ServerPlayer player : server.getPlayerList().getPlayers()) {
            for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
                ItemStack candidate = player.getInventory().getItem(slot);
                if (candidate.getItem() instanceof EchoSummonerItem1211
                        && summonerId.equals(EchoSummonerItem1211.getSummonerId(candidate))) {
                    EchoSummonerItem1211.setRelicStack(candidate, relic);
                    EchoSummonerItem1211.setAccessoryStacks(candidate, accessories);
                }
            }
        }
    }

    private static void synchronizeInventory(net.minecraft.server.level.ServerPlayer player) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.getItem() instanceof EchoSummonerItem1211) synchronize(player.serverLevel(), stack);
        }
    }

    private static UUID resolvePhysicalSummonerId(MinecraftServer server, ItemStack physicalStack) {
        UUID id = EchoSummonerItem1211.getOrCreateSummonerId(physicalStack);
        PhysicalLocation observed = locatePhysicalStack(server, physicalStack);
        PhysicalLocation canonical = CANONICAL_PHYSICAL_LOCATIONS.get(id);
        if (observed != null && (canonical == null || canonical.equals(observed)
                || !locationStillContains(server, canonical, id))) {
            CANONICAL_PHYSICAL_LOCATIONS.put(id, observed);
        } else if (observed != null && canonical != null && !canonical.equals(observed)) {
            UUID duplicateId = EchoSummonerItem1211.replaceSummonerIdForDuplicate(physicalStack);
            EchoWarrior1211.LOGGER.warn("Detected duplicate 1.21.1 Echo Summoner UUID {}; reassigned later copy to {}",
                    id, duplicateId);
            id = duplicateId;
            CANONICAL_PHYSICAL_LOCATIONS.put(id, observed);
        }
        return id;
    }

    private static PhysicalLocation locatePhysicalStack(MinecraftServer server, ItemStack physicalStack) {
        for (net.minecraft.server.level.ServerPlayer player : server.getPlayerList().getPlayers()) {
            for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
                if (player.getInventory().getItem(slot) == physicalStack) {
                    return new PhysicalLocation(player.getUUID(), slot);
                }
            }
        }
        return null;
    }

    private static boolean locationStillContains(MinecraftServer server, PhysicalLocation location, UUID summonerId) {
        net.minecraft.server.level.ServerPlayer player = server.getPlayerList().getPlayer(location.playerId());
        if (player == null || location.slot() < 0
                || location.slot() >= player.getInventory().getContainerSize()) return false;
        ItemStack stack = player.getInventory().getItem(location.slot());
        return stack.getItem() instanceof EchoSummonerItem1211
                && summonerId.equals(EchoSummonerItem1211.getSummonerId(stack));
    }

    private static ItemStack findPhysicalSummoner(net.minecraft.server.level.ServerPlayer player, UUID summonerId) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.getItem() instanceof EchoSummonerItem1211
                    && summonerId.equals(EchoSummonerItem1211.getSummonerId(stack))) return stack;
        }
        return ItemStack.EMPTY;
    }

    private static boolean matchingStacks(List<ItemStack> first, List<ItemStack> second) {
        if (first.size() != second.size()) return false;
        for (int index = 0; index < first.size(); index++) {
            if (!ItemStack.matches(first.get(index), second.get(index))) return false;
        }
        return true;
    }

    /**
     * Keeps live relic runtime state authoritative while still allowing a menu to remove or replace the relic.
     */
    public static ItemStack reconcileMenuRelicForSave(ItemStack menuRelic, ItemStack authoritativeRelic) {
        return sameRelicIdentity(menuRelic, authoritativeRelic)
                ? authoritativeRelic.copy()
                : menuRelic.copy();
    }

    private static boolean sameRelicIdentity(ItemStack first, ItemStack second) {
        if (first.isEmpty() || second.isEmpty()) return first.isEmpty() && second.isEmpty();
        if (!(first.getItem() instanceof EchoRelicItem1211) || !(second.getItem() instanceof EchoRelicItem1211)) {
            return ItemStack.isSameItemSameComponents(first, second);
        }
        String firstId = EchoRelicState1211.relicId(first);
        String secondId = EchoRelicState1211.relicId(second);
        return !firstId.isEmpty() && firstId.equals(secondId);
    }

    public static EchoWarriorEntity1211 findLoaded(MinecraftServer server, UUID spiritId) {
        if (spiritId == null) return null;
        for (ServerLevel level : server.getAllLevels()) {
            Entity entity = level.getEntity(spiritId);
            if (entity instanceof EchoWarriorEntity1211 echo && echo.livingEntity().isAlive()) return echo;
        }
        return null;
    }

    public static SpawnAttempt summonNew(ServerLevel level, Player controller, ItemStack summoner) {
        EchoBindingSavedData1211.Binding binding = synchronize(level, summoner);
        if (binding.active()) return SpawnAttempt.failed(SpawnFailure.CREATE_FAILED);
        if (!canAddControllerEcho(level.getServer(), controller.getUUID(), binding.summonerId())) {
            return SpawnAttempt.failed(SpawnFailure.LIMIT_REACHED);
        }
        return spawnAndActivate(level, controller, summoner, binding, Float.NaN);
    }

    public static boolean dismiss(ServerLevel level, ItemStack summoner) {
        EchoBindingSavedData1211 data = EchoBindingSavedData1211.get(level.getServer());
        EchoBindingSavedData1211.Binding binding = synchronize(level, summoner);
        if (!binding.active()) return false;
        EchoWarriorEntity1211 loaded = findLoaded(level.getServer(), binding.spiritId());
        binding.deactivate();
        data.setDirty();
        EchoSummonerItem1211.setSpiritId(summoner, null);
        if (loaded != null) loaded.livingEntity().discard();
        return true;
    }

    public static boolean dismiss(MinecraftServer server, UUID summonerId) {
        EchoBindingSavedData1211 data = EchoBindingSavedData1211.get(server);
        EchoBindingSavedData1211.Binding binding = data.get(summonerId);
        if (binding == null || !binding.active()) return false;
        EchoWarriorEntity1211 loaded = findLoaded(server, binding.spiritId());
        binding.deactivate();
        data.setDirty();
        if (loaded != null) loaded.livingEntity().discard();
        for (net.minecraft.server.level.ServerPlayer player : server.getPlayerList().getPlayers()) {
            for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
                ItemStack candidate = player.getInventory().getItem(slot);
                if (candidate.getItem() instanceof EchoSummonerItem1211
                        && summonerId.equals(EchoSummonerItem1211.getSummonerId(candidate))) {
                    EchoSummonerItem1211.setSpiritId(candidate, null);
                }
            }
        }
        return true;
    }

    public static boolean forceReconstruct(MinecraftServer server, UUID summonerId) {
        EchoBindingSavedData1211.Binding binding = EchoBindingSavedData1211.get(server).get(summonerId);
        if (binding == null || !binding.active()
                || binding.activityMode() != EchoRelicState1211.ActivityMode.FOLLOW.ordinal()
                || binding.controllerId() == null) return false;
        net.minecraft.server.level.ServerPlayer controller = server.getPlayerList().getPlayer(binding.controllerId());
        if (controller == null || !controller.isAlive() || controller.isSpectator()) return false;
        ItemStack summoner = findPhysicalSummoner(controller, summonerId);
        if (summoner.isEmpty()) return false;
        EchoWarriorEntity1211 loaded = findLoaded(server, binding.spiritId());
        float restoredHealth = loaded == null ? binding.health() : loaded.livingEntity().getHealth();
        if (loaded != null) track(loaded);
        SpawnAttempt attempt = spawnAndActivate(
                controller.serverLevel(), controller, summoner, binding, restoredHealth);
        if (!attempt.succeeded()) return false;
        if (loaded != null) loaded.livingEntity().discard();
        return true;
    }

    /** Permanently removes a summoner binding when the physical item is actually destroyed. */
    public static boolean destroySummoner(ServerLevel level, ItemStack summoner) {
        UUID summonerId = EchoSummonerItem1211.getSummonerId(summoner);
        return summonerId != null && destroySummoner(level, summonerId);
    }

    /** Permanently removes a summoner binding when only its stable id remains available. */
    public static boolean destroySummoner(ServerLevel level, UUID summonerId) {
        EchoBindingSavedData1211 data = EchoBindingSavedData1211.get(level.getServer());
        EchoBindingSavedData1211.Binding binding = data.get(summonerId);
        if (binding == null) return false;
        EchoWarriorEntity1211 loaded = findLoaded(level.getServer(), binding.spiritId());
        if (loaded != null) loaded.livingEntity().discard();
        data.remove(summonerId);
        CANONICAL_PHYSICAL_LOCATIONS.remove(summonerId);
        return true;
    }

    public static EchoWarriorEntity1211 recallOrReconstruct(
            ServerLevel destination,
            Player controller,
            ItemStack summoner,
            EchoBindingSavedData1211.Binding binding
    ) {
        if (!binding.active()) return null;
        EchoWarriorEntity1211 loaded = findLoaded(destination.getServer(), binding.spiritId());
        if (loaded != null && loaded.livingEntity().level() == destination) {
            loaded.bindTo(controller, binding.summonerId(), binding.generation());
            loaded.recallTo(controller);
            track(loaded);
            return loaded;
        }

        float health = loaded != null ? loaded.livingEntity().getHealth() : binding.health();
        if (loaded != null) track(loaded);
        SpawnAttempt attempt = spawnAndActivate(
                destination, controller, summoner, binding, health);
        EchoWarriorEntity1211 replacement = attempt.spirit();
        if (replacement != null && loaded != null) loaded.livingEntity().discard();
        return replacement;
    }

    private static SpawnAttempt spawnAndActivate(
            ServerLevel level,
            Player controller,
            ItemStack summoner,
            EchoBindingSavedData1211.Binding binding,
            float restoredHealth
    ) {
        EchoWarriorEntity1211 spirit = createHero(level, binding.relic());
        if (spirit == null) return SpawnAttempt.failed(SpawnFailure.CREATE_FAILED);
        LivingEntity living = spirit.livingEntity();
        if (!(living instanceof PathfinderMob pathfinder)) {
            return SpawnAttempt.failed(SpawnFailure.CREATE_FAILED);
        }
        Vec3 spawnPosition = EchoSafeTeleport1211.findSafeDestination(level, pathfinder, controller);
        if (spawnPosition == null) return SpawnAttempt.failed(SpawnFailure.NO_SAFE_POSITION);
        living.moveTo(spawnPosition.x, spawnPosition.y, spawnPosition.z, controller.getYRot(), 0.0F);
        spirit.applyRelicState(binding.relic(), false);
        EchoAccessorySystem1211.apply(spirit, binding.accessories());
        float health = Float.isFinite(restoredHealth) && restoredHealth > 0.0F
                ? Math.min(restoredHealth, living.getMaxHealth())
                : living.getMaxHealth();
        living.setHealth(health);
        living.setAbsorptionAmount(binding.absorption());
        living.setRemainingFireTicks(binding.remainingFireTicks());
        living.setTicksFrozen(binding.ticksFrozen());
        living.setAirSupply(binding.airSupply());
        spirit.readMigrationState(binding.migrationState());
        net.minecraft.nbt.CompoundTag migrationState = new net.minecraft.nbt.CompoundTag();
        spirit.writeMigrationState(migrationState);
        long generation = binding.activate(controller.getUUID(), living.getUUID(),
                level.dimension().location().toString(), living.getX(), living.getY(), living.getZ(), health,
                living.getAbsorptionAmount(), living.getRemainingFireTicks(), living.getTicksFrozen(),
                living.getAirSupply(), migrationState);
        spirit.bindTo(controller, binding.summonerId(), generation);
        if (!level.addFreshEntity(living)) {
            binding.deactivate();
            EchoBindingSavedData1211.get(level.getServer()).setDirty();
            return SpawnAttempt.failed(SpawnFailure.CREATE_FAILED);
        }
        spirit.recallTo(controller);
        track(spirit);
        EchoBindingSavedData1211.get(level.getServer()).setDirty();
        EchoSummonerItem1211.setSpiritId(summoner, living.getUUID());
        return SpawnAttempt.succeeded(spirit);
    }

    private static EchoWarriorEntity1211 createHero(ServerLevel level, ItemStack relic) {
        if (!(relic.getItem() instanceof EchoRelicItem1211 relicItem)) return null;
        return switch (relicItem.heroType()) {
            case ROMAN_LEGIONARY -> ModContent1211.ROMAN_LEGIONARY_ECHO.create(level);
            case AZTEC_WARRIOR -> ModContent1211.AZTEC_WARRIOR_ECHO.create(level);
            case GUANDAO_WARRIOR -> ModContent1211.GUANDAO_WARRIOR_ECHO.create(level);
            case JAPANESE_SAMURAI -> ModContent1211.JAPANESE_SAMURAI_ECHO.create(level);
            case EGYPTIAN_ARCHER -> ModContent1211.EGYPTIAN_ARCHER_ECHO.create(level);
        };
    }

    public static boolean validateAndTrack(EchoWarriorEntity1211 spirit) {
        LivingEntity living = spirit.livingEntity();
        if (living.isDeadOrDying()) return true;
        if (!(living.level() instanceof ServerLevel level) || spirit.getSummonerId() == null) return true;
        EchoBindingSavedData1211.Binding binding = EchoBindingSavedData1211.get(level.getServer())
                .get(spirit.getSummonerId());
        if (binding == null || !binding.active()
                || !living.getUUID().equals(binding.spiritId())
                || spirit.getBindingGeneration() != binding.generation()) {
            return false;
        }
        if (living.tickCount % 20 == 0) track(spirit);
        return true;
    }

    public static boolean validateAndSnapshot(EchoWarriorEntity1211 spirit, ServerLevel ignoredLevel) {
        return validateAndTrack(spirit);
    }

    public static void track(EchoWarriorEntity1211 spirit) {
        LivingEntity living = spirit.livingEntity();
        if (!(living.level() instanceof ServerLevel level) || spirit.getSummonerId() == null) return;
        EchoBindingSavedData1211 data = EchoBindingSavedData1211.get(level.getServer());
        EchoBindingSavedData1211.Binding binding = data.get(spirit.getSummonerId());
        if (binding == null || !living.getUUID().equals(binding.spiritId())) return;
        net.minecraft.nbt.CompoundTag migrationState = new net.minecraft.nbt.CompoundTag();
        spirit.writeMigrationState(migrationState);
        binding.track(level.dimension().location().toString(), living.getX(), living.getY(), living.getZ(),
                living.getHealth(), living.getAbsorptionAmount(), living.getRemainingFireTicks(),
                living.getTicksFrozen(), living.getAirSupply(), migrationState);
        data.setDirty();
    }

    public static void deactivate(EchoWarriorEntity1211 spirit) {
        LivingEntity living = spirit.livingEntity();
        if (!(living.level() instanceof ServerLevel level) || spirit.getSummonerId() == null) return;
        EchoBindingSavedData1211 data = EchoBindingSavedData1211.get(level.getServer());
        EchoBindingSavedData1211.Binding binding = data.get(spirit.getSummonerId());
        if (binding == null || !living.getUUID().equals(binding.spiritId())
                || spirit.getBindingGeneration() != binding.generation()) return;
        binding.deactivate();
        data.setDirty();
    }

    public static void tick(MinecraftServer server) {
        if (server.getTickCount() % 10 != 0) return;
        for (EchoBindingSavedData1211.Binding binding : EchoBindingSavedData1211.get(server).bindings()) {
            if (!binding.active() || binding.controllerId() == null
                    || binding.activityMode() != EchoRelicState1211.ActivityMode.FOLLOW.ordinal()) continue;
            net.minecraft.server.level.ServerPlayer controller = server.getPlayerList().getPlayer(binding.controllerId());
            if (controller == null || !controller.isAlive() || controller.isSpectator()) continue;
            EchoWarriorEntity1211 loaded = findLoaded(server, binding.spiritId());
            if (loaded != null && loaded.livingEntity().level() == controller.level()) continue;
            ItemStack summoner = findPhysicalSummoner(controller, binding.summonerId());
            if (summoner.isEmpty()) continue;
            float restoredHealth = loaded == null ? binding.health() : loaded.livingEntity().getHealth();
            if (loaded != null) track(loaded);
            SpawnAttempt attempt = spawnAndActivate(
                    controller.serverLevel(), controller, summoner, binding, restoredHealth);
            EchoWarriorEntity1211 replacement = attempt.spirit();
            if (replacement != null && loaded != null) loaded.livingEntity().discard();
        }
    }

    public enum SpawnFailure {
        NONE,
        CREATE_FAILED,
        NO_SAFE_POSITION,
        LIMIT_REACHED
    }

    public record SpawnAttempt(EchoWarriorEntity1211 spirit, SpawnFailure failure) {
        public static SpawnAttempt succeeded(EchoWarriorEntity1211 spirit) {
            return new SpawnAttempt(spirit, SpawnFailure.NONE);
        }

        public static SpawnAttempt failed(SpawnFailure failure) {
            return new SpawnAttempt(null, failure);
        }

        public boolean succeeded() {
            return this.spirit != null;
        }
    }

    public static int addFuel(ServerLevel level, ItemStack summoner, int amount) {
        EchoBindingSavedData1211 data = EchoBindingSavedData1211.get(level.getServer());
        EchoBindingSavedData1211.Binding binding = synchronize(level, summoner);
        binding.setFuel(binding.fuel() + Math.max(0, amount));
        SummonerFuel1211.setAmount(summoner, binding.fuel());
        data.setDirty();
        return binding.fuel();
    }

    public static boolean consumeFuel(ServerLevel level, ItemStack summoner, int amount) {
        EchoBindingSavedData1211 data = EchoBindingSavedData1211.get(level.getServer());
        EchoBindingSavedData1211.Binding binding = synchronize(level, summoner);
        if (!binding.consumeFuel(amount)) return false;
        SummonerFuel1211.setAmount(summoner, binding.fuel());
        data.setDirty();
        return true;
    }

    public static boolean consumeFuel(ServerLevel level, UUID summonerId, int amount) {
        EchoBindingSavedData1211 data = EchoBindingSavedData1211.get(level.getServer());
        EchoBindingSavedData1211.Binding binding = data.get(summonerId);
        if (binding == null || !binding.consumeFuel(amount)) return false;
        data.setDirty();
        return true;
    }

    public static boolean consumeFractionalFuel(ServerLevel level, UUID summonerId, double amount) {
        EchoBindingSavedData1211 data = EchoBindingSavedData1211.get(level.getServer());
        EchoBindingSavedData1211.Binding binding = data.get(summonerId);
        if (binding == null || !binding.consumeFractionalFuel(amount)) return false;
        data.setDirty();
        return true;
    }

    public static void setActivityMode(ServerLevel level, EchoBindingSavedData1211.Binding binding, int mode) {
        binding.setActivityMode(mode);
        EchoBindingSavedData1211.get(level.getServer()).setDirty();
        synchronizeVisibleCopies(level.getServer(), binding.summonerId(), binding.relic(), binding.accessories());
        EchoWarriorEntity1211 spirit = findLoaded(level.getServer(), binding.spiritId());
        if (spirit != null) spirit.applyBindingState(binding, true);
    }

    public static void setAlertMode(ServerLevel level, EchoBindingSavedData1211.Binding binding, int mode) {
        binding.setAlertMode(mode);
        EchoBindingSavedData1211.get(level.getServer()).setDirty();
        synchronizeVisibleCopies(level.getServer(), binding.summonerId(), binding.relic(), binding.accessories());
        EchoWarriorEntity1211 spirit = findLoaded(level.getServer(), binding.spiritId());
        if (spirit != null) spirit.applyBindingState(binding, false);
    }

    public static void toggleSkill(ServerLevel level, EchoBindingSavedData1211.Binding binding, int skill) {
        ItemStack relic = binding.relic();
        if (EchoHeroType1211.fromRelic(relic) == EchoHeroType1211.EGYPTIAN_ARCHER && skill == 1) {
            if (!EchoRelicState1211.cycleEgyptianArrowMode(relic, level.getGameTime())) return;
            binding.setRelic(relic);
        } else {
            binding.toggleSkill(skill);
        }
        EchoBindingSavedData1211.get(level.getServer()).setDirty();
        synchronizeVisibleCopies(level.getServer(), binding.summonerId(), binding.relic(), binding.accessories());
        EchoWarriorEntity1211 spirit = findLoaded(level.getServer(), binding.spiritId());
        if (spirit != null) spirit.applyBindingState(binding, false);
    }

    public static boolean consumeShieldCharge(ServerLevel level, EchoBindingSavedData1211.Binding binding, long now) {
        if (!binding.consumeShieldCharge(now)) return false;
        EchoBindingSavedData1211.get(level.getServer()).setDirty();
        return true;
    }

    public static void setLegionCooldownEnd(ServerLevel level, EchoBindingSavedData1211.Binding binding, long end) {
        binding.setLegionCooldownEnd(end);
        EchoBindingSavedData1211.get(level.getServer()).setDirty();
    }

    private record PhysicalLocation(UUID playerId, int slot) {
    }
}
