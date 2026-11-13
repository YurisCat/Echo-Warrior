package com.yuriscat.echowarrior.entity;

import com.yuriscat.echowarrior.ModDamageTypes;
import com.yuriscat.echowarrior.ModEffects;
import com.yuriscat.echowarrior.binding.EchoBindingSystem;
import com.yuriscat.echowarrior.item.EchoRelicState;
import com.yuriscat.echowarrior.item.EchoSkillScaling;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public final class EchoBleeding {
    private EchoBleeding() { }

    public static void apply(ServerLevel level, LivingEntity target, EchoWarriorEntity caster,
                             boolean wound, double attackSnapshot) {
        if (!EchoAssistPolicy.allowsTarget(caster, target)) return;
        applySnapshot(level, target, wound, new EchoCombatState.Bleed(caster.livingEntity().getUUID(), caster.getOwnerUuid(),
                caster.getSummonerUuid(), EchoSkillScaling.bleedingDamage(attackSnapshot),
                level.getGameTime() + (wound ? 120 : 80), caster.assistMode().ordinal()));
    }

    public static void applySnapshot(ServerLevel level, LivingEntity target, boolean wound, EchoCombatState.Bleed source) {
        if (!allowed(level, target, source)) return;
        var effect = wound ? ModEffects.OBSIDIAN_WOUND : ModEffects.BLEEDING;
        EchoCombatState state = EchoCombatState.of(target);
        if (!target.hasEffect(effect)) state.clear(wound);
        long now = level.getGameTime();
        state.apply(wound, source, now);
        // One extra tick keeps the final scheduled tick alive; duration is never the damage clock.
        int remaining = (int)(state.sources(wound).values().stream()
                .mapToLong(EchoCombatState.Bleed::expiresAt).max().orElse(now) - now + 1);
        target.addEffect(new MobEffectInstance(effect, remaining, 0, false, true, true));
    }

    public static boolean tick(ServerLevel level, LivingEntity target, boolean wound) {
        EchoCombatState state = EchoCombatState.of(target);
        long now = level.getGameTime();
        state.sources(wound).values().removeIf(source -> !allowed(level, target, source));
        EchoCombatState.Bleed source = state.strongest(wound, now);
        if (source == null || !state.tickDue(wound, now)) return false;
        var caster = level.getEntity(source.caster());
        var type = wound ? ModDamageTypes.OBSIDIAN_WOUND : ModDamageTypes.BLEEDING;
        var motion = target.getDeltaMovement();
        boolean damaged = target.hurtServer(level, ModDamageTypes.source(level, type, null, caster), source.damage());
        target.setDeltaMovement(motion);
        return damaged;
    }

    private static boolean allowed(ServerLevel level, LivingEntity target, EchoCombatState.Bleed source) {
        if (source.owner() != null && source.owner().equals(target.getUUID())) return false;
        if (source.owner() != null && target instanceof net.minecraft.world.entity.OwnableEntity ownable
                && source.owner().equals((ownable.getOwnerReference() == null ? null : ownable.getOwnerReference().getUUID()))) return false;
        var owner = source.owner() == null ? null : level.getPlayerByUUID(source.owner());
        if (owner != null && owner.isAlliedTo(target)) return false;
        if (target instanceof EchoWarriorEntity echo && source.owner() != null
                && source.owner().equals(echo.getOwnerUuid())) return false;
        if (level.getEntity(source.caster()) instanceof EchoWarriorEntity echo) {
            return echo.livingEntity() instanceof net.minecraft.world.entity.Mob mob && mob.canAttack(target);
        }
        return EchoAssistPolicy.allowsStoredTarget(level, target, source.summoner(), source.owner(), source.assist());
    }
}
