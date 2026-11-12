package com.yuriscat.echowarrior.client;

import com.yuriscat.echowarrior.EchoWarrior;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

public final class SkillTooltipClientSelfTest {
    private SkillTooltipClientSelfTest() { }
    public static void run() {
        String base = "gui.echo_warrior.summoner.skill.";
        List<Component> shortEndures = EchoSkillTooltips.descriptions(base + "roman.endures", 100, 26.48, false);
        List<Component> fullEndures = EchoSkillTooltips.descriptions(base + "roman.endures", 100, 26.48, true);
        check(shortEndures.size() == 1 && fullEndures.size() == 2, "held Alt expands; released uses short copy");
        check(text(shortEndures).contains("22*") && text(shortEndures).contains("200%*"), "current level-100 values");
        check(!text(EchoSkillTooltips.descriptions(base + "roman.formation", 100, 26.48, false)).contains("*"), "fixed aura has no star");
        var colors = new HashSet<Integer>();
        for (Component line : fullEndures) line.visit((style, part) -> {
            if (style.getColor() != null) colors.add(style.getColor().getValue());
            return Optional.empty();
        }, Style.EMPTY);
        check(colors.contains(0xAAAAAA) && colors.contains(0x55FF55), "gray formula and green level");
        var wound = EchoSkillTooltips.descriptions(base + "aztec.obsidian_wound", 100, 35.3103448276, true);
        check(text(wound).contains("4.53*"), "current attack snapshot rounds only for display");
        colors.clear();
        for (Component line : wound) line.visit((style, part) -> {
            if (style.getColor() != null) colors.add(style.getColor().getValue());
            return Optional.empty();
        }, Style.EMPTY);
        check(colors.contains(0xFF5555), "attack term red");
        for (String skill : List.of("roman.formation", "roman.endures", "aztec.quetzalcoatls_curse",
                "aztec.obsidian_wound", "aztec.macuahuitl", "egyptian.cat_god",
                "egyptian.leaf_arrow", "egyptian.cone_arrow", "guandao.combo")) {
            for (boolean full : new boolean[]{false, true}) {
                var lines = EchoSkillTooltips.descriptions(base + skill, 100, 35.3103448276, full);
                String text = text(lines);
                for (int width : new int[]{160, 240, 320, 480}) {
                    var font = net.minecraft.client.Minecraft.getInstance().font;
                    var wrapped = EchoSkillTooltips.wrap(font, lines, width);
                    check(!wrapped.isEmpty() && wrapped.stream().allMatch(line -> font.width(line) <= Math.min(320, width - 16)),
                            "real-font wrapping stays on screen: " + skill + " width=" + width);
                }
                check(!lines.isEmpty() && !text.contains("{") && !text.contains("<") && !text.contains("gui.echo_warrior"),
                        "all copy resolves without placeholders: " + skill);
            }
        }
        checkTranslations(base);
        check(text(EchoSkillTooltips.descriptions(base + "egyptian.cone_arrow", 200, 1, false)).contains("135%*"),
                "tooltip preserves penetration above 100%");
        EchoWarrior.LOGGER.info("SKILL TOOLTIP CLIENT SELFTEST PASSED current-values=true styles=gray-red-green short-full=9 resource-locales=8 accessory-wrap=true locale={}",
                net.minecraft.client.Minecraft.getInstance().options.languageCode);
    }
    private static void checkTranslations(String base) {
        var client = net.minecraft.client.Minecraft.getInstance();
        for (String locale : List.of("en_us", "zh_cn", "zh_tw", "ja_jp", "ru_ru", "pt_br", "es_es", "es_mx")) {
            var id = net.minecraft.resources.Identifier.fromNamespaceAndPath("echo_warrior", "lang/" + locale + ".json");
            try (var reader = new java.io.InputStreamReader(
                    client.getResourceManager().getResource(id).orElseThrow().open(), java.nio.charset.StandardCharsets.UTF_8)) {
                var entries = com.google.gson.JsonParser.parseReader(reader).getAsJsonObject();
                for (var entry : entries.entrySet()) {
                    if (locale.equals(client.options.languageCode)
                            && entry.getKey().startsWith("item.echo_warrior.accessory.")
                            && entry.getKey().contains(".effect.")) {
                        checkAccessoryTooltip(entry.getKey(), client);
                    }
                    if (!entry.getKey().startsWith(base)
                            || !(entry.getKey().contains(".simple.") || entry.getKey().contains(".full."))) continue;
                    // A deliberately wide current value exercises wrapping in every shipped base locale.
                    var line = EchoSkillTooltips.colored(entry.getValue().getAsString().replaceAll("\\{[a-z][a-z0-9_]*\\}", "123.45"));
                    for (int width : new int[]{160, 240, 320, 480}) {
                        var restored = new StringBuilder();
                        for (var wrapped : EchoSkillTooltips.wrap(client.font, List.of(line), width)) {
                            check(client.font.width(wrapped) <= Math.min(320, width - 16),
                                    "localized real-font width: " + locale + " " + entry.getKey());
                            wrapped.accept((index, style, codePoint) -> { restored.appendCodePoint(codePoint); return true; });
                        }
                        check(restored.toString().replaceAll("\\s", "").equals(line.getString().replaceAll("\\s", "")),
                                "localized text remains complete: " + locale + " " + entry.getKey());
                    }
                }
            } catch (java.io.IOException failure) {
                throw new IllegalStateException("Cannot check skill locale: " + locale, failure);
            }
        }
    }
    private static void checkAccessoryTooltip(String key, net.minecraft.client.Minecraft client) {
        for (int color : new int[]{0x93CE85, 0xE46D6D}) {
            var original = Component.literal("+").withStyle(style -> style.withColor(color))
                    .append(Component.translatable(key).withStyle(style -> style.withColor(color)));
            for (int width : new int[]{160, 240, 320, 480}) {
                var restored = new StringBuilder();
                for (var line : PetFeedbackLayout.wrapTooltipLines(client.font, List.of(original), width)) {
                    check(client.font.width(line) <= PetFeedbackLayout.availableWidth(width), "accessory tooltip width: " + key);
                    line.visit((style, value) -> {
                        check(value.isEmpty() || style.getColor() != null && style.getColor().getValue() == color,
                                "accessory polarity color: " + key);
                        return Optional.empty();
                    }, line.getStyle());
                    restored.append(line.getString());
                }
                check(restored.toString().replaceAll("\\s", "").equals(original.getString().replaceAll("\\s", "")),
                        "accessory tooltip remains complete: " + key);
            }
        }
    }
    private static String text(List<Component> lines) {
        // Localized percent spacing is typography; keep the numeric assertions unchanged.
        return String.join("\n", lines.stream().map(Component::getString).toList())
                .replace(" %", "%").replace("\u00a0%", "%").replace("\u202f%", "%");
    }
    private static void check(boolean value, String message) {
        if (!value) throw new IllegalStateException("Skill tooltip: " + message);
    }
}
