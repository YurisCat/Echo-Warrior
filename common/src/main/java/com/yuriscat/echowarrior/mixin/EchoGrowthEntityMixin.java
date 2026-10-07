package com.yuriscat.echowarrior.mixin;

import com.yuriscat.echowarrior.entity.EchoWarriorEntity;
import com.yuriscat.echowarrior.item.EchoGrowthAttribute;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class EchoGrowthEntityMixin {
    @Inject(method = "<init>", at = @At("TAIL"))
    private void echoWarrior$initializeGrowthRange(EntityType<? extends LivingEntity> type, Level level, CallbackInfo callback) {
        if (!((Object)this instanceof EchoWarriorEntity)) return;
        LivingEntity self = (LivingEntity)(Object)this;
        for (var attribute : java.util.List.of(Attributes.MAX_HEALTH, Attributes.ATTACK_DAMAGE)) {
            var instance = self.getAttribute(attribute);
            if (instance instanceof EchoGrowthAttribute growth) growth.echoWarrior$enableGrowthRange();
        }
    }
}
