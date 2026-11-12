package com.yuriscat.echowarrior.compat.client;

import com.yuriscat.echowarrior.compat.item.EchoSkillScaling1201;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.lwjgl.glfw.GLFW;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** The same short copy is used by summoner and tutorial; Alt is sampled each frame. */
public final class EchoSkillTooltips1201 {
    private EchoSkillTooltips1201() { }

    public static boolean append(List<Component> into, String key, int level, double attack) {
        if (!I18n.exists(key + ".simple.1")) return false;
        long window = Minecraft.getInstance().getWindow().getWindow();
        boolean expanded = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_ALT) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_ALT) == GLFW.GLFW_PRESS;
        into.addAll(descriptions(key, level, attack, expanded));
        return true;
    }

    public static List<Component> descriptions(String key, int level, double attack, boolean expanded) {
        Map<String, String> values = Map.ofEntries(
                Map.entry("armor", number(EchoSkillScaling1201.legionArmor(level))),
                Map.entry("release", number(EchoSkillScaling1201.legionRelease(level) * 100)),
                Map.entry("curse", number(EchoSkillScaling1201.cursePerEffect(level))),
                Map.entry("bleed", number(EchoSkillScaling1201.bleedingDamage(attack))),
                Map.entry("half", number(attack * 0.5)),
                Map.entry("threequarters", number(attack * 0.75)),
                Map.entry("penetration", number(EchoSkillScaling1201.conePenetration(level) * 100)),
                Map.entry("pierce", number(EchoSkillScaling1201.conePierceChance(level) * 100)),
                Map.entry("hit1", number(attack * 0.7)),
                Map.entry("hit2", number(attack)),
                Map.entry("hit4", number(attack * 1.8)));
        List<Component> result = new ArrayList<>();
        String prefix = key + (expanded ? ".full." : ".simple.");
        for (int line = 1; I18n.exists(prefix + line); line++) {
            String text = Component.translatable(prefix + line).getString();
            for (var value : values.entrySet()) text = text.replace("{" + value.getKey() + "}", value.getValue());
            result.add(colored(text));
        }
        return result;
    }

    public static List<net.minecraft.util.FormattedCharSequence> wrap(net.minecraft.client.gui.Font font,
            List<Component> lines, int screenWidth) {
        int width = Math.max(1, Math.min(320, screenWidth - 16));
        List<net.minecraft.util.FormattedCharSequence> wrapped = new ArrayList<>();
        for (Component line : lines) wrapped.addAll(font.split(line, width));
        return wrapped;
    }

    public static String number(double value) {
        return String.format(Locale.ROOT, "%.2f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    /** Tiny, explicit semantic markup keeps translators independent of Chinese word matching. */
    public static Component colored(String text) {
        MutableComponent result = Component.empty();
        ArrayDeque<Integer> colors = new ArrayDeque<>();
        colors.push(0xFFFFFF);
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("</?(?:gray|attack|level)>").matcher(text);
        int last = 0;
        while (matcher.find()) {
            if (matcher.start() > last) result.append(Component.literal(text.substring(last, matcher.start()))
                    .withStyle(style -> style.withColor(colors.peek())));
            String token = matcher.group();
            if (token.startsWith("</")) {
                if (colors.size() > 1) colors.pop();
            } else colors.push(switch (token) {
                case "<attack>" -> 0xFF5555;
                case "<level>" -> 0x55FF55;
                default -> 0xAAAAAA;
            });
            last = matcher.end();
        }
        if (last < text.length()) result.append(Component.literal(text.substring(last)).withStyle(style -> style.withColor(colors.peek())));
        return result;
    }
}
