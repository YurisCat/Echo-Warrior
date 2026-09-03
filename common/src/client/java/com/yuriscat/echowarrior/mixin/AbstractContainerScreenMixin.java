package com.yuriscat.echowarrior.mixin;

import com.mojang.blaze3d.platform.NativeImage;
import com.yuriscat.echowarrior.EchoWarrior;
import com.yuriscat.echowarrior.client.SummonerFuelParticleHost;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin implements SummonerFuelParticleHost {
	@Unique private static final int ECHO_WARRIOR_MAX_FUEL_PARTICLES = 96;
	@Unique private static final int ECHO_WARRIOR_MAX_POLISH_SWEEPS = 12;
	@Unique private static final int ECHO_WARRIOR_SOUL_DARK = 0xFF77D6CE;
	@Unique private static final int ECHO_WARRIOR_SOUL_LIGHT = 0xFFB7F4E9;
	@Unique private static final int ECHO_WARRIOR_FLESH_DARK = 0xFF8B4A42;
	@Unique private static final int ECHO_WARRIOR_FLESH_LIGHT = 0xFFB66A55;
	@Unique private static final Identifier ECHO_WARRIOR_SUMMONER_TEXTURE =
			EchoWarrior.id("textures/item/test_echo_summoner.png");

	@Shadow protected int leftPos;
	@Shadow protected int topPos;
	@Shadow protected @Nullable Slot hoveredSlot;

	@Unique private final List<FuelParticle> echoWarrior$fuelInsertionParticles = new ArrayList<>();
	@Unique private final List<PolishSweep> echoWarrior$insertionPolishSweeps = new ArrayList<>();

	@Override
	public void echoWarrior$spawnFuelInsertionParticles(Slot slot, ItemStack fuel) {
		Slot displaySlot = this.hoveredSlot != null ? this.hoveredSlot : slot;
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
			this.echoWarrior$fuelInsertionParticles.add(new FuelParticle(
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
		Slot displaySlot = this.hoveredSlot != null ? this.hoveredSlot : slot;
		ThreadLocalRandom random = ThreadLocalRandom.current();
		this.echoWarrior$insertionPolishSweeps.add(new PolishSweep(
				displaySlot.x,
				displaySlot.y,
				22 + random.nextInt(5),
				echoWarrior$loadSummonerAlphaMask()
		));
		while (this.echoWarrior$insertionPolishSweeps.size() > ECHO_WARRIOR_MAX_POLISH_SWEEPS) {
			this.echoWarrior$insertionPolishSweeps.remove(0);
		}
	}

	@Inject(method = "extractContents", at = @At("TAIL"))
	private void echoWarrior$renderInsertionFeedback(
			GuiGraphicsExtractor graphics,
			int mouseX,
			int mouseY,
			float partialTick,
			CallbackInfo callback
	) {
		if (this.echoWarrior$fuelInsertionParticles.isEmpty()
				&& this.echoWarrior$insertionPolishSweeps.isEmpty()) {
			return;
		}

		graphics.nextStratum();
		for (FuelParticle particle : this.echoWarrior$fuelInsertionParticles) {
			double progress = Math.min(1.0, Math.max(0.0,
					(particle.age + partialTick) / particle.lifetime));
			double radialProgress;
			if (progress < 0.38) {
				double outward = progress / 0.38;
				radialProgress = 1.0 - Math.pow(1.0 - outward, 3.0);
			} else {
				double inward = (progress - 0.38) / 0.62;
				double smooth = inward * inward * (3.0 - 2.0 * inward);
				radialProgress = 1.0 - smooth;
			}

			double curvedAngle = particle.angle + particle.spin * Math.sin(progress * Math.PI);
			double radius = particle.distance * radialProgress;
			int x = (int)Math.round(this.leftPos + particle.centerX + Math.cos(curvedAngle) * radius);
			int y = (int)Math.round(this.topPos + particle.centerY + Math.sin(curvedAngle) * radius);
			double fade = progress <= 0.85 ? 1.0 : (1.0 - progress) / 0.15;
			int alpha = Math.min(255, Math.max(0, (int)Math.round(255.0 * fade)));
			int color = alpha << 24 | particle.color & 0xFFFFFF;
			graphics.fill(x, y, x + 1, y + 1, color);
		}

		for (PolishSweep sweep : this.echoWarrior$insertionPolishSweeps) {
			renderPolishSweep(graphics, sweep, partialTick);
		}
	}

	@Inject(method = "tick", at = @At("TAIL"))
	private void echoWarrior$tickInsertionFeedback(CallbackInfo callback) {
		this.echoWarrior$fuelInsertionParticles.removeIf(particle -> ++particle.age > particle.lifetime);
		this.echoWarrior$insertionPolishSweeps.removeIf(sweep -> ++sweep.age > sweep.lifetime);
	}

	@Unique
	private void renderPolishSweep(GuiGraphicsExtractor graphics, PolishSweep sweep, float partialTick) {
		double progress = echoWarrior$clamp01((sweep.age + partialTick) / sweep.lifetime);
		double eased = echoWarrior$easeOutBack(echoWarrior$smoothstep(0.0, 1.0, progress));
		double head = -5.0 + 25.0 * eased;
		double fadeIn = Math.min(1.0, (progress + 0.04) / 0.12);
		double fadeOut = 1.0 - echoWarrior$smoothstep(0.70, 1.0, progress);
		double intensity = fadeIn * fadeOut * (0.82 + Math.sin(progress * Math.PI) * 0.18);
		int slotLeft = this.leftPos + sweep.slotX;
		int slotTop = this.topPos + sweep.slotY;

		for (int pixelY = 0; pixelY < 16; pixelY++) {
			for (int pixelX = 0; pixelX < 16; pixelX++) {
				int textureAlpha = sweep.alphaMask[pixelX + pixelY * 16];
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
				int alpha = Math.clamp((int)Math.round(
						238.0 * intensity * bandStrength * textureAlpha / 255.0), 0, 255);
				if (alpha <= 0) continue;
				int color = alpha << 24 | (distance <= 2.0 ? 0xFFFFFF : 0xEAFBFF);
				graphics.fill(
						slotLeft + pixelX,
						slotTop + pixelY,
						slotLeft + pixelX + 1,
						slotTop + pixelY + 1,
						color
				);
			}
		}
	}

	@Unique
	private static int[] echoWarrior$loadSummonerAlphaMask() {
		int[] alphaMask = new int[16 * 16];
		var resource = Minecraft.getInstance().getResourceManager().getResource(ECHO_WARRIOR_SUMMONER_TEXTURE);
		if (resource.isEmpty()) {
			return alphaMask;
		}
		try (InputStream stream = resource.get().open(); NativeImage image = NativeImage.read(stream)) {
			int width = Math.max(1, image.getWidth());
			int height = Math.max(1, image.getHeight());
			for (int pixelY = 0; pixelY < 16; pixelY++) {
				for (int pixelX = 0; pixelX < 16; pixelX++) {
					int sourceX = Math.min(width - 1, pixelX * width / 16);
					int sourceY = Math.min(height - 1, pixelY * height / 16);
					alphaMask[pixelX + pixelY * 16] = image.getPixel(sourceX, sourceY) >>> 24 & 0xFF;
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

	@Unique
	private static final class FuelParticle {
		private final double centerX;
		private final double centerY;
		private final double angle;
		private final double distance;
		private final double spin;
		private final int lifetime;
		private final int color;
		private int age;

		private FuelParticle(
				double centerX,
				double centerY,
				double angle,
				double distance,
				double spin,
				int lifetime,
				int color
		) {
			this.centerX = centerX;
			this.centerY = centerY;
			this.angle = angle;
			this.distance = distance;
			this.spin = spin;
			this.lifetime = lifetime;
			this.color = color;
		}
	}

	@Unique
	private static final class PolishSweep {
		private final int slotX;
		private final int slotY;
		private final int lifetime;
		private final int[] alphaMask;
		private int age;

		private PolishSweep(int slotX, int slotY, int lifetime, int[] alphaMask) {
			this.slotX = slotX;
			this.slotY = slotY;
			this.lifetime = lifetime;
			this.alphaMask = alphaMask;
		}
	}
}
