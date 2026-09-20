package com.yuriscat.echowarrior.compat.effect;

import com.yuriscat.echowarrior.compat.EchoWarrior1201;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public final class SunBlessingMobEffect1201 extends MobEffect {
    public SunBlessingMobEffect1201() {
        super(MobEffectCategory.BENEFICIAL, 0xF2A12B);
        addAttributeModifier(Attributes.ATTACK_DAMAGE, com.yuriscat.echowarrior.compat.LegacyAttributes1201.id(EchoWarrior1201.id("huitzilopochtli_attack")).toString(),
                0.15, AttributeModifier.Operation.MULTIPLY_TOTAL);
    }
}
