package com.yuriscat.echowarrior.compat.item;

import com.yuriscat.echowarrior.compat.knowledge.KnowledgeCatalog1211;
import com.yuriscat.echowarrior.compat.knowledge.KnowledgeStackData1211;
import com.yuriscat.echowarrior.compat.menu.KnowledgeReaderMenu1211;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public final class KnowledgeFragmentItem1211 extends Item {
    public KnowledgeFragmentItem1211(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return KnowledgeReaderMenu1211.open(player, hand);
    }

    @Override
    public Component getName(ItemStack stack) {
        return KnowledgeTooltip1211.knowledgeName("item.echo_warrior.knowledge_fragment");
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        KnowledgeStackData1211.fragmentId(stack).flatMap(KnowledgeCatalog1211::entry).ifPresent(entry -> tooltip.add(
                Component.translatable(KnowledgeCatalog1211.cultureTranslationKey(entry.culture()))
                        .append(" · ").append(Component.translatable(entry.titleKey()))
                        .withStyle(ChatFormatting.GRAY)));
        tooltip.add(Component.translatable("item.echo_warrior.knowledge_fragment.read_hint").withStyle(ChatFormatting.DARK_GRAY));
        KnowledgeTooltip1211.appendFragmentDetails(tooltip);
    }
}
