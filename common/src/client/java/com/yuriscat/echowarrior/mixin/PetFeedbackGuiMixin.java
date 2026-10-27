package com.yuriscat.echowarrior.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.yuriscat.echowarrior.client.PetFeedbackLayout;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Gui.class)
public abstract class PetFeedbackGuiMixin {
    @WrapOperation(method = "extractOverlayMessage", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;textWithBackdrop(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIII)V"))
    private void echoWarrior$wrapPetMessage(GuiGraphicsExtractor graphics, Font font, Component message,
                                           int x, int y, int width, int color, Operation<Void> original) {
        if (PetFeedbackLayout.wraps(font, message, graphics.guiWidth()))
            PetFeedbackLayout.draw(graphics, font, message, y, color);
        else original.call(graphics, font, message, x, y, width, color);
    }
}
