package com.yuriscat.echowarrior.item;

import com.yuriscat.echowarrior.knowledge.KnowledgeStackData;
import com.yuriscat.echowarrior.menu.KnowledgeReaderMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.LinkedHashMap;
import java.util.function.Consumer;

public final class KnowledgeFragmentCollectionItem extends Item {
	public KnowledgeFragmentCollectionItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		return KnowledgeReaderMenu.open(player, hand);
	}

	@Override
	public Component getName(ItemStack stack) {
		return KnowledgeTooltip.knowledgeName("item.echo_warrior.knowledge_fragment_collection");
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
			Consumer<Component> builder, TooltipFlag flag) {
		builder.accept(Component.translatable(
				"item.echo_warrior.knowledge_fragment_collection.summary",
				KnowledgeStackData.uniqueCount(stack),
				KnowledgeStackData.totalCount(stack)
		).withStyle(ChatFormatting.GRAY));
		builder.accept(Component.translatable("item.echo_warrior.knowledge_fragment.read_hint")
				.withStyle(ChatFormatting.DARK_GRAY));
	}

	@Override
	public boolean overrideOtherStackedOnMe(ItemStack self, ItemStack other, Slot slot, ClickAction clickAction,
			Player player, SlotAccess carriedItem) {
		if (clickAction != ClickAction.PRIMARY || other.isEmpty() || !other.is(com.yuriscat.echowarrior.ModItems.KNOWLEDGE_FRAGMENT)) {
			return false;
		}
		if (!slot.allowModification(player)) {
			player.playSound(SoundEvents.BUNDLE_INSERT_FAIL, 1.0F, 1.0F);
			return true;
		}
		String id = KnowledgeStackData.fragmentId(other).orElse("");
		if (id.isEmpty()) {
			player.playSound(SoundEvents.BUNDLE_INSERT_FAIL, 1.0F, 1.0F);
			return true;
		}
		LinkedHashMap<String, Integer> counts = KnowledgeStackData.collectionCounts(self);
		KnowledgeStackData.merge(counts, id, other.getCount());
		KnowledgeStackData.writeCollection(self, counts, KnowledgeStackData.bookmark(self));
		other.setCount(0);
		player.playSound(SoundEvents.BUNDLE_INSERT, 0.8F, 0.8F + player.level().getRandom().nextFloat() * 0.4F);
		if (player.containerMenu != null) player.containerMenu.broadcastChanges();
		return true;
	}
}
