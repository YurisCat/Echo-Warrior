package com.yuriscat.echowarrior.item;

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
	private static final int USE_TICKS = 32;
	private final LegacyType type;

	public LegacyItem(Properties properties, LegacyType type) {
		super(properties);
		this.type = type;
	}

	public LegacyType type() {
		return this.type;
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
		builder.accept(Component.translatable("item.echo_warrior.legacy." + this.type.id + ".effect"));
		builder.accept(Component.translatable(this.type == LegacyType.CRAFT
				? "item.echo_warrior.legacy.craft_hint"
				: "item.echo_warrior.legacy.hold_hint"));
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
		COURAGE("courage"),
		FORTITUDE("fortitude"),
		PURITY("purity"),
		WISDOM("wisdom"),
		CRAFT("craft");

		private final String id;

		LegacyType(String id) {
			this.id = id;
		}
	}
}
