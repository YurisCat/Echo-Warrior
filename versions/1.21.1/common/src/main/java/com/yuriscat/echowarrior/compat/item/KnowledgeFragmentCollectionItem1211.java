package com.yuriscat.echowarrior.compat.item;

import com.yuriscat.echowarrior.compat.ModContent1211;
import com.yuriscat.echowarrior.compat.knowledge.KnowledgeStackData1211;
import com.yuriscat.echowarrior.compat.menu.KnowledgeReaderMenu1211;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.LinkedHashMap;
import java.util.List;

public final class KnowledgeFragmentCollectionItem1211 extends Item {
    public KnowledgeFragmentCollectionItem1211(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return KnowledgeReaderMenu1211.open(player, hand);
    }

    @Override
    public Component getName(ItemStack stack) {
        return super.getName(stack).copy().withStyle(style -> style.withColor(0xFFEDC9));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.echo_warrior.knowledge_fragment_collection.summary",
                KnowledgeStackData1211.uniqueCount(stack), KnowledgeStackData1211.totalCount(stack))
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.echo_warrior.knowledge_fragment.read_hint").withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public boolean overrideOtherStackedOnMe(ItemStack self, ItemStack other, Slot slot, ClickAction action,
                                              Player player, SlotAccess carriedItem) {
        if (action != ClickAction.PRIMARY || other.isEmpty() || !other.is(ModContent1211.KNOWLEDGE_FRAGMENT)) return false;
        String id = KnowledgeStackData1211.fragmentId(other).orElse("");
        if (!slot.allowModification(player) || id.isEmpty()) {
            player.playSound(SoundEvents.DISPENSER_FAIL, 0.7F, 1.0F);
            return true;
        }
        LinkedHashMap<String, Integer> counts = KnowledgeStackData1211.collectionCounts(self);
        KnowledgeStackData1211.merge(counts, id, other.getCount());
        KnowledgeStackData1211.writeCollection(self, counts, KnowledgeStackData1211.bookmark(self));
        other.setCount(0);
        player.playSound(SoundEvents.BUNDLE_INSERT, 0.8F, 1.0F);
        if (player.containerMenu != null) player.containerMenu.broadcastChanges();
        return true;
    }
}
