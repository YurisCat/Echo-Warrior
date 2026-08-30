package com.yuriscat.echowarrior.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.function.Consumer;

/** A hold-to-use inheritance item. Craft inheritance is recipe-only. */
public final class LegacyItem extends Item {
	private static final int USE_TICKS = 16;
	private static final int POSITIVE_COLOR = 0x93CE85;
	private static final int NEGATIVE_COLOR = 0xE46D6D;
	private static final int TERM_COLOR = KnowledgeTooltip.KNOWLEDGE_COLOR;
	private final LegacyType type;

	public LegacyItem(Properties properties, LegacyType type) {
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
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (this.type == LegacyType.CRAFT) return InteractionResult.PASS;
		player.startUsingItem(hand);
		return InteractionResult.CONSUME;
	}

	@Override
	public int getUseDuration(ItemStack stack, LivingEntity user) {
		return this.type == LegacyType.CRAFT ? 0 : USE_TICKS;
	}

	@Override
	public ItemUseAnimation getUseAnimation(ItemStack stack) {
		return this.type == LegacyType.CRAFT ? ItemUseAnimation.NONE : ItemUseAnimation.BOW;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
			Consumer<Component> builder, TooltipFlag flag) {
		String prefix = "item.echo_warrior.legacy." + this.type.id;
		builder.accept(detailLine(prefix + ".effect", coloredTerm(prefix + ".term", this.type.effectColor)));
		builder.accept(detailLine(
				"item.echo_warrior.legacy.accessory_material",
				coloredTerm("item.echo_warrior.legacy.term.accessory", TERM_COLOR)
		));
		if (!TooltipShiftState.isShiftDown()) {
			builder.accept(Component.translatable("item.echo_warrior.legacy.more_hint")
					.withStyle(ChatFormatting.DARK_GRAY));
			return;
		}

		builder.accept(Component.translatable("item.echo_warrior.legacy.lore.1")
				.withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
		builder.accept(Component.translatable("item.echo_warrior.legacy.lore.2")
				.withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
	}

	private static Component detailLine(String translationKey, Component... arguments) {
		return Component.literal("+").withStyle(ChatFormatting.GRAY)
				.append(Component.translatable(translationKey, (Object[]) arguments)
						.withStyle(ChatFormatting.GRAY));
	}

	private static Component coloredTerm(String translationKey, int color) {
		return Component.translatable(translationKey).withStyle(style -> style.withColor(color));
	}

	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
		if (!(level instanceof ServerLevel serverLevel) || !(user instanceof Player player)
				|| this.type == LegacyType.CRAFT) return stack;

		switch (this.type) {
			case COURAGE -> player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 20 * 60, 1));
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
