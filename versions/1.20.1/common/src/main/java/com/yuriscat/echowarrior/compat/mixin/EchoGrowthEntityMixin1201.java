package com.yuriscat.echowarrior.compat.mixin;

import com.yuriscat.echowarrior.compat.entity.EchoWarriorEntity1201;
import com.yuriscat.echowarrior.compat.item.EchoGrowthAttribute1201;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class EchoGrowthEntityMixin1201 {
    @Inject(method = "<init>", at = @At("TAIL"))
    private void echoWarrior$initializeGrowthRange(EntityType<? extends LivingEntity> type, Level level, CallbackInfo callback) {
        if (!((Object)this instanceof EchoWarriorEntity1201)) return;
        LivingEntity self = (LivingEntity)(Object)this;
        for (var attribute : java.util.List.of(Attributes.MAX_HEALTH, Attributes.ATTACK_DAMAGE)) {
            var instance = self.getAttribute(attribute);
            if (instance instanceof EchoGrowthAttribute1201 growth) growth.echoWarrior$enableGrowthRange();
        }
    }
}
