package com.yuriscat.echowarrior.compat.mixin;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

@Mixin(GuiGraphics.class)
public interface GuiGraphicsInvoker1211 {
    @Invoker("renderTooltipInternal")
    void echoWarrior$renderTooltipInternal(Font font, List<ClientTooltipComponent> components,
                                           int mouseX, int mouseY, ClientTooltipPositioner positioner);
}
