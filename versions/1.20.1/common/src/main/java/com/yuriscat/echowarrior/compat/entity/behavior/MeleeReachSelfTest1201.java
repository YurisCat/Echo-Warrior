package com.yuriscat.echowarrior.compat.entity.behavior;

import com.yuriscat.echowarrior.compat.EchoWarrior1201;
import com.yuriscat.echowarrior.compat.entity.AztecWarriorEchoEntity1201;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.level.block.Blocks;
import net.tslat.smartbrainlib.util.BrainUtils;

/** Isolated-server regression: execute the hero's real configured start and damage predicates. */
public final class MeleeReachSelfTest1201 {
    private MeleeReachSelfTest1201() {}

    @SuppressWarnings("unchecked")
    public static void run(ServerLevel level, AztecWarriorEchoEntity1201 hero) {
        var attack = (EchoAnimatableMeleeAttack1201<AztecWarriorEchoEntity1201>)hero.getFightTasks()
                .getBehaviours().stream().filter(EchoAnimatableMeleeAttack1201.class::isInstance)
                .findFirst().orElseThrow();
        var target = EntityType.ZOMBIE.create(level);
        if (target == null) throw new IllegalStateException("Aztec reach fixture: no zombie");
        var oldPosition = hero.position();
        BlockPos wall = new BlockPos(8, 97, 9);
        var oldWall = level.getBlockState(wall);
        try {
            hero.setPos(8.5, 96, 8.5);
            target.setNoAi(true);
            target.moveTo(8.5, 96, 10.9, 0, 0); // 2.4 blocks: inside 2.25 + half-width, outside vanilla reach.
            level.addFreshEntity(target);
            BrainUtils.setMemory(hero, MemoryModuleType.ATTACK_TARGET, target);
            check(!hero.isWithinMeleeAttackRange(target), "fixture must exceed vanilla reach");
            check(attack.checkExtraStartConditions(level, hero), "custom range must allow starting at 2.4 blocks");
            float before = target.getHealth();
            attack.doDelayedAction(hero);
            check(target.getHealth() < before, "custom range must also allow the delayed damage frame");
            check(hero.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_COOLING_DOWN), "damage frame keeps cooldown");

            target.invulnerableTime = 0;
            target.setHealth(target.getMaxHealth());
            target.setPos(8.5, 96, 9.5);
            check(attack.checkExtraStartConditions(level, hero), "near target starts normally");
            target.setPos(8.5, 96, 10.9);
            before = target.getHealth();
            attack.doDelayedAction(hero);
            check(target.getHealth() < before, "target moving beyond vanilla reach during windup still takes damage");

            target.invulnerableTime = 0;
            target.setHealth(target.getMaxHealth());
            target.setPos(8.5, 96, 11.1); // 2.6 > 2.25 + zombie half-width.
            check(!attack.checkExtraStartConditions(level, hero), "beyond custom reach cannot start");
            before = target.getHealth();
            attack.doDelayedAction(hero);
            check(target.getHealth() == before, "damage frame rechecks custom range");

            target.setPos(8.5, 99, 9.5);
            check(!attack.checkExtraStartConditions(level, hero), "no hit across disjoint vertical boxes");
            attack.doDelayedAction(hero);
            check(target.getHealth() == before, "damage frame also rechecks vertical overlap");

            target.setPos(8.5, 96, 10.9);
            level.setBlock(wall, Blocks.STONE.defaultBlockState(), 2);
            check(!attack.checkExtraStartConditions(level, hero), "no attack through wall");
            attack.doDelayedAction(hero);
            check(target.getHealth() == before, "no delayed damage through wall");
            level.setBlock(wall, oldWall, 2);

            target.setHealth(0);
            check(!attack.checkExtraStartConditions(level, hero), "dead target rejected");
            BrainUtils.clearMemory(hero, MemoryModuleType.ATTACK_TARGET);
            check(!attack.checkExtraStartConditions(level, hero), "absent target rejected");
            attack.doDelayedAction(hero);
            EchoWarrior1201.LOGGER.info("[Compat1201] MELEE REACH SELFTEST PASSED hero=aztec start=2.4 delayed=2.4 reject=2.6 vertical=checked wall=checked");
        } finally {
            level.setBlock(wall, oldWall, 2);
            target.discard();
            hero.setPos(oldPosition);
            BrainUtils.clearMemory(hero, MemoryModuleType.ATTACK_TARGET);
            BrainUtils.clearMemory(hero, MemoryModuleType.ATTACK_COOLING_DOWN);
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new IllegalStateException("[Compat1201] Melee reach: " + message);
    }
}
