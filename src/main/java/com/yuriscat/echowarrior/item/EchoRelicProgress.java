package com.yuriscat.echowarrior.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class EchoRelicProgress {
	public static final int MAX_LEVEL = 30;
	private static final String LEVEL_KEY = "EchoWarriorLevel";
	private static final String EXPERIENCE_KEY = "EchoWarriorExperience";

	private EchoRelicProgress() {
	}

	public static int level(ItemStack relic) {
		int stored = relic.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
				.copyTag()
				.getIntOr(LEVEL_KEY, 1);
		return Math.clamp(stored, 1, MAX_LEVEL);
	}

	public static int experience(ItemStack relic) {
		if (level(relic) >= MAX_LEVEL) {
			return 0;
		}
		int stored = relic.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
				.copyTag()
				.getIntOr(EXPERIENCE_KEY, 0);
		return Math.clamp(stored, 0, experienceNeeded(level(relic)) - 1);
	}

	public static int experienceNeeded(int level) {
		return level >= MAX_LEVEL ? 0 : 15 + 2 * Math.clamp(level, 1, MAX_LEVEL);
	}

	public static double maximumHealth(int level) {
		int clamped = Math.clamp(level, 1, MAX_LEVEL);
		return clamped == MAX_LEVEL ? 60.0 : 29.0 + clamped;
	}

	public static double attackDamage(int level) {
		return 6.0 + Math.clamp(level, 1, MAX_LEVEL) / 5;
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
			if (newExperience < needed) {
				break;
			}
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

	public record ProgressResult(
			int oldLevel,
			int newLevel,
			int oldExperience,
			int newExperience,
			int levelsGained
	) {
	}
}
