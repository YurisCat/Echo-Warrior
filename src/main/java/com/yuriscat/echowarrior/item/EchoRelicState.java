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

	public static final int SKILL_COUNT = 5;
	public static final int ALL_SKILLS_ENABLED = (1 << SKILL_COUNT) - 1;
	public static final int MAX_SHIELD_CHARGES = 3;
	public static final long SHIELD_CHARGE_TICKS = 100L;
	public static final int MAX_PURSUIT_CHARGES = 2;
	public static final long PURSUIT_CHARGE_TICKS = 120L;
	public static final int MAX_BACKSTEP_CHARGES = 2;
	public static final long BACKSTEP_CHARGE_TICKS = 120L;
	public static final long EGYPTIAN_ARROW_SWITCH_COOLDOWN_TICKS = 10L;
	public static final long GUANDAO_COMBO_COOLDOWN_TICKS = 200L;

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

		int traitMask = rollTraits(random);
		EchoHeroType heroType = EchoHeroType.fromRelic(relic);
		CustomData.update(DataComponents.CUSTOM_DATA, relic, tag -> {
			tag.putBoolean(INITIALIZED_KEY, true);
			tag.putString(RELIC_ID_KEY, UUID.randomUUID().toString());
			tag.putInt(TRAIT_MASK_KEY, traitMask);
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
		});
		return true;
	}

	private static int rollTraits(RandomSource random) {
		int roll = random.nextInt(100);
		int count = roll < 10 ? 0 : roll < 35 ? 1 : roll < 75 ? 2 : roll < 95 ? 3 : 4;
		List<EchoTrait> available = new ArrayList<>(List.of(EchoTrait.values()));
		int mask = 0;
		while (Integer.bitCount(mask) < count && !available.isEmpty()) {
			EchoTrait selected = available.remove(random.nextInt(available.size()));
			mask |= selected.mask();
			if (selected == EchoTrait.BAD_TEMPER) {
				available.remove(EchoTrait.LAZY);
			} else if (selected == EchoTrait.LAZY) {
				available.remove(EchoTrait.BAD_TEMPER);
			} else if (selected == EchoTrait.SKINNY) {
				available.remove(EchoTrait.STURDY);
			} else if (selected == EchoTrait.STURDY) {
				available.remove(EchoTrait.SKINNY);
			}
		}
		return mask;
	}

	public static int rerollTraits(ItemStack relic, RandomSource random, long gameTime) {
		if (!(relic.getItem() instanceof EchoRelicItem)) return 0;
		int traitMask = rollTraits(random);
		CustomData.update(DataComponents.CUSTOM_DATA, relic, tag -> {
			tag.putBoolean(INITIALIZED_KEY, true);
			if (tag.getStringOr(RELIC_ID_KEY, "").isEmpty()) tag.putString(RELIC_ID_KEY, UUID.randomUUID().toString());
			tag.putInt(TRAIT_MASK_KEY, traitMask);
			tag.putLong(SHIELD_CHARGE_TIME_KEY, gameTime);
		});
		return traitMask;
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
		int enabled = intValue(relic, ENABLED_SKILLS_KEY, heroType.defaultEnabledSkillsMask()) & allowed;
		return heroType == EchoHeroType.GUANDAO_WARRIOR ? enabled | 0b0111 : enabled;
	}

	public static boolean skillEnabled(ItemStack relic, int skill) {
		return skill >= 0 && skill < EchoHeroType.fromRelic(relic).skillCount()
				&& (enabledSkills(relic) & 1 << skill) != 0;
	}

	public static void toggleSkill(ItemStack relic, int skill) {
		if (skill < 0 || skill >= EchoHeroType.fromRelic(relic).skillCount()) {
			return;
		}
		if (EchoHeroType.fromRelic(relic) == EchoHeroType.GUANDAO_WARRIOR && skill != 3) {
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
			case GUANDAO_WARRIOR -> guandaoComboCooldownEnd(relic) <= gameTime ? 1 : 0;
		};
	}

	public static int activeSkillMaximumCharges(ItemStack relic) {
		return switch (EchoHeroType.fromRelic(relic)) {
			case ROMAN_LEGIONARY -> MAX_SHIELD_CHARGES;
			case AZTEC_WARRIOR -> MAX_PURSUIT_CHARGES;
			case EGYPTIAN_ARCHER -> MAX_BACKSTEP_CHARGES;
			case GUANDAO_WARRIOR -> 1;
		};
	}

	public static int activeSkillChargeProgress(ItemStack relic, long gameTime) {
		return switch (EchoHeroType.fromRelic(relic)) {
			case ROMAN_LEGIONARY -> shieldChargeProgress(relic, gameTime);
			case AZTEC_WARRIOR -> pursuitChargeProgress(relic, gameTime);
			case EGYPTIAN_ARCHER -> backstepChargeProgress(relic, gameTime);
			case GUANDAO_WARRIOR -> guandaoComboCooldownProgress(relic, gameTime);
		};
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
		int percent = 100;
		if (hasTrait(relic, EchoTrait.BAD_TEMPER)) percent += 20;
		if (hasTrait(relic, EchoTrait.LAZY)) percent -= 20;
		return percent;
	}

	public static double maximumHealth(ItemStack relic) {
		double value = EchoRelicProgress.maximumHealth(EchoHeroType.fromRelic(relic), EchoRelicProgress.level(relic));
		return hasTrait(relic, EchoTrait.SKINNY) ? value * 0.75 : value;
	}

	public static double attackDamage(ItemStack relic) {
		double value = EchoRelicProgress.attackDamage(EchoHeroType.fromRelic(relic), EchoRelicProgress.level(relic));
		if (hasTrait(relic, EchoTrait.BAD_TEMPER)) value += 4.0;
		if (hasTrait(relic, EchoTrait.COURAGE)) value += 2.0;
		return value;
	}

	public static double armor(ItemStack relic) {
		return EchoHeroType.fromRelic(relic).baseArmor() + (hasTrait(relic, EchoTrait.STURDY) ? 4.0 : 0.0);
	}

	public static double movementSpeed(ItemStack relic) {
		return EchoHeroType.fromRelic(relic).baseMovementSpeed() * movementPercent(relic) / 100.0;
	}

	public static double knockbackResistance(ItemStack relic) {
		return EchoHeroType.fromRelic(relic).baseKnockbackResistance();
	}

	public static int movementPercent(ItemStack relic) {
		int percent = 100;
		if (hasTrait(relic, EchoTrait.LAZY)) percent -= 25;
		if (hasTrait(relic, EchoTrait.SKINNY)) percent += 25;
		if (hasTrait(relic, EchoTrait.STURDY)) percent -= 25;
		return percent;
	}

	public static int attackSpeedPercent(ItemStack relic) {
		int base = Math.round(2000.0F / EchoHeroType.fromRelic(relic).baseAttackIntervalTicks());
		return hasTrait(relic, EchoTrait.SKINNY) ? Math.round(base * 1.25F) : base;
	}

	public static int attackIntervalTicks(ItemStack relic) {
		int percent = attackSpeedPercent(relic);
		int minimum = EchoHeroType.fromRelic(relic) == EchoHeroType.EGYPTIAN_ARCHER ? 24 : 4;
		return Math.max(minimum, Math.round(20.0F * 100.0F / percent));
	}

	private static int intValue(ItemStack stack, String key, int fallback) {
		return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getIntOr(key, fallback);
	}

	private static long longValue(ItemStack stack, String key, long fallback) {
		return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getLongOr(key, fallback);
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
