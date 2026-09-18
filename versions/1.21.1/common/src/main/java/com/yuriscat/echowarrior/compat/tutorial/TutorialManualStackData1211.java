package com.yuriscat.echowarrior.compat.tutorial;

import com.yuriscat.echowarrior.compat.ModContent1211;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class TutorialManualStackData1211 {
    private static final String BOOKMARK = "EchoWarriorTutorialPage";

    private TutorialManualStackData1211() {
    }

    public static int bookmark(ItemStack stack) {
        if (!stack.is(ModContent1211.TUTORIAL_MANUAL)) return 0;
        int page = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getInt(BOOKMARK);
        return Math.clamp(page, 0, TutorialManualCatalog1211.pageCount() - 1);
    }

    public static void setBookmark(ItemStack stack, int page) {
        if (!stack.is(ModContent1211.TUTORIAL_MANUAL)) return;
        int normalized = Math.clamp(page, 0, TutorialManualCatalog1211.pageCount() - 1);
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt(BOOKMARK, normalized));
    }

    /** The renderer calls this only for the old and new snapshots of one hand slot. */
    public static boolean isSamePhysicalManual(ItemStack first, ItemStack second) {
        return first.is(ModContent1211.TUTORIAL_MANUAL) && second.is(ModContent1211.TUTORIAL_MANUAL);
    }
}
