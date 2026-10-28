package com.yuriscat.echowarrior.compat.integration.petmixin;
import com.yuriscat.echowarrior.compat.integration.PetCollarCompat1201;
import com.yuriscat.echowarrior.compat.entity.EchoWarriorEntity1201;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.resources.ResourceKey;
@Pseudo
@Mixin(targets = "com.github.alexthe668.domesticationinnovation.server.entity.TameableUtils", remap = false)
public abstract class DomesticationEnchantmentGuardMixin1201 {
    @Inject(target = @Desc(value = "getEnchantLevel", args = {LivingEntity.class, Enchantment.class}, ret = int.class),
            at = @At("HEAD"), cancellable = true)
    private static void echoWarrior$rejectIndependentRecreation(LivingEntity entity, Enchantment enchantment,
            CallbackInfoReturnable<Integer> ci) {
        if (!(entity instanceof EchoWarriorEntity1201)) return;
        var key = net.minecraft.core.registries.BuiltInRegistries.ENCHANTMENT.getKey(enchantment);
        if (key != null && (key.getPath().equals("undead_curse") || key.getPath().equals("total_recall")))
            ci.setReturnValue(0);
    }
}
