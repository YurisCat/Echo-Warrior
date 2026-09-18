package com.yuriscat.echowarrior.compat.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/** Minecraft 1.21.1 implementation of the five inheritance consumables. */
public final class LegacyItem1211 extends Item {
    private static final int USE_TICKS = 16;
    private static final int POSITIVE_COLOR = 0x93CE85;
    private static final int NEGATIVE_COLOR = 0xE46D6D;
    private static final int TERM_COLOR = 0xFFEDC9;
    private final LegacyType type;

    public LegacyItem1211(Properties properties, LegacyType type) {
        super(properties);
        this.type = type;
    }

    public LegacyType type() {
        return this.type;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable(this.getDescriptionId())
                .withStyle(style -> style.withColor(this.type.nameColor));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (this.type == LegacyType.CRAFT) return InteractionResultHolder.pass(stack);
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return this.type == LegacyType.CRAFT ? 0 : USE_TICKS;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return this.type == LegacyType.CRAFT ? UseAnim.NONE : UseAnim.BOW;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        String prefix = "item.echo_warrior.legacy." + this.type.id;
        tooltip.add(detailLine(prefix + ".effect", coloredTerm(prefix + ".term", this.type.effectColor)));
        tooltip.add(detailLine(
                "item.echo_warrior.legacy.accessory_material",
                coloredTerm("item.echo_warrior.legacy.term.accessory", TERM_COLOR)
        ));
        if (!TooltipShiftState1211.isShiftDown()) {
            tooltip.add(Component.translatable("item.echo_warrior.legacy.more_hint")
                    .withStyle(ChatFormatting.DARK_GRAY));
            return;
        }

        tooltip.add(Component.translatable("item.echo_warrior.legacy.lore.1")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        tooltip.add(Component.translatable("item.echo_warrior.legacy.lore.2")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
        if (!(level instanceof ServerLevel serverLevel) || !(user instanceof Player player)
                || this.type == LegacyType.CRAFT) return stack;

        switch (this.type) {
            case COURAGE -> player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 20 * 60, 1));
            case FORTITUDE -> player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 60, 1));
            case PURITY -> new ArrayList<>(player.getActiveEffects()).stream()
                    .filter(effect -> effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL)
                    .forEach(effect -> player.removeEffect(effect.getEffect()));
            case WISDOM -> player.giveExperiencePoints(100);
            case CRAFT -> { }
        }
        if (!player.getAbilities().instabuild) stack.shrink(1);
        serverLevel.sendParticles(ParticleTypes.ENCHANT,
                player.getX(), player.getY() + player.getBbHeight() * 0.6, player.getZ(),
                18, 0.35, 0.45, 0.35, 0.12);
        serverLevel.playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE,
                SoundSource.PLAYERS, 0.8F, 0.9F + serverLevel.getRandom().nextFloat() * 0.25F);
        return stack;
    }

    private static Component detailLine(String translationKey, Component... arguments) {
        return Component.literal("+").withStyle(ChatFormatting.GRAY)
                .append(Component.translatable(translationKey, (Object[])arguments).withStyle(ChatFormatting.GRAY));
    }

    private static Component coloredTerm(String translationKey, int color) {
        return Component.translatable(translationKey).withStyle(style -> style.withColor(color));
    }

    public enum LegacyType {
        COURAGE("courage", 0xB33A24, POSITIVE_COLOR),
        FORTITUDE("fortitude", 0x4F9E2F, POSITIVE_COLOR),
        PURITY("purity", 0xE3E3E3, NEGATIVE_COLOR),
        WISDOM("wisdom", 0x2A86C2, POSITIVE_COLOR),
        CRAFT("craft", 0xE0B72D, POSITIVE_COLOR);

        private final String id;
        private final int nameColor;
        private final int effectColor;

        LegacyType(String id, int nameColor, int effectColor) {
            this.id = id;
            this.nameColor = nameColor;
            this.effectColor = effectColor;
        }
    }
}
