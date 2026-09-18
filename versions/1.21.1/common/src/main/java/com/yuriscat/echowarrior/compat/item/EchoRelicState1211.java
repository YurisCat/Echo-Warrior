package com.yuriscat.echowarrior.compat.item;

import com.yuriscat.echowarrior.compat.EchoHeroType1211;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Persistent per-relic state shared by all five 1.21.1 heroes. */
public final class EchoRelicState1211 {
    private static final String INITIALIZED_KEY = "EchoWarriorTraitsInitialized";
    private static final String RELIC_ID_KEY = "EchoWarriorRelicId";
    private static final String TRAIT_MASK_KEY = "EchoWarriorTraitMask";
    private static final String BIOME_AFFINITY_KEY = "EchoWarriorBiomeAffinity";
    private static final String WISE_GROWTH_REMAINDER_KEY = "EchoWarriorWiseGrowthRemainder";
    private static final String ACTIVITY_MODE_KEY = "EchoWarriorActivityMode";
    private static final String ALERT_MODE_KEY = "EchoWarriorAlertMode";
    private static final String ENABLED_SKILLS_KEY = "EchoWarriorEnabledSkills";
    private static final String SKILL_LAYOUT_VERSION_KEY = "EchoWarriorSkillLayoutVersion";
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

    public static final int MAX_SKILL_COUNT = 5;
    private static final int CURRENT_SKILL_LAYOUT_VERSION = 2;
    private static final int ROMAN_BULWARK_ADDED_LAYOUT_VERSION = 1;
    private static final int ROMAN_BULWARK_SECOND_LAYOUT_VERSION = 2;
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

    private EchoRelicState1211() {
    }

    public static boolean ensureInitialized(ItemStack relic, RandomSource random) {
        return ensureInitialized(relic, random, 0L);
    }

    public static boolean ensureInitialized(ItemStack relic, RandomSource random, long gameTime) {
        if (!(relic.getItem() instanceof EchoRelicItem1211)) return false;
        if (initialized(relic)) {
            ensureRuntimeState(relic, gameTime);
            migrateSkillLayout(relic);
            return false;
        }
        TraitRoll traits = rollTraits(random);
        EchoHeroType1211 hero = EchoHeroType1211.fromRelic(relic);
        CustomData.update(DataComponents.CUSTOM_DATA, relic, tag -> {
            tag.putBoolean(INITIALIZED_KEY, true);
            tag.putString(RELIC_ID_KEY, UUID.randomUUID().toString());
            tag.putInt(TRAIT_MASK_KEY, traits.mask());
            tag.putInt(BIOME_AFFINITY_KEY, traits.biomeAffinity().ordinal());
            putRuntimeDefaults(tag, hero, gameTime);
        });
        return true;
    }

    private static void ensureRuntimeState(ItemStack relic, long gameTime) {
        EchoHeroType1211 hero = EchoHeroType1211.fromRelic(relic);
        CustomData.update(DataComponents.CUSTOM_DATA, relic, tag -> {
            if (!tag.contains(ACTIVITY_MODE_KEY)) tag.putInt(ACTIVITY_MODE_KEY, ActivityMode.FOLLOW.ordinal());
            if (!tag.contains(ALERT_MODE_KEY)) tag.putInt(ALERT_MODE_KEY, AlertMode.DEFENSIVE.ordinal());
            if (!tag.contains(ENABLED_SKILLS_KEY)) tag.putInt(ENABLED_SKILLS_KEY, hero.defaultEnabledSkillsMask());
            if (!tag.contains(SKILL_LAYOUT_VERSION_KEY)) tag.putInt(SKILL_LAYOUT_VERSION_KEY, CURRENT_SKILL_LAYOUT_VERSION);
            putChargeDefault(tag, SHIELD_CHARGES_KEY, SHIELD_CHARGE_TIME_KEY, MAX_SHIELD_CHARGES, gameTime);
            if (!tag.contains(LEGION_COOLDOWN_END_KEY)) tag.putLong(LEGION_COOLDOWN_END_KEY, 0L);
            putChargeDefault(tag, PURSUIT_CHARGES_KEY, PURSUIT_CHARGE_TIME_KEY, MAX_PURSUIT_CHARGES, gameTime);
            if (!tag.contains(PURSUIT_COOLDOWN_END_KEY)) tag.putLong(PURSUIT_COOLDOWN_END_KEY, 0L);
            if (!tag.contains(EGYPTIAN_ARROW_MODE_KEY)) tag.putInt(EGYPTIAN_ARROW_MODE_KEY, EgyptianArrowMode.OFF.ordinal());
            if (!tag.contains(EGYPTIAN_ARROW_SWITCH_TIME_KEY)) tag.putLong(EGYPTIAN_ARROW_SWITCH_TIME_KEY, Long.MIN_VALUE / 2L);
            putChargeDefault(tag, BACKSTEP_CHARGES_KEY, BACKSTEP_CHARGE_TIME_KEY, MAX_BACKSTEP_CHARGES, gameTime);
            if (!tag.contains(GUANDAO_COMBO_COOLDOWN_END_KEY)) tag.putLong(GUANDAO_COMBO_COOLDOWN_END_KEY, 0L);
            putChargeDefault(tag, FUMIKOMI_CHARGES_KEY, FUMIKOMI_CHARGE_TIME_KEY, MAX_FUMIKOMI_CHARGES, gameTime);
            if (!tag.contains(SAMURAI_STAB_COOLDOWN_END_KEY)) tag.putLong(SAMURAI_STAB_COOLDOWN_END_KEY, 0L);
        });
    }

