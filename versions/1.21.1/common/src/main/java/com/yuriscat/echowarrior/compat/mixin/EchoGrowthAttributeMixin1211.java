package com.yuriscat.echowarrior.compat.mixin;

import com.yuriscat.echowarrior.compat.item.EchoGrowthAttribute1211;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AttributeInstance.class)
public abstract class EchoGrowthAttributeMixin1211 implements EchoGrowthAttribute1211 {
    @Unique private boolean echoWarrior$growthRange;
    @Shadow protected abstract void setDirty();

    @Override public void echoWarrior$enableGrowthRange() {
        this.echoWarrior$growthRange = true;
        this.setDirty();
    }

    @WrapOperation(method = "calculateValue", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/ai/attributes/Attribute;sanitizeValue(D)D"))
    private double echoWarrior$allowCultivatedAttribute(Attribute attribute, double value, Operation<Double> original) {
        double vanilla = original.call(attribute, value);
        return this.echoWarrior$growthRange && Double.isFinite(value) && value > vanilla ? value : vanilla;
    }
}
