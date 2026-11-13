package com.yuriscat.echowarrior.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class EchoRelicProgress {
	public static int maxLevel() { return EchoProgressionConfig.maxLevel(); }
	private static final String LEVEL_KEY = "EchoWarriorLevel";
	private static final String EXPERIENCE_KEY = "EchoWarriorExperience";

	private EchoRelicProgress() {
	}

	public static int storedLevel(ItemStack relic) {
		int stored = relic.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
				.copyTag()
				.getIntOr(LEVEL_KEY, 1);
		return Math.max(1, stored);
	}

    public static int level(ItemStack relic) {
        return Math.min(storedLevel(relic), maxLevel());
    }

	public static int experience(ItemStack relic) {
		if (level(relic) >= maxLevel()) {
			return 0;
		}
		int stored = relic.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
				.copyTag()
				.getIntOr(EXPERIENCE_KEY, 0);
		return Math.clamp(stored, 0, experienceNeeded(level(relic)) - 1);
	}

	public static int experienceNeeded(int level) {
		return level >= maxLevel() ? 0 : 15 + 2 * Math.clamp(level, 1, maxLevel());
	}

	public static double maximumHealth(EchoHeroType heroType, int level) {
		return heroType.baseMaximumHealth() * growthMultiplier(level);
	}

	public static double attackDamage(EchoHeroType heroType, int level) {
		return heroType.baseAttackDamage() * growthMultiplier(level);
	}

	private static double growthMultiplier(int level) {
		int clamped = Math.clamp(level, 1, maxLevel());
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
			if (newExperience < needed) {
				break;
			}
			newExperience -= needed;
			newLevel++;
		}
		if (newLevel >= maxLevel()) {
			newLevel = maxLevel();
			newExperience = 0;
		}

		int finalLevel = newLevel;
		int finalExperience = (int) newExperience;
		CustomData.update(DataComponents.CUSTOM_DATA, relic, tag -> {
			tag.putInt(LEVEL_KEY, finalLevel);
			tag.putInt(EXPERIENCE_KEY, finalExperience);
		});
		return new ProgressResult(oldLevel, newLevel, oldExperience, finalExperience, newLevel - oldLevel);
	}

	public record ProgressResult(
			int oldLevel,
			int newLevel,
			int oldExperience,
			int newExperience,
			int levelsGained
	) {
	}
}
