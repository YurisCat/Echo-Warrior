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

	public static final int SKILL_COUNT = 3;
	public static final int ALL_SKILLS_ENABLED = (1 << SKILL_COUNT) - 1;
	public static final int MAX_SHIELD_CHARGES = 3;
	public static final long SHIELD_CHARGE_TICKS = 100L;

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
		CustomData.update(DataComponents.CUSTOM_DATA, relic, tag -> {
			tag.putBoolean(INITIALIZED_KEY, true);
			tag.putString(RELIC_ID_KEY, UUID.randomUUID().toString());
			tag.putInt(TRAIT_MASK_KEY, traitMask);
			tag.putInt(ACTIVITY_MODE_KEY, ActivityMode.FOLLOW.ordinal());
			tag.putInt(ALERT_MODE_KEY, AlertMode.DEFENSIVE.ordinal());
			tag.putInt(ENABLED_SKILLS_KEY, ALL_SKILLS_ENABLED);
			tag.putInt(SHIELD_CHARGES_KEY, MAX_SHIELD_CHARGES);
			tag.putLong(SHIELD_CHARGE_TIME_KEY, gameTime);
			tag.putLong(LEGION_COOLDOWN_END_KEY, 0L);
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
		return intValue(relic, ENABLED_SKILLS_KEY, ALL_SKILLS_ENABLED) & ALL_SKILLS_ENABLED;
	}

	public static boolean skillEnabled(ItemStack relic, int skill) {
		return skill >= 0 && skill < SKILL_COUNT && (enabledSkills(relic) & 1 << skill) != 0;
	}

	public static void toggleSkill(ItemStack relic, int skill) {
		if (skill < 0 || skill >= SKILL_COUNT) {
			return;
		}
		int updated = enabledSkills(relic) ^ 1 << skill;
		CustomData.update(DataComponents.CUSTOM_DATA, relic, tag -> tag.putInt(ENABLED_SKILLS_KEY, updated));
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
		int charges = shieldCharges(relic, gameTime);
		if (charges <= 0) {
			return false;
		}
		int remaining = charges - 1;
		CustomData.update(DataComponents.CUSTOM_DATA, relic, tag -> {
			tag.putInt(SHIELD_CHARGES_KEY, remaining);
			if (remaining < MAX_SHIELD_CHARGES) {
				tag.putLong(SHIELD_CHARGE_TIME_KEY, gameTime);
			}
		});
		return true;
	}

	private static void updateShieldCharges(ItemStack relic, long gameTime) {
		int charges = intValue(relic, SHIELD_CHARGES_KEY, MAX_SHIELD_CHARGES);
		if (charges >= MAX_SHIELD_CHARGES) {
			return;
		}
		long last = longValue(relic, SHIELD_CHARGE_TIME_KEY, gameTime);
		long elapsed = Math.max(0L, gameTime - last);
		int restored = (int)(elapsed / SHIELD_CHARGE_TICKS);
		if (restored <= 0) {
			return;
		}
		int updated = Math.min(MAX_SHIELD_CHARGES, charges + restored);
		long updatedTime = updated >= MAX_SHIELD_CHARGES ? gameTime : last + restored * SHIELD_CHARGE_TICKS;
		CustomData.update(DataComponents.CUSTOM_DATA, relic, tag -> {
			tag.putInt(SHIELD_CHARGES_KEY, updated);
			tag.putLong(SHIELD_CHARGE_TIME_KEY, updatedTime);
		});
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
		double value = EchoRelicProgress.maximumHealth(EchoRelicProgress.level(relic));
		return hasTrait(relic, EchoTrait.SKINNY) ? value * 0.75 : value;
	}

	public static double attackDamage(ItemStack relic) {
		double value = EchoRelicProgress.attackDamage(EchoRelicProgress.level(relic));
		if (hasTrait(relic, EchoTrait.BAD_TEMPER)) value += 4.0;
		if (hasTrait(relic, EchoTrait.COURAGE)) value += 2.0;
		return value;
	}

	public static double armor(ItemStack relic) {
		return 8.0 + (hasTrait(relic, EchoTrait.STURDY) ? 4.0 : 0.0);
	}

	public static int movementPercent(ItemStack relic) {
		int percent = 100;
		if (hasTrait(relic, EchoTrait.LAZY)) percent -= 25;
		if (hasTrait(relic, EchoTrait.SKINNY)) percent += 25;
		if (hasTrait(relic, EchoTrait.STURDY)) percent -= 25;
		return percent;
	}

	public static int attackSpeedPercent(ItemStack relic) {
		return hasTrait(relic, EchoTrait.SKINNY) ? 125 : 100;
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
}
