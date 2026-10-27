package com.yuriscat.echowarrior.compat.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.yuriscat.echowarrior.compat.client.PetFeedbackLayout1201;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Gui.class)
public abstract class PetFeedbackGuiMixin1201 {
    @Shadow private Component overlayMessageString;

    // 1.20.1 draws the action-bar backdrop separately; ordinal 0 excludes title/subtitle backdrops.
    @WrapOperation(method = "render", at = @At(value = "INVOKE", ordinal = 0,
            target = "Lnet/minecraft/client/gui/Gui;drawBackdrop(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/gui/Font;III)V"))
    private void echoWarrior$wrapPetBackdrop(Gui gui, GuiGraphics graphics, Font font,
                                             int y, int width, int color, Operation<Void> original) {
        if (!PetFeedbackLayout1201.wraps(font, overlayMessageString, graphics.guiWidth()))
            original.call(gui, graphics, font, y, width, color);
    }

    @WrapOperation(method = "render", at = @At(value = "INVOKE", ordinal = 0,
            target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)I"))
    private int echoWarrior$wrapPetMessage(GuiGraphics graphics, Font font, Component message,
                                         int x, int y, int color, Operation<Integer> original) {
        if (!PetFeedbackLayout1201.wraps(font, message, graphics.guiWidth()))
            return original.call(graphics, font, message, x, y, color);
        PetFeedbackLayout1201.draw(graphics, font, message, y, color);
        return 0; // Vanilla discards this drawString result.
    }
}
