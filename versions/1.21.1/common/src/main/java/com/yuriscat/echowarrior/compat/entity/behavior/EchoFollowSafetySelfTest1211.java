package com.yuriscat.echowarrior.compat.entity.behavior;

import com.mojang.authlib.GameProfile;
import com.yuriscat.echowarrior.compat.entity.EchoWarriorEntity1211;
import com.yuriscat.echowarrior.compat.entity.RomanLegionaryEchoEntity1211;
import com.yuriscat.echowarrior.compat.item.EchoRelicState1211;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

/** Real registered heroes and production movement callbacks, without a client or player save. */
public final class EchoFollowSafetySelfTest1211 {
    private EchoFollowSafetySelfTest1211() { }

    public static void run(ServerLevel level) {
        var blocks = new LinkedHashMap<BlockPos, BlockState>();
        var fixtures = new ArrayList<PathfinderMob>();
        var owner = new ServerPlayer(level.getServer(), level,
                new GameProfile(UUID.randomUUID(), "AirFollowTest"),
                net.minecraft.server.level.ClientInformation.createDefault());
        try {
            // This production callback used to teleport directly to an airborne owner at >24 blocks.
            var roman = (RomanLegionaryEchoEntity1211)create(level, "roman_legionary", fixtures);
            roman.setPos(40.5, 96, 40.5);
            owner.setPos(40.5, 136, 40.5);
            owner.setOnGround(false);
            var boundary = roman.getClass().getDeclaredMethod("enforceActivityBoundary", LivingEntity.class);
            boundary.setAccessible(true);
            Vec3 before = roman.position();
            boundary.invoke(roman, owner);
            check(roman.position().equals(before), "activity boundary teleported Roman into unsupported air");

            // A known, isolated platform above the flat world's terrain; restore every block afterwards.
            for (int x = 34; x <= 46; x++) for (int z = 34; z <= 46; z++)
                for (int y = 95; y <= 139; y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    blocks.put(pos, level.getBlockState(pos));
                    level.setBlock(pos, y == 95 ? Blocks.STONE.defaultBlockState()
                            : Blocks.AIR.defaultBlockState(), 2);
                }
            for (String hero : List.of("roman_legionary", "aztec_warrior", "egyptian_archer",
                    "guandao_warrior", "japanese_samurai")) {
                PathfinderMob mob = hero.equals("roman_legionary") ? roman : create(level, hero, fixtures);
                EchoWarriorEntity1211 echo = (EchoWarriorEntity1211)mob;
                ItemStack relic = new ItemStack(BuiltInRegistries.ITEM.get(
                        net.minecraft.resources.ResourceLocation.parse("echo_warrior:" + hero + "_relic")));
                echo.bindTo(owner, UUID.randomUUID());
                echo.applyRelicState(relic, false);
                Runnable follow = movementTick(level, mob, owner);
                check(echo.shouldFollowOwner(), hero + " FOLLOW enabled");
                if (hero.equals("roman_legionary")) {
                    mob.setPos(0.5, 96, 40.5);
                    for (String fieldName : List.of("shieldChargeTarget", "mutualGazePlayerId")) {
                        var field = mob.getClass().getDeclaredField(fieldName);
                        field.setAccessible(true);
                        field.set(mob, fieldName.equals("shieldChargeTarget") ? owner : owner.getUUID());
                        before = mob.position();
                        check(echo.isFollowMovementSuppressed(), "Roman yields movement to " + fieldName);
                        follow.run();
                        check(mob.position().equals(before), "Roman cannot follow during " + fieldName);
                        field.set(mob, null);
                    }
                    check(!echo.isFollowMovementSuppressed(), "Roman resumes follow after action/interaction");
                }


                // Vertical separation alone must not make a grounded companion repeatedly teleport.
                mob.setPos(40.5, 96, 40.5);
                owner.setPos(40.5, 136, 40.5);
                before = mob.position();
                follow.run();
                check(mob.position().equals(before) && mob.getNavigation().isDone(),
                        hero + " waits below hovering owner");

                // Horizontal flight must select supported terrain, clearing inherited fall state.
                mob.setPos(0.5, 96, 40.5);
                mob.fallDistance = 40;
                mob.setDeltaMovement(0, -3, 0);
                follow.run();
                check(mob.getY() == 96 && mob.getX() >= 34 && mob.getX() <= 47,
                        hero + " follows distant airborne owner onto surface");
                BlockPos floor = mob.blockPosition().below();
                check(level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP)
                        && level.noCollision(mob), hero + " full body has support and clearance");
                check(mob.fallDistance == 0 && mob.getDeltaMovement().equals(Vec3.ZERO),
                        hero + " teleport clears fall distance and downward velocity");

                // Repeated airborne travel during the same tick cannot teleport again.
                before = mob.position();
                owner.setPos(80.5, 136, 40.5);
                follow.run();
                check(mob.position().equals(before), hero + " automatic retry is throttled");

                // Manual recall uses the same safety rules, even while the owner is flying.
                owner.setPos(40.5, 136, 40.5);
                mob.setPos(0.5, 96, 40.5);
                echo.recallTo(owner);
                check(mob.getY() == 96 && mob.getX() >= 34, hero + " safe airborne manual recall");
                before = mob.position();
                owner.setPos(40.5, level.getMinBuildHeight() - 30, 40.5);
                check(!EchoSafeTeleport1211.teleportBesideOwner(mob, owner)
                        && mob.position().equals(before), hero + " no fallback to unsafe owner coordinates");

                // Mode changes must also stop the already-created follow callback.
                for (var mode : List.of(EchoRelicState1211.ActivityMode.WAIT, EchoRelicState1211.ActivityMode.WANDER)) {
                    EchoRelicState1211.setActivityMode(relic, mode);
                    if (echo instanceof RomanLegionaryEchoEntity1211) {
                        // Legacy Roman receives activity controls from its authoritative binding.
                        var binding = new com.yuriscat.echowarrior.compat.binding.EchoBindingSavedData1211()
                                .getOrCreate(UUID.randomUUID());
                        binding.setRelic(relic);
                        roman.applyBindingState(binding, false);
                    } else {
                        echo.applyRelicState(relic, false);
                    }
                    check(!echo.shouldFollowOwner(), hero + " respects " + mode);
                    follow.run();
                    check(mob.position().equals(before), hero + " no teleport in " + mode);
                }
            }
            org.slf4j.LoggerFactory.getLogger("echo_warrior").info(
                    "FOLLOW SAFETY SELFTEST PASSED heroes=5 boundary=grounded hover=wait flight=surface fall=reset unsafe=stay modes=guarded");
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Follow safety callback lookup", ex);
        } finally {
            fixtures.forEach(PathfinderMob::discard);
            blocks.forEach((pos, state) -> level.setBlock(pos, state, 2));
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Runnable movementTick(ServerLevel level, PathfinderMob mob, ServerPlayer owner)
            throws ReflectiveOperationException {
        if (mob.getClass().getSimpleName().startsWith("EgyptianArcher")) {
            var method = mob.getClass().getDeclaredMethod("tickMovement", ServerLevel.class, LivingEntity.class);
            method.setAccessible(true);
            return () -> {
                try { method.invoke(mob, level, owner); }
                catch (ReflectiveOperationException ex) { throw new IllegalStateException(ex); }
            };
        }
        var group = (net.tslat.smartbrainlib.api.core.BrainActivityGroup)
                mob.getClass().getMethod("getIdleTasks").invoke(mob);
        Object first = group.getBehaviours().get(0);
        check(first instanceof EchoFollowOwner1211, mob.getType() + " installs safe follow behaviour");
        EchoFollowOwner1211 follow = (EchoFollowOwner1211)first;
        // Supply a simulated owner only at the lookup boundary; execute the actual installed behaviour.
        follow.following(ignored -> owner);
        return () -> follow.tick(mob);
    }

    private static PathfinderMob create(ServerLevel level, String hero, List<PathfinderMob> fixtures) {
        var type = BuiltInRegistries.ENTITY_TYPE.get(
                net.minecraft.resources.ResourceLocation.parse("echo_warrior:" + hero + "_echo"));
        var mob = (PathfinderMob)type.create(level);
        check(mob != null, "registered factory " + hero);
        fixtures.add(mob);
        return mob;
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new IllegalStateException("Follow safety: " + message);
    }
}
