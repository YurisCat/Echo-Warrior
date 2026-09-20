package com.yuriscat.echowarrior.compat.binding;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import com.yuriscat.echowarrior.compat.entity.EchoWarriorEntity1201;
import com.yuriscat.echowarrior.compat.item.EchoRelicState1201;
import com.yuriscat.echowarrior.compat.item.SummonerData1201;
import java.util.UUID;
import java.util.List;
import com.yuriscat.echowarrior.compat.ModContent1201;
import com.yuriscat.echowarrior.compat.EchoHeroType1201;
import com.yuriscat.echowarrior.compat.item.EchoRelicItem1201;
import com.yuriscat.echowarrior.compat.item.SummonerFuel1201;
import com.yuriscat.echowarrior.compat.entity.behavior.EchoSafeTeleport1201;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.PathfinderMob;

/** Server authority for physical inventory, active generations and transactional hero creation. */
public final class EchoBindingSystem1201 {
    private EchoBindingSystem1201() {}

    public static int countActive(MinecraftServer server, UUID controller, UUID excluding) {
        return (int)EchoBindingSavedData1201.get(server).bindings().stream()
                .filter(b -> b.active() && controller.equals(b.controllerId()) && !b.summonerId().equals(excluding)).count();
    }
    public static boolean canAddControllerEcho(MinecraftServer server, UUID controller, UUID excluding) {
        int limit = EchoBindingConfig1201.maxLivingEchoesPerController();
        return limit == 0 || countActive(server, controller, excluding) < limit;
    }

    public static SpawnAttempt summonNew(ServerLevel level, Player controller, ItemStack summoner) {
        var binding = synchronize(level, summoner);
        if (binding.active()) return SpawnAttempt.failed(SpawnFailure.CREATE_FAILED);
        if (!(binding.relic().getItem() instanceof EchoRelicItem1201)) return SpawnAttempt.failed(SpawnFailure.NO_RELIC);
        if (!canAddControllerEcho(level.getServer(), controller.getUUID(), binding.summonerId())) {
            return SpawnAttempt.failed(SpawnFailure.LIMIT_REACHED);
        }
        int cost = SummonerFuel1201.summonCost(binding.relic());
        if (binding.fuel() < cost) return SpawnAttempt.failed(SpawnFailure.NOT_ENOUGH_FUEL);
        return spawnAndActivate(level, controller, summoner, binding, false, cost);
    }

    /** Validate terrain and add the entity before changing fuel or replacing the old generation. */
    private static SpawnAttempt spawnAndActivate(ServerLevel level, Player controller, ItemStack summoner,
            EchoBindingSavedData1201.Binding binding, boolean restore, int cost) {
        EchoWarriorEntity1201 echo = createHero(level, binding.relic());
        if (echo == null || !(echo.livingEntity() instanceof PathfinderMob living)) return SpawnAttempt.failed(SpawnFailure.CREATE_FAILED);
        var destination = EchoSafeTeleport1201.findSafeDestination(level, living, controller);
        if (destination == null) return SpawnAttempt.failed(SpawnFailure.NO_SAFE_POSITION);
        living.moveTo(destination.x, destination.y, destination.z, controller.getYRot(), 0);
        long nextGeneration = Math.addExact(binding.generation(), 1);
        long expectedRevision = binding.stateRevision();
        echo.bindTo(controller, binding.summonerId(), nextGeneration);
        echo.applyBindingState(binding, true);
        var saved = binding.snapshot();
        living.setHealth(restore && saved.health() > 0 ? Math.min(saved.health(), living.getMaxHealth()) : living.getMaxHealth());
        if (restore) {
            living.setAbsorptionAmount(saved.absorption());
            living.setRemainingFireTicks(saved.fireTicks());
            living.setTicksFrozen(saved.frozenTicks());
            living.setAirSupply(saved.airSupply());
            echo.readMigrationState(saved.migrationState());
        }
        if (!level.addFreshEntity(living)) return SpawnAttempt.failed(SpawnFailure.CREATE_FAILED);
        // Loader entity-join callbacks can veto creation or modify authority synchronously.
        if (EchoBindingSavedData1201.get(level.getServer()).get(binding.summonerId()) != binding
                || binding.stateRevision() != expectedRevision || !binding.consumeFuel(cost)) {
            living.discard();
            return SpawnAttempt.failed(SpawnFailure.CREATE_FAILED);
        }
        binding.activate(controller.getUUID(), living.getUUID(), snapshot(echo));
        binding.mirrorTo(summoner);
        mirrorVisible(level.getServer(), binding);
        if (!restore && controller instanceof ServerPlayer player) noteNewSummon(player);
        return new SpawnAttempt(echo, SpawnFailure.NONE);
    }

