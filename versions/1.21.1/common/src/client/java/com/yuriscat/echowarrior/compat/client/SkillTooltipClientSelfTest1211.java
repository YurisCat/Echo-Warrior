package com.yuriscat.echowarrior.compat.client;

import com.yuriscat.echowarrior.compat.EchoWarrior1211;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

public final class SkillTooltipClientSelfTest1211 {
    private SkillTooltipClientSelfTest1211() { }
    public static void run() {
        String base = "gui.echo_warrior.summoner.skill.";
        List<Component> shortEndures = EchoSkillTooltips1211.descriptions(base + "roman.endures", 100, 26.48, false);
        List<Component> fullEndures = EchoSkillTooltips1211.descriptions(base + "roman.endures", 100, 26.48, true);
        check(shortEndures.size() == 1 && fullEndures.size() == 2, "held Alt expands; released uses short copy");
        check(text(shortEndures).contains("22*") && text(shortEndures).contains("200%*"), "current level-100 values");
        check(!text(EchoSkillTooltips1211.descriptions(base + "roman.formation", 100, 26.48, false)).contains("*"), "fixed aura has no star");
        var colors = new HashSet<Integer>();
        for (Component line : fullEndures) line.visit((style, part) -> {
            if (style.getColor() != null) colors.add(style.getColor().getValue());
            return Optional.empty();
        }, Style.EMPTY);
        check(colors.contains(0xAAAAAA) && colors.contains(0x55FF55), "gray formula and green level");
        var wound = EchoSkillTooltips1211.descriptions(base + "aztec.obsidian_wound", 100, 35.3103448276, true);
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
                var lines = EchoSkillTooltips1211.descriptions(base + skill, 100, 35.3103448276, full);
                String text = text(lines);
                for (int width : new int[]{160, 240, 320, 480}) {
                    var font = net.minecraft.client.Minecraft.getInstance().font;
                    var wrapped = EchoSkillTooltips1211.wrap(font, lines, width);
                    check(!wrapped.isEmpty() && wrapped.stream().allMatch(line -> font.width(line) <= Math.min(320, width - 16)),
                            "real-font wrapping stays on screen: " + skill + " width=" + width);
                }
                check(!lines.isEmpty() && !text.contains("{") && !text.contains("<") && !text.contains("gui.echo_warrior"),
                        "all copy resolves without placeholders: " + skill);
            }
        }
        check(text(EchoSkillTooltips1211.descriptions(base + "egyptian.cone_arrow", 200, 1, false)).contains("135%*"),
                "tooltip preserves penetration above 100%");
        EchoWarrior1211.LOGGER.info("SKILL TOOLTIP CLIENT SELFTEST PASSED current-values=true styles=gray-red-green short-full=9");
    }
    private static String text(List<Component> lines) {
        return String.join("\n", lines.stream().map(Component::getString).toList());
    }
    private static void check(boolean value, String message) {
        if (!value) throw new IllegalStateException("Skill tooltip: " + message);
    }
}
