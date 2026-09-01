package com.yuriscat.echowarrior.item;

import net.minecraft.world.item.ItemStack;

public enum EchoHeroType {
	ROMAN_LEGIONARY(
			"roman_legionary",
			30.0,
			6.0,
			8.0,
			0.28,
			0.30,
			20,
			4
	),
	AZTEC_WARRIOR(
			"aztec_warrior",
			34.0,
			8.0,
			4.0,
			0.30,
			0.20,
			29,
			5
	),
	EGYPTIAN_ARCHER(
			"egyptian_archer",
			28.0,
			5.0,
			0.0,
			0.30,
			0.15,
			42,
			4
	),
	GUANDAO_WARRIOR(
			"guandao_warrior",
			30.0,
			7.0,
			12.0,
			0.26,
			0.30,
			34,
			4
	),
	JAPANESE_SAMURAI(
			"japanese_samurai",
			28.0,
			6.0,
			6.0,
			0.32,
			0.20,
			40,
			4
	);

	private final String id;
	private final String nameTranslationKey;
	private final double baseMaximumHealth;
	private final double baseAttackDamage;
	private final double baseArmor;
	private final double baseMovementSpeed;
	private final double baseKnockbackResistance;
	private final int baseAttackIntervalTicks;
	private final int skillCount;

	EchoHeroType(
			String id,
			double baseMaximumHealth,
			double baseAttackDamage,
			double baseArmor,
			double baseMovementSpeed,
			double baseKnockbackResistance,
			int baseAttackIntervalTicks,
			int skillCount
	) {
		this.id = id;
		this.nameTranslationKey = "hero.echo_warrior." + id;
		this.baseMaximumHealth = baseMaximumHealth;
		this.baseAttackDamage = baseAttackDamage;
		this.baseArmor = baseArmor;
		this.baseMovementSpeed = baseMovementSpeed;
		this.baseKnockbackResistance = baseKnockbackResistance;
		this.baseAttackIntervalTicks = baseAttackIntervalTicks;
		this.skillCount = skillCount;
	}

	public String id() { return this.id; }
	public String nameTranslationKey() { return this.nameTranslationKey; }
	public double baseMaximumHealth() { return this.baseMaximumHealth; }
	public double baseAttackDamage() { return this.baseAttackDamage; }
	public double baseArmor() { return this.baseArmor; }
	public double baseMovementSpeed() { return this.baseMovementSpeed; }
	public double baseKnockbackResistance() { return this.baseKnockbackResistance; }
	public int baseAttackIntervalTicks() { return this.baseAttackIntervalTicks; }
	public int skillCount() { return this.skillCount; }
	public int allSkillsEnabledMask() { return (1 << this.skillCount) - 1; }
	public int defaultEnabledSkillsMask() {
		return this == EGYPTIAN_ARCHER
				? this.allSkillsEnabledMask() & ~(1 << 1)
				: this.allSkillsEnabledMask();
	}

	public static EchoHeroType fromRelic(ItemStack relic) {
		return relic.getItem() instanceof EchoRelicItem echoRelic
				? echoRelic.heroType()
				: ROMAN_LEGIONARY;
	}
}
