package com.yuriscat.echowarrior.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;

public record EchoCompassTooltipTitle(Component title) implements ClientTooltipComponent {
	private static final int DEFAULT_TITLE_RGB = 0xFFAA00;

	@Override
	public int getHeight(Font font) {
		return 10;
	}

	@Override
	public int getWidth(Font font) {
		return font.width(this.title);
	}

	@Override
	public void extractText(GuiGraphicsExtractor graphics, Font font, int x, int y) {
		TextColor textColor = this.title.getStyle().getColor();
		int rgb = textColor == null ? DEFAULT_TITLE_RGB : textColor.getValue();
		EchoCompassPulseHud.renderTooltipTitle(graphics, font, this.title.getString(), x, y, rgb);
	}
}
