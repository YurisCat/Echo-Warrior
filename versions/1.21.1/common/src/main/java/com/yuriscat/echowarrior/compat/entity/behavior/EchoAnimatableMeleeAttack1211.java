package com.yuriscat.echowarrior.compat.entity.behavior;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.tslat.smartbrainlib.api.core.behaviour.custom.attack.AnimatableMeleeAttack;

import java.util.function.BiPredicate;

/** Restores the predicate/callback hooks available to the mainline SBL melee behaviour. */
public final class EchoAnimatableMeleeAttack1211<E extends Mob> extends AnimatableMeleeAttack<E> {
    private BiPredicate<E, LivingEntity> attackPredicate = (entity, target) -> true;

    public EchoAnimatableMeleeAttack1211(int delayTicks) {
        super(delayTicks);
    }

    public EchoAnimatableMeleeAttack1211<E> canAttack(BiPredicate<E, LivingEntity> predicate) {
        this.attackPredicate = predicate;
        return this;
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, E entity) {
        return super.checkExtraStartConditions(level, entity)
                && this.target != null
                && this.attackPredicate.test(entity, this.target);
    }
}
