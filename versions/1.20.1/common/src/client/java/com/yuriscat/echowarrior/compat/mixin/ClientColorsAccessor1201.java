package com.yuriscat.echowarrior.compat.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.color.item.ItemColors;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Minecraft.class)
public interface ClientColorsAccessor1201 {
    @Accessor("itemColors") ItemColors echoWarrior$itemColors();
}
