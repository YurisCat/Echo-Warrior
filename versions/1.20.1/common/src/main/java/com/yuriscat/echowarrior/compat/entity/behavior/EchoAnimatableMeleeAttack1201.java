package com.yuriscat.echowarrior.compat.entity.behavior;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.tslat.smartbrainlib.api.core.behaviour.custom.attack.AnimatableMeleeAttack;
import net.tslat.smartbrainlib.util.BrainUtils;

import java.util.function.BiPredicate;

/** Restores the predicate/callback hooks available to the mainline SBL melee behaviour. */
public final class EchoAnimatableMeleeAttack1201<E extends Mob> extends AnimatableMeleeAttack<E> {
    private BiPredicate<E, LivingEntity> attackPredicate = (entity, target) ->
            entity.getSensing().hasLineOfSight(target) && entity.isWithinMeleeAttackRange(target);

    public EchoAnimatableMeleeAttack1201(int delayTicks) {
        super(delayTicks);
    }

    public EchoAnimatableMeleeAttack1201<E> canAttack(BiPredicate<E, LivingEntity> predicate) {
        this.attackPredicate = predicate;
        return this;
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, E entity) {
        // SBL 2 replaces its default predicate; SBL 1.15 hardcodes vanilla reach in
        // both this hook and doDelayedAction. Calling super silently caps hero reach.
        this.target = BrainUtils.getTargetOfEntity(entity);
        return this.target != null && this.attackPredicate.test(entity, this.target);
    }

    @Override
    protected void doDelayedAction(E entity) {
        // Preserve the old scheduler's cooldown, including a committed swing that misses.
        BrainUtils.setForgettableMemory(entity, MemoryModuleType.ATTACK_COOLING_DOWN,
                true, this.attackIntervalSupplier.apply(entity));
        if (this.target != null && this.attackPredicate.test(entity, this.target)) {
            entity.doHurtTarget(this.target);
        }
    }
}
