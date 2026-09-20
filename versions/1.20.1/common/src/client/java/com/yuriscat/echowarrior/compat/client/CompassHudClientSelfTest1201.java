package com.yuriscat.echowarrior.compat.client;

import com.yuriscat.echowarrior.compat.EchoWarrior1201;
import com.yuriscat.echowarrior.compat.ModContent1201;
import com.yuriscat.echowarrior.compat.item.EchoCompassItem1201;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Never calls the HUD renderer: evidence must come through the actual loader's frame callback. */
public final class CompassHudClientSelfTest1201 {
    private static final String[] KEYS = {"sound_enabled", "sound_disabled", "echo_detected", "no_nearby_site", "remaining_echoes", "site_quiet"};
    private static int phase;
    private static long deadline;
    private static ItemStack main, offhand;
    private static boolean hidden, pulse, message;
    private static Component expected;
    private CompassHudClientSelfTest1201() {}

    static void messageDrawn(Component rendered) { if (rendered.equals(expected)) message = true; }
    static void pulseDrawn() { if (phase > 0 && phase <= KEYS.length) pulse = true; }

    public static boolean tick(Minecraft client) {
        if (phase == KEYS.length + 1) return true;
        try {
            if (phase == 0) {
                // The preceding suite has paused the integrated server. No packets or persistent inventory edits.
                client.setScreen(new PauseScreen(true));
                client.mouseHandler.releaseMouse();
                main = client.player.getMainHandItem(); offhand = client.player.getOffhandItem();
                hidden = client.options.hideGui; client.options.hideGui = false;
                var compass = new ItemStack(ModContent1201.ECHO_COMPASS);
                EchoCompassItem1201.writeTracking(compass, EchoCompassItem1201.MODE_INNER, client.player.blockPosition().north(3).asLong());
                client.player.getInventory().setItem(client.player.getInventory().selected, ItemStack.EMPTY);
                client.player.getInventory().setItem(40, compass);
                nextMessage(client);
            } else {
                if (System.nanoTime() > deadline) throw new IllegalStateException("Compass HUD callback timed out: " + KEYS[phase - 1]);
                if (message && pulse) {
                    if (phase < KEYS.length) nextMessage(client);
                    else {
                        restore(client); phase++;
                        EchoWarrior1201.LOGGER.info("[Compat1201] COMPASS HUD SELFTEST PASSED messages=6 pulse=orange-glyphs path=loader-frame inventory=restored");
                    }
                }
            }
        } catch (RuntimeException error) { restore(client); throw error; }
        return phase == KEYS.length + 1;
    }

    private static void nextMessage(Minecraft client) {
        expected = Component.translatable("message.echo_warrior.echo_compass." + KEYS[phase], 3);
        if (expected.getString().contains("message.echo_warrior")) throw new IllegalStateException("Untranslated compass message");
        message = false;
        client.gui.setOverlayMessage(expected, false);
        phase++; deadline = System.nanoTime() + 8_000_000_000L;
    }

    private static void restore(Minecraft client) {
        if (main == null) return;
        client.player.getInventory().setItem(client.player.getInventory().selected, main);
        client.player.getInventory().setItem(40, offhand);
        client.options.hideGui = hidden;
        main = null; offhand = null; expected = null;
    }
}