    private static void putRuntimeDefaults(CompoundTag tag, EchoHeroType1211 hero, long gameTime) {
        tag.putInt(ACTIVITY_MODE_KEY, ActivityMode.FOLLOW.ordinal());
        tag.putInt(ALERT_MODE_KEY, AlertMode.DEFENSIVE.ordinal());
        tag.putInt(ENABLED_SKILLS_KEY, hero.defaultEnabledSkillsMask());
        tag.putInt(SKILL_LAYOUT_VERSION_KEY, CURRENT_SKILL_LAYOUT_VERSION);
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
    }

    private static void putChargeDefault(CompoundTag tag, String chargesKey, String timeKey, int maximum, long gameTime) {
        if (!tag.contains(chargesKey)) tag.putInt(chargesKey, maximum);
        if (!tag.contains(timeKey)) tag.putLong(timeKey, gameTime);
    }

    private static TraitRoll rollTraits(RandomSource random) {
        int roll = random.nextInt(100);
        int count = roll < 60 ? 2 : roll < 90 ? 3 : 4;
        List<EchoTrait1211> available = new ArrayList<>(List.of(EchoTrait1211.values()));
        int mask = 0;
        while (Integer.bitCount(mask) < count && !available.isEmpty()) {
            EchoTrait1211 selected = available.remove(random.nextInt(available.size()));
            mask |= selected.mask();
            available.removeIf(selected::conflictsWith);
        }
        EchoBiomeAffinity1211 affinity = EchoBiomeAffinity1211.values()[random.nextInt(EchoBiomeAffinity1211.values().length)];
        return new TraitRoll(mask, affinity);
    }

    public static int rerollTraits(ItemStack relic, RandomSource random) {
        return rerollTraits(relic, random, 0L);
    }

    public static int rerollTraits(ItemStack relic, RandomSource random, long gameTime) {
        if (!(relic.getItem() instanceof EchoRelicItem1211)) return 0;
        TraitRoll traits = rollTraits(random);
        CustomData.update(DataComponents.CUSTOM_DATA, relic, tag -> {
            tag.putBoolean(INITIALIZED_KEY, true);
            if (!tag.contains(RELIC_ID_KEY)) tag.putString(RELIC_ID_KEY, UUID.randomUUID().toString());
            tag.putInt(TRAIT_MASK_KEY, traits.mask());
            tag.putInt(BIOME_AFFINITY_KEY, traits.biomeAffinity().ordinal());
            tag.putLong(SHIELD_CHARGE_TIME_KEY, gameTime);
        });
        ensureRuntimeState(relic, gameTime);
        return traits.mask();
    }

