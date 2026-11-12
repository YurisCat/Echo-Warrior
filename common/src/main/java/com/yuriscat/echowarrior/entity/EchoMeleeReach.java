package com.yuriscat.echowarrior.entity;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;

public final class EchoMeleeReach {
    private EchoMeleeReach() { }
    public static boolean restricted(LivingEntity attacker) {
        return EchoCombatState.of(attacker).reachRestricted(attacker.level().getGameTime());
    }
    /** Keep the body (and mount) intact; reduce only reach outside that body. */
    public static AABB reduceBox(Mob attacker, AABB attackBox) {
        if (!restricted(attacker)) return attackBox;
        AABB body = attacker.getBoundingBox();
        if (attacker.getVehicle() != null) {
            AABB mount = attacker.getVehicle().getBoundingBox();
            body = new AABB(Math.min(body.minX, mount.minX), body.minY, Math.min(body.minZ, mount.minZ),
                    Math.max(body.maxX, mount.maxX), body.maxY, Math.max(body.maxZ, mount.maxZ));
        }
        return new AABB(body.minX - Math.max(0, body.minX - attackBox.minX) * 0.2, attackBox.minY,
                body.minZ - Math.max(0, body.minZ - attackBox.minZ) * 0.2,
                body.maxX + Math.max(0, attackBox.maxX - body.maxX) * 0.2, attackBox.maxY,
                body.maxZ + Math.max(0, attackBox.maxZ - body.maxZ) * 0.2);
    }
    public static double reduceSquaredRange(Mob attacker, LivingEntity target, double original) {
        if (!restricted(attacker)) return original;
        double contact = (attacker.getBbWidth() + target.getBbWidth()) * 0.5;
        double reach = contact + Math.max(0.0, Math.sqrt(original) - contact) * 0.2;
        return reach * reach;
    }
    /** A delayed/custom AI must recheck at the actual melee damage boundary. */
    public static boolean allowsDamage(LivingEntity victim, DamageSource source) {
        if (!(source.getDirectEntity() instanceof Mob attacker) || source.getEntity() != attacker
                || !restricted(attacker) || source.is(DamageTypeTags.IS_PROJECTILE)
                || source.is(DamageTypeTags.IS_EXPLOSION)) return true;
        if (!source.is(DamageTypes.MOB_ATTACK) && !source.is(DamageTypes.MOB_ATTACK_NO_AGGRO)) return true;
        return attacker.isWithinMeleeAttackRange(victim);
    }
}
