package com.yuriscat.echowarrior.compat.entity;

import com.yuriscat.echowarrior.compat.EchoWarrior1201;
import com.yuriscat.echowarrior.compat.item.EchoAccessorySystem1201;
import com.yuriscat.echowarrior.compat.item.EchoRelicState1201;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.animal.horse.AbstractHorse;

/** A safety policy, independent of alert/activity modes and checked again at damage time. */
public final class EchoAssistPolicy1201 {
    public static final TagKey<EntityType<?>> PROTECTED = TagKey.create(
            Registries.ENTITY_TYPE, EchoWarrior1201.id("assist_protected"));

    private EchoAssistPolicy1201() { }

    public static boolean isProtected(LivingEntity target) {
        if (target.getType().builtInRegistryHolder().is(PROTECTED)) return true;
        // Echo ownership is not pet taming; retain the existing ally/team rules for echoes.
        if (target instanceof EchoWarriorEntity1201) return false;
        if (target instanceof AbstractHorse horse && horse.isTamed()) return true;
        return target instanceof OwnableEntity ownable && ownable.getOwnerUUID() != null;
    }

    public static boolean allowsTarget(EchoWarriorEntity1201 echo, LivingEntity target) {
        return echo.assistMode() == EchoRelicState1201.AssistMode.ALL || !isProtected(target);
    }

    public static boolean followsOwner(EchoWarriorEntity1201 echo) {
        return echo.assistMode() != EchoRelicState1201.AssistMode.OFF;
    }

    /** In-flight/periodic damage still has a policy when the source entity is unloaded. */
    public static boolean allowsStoredTarget(net.minecraft.server.level.ServerLevel level, LivingEntity target,
                                            java.util.UUID summoner, java.util.UUID fallbackOwner, int fallbackMode) {
        var controller = summoner == null ? null
                : com.yuriscat.echowarrior.compat.binding.EchoBindingSystem1201.controllerId(level, summoner);
        if (controller == null) controller = fallbackOwner;
        if (controller != null) {
            if (controller.equals(target.getUUID())) return false;
            if (target instanceof EchoWarriorEntity1201 echo && controller.equals(echo.getOwnerUuid())) return false;
            if (target instanceof OwnableEntity ownable && ownable.getOwnerUUID() != null
                    && controller.equals(ownable.getOwnerUUID())) return false;
            var player = level.getPlayerByUUID(controller);
            if (player != null && player.isAlliedTo(target)) return false;
        }
        if (target instanceof net.minecraft.world.entity.player.Player player
                && (player.isCreative() || player.isSpectator())) return false;
        var relic = summoner == null ? net.minecraft.world.item.ItemStack.EMPTY
                : com.yuriscat.echowarrior.compat.binding.EchoBindingSystem1201.relic(level, summoner);
        var mode = relic.isEmpty() ? EchoRelicState1201.AssistMode.byOrdinal(fallbackMode) : EchoRelicState1201.assistMode(relic);
        return mode == EchoRelicState1201.AssistMode.ALL || !isProtected(target);
    }

    public static boolean allowsDamage(LivingEntity victim, DamageSource source) {
        EchoWarriorEntity1201 echo = source.getDirectEntity() instanceof EchoWarriorEntity1201 direct
                ? direct : EchoAccessorySystem1201.resolveAttackingEcho(source);
        return echo == null || echo.livingEntity() instanceof net.minecraft.world.entity.Mob mob && mob.canAttack(victim);
    }
}
