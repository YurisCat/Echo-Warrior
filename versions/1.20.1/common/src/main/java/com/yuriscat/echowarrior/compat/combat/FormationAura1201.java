package com.yuriscat.echowarrior.compat.combat;

import com.yuriscat.echowarrior.compat.ModEffects1201;
import net.minecraft.world.entity.LivingEntity;

public final class FormationAura1201 {
    private FormationAura1201() {
    }

    public static float modifyFinalIncomingDamage(LivingEntity entity, float damage) {
        return ModEffects1201.SHIELDS_RAISED != null && entity.hasEffect(ModEffects1201.SHIELDS_RAISED)
                ? damage * 0.85F : damage;
    }
}
