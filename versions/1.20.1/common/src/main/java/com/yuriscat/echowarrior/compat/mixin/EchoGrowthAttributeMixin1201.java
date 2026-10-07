package com.yuriscat.echowarrior.compat.mixin;

import com.yuriscat.echowarrior.compat.item.EchoGrowthAttribute1201;
import org.spongepowered.asm.mixin.injection.Redirect;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AttributeInstance.class)
public abstract class EchoGrowthAttributeMixin1201 implements EchoGrowthAttribute1201 {
    @Unique private boolean echoWarrior$growthRange;
    @Shadow protected abstract void setDirty();

    @Override public void echoWarrior$enableGrowthRange() {
        this.echoWarrior$growthRange = true;
        this.setDirty();
    }

    @Redirect(method = "calculateValue", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/ai/attributes/Attribute;sanitizeValue(D)D"))
    private double echoWarrior$allowCultivatedAttribute(Attribute attribute, double value) {
        double vanilla = attribute.sanitizeValue(value);
        return this.echoWarrior$growthRange && Double.isFinite(value) && value > vanilla ? value : vanilla;
    }
}
