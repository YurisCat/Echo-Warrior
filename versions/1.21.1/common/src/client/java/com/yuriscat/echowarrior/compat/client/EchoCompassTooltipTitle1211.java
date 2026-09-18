package com.yuriscat.echowarrior.compat.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import org.joml.Matrix4f;

/** Tooltip title renderer used while the Echo Compass is emitting a semantic pulse. */
public record EchoCompassTooltipTitle1211(Component title) implements ClientTooltipComponent {
    @Override
    public int getHeight() {
        return 10;
    }

    @Override
    public int getWidth(Font font) {
        return font.width(this.title);
    }

    @Override
    public void renderText(Font font, int x, int y, Matrix4f matrix,
                           MultiBufferSource.BufferSource buffers) {
        int color = this.title.getStyle().getColor() == null
                ? 0xFFAA00
                : this.title.getStyle().getColor().getValue();
        EchoCompassPulseHud1211.renderTooltipTitle(font, this.title, x, y, matrix, buffers, color);
    }
}
