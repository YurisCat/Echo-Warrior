package com.yuriscat.echowarrior.compat.mixin;

import com.mojang.blaze3d.platform.NativeImage;
import com.yuriscat.echowarrior.compat.EchoWarrior1201;
import com.yuriscat.echowarrior.compat.client.SummonerFuelParticleHost1201;
import com.yuriscat.echowarrior.compat.client.SummonerInsertionParticle1201;
import com.yuriscat.echowarrior.compat.client.SummonerInsertionPolish1201;
import com.yuriscat.echowarrior.compat.item.SummonerFuelInsertFeedback1201;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin1201 implements SummonerFuelParticleHost1201 {
    @Unique private static final int ECHO_WARRIOR_MAX_FUEL_PARTICLES = 96;
    @Unique private static final int ECHO_WARRIOR_MAX_POLISH_SWEEPS = 12;
    @Unique private static final int ECHO_WARRIOR_SOUL_DARK = 0xFF77D6CE;
    @Unique private static final int ECHO_WARRIOR_SOUL_LIGHT = 0xFFB7F4E9;
    @Unique private static final int ECHO_WARRIOR_FLESH_DARK = 0xFF8B4A42;
    @Unique private static final int ECHO_WARRIOR_FLESH_LIGHT = 0xFFB66A55;
    @Unique private static final ResourceLocation ECHO_WARRIOR_SUMMONER_TEXTURE =
            new ResourceLocation(EchoWarrior1201.MOD_ID, "textures/item/test_echo_summoner.png");

    @Shadow protected int leftPos;
    @Shadow protected int topPos;
    @Shadow protected Slot hoveredSlot;

    @Unique private final List<SummonerInsertionParticle1201> echoWarrior$fuelInsertionParticles = new ArrayList<>();
    @Unique private final List<SummonerInsertionPolish1201> echoWarrior$insertionPolishSweeps = new ArrayList<>();

    @Override
    public void echoWarrior$spawnFuelInsertionParticles(Slot slot, ItemStack fuel) {
        if (com.yuriscat.echowarrior.compat.client.AutomatedTestController1201.ENABLED)
            com.yuriscat.echowarrior.compat.client.InsertionRenderAudit1201.fuelBursts++;
        Slot displaySlot = slot;
        ThreadLocalRandom random = ThreadLocalRandom.current();
        boolean soulFuel = fuel.is(Items.SOUL_SAND) || fuel.is(Items.SOUL_SOIL);
        int count = 9 + random.nextInt(6);
        for (int index = 0; index < count; index++) {
            double angle = random.nextDouble(Math.PI * 2.0);
            double cosine = Math.cos(angle);
            double sine = Math.sin(angle);
            double slotEdgeDistance = 8.5 / Math.max(Math.abs(cosine), Math.abs(sine));
            double distance = slotEdgeDistance + 3.0 + random.nextDouble(18.0);
            int color = soulFuel
                    ? (random.nextBoolean() ? ECHO_WARRIOR_SOUL_DARK : ECHO_WARRIOR_SOUL_LIGHT)
                    : (random.nextBoolean() ? ECHO_WARRIOR_FLESH_DARK : ECHO_WARRIOR_FLESH_LIGHT);
            this.echoWarrior$fuelInsertionParticles.add(SummonerInsertionParticle1201.fuel(
                    displaySlot.x + 8.0,
                    displaySlot.y + 8.0,
                    angle,
                    distance,
                    random.nextDouble(-0.45, 0.45),
                    14 + random.nextInt(7),
                    color
            ));
        }
        while (this.echoWarrior$fuelInsertionParticles.size() > ECHO_WARRIOR_MAX_FUEL_PARTICLES) {
            this.echoWarrior$fuelInsertionParticles.remove(0);
        }
    }

    @Override
    public void echoWarrior$spawnInsertionPolish(Slot slot) {
        Slot displaySlot = slot;
        ThreadLocalRandom random = ThreadLocalRandom.current();
        this.echoWarrior$insertionPolishSweeps.add(new SummonerInsertionPolish1201(
                displaySlot.x,
                displaySlot.y,
                22 + random.nextInt(5),
                echoWarrior$loadSummonerAlphaMask()
        ));
        while (this.echoWarrior$insertionPolishSweeps.size() > ECHO_WARRIOR_MAX_POLISH_SWEEPS) {
            this.echoWarrior$insertionPolishSweeps.remove(0);
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void echoWarrior$renderInsertionFeedback(
            GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo callback) {
        int drained = SummonerFuelInsertFeedback1201.drain((slot, feedback) -> {
            // A delayed ACK must not follow the mouse or leak into a different screen.
            if (!((AbstractContainerScreen<?>)(Object)this).getMenu().slots.contains(slot)) return;
            if (feedback.effect() == SummonerFuelInsertFeedback1201.Effect.FUEL) {
                this.echoWarrior$spawnFuelInsertionParticles(slot, feedback.item());
            } else {
                this.echoWarrior$spawnInsertionPolish(slot);
            }
        });
        if (drained > 0 && Boolean.getBoolean("echo_warrior.summoner_insertion_diagnostics")) {
            EchoWarrior1201.LOGGER.info(
                    "[SummonerInsertionFeedback] screen={} drained={} fuelParticles={} polishSweeps={}",
                    this.getClass().getName(), drained, this.echoWarrior$fuelInsertionParticles.size(),
                    this.echoWarrior$insertionPolishSweeps.size());
        }
        if (this.echoWarrior$fuelInsertionParticles.isEmpty()
                && this.echoWarrior$insertionPolishSweeps.isEmpty()) return;

        // Minecraft 26 uses nextStratum() for this overlay. In 1.21.1, explicitly
        // close the container/item batch first and submit feedback through the
        // no-depth GUI overlay type; otherwise the vertices can be accepted here
        // but later disappear behind the creative inventory's shared GUI batch.
        graphics.flush();
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 250.0F);
        try {
            for (SummonerInsertionParticle1201 particle : this.echoWarrior$fuelInsertionParticles) {
                double progress = particle.progress(partialTick);
                double radialProgress;
                if (progress < 0.38) {
                    double outward = progress / 0.38;
                    radialProgress = 1.0 - Math.pow(1.0 - outward, 3.0);
                } else {
                    double inward = (progress - 0.38) / 0.62;
                    double smooth = inward * inward * (3.0 - 2.0 * inward);
                    radialProgress = 1.0 - smooth;
                }

                double curvedAngle = particle.angle() + particle.spin() * Math.sin(progress * Math.PI);
                double radius = particle.distance() * radialProgress;
                int x = (int)Math.round(this.leftPos + particle.centerX() + Math.cos(curvedAngle) * radius);
                int y = (int)Math.round(this.topPos + particle.centerY() + Math.sin(curvedAngle) * radius);
                double fade = progress <= 0.85 ? 1.0 : (1.0 - progress) / 0.15;
                int alpha = Math.min(255, Math.max(0, (int)Math.round(255.0 * fade)));
                graphics.fill(
                        RenderType.guiOverlay(), x, y, x + 1, y + 1,
                        alpha << 24 | particle.color() & 0xFFFFFF);
            }

            for (SummonerInsertionPolish1201 sweep : this.echoWarrior$insertionPolishSweeps) {
                echoWarrior$renderPolishSweep(graphics, sweep, partialTick);
            }
            graphics.flush();
        } finally {
            graphics.pose().popPose();
        }
    }

    // InventoryScreen overrides containerTick without super; final tick is shared by every container.
    @Inject(method = "tick", at = @At("TAIL"))
    private void echoWarrior$tickInsertionFeedback(CallbackInfo callback) {
        this.echoWarrior$fuelInsertionParticles.removeIf(SummonerInsertionParticle1201::tickAndExpired);
        this.echoWarrior$insertionPolishSweeps.removeIf(SummonerInsertionPolish1201::tickAndExpired);
    }

    @Unique
    private void echoWarrior$renderPolishSweep(
            GuiGraphics graphics, SummonerInsertionPolish1201 sweep, float partialTick) {
        double progress = sweep.progress(partialTick);
        double eased = echoWarrior$easeOutBack(echoWarrior$smoothstep(0.0, 1.0, progress));
        double head = -5.0 + 25.0 * eased;
        double fadeIn = Math.min(1.0, (progress + 0.04) / 0.12);
        double fadeOut = 1.0 - echoWarrior$smoothstep(0.70, 1.0, progress);
        double intensity = fadeIn * fadeOut * (0.82 + Math.sin(progress * Math.PI) * 0.18);
        int slotLeft = this.leftPos + sweep.slotX();
        int slotTop = this.topPos + sweep.slotY();
        int renderedPixels = 0;

        for (int pixelY = 0; pixelY < 16; pixelY++) {
            for (int pixelX = 0; pixelX < 16; pixelX++) {
                int textureAlpha = sweep.alphaMask()[pixelX + pixelY * 16];
                if (textureAlpha <= 0) continue;
                double diagonal = (pixelX + pixelY) * 0.5;
                double distance = Math.abs(diagonal - head);
                double bandStrength;
                if (distance <= 2.5) {
                    bandStrength = 1.0 - distance / 2.5 * 0.24;
                } else if (distance <= 4.0) {
                    bandStrength = (1.0 - (distance - 2.5) / 1.5) * 0.52;
                } else {
                    continue;
                }
                int alpha = Math.max(0, Math.min(255, (int)Math.round(
                        238.0 * intensity * bandStrength * textureAlpha / 255.0)));
                if (alpha <= 0) continue;
                int color = alpha << 24 | (distance <= 2.0 ? 0xFFFFFF : 0xEAFBFF);
                renderedPixels++;
                graphics.fill(
                        RenderType.guiOverlay(),
                        slotLeft + pixelX,
                        slotTop + pixelY,
                        slotLeft + pixelX + 1,
                        slotTop + pixelY + 1,
                        color
                );
            }
        }
        if (renderedPixels > 0 && com.yuriscat.echowarrior.compat.client.AutomatedTestController1201.ENABLED)
            com.yuriscat.echowarrior.compat.client.InsertionRenderAudit1201.visiblePolishFrames++;
        if (renderedPixels > 0
                && Boolean.getBoolean("echo_warrior.summoner_insertion_diagnostics")
                && sweep.markFirstVisibleRenderForDiagnostics()) {
            EchoWarrior1201.LOGGER.info(
                    "[SummonerInsertionFeedback] stage=render effect=POLISH screen={} slotLeft={} slotTop={} pixels={} progress={} layer=gui_overlay flushed=true",
                    this.getClass().getName(), slotLeft, slotTop, renderedPixels, progress);
        }
    }

    @Unique
    private static int[] echoWarrior$loadSummonerAlphaMask() {
        int[] alphaMask = new int[16 * 16];
        var resource = Minecraft.getInstance().getResourceManager().getResource(ECHO_WARRIOR_SUMMONER_TEXTURE);
        if (resource.isEmpty()) return alphaMask;
        try (InputStream stream = resource.get().open(); NativeImage image = NativeImage.read(stream)) {
            int width = Math.max(1, image.getWidth());
            int height = Math.max(1, image.getHeight());
            for (int pixelY = 0; pixelY < 16; pixelY++) {
                for (int pixelX = 0; pixelX < 16; pixelX++) {
                    int sourceX = Math.min(width - 1, pixelX * width / 16);
                    int sourceY = Math.min(height - 1, pixelY * height / 16);
                    alphaMask[pixelX + pixelY * 16] = image.getPixelRGBA(sourceX, sourceY) >>> 24 & 0xFF;
                }
            }
        } catch (IOException ignored) {
            // Skip the sweep rather than flashing transparent pixels when a texture cannot be decoded.
        }
        return alphaMask;
    }

    @Unique
    private static double echoWarrior$easeOutBack(double progress) {
        double shifted = progress - 1.0;
        double strength = 0.85;
        return 1.0 + (strength + 1.0) * shifted * shifted * shifted
                + strength * shifted * shifted;
    }

    @Unique
    private static double echoWarrior$smoothstep(double edge0, double edge1, double value) {
        double progress = echoWarrior$clamp01((value - edge0) / (edge1 - edge0));
        return progress * progress * (3.0 - 2.0 * progress);
    }

    @Unique
    private static double echoWarrior$clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

}
