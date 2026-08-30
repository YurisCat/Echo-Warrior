package com.yuriscat.echowarrior.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class EchoRelicState {
	private static final String INITIALIZED_KEY = "EchoWarriorTraitsInitialized";
	private static final String RELIC_ID_KEY = "EchoWarriorRelicId";
	private static final String TRAIT_MASK_KEY = "EchoWarriorTraitMask";
	private static final String BIOME_AFFINITY_KEY = "EchoWarriorBiomeAffinity";
	private static final String WISE_GROWTH_REMAINDER_KEY = "EchoWarriorWiseGrowthRemainder";
	private static final String ACTIVITY_MODE_KEY = "EchoWarriorActivityMode";
	private static final String ALERT_MODE_KEY = "EchoWarriorAlertMode";
	private static final String ENABLED_SKILLS_KEY = "EchoWarriorEnabledSkills";
	private static final String SHIELD_CHARGES_KEY = "EchoWarriorShieldCharges";
	private static final String SHIELD_CHARGE_TIME_KEY = "EchoWarriorShieldChargeTime";
	private static final String LEGION_COOLDOWN_END_KEY = "EchoWarriorLegionCooldownEnd";
	private static final String PURSUIT_CHARGES_KEY = "EchoWarriorPursuitCharges";
	private static final String PURSUIT_CHARGE_TIME_KEY = "EchoWarriorPursuitChargeTime";
	private static final String PURSUIT_COOLDOWN_END_KEY = "EchoWarriorPursuitCooldownEnd";
	private static final String EGYPTIAN_ARROW_MODE_KEY = "EchoWarriorEgyptianArrowMode";
	private static final String EGYPTIAN_ARROW_SWITCH_TIME_KEY = "EchoWarriorEgyptianArrowSwitchTime";
	private static final String BACKSTEP_CHARGES_KEY = "EchoWarriorBackstepCharges";
	private static final String BACKSTEP_CHARGE_TIME_KEY = "EchoWarriorBackstepChargeTime";
	private static final String GUANDAO_COMBO_COOLDOWN_END_KEY = "EchoWarriorGuandaoComboCooldownEnd";
	private static final String FUMIKOMI_CHARGES_KEY = "EchoWarriorFumikomiCharges";
	private static final String FUMIKOMI_CHARGE_TIME_KEY = "EchoWarriorFumikomiChargeTime";
	private static final String SAMURAI_STAB_COOLDOWN_END_KEY = "EchoWarriorSamuraiStabCooldownEnd";

	public static final int SKILL_COUNT = 5;
	public static final int ALL_SKILLS_ENABLED = (1 << SKILL_COUNT) - 1;
	public static final int MAX_SHIELD_CHARGES = 3;
	public static final long SHIELD_CHARGE_TICKS = 100L;
	public static final int MAX_PURSUIT_CHARGES = 2;
	public static final long PURSUIT_CHARGE_TICKS = 120L;
	public static final int MAX_BACKSTEP_CHARGES = 2;
	public static final long BACKSTEP_CHARGE_TICKS = 120L;
	public static final long EGYPTIAN_ARROW_SWITCH_COOLDOWN_TICKS = 10L;
	public static final long GUANDAO_COMBO_COOLDOWN_TICKS = 240L;
	public static final int MAX_FUMIKOMI_CHARGES = 3;
	public static final long FUMIKOMI_CHARGE_TICKS = 100L;
	public static final long SAMURAI_STAB_COOLDOWN_TICKS = 200L;

	private EchoRelicState() {
	}

	public static boolean ensureInitialized(ItemStack relic, RandomSource random, long gameTime) {
		if (!(relic.getItem() instanceof EchoRelicItem)) {
			return false;
		}
		CustomData data = relic.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
		if (data.copyTag().getBooleanOr(INITIALIZED_KEY, false)) {
			return false;
		}

		TraitRoll traits = rollTraits(random);
		EchoHeroType heroType = EchoHeroType.fromRelic(relic);
		CustomData.update(DataComponents.CUSTOM_DATA, relic, tag -> {
			tag.putBoolean(INITIALIZED_KEY, true);
			tag.putString(RELIC_ID_KEY, UUID.randomUUID().toString());
			tag.putInt(TRAIT_MASK_KEY, traits.mask());
			tag.putInt(BIOME_AFFINITY_KEY, traits.biomeAffinity().ordinal());
			tag.putInt(ACTIVITY_MODE_KEY, ActivityMode.FOLLOW.ordinal());
			tag.putInt(ALERT_MODE_KEY, AlertMode.DEFENSIVE.ordinal());
			tag.putInt(ENABLED_SKILLS_KEY, heroType.defaultEnabledSkillsMask());
			tag.putInt(SHIELD_CHARGES_KEY, MAX_SHIELD_CHARGES);
			tag.putLong(SHIELD_CHARGE_TIME_KEY, gameTime);
			tag.putLong(LEGION_COOLDOWN_END_KEY, 0L);
			tag.putInt(PURSUIT_CHARGES_KEY, MAX_PURSUIT_CHARGES);
			tag.putLong(PURSUIT_CHARGE_TIME_KEY, gameTime);
			tag.putLong(PURSUIT_COOLDOWN_END_KEY, 0L);
			tag.putInt(EGYPTIAN_ARROW_MODE_KEY, EgyptianArrowMode.OFF.ordinal());
			tag.putLong(EGYPTIAN_ARROW_SWITCH_TIME_KEY, Long.MIN_VALUE / 2L);
			tag.putInt(BACKSTEP_CHARGES_KEY, MAX_BACKSTEP_CHARGES);
			tag.putLong(BACKSTEP_CHARGE_TIME_KEY, gameTime);
			tag.putLong(GUANDAO_COMBO_COOLDOWN_END_KEY, 0L);
			tag.putInt(FUMIKOMI_CHARGES_KEY, MAX_FUMIKOMI_CHARGES);
			tag.putLong(FUMIKOMI_CHARGE_TIME_KEY, gameTime);
			tag.putLong(SAMURAI_STAB_COOLDOWN_END_KEY, 0L);
		});
		return true;
	}

	private static TraitRoll rollTraits(RandomSource random) {
		int roll = random.nextInt(100);
		int count = roll < 60 ? 2 : roll < 90 ? 3 : 4;
		List<EchoTrait> available = new ArrayList<>(List.of(EchoTrait.values()));
		int mask = 0;
		while (Integer.bitCount(mask) < count && !available.isEmpty()) {
			EchoTrait selected = available.remove(random.nextInt(available.size()));
			mask |= selected.mask();
			available.removeIf(selected::conflictsWith);
		}
		EchoBiomeAffinity affinity = EchoBiomeAffinity.values()[random.nextInt(EchoBiomeAffinity.values().length)];
		return new TraitRoll(mask, affinity);
	}

	public static int rerollTraits(ItemStack relic, RandomSource random, long gameTime) {
		if (!(relic.getItem() instanceof EchoRelicItem)) return 0;
		TraitRoll traits = rollTraits(random);
		CustomData.update(DataComponents.CUSTOM_DATA, relic, tag -> {
			tag.putBoolean(INITIALIZED_KEY, true);
			if (tag.getStringOr(RELIC_ID_KEY, "").isEmpty()) tag.putString(RELIC_ID_KEY, UUID.randomUUID().toString());
			tag.putInt(TRAIT_MASK_KEY, traits.mask());
			tag.putInt(BIOME_AFFINITY_KEY, traits.biomeAffinity().ordinal());
			tag.putLong(SHIELD_CHARGE_TIME_KEY, gameTime);
		});
		return traits.mask();
	}

	public static boolean initialized(ItemStack relic) {
		return relic.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
				.copyTag().getBooleanOr(INITIALIZED_KEY, false);
	}

	public static String relicId(ItemStack relic) {
		return relic.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
				.copyTag().getStringOr(RELIC_ID_KEY, "");
	}

	public static int traitMask(ItemStack relic) {
		return relic.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
				.copyTag().getIntOr(TRAIT_MASK_KEY, 0);
	}

	public static boolean hasTrait(ItemStack relic, EchoTrait trait) {
		return (traitMask(relic) & trait.mask()) != 0;
	}

	public static EchoBiomeAffinity biomeAffinity(ItemStack relic) {
		return EchoBiomeAffinity.byOrdinal(intValue(relic, BIOME_AFFINITY_KEY, EchoBiomeAffinity.WOODLAND.ordinal()));
	}

	public static ActivityMode activityMode(ItemStack relic) {
		return ActivityMode.byOrdinal(intValue(relic, ACTIVITY_MODE_KEY, ActivityMode.FOLLOW.ordinal()));
	}

	public static void setActivityMode(ItemStack relic, ActivityMode mode) {
		CustomData.update(DataComponents.CUSTOM_DATA, relic, tag -> tag.putInt(ACTIVITY_MODE_KEY, mode.ordinal()));
	}

	public static AlertMode alertMode(ItemStack relic) {
		return AlertMode.byOrdinal(intValue(relic, ALERT_MODE_KEY, AlertMode.DEFENSIVE.ordinal()));
	}

	public static void setAlertMode(ItemStack relic, AlertMode mode) {
		CustomData.update(DataComponents.CUSTOM_DATA, relic, tag -> tag.putInt(ALERT_MODE_KEY, mode.ordinal()));
	}

	public static int enabledSkills(ItemStack relic) {
		EchoHeroType heroType = EchoHeroType.fromRelic(relic);
		int allowed = heroType.allSkillsEnabledMask();
		return intValue(relic, ENABLED_SKILLS_KEY, heroType.defaultEnabledSkillsMask()) & allowed;
	}

	public static boolean skillEnabled(ItemStack relic, int skill) {
		return skill >= 0 && skill < EchoHeroType.fromRelic(relic).skillCount()
				&& (enabledSkills(relic) & 1 << skill) != 0;
	}

	public static void toggleSkill(ItemStack relic, int skill) {
		if (skill < 0 || skill >= EchoHeroType.fromRelic(relic).skillCount()) {
			return;
		}
		int updated = enabledSkills(relic) ^ 1 << skill;
		CustomData.update(DataComponents.CUSTOM_DATA, relic, tag -> tag.putInt(ENABLED_SKILLS_KEY, updated));
	}

	public static EgyptianArrowMode egyptianArrowMode(ItemStack relic) {
		if (EchoHeroType.fromRelic(relic) != EchoHeroType.EGYPTIAN_ARCHER) return EgyptianArrowMode.OFF;
		return EgyptianArrowMode.byOrdinal(intValue(relic, EGYPTIAN_ARROW_MODE_KEY, EgyptianArrowMode.OFF.ordinal()));
	}

	public static boolean cycleEgyptianArrowMode(ItemStack relic, long gameTime) {
		if (EchoHeroType.fromRelic(relic) != EchoHeroType.EGYPTIAN_ARCHER) return false;
		long previous = longValue(relic, EGYPTIAN_ARROW_SWITCH_TIME_KEY, Long.MIN_VALUE / 2L);
		if (gameTime - previous < EGYPTIAN_ARROW_SWITCH_COOLDOWN_TICKS) return false;
		EgyptianArrowMode next = egyptianArrowMode(relic).next();
		int updatedSkills = next == EgyptianArrowMode.OFF
				? enabledSkills(relic) & ~(1 << 1)
				: enabledSkills(relic) | 1 << 1;
		CustomData.update(DataComponents.CUSTOM_DATA, relic, tag -> {
			tag.putInt(EGYPTIAN_ARROW_MODE_KEY, next.ordinal());
			tag.putLong(EGYPTIAN_ARROW_SWITCH_TIME_KEY, gameTime);
			tag.putInt(ENABLED_SKILLS_KEY, updatedSkills);
		});
		return true;
	}

	public static int shieldCharges(ItemStack relic, long gameTime) {
		updateShieldCharges(relic, gameTime);
		return intValue(relic, SHIELD_CHARGES_KEY, MAX_SHIELD_CHARGES);
	}

	public static int shieldChargeProgress(ItemStack relic, long gameTime) {
		int charges = shieldCharges(relic, gameTime);
		if (charges >= MAX_SHIELD_CHARGES) {
			return 1000;
		}
		long last = longValue(relic, SHIELD_CHARGE_TIME_KEY, gameTime);
		return (int)Math.clamp((gameTime - last) * 1000L / SHIELD_CHARGE_TICKS, 0L, 1000L);
	}

	public static boolean consumeShieldCharge(ItemStack relic, long gameTime) {
		return consumeCharge(relic, gameTime, SHIELD_CHARGES_KEY, SHIELD_CHARGE_TIME_KEY,
				MAX_SHIELD_CHARGES, SHIELD_CHARGE_TICKS);
	}

	private static void updateShieldCharges(ItemStack relic, long gameTime) {
		updateCharges(relic, gameTime, SHIELD_CHARGES_KEY, SHIELD_CHARGE_TIME_KEY,
				MAX_SHIELD_CHARGES, SHIELD_CHARGE_TICKS);
	}

	public static int pursuitCharges(ItemStack relic, long gameTime) {
		updatePursuitCharges(relic, gameTime);
		return intValue(relic, PURSUIT_CHARGES_KEY, MAX_PURSUIT_CHARGES);
	}

	public static int pursuitChargeProgress(ItemStack relic, long gameTime) {
		int charges = pursuitCharges(relic, gameTime);
		if (charges >= MAX_PURSUIT_CHARGES) return 1000;
		long last = longValue(relic, PURSUIT_CHARGE_TIME_KEY, gameTime);
		return (int)Math.clamp((gameTime - last) * 1000L / PURSUIT_CHARGE_TICKS, 0L, 1000L);
	}

	public static boolean consumePursuitCharge(ItemStack relic, long gameTime) {
		return consumeCharge(relic, gameTime, PURSUIT_CHARGES_KEY, PURSUIT_CHARGE_TIME_KEY,
				MAX_PURSUIT_CHARGES, PURSUIT_CHARGE_TICKS);
	}

	private static void updatePursuitCharges(ItemStack relic, long gameTime) {
		updateCharges(relic, gameTime, PURSUIT_CHARGES_KEY, PURSUIT_CHARGE_TIME_KEY,
				MAX_PURSUIT_CHARGES, PURSUIT_CHARGE_TICKS);
	}

	public static int backstepCharges(ItemStack relic, long gameTime) {
		updateCharges(relic, gameTime, BACKSTEP_CHARGES_KEY, BACKSTEP_CHARGE_TIME_KEY,
				MAX_BACKSTEP_CHARGES, BACKSTEP_CHARGE_TICKS);
		return intValue(relic, BACKSTEP_CHARGES_KEY, MAX_BACKSTEP_CHARGES);
	}

	public static int backstepChargeProgress(ItemStack relic, long gameTime) {
		int charges = backstepCharges(relic, gameTime);
		if (charges >= MAX_BACKSTEP_CHARGES) return 1000;
		long last = longValue(relic, BACKSTEP_CHARGE_TIME_KEY, gameTime);
		return (int)Math.clamp((gameTime - last) * 1000L / BACKSTEP_CHARGE_TICKS, 0L, 1000L);
	}

	public static boolean consumeBackstepCharge(ItemStack relic, long gameTime) {
		return consumeCharge(relic, gameTime, BACKSTEP_CHARGES_KEY, BACKSTEP_CHARGE_TIME_KEY,
				MAX_BACKSTEP_CHARGES, BACKSTEP_CHARGE_TICKS);
	}

	public static int fumikomiCharges(ItemStack relic, long gameTime) {
		updateCharges(relic, gameTime, FUMIKOMI_CHARGES_KEY, FUMIKOMI_CHARGE_TIME_KEY,
				MAX_FUMIKOMI_CHARGES, FUMIKOMI_CHARGE_TICKS);
		return intValue(relic, FUMIKOMI_CHARGES_KEY, MAX_FUMIKOMI_CHARGES);
	}

	public static int fumikomiChargeProgress(ItemStack relic, long gameTime) {
		int charges = fumikomiCharges(relic, gameTime);
		if (charges >= MAX_FUMIKOMI_CHARGES) return 1000;
		long last = longValue(relic, FUMIKOMI_CHARGE_TIME_KEY, gameTime);
		return (int)Math.clamp((gameTime - last) * 1000L / FUMIKOMI_CHARGE_TICKS, 0L, 1000L);
	}

	public static boolean consumeFumikomiCharge(ItemStack relic, long gameTime) {
		return consumeCharge(relic, gameTime, FUMIKOMI_CHARGES_KEY, FUMIKOMI_CHARGE_TIME_KEY,
				MAX_FUMIKOMI_CHARGES, FUMIKOMI_CHARGE_TICKS);
	}

	public static boolean addFumikomiCharge(ItemStack relic, long gameTime) {
		int charges = fumikomiCharges(relic, gameTime);
		if (charges >= MAX_FUMIKOMI_CHARGES) return false;
		int updated = charges + 1;
		CustomData.update(DataComponents.CUSTOM_DATA, relic, tag -> {
			tag.putInt(FUMIKOMI_CHARGES_KEY, updated);
			if (updated >= MAX_FUMIKOMI_CHARGES) tag.putLong(FUMIKOMI_CHARGE_TIME_KEY, gameTime);
		});
		return true;
	}

	private static boolean consumeCharge(
			ItemStack relic,
			long gameTime,
			String chargesKey,
			String chargeTimeKey,
			int maximumCharges,
			long chargeTicks
	) {
		updateCharges(relic, gameTime, chargesKey, chargeTimeKey, maximumCharges, chargeTicks);
		int charges = intValue(relic, chargesKey, maximumCharges);
		if (charges <= 0) return false;
		int remaining = charges - 1;
		CustomData.update(DataComponents.CUSTOM_DATA, relic, tag -> {
			tag.putInt(chargesKey, remaining);
			// A full stack has no active recharge timer. Spending from full starts one;
			// spending another charge preserves the partial progress already accumulated.
			if (charges >= maximumCharges) tag.putLong(chargeTimeKey, gameTime);
		});
		return true;
	}

	private static void updateCharges(
			ItemStack relic,
			long gameTime,
			String chargesKey,
			String chargeTimeKey,
			int maximumCharges,
			long chargeTicks
	) {
		int charges = intValue(relic, chargesKey, maximumCharges);
		if (charges >= maximumCharges) {
			return;
		}
		long last = longValue(relic, chargeTimeKey, gameTime);
		long elapsed = Math.max(0L, gameTime - last);
		int restored = (int)(elapsed / chargeTicks);
		if (restored <= 0) {
			return;
		}
		int updated = Math.min(maximumCharges, charges + restored);
		long updatedTime = updated >= maximumCharges ? gameTime : last + restored * chargeTicks;
		CustomData.update(DataComponents.CUSTOM_DATA, relic, tag -> {
			tag.putInt(chargesKey, updated);
			tag.putLong(chargeTimeKey, updatedTime);
		});
	}

	public static long pursuitCooldownEnd(ItemStack relic) {
		return longValue(relic, PURSUIT_COOLDOWN_END_KEY, 0L);
	}

	public static void setPursuitCooldownEnd(ItemStack relic, long end) {
		CustomData.update(DataComponents.CUSTOM_DATA, relic, tag -> tag.putLong(PURSUIT_COOLDOWN_END_KEY, end));
	}

	public static int activeSkillCharges(ItemStack relic, long gameTime) {
		return switch (EchoHeroType.fromRelic(relic)) {
			case ROMAN_LEGIONARY -> shieldCharges(relic, gameTime);
			case AZTEC_WARRIOR -> pursuitCharges(relic, gameTime);
			case EGYPTIAN_ARCHER -> backstepCharges(relic, gameTime);
			case GUANDAO_WARRIOR -> 0;
			case JAPANESE_SAMURAI -> fumikomiCharges(relic, gameTime);
		};
	}

	public static int activeSkillMaximumCharges(ItemStack relic) {
		return switch (EchoHeroType.fromRelic(relic)) {
			case ROMAN_LEGIONARY -> MAX_SHIELD_CHARGES;
			case AZTEC_WARRIOR -> MAX_PURSUIT_CHARGES;
			case EGYPTIAN_ARCHER -> MAX_BACKSTEP_CHARGES;
			case GUANDAO_WARRIOR -> 0;
			case JAPANESE_SAMURAI -> MAX_FUMIKOMI_CHARGES;
		};
	}

	public static int activeSkillChargeProgress(ItemStack relic, long gameTime) {
		return switch (EchoHeroType.fromRelic(relic)) {
			case ROMAN_LEGIONARY -> shieldChargeProgress(relic, gameTime);
			case AZTEC_WARRIOR -> pursuitChargeProgress(relic, gameTime);
			case EGYPTIAN_ARCHER -> backstepChargeProgress(relic, gameTime);
			case GUANDAO_WARRIOR -> guandaoComboCooldownProgress(relic, gameTime);
			case JAPANESE_SAMURAI -> fumikomiChargeProgress(relic, gameTime);
		};
	}

	public static long samuraiStabCooldownEnd(ItemStack relic) {
		return longValue(relic, SAMURAI_STAB_COOLDOWN_END_KEY, 0L);
	}

	public static void setSamuraiStabCooldownEnd(ItemStack relic, long end) {
		CustomData.update(DataComponents.CUSTOM_DATA, relic, tag -> tag.putLong(SAMURAI_STAB_COOLDOWN_END_KEY, end));
	}

	public static int samuraiStabCooldownProgress(ItemStack relic, long gameTime) {
		long remaining = Math.max(0L, samuraiStabCooldownEnd(relic) - gameTime);
		return (int)Math.clamp(
				(SAMURAI_STAB_COOLDOWN_TICKS - remaining) * 1000L / SAMURAI_STAB_COOLDOWN_TICKS,
				0L,
				1000L
		);
	}

	public static long guandaoComboCooldownEnd(ItemStack relic) {
		return longValue(relic, GUANDAO_COMBO_COOLDOWN_END_KEY, 0L);
	}

	public static void setGuandaoComboCooldownEnd(ItemStack relic, long end) {
		CustomData.update(DataComponents.CUSTOM_DATA, relic, tag -> tag.putLong(GUANDAO_COMBO_COOLDOWN_END_KEY, end));
	}

	public static int guandaoComboCooldownProgress(ItemStack relic, long gameTime) {
		long remaining = Math.max(0L, guandaoComboCooldownEnd(relic) - gameTime);
		return (int)Math.clamp(
				(GUANDAO_COMBO_COOLDOWN_TICKS - remaining) * 1000L / GUANDAO_COMBO_COOLDOWN_TICKS,
				0L,
				1000L
		);
	}

	public static long legionCooldownEnd(ItemStack relic) {
		return longValue(relic, LEGION_COOLDOWN_END_KEY, 0L);
	}

	public static void setLegionCooldownEnd(ItemStack relic, long end) {
		CustomData.update(DataComponents.CUSTOM_DATA, relic, tag -> tag.putLong(LEGION_COOLDOWN_END_KEY, end));
	}

	public static int summonCostPercent(ItemStack relic) {
		return hasTrait(relic, EchoTrait.LAZY) ? 85 : 100;
	}

	public static double maximumHealth(ItemStack relic) {
		return EchoRelicProgress.maximumHealth(EchoHeroType.fromRelic(relic), EchoRelicProgress.level(relic));
	}

	public static double attackDamage(ItemStack relic) {
		double value = EchoRelicProgress.attackDamage(EchoHeroType.fromRelic(relic), EchoRelicProgress.level(relic));
		if (hasTrait(relic, EchoTrait.COURAGE)) value += 1.0;
		return value;
	}

	public static double armor(ItemStack relic) {
		return EchoHeroType.fromRelic(relic).baseArmor() + (hasTrait(relic, EchoTrait.STURDY) ? 2.0 : 0.0);
	}

	public static double movementSpeed(ItemStack relic) {
		return EchoHeroType.fromRelic(relic).baseMovementSpeed() * movementPercent(relic) / 100.0;
	}

	public static double knockbackResistance(ItemStack relic) {
		return EchoHeroType.fromRelic(relic).baseKnockbackResistance();
	}

	public static int movementPercent(ItemStack relic) {
		return hasTrait(relic, EchoTrait.SKINNY) ? 110 : 100;
	}

	public static int attackSpeedPercent(ItemStack relic) {
		int base = Math.round(2000.0F / EchoHeroType.fromRelic(relic).baseAttackIntervalTicks());
		return hasTrait(relic, EchoTrait.SKINNY) ? Math.round(base * 1.10F) : base;
	}

	public static int addWiseGrowthExperience(ItemStack relic, int baseAmount) {
		if (baseAmount <= 0 || !hasTrait(relic, EchoTrait.WISE)) return Math.max(0, baseAmount);
		float remainder = relic.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
				.copyTag().getFloatOr(WISE_GROWTH_REMAINDER_KEY, 0.0F);
		float exact = baseAmount * 1.25F + remainder;
		int result = (int)Math.floor(exact);
		CustomData.update(DataComponents.CUSTOM_DATA, relic,
				tag -> tag.putFloat(WISE_GROWTH_REMAINDER_KEY, exact - result));
		return result;
	}

	public static int attackIntervalTicks(ItemStack relic) {
		int percent = attackSpeedPercent(relic);
		EchoHeroType heroType = EchoHeroType.fromRelic(relic);
		int calculated = Math.round(20.0F * 100.0F / percent);
		if (heroType == EchoHeroType.JAPANESE_SAMURAI) return Math.clamp(calculated, 24, 80);
		int minimum = heroType == EchoHeroType.EGYPTIAN_ARCHER ? 24 : 4;
		return Math.max(minimum, calculated);
	}

	private static int intValue(ItemStack stack, String key, int fallback) {
		return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getIntOr(key, fallback);
	}

	private static long longValue(ItemStack stack, String key, long fallback) {
		return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getLongOr(key, fallback);
	}

	private record TraitRoll(int mask, EchoBiomeAffinity biomeAffinity) {
	}

	public enum ActivityMode {
		FOLLOW, WAIT, WANDER;

		public static ActivityMode byOrdinal(int value) {
			return values()[Math.clamp(value, 0, values().length - 1)];
		}
	}

	public enum AlertMode {
		AGGRESSIVE, DEFENSIVE, PEACEFUL;

		public static AlertMode byOrdinal(int value) {
			return values()[Math.clamp(value, 0, values().length - 1)];
		}
	}

	public enum EgyptianArrowMode {
		OFF, LEAF, CONE;

		public EgyptianArrowMode next() {
			return values()[(this.ordinal() + 1) % values().length];
		}

		public static EgyptianArrowMode byOrdinal(int value) {
			return values()[Math.clamp(value, 0, values().length - 1)];
		}
	}
}
