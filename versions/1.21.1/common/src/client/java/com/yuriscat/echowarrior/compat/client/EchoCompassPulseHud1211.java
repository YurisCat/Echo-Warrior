package com.yuriscat.echowarrior.compat.client;

import com.yuriscat.echowarrior.compat.ModContent1211;
import com.yuriscat.echowarrior.compat.item.EchoCompassItem1211;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Client-side directional pulse display driven by the server-synced compass item state. */
public final class EchoCompassPulseHud1211 {
    private static final long PULSE_DURATION_NANOS = 650_000_000L;
    private static final long MESSAGE_DURATION_NANOS = 2_400_000_000L;
    private static final long MESSAGE_SHAKE_DURATION_NANOS = 650_000_000L;
    private static final long MESSAGE_FADE_DURATION_NANOS = 450_000_000L;
    private static final String TRANSLATION_KEY = "hud.echo_warrior.echo_compass.directional_pulse";
    private static final List<String> OVERLAY_MESSAGE_KEYS = List.of(
            "message.echo_warrior.echo_compass.sound_enabled",
            "message.echo_warrior.echo_compass.sound_disabled",
            "message.echo_warrior.echo_compass.echo_detected",
            "message.echo_warrior.echo_compass.no_nearby_site",
            "message.echo_warrior.echo_compass.remaining_echoes",
            "message.echo_warrior.echo_compass.site_quiet"
    );
    private static long nextPulseTick;
    private static long pulseStartedAt;
    private static long pulseSeed;
    private static long targetPos;
    private static int mode;
    private static float closeness;
    private static boolean inner;
    private static Component message;
    private static long messageStartedAt;
    private static long messageSeed;

    private EchoCompassPulseHud1211() {
    }

    public static void tick(Minecraft client) {
        if (client.player == null || client.level == null) {
            clear();
            return;
        }
        ItemStack compass = displayedCompass(client);
        if (compass.isEmpty()) {
            clearPulse();
            return;
        }
        int nextMode = EchoCompassItem1211.trackingMode(compass);
        long nextTarget = EchoCompassItem1211.trackingTarget(compass);
        if (nextMode != mode || nextTarget != targetPos) {
            mode = nextMode;
            targetPos = nextTarget;
            nextPulseTick = client.level.getGameTime();
        }
        if (mode != EchoCompassItem1211.MODE_OUTSIDE
                && mode != EchoCompassItem1211.MODE_INNER
                && mode != EchoCompassItem1211.MODE_SALVAGE) return;

        double distance = client.player.position().distanceTo(BlockPos.of(targetPos).getCenter());
        inner = mode == EchoCompassItem1211.MODE_INNER || mode == EchoCompassItem1211.MODE_SALVAGE;
        if (!inner && distance > 320.0) return;
        closeness = (float)Math.clamp(1.0 - distance / (inner ? 48.0 : 320.0), 0.0, 1.0);
        long now = client.level.getGameTime();
        if (now < nextPulseTick) return;
        pulseStartedAt = System.nanoTime();
        pulseSeed++;
        nextPulseTick = now + (inner ? directionalPulseInterval(distance)
                : Math.round(80.0 + (1.0 - closeness) * 160.0));
    }

