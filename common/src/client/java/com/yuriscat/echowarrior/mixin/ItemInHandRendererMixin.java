package com.yuriscat.echowarrior.mixin;

import com.yuriscat.echowarrior.item.TestEchoSummonerItem;
import com.yuriscat.echowarrior.knowledge.KnowledgeStackData;
import com.yuriscat.echowarrior.tutorial.TutorialManualStackData;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {
	@Inject(method = "shouldInstantlyReplaceVisibleItem", at = @At("HEAD"), cancellable = true)
	private void echoWarrior$keepSummonerSteady(
			ItemStack currentlyVisibleItem,
			ItemStack expectedItem,
			CallbackInfoReturnable<Boolean> callback
	) {
		if (TutorialManualStackData.isSamePhysicalManual(currentlyVisibleItem, expectedItem)
				|| KnowledgeStackData.isSamePhysicalCollection(currentlyVisibleItem, expectedItem)) {
			callback.setReturnValue(true);
			return;
		}
		if (!(currentlyVisibleItem.getItem() instanceof TestEchoSummonerItem)
				|| !(expectedItem.getItem() instanceof TestEchoSummonerItem)) return;
		var currentId = TestEchoSummonerItem.getSummonerId(currentlyVisibleItem);
		var expectedId = TestEchoSummonerItem.getSummonerId(expectedItem);
		if (currentId.isPresent() && currentId.equals(expectedId)) callback.setReturnValue(true);
	}
}
