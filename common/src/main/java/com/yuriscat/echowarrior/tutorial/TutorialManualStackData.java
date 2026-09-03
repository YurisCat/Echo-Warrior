package com.yuriscat.echowarrior.tutorial;

import com.yuriscat.echowarrior.ModItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class TutorialManualStackData {
	private static final String BOOKMARK = "EchoWarriorTutorialPage";

	private TutorialManualStackData() {
	}

	public static int bookmark(ItemStack stack) {
		if (!stack.is(ModItems.TUTORIAL_MANUAL)) return 0;
		int page = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
				.copyTag().getIntOr(BOOKMARK, 0);
		return Math.clamp(page, 0, TutorialManualCatalog.pageCount() - 1);
	}

	public static void setBookmark(ItemStack stack, int page) {
		if (!stack.is(ModItems.TUTORIAL_MANUAL)) return;
		int normalized = Math.clamp(page, 0, TutorialManualCatalog.pageCount() - 1);
		CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt(BOOKMARK, normalized));
	}
}
