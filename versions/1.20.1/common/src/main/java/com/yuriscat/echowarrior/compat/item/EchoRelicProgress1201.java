package com.yuriscat.echowarrior.compat.item;

import com.yuriscat.echowarrior.compat.EchoHeroType1201;
import net.minecraft.world.item.ItemStack;

public final class EchoRelicProgress1201 {
    public static int maxLevel() { return EchoProgressionConfig1201.maxLevel(); }
    private static final String LEVEL_KEY = "EchoWarriorLevel";
    private static final String EXPERIENCE_KEY = "EchoWarriorExperience";

    private EchoRelicProgress1201() {
    }

    public static int storedLevel(ItemStack relic) {
        int stored = RelicNbt1201.read(relic).getInt(LEVEL_KEY);
        return Math.max(1, stored);
    }

    public static int level(ItemStack relic) {
        return Math.min(storedLevel(relic), maxLevel());
    }

    public static int experience(ItemStack relic) {
        int level = level(relic);
        if (level >= maxLevel()) return 0;
        int stored = RelicNbt1201.read(relic).getInt(EXPERIENCE_KEY);
        return Math.max(0, Math.min(experienceNeeded(level) - 1, stored));
    }

    public static int experienceNeeded(int level) {
        return level >= maxLevel() ? 0 : 15 + 2 * Math.max(1, Math.min(maxLevel(), level));
    }

    public static double maximumHealth(EchoHeroType1201 heroType, int level) {
        return heroType.maximumHealth() * growthMultiplier(level);
    }

    public static double attackDamage(EchoHeroType1201 heroType, int level) {
        return heroType.attackDamage() * growthMultiplier(level);
    }

    private static double growthMultiplier(int level) {
        int clamped = Math.max(1, Math.min(maxLevel(), level));
        return 1.0 + (clamped - 1.0) / 29.0;
    }

    public static ProgressResult addExperience(ItemStack relic, int amount) {
        int oldLevel = level(relic);
        int oldExperience = experience(relic);
        if (amount <= 0 || oldLevel >= maxLevel()) {
            return new ProgressResult(oldLevel, oldLevel, oldExperience, oldExperience, 0);
        }

        int newLevel = oldLevel;
        long newExperience = (long) oldExperience + amount;
        while (newLevel < maxLevel()) {
            int needed = experienceNeeded(newLevel);
            if (newExperience < needed) break;
            newExperience -= needed;
            newLevel++;
        }
        if (newLevel >= maxLevel()) {
            newLevel = maxLevel();
            newExperience = 0;
        }

        int finalLevel = newLevel;
        int finalExperience = (int) newExperience;
        RelicNbt1201.update(relic, tag -> {
            tag.putInt(LEVEL_KEY, finalLevel);
            tag.putInt(EXPERIENCE_KEY, finalExperience);
        });
        return new ProgressResult(oldLevel, newLevel, oldExperience, finalExperience, newLevel - oldLevel);
    }

    public record ProgressResult(int oldLevel, int newLevel, int oldExperience, int newExperience, int levelsGained) {
    }
}
