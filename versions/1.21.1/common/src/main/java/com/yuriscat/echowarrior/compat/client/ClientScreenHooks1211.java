package com.yuriscat.echowarrior.compat.client;

import net.minecraft.world.item.ItemStack;

import java.util.function.Consumer;

/** Loader-neutral bridge that keeps client screen classes off dedicated-server classpaths. */
public final class ClientScreenHooks1211 {
    private static Consumer<ItemStack> knowledgeOpener = ignored -> { };
    private static Runnable tutorialOpener = () -> { };

    private ClientScreenHooks1211() {
    }

    public static void setKnowledgeOpener(Consumer<ItemStack> opener) {
        knowledgeOpener = opener == null ? ignored -> { } : opener;
    }

    public static void setTutorialOpener(Runnable opener) {
        tutorialOpener = opener == null ? () -> { } : opener;
    }

    public static void openKnowledge(ItemStack stack) {
        knowledgeOpener.accept(stack.copy());
    }

    public static void openTutorial() {
        tutorialOpener.run();
    }
}
