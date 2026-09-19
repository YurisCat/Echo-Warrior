package com.yuriscat.echowarrior.compat.client;

import com.yuriscat.echowarrior.compat.ModContent1211;
import com.yuriscat.echowarrior.compat.item.RecyclerChestItem1211;
import com.yuriscat.echowarrior.compat.menu.RecyclerMenu1211;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

public final class RecyclerScreen1211 extends AbstractContainerScreen<RecyclerMenu1211> {
    private static final ResourceLocation BACKGROUND = ModContent1211.id("textures/gui/recycler/recycler.png");
    private static final ResourceLocation INFO = ModContent1211.id("textures/gui/recycler/recycler_info.png");
    private static final ResourceLocation INFO_HOVERED = ModContent1211.id("textures/gui/recycler/recycler_info_hovered.png");
    private static final int INFO_X = 153;
    private static final int INFO_Y = 4;
    private static final int INFO_WIDTH = 15;
    private static final int INFO_HEIGHT = 12;
    private static final int INFO_HIT_PADDING = 1;
    private static final int TITLE_X = 8;
    private static final int TITLE_Y = 6;
    private static final int TITLE_ICON_GAP = 5;

    public RecyclerScreen1211(RecyclerMenu1211 menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageHeight = 114 + menu.getRowCount() * 18;
        this.inventoryLabelY = this.imageHeight - 94;
        this.titleLabelX = -1000;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        graphics.blit(BACKGROUND, x, y, 0.0F, 0.0F, this.imageWidth, 71, 256, 256);
        graphics.blit(BACKGROUND, x, y + 71, 0.0F, 126.0F, this.imageWidth, 96, 256, 256);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        drawFittedTitle(graphics);
        boolean hovered = isInside(mouseX, mouseY,
                this.leftPos + INFO_X - INFO_HIT_PADDING, this.topPos + INFO_Y - INFO_HIT_PADDING,
                INFO_WIDTH + INFO_HIT_PADDING * 2, INFO_HEIGHT + INFO_HIT_PADDING * 2);
        graphics.blit(hovered ? INFO_HOVERED : INFO, INFO_X, INFO_Y,
                0.0F, 0.0F, INFO_WIDTH, INFO_HEIGHT, INFO_WIDTH, INFO_HEIGHT);
    }

    private void drawFittedTitle(GuiGraphics graphics) {
        FormattedCharSequence sequence = this.title.getVisualOrderText();
        int width = this.font.width(sequence);
        int maximumWidth = INFO_X - INFO_HIT_PADDING - TITLE_ICON_GAP - TITLE_X;
        if (width <= maximumWidth) {
            graphics.drawString(this.font, sequence, TITLE_X, TITLE_Y, 0x404040, false);
            return;
        }
        float scale = maximumWidth / (float) width;
        graphics.pose().pushPose();
        graphics.pose().translate(TITLE_X, TITLE_Y + (9.0F - 9.0F * scale) / 2.0F, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawString(this.font, sequence, 0, 0, 0x404040, false);
        graphics.pose().popPose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        if (isInside(mouseX, mouseY,
                this.leftPos + INFO_X - INFO_HIT_PADDING, this.topPos + INFO_Y - INFO_HIT_PADDING,
                INFO_WIDTH + INFO_HIT_PADDING * 2, INFO_HEIGHT + INFO_HIT_PADDING * 2)) {
            graphics.renderComponentTooltip(this.font, infoTooltip(), mouseX, mouseY);
        }
    }

    private static List<Component> infoTooltip() {
        return List.of(
                Component.translatable("gui.echo_warrior.recycler.info_title")
                        .withStyle(style -> style.withColor(RecyclerChestItem1211.NAME_COLOR)),
                Component.translatable("gui.echo_warrior.recycler.accept_header").withStyle(ChatFormatting.GRAY),
                Component.translatable("gui.echo_warrior.recycler.accept_knowledge").withStyle(ChatFormatting.GRAY),
                Component.translatable("gui.echo_warrior.recycler.accept_legacy").withStyle(ChatFormatting.GRAY),
                Component.translatable("gui.echo_warrior.recycler.accept_accessory").withStyle(ChatFormatting.GRAY),
                Component.translatable("gui.echo_warrior.recycler.accept_relic").withStyle(ChatFormatting.GRAY),
                Component.translatable("gui.echo_warrior.recycler.midnight_result").withStyle(ChatFormatting.GRAY));
    }

    private static boolean isInside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
}