    public static boolean initialized(ItemStack relic) { return tag(relic).getBoolean(INITIALIZED_KEY); }
    public static String relicId(ItemStack relic) { return tag(relic).getString(RELIC_ID_KEY); }
    public static int traitMask(ItemStack relic) { return tag(relic).getInt(TRAIT_MASK_KEY); }
    public static boolean hasTrait(ItemStack relic, EchoTrait1211 trait) { return (traitMask(relic) & trait.mask()) != 0; }
    public static EchoBiomeAffinity1211 biomeAffinity(ItemStack relic) {
        return EchoBiomeAffinity1211.byOrdinal(intValue(relic, BIOME_AFFINITY_KEY, EchoBiomeAffinity1211.WOODLAND.ordinal()));
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
        EchoHeroType1211 hero = EchoHeroType1211.fromRelic(relic);
        int allowed = hero.allSkillsEnabledMask();
        int enabled = intValue(relic, ENABLED_SKILLS_KEY, hero.defaultEnabledSkillsMask());
        int layoutVersion = intValue(relic, SKILL_LAYOUT_VERSION_KEY, CURRENT_SKILL_LAYOUT_VERSION);
        if (hero == EchoHeroType1211.ROMAN_LEGIONARY) {
            if (layoutVersion < ROMAN_BULWARK_ADDED_LAYOUT_VERSION) enabled |= 1 << 3;
            if (layoutVersion < ROMAN_BULWARK_SECOND_LAYOUT_VERSION) {
                int previous = enabled;
                enabled = previous & 1
                        | (previous & 1 << 3) >> 2
                        | (previous & 1 << 1) << 1
                        | (previous & 1 << 2) << 1;
            }
        }
        return enabled & allowed;
    }

    public static boolean skillEnabled(ItemStack relic, int skill) {
        return skill >= 0 && skill < EchoHeroType1211.fromRelic(relic).skillCount()
                && (enabledSkills(relic) & 1 << skill) != 0;
    }

    public static void toggleSkill(ItemStack relic, int skill) {
        EchoHeroType1211 hero = EchoHeroType1211.fromRelic(relic);
        if (skill < 0 || skill >= hero.skillCount()) return;
        int updated = enabledSkills(relic) ^ 1 << skill;
        CustomData.update(DataComponents.CUSTOM_DATA, relic, tag -> {
            tag.putInt(ENABLED_SKILLS_KEY, updated);
            tag.putInt(SKILL_LAYOUT_VERSION_KEY, CURRENT_SKILL_LAYOUT_VERSION);
        });
    }

    private static void migrateSkillLayout(ItemStack relic) {
        if (intValue(relic, SKILL_LAYOUT_VERSION_KEY, CURRENT_SKILL_LAYOUT_VERSION) >= CURRENT_SKILL_LAYOUT_VERSION) return;
        int enabled = enabledSkills(relic);
        CustomData.update(DataComponents.CUSTOM_DATA, relic, tag -> {
            tag.putInt(ENABLED_SKILLS_KEY, enabled);
            tag.putInt(SKILL_LAYOUT_VERSION_KEY, CURRENT_SKILL_LAYOUT_VERSION);
        });
    }

    public static EgyptianArrowMode egyptianArrowMode(ItemStack relic) {
        if (EchoHeroType1211.fromRelic(relic) != EchoHeroType1211.EGYPTIAN_ARCHER) return EgyptianArrowMode.OFF;
        return EgyptianArrowMode.byOrdinal(intValue(relic, EGYPTIAN_ARROW_MODE_KEY, EgyptianArrowMode.OFF.ordinal()));
    }

