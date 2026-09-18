package com.yuriscat.echowarrior.compat.mixin;

import com.yuriscat.echowarrior.compat.ModContent1211;
import com.yuriscat.echowarrior.compat.client.EchoCompassPulseHud1211;
import com.yuriscat.echowarrior.compat.client.EchoCompassTooltipTitle1211;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsMixin1211 {
    @Shadow @Final private Minecraft minecraft;

    @Inject(method = "renderTooltip(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;II)V",
            at = @At("HEAD"), cancellable = true)
    private void echoWarrior$renderPulsingCompassTitle(Font font, ItemStack stack, int mouseX, int mouseY,
                                                       CallbackInfo callback) {
        if (!stack.is(ModContent1211.ECHO_COMPASS) || !EchoCompassPulseHud1211.isTooltipShakeActive()) return;
        List<Component> lines = Screen.getTooltipFromItem(this.minecraft, stack);
        if (lines.isEmpty()) return;

        List<ClientTooltipComponent> components = new ArrayList<>();
        components.add(new EchoCompassTooltipTitle1211(lines.getFirst()));
        for (int index = 1; index < lines.size(); index++) {
            components.add(ClientTooltipComponent.create(lines.get(index).getVisualOrderText()));
        }
        stack.getTooltipImage().ifPresent(image -> components.add(1, ClientTooltipComponent.create(image)));
        ((GuiGraphicsInvoker1211)this).echoWarrior$renderTooltipInternal(
                font, components, mouseX, mouseY, DefaultTooltipPositioner.INSTANCE);
        callback.cancel();
    }
}