    public static EchoWarriorEntity1201 createHero(ServerLevel level, ItemStack relic) {
        if (!(relic.getItem() instanceof EchoRelicItem1201 item)) return null;
        return switch (item.heroType()) {
            case ROMAN_LEGIONARY -> ModContent1201.ROMAN_LEGIONARY_ECHO.create(level);
            case AZTEC_WARRIOR -> ModContent1201.AZTEC_WARRIOR_ECHO.create(level);
            case GUANDAO_WARRIOR -> ModContent1201.GUANDAO_WARRIOR_ECHO.create(level);
            case JAPANESE_SAMURAI -> ModContent1201.JAPANESE_SAMURAI_ECHO.create(level);
            case EGYPTIAN_ARCHER -> ModContent1201.EGYPTIAN_ARCHER_ECHO.create(level);
        };
    }

    public static boolean dismiss(ServerLevel level, ItemStack summoner) {
        var binding = synchronize(level, summoner);
        boolean result = dismiss(level.getServer(), binding.summonerId());
        binding.mirrorTo(summoner);
        return result;
    }
    public static boolean dismiss(MinecraftServer server, UUID id) {
        var binding = EchoBindingSavedData1201.get(server).get(id);
        if (binding == null || !binding.active()) return false;
        var echo = findLoaded(server, binding.spiritId());
        if (!binding.deactivate(binding.spiritId(), binding.generation())) return false;
        if (echo != null) echo.dismiss();
        mirrorVisible(server, binding);
        return true;
    }
    public static EchoWarriorEntity1201 recallOrReconstruct(ServerLevel destination, Player controller,
            ItemStack summoner, EchoBindingSavedData1201.Binding binding) {
        if (!binding.active()) return null;
        // Recalling is movement, not implicit ownership transfer. Explicit FOLLOW is the transfer control.
        if (!controller.getUUID().equals(binding.controllerId())) return null;
        var loaded = findLoaded(destination.getServer(), binding.spiritId());
        if (loaded != null && loaded.livingEntity().level() == destination) {
            loaded.bindTo(controller, binding.summonerId(), binding.generation());
            loaded.recallTo(controller);
            binding.transferController(controller.getUUID());
            track(loaded);
            mirrorVisible(destination.getServer(), binding);
            return loaded;
        }
        if (loaded != null) track(loaded);
        var attempt = spawnAndActivate(destination, controller, summoner, binding, true, 0);
        if (attempt.succeeded() && loaded != null) loaded.dismiss();
        return attempt.spirit();
    }
    public static boolean forceReconstruct(MinecraftServer server, UUID id) {
        var binding = EchoBindingSavedData1201.get(server).get(id);
        if (binding == null || !binding.active() || binding.activityMode() != EchoRelicState1201.ActivityMode.FOLLOW.ordinal()) return false;
        var player = server.getPlayerList().getPlayer(binding.controllerId());
        if (player == null || !player.isAlive() || player.isSpectator()) return false;
        ItemStack stack = findPhysicalSummoner(player, id);
        if (stack.isEmpty()) return false;
        var loaded = findLoaded(server, binding.spiritId());
        if (loaded != null) track(loaded);
        var attempt = spawnAndActivate(player.serverLevel(), player, stack, binding, true, 0);
        if (attempt.succeeded() && loaded != null) loaded.dismiss();
        return attempt.succeeded();
    }
    private static ItemStack findPhysicalSummoner(ServerPlayer player, UUID id) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            var stack = player.getInventory().getItem(slot);
            if (id.equals(SummonerData1201.summonerId(stack))) return stack;
        }
        var cursor = player.containerMenu.getCarried();
        return id.equals(SummonerData1201.summonerId(cursor)) ? cursor : ItemStack.EMPTY;
    }
    /** Explicit transfer recreates the entity, cancelling old pending attacks/projectiles by generation. */
    public static boolean transferToFollowing(ServerPlayer player, ItemStack summoner,
            EchoBindingSavedData1201.Binding binding) {
        if (!binding.active() || !binding.summonerId().equals(SummonerData1201.summonerId(summoner))
                || !canAddControllerEcho(player.server, player.getUUID(), binding.summonerId())) return false;
        var old = findLoaded(player.server, binding.spiritId());
        if (old != null) track(old);
        var attempt = spawnAndActivate(player.serverLevel(), player, summoner, binding, true, 0);
        if (!attempt.succeeded()) return false;
        setActivityMode(player.serverLevel(), binding, EchoRelicState1201.ActivityMode.FOLLOW.ordinal());
        if (old != null) old.dismiss();
        return true;
    }
    public static void tick(MinecraftServer server) {
        if (server.getTickCount() % 10 != 0) return;
        for (var binding : EchoBindingSavedData1201.get(server).bindings()) {
            if (!binding.active() || binding.activityMode() != EchoRelicState1201.ActivityMode.FOLLOW.ordinal()) continue;
            var controller = server.getPlayerList().getPlayer(binding.controllerId());
            if (controller == null) continue;
            var loaded = findLoaded(server, binding.spiritId());
            if (loaded == null || loaded.livingEntity().level() != controller.level()) forceReconstruct(server, binding.summonerId());
        }
    }
    public static void noteNewSummon(ServerPlayer controller) {
        int count = countActive(controller.server, controller.getUUID(), null);
        if (count > 8 && EchoBindingSavedData1201.get(controller.server).tryWarning(controller.getUUID(), System.currentTimeMillis())) {
            controller.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.echo_warrior.echo_count_performance_warning", count));
        }
    }
    public static void setActivityMode(ServerLevel level, EchoBindingSavedData1201.Binding binding, int mode) {
        if (mode < 0 || mode >= EchoRelicState1201.ActivityMode.values().length) return;
        var relic = binding.relic();
        EchoRelicState1201.setActivityMode(relic, EchoRelicState1201.ActivityMode.values()[mode]);
        if (binding.persistRelic(relic)) applyChangedState(level, binding, true);
    }
    public static void setAlertMode(ServerLevel level, EchoBindingSavedData1201.Binding binding, int mode) {
        if (mode < 0 || mode >= EchoRelicState1201.AlertMode.values().length) return;
        var relic = binding.relic();
        EchoRelicState1201.setAlertMode(relic, EchoRelicState1201.AlertMode.values()[mode]);
        if (binding.persistRelic(relic)) applyChangedState(level, binding, false);
    }
    public static void toggleSkill(ServerLevel level, EchoBindingSavedData1201.Binding binding, int skill) {
        if (skill < 0 || skill >= EchoHeroType1201.fromRelic(binding.relic()).skillCount()) return;
        var relic = binding.relic();
        if (EchoHeroType1201.fromRelic(relic) == EchoHeroType1201.EGYPTIAN_ARCHER && skill == 1) {
            if (!EchoRelicState1201.cycleEgyptianArrowMode(relic, level.getGameTime())) return;
        } else EchoRelicState1201.toggleSkill(relic, skill);
        if (binding.persistRelic(relic)) applyChangedState(level, binding, false);
    }
    private static void applyChangedState(ServerLevel level, EchoBindingSavedData1201.Binding binding, boolean reset) {
        mirrorVisible(level.getServer(), binding);
        var echo = findLoaded(level.getServer(), binding.spiritId());
        if (echo != null) echo.applyBindingState(binding, reset);
    }
    public enum SpawnFailure { NONE, CREATE_FAILED, NO_RELIC, NO_SAFE_POSITION, LIMIT_REACHED, NOT_ENOUGH_FUEL }
    public record SpawnAttempt(EchoWarriorEntity1201 spirit, SpawnFailure failure) {
        public boolean succeeded() { return spirit != null; }
        public static SpawnAttempt failed(SpawnFailure failure) { return new SpawnAttempt(null, failure); }
    }

    /** Confirmed permanent destruction only; generation validity immediately rejects removed records. */
    public static boolean destroySummoner(ServerLevel level, java.util.UUID summonerId) {
        if (!level.getServer().isSameThread()) throw new IllegalStateException("Destruction off server thread");
        var binding = EchoBindingSavedData1201.get(level.getServer()).get(summonerId);
        if (binding == null) return false;
        var loaded = findLoaded(level.getServer(), binding.spiritId());
        boolean removed = EchoBindingSavedData1201.get(level.getServer()).remove(summonerId);
        if (loaded != null) loaded.dismiss();
        return removed;
    }

    public static ItemStack relic(ServerLevel level, UUID id) {
        var binding = EchoBindingSavedData1201.get(level.getServer()).get(id);
        return binding == null ? ItemStack.EMPTY : binding.relic();
    }
    public static List<ItemStack> accessories(ServerLevel level, UUID id) {
        var binding = EchoBindingSavedData1201.get(level.getServer()).get(id);
        return binding == null ? List.of() : binding.accessories();
    }
    public static UUID controllerId(ServerLevel level, UUID id) {
        var binding = EchoBindingSavedData1201.get(level.getServer()).get(id);
        return binding == null ? null : binding.controllerId();
    }
    public static boolean isActive(ServerLevel level, UUID id) {
        var binding = EchoBindingSavedData1201.get(level.getServer()).get(id);
        return binding != null && binding.active();
    }
    public static long generation(ServerLevel level, UUID id) {
        var binding = EchoBindingSavedData1201.get(level.getServer()).get(id);
        return binding == null ? -1 : binding.generation();
    }
    public static void persistRelic(ServerLevel level, UUID id, ItemStack relic) {
        var binding = EchoBindingSavedData1201.get(level.getServer()).get(id);
        if (binding != null && binding.persistRelic(relic)) mirrorVisible(level.getServer(), binding);
    }
    private static void mirrorVisible(MinecraftServer server, EchoBindingSavedData1201.Binding binding) {
        for (var player : server.getPlayerList().getPlayers()) {
            for (var stack : player.inventoryMenu.getItems()) {
                if (binding.summonerId().equals(SummonerData1201.summonerId(stack))) binding.mirrorTo(stack);
            }
            var cursor = player.containerMenu.getCarried();
            if (binding.summonerId().equals(SummonerData1201.summonerId(cursor))) binding.mirrorTo(cursor);
        }
    }
    public static EchoWarriorEntity1201 findLoaded(MinecraftServer server, UUID spiritId) {
        if (spiritId == null) return null;
        for (var level : server.getAllLevels()) {
            var entity = level.getEntity(spiritId);
            if (entity instanceof EchoWarriorEntity1201 echo && echo.livingEntity().isAlive()) return echo;
        }
        return null;
    }
    public static boolean validateAndTrack(EchoWarriorEntity1201 echo) {
        var living = echo.livingEntity();
        if (!(living.level() instanceof ServerLevel level) || echo.getSummonerId() == null || living.isDeadOrDying()) return true;
        var binding = EchoBindingSavedData1201.get(level.getServer()).get(echo.getSummonerId());
        if (binding == null || !binding.matches(living.getUUID(), echo.getBindingGeneration())) return false;
        if (living.tickCount % 20 == 0) track(echo);
        return true;
    }
    public static boolean validateAndSnapshot(EchoWarriorEntity1201 echo, ServerLevel level) { return validateAndTrack(echo); }
    public static EchoBindingSavedData1201.SpiritSnapshot snapshot(EchoWarriorEntity1201 echo) {
        var living = echo.livingEntity();
        var state = new net.minecraft.nbt.CompoundTag();
        echo.writeMigrationState(state);
        return new EchoBindingSavedData1201.SpiritSnapshot(living.level().dimension().location().toString(),
                living.getX(), living.getY(), living.getZ(), living.getHealth(), living.getAbsorptionAmount(),
                living.getRemainingFireTicks(), living.getTicksFrozen(), living.getAirSupply(), state);
    }
    public static void track(EchoWarriorEntity1201 echo) {
        if (!(echo.livingEntity().level() instanceof ServerLevel level)) return;
        var binding = EchoBindingSavedData1201.get(level.getServer()).get(echo.getSummonerId());
        if (binding != null) binding.track(echo.livingEntity().getUUID(), echo.getBindingGeneration(), snapshot(echo));
    }
    public static void deactivate(EchoWarriorEntity1201 echo) {
        if (!(echo.livingEntity().level() instanceof ServerLevel level)) return;
        var binding = EchoBindingSavedData1201.get(level.getServer()).get(echo.getSummonerId());
        if (binding != null && binding.deactivate(echo.livingEntity().getUUID(), echo.getBindingGeneration())) mirrorVisible(level.getServer(), binding);
    }
    public static boolean consumeFractionalFuel(ServerLevel level, UUID id, double amount) {
        var binding = EchoBindingSavedData1201.get(level.getServer()).get(id);
        return binding != null && binding.consumeFractionalFuel(amount);
    }
    public static boolean consumeShieldCharge(ServerLevel level, EchoBindingSavedData1201.Binding binding, long now) {
        var relic = binding.relic();
        if (!EchoRelicState1201.consumeShieldCharge(relic, now)) return false;
        return binding.persistRelic(relic);
    }
    public static void setLegionCooldownEnd(ServerLevel level, EchoBindingSavedData1201.Binding binding, long end) {
        var relic = binding.relic();
        EchoRelicState1201.setLegionCooldownEnd(relic, end);
        binding.persistRelic(relic);
    }

    public static EchoBindingSavedData1201.Binding synchronize(ServerLevel level, ItemStack stack) {
        MinecraftServer server = level.getServer();
        if (!server.isSameThread()) throw new IllegalStateException("Binding update off the server thread");
        EchoBindingSavedData1201 data = EchoBindingSavedData1201.get(server);
        data.identities().resolve(stack, locate(server, stack), location -> lookup(server, location));
        EchoBindingSavedData1201.Binding binding = data.register(stack);
        binding.mirrorTo(stack);
        return binding;
    }

    private static SummonerIdentityTracker1201.Location locate(MinecraftServer server, ItemStack stack) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
                if (player.getInventory().getItem(slot) == stack) {
                    return new SummonerIdentityTracker1201.Location(player.getUUID(), slot);
                }
            }
            if (player.containerMenu.getCarried() == stack) {
                return new SummonerIdentityTracker1201.Location(player.getUUID(), -1);
            }
        }
        return null;
    }

    private static ItemStack lookup(MinecraftServer server, SummonerIdentityTracker1201.Location location) {
        ServerPlayer player = server.getPlayerList().getPlayer(location.playerId());
        if (player == null) return ItemStack.EMPTY;
        if (location.slot() == -1) return player.containerMenu.getCarried();
        return location.slot() >= 0 && location.slot() < player.getInventory().getContainerSize()
                ? player.getInventory().getItem(location.slot()) : ItemStack.EMPTY;
    }
}