    public static boolean cycleEgyptianArrowMode(ItemStack relic, long gameTime) {
        if (EchoHeroType1211.fromRelic(relic) != EchoHeroType1211.EGYPTIAN_ARCHER) return false;
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
        updateCharges(relic, gameTime, SHIELD_CHARGES_KEY, SHIELD_CHARGE_TIME_KEY, MAX_SHIELD_CHARGES, SHIELD_CHARGE_TICKS);
        return intValue(relic, SHIELD_CHARGES_KEY, MAX_SHIELD_CHARGES);
    }
    public static int shieldChargeProgress(ItemStack relic, long gameTime) {
        return chargeProgress(relic, gameTime, SHIELD_CHARGES_KEY, SHIELD_CHARGE_TIME_KEY, MAX_SHIELD_CHARGES, SHIELD_CHARGE_TICKS);
    }
    public static boolean consumeShieldCharge(ItemStack relic, long gameTime) {
        return consumeCharge(relic, gameTime, SHIELD_CHARGES_KEY, SHIELD_CHARGE_TIME_KEY, MAX_SHIELD_CHARGES, SHIELD_CHARGE_TICKS);
    }
    public static int pursuitCharges(ItemStack relic, long gameTime) {
        updateCharges(relic, gameTime, PURSUIT_CHARGES_KEY, PURSUIT_CHARGE_TIME_KEY, MAX_PURSUIT_CHARGES, PURSUIT_CHARGE_TICKS);
        return intValue(relic, PURSUIT_CHARGES_KEY, MAX_PURSUIT_CHARGES);
    }
    public static int pursuitChargeProgress(ItemStack relic, long gameTime) {
        return chargeProgress(relic, gameTime, PURSUIT_CHARGES_KEY, PURSUIT_CHARGE_TIME_KEY, MAX_PURSUIT_CHARGES, PURSUIT_CHARGE_TICKS);
    }
    public static boolean consumePursuitCharge(ItemStack relic, long gameTime) {
        return consumeCharge(relic, gameTime, PURSUIT_CHARGES_KEY, PURSUIT_CHARGE_TIME_KEY, MAX_PURSUIT_CHARGES, PURSUIT_CHARGE_TICKS);
    }
    public static int backstepCharges(ItemStack relic, long gameTime) {
        updateCharges(relic, gameTime, BACKSTEP_CHARGES_KEY, BACKSTEP_CHARGE_TIME_KEY, MAX_BACKSTEP_CHARGES, BACKSTEP_CHARGE_TICKS);
        return intValue(relic, BACKSTEP_CHARGES_KEY, MAX_BACKSTEP_CHARGES);
    }
    public static int backstepChargeProgress(ItemStack relic, long gameTime) {
        return chargeProgress(relic, gameTime, BACKSTEP_CHARGES_KEY, BACKSTEP_CHARGE_TIME_KEY, MAX_BACKSTEP_CHARGES, BACKSTEP_CHARGE_TICKS);
    }
    public static boolean consumeBackstepCharge(ItemStack relic, long gameTime) {
        return consumeCharge(relic, gameTime, BACKSTEP_CHARGES_KEY, BACKSTEP_CHARGE_TIME_KEY, MAX_BACKSTEP_CHARGES, BACKSTEP_CHARGE_TICKS);
    }
    public static int fumikomiCharges(ItemStack relic, long gameTime) {
        updateCharges(relic, gameTime, FUMIKOMI_CHARGES_KEY, FUMIKOMI_CHARGE_TIME_KEY, MAX_FUMIKOMI_CHARGES, FUMIKOMI_CHARGE_TICKS);
        return intValue(relic, FUMIKOMI_CHARGES_KEY, MAX_FUMIKOMI_CHARGES);
    }
    public static int fumikomiChargeProgress(ItemStack relic, long gameTime) {
        return chargeProgress(relic, gameTime, FUMIKOMI_CHARGES_KEY, FUMIKOMI_CHARGE_TIME_KEY, MAX_FUMIKOMI_CHARGES, FUMIKOMI_CHARGE_TICKS);
    }
    public static boolean consumeFumikomiCharge(ItemStack relic, long gameTime) {
        return consumeCharge(relic, gameTime, FUMIKOMI_CHARGES_KEY, FUMIKOMI_CHARGE_TIME_KEY, MAX_FUMIKOMI_CHARGES, FUMIKOMI_CHARGE_TICKS);
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

    private static int chargeProgress(ItemStack relic, long gameTime, String chargesKey, String timeKey,
                                      int maximum, long rechargeTicks) {
        updateCharges(relic, gameTime, chargesKey, timeKey, maximum, rechargeTicks);
        int charges = intValue(relic, chargesKey, maximum);
        if (charges >= maximum) return 1000;
        long last = longValue(relic, timeKey, gameTime);
        return clampInt((gameTime - last) * 1000L / rechargeTicks, 0, 1000);
    }

    private static boolean consumeCharge(ItemStack relic, long gameTime, String chargesKey, String timeKey,
                                         int maximum, long rechargeTicks) {
        updateCharges(relic, gameTime, chargesKey, timeKey, maximum, rechargeTicks);
        int charges = intValue(relic, chargesKey, maximum);
        if (charges <= 0) return false;
        int remaining = charges - 1;
        CustomData.update(DataComponents.CUSTOM_DATA, relic, tag -> {
            tag.putInt(chargesKey, remaining);
            if (charges >= maximum) tag.putLong(timeKey, gameTime);
        });
        return true;
    }

    private static void updateCharges(ItemStack relic, long gameTime, String chargesKey, String timeKey,
                                      int maximum, long rechargeTicks) {
        int charges = intValue(relic, chargesKey, maximum);
        if (charges >= maximum) return;
        long last = longValue(relic, timeKey, gameTime);
        long elapsed = Math.max(0L, gameTime - last);
        int restored = (int)(elapsed / rechargeTicks);
        if (restored <= 0) return;
        int updated = Math.min(maximum, charges + restored);
        long updatedTime = updated >= maximum ? gameTime : last + restored * rechargeTicks;
        CustomData.update(DataComponents.CUSTOM_DATA, relic, tag -> {
            tag.putInt(chargesKey, updated);
            tag.putLong(timeKey, updatedTime);
        });
    }

    public static long legionCooldownEnd(ItemStack relic) { return longValue(relic, LEGION_COOLDOWN_END_KEY, 0L); }
    public static void setLegionCooldownEnd(ItemStack relic, long end) {
        CustomData.update(DataComponents.CUSTOM_DATA, relic, tag -> tag.putLong(LEGION_COOLDOWN_END_KEY, Math.max(0L, end)));
    }
    public static long pursuitCooldownEnd(ItemStack relic) { return longValue(relic, PURSUIT_COOLDOWN_END_KEY, 0L); }
    public static void setPursuitCooldownEnd(ItemStack relic, long end) {
        CustomData.update(DataComponents.CUSTOM_DATA, relic, tag -> tag.putLong(PURSUIT_COOLDOWN_END_KEY, Math.max(0L, end)));
    }
    public static long guandaoComboCooldownEnd(ItemStack relic) { return longValue(relic, GUANDAO_COMBO_COOLDOWN_END_KEY, 0L); }
    public static void setGuandaoComboCooldownEnd(ItemStack relic, long end) {
        CustomData.update(DataComponents.CUSTOM_DATA, relic, tag -> tag.putLong(GUANDAO_COMBO_COOLDOWN_END_KEY, Math.max(0L, end)));
    }
    public static int guandaoComboCooldownProgress(ItemStack relic, long gameTime) {
        return cooldownProgress(guandaoComboCooldownEnd(relic), gameTime, GUANDAO_COMBO_COOLDOWN_TICKS);
    }
    public static long samuraiStabCooldownEnd(ItemStack relic) { return longValue(relic, SAMURAI_STAB_COOLDOWN_END_KEY, 0L); }
    public static void setSamuraiStabCooldownEnd(ItemStack relic, long end) {
        CustomData.update(DataComponents.CUSTOM_DATA, relic, tag -> tag.putLong(SAMURAI_STAB_COOLDOWN_END_KEY, Math.max(0L, end)));
    }
    public static int samuraiStabCooldownProgress(ItemStack relic, long gameTime) {
        return cooldownProgress(samuraiStabCooldownEnd(relic), gameTime, SAMURAI_STAB_COOLDOWN_TICKS);
    }

    private static int cooldownProgress(long end, long gameTime, long cooldownTicks) {
        long remaining = Math.max(0L, end - gameTime);
        return clampInt((cooldownTicks - remaining) * 1000L / cooldownTicks, 0, 1000);
    }

    public static int activeSkillCharges(ItemStack relic, long gameTime) {
        return switch (EchoHeroType1211.fromRelic(relic)) {
            case ROMAN_LEGIONARY -> shieldCharges(relic, gameTime);
            case AZTEC_WARRIOR -> pursuitCharges(relic, gameTime);
            case EGYPTIAN_ARCHER -> backstepCharges(relic, gameTime);
            case GUANDAO_WARRIOR -> 0;
            case JAPANESE_SAMURAI -> fumikomiCharges(relic, gameTime);
        };
    }
    public static int activeSkillMaximumCharges(ItemStack relic) {
        return switch (EchoHeroType1211.fromRelic(relic)) {
            case ROMAN_LEGIONARY -> MAX_SHIELD_CHARGES;
            case AZTEC_WARRIOR -> MAX_PURSUIT_CHARGES;
            case EGYPTIAN_ARCHER -> MAX_BACKSTEP_CHARGES;
            case GUANDAO_WARRIOR -> 0;
            case JAPANESE_SAMURAI -> MAX_FUMIKOMI_CHARGES;
        };
    }
    public static int activeSkillChargeProgress(ItemStack relic, long gameTime) {
        return switch (EchoHeroType1211.fromRelic(relic)) {
            case ROMAN_LEGIONARY -> shieldChargeProgress(relic, gameTime);
            case AZTEC_WARRIOR -> pursuitChargeProgress(relic, gameTime);
            case EGYPTIAN_ARCHER -> backstepChargeProgress(relic, gameTime);
            case GUANDAO_WARRIOR -> guandaoComboCooldownProgress(relic, gameTime);
            case JAPANESE_SAMURAI -> fumikomiChargeProgress(relic, gameTime);
        };
    }

    public static int summonCostPercent(ItemStack relic) { return hasTrait(relic, EchoTrait1211.LAZY) ? 85 : 100; }
    public static int summonCost(ItemStack relic) {
        return (int)Math.ceil(SummonerFuel1211.SUMMON_COST * summonCostPercent(relic) / 100.0);
    }
    public static double naturalHealingCost(ItemStack relic, int baseCost) {
        return Math.max(0, baseCost) * summonCostPercent(relic) / 100.0;
    }
    public static double maximumHealth(ItemStack relic) {
        return EchoRelicProgress1211.maximumHealth(EchoHeroType1211.fromRelic(relic), EchoRelicProgress1211.level(relic));
    }
    public static double attackDamage(ItemStack relic) {
        double value = EchoRelicProgress1211.attackDamage(EchoHeroType1211.fromRelic(relic), EchoRelicProgress1211.level(relic));
        return hasTrait(relic, EchoTrait1211.COURAGE) ? value + 1.0 : value;
    }
    public static double armor(ItemStack relic) {
        return EchoHeroType1211.fromRelic(relic).armor() + (hasTrait(relic, EchoTrait1211.STURDY) ? 2.0 : 0.0);
    }
    public static double movementSpeed(ItemStack relic) {
        return EchoHeroType1211.fromRelic(relic).movementSpeed() * movementPercent(relic) / 100.0;
    }
    public static double knockbackResistance(ItemStack relic) { return EchoHeroType1211.fromRelic(relic).knockbackResistance(); }
    public static int movementPercent(ItemStack relic) { return hasTrait(relic, EchoTrait1211.SKINNY) ? 110 : 100; }
    public static int attackSpeedPercent(ItemStack relic) {
        int base = Math.round(2000.0F / EchoHeroType1211.fromRelic(relic).baseAttackIntervalTicks());
        return hasTrait(relic, EchoTrait1211.SKINNY) ? Math.round(base * 1.10F) : base;
    }
    public static int attackIntervalTicks(ItemStack relic) {
        int calculated = Math.round(20.0F * 100.0F / attackSpeedPercent(relic));
        EchoHeroType1211 hero = EchoHeroType1211.fromRelic(relic);
        if (hero == EchoHeroType1211.JAPANESE_SAMURAI) return Math.max(24, Math.min(80, calculated));
        return Math.max(hero == EchoHeroType1211.EGYPTIAN_ARCHER ? 24 : 4, calculated);
    }
    public static int addWiseGrowthExperience(ItemStack relic, int baseAmount) {
        if (baseAmount <= 0 || !hasTrait(relic, EchoTrait1211.WISE)) return Math.max(0, baseAmount);
        float remainder = floatValue(relic, WISE_GROWTH_REMAINDER_KEY, 0.0F);
        float exact = baseAmount * 1.25F + remainder;
        int result = (int)Math.floor(exact);
        CustomData.update(DataComponents.CUSTOM_DATA, relic,
                tag -> tag.putFloat(WISE_GROWTH_REMAINDER_KEY, exact - result));
        return result;
    }

    public static void setTraitsForSelfTest(ItemStack relic, int mask, EchoBiomeAffinity1211 affinity) {
        CustomData.update(DataComponents.CUSTOM_DATA, relic, tag -> {
            tag.putBoolean(INITIALIZED_KEY, true);
            if (!tag.contains(RELIC_ID_KEY)) tag.putString(RELIC_ID_KEY, UUID.randomUUID().toString());
            tag.putInt(TRAIT_MASK_KEY, mask);
            tag.putInt(BIOME_AFFINITY_KEY, affinity.ordinal());
        });
        ensureRuntimeState(relic, 0L);
    }

    /** Imports fields used by the first Roman-only compatibility saves. */
    public static void importLegacyBindingState(ItemStack relic, int activityMode, int alertMode, int enabledSkills,
                                                int shieldCharges, long shieldChargeTime, long legionCooldownEnd) {
        if (!(relic.getItem() instanceof EchoRelicItem1211)) return;
        CustomData.update(DataComponents.CUSTOM_DATA, relic, tag -> {
            if (!tag.contains(ACTIVITY_MODE_KEY)) tag.putInt(ACTIVITY_MODE_KEY, ActivityMode.byOrdinal(activityMode).ordinal());
            if (!tag.contains(ALERT_MODE_KEY)) tag.putInt(ALERT_MODE_KEY, AlertMode.byOrdinal(alertMode).ordinal());
            if (!tag.contains(ENABLED_SKILLS_KEY)) tag.putInt(ENABLED_SKILLS_KEY, enabledSkills);
            if (!tag.contains(SKILL_LAYOUT_VERSION_KEY)) tag.putInt(SKILL_LAYOUT_VERSION_KEY, CURRENT_SKILL_LAYOUT_VERSION);
            if (!tag.contains(SHIELD_CHARGES_KEY)) tag.putInt(SHIELD_CHARGES_KEY, Math.max(0, Math.min(MAX_SHIELD_CHARGES, shieldCharges)));
            if (!tag.contains(SHIELD_CHARGE_TIME_KEY)) tag.putLong(SHIELD_CHARGE_TIME_KEY, shieldChargeTime);
            if (!tag.contains(LEGION_COOLDOWN_END_KEY)) tag.putLong(LEGION_COOLDOWN_END_KEY, legionCooldownEnd);
        });
        ensureRuntimeState(relic, shieldChargeTime);
    }

    private static CompoundTag tag(ItemStack relic) {
        return relic.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }
    private static int intValue(ItemStack stack, String key, int fallback) {
        CompoundTag tag = tag(stack);
        return tag.contains(key) ? tag.getInt(key) : fallback;
    }
    private static long longValue(ItemStack stack, String key, long fallback) {
        CompoundTag tag = tag(stack);
        return tag.contains(key) ? tag.getLong(key) : fallback;
    }
    private static float floatValue(ItemStack stack, String key, float fallback) {
        CompoundTag tag = tag(stack);
        return tag.contains(key) ? tag.getFloat(key) : fallback;
    }
    private static int clampInt(long value, int minimum, int maximum) {
        return (int)Math.max(minimum, Math.min(maximum, value));
    }

    private record TraitRoll(int mask, EchoBiomeAffinity1211 biomeAffinity) {
    }

    public enum ActivityMode {
        FOLLOW, WAIT, WANDER;
        public static ActivityMode byOrdinal(int value) {
            return values()[Math.max(0, Math.min(values().length - 1, value))];
        }
    }
    public enum AlertMode {
        AGGRESSIVE, DEFENSIVE, PEACEFUL;
        public static AlertMode byOrdinal(int value) {
            return values()[Math.max(0, Math.min(values().length - 1, value))];
        }
    }
    public enum EgyptianArrowMode {
        OFF, LEAF, CONE;
        public EgyptianArrowMode next() { return values()[(ordinal() + 1) % values().length]; }
        public static EgyptianArrowMode byOrdinal(int value) {
            return values()[Math.max(0, Math.min(values().length - 1, value))];
        }
    }
}
