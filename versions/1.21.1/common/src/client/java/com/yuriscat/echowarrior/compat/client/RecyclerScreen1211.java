package com.yuriscat.echowarrior.compat.client;

import com.yuriscat.echowarrior.compat.ModContent1211;
import com.yuriscat.echowarrior.compat.item.RecyclerChestItem1211;
import com.yuriscat.echowarrior.compat.menu.RecyclerMenu1211;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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

    public RecyclerScreen1211(RecyclerMenu1211 menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageHeight = 114 + menu.getRowCount() * 18;
        this.inventoryLabelY = this.imageHeight - 94;
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
        boolean hovered = isInside(mouseX, mouseY, this.leftPos + INFO_X - 1, this.topPos + INFO_Y - 1,
                INFO_WIDTH + 2, INFO_HEIGHT + 2);
        graphics.blit(hovered ? INFO_HOVERED : INFO, INFO_X, INFO_Y,
                0.0F, 0.0F, INFO_WIDTH, INFO_HEIGHT, INFO_WIDTH, INFO_HEIGHT);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        if (isInside(mouseX, mouseY, this.leftPos + INFO_X - 1, this.topPos + INFO_Y - 1,
                INFO_WIDTH + 2, INFO_HEIGHT + 2)) {
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
