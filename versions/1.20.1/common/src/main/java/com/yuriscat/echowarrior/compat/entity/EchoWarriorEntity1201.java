package com.yuriscat.echowarrior.compat.entity;

import com.yuriscat.echowarrior.compat.EchoHeroType1201;
import com.yuriscat.echowarrior.compat.binding.EchoBindingSavedData1201;
import com.yuriscat.echowarrior.compat.binding.EchoBindingSystem1201;
import com.yuriscat.echowarrior.compat.item.EchoAccessorySystem1201;
import com.yuriscat.echowarrior.compat.item.EchoRelicState1201;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/** Common server contract used by every 1.20.1 echo implementation. */
public interface EchoWarriorEntity1201 extends OwnableEntity {
    LivingEntity livingEntity();
    EchoHeroType1201 heroType();
    UUID getOwnerUUID();
    UUID getSummonerId();
    long getBindingGeneration();
    void bindTo(Player owner, UUID summonerId);
    default void bindTo(Player owner, UUID summonerId, long generation) {
        bindTo(owner, summonerId);
        setBindingGeneration(generation);
    }
    default void applyBindingState(EchoBindingSavedData1201.Binding binding, boolean resetAnchor) {
        applyRelicState(binding.relic(), resetAnchor);
        EchoAccessorySystem1201.apply(this, binding.accessories());
    }
    void applyRelicState(ItemStack relic, boolean preserveHealthGain);
    void recallTo(Player player);

    default void writeMigrationState(CompoundTag tag) { }
    default void readMigrationState(CompoundTag tag) { }

    default UUID getOwnerUuid() { return getOwnerUUID(); }
    default UUID getSummonerUuid() { return getSummonerId(); }
    default LivingEntity ownerEntity() {
        if (!(livingEntity().level() instanceof ServerLevel level) || getOwnerUUID() == null) return null;
        return level.getPlayerByUUID(getOwnerUUID());
    }
    default void setBindingGeneration(long generation) { }
    default void dismiss() { livingEntity().discard(); }
    default void applyModuleState() { EchoAccessorySystem1201.apply(this); }
    default void reflectModuleMeleeDamage(ServerLevel level, net.minecraft.world.damagesource.DamageSource source,
                                          float previousHealth) {
        EchoAccessorySystem1201.reflectMeleeDamage(this, level, source, previousHealth);
    }
    default void onAccessoryDodge(net.minecraft.world.damagesource.DamageSource source) {
        LivingEntity living = livingEntity();
        if (!(living.level() instanceof ServerLevel level)) return;
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD,
                living.getX(), living.getY() + living.getBbHeight() * 0.5, living.getZ(),
                8, 0.28, 0.35, 0.28, 0.05);
        level.playSound(null, living.blockPosition(), net.minecraft.sounds.SoundEvents.PLAYER_ATTACK_NODAMAGE,
                net.minecraft.sounds.SoundSource.PLAYERS, 0.55F, 1.55F);
    }
    default ItemStack activeRelic() {
        return livingEntity().level() instanceof ServerLevel level && getSummonerId() != null
                ? EchoBindingSystem1201.relic(level, getSummonerId())
                : ItemStack.EMPTY;
    }
    default boolean isFormationActive() { return false; }
    default boolean isLegionEnduresActive() { return false; }
    default boolean shouldFollowOwner() {
        ItemStack relic = activeRelic();
        return !relic.isEmpty() && EchoRelicState1201.activityMode(relic) == EchoRelicState1201.ActivityMode.FOLLOW;
    }
    default boolean isFollowMovementSuppressed() { return false; }
}
