package com.yuriscat.echowarrior;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;

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

	private ModDamageTypes() {
	}
}
