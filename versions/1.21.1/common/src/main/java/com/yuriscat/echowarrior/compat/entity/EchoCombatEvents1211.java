package com.yuriscat.echowarrior.compat.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;

/** Cross-loader reactions that run after a successful damage application. */
public final class EchoCombatEvents1211 {
    private EchoCombatEvents1211() {
    }

    public static void afterDamage(LivingEntity victim, DamageSource source, float damageTaken, boolean blocked) {
        if (!(victim.level() instanceof ServerLevel level) || blocked || damageTaken <= 0.0F) return;
        if (victim instanceof Creeper creeper && source.getDirectEntity() != null) {
            CatGodCreeperSystem1211.onDirectlyDamaged(level, creeper);
        }
        if (!(source.getEntity() instanceof LivingEntity attacker) || source.getDirectEntity() == null) return;
        if (victim instanceof AztecWarriorEchoEntity1211 aztec) {
            aztec.tryApplyCurse(level, attacker);
            return;
        }
        for (AztecWarriorEchoEntity1211 aztec : level.getEntitiesOfClass(
                AztecWarriorEchoEntity1211.class,
                victim.getBoundingBox().inflate(128.0),
                candidate -> candidate.isAlive() && victim.getUUID().equals(candidate.getOwnerUuid()))) {
            aztec.tryApplyCurse(level, attacker);
            break;
        }
    }
}
