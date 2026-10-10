package com.yuriscat.echowarrior.compat.client;

import com.yuriscat.echowarrior.compat.EchoWarrior1201;
import com.yuriscat.echowarrior.compat.ModContent1201;
import com.yuriscat.echowarrior.compat.test.JoinPausePolicy1201;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemStack;

/** Opt-in smoke test only. Never captures the author's mouse or alters normal play. */
public final class AutomatedTestController1201 {
    public static final boolean ENABLED = Boolean.getBoolean("echo_warrior.auto_pause_after_quick_play");
    private static final JoinPausePolicy1201 PAUSE = new JoinPausePolicy1201(
            ENABLED, Boolean.getBoolean("echo_warrior.pause_on_join"));
    private static long pausedAt;
    private static boolean stopping;
    private static java.util.concurrent.CompletableFuture<Void> protection;
    private static java.util.concurrent.CompletableFuture<Void> restoredProtection;
    private static boolean respawnRequested;

    private AutomatedTestController1201() {}

    public static boolean blockMouseCapture() { return PAUSE.blockMouseCapture(); }

    public static void tick(Minecraft client) {
        if (!PAUSE.enabled() || stopping) return;
        if (client.level == null) {
            PAUSE.onDisconnected();
            pausedAt = 0;
            return;
        }
        if (client.player == null) return;
        if (ENABLED && !client.player.isAlive()) {
            if (!respawnRequested) { client.player.respawn(); respawnRequested = true; }
            return;
        }
        if (ENABLED && protection == null) {
            var server = client.getSingleplayerServer();
            var id = client.player.getUUID();
            protection = server.submit(() -> com.yuriscat.echowarrior.compat.test.ClientTestProtection1201.begin(server, id));
            return;
        }
        if (ENABLED && !protection.isDone()) return;
        if (ENABLED) protection.join();
        if (PAUSE.needsPause()) {
            if (client.screen != null && !(client.screen instanceof PauseScreen)) return;
            if (!(client.screen instanceof PauseScreen)) client.setScreen(new PauseScreen(true));
            client.mouseHandler.releaseMouse();
            if (client.mouseHandler.isMouseGrabbed()) throw new IllegalStateException("Smoke test mouse is grabbed");
            PAUSE.onPauseShown();
            pausedAt = System.nanoTime();
            if (!ENABLED) {
                EchoWarrior1201.LOGGER.info("[Compat1201] Manual test client paused on join; mouse released; waiting for the player to resume (no automatic shutdown)");
                return;
            }
            BakedModel model = client.getItemRenderer().getModel(new ItemStack(ModContent1201.ECHO_SUMMONER),
                    client.level, client.player, 0);
            if (model == client.getModelManager().getMissingModel()
                    || !model.getParticleIcon().contents().name().toString().equals("echo_warrior:item/test_echo_summoner")) {
                throw new IllegalStateException("Summoner item model/texture was not baked correctly");
            }
            EchoWarrior1201.LOGGER.info("[Compat1201] Summoner item model and texture resolved");
            ModContent1201.items().forEach((id, item) -> {
                BakedModel itemModel = client.getItemRenderer().getModel(new ItemStack(item), client.level, client.player, 0);
                boolean recycler = item == ModContent1201.ECHO_RECYCLER_ITEM;
                String texture = itemModel.getParticleIcon().contents().name().toString();
                boolean exploration = item == ModContent1201.ECHO_COMPASS
                        ? texture.equals("echo_warrior:item/echo_compass/echo_compass_background")
                        : (item == ModContent1201.SUSPICIOUS_GRASS_BLOCK_ITEM || item == ModContent1201.SUSPICIOUS_DIRT_ITEM)
                            && !texture.equals("minecraft:missingno");
                if (itemModel == client.getModelManager().getMissingModel()
                        || (recycler ? !itemModel.isCustomRenderer()
                            : !exploration && !texture.equals("echo_warrior:item/" + id.getPath()))) {
                    throw new IllegalStateException("Missing registered item model/texture: " + id);
                }
                if (!net.minecraft.client.resources.language.I18n.exists(item.getDescriptionId())) {
                    throw new IllegalStateException("Missing registered item translation: " + id);
                }
            });
            EchoWarrior1201.LOGGER.info("[Compat1201] All {} registered item models, textures and names resolved", ModContent1201.items().size());
            EchoWarrior1201.LOGGER.info("Automated client smoke test opened the pause menu to release the mouse.");
        } else if (PAUSE.shouldAutoClose() && NetworkClientSelfTest1201.tick(client) && MenuClientSelfTest1201.tick(client)
                && InventoryInsertionClientSelfTest1201.tick(client)
                && CreativeDestructionClientSelfTest1201.tick(client)
                && HeroMenuClientSelfTest1201.tick(client)
                && BooksClientSelfTest1201.tick(client)
                && ExplorationClientSelfTest1201.tick(client)
                && CompassHudClientSelfTest1201.tick(client)
                && RecyclerClientSelfTest1201.tick(client)
                && System.nanoTime() - pausedAt >= 12_000_000_000L) {
            if (restoredProtection == null) {
                var server = client.getSingleplayerServer();
                restoredProtection = server.submit(() -> com.yuriscat.echowarrior.compat.test.ClientTestProtection1201.restore(server));
                return;
            }
            if (!restoredProtection.isDone()) return;
            restoredProtection.join();
            stopping = true;
            EchoWarrior1201.LOGGER.info("[Compat1201] Client smoke test requesting normal shutdown");
            client.stop();
        }
    }
}
