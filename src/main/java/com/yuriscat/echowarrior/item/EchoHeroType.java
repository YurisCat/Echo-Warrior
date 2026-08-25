package com.yuriscat.echowarrior.item;

import net.minecraft.world.item.ItemStack;

public enum EchoHeroType {
	ROMAN_LEGIONARY(
			"roman_legionary",
			"罗马军团兵",
			"Roman Legionary",
			30.0,
			6.0,
			8.0,
			0.28,
			0.30,
			20,
			3
	),
	AZTEC_WARRIOR(
			"aztec_warrior",
			"阿兹特克勇士",
			"Aztec Warrior",
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
			"埃及弓箭手",
			"Egyptian Archer",
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
			"关刀甲胄战士",
			"Guandao Warrior",
			30.0,
			7.0,
			12.0,
			0.26,
			0.30,
			34,
			4
	);

	private final String id;
	private final String chineseName;
	private final String englishName;
	private final double baseMaximumHealth;
	private final double baseAttackDamage;
	private final double baseArmor;
	private final double baseMovementSpeed;
	private final double baseKnockbackResistance;
	private final int baseAttackIntervalTicks;
	private final int skillCount;

	EchoHeroType(
			String id,
			String chineseName,
			String englishName,
			double baseMaximumHealth,
			double baseAttackDamage,
			double baseArmor,
			double baseMovementSpeed,
			double baseKnockbackResistance,
			int baseAttackIntervalTicks,
			int skillCount
	) {
		this.id = id;
		this.chineseName = chineseName;
		this.englishName = englishName;
		this.baseMaximumHealth = baseMaximumHealth;
		this.baseAttackDamage = baseAttackDamage;
		this.baseArmor = baseArmor;
		this.baseMovementSpeed = baseMovementSpeed;
		this.baseKnockbackResistance = baseKnockbackResistance;
		this.baseAttackIntervalTicks = baseAttackIntervalTicks;
		this.skillCount = skillCount;
	}

	public String id() { return this.id; }
	public String chineseName() { return this.chineseName; }
	public String englishName() { return this.englishName; }
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
