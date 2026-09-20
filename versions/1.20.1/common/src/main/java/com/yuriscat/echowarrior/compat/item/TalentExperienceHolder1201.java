package com.yuriscat.echowarrior.compat.item;

import net.minecraft.server.level.ServerPlayer;

public interface TalentExperienceHolder1201 {
    int echoWarrior1201$consumeWiseBonus(int baseAmount);
    int echoWarrior1201$consumeMentorBonus(int baseAmount);

    static TalentExperienceHolder1201 of(ServerPlayer player) {
        return (TalentExperienceHolder1201)player;
    }
}
