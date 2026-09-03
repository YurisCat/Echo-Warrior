package com.yuriscat.echowarrior.item;

import net.minecraft.server.level.ServerPlayer;

public interface TalentExperienceHolder {
	int echoWarrior$consumeWiseBonus(int baseAmount);

	int echoWarrior$consumeMentorBonus(int baseAmount);

	static TalentExperienceHolder of(ServerPlayer player) {
		return (TalentExperienceHolder)player;
	}
}
