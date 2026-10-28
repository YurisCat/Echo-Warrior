package com.yuriscat.echowarrior.compat.client;

import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.FormattedCharSequence;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** Wrap only our two pet-management messages; vanilla owns time, replacement and HUD visibility. */
public final class PetFeedbackLayout1211 {
    private static final List<String> KEYS = List.of(
            "message.echo_warrior.pet_management_restricted",
            "message.echo_warrior.pet_compass_unavailable");
    private static final List<String> TOOLTIP_KEYS = List.of(
            "tooltip.echo_warrior.pet_collar_saved",
            "tooltip.echo_warrior.pet_collar_limits",
            "tooltip.echo_warrior.pet_collar_immaturity");
    private PetFeedbackLayout1211() {}

    public static boolean wraps(Font font, Component message, int guiWidth) {
        return message != null && message.getContents() instanceof TranslatableContents text
                && KEYS.contains(text.getKey()) && font.width(message) > availableWidth(guiWidth);
    }

    public static int availableWidth(int guiWidth) { return Math.max(1, guiWidth - 16); }

    public static List<FormattedCharSequence> lines(Font font, Component message, int guiWidth) {
        return font.split(message, availableWidth(guiWidth));
    }

    /** Split only our collar explanation rows before vanilla or loader tooltip processing. */
    public static List<Component> wrapTooltipLines(Font font, List<Component> original, int guiWidth) {
        var result = new java.util.ArrayList<Component>();
        boolean changed = false;
        for (Component line : original) {
            if (line.getContents() instanceof TranslatableContents text && TOOLTIP_KEYS.contains(text.getKey())
                    && font.width(line) > availableWidth(guiWidth)) {
                for (var part : font.getSplitter().splitLines(line, availableWidth(guiWidth), line.getStyle())) {
                    var styled = Component.empty();
                    part.visit((style, value) -> {
                        styled.append(Component.literal(value).setStyle(style));
                        return java.util.Optional.empty();
                    }, line.getStyle());
                    result.add(styled);
                }
                changed = true;
            } else result.add(line);
        }
        return changed ? result : original;
    }

    /** Called inside vanilla's action-bar pose, with the last row on its original baseline. */
    public static void draw(GuiGraphics graphics, Font font, Component message, int y, int color) {
        var lines = lines(font, message, graphics.guiWidth());
        int rowY = y - (lines.size() - 1) * (font.lineHeight + 2);
        int background = Minecraft.getInstance().options.getBackgroundColor(0.0F);
        for (var line : lines) {
            int width = font.width(line);
            int x = -width / 2;
            if (background != 0)
                graphics.fill(x - 2, rowY - 2, x + width + 2, rowY + font.lineHeight + 2,
                        FastColor.ARGB32.multiply(background, color));
            graphics.drawString(font, line, x, rowY, color, true);
            rowY += font.lineHeight + 2;
        }
    }

    /** Opt-in production-client check using actual loaded glyph metrics, not character-count estimates. */
    public static void selfTest(Minecraft client) {
        int cases = 0;
        try {
            for (String locale : List.of("en_us", "zh_cn", "zh_tw", "ja_jp", "ru_ru", "pt_br", "es_es", "es_mx")) {
                var id = ResourceLocation.fromNamespaceAndPath("echo_warrior", "lang/" + locale + ".json");
                try (var reader = new InputStreamReader(client.getResourceManager().getResource(id).orElseThrow().open(),
                        StandardCharsets.UTF_8)) {
                    var language = JsonParser.parseReader(reader).getAsJsonObject();
                    for (String key : java.util.stream.Stream.concat(KEYS.stream(), TOOLTIP_KEYS.stream()).toList()) {
                        String text = language.get(key).getAsString();
                        for (int guiWidth : new int[]{320, 426, 640}) {
                            StringBuilder restored = new StringBuilder();
                            for (var line : lines(client.font, Component.literal(text), guiWidth)) {
                                if (client.font.width(line) > availableWidth(guiWidth))
                                    throw new IllegalStateException("Pet message exceeds viewport: " + locale);
                                line.accept((index, style, codePoint) -> { restored.appendCodePoint(codePoint); return true; });
                            }
                            if (!restored.toString().replaceAll("\\s", "").equals(text.replaceAll("\\s", "")))
                                throw new IllegalStateException("Pet message lost text: " + locale);
                            cases++;
                        }
                    }
                }
            }
            if (wraps(client.font, Component.literal("Unrelated ".repeat(200)), 320)
                    || wraps(client.font, Component.translatable("record.nowPlaying", "Record ".repeat(200)), 320)
                    || wraps(client.font, Component.translatable(KEYS.get(0)), 10000)
                    || !wraps(client.font, Component.translatable(KEYS.get(0)), 20))
                throw new IllegalStateException("Pet message scope or fit regression");
            List<Component> unrelated = List.of(Component.literal("Unrelated tooltip ".repeat(200)));
            if (wrapTooltipLines(client.font, unrelated, 320) != unrelated)
                throw new IllegalStateException("Unrelated tooltip was changed");
            for (String key : TOOLTIP_KEYS) {
                var original = Component.translatable(key).withStyle(net.minecraft.ChatFormatting.GOLD);
                StringBuilder restored = new StringBuilder();
                for (var line : wrapTooltipLines(client.font, List.of(original), 320)) {
                    if (client.font.width(line) > availableWidth(320))
                        throw new IllegalStateException("Collar tooltip exceeds viewport");
                    line.visit((style, value) -> {
                        if (!value.isEmpty() && !original.getStyle().getColor().equals(style.getColor()))
                            throw new IllegalStateException("Collar tooltip lost its color");
                        return java.util.Optional.empty();
                    }, line.getStyle());
                    restored.append(line.getString());
                }
                if (!restored.toString().replaceAll("\\s", "").equals(original.getString().replaceAll("\\s", "")))
                    throw new IllegalStateException("Collar tooltip lost text");
            }
        } catch (Exception failure) {
            throw new IllegalStateException("Pet feedback layout self-test failed", failure);
        }
        org.slf4j.LoggerFactory.getLogger("echo_warrior").info(
                "PET FEEDBACK LAYOUT SELFTEST PASSED cases={} locales=8 widths=320,426,640 scope=pet-only", cases);
    }
}
