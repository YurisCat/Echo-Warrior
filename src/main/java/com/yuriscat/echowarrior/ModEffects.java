package com.yuriscat.echowarrior;

import com.yuriscat.echowarrior.effect.SoldierFormationMobEffect;
import com.yuriscat.echowarrior.effect.ObsidianWoundMobEffect;
import com.yuriscat.echowarrior.effect.SunBlessingMobEffect;
import com.yuriscat.echowarrior.effect.BleedingMobEffect;
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
	public static final Holder<MobEffect> HUITZILOPOCHTLI_BLESSING = Registry.registerForHolder(
			BuiltInRegistries.MOB_EFFECT,
			EchoWarrior.id("huitzilopochtli_blessing"),
			new SunBlessingMobEffect()
	);
	public static final Holder<MobEffect> OBSIDIAN_WOUND = Registry.registerForHolder(
			BuiltInRegistries.MOB_EFFECT,
			EchoWarrior.id("obsidian_wound"),
			new ObsidianWoundMobEffect()
	);
	public static final Holder<MobEffect> BLEEDING = Registry.registerForHolder(
			BuiltInRegistries.MOB_EFFECT,
			EchoWarrior.id("bleeding"),
			new BleedingMobEffect()
	);

	private ModEffects() {
	}

	public static void initialize() {
	}
}
