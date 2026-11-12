package com.yuriscat.echowarrior.compat.entity;

import com.yuriscat.echowarrior.compat.ModDamageTypes1211;
import com.yuriscat.echowarrior.compat.ModEffects1211;
import com.yuriscat.echowarrior.compat.binding.EchoBindingSystem1211;
import com.yuriscat.echowarrior.compat.item.EchoRelicState1211;
import com.yuriscat.echowarrior.compat.item.EchoSkillScaling1211;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public final class EchoBleeding1211 {
    private EchoBleeding1211() { }

    public static void apply(ServerLevel level, LivingEntity target, EchoWarriorEntity1211 caster,
                             boolean wound, double attackSnapshot) {
        if (!EchoAssistPolicy1211.allowsTarget(caster, target)) return;
        applySnapshot(level, target, wound, new EchoCombatState1211.Bleed(caster.livingEntity().getUUID(), caster.getOwnerUuid(),
                caster.getSummonerUuid(), EchoSkillScaling1211.bleedingDamage(attackSnapshot),
                level.getGameTime() + (wound ? 120 : 80), caster.assistMode().ordinal()));
    }

    public static void applySnapshot(ServerLevel level, LivingEntity target, boolean wound, EchoCombatState1211.Bleed source) {
        if (!allowed(level, target, source)) return;
        var effect = wound ? ModEffects1211.OBSIDIAN_WOUND : ModEffects1211.BLEEDING;
        EchoCombatState1211 state = EchoCombatState1211.of(target);
        if (!target.hasEffect(effect)) state.clear(wound);
        long now = level.getGameTime();
        state.apply(wound, source, now);
        // One extra tick keeps the final scheduled tick alive; duration is never the damage clock.
        int remaining = (int)(state.sources(wound).values().stream()
                .mapToLong(EchoCombatState1211.Bleed::expiresAt).max().orElse(now) - now + 1);
        target.addEffect(new MobEffectInstance(effect, remaining, 0, false, true, true));
    }

    public static boolean tick(ServerLevel level, LivingEntity target, boolean wound) {
        EchoCombatState1211 state = EchoCombatState1211.of(target);
        long now = level.getGameTime();
        state.sources(wound).values().removeIf(source -> !allowed(level, target, source));
        EchoCombatState1211.Bleed source = state.strongest(wound, now);
        if (source == null || !state.tickDue(wound, now)) return false;
        var caster = level.getEntity(source.caster());
        var type = wound ? ModDamageTypes1211.OBSIDIAN_WOUND : ModDamageTypes1211.BLEEDING;
        var motion = target.getDeltaMovement();
        boolean damaged = target.hurt( ModDamageTypes1211.source(level, type, null, caster), source.damage());
        target.setDeltaMovement(motion);
        return damaged;
    }

    private static boolean allowed(ServerLevel level, LivingEntity target, EchoCombatState1211.Bleed source) {
        if (source.owner() != null && source.owner().equals(target.getUUID())) return false;
        if (source.owner() != null && target instanceof net.minecraft.world.entity.OwnableEntity ownable
                && source.owner().equals((ownable.getOwnerUUID() == null ? null : ownable.getOwnerUUID()))) return false;
        var owner = source.owner() == null ? null : level.getPlayerByUUID(source.owner());
        if (owner != null && owner.isAlliedTo(target)) return false;
        if (target instanceof EchoWarriorEntity1211 echo && source.owner() != null
                && source.owner().equals(echo.getOwnerUuid())) return false;
        if (level.getEntity(source.caster()) instanceof EchoWarriorEntity1211 echo) {
            return echo.livingEntity() instanceof net.minecraft.world.entity.Mob mob && mob.canAttack(target);
        }
        return EchoAssistPolicy1211.allowsStoredTarget(level, target, source.summoner(), source.owner(), source.assist());
    }
}
