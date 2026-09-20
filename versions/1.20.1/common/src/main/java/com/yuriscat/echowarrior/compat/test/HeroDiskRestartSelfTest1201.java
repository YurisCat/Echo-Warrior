package com.yuriscat.echowarrior.compat.test;

import com.mojang.authlib.GameProfile;
import com.yuriscat.echowarrior.compat.*;
import com.yuriscat.echowarrior.compat.binding.*;
import com.yuriscat.echowarrior.compat.entity.*;
import com.yuriscat.echowarrior.compat.item.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.*;

/** Opt-in, isolated dedicated world only. Entity chunks must be read by vanilla on boot two. */
public final class HeroDiskRestartSelfTest1201 {
    private static final UUID OWNER = UUID.fromString("2973becd-19cc-4b1b-8998-3b74dc072b33");
    private static boolean pending;
    private static int ticks;
    private HeroDiskRestartSelfTest1201() {}

    public static void prepare(MinecraftServer server) {
        if (!Boolean.getBoolean("echo_warrior.compat_bootstrap_test")) return;
        int boot = Integer.getInteger("echo_warrior.compat_storage_expected_boot", 0);
        var level = server.overworld();
        var data = fixture(level);
        check(boot == 1 || boot == 2, "explicit two-boot fixture");
        level.setChunkForced(4, 4, true); level.getChunk(4, 4);
        if (boot == 1) {
            check(data.state.isEmpty(), "first boot is fresh");
            for (int x = 64; x < 80; x++) for (int z = 64; z < 80; z++) {
                level.setBlock(new BlockPos(x, 95, z), Blocks.STONE.defaultBlockState(), 2);
            }
            var player = new ServerPlayer(server, level, new GameProfile(OWNER, "HeroDiskTest"));
            ListTag entries = new ListTag();
            for (var type : EchoHeroType1201.values()) {
                player.moveTo(66 + type.ordinal() * 2, 96, 70, 0, 0);
                var stack = new ItemStack(ModContent1201.ECHO_SUMMONER);
                var binding = EchoBindingSystem1201.synchronize(level, stack);
                var relic = new ItemStack(ModContent1201.relic(type));
                EchoRelicState1201.ensureInitialized(relic, player.getRandom(), level.getGameTime());
                EchoRelicState1201.setTraitsForSelfTest(relic, 0, EchoBiomeAffinity1201.OPENLAND);
                // NoAI does not stop the server's sun aura. Keep this persistence
                // fixture's HP constant without disabling real gameplay healing.
                if (type == EchoHeroType1201.AZTEC_WARRIOR && EchoRelicState1201.skillEnabled(relic, 0)) {
                    EchoRelicState1201.toggleSkill(relic, 0);
                }
                var contents = new ArrayList<>(binding.contents()); contents.set(7, relic);
                check(binding.commitEquipment(binding.stateRevision(), contents), "install disk relic");
                binding.setFuel(300); binding.mirrorTo(stack);
                var result = EchoBindingSystem1201.summonNew(level, player, stack);
                check(result.succeeded(), "spawn disk hero " + type);
                var echo = result.spirit(); var mob = (PathfinderMob)echo.livingEntity();
                mob.moveTo(player.getX(), 96, 70, 0, 0); mob.setNoAi(true); mob.setNoGravity(true);
                mob.setPersistenceRequired(); mob.setHealth(mob.getMaxHealth() - 3); mob.setAbsorptionAmount(2);
                binding.setFuel(0); EchoBindingSystem1201.track(echo); binding.mirrorTo(stack);
                var entry = new CompoundTag(); entry.put("Summoner", stack.save(new CompoundTag()));
                entry.putUUID("Entity", mob.getUUID()); entry.putLong("Generation", binding.generation());
                entry.putFloat("Health", mob.getHealth()); entry.putString("Hero", type.name()); entries.add(entry);
                if (type == EchoHeroType1201.EGYPTIAN_ARCHER) {
                    var arrow = ModContent1201.EGYPTIAN_ARCHER_ARROW.create(level);
                    arrow.configure(mob, EchoRelicState1201.EgyptianArrowMode.CONE, 7, true);
                    arrow.moveTo(70, 120, 70, 0, 0); arrow.setNoGravity(true);
                    check(level.addFreshEntity(arrow), "spawn disk arrow"); data.state.putUUID("Arrow", arrow.getUUID());
                }
            }
            data.state.put("Heroes", entries); data.setDirty();
        } else check(data.state.getList("Heroes", Tag.TAG_COMPOUND).size() == 5, "fixture manifest restored from disk");
        pending = true; ticks = 0;
    }

