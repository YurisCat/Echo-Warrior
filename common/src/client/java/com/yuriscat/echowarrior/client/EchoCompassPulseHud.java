package com.yuriscat.echowarrior.client;

import com.yuriscat.echowarrior.ModItems;
import com.yuriscat.echowarrior.network.EchoCompassMessagePayload;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.locale.Language;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class EchoCompassPulseHud {
	private static final long TOOLTIP_SHAKE_DURATION_NANOS = 650_000_000L;
	private static final long MESSAGE_DURATION_NANOS = 2_400_000_000L;
	private static final long MESSAGE_SHAKE_DURATION_NANOS = 650_000_000L;
	private static final long MESSAGE_FADE_DURATION_NANOS = 450_000_000L;
	private static final double MIN_SHAKE_UPDATES_PER_SECOND = 5.0;
	private static final double MAX_SHAKE_UPDATES_PER_SECOND = 28.0;
	private static final double MESSAGE_SHAKE_UPDATES_PER_SECOND = 22.0;
	private static final int MESSAGE_RGB = 0xFFFFFF;
	private static final int DIRECTIONAL_PULSE_RGB = 0xFFAA00;
	private static final String DIRECTIONAL_PULSE_TRANSLATION_KEY =
			"hud.echo_warrior.echo_compass.directional_pulse";

	private static long tooltipStartedAtNanos;
	private static long tooltipSeed;
	private static float tooltipCloseness;
	private static boolean directionalPulse;
	private static EchoCompassMessagePayload message;
	private static long messageStartedAtNanos;
	private static long messageSeed;

	private EchoCompassPulseHud() {
	}

	public static void pulse(float pulseCloseness, boolean directional) {
		tooltipCloseness = Math.clamp(pulseCloseness, 0.0F, 1.0F);
		directionalPulse = directional;
		tooltipStartedAtNanos = System.nanoTime();
		tooltipSeed++;
	}

	public static void showMessage(EchoCompassMessagePayload payload) {
		message = payload;
		messageStartedAtNanos = System.nanoTime();
		messageSeed++;
	}

	public static boolean isTooltipShakeActive() {
		long elapsedNanos = System.nanoTime() - tooltipStartedAtNanos;
		return tooltipStartedAtNanos != 0L
				&& elapsedNanos >= 0L
				&& elapsedNanos < TOOLTIP_SHAKE_DURATION_NANOS;
	}

	public static void renderTooltipTitle(
			GuiGraphicsExtractor graphics,
			Font font,
			String title,
			int x,
			int y,
			int rgb
	) {
		long elapsedNanos = System.nanoTime() - tooltipStartedAtNanos;
		if (tooltipStartedAtNanos == 0L || elapsedNanos < 0L || elapsedNanos >= TOOLTIP_SHAKE_DURATION_NANOS) {
			graphics.text(font, title, x, y, 0xFF000000 | rgb, true);
			return;
		}

		double updatesPerSecond = MIN_SHAKE_UPDATES_PER_SECOND
				+ (MAX_SHAKE_UPDATES_PER_SECOND - MIN_SHAKE_UPDATES_PER_SECOND) * tooltipCloseness;
		renderGraphemes(graphics, font, title, x, y, 0xFF000000 | rgb, tooltipSeed,
				shakeStep(elapsedNanos, updatesPerSecond), true);
	}

	public static void clear() {
		tooltipStartedAtNanos = 0L;
		tooltipCloseness = 0.0F;
		directionalPulse = false;
		message = null;
		messageStartedAtNanos = 0L;
	}

	public static void renderMessage(GuiGraphicsExtractor graphics) {
		Minecraft client = Minecraft.getInstance();
		if (message == null || messageStartedAtNanos == 0L || client.player == null || client.options.hideGui) return;

		long elapsedNanos = System.nanoTime() - messageStartedAtNanos;
		if (elapsedNanos < 0L || elapsedNanos >= MESSAGE_DURATION_NANOS) {
			message = null;
			messageStartedAtNanos = 0L;
			return;
		}

		int alpha = messageAlpha(elapsedNanos);
		if (alpha <= 0) return;
		String translated = message.component().getString();
		List<String> graphemes = graphemes(translated, client.options.languageCode);
		if (graphemes.isEmpty()) return;
		if (Language.getInstance().isDefaultRightToLeft()) Collections.reverse(graphemes);

		Font font = client.font;
		int totalWidth = 0;
		for (String grapheme : graphemes) totalWidth += font.width(grapheme);
		int x = (graphics.guiWidth() - totalWidth) / 2;
		int y = Math.max(20, graphics.guiHeight() - 68);
		long step = shakeStep(elapsedNanos, MESSAGE_SHAKE_UPDATES_PER_SECOND);
		boolean shaking = elapsedNanos < MESSAGE_SHAKE_DURATION_NANOS;
		renderGraphemes(graphics, font, graphemes, x, y, alpha << 24 | MESSAGE_RGB,
				messageSeed, step, shaking);
	}

	public static void renderDirectionalPulse(GuiGraphicsExtractor graphics) {
		Minecraft client = Minecraft.getInstance();
		if (tooltipStartedAtNanos == 0L || client.player == null || client.options.hideGui) return;
		if (!client.player.getMainHandItem().is(ModItems.ECHO_COMPASS)
				&& !client.player.getOffhandItem().is(ModItems.ECHO_COMPASS)) return;
		if (!EchoCompassClientState.hasDirectionalPulseTarget()) return;

		long elapsedNanos = System.nanoTime() - tooltipStartedAtNanos;
		if (elapsedNanos < 0L || elapsedNanos >= TOOLTIP_SHAKE_DURATION_NANOS) return;
		double distanceResponse = smoothstep(tooltipCloseness);
		double distanceAlpha = directionalPulse
				? 0.50 + distanceResponse * 0.50
				: 0.10 + distanceResponse * 0.40;
		int alpha = Math.clamp((int)Math.round(255.0 * distanceAlpha * pulseAlpha(
				elapsedNanos / (double)TOOLTIP_SHAKE_DURATION_NANOS)), 0, 255);
		if (alpha <= 0) return;

		String translated = net.minecraft.network.chat.Component
				.translatable(DIRECTIONAL_PULSE_TRANSLATION_KEY).getString();
		List<String> graphemes = graphemes(translated, client.options.languageCode);
		if (graphemes.isEmpty()) return;
		if (Language.getInstance().isDefaultRightToLeft()) Collections.reverse(graphemes);

		Font font = client.font;
		int totalWidth = 0;
		for (String grapheme : graphemes) totalWidth += font.width(grapheme);
		int centerX = directionalCenterX(graphics, client.gameRenderer.getMainCamera());
		int x = Math.clamp(centerX - totalWidth / 2, 8, Math.max(8, graphics.guiWidth() - totalWidth - 8));
		int y = directionalPulseY(graphics, client.gameRenderer.getMainCamera(), tooltipSeed);
		double updatesPerSecond = MIN_SHAKE_UPDATES_PER_SECOND
				+ (MAX_SHAKE_UPDATES_PER_SECOND - MIN_SHAKE_UPDATES_PER_SECOND) * distanceResponse;
		renderGraphemes(graphics, font, graphemes, x, y, alpha << 24 | DIRECTIONAL_PULSE_RGB,
				tooltipSeed, shakeStep(elapsedNanos, updatesPerSecond), true);
	}

	private static int directionalCenterX(GuiGraphicsExtractor graphics, Camera camera) {
		BlockPos targetPos = EchoCompassClientState.directionalPulseTarget();
		Vec3 target = targetPos.getCenter();
		Vec3 cameraPos = camera.position();
		double deltaX = target.x - cameraPos.x;
		double deltaZ = target.z - cameraPos.z;
		double relativeYaw = 0.0;
		if (deltaX * deltaX + deltaZ * deltaZ > 1.0E-6) {
			double targetYaw = Math.toDegrees(Math.atan2(-deltaX, deltaZ));
			relativeYaw = Mth.wrapDegrees(targetYaw - camera.yRot());
		}
		double normalized = Math.clamp(relativeYaw / 90.0, -1.0, 1.0);
		int horizontalRange = Math.max(40, (int)Math.round(graphics.guiWidth() * 0.32));
		return graphics.guiWidth() / 2 + (int)Math.round(normalized * horizontalRange);
	}

	private static int directionalPulseY(GuiGraphicsExtractor graphics, Camera camera, long seed) {
		int guiHeight = graphics.guiHeight();
		int minY = Math.max(20, (int)Math.round(guiHeight * 0.18));
		int maxY = Math.max(minY, (int)Math.round(guiHeight * 0.64));
		Vec3 target = EchoCompassClientState.directionalPulseTarget().getCenter();
		Vec3 cameraPos = camera.position();
		double deltaX = target.x - cameraPos.x;
		double deltaY = target.y - cameraPos.y;
		double deltaZ = target.z - cameraPos.z;
		double horizontalDistance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
		double targetPitch = -Math.toDegrees(Math.atan2(deltaY, Math.max(horizontalDistance, 1.0E-6)));
		double relativePitch = Mth.wrapDegrees(targetPitch - camera.xRot());
		double normalizedPitch = Math.clamp(relativePitch / 55.0, -1.0, 1.0);

		double middleY = (minY + maxY) * 0.5;
		double logicalOffset = normalizedPitch * (maxY - minY) * 0.38;
		double randomUnit = Math.floorMod(mix(seed ^ 0xD1B54A32D192ED03L), 2001L) / 1000.0 - 1.0;
		double randomOffset = randomUnit * (maxY - minY) * 0.07;
		return Math.clamp((int)Math.round(middleY + logicalOffset + randomOffset), minY, maxY);
	}

	private static void renderGraphemes(
			GuiGraphicsExtractor graphics,
			Font font,
			String text,
			int x,
			int y,
			int color,
			long seed,
			long shakeStep,
			boolean shaking
	) {
		Minecraft client = Minecraft.getInstance();
		List<String> graphemes = graphemes(text, client.options.languageCode);
		if (Language.getInstance().isDefaultRightToLeft()) Collections.reverse(graphemes);
		renderGraphemes(graphics, font, graphemes, x, y, color, seed, shakeStep, shaking);
	}

	private static void renderGraphemes(
			GuiGraphicsExtractor graphics,
			Font font,
			List<String> graphemes,
			int x,
			int y,
			int color,
			long seed,
			long shakeStep,
			boolean shaking
	) {
		for (int index = 0; index < graphemes.size(); index++) {
			String grapheme = graphemes.get(index);
			int width = font.width(grapheme);
			if (!grapheme.isBlank()) {
				int offsetX = shaking ? jitter(seed, index, shakeStep, 0) : 0;
				int offsetY = shaking ? jitter(seed, index, shakeStep, 1) : 0;
				graphics.text(font, grapheme, x + offsetX, y + offsetY, color, true);
			}
			x += width;
		}
	}

	private static List<String> graphemes(String text, String languageCode) {
		Locale locale = Locale.forLanguageTag(languageCode.replace('_', '-'));
		BreakIterator iterator = BreakIterator.getCharacterInstance(locale);
		iterator.setText(text);
		List<String> result = new ArrayList<>();
		int start = iterator.first();
		for (int end = iterator.next(); end != BreakIterator.DONE; start = end, end = iterator.next()) {
			result.add(text.substring(start, end));
		}
		return result;
	}

	private static long shakeStep(long elapsedNanos, double updatesPerSecond) {
		return (long)Math.floor(elapsedNanos / 1_000_000_000.0 * updatesPerSecond);
	}

	private static int jitter(long seed, int graphemeIndex, long shakeStep, int axis) {
		long value = seed * 0x9E3779B97F4A7C15L
				^ (long)graphemeIndex * 0xC2B2AE3D27D4EB4FL
				^ shakeStep * 0x165667B19E3779F9L
				^ (long)axis * 0x85EBCA77C2B2AE63L;
		value ^= value >>> 30;
		value *= 0xBF58476D1CE4E5B9L;
		value ^= value >>> 27;
		value *= 0x94D049BB133111EBL;
		value ^= value >>> 31;
		return (int)Math.floorMod(value, 3L) - 1;
	}

	private static long mix(long value) {
		value ^= value >>> 30;
		value *= 0xBF58476D1CE4E5B9L;
		value ^= value >>> 27;
		value *= 0x94D049BB133111EBL;
		return value ^ value >>> 31;
	}

	private static double pulseAlpha(double progress) {
		if (progress < 0.08) return progress / 0.08;
		if (progress > 0.62) return (1.0 - progress) / 0.38;
		return 1.0;
	}

	private static double smoothstep(double value) {
		double clamped = Math.clamp(value, 0.0, 1.0);
		return clamped * clamped * (3.0 - 2.0 * clamped);
	}

	private static int messageAlpha(long elapsedNanos) {
		long fadeStart = MESSAGE_DURATION_NANOS - MESSAGE_FADE_DURATION_NANOS;
		if (elapsedNanos < fadeStart) return 255;
		double fadeProgress = (elapsedNanos - fadeStart) / (double)MESSAGE_FADE_DURATION_NANOS;
		return Math.clamp((int)Math.round(255.0 * (1.0 - fadeProgress)), 0, 255);
	}
}
