package com.yuriscat.echowarrior.compat.entity;

import com.yuriscat.echowarrior.compat.ModDamageTypes1201;
import com.yuriscat.echowarrior.compat.ModEffects1201;
import com.yuriscat.echowarrior.compat.binding.EchoBindingSystem1201;
import com.yuriscat.echowarrior.compat.item.EchoRelicState1201;
import com.yuriscat.echowarrior.compat.item.EchoSkillScaling1201;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public final class EchoBleeding1201 {
    private EchoBleeding1201() { }

    public static void apply(ServerLevel level, LivingEntity target, EchoWarriorEntity1201 caster,
                             boolean wound, double attackSnapshot) {
        if (!EchoAssistPolicy1201.allowsTarget(caster, target)) return;
        applySnapshot(level, target, wound, new EchoCombatState1201.Bleed(caster.livingEntity().getUUID(), caster.getOwnerUuid(),
                caster.getSummonerUuid(), EchoSkillScaling1201.bleedingDamage(attackSnapshot),
                level.getGameTime() + (wound ? 120 : 80), caster.assistMode().ordinal()));
    }

    public static void applySnapshot(ServerLevel level, LivingEntity target, boolean wound, EchoCombatState1201.Bleed source) {
        if (!allowed(level, target, source)) return;
        var effect = wound ? ModEffects1201.OBSIDIAN_WOUND : ModEffects1201.BLEEDING;
        EchoCombatState1201 state = EchoCombatState1201.of(target);
        if (!target.hasEffect(effect)) state.clear(wound);
        long now = level.getGameTime();
        state.apply(wound, source, now);
        // One extra tick keeps the final scheduled tick alive; duration is never the damage clock.
        int remaining = (int)(state.sources(wound).values().stream()
                .mapToLong(EchoCombatState1201.Bleed::expiresAt).max().orElse(now) - now + 1);
        target.addEffect(new MobEffectInstance(effect, remaining, 0, false, true, true));
    }

    public static boolean tick(ServerLevel level, LivingEntity target, boolean wound) {
        EchoCombatState1201 state = EchoCombatState1201.of(target);
        long now = level.getGameTime();
        state.sources(wound).values().removeIf(source -> !allowed(level, target, source));
        EchoCombatState1201.Bleed source = state.strongest(wound, now);
        if (source == null || !state.tickDue(wound, now)) return false;
        var caster = level.getEntity(source.caster());
        var type = wound ? ModDamageTypes1201.OBSIDIAN_WOUND : ModDamageTypes1201.BLEEDING;
        var motion = target.getDeltaMovement();
        boolean damaged = target.hurt( ModDamageTypes1201.source(level, type, null, caster), source.damage());
        target.setDeltaMovement(motion);
        return damaged;
    }

    private static boolean allowed(ServerLevel level, LivingEntity target, EchoCombatState1201.Bleed source) {
        if (source.owner() != null && source.owner().equals(target.getUUID())) return false;
        if (source.owner() != null && target instanceof net.minecraft.world.entity.OwnableEntity ownable
                && source.owner().equals((ownable.getOwnerUUID() == null ? null : ownable.getOwnerUUID()))) return false;
        var owner = source.owner() == null ? null : level.getPlayerByUUID(source.owner());
        if (owner != null && owner.isAlliedTo(target)) return false;
        if (target instanceof EchoWarriorEntity1201 echo && source.owner() != null
                && source.owner().equals(echo.getOwnerUuid())) return false;
        if (level.getEntity(source.caster()) instanceof EchoWarriorEntity1201 echo) {
            return echo.livingEntity() instanceof net.minecraft.world.entity.Mob mob && mob.canAttack(target);
        }
        return EchoAssistPolicy1201.allowsStoredTarget(level, target, source.summoner(), source.owner(), source.assist());
    }
}
