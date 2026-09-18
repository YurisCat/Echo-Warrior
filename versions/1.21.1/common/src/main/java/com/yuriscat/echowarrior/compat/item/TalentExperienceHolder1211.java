package com.yuriscat.echowarrior.compat.item;

import net.minecraft.server.level.ServerPlayer;

public interface TalentExperienceHolder1211 {
    int echoWarrior1211$consumeWiseBonus(int baseAmount);
    int echoWarrior1211$consumeMentorBonus(int baseAmount);

    static TalentExperienceHolder1211 of(ServerPlayer player) {
        return (TalentExperienceHolder1211)player;
    }
}
