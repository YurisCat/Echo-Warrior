package com.yuriscat.echowarrior.mixin;

import com.yuriscat.echowarrior.ModItems;
import com.yuriscat.echowarrior.client.EchoCompassPulseHud;
import com.yuriscat.echowarrior.client.EchoCompassTooltipTitle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Mixin(GuiGraphicsExtractor.class)
public abstract class GuiGraphicsExtractorMixin {
	@Shadow @Final private Minecraft minecraft;
	@Shadow private @Nullable Runnable deferredTooltip;

	@Shadow
	public abstract void tooltip(
			Font font,
			List<ClientTooltipComponent> lines,
			int xo,
			int yo,
			ClientTooltipPositioner positioner,
			@Nullable Identifier style
	);

	@Inject(
			method = "setTooltipForNextFrame(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;II)V",
			at = @At("HEAD"),
			cancellable = true
	)
	private void echoWarrior$shakeEchoCompassTooltipTitle(
			Font font,
			ItemStack stack,
			int x,
			int y,
			CallbackInfo callback
	) {
		if (!stack.is(ModItems.ECHO_COMPASS) || !EchoCompassPulseHud.isTooltipShakeActive()) return;

		List<Component> textLines = Screen.getTooltipFromItem(this.minecraft, stack);
		List<ClientTooltipComponent> components = new ArrayList<>(textLines.size() + 1);
		for (int index = 0; index < textLines.size(); index++) {
			Component line = textLines.get(index);
			components.add(index == 0
					? new EchoCompassTooltipTitle(line)
					: ClientTooltipComponent.create(line.getVisualOrderText()));
		}

		Optional<TooltipComponent> optionalImage = stack.getTooltipImage();
		optionalImage.ifPresent(image -> components.add(
				components.isEmpty() ? 0 : 1,
				ClientTooltipComponent.create(image)));
		if (!components.isEmpty() && this.deferredTooltip == null) {
			Identifier style = stack.get(DataComponents.TOOLTIP_STYLE);
			this.deferredTooltip = () -> this.tooltip(
					font, components, x, y, DefaultTooltipPositioner.INSTANCE, style);
		}
		callback.cancel();
	}
}
