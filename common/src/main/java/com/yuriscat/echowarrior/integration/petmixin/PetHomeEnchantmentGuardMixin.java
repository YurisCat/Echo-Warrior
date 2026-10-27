package com.yuriscat.echowarrior.integration.petmixin;
import com.yuriscat.echowarrior.integration.PetCollarCompat;
import com.yuriscat.echowarrior.entity.EchoWarriorEntity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.resources.ResourceKey;
@Pseudo
@Mixin(targets = "com.github.yzqdev.pethome.util.TameableUtils", remap = false)
public abstract class PetHomeEnchantmentGuardMixin {
    @Inject(target = @Desc(value = "getEnchantLevel", args = {LivingEntity.class, ResourceKey.class}, ret = int.class),
            at = @At("HEAD"), cancellable = true)
    private static void echoWarrior$rejectIndependentRecreation(LivingEntity entity, ResourceKey enchantment,
            CallbackInfoReturnable<Integer> ci) {
        if (!(entity instanceof EchoWarriorEntity)) return;
        var key = enchantment.identifier();
        if (key != null && (key.getPath().equals("undead_curse") || key.getPath().equals("total_recall")
                || key.getPath().equals("immaturity_curse")))
            ci.setReturnValue(0);
    }
    @com.llamalad7.mixinextras.injector.ModifyExpressionValue(method = "onUpdateEnchants",
            at = @At(value = "INVOKE", target = "Ljava/util/Map;containsKey(Ljava/lang/Object;)Z"))
    private static boolean echoWarrior$noAnimalAgeCast(boolean present,
            @com.llamalad7.mixinextras.sugar.Local(argsOnly = true) LivingEntity entity) {
        return !(entity instanceof EchoWarriorEntity) && present;
    }
}
