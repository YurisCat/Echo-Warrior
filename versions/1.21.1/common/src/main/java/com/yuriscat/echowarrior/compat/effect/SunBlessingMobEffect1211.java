package com.yuriscat.echowarrior.compat.effect;

import com.yuriscat.echowarrior.compat.EchoWarrior1211;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public final class SunBlessingMobEffect1211 extends MobEffect {
    public SunBlessingMobEffect1211() {
        super(MobEffectCategory.BENEFICIAL, 0xF2A12B);
        addAttributeModifier(Attributes.ATTACK_DAMAGE, EchoWarrior1211.id("huitzilopochtli_attack"),
                0.15, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }
}
