package com.yuriscat.echowarrior.entity;

import com.yuriscat.echowarrior.item.EchoHeroType;
import com.yuriscat.echowarrior.item.EchoAccessorySystem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

public interface EchoWarriorEntity extends OwnableEntity {
	LivingEntity livingEntity();

	EchoHeroType heroType();

	@Nullable UUID getOwnerUuid();

	@Nullable UUID getSummonerUuid();

	void bindTo(Player owner, UUID summonerUuid);

	void applyRelicState(ItemStack relic, boolean resetAnchor);

	default void applyModuleState() {
		EchoAccessorySystem.apply(this);
	}

	default void reflectModuleMeleeDamage(ServerLevel level, DamageSource source, float previousHealth) {
		EchoAccessorySystem.reflectMeleeDamage(this, level, source, previousHealth);
	}

	default void onAccessoryDodge(DamageSource source) {
		LivingEntity living = livingEntity();
		if (!(living.level() instanceof ServerLevel level)) return;
		level.sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD,
				living.getX(), living.getY() + living.getBbHeight() * 0.5, living.getZ(),
				8, 0.28, 0.35, 0.28, 0.05);
		level.playSound(null, living.blockPosition(), net.minecraft.sounds.SoundEvents.PLAYER_ATTACK_NODAMAGE,
				net.minecraft.sounds.SoundSource.PLAYERS, 0.55F, 1.55F);
	}

	void recallTo(Player player);

	void dismiss();

	boolean shouldFollowOwner();

	default boolean isFollowMovementSuppressed() { return false; }

	default boolean isFormationActive() { return false; }

	default boolean isShieldBondActive() { return false; }

	default boolean isLegionEnduresActive() { return false; }
}
