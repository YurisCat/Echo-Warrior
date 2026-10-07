package com.yuriscat.echowarrior.compat.item;

import com.yuriscat.echowarrior.compat.EchoHeroType1201;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public final class EchoRelicItem1201 extends Item {
    private final EchoHeroType1201 heroType;

    public EchoRelicItem1201(Properties properties, EchoHeroType1201 heroType) {
        super(properties);
        this.heroType = heroType;
    }

    public EchoHeroType1201 heroType() {
        return this.heroType;
    }

    @Override
    public void inventoryTick(ItemStack stack, net.minecraft.world.level.Level level, Entity entity,
                              int slot, boolean selected) {
        if (level instanceof ServerLevel serverLevel) {
            EchoRelicState1201.ensureInitialized(stack, serverLevel.random, serverLevel.getGameTime());
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, net.minecraft.world.level.Level context, List<Component> tooltip, TooltipFlag flag) {
        int level = EchoRelicProgress1201.level(stack);
        tooltip.add(Component.translatable("tooltip.echo_warrior.relic.level",
                level, EchoRelicProgress1201.maxLevel()).withStyle(ChatFormatting.AQUA));
        if (level >= EchoRelicProgress1201.maxLevel()) {
            tooltip.add(Component.translatable("tooltip.echo_warrior.relic.experience.max")
                    .withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.add(Component.translatable("tooltip.echo_warrior.relic.experience",
                    EchoRelicProgress1201.experience(stack), EchoRelicProgress1201.experienceNeeded(level))
                    .withStyle(ChatFormatting.GRAY));
        }
        if (!EchoRelicState1201.initialized(stack)) {
            tooltip.add(Component.translatable("tooltip.echo_warrior.relic.talents.pending")
                    .withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        int mask = EchoRelicState1201.traitMask(stack);
        tooltip.add(Component.empty());
        if (mask == 0) {
            tooltip.add(Component.translatable("tooltip.echo_warrior.relic.talents.none")
                    .withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        tooltip.add(Component.translatable("tooltip.echo_warrior.relic.talents.header")
                .withStyle(ChatFormatting.GOLD));
        boolean expanded = TooltipShiftState1201.isShiftDown();
        for (EchoTrait1201 trait : EchoTrait1201.values()) {
            if ((mask & trait.mask()) == 0) continue;
            String key = trait == EchoTrait1201.BIOME_AFFINITY
                    ? EchoRelicState1201.biomeAffinity(stack).nameTranslationKey()
                    : trait.nameTranslationKey();
            tooltip.add(Component.translatable(key).withStyle(ChatFormatting.AQUA));
            if (expanded) tooltip.add(Component.translatable(trait.descriptionTranslationKey())
                    .withStyle(ChatFormatting.GRAY));
        }
        if (!expanded) tooltip.add(Component.translatable("tooltip.echo_warrior.relic.more_hint")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
