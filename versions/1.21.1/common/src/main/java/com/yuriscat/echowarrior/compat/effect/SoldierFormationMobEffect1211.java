package com.yuriscat.echowarrior.compat.effect;

import com.yuriscat.echowarrior.compat.EchoWarrior1211;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public final class SoldierFormationMobEffect1211 extends MobEffect {
    public SoldierFormationMobEffect1211() {
        super(MobEffectCategory.BENEFICIAL, 0xD8B55A);
        addAttributeModifier(Attributes.ATTACK_DAMAGE, EchoWarrior1211.id("soldier_formation_attack"),
                2.0, AttributeModifier.Operation.ADD_VALUE);
    }
}
