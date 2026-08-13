package com.yuriscat.echowarrior;

import com.yuriscat.echowarrior.effect.SoldierFormationMobEffect;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;

public final class ModEffects {
	public static final Holder<MobEffect> LEGACY_SOLDIER_FORMATION = Registry.registerForHolder(
			BuiltInRegistries.MOB_EFFECT,
			EchoWarrior.id("soldier_formation"),
			new SoldierFormationMobEffect()
	);
	public static final Holder<MobEffect> WEAPONS_RAISED = Registry.registerForHolder(
			BuiltInRegistries.MOB_EFFECT,
			EchoWarrior.id("weapons_raised"),
			new SoldierFormationMobEffect()
	);
	public static final Holder<MobEffect> SHIELDS_RAISED = Registry.registerForHolder(
			BuiltInRegistries.MOB_EFFECT,
			EchoWarrior.id("shields_raised"),
			new SoldierFormationMobEffect()
	);

	private ModEffects() {
	}

	public static void initialize() {
	}
}
