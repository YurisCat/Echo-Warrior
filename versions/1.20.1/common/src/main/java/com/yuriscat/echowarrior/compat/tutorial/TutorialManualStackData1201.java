package com.yuriscat.echowarrior.compat.tutorial;

import com.yuriscat.echowarrior.compat.ModContent1201;
import net.minecraft.world.item.ItemStack;

public final class TutorialManualStackData1201 {
    private static net.minecraft.nbt.CompoundTag readTag(ItemStack stack) {
        return stack.hasTag() ? stack.getTag().copy() : new net.minecraft.nbt.CompoundTag();
    }
    private static void updateTag(ItemStack stack, java.util.function.Consumer<net.minecraft.nbt.CompoundTag> update) {
        var tag = readTag(stack);
        update.accept(tag);
        stack.setTag(tag);
    }

    private static final String BOOKMARK = "EchoWarriorTutorialPage";

    private TutorialManualStackData1201() {
    }

    public static int bookmark(ItemStack stack) {
        if (!stack.is(ModContent1201.TUTORIAL_MANUAL)) return 0;
        int page = readTag(stack).getInt(BOOKMARK);
        return net.minecraft.util.Mth.clamp(page, 0, TutorialManualCatalog1201.pageCount() - 1);
    }

    public static void setBookmark(ItemStack stack, int page) {
        if (!stack.is(ModContent1201.TUTORIAL_MANUAL)) return;
        int normalized = net.minecraft.util.Mth.clamp(page, 0, TutorialManualCatalog1201.pageCount() - 1);
        updateTag(stack, tag -> tag.putInt(BOOKMARK, normalized));
    }

    /** The renderer calls this only for the old and new snapshots of one hand slot. */
    public static boolean isSamePhysicalManual(ItemStack first, ItemStack second) {
        return first.is(ModContent1201.TUTORIAL_MANUAL) && second.is(ModContent1201.TUTORIAL_MANUAL);
    }
}
