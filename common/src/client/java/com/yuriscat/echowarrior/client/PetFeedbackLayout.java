package com.yuriscat.echowarrior.client;

import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** Wrap only our two pet-management messages; vanilla owns time, replacement and HUD visibility. */
public final class PetFeedbackLayout {
    private static final List<String> KEYS = List.of(
            "message.echo_warrior.pet_management_restricted",
            "message.echo_warrior.pet_compass_unavailable");
    private PetFeedbackLayout() {}

    public static boolean wraps(Font font, Component message, int guiWidth) {
        return message != null && message.getContents() instanceof TranslatableContents text
                && KEYS.contains(text.getKey()) && font.width(message) > availableWidth(guiWidth);
    }

    public static int availableWidth(int guiWidth) { return Math.max(1, guiWidth - 16); }

    public static List<FormattedCharSequence> lines(Font font, Component message, int guiWidth) {
        return font.split(message, availableWidth(guiWidth));
    }

    /** Called inside vanilla's action-bar pose, with the last row on its original baseline. */
    public static void draw(GuiGraphicsExtractor graphics, Font font, Component message, int y, int color) {
        var lines = lines(font, message, graphics.guiWidth());
        int rowY = y - (lines.size() - 1) * (font.lineHeight + 2);
        int background = Minecraft.getInstance().options.getBackgroundColor(0.0F);
        for (var line : lines) {
            int width = font.width(line);
            int x = -width / 2;
            if (background != 0)
                graphics.fill(x - 2, rowY - 2, x + width + 2, rowY + font.lineHeight + 2,
                        ARGB.multiply(background, color));
            graphics.text(font, line, x, rowY, color, true);
            rowY += font.lineHeight + 2;
        }
    }

    /** Opt-in production-client check using actual loaded glyph metrics, not character-count estimates. */
    public static void selfTest(Minecraft client) {
        int cases = 0;
        try {
            for (String locale : List.of("en_us", "zh_cn", "zh_tw", "ja_jp", "ru_ru", "pt_br", "es_es", "es_mx")) {
                var id = Identifier.fromNamespaceAndPath("echo_warrior", "lang/" + locale + ".json");
                try (var reader = new InputStreamReader(client.getResourceManager().getResource(id).orElseThrow().open(),
                        StandardCharsets.UTF_8)) {
                    var language = JsonParser.parseReader(reader).getAsJsonObject();
                    for (String key : KEYS) {
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
                    || wraps(client.font, Component.translatable(KEYS.getFirst()), 10000)
                    || !wraps(client.font, Component.translatable(KEYS.getFirst()), 20))
                throw new IllegalStateException("Pet message scope or fit regression");
        } catch (Exception failure) {
            throw new IllegalStateException("Pet feedback layout self-test failed", failure);
        }
        org.slf4j.LoggerFactory.getLogger("echo_warrior").info(
                "PET FEEDBACK LAYOUT SELFTEST PASSED cases={} locales=8 widths=320,426,640 scope=pet-only", cases);
    }
}
