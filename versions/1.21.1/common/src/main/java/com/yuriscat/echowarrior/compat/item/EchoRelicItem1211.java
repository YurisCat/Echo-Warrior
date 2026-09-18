package com.yuriscat.echowarrior.compat.item;

import com.yuriscat.echowarrior.compat.EchoHeroType1211;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public final class EchoRelicItem1211 extends Item {
    private final EchoHeroType1211 heroType;

    public EchoRelicItem1211(Properties properties, EchoHeroType1211 heroType) {
        super(properties);
        this.heroType = heroType;
    }

    public EchoHeroType1211 heroType() {
        return this.heroType;
    }

    @Override
    public void inventoryTick(ItemStack stack, net.minecraft.world.level.Level level, Entity entity,
                              int slot, boolean selected) {
        if (level instanceof ServerLevel serverLevel) {
            EchoRelicState1211.ensureInitialized(stack, serverLevel.random);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        int level = EchoRelicProgress1211.level(stack);
        tooltip.add(Component.translatable("tooltip.echo_warrior.relic.level",
                level, EchoRelicProgress1211.MAX_LEVEL).withStyle(ChatFormatting.AQUA));
        if (level >= EchoRelicProgress1211.MAX_LEVEL) {
            tooltip.add(Component.translatable("tooltip.echo_warrior.relic.experience.max")
                    .withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.add(Component.translatable("tooltip.echo_warrior.relic.experience",
                    EchoRelicProgress1211.experience(stack), EchoRelicProgress1211.experienceNeeded(level))
                    .withStyle(ChatFormatting.GRAY));
        }
        if (!EchoRelicState1211.initialized(stack)) {
            tooltip.add(Component.translatable("tooltip.echo_warrior.relic.talents.pending")
                    .withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        int mask = EchoRelicState1211.traitMask(stack);
        tooltip.add(Component.empty());
        if (mask == 0) {
            tooltip.add(Component.translatable("tooltip.echo_warrior.relic.talents.none")
                    .withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        tooltip.add(Component.translatable("tooltip.echo_warrior.relic.talents.header")
                .withStyle(ChatFormatting.GOLD));
        boolean expanded = TooltipShiftState1211.isShiftDown();
        for (EchoTrait1211 trait : EchoTrait1211.values()) {
            if ((mask & trait.mask()) == 0) continue;
            String key = trait == EchoTrait1211.BIOME_AFFINITY
                    ? EchoRelicState1211.biomeAffinity(stack).nameTranslationKey()
                    : trait.nameTranslationKey();
            tooltip.add(Component.translatable(key).withStyle(ChatFormatting.AQUA));
            if (expanded) tooltip.add(Component.translatable(trait.descriptionTranslationKey())
                    .withStyle(ChatFormatting.GRAY));
        }
        if (!expanded) tooltip.add(Component.translatable("tooltip.echo_warrior.relic.more_hint")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
