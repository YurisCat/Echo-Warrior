package com.yuriscat.echowarrior.entity;

import com.yuriscat.echowarrior.EchoWarrior;
import com.yuriscat.echowarrior.item.EchoAccessorySystem;
import com.yuriscat.echowarrior.item.EchoRelicState;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.animal.equine.AbstractHorse;

/** A safety policy, independent of alert/activity modes and checked again at damage time. */
public final class EchoAssistPolicy {
    public static final TagKey<EntityType<?>> PROTECTED = TagKey.create(
            Registries.ENTITY_TYPE, EchoWarrior.id("assist_protected"));

    private EchoAssistPolicy() { }

    public static boolean isProtected(LivingEntity target) {
        if (target.getType().builtInRegistryHolder().is(PROTECTED)) return true;
        // Echo ownership is not pet taming; retain the existing ally/team rules for echoes.
        if (target instanceof EchoWarriorEntity) return false;
        if (target instanceof AbstractHorse horse && horse.isTamed()) return true;
        return target instanceof OwnableEntity ownable && ownable.getOwnerReference() != null;
    }

    public static boolean allowsTarget(EchoWarriorEntity echo, LivingEntity target) {
        return echo.assistMode() == EchoRelicState.AssistMode.ALL || !isProtected(target);
    }

    public static boolean followsOwner(EchoWarriorEntity echo) {
        return echo.assistMode() != EchoRelicState.AssistMode.OFF;
    }

    /** In-flight/periodic damage still has a policy when the source entity is unloaded. */
    public static boolean allowsStoredTarget(net.minecraft.server.level.ServerLevel level, LivingEntity target,
                                            java.util.UUID summoner, java.util.UUID fallbackOwner, int fallbackMode) {
        var controller = summoner == null ? null
                : com.yuriscat.echowarrior.binding.EchoBindingSystem.controllerId(level, summoner);
        if (controller == null) controller = fallbackOwner;
        if (controller != null) {
            if (controller.equals(target.getUUID())) return false;
            if (target instanceof EchoWarriorEntity echo && controller.equals(echo.getOwnerUuid())) return false;
            if (target instanceof OwnableEntity ownable && ownable.getOwnerReference() != null
                    && controller.equals(ownable.getOwnerReference().getUUID())) return false;
            var player = level.getPlayerByUUID(controller);
            if (player != null && player.isAlliedTo(target)) return false;
        }
        if (target instanceof net.minecraft.world.entity.player.Player player
                && (player.isCreative() || player.isSpectator())) return false;
        var relic = summoner == null ? net.minecraft.world.item.ItemStack.EMPTY
                : com.yuriscat.echowarrior.binding.EchoBindingSystem.relic(level, summoner);
        var mode = relic.isEmpty() ? EchoRelicState.AssistMode.byOrdinal(fallbackMode) : EchoRelicState.assistMode(relic);
        return mode == EchoRelicState.AssistMode.ALL || !isProtected(target);
    }

    public static boolean allowsDamage(LivingEntity victim, DamageSource source) {
        EchoWarriorEntity echo = source.getDirectEntity() instanceof EchoWarriorEntity direct
                ? direct : EchoAccessorySystem.resolveAttackingEcho(source);
        return echo == null || echo.livingEntity() instanceof net.minecraft.world.entity.Mob mob && mob.canAttack(victim);
    }
}
