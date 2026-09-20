package com.yuriscat.echowarrior.compat.client;

import com.yuriscat.echowarrior.compat.*;
import com.yuriscat.echowarrior.compat.knowledge.*;
import com.yuriscat.echowarrior.compat.tutorial.*;
import com.yuriscat.echowarrior.compat.menu.*;
import com.yuriscat.echowarrior.compat.test.HeroClientFixture1201;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/** Real item-use, vanilla button/slot packets and original book screens, in both hands. */
public final class BooksClientSelfTest1201 {
    private static int round, phase, page, delay;
    private static long deadline;
    private static CompletableFuture<HeroClientFixture1201> setup;
    private static CompletableFuture<Void> task;
    private static HeroClientFixture1201 fixture;
    private static final String[] CASES = {"tutorial-main", "tutorial-offhand", "knowledge-main", "knowledge-offhand"};
    private BooksClientSelfTest1201() {}
    public static boolean tick(Minecraft client) {
        if (round == 4) return true;
        var server = client.getSingleplayerServer();
        if (phase != 0 && phase != 99 && System.nanoTime() > deadline) finish(client, "Books timeout " + CASES[round] + " phase=" + phase + " page=" + page);
        boolean manual = round < 2, offhand = round % 2 == 1;
        int slot = offhand ? 40 : 0;
        ItemStack held = client.player.getInventory().getItem(slot);
        String first = KnowledgeCatalog1201.entries().get(0).id(), second = KnowledgeCatalog1201.entries().get(1).id();
        try {
            switch (phase) {
                case 0 -> {
                    var id = client.player.getUUID();
                    setup = server.submit(() -> {
                        var current = HeroClientFixture1201.prepare(server.getPlayerList().getPlayer(id), EchoHeroType1201.ROMAN_LEGIONARY);
                        try {
                            current.player.getInventory().clearContent();
                            ItemStack book = manual ? new ItemStack(ModContent1201.TUTORIAL_MANUAL)
                                    : KnowledgeStackData1201.collection(Map.of(first, 1, second, 2), first);
                            current.player.getInventory().setItem(slot, book);
                            // Both hands readable: opening the offhand must not overwrite/choose the main-hand book.
                            current.player.getInventory().setItem(offhand ? 0 : 40, KnowledgeStackData1201.fragment(KnowledgeCatalog1201.entries().get(39).id()));
                            current.player.inventoryMenu.broadcastChanges();
                            return current;
                        } catch (RuntimeException error) { current.restore(); throw error; }
                    });
                    advance(1);
                }
                case 1 -> { if (setup.isDone()) { fixture = setup.join(); advance(2); } }
                case 2 -> {
                    if (held.is(manual ? ModContent1201.TUTORIAL_MANUAL : ModContent1201.KNOWLEDGE_FRAGMENT_COLLECTION)) {
                        client.player.getInventory().selected = 0;
                        client.gameMode.useItem(client.player, offhand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
                        page = 0; advance(3);
                    }
                }
                case 3 -> {
                    if (manual && client.screen instanceof TutorialManualScreen1201 && client.player.containerMenu instanceof TutorialManualMenu1201 menu
                            && menu.currentPage() == page && TutorialManualStackData1201.bookmark(held) == page) {
                        // At least two client ticks per page: the normal rendering path sees every original page.
                        delay = client.player.tickCount + 2; advance(4);
                    } else if (!manual && client.screen instanceof KnowledgeReaderScreen1201 && client.player.containerMenu instanceof KnowledgeReaderMenu1201 menu
                            && menu.visiblePages().size() == 2 && KnowledgeStackData1201.bookmark(menu.sourceStack()).equals(first)) {
                        ((KnowledgeReaderScreen1201)client.screen).keyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT, 0, 0);
                        advance(5);
                    }
                }
                case 4 -> {
                    if (client.player.tickCount >= delay) {
                        if (page == 43) finish(client, null);
                        else { page++; client.gameMode.handleInventoryButtonClick(client.player.containerMenu.containerId, TutorialManualMenu1201.BUTTON_NEXT); advance(3); }
                    }
                }
                case 5 -> {
                    if (client.player.containerMenu instanceof KnowledgeReaderMenu1201 menu && KnowledgeStackData1201.bookmark(held).equals(second)
                            && KnowledgeStackData1201.bookmark(menu.sourceStack()).equals(second)) {
                        client.player.closeContainer();
                        client.gameMode.useItem(client.player, offhand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
                        advance(6);
                    }
                }
                case 6 -> {
                    if (client.screen instanceof KnowledgeReaderScreen1201 && client.player.containerMenu instanceof KnowledgeReaderMenu1201 menu
                            && KnowledgeStackData1201.bookmark(menu.sourceStack()).equals(second)) {
                        client.gameMode.handleInventoryButtonClick(menu.containerId, KnowledgeReaderMenu1201.BUTTON_EXTRACT); advance(7);
                    }
                }
                case 7 -> {
                    if (KnowledgeStackData1201.totalCount(held) == 2 && client.player.containerMenu instanceof KnowledgeReaderMenu1201 menu) {
                        client.gameMode.handleInventoryButtonClick(menu.containerId, KnowledgeReaderMenu1201.BUTTON_EXTRACT); advance(8);
                    }
                }
                case 8 -> {
                    if (client.player.containerMenu == client.player.inventoryMenu && KnowledgeStackData1201.fragmentId(held).orElse("").equals(first)) {
                        int count = 0;
                        for (int i = 0; i < client.player.getInventory().getContainerSize(); i++) {
                            ItemStack stack = client.player.getInventory().getItem(i);
                            if (KnowledgeStackData1201.fragmentId(stack).orElse("").equals(second)) count += stack.getCount();
                        }
                        if (count == 2) finish(client, null);
                    }
                }
                case 99 -> {
                    if (task.isDone()) {
                        task.join();
                        EchoWarrior1201.LOGGER.info("[Compat1201] BOOKS CLIENT SELFTEST PASSED case={} source=server-snapshot pages={} inventory=restored", CASES[round], manual ? 44 : 2);
                        fixture = null; round++; phase = 0;
                    }
                }
                default -> { }
            }
        } catch (RuntimeException error) {
            if (phase == 99) throw error;
            finish(client, error.toString());
        }
        return round == 4;
    }
    private static void advance(int value) { phase = value; deadline = System.nanoTime() + 20_000_000_000L; }
    private static void finish(Minecraft client, String failure) {
        client.player.closeContainer(); client.setScreen(new PauseScreen(true)); client.mouseHandler.releaseMouse();
        var server = client.getSingleplayerServer();
        task = setup.thenCompose(current -> server.submit(() -> {
            try { if (failure != null) throw new IllegalStateException(failure); }
            finally { current.restore(); }
        }));
        phase = 99;
    }
}
