package com.yuriscat.echowarrior.compat.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.yuriscat.echowarrior.compat.client.PetFeedbackLayout1211;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Gui.class)
public abstract class PetFeedbackGuiMixin1211 {
    @WrapOperation(method = "renderOverlayMessage", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;drawStringWithBackdrop(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIII)I"))
    private int echoWarrior$wrapPetMessage(GuiGraphics graphics, Font font, Component message,
                                         int x, int y, int width, int color, Operation<Integer> original) {
        if (!PetFeedbackLayout1211.wraps(font, message, graphics.guiWidth()))
            return original.call(graphics, font, message, x, y, width, color);
        PetFeedbackLayout1211.draw(graphics, font, message, y, color);
        return 0; // Vanilla discards the returned end position.
    }
}
