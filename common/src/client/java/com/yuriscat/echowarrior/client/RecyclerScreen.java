package com.yuriscat.echowarrior.client;

import com.yuriscat.echowarrior.EchoWarrior;
import com.yuriscat.echowarrior.item.RecyclerChestItem;
import com.yuriscat.echowarrior.menu.RecyclerMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;
import java.util.Optional;

public final class RecyclerScreen extends AbstractContainerScreen<RecyclerMenu> {
	private static final Identifier BACKGROUND = EchoWarrior.id("textures/gui/recycler/recycler.png");
	private static final Identifier INFO = EchoWarrior.id("textures/gui/recycler/recycler_info.png");
	private static final Identifier INFO_HOVERED = EchoWarrior.id("textures/gui/recycler/recycler_info_hovered.png");
	private static final int INFO_X = 153;
	private static final int INFO_Y = 4;
	private static final int INFO_WIDTH = 15;
	private static final int INFO_HEIGHT = 12;
	private static final int INFO_HIT_PADDING = 1;

	public RecyclerScreen(RecyclerMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, 176, 114 + menu.getRowCount() * 18);
		this.inventoryLabelY = this.imageHeight - 94;
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractBackground(graphics, mouseX, mouseY, partialTick);
		int x = (this.width - this.imageWidth) / 2;
		int y = (this.height - this.imageHeight) / 2;
		graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, x, y,
				0.0F, 0.0F, this.imageWidth, 71, 256, 256);
		graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, x, y + 71,
				0.0F, 126.0F, this.imageWidth, 96, 256, 256);
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		super.extractLabels(graphics, mouseX, mouseY);
		boolean hovered = isInside(
				mouseX,
				mouseY,
				this.leftPos + INFO_X - INFO_HIT_PADDING,
				this.topPos + INFO_Y - INFO_HIT_PADDING,
				INFO_WIDTH + INFO_HIT_PADDING * 2,
				INFO_HEIGHT + INFO_HIT_PADDING * 2
		);
		Identifier texture = hovered ? INFO_HOVERED : INFO;
		graphics.blit(RenderPipelines.GUI_TEXTURED, texture, INFO_X, INFO_Y,
				0.0F, 0.0F, INFO_WIDTH, INFO_HEIGHT, INFO_WIDTH, INFO_HEIGHT);
		if (hovered) {
			graphics.setTooltipForNextFrame(this.font, infoTooltip(), Optional.empty(), mouseX, mouseY);
		}
	}

	private static List<Component> infoTooltip() {
		return List.of(
				Component.translatable("gui.echo_warrior.recycler.info_title")
						.withStyle(style -> style.withColor(RecyclerChestItem.NAME_COLOR)),
				Component.translatable("gui.echo_warrior.recycler.accept_header").withStyle(ChatFormatting.GRAY),
				Component.translatable("gui.echo_warrior.recycler.accept_knowledge").withStyle(ChatFormatting.GRAY),
				Component.translatable("gui.echo_warrior.recycler.accept_legacy").withStyle(ChatFormatting.GRAY),
				Component.translatable("gui.echo_warrior.recycler.accept_accessory").withStyle(ChatFormatting.GRAY),
				Component.translatable("gui.echo_warrior.recycler.accept_relic").withStyle(ChatFormatting.GRAY),
				Component.translatable("gui.echo_warrior.recycler.midnight_result").withStyle(ChatFormatting.GRAY)
		);
	}

	private static boolean isInside(double mouseX, double mouseY, int x, int y, int width, int height) {
		return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
	}
}