    public static void tick(MinecraftServer server) {
        if (!pending || !Boolean.getBoolean("echo_warrior.compat_bootstrap_test")) return;
        var level = server.overworld(); var data = fixture(level);
        int boot = Integer.getInteger("echo_warrior.compat_storage_expected_boot", 0);
        ticks++;
        var entries = data.state.getList("Heroes", Tag.TAG_COMPOUND);
        boolean allLoaded = level.getEntity(data.state.getUUID("Arrow")) != null;
        for (var tag : entries) allLoaded &= level.getEntity(((CompoundTag)tag).getUUID("Entity")) != null;
        if (!allLoaded) { check(ticks < 200, "vanilla entity chunks loaded after restart"); return; }
        if (boot == 2) {
            for (var tag : entries) {
                var entry = (CompoundTag)tag;
                var echo = (EchoWarriorEntity1201)level.getEntity(entry.getUUID("Entity"));
                var stack = ItemStack.of(entry.getCompound("Summoner"));
                var binding = EchoBindingSystem1201.synchronize(level, stack);
                check(echo.heroType().name().equals(entry.getString("Hero"))
                        && binding.matches(echo.livingEntity().getUUID(), entry.getLong("Generation"))
                        && OWNER.equals(binding.controllerId()) && EchoBindingSystem1201.validateAndTrack(echo), "disk identity and generation");
                check(echo.livingEntity().getHealth() == entry.getFloat("Health")
                        && echo.livingEntity().getAbsorptionAmount() == 2 && binding.fuel() == 0, "disk HP, absorption and fuel: "
                        + echo.heroType() + " hp=" + echo.livingEntity().getHealth() + "/" + entry.getFloat("Health")
                        + " absorption=" + echo.livingEntity().getAbsorptionAmount() + " fuel=" + binding.fuel());
                migrate(server, echo, stack, binding);
                EchoBindingSystem1201.destroySummoner(level, binding.summonerId());
            }
            var arrow = level.getEntity(data.state.getUUID("Arrow"));
            check(arrow instanceof EgyptianArcherArrowEntity1201 a
                    && a.arrowMode() == EchoRelicState1201.EgyptianArrowMode.CONE
                    && a.pickup == net.minecraft.world.entity.projectile.AbstractArrow.Pickup.DISALLOWED, "disk arrow mode and no pickup");
            arrow.tick(); check(arrow.isRemoved(), "disk arrow invalidated after owner generation removed");
            level.setChunkForced(4, 4, false);
            data.state.putBoolean("Completed", true); data.setDirty();
        }
        pending = false;
        EchoWarrior1201.LOGGER.info("[Compat1201] HERO DISK SELFTEST PASSED boot={} heroes=5 arrow=1 migration={}", boot, boot == 2 ? "actual-cross-dimension" : "pending-restart");
    }

    private static void migrate(MinecraftServer server, EchoWarriorEntity1201 old, ItemStack stack,
                                EchoBindingSavedData1201.Binding binding) {
        var destination = server.getLevel(Level.NETHER);
        var saved = new LinkedHashMap<BlockPos, BlockState>();
        try {
            for (int x = 64; x <= 78; x++) for (int z = 64; z <= 78; z++) for (int y = 95; y <= 99; y++) {
                var pos = new BlockPos(x, y, z);
                check(destination.getBlockEntity(pos) == null, "no existing migration fixture block entity");
                saved.put(pos, destination.getBlockState(pos));
                destination.setBlock(pos, y == 95 ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), 2);
            }
            var owner = new ServerPlayer(server, destination, new GameProfile(OWNER, "HeroDiskTest"));
            owner.moveTo(71, 96, 71, 0, 0);
            float health = old.livingEntity().getHealth(); long generation = binding.generation();
            var restored = EchoBindingSystem1201.recallOrReconstruct(destination, owner, stack, binding);
            check(restored != null && restored.livingEntity().level() == destination && old.livingEntity().isRemoved()
                    && binding.generation() == generation + 1 && restored.livingEntity().getHealth() == health
                    && restored.livingEntity().getAbsorptionAmount() == 2 && binding.fuel() == 0, "cross-dimension reconstruction preserves authority and health");
            check(!EchoBindingSystem1201.validateAndTrack(old), "old dimension generation cannot return");
        } finally { saved.forEach((pos, state) -> destination.setBlock(pos, state, 2)); }
    }

    public static void clear() { pending = false; ticks = 0; }
    private static Fixture fixture(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(Fixture::load, Fixture::new, "echo_warrior_test_hero_disk");
    }
    private static void check(boolean condition, String message) {
        if (!condition) throw new IllegalStateException("Hero disk restart: " + message);
    }
    private static final class Fixture extends SavedData {
        private CompoundTag state = new CompoundTag();
        private static Fixture load(CompoundTag tag) { var f = new Fixture(); f.state = tag.copy(); return f; }
        @Override public CompoundTag save(CompoundTag tag) { return state.copy(); }
    }
}
