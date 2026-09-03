package com.yuriscat.echowarrior;

import com.yuriscat.echowarrior.effect.SoldierFormationMobEffect;
import com.yuriscat.echowarrior.effect.ObsidianWoundMobEffect;
import com.yuriscat.echowarrior.effect.SunBlessingMobEffect;
import com.yuriscat.echowarrior.effect.BleedingMobEffect;
import com.yuriscat.echowarrior.platform.RegistryRegistrar;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;

public final class ModEffects {
	private static final MobEffect LEGACY_SOLDIER_FORMATION_VALUE = new SoldierFormationMobEffect();
	private static final MobEffect WEAPONS_RAISED_VALUE = new SoldierFormationMobEffect();
	private static final MobEffect SHIELDS_RAISED_VALUE = new SoldierFormationMobEffect();
	private static final MobEffect HUITZILOPOCHTLI_BLESSING_VALUE = new SunBlessingMobEffect();
	private static final MobEffect OBSIDIAN_WOUND_VALUE = new ObsidianWoundMobEffect();
	private static final MobEffect BLEEDING_VALUE = new BleedingMobEffect();

	public static Holder<MobEffect> LEGACY_SOLDIER_FORMATION;
	public static Holder<MobEffect> WEAPONS_RAISED;
	public static Holder<MobEffect> SHIELDS_RAISED;
	public static Holder<MobEffect> HUITZILOPOCHTLI_BLESSING;
	public static Holder<MobEffect> OBSIDIAN_WOUND;
	public static Holder<MobEffect> BLEEDING;

	private ModEffects() {
	}

	public static void register(RegistryRegistrar<MobEffect> registrar) {
		LEGACY_SOLDIER_FORMATION = register(registrar, "soldier_formation", LEGACY_SOLDIER_FORMATION_VALUE);
		WEAPONS_RAISED = register(registrar, "weapons_raised", WEAPONS_RAISED_VALUE);
		SHIELDS_RAISED = register(registrar, "shields_raised", SHIELDS_RAISED_VALUE);
		HUITZILOPOCHTLI_BLESSING = register(registrar, "huitzilopochtli_blessing", HUITZILOPOCHTLI_BLESSING_VALUE);
		OBSIDIAN_WOUND = register(registrar, "obsidian_wound", OBSIDIAN_WOUND_VALUE);
		BLEEDING = register(registrar, "bleeding", BLEEDING_VALUE);
	}

	private static Holder<MobEffect> register(RegistryRegistrar<MobEffect> registrar, String path, MobEffect effect) {
		Identifier id = EchoWarrior.id(path);
		registrar.register(id, effect);
		return BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect);
	}
}
