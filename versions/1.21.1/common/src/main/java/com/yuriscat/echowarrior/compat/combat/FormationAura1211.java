package com.yuriscat.echowarrior.compat.combat;

import com.yuriscat.echowarrior.compat.ModEffects1211;
import net.minecraft.world.entity.LivingEntity;

public final class FormationAura1211 {
    private FormationAura1211() {
    }

    public static float modifyFinalIncomingDamage(LivingEntity entity, float damage) {
        return ModEffects1211.SHIELDS_RAISED != null && entity.hasEffect(ModEffects1211.SHIELDS_RAISED)
                ? damage * 0.85F : damage;
    }
}
