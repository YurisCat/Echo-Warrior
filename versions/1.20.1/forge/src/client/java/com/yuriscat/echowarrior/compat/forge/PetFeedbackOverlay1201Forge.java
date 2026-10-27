package com.yuriscat.echowarrior.compat.forge;

import com.mojang.blaze3d.systems.RenderSystem;
import com.yuriscat.echowarrior.compat.client.PetFeedbackLayout1201;
import com.yuriscat.echowarrior.compat.client.PetFeedbackOverlayAccess1201;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;

/** ForgeGui does not call Gui.render; preserve its health-bar offset and vanilla message state. */
public final class PetFeedbackOverlay1201Forge {
    private PetFeedbackOverlay1201Forge() {}

    public static void render(RenderGuiOverlayEvent.Pre event) {
        if (!event.getOverlay().id().equals(VanillaGuiOverlay.RECORD_OVERLAY.id())) return;
        var client = Minecraft.getInstance();
        if (client.player == null || client.options.hideGui) return;
        var graphics = event.getGuiGraphics();
        var state = (PetFeedbackOverlayAccess1201) client.gui;
        var message = state.echoWarrior$petOverlayMessage();
        if (!PetFeedbackLayout1201.wraps(client.font, message, graphics.guiWidth())) return;
        event.setCanceled(true);
        float remaining = state.echoWarrior$petOverlayTime() - event.getPartialTick();
        int alpha = Math.min(255, (int) (remaining * 255.0F / 20.0F));
        if (alpha <= 8) return;
        int rgb = state.echoWarrior$petOverlayAnimated()
                ? Mth.hsvToRgb(remaining / 50.0F, 0.7F, 0.6F) & 0xFFFFFF : 0xFFFFFF;
        var forgeGui = (ForgeGui) client.gui;
        int offset = Math.max(68, Math.max(forgeGui.leftHeight, forgeGui.rightHeight) + 9);
        graphics.pose().pushPose();
        graphics.pose().translate(graphics.guiWidth() / 2.0, graphics.guiHeight() - offset, 0.0);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        PetFeedbackLayout1201.draw(graphics, client.font, message, -4, rgb | alpha << 24);
        RenderSystem.disableBlend();
        graphics.pose().popPose();
    }
}
