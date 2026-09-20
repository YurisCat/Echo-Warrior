package com.yuriscat.echowarrior.compat.effect;

import com.yuriscat.echowarrior.compat.EchoWarrior1201;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public final class SoldierFormationMobEffect1201 extends MobEffect {
    public SoldierFormationMobEffect1201() {
        super(MobEffectCategory.BENEFICIAL, 0xD8B55A);
        addAttributeModifier(Attributes.ATTACK_DAMAGE, com.yuriscat.echowarrior.compat.LegacyAttributes1201.id(EchoWarrior1201.id("soldier_formation_attack")).toString(),
                2.0, AttributeModifier.Operation.ADDITION);
    }
}