    public static void render(GuiGraphics graphics) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.options.hideGui) return;
        renderMessage(graphics, client);
        renderDirectionalPulse(graphics, client);
    }

    /** Converts only Echo Compass action-bar components into the custom per-grapheme presentation. */
    public static boolean acceptOverlayMessage(Component component) {
        if (!(component.getContents() instanceof TranslatableContents translatable)
                || !OVERLAY_MESSAGE_KEYS.contains(translatable.getKey())) return false;
        message = component;
        messageStartedAt = System.nanoTime();
        messageSeed++;
        return true;
    }

    public static boolean isTooltipShakeActive() {
        if (pulseStartedAt == 0L) return false;
        long elapsed = System.nanoTime() - pulseStartedAt;
        return elapsed >= 0L && elapsed < PULSE_DURATION_NANOS;
    }

    public static void renderTooltipTitle(Font font, Component title, int x, int y, Matrix4f matrix,
                                          MultiBufferSource.BufferSource buffers, int rgb) {
        long elapsed = System.nanoTime() - pulseStartedAt;
        if (elapsed < 0L || elapsed >= PULSE_DURATION_NANOS) {
            font.drawInBatch(title, x, y, rgb, true, matrix, buffers, Font.DisplayMode.NORMAL, 0, 15728880);
            return;
        }
        List<String> titleGraphemes = graphemes(title.getString(), Minecraft.getInstance().options.languageCode);
        if (Language.getInstance().isDefaultRightToLeft()) Collections.reverse(titleGraphemes);
        long step = shakeStep(elapsed, 5.0 + 23.0 * smoothstep(closeness));
        int drawX = x;
        for (int index = 0; index < titleGraphemes.size(); index++) {
            String grapheme = titleGraphemes.get(index);
            int width = font.width(grapheme);
            if (!grapheme.isBlank()) {
                font.drawInBatch(grapheme, drawX + jitter(pulseSeed, index, step, 0),
                        y + jitter(pulseSeed, index, step, 1), rgb, true, matrix, buffers,
                        Font.DisplayMode.NORMAL, 0, 15728880);
            }
            drawX += width;
        }
    }

    private static void renderMessage(GuiGraphics graphics, Minecraft client) {
        if (message == null || messageStartedAt == 0L) return;
        long elapsed = System.nanoTime() - messageStartedAt;
        if (elapsed < 0L || elapsed >= MESSAGE_DURATION_NANOS) {
            message = null;
            messageStartedAt = 0L;
            return;
        }
        List<String> messageGraphemes = graphemes(message.getString(), client.options.languageCode);
        if (messageGraphemes.isEmpty()) return;
        if (Language.getInstance().isDefaultRightToLeft()) Collections.reverse(messageGraphemes);
        Font font = client.font;
        int width = messageGraphemes.stream().mapToInt(font::width).sum();
        int x = Math.max(8, (graphics.guiWidth() - width) / 2);
        int y = Math.max(20, graphics.guiHeight() - 68);
        int alpha = messageAlpha(elapsed);
        int color = alpha << 24 | 0xFFFFFF;
        boolean shaking = elapsed < MESSAGE_SHAKE_DURATION_NANOS;
        long step = shaking ? shakeStep(elapsed, 22.0) : 0L;
        for (int index = 0; index < messageGraphemes.size(); index++) {
            String grapheme = messageGraphemes.get(index);
            int glyphWidth = font.width(grapheme);
            if (!grapheme.isBlank()) {
                int offsetX = shaking ? jitter(messageSeed, index, step, 0) : 0;
                int offsetY = shaking ? jitter(messageSeed, index, step, 1) : 0;
                graphics.drawString(font, grapheme, x + offsetX, y + offsetY, color, true);
            }
            x += glyphWidth;
        }
    }

    private static void renderDirectionalPulse(GuiGraphics graphics, Minecraft client) {
        if (pulseStartedAt == 0L || displayedCompass(client).isEmpty()) return;
        long elapsed = System.nanoTime() - pulseStartedAt;
        if (elapsed < 0L || elapsed >= PULSE_DURATION_NANOS) return;

        String text = Component.translatable(TRANSLATION_KEY).getString();
        List<String> graphemes = graphemes(text, client.options.languageCode);
        if (graphemes.isEmpty()) return;
        if (Language.getInstance().isDefaultRightToLeft()) Collections.reverse(graphemes);
        Font font = client.font;
        int width = graphemes.stream().mapToInt(font::width).sum();
        Camera camera = client.gameRenderer.getMainCamera();
        int centerX = directionalCenterX(graphics, camera);
        int x = Math.clamp(centerX - width / 2, 8, Math.max(8, graphics.guiWidth() - width - 8));
        int y = directionalY(graphics, camera);
        double distanceResponse = smoothstep(closeness);
        double distanceAlpha = inner ? 0.50 + distanceResponse * 0.50 : 0.10 + distanceResponse * 0.40;
        double progress = elapsed / (double)PULSE_DURATION_NANOS;
        double pulseAlpha = progress < 0.08 ? progress / 0.08
                : progress > 0.62 ? (1.0 - progress) / 0.38 : 1.0;
        int alpha = Math.clamp((int)Math.round(255.0 * distanceAlpha * pulseAlpha), 0, 255);
        long shakeStep = shakeStep(elapsed, 5.0 + 23.0 * distanceResponse);
        int color = alpha << 24 | 0xFFAA00;
        for (int index = 0; index < graphemes.size(); index++) {
            String grapheme = graphemes.get(index);
            int glyphWidth = font.width(grapheme);
            if (!grapheme.isBlank()) {
                graphics.drawString(font, grapheme, x + jitter(pulseSeed, index, shakeStep, 0),
                        y + jitter(pulseSeed, index, shakeStep, 1), color, true);
            }
            x += glyphWidth;
        }
    }

    private static ItemStack displayedCompass(Minecraft client) {
        ItemStack main = client.player == null ? ItemStack.EMPTY : client.player.getMainHandItem();
        if (main.is(ModContent1211.ECHO_COMPASS)) return main;
        ItemStack offhand = client.player == null ? ItemStack.EMPTY : client.player.getOffhandItem();
        return offhand.is(ModContent1211.ECHO_COMPASS) ? offhand : ItemStack.EMPTY;
    }

    private static int directionalCenterX(GuiGraphics graphics, Camera camera) {
        Vec3 target = BlockPos.of(targetPos).getCenter();
        Vec3 cameraPos = camera.getPosition();
        double dx = target.x - cameraPos.x;
        double dz = target.z - cameraPos.z;
        double relativeYaw = 0.0;
        if (dx * dx + dz * dz > 1.0E-6) {
            relativeYaw = Mth.wrapDegrees(Math.toDegrees(Math.atan2(-dx, dz)) - camera.getYRot());
        }
        double normalized = Math.clamp(relativeYaw / 90.0, -1.0, 1.0);
        int range = Math.max(40, (int)Math.round(graphics.guiWidth() * 0.32));
        return graphics.guiWidth() / 2 + (int)Math.round(normalized * range);
    }

    private static int directionalY(GuiGraphics graphics, Camera camera) {
        int minY = Math.max(20, (int)Math.round(graphics.guiHeight() * 0.18));
        int maxY = Math.max(minY, (int)Math.round(graphics.guiHeight() * 0.64));
        Vec3 target = BlockPos.of(targetPos).getCenter();
        Vec3 cameraPos = camera.getPosition();
        double dx = target.x - cameraPos.x;
        double dy = target.y - cameraPos.y;
        double dz = target.z - cameraPos.z;
        double targetPitch = -Math.toDegrees(Math.atan2(dy, Math.max(Math.sqrt(dx * dx + dz * dz), 1.0E-6)));
        double normalized = Math.clamp(Mth.wrapDegrees(targetPitch - camera.getXRot()) / 55.0, -1.0, 1.0);
        double random = Math.floorMod(mix(pulseSeed ^ 0xD1B54A32D192ED03L), 2001L) / 1000.0 - 1.0;
        return Math.clamp((int)Math.round((minY + maxY) * 0.5
                + normalized * (maxY - minY) * 0.38 + random * (maxY - minY) * 0.07), minY, maxY);
    }

    private static long directionalPulseInterval(double distance) {
        double value = Math.clamp(distance, 0.0, 48.0);
        if (value <= 4.0) return staged(value, 0.0, 4.0, 14.0, 18.0);
        if (value <= 10.0) return staged(value, 4.0, 10.0, 18.0, 24.0);
        if (value <= 20.0) return staged(value, 10.0, 20.0, 24.0, 36.0);
        if (value <= 32.0) return staged(value, 20.0, 32.0, 36.0, 52.0);
        return staged(value, 32.0, 48.0, 52.0, 72.0);
    }

    private static long staged(double value, double minimum, double maximum, double start, double end) {
        double progress = smoothstep((value - minimum) / (maximum - minimum));
        return Math.round(start + (end - start) * progress);
    }

    private static double smoothstep(double value) {
        double clamped = Math.clamp(value, 0.0, 1.0);
        return clamped * clamped * (3.0 - 2.0 * clamped);
    }

    private static List<String> graphemes(String text, String languageCode) {
        BreakIterator iterator = BreakIterator.getCharacterInstance(Locale.forLanguageTag(languageCode.replace('_', '-')));
        iterator.setText(text);
        List<String> result = new ArrayList<>();
        int start = iterator.first();
        for (int end = iterator.next(); end != BreakIterator.DONE; start = end, end = iterator.next()) {
            result.add(text.substring(start, end));
        }
        return result;
    }

    private static long shakeStep(long elapsed, double frequency) {
        return (long)Math.floor(elapsed / 1_000_000_000.0 * frequency);
    }

    private static int messageAlpha(long elapsed) {
        long fadeStartedAt = MESSAGE_DURATION_NANOS - MESSAGE_FADE_DURATION_NANOS;
        if (elapsed <= fadeStartedAt) return 255;
        double progress = (elapsed - fadeStartedAt) / (double)MESSAGE_FADE_DURATION_NANOS;
        return Math.clamp((int)Math.round(255.0 * (1.0 - progress)), 0, 255);
    }

    private static int jitter(long seed, int index, long step, int axis) {
        long value = seed * 0x9E3779B97F4A7C15L ^ (long)index * 0xC2B2AE3D27D4EB4FL
                ^ step * 0x165667B19E3779F9L ^ (long)axis * 0x85EBCA77C2B2AE63L;
        return (int)Math.floorMod(mix(value), 3L) - 1;
    }

    private static long mix(long value) {
        value ^= value >>> 30;
        value *= 0xBF58476D1CE4E5B9L;
        value ^= value >>> 27;
        value *= 0x94D049BB133111EBL;
        return value ^ value >>> 31;
    }

    private static void clear() {
        clearPulse();
        message = null;
        messageStartedAt = 0L;
    }

    private static void clearPulse() {
        nextPulseTick = 0L;
        pulseStartedAt = 0L;
        targetPos = 0L;
        mode = EchoCompassItem1211.MODE_INACTIVE;
    }
}
