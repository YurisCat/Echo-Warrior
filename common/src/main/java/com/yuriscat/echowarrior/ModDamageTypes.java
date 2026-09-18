package com.yuriscat.echowarrior;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;

public final class ModDamageTypes {
	public static final ResourceKey<DamageType> OBSIDIAN_WOUND = ResourceKey.create(
			Registries.DAMAGE_TYPE,
			EchoWarrior.id("obsidian_wound")
	);
	public static final ResourceKey<DamageType> BLEEDING = ResourceKey.create(
			Registries.DAMAGE_TYPE,
			EchoWarrior.id("bleeding")
	);
	public static final ResourceKey<DamageType> ARMOR_PIERCING_ARROW = ResourceKey.create(
			Registries.DAMAGE_TYPE,
			EchoWarrior.id("armor_piercing_arrow")
	);
	public static final ResourceKey<DamageType> SAMURAI_FIRST_SLASH = ResourceKey.create(
			Registries.DAMAGE_TYPE,
			EchoWarrior.id("samurai_first_slash")
	);
	public static final ResourceKey<DamageType> SAMURAI_STAB = ResourceKey.create(
			Registries.DAMAGE_TYPE,
			EchoWarrior.id("samurai_stab")
	);
	public static final ResourceKey<DamageType> ROMAN_FIRST_STRIKE = ResourceKey.create(
			Registries.DAMAGE_TYPE,
			EchoWarrior.id("roman_first_strike")
	);
	public static final ResourceKey<DamageType> ROMAN_FOLLOWUP = ResourceKey.create(
			Registries.DAMAGE_TYPE,
			EchoWarrior.id("roman_followup")
	);
	public static final ResourceKey<DamageType> SPIKED_ARMOR_REFLECTION = ResourceKey.create(
			Registries.DAMAGE_TYPE,
			EchoWarrior.id("spiked_armor_reflection")
	);

	private ModDamageTypes() {
	}

	public static DamageSource source(ServerLevel level, ResourceKey<DamageType> type) {
		return new DamageSource(level.registryAccess()
				.lookupOrThrow(Registries.DAMAGE_TYPE)
				.getOrThrow(type));
	}

	public static DamageSource source(ServerLevel level, ResourceKey<DamageType> type, Entity directEntity) {
		return new DamageSource(level.registryAccess()
				.lookupOrThrow(Registries.DAMAGE_TYPE)
				.getOrThrow(type), directEntity);
	}

	public static DamageSource source(
			ServerLevel level,
			ResourceKey<DamageType> type,
			Entity directEntity,
			Entity causingEntity
	) {
		return new DamageSource(level.registryAccess()
				.lookupOrThrow(Registries.DAMAGE_TYPE)
				.getOrThrow(type), directEntity, causingEntity);
	}
}
