package com.yuriscat.echowarrior.compat.integration.petmixin;
import com.yuriscat.echowarrior.compat.integration.PetCollarCompat1211;
import com.yuriscat.echowarrior.compat.entity.EchoWarriorEntity1211;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.resources.ResourceKey;
@Pseudo
@Mixin(targets = "com.github.yzqdev.pethome.util.TameableUtils", remap = false)
public abstract class PetHomeEnchantmentGuardMixin1211 {
    @Inject(target = @Desc(value = "getEnchantLevel", args = {LivingEntity.class, ResourceKey.class}, ret = int.class),
            at = @At("HEAD"), cancellable = true)
    private static void echoWarrior$rejectIndependentRecreation(LivingEntity entity, ResourceKey enchantment,
            CallbackInfoReturnable<Integer> ci) {
        if (!(entity instanceof EchoWarriorEntity1211)) return;
        var key = enchantment.location();
        if (key != null && (key.getPath().equals("undead_curse") || key.getPath().equals("total_recall")))
            ci.setReturnValue(0);
    }
}
