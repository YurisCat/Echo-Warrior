package com.yuriscat.echowarrior.compat.item;

import com.yuriscat.echowarrior.compat.EchoHeroType1211;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class EchoRelicProgress1211 {
    public static final int MAX_LEVEL = 30;
    private static final String LEVEL_KEY = "EchoWarriorLevel";
    private static final String EXPERIENCE_KEY = "EchoWarriorExperience";

    private EchoRelicProgress1211() {
    }

    public static int level(ItemStack relic) {
        int stored = relic.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getInt(LEVEL_KEY);
        return stored <= 0 ? 1 : Math.max(1, Math.min(MAX_LEVEL, stored));
    }

    public static int experience(ItemStack relic) {
        int level = level(relic);
        if (level >= MAX_LEVEL) return 0;
        int stored = relic.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getInt(EXPERIENCE_KEY);
        return Math.max(0, Math.min(experienceNeeded(level) - 1, stored));
    }

    public static int experienceNeeded(int level) {
        return level >= MAX_LEVEL ? 0 : 15 + 2 * Math.max(1, Math.min(MAX_LEVEL, level));
    }

    public static double maximumHealth(EchoHeroType1211 heroType, int level) {
        return heroType.maximumHealth() * growthMultiplier(level);
    }

    public static double attackDamage(EchoHeroType1211 heroType, int level) {
        return heroType.attackDamage() * growthMultiplier(level);
    }

    private static double growthMultiplier(int level) {
        int clamped = Math.max(1, Math.min(MAX_LEVEL, level));
        return 1.0 + (clamped - 1.0) / (MAX_LEVEL - 1.0);
    }

    public static ProgressResult addExperience(ItemStack relic, int amount) {
        int oldLevel = level(relic);
        int oldExperience = experience(relic);
        if (amount <= 0 || oldLevel >= MAX_LEVEL) {
            return new ProgressResult(oldLevel, oldLevel, oldExperience, oldExperience, 0);
        }

        int newLevel = oldLevel;
        int newExperience = oldExperience + amount;
        while (newLevel < MAX_LEVEL) {
            int needed = experienceNeeded(newLevel);
            if (newExperience < needed) break;
            newExperience -= needed;
            newLevel++;
        }
        if (newLevel >= MAX_LEVEL) {
            newLevel = MAX_LEVEL;
            newExperience = 0;
        }

        int finalLevel = newLevel;
        int finalExperience = newExperience;
        CustomData.update(DataComponents.CUSTOM_DATA, relic, tag -> {
            tag.putInt(LEVEL_KEY, finalLevel);
            tag.putInt(EXPERIENCE_KEY, finalExperience);
        });
        return new ProgressResult(oldLevel, newLevel, oldExperience, newExperience, newLevel - oldLevel);
    }

    public record ProgressResult(int oldLevel, int newLevel, int oldExperience, int newExperience, int levelsGained) {
    }
}
