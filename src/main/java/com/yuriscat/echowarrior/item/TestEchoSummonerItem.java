package com.yuriscat.echowarrior.item;

import com.yuriscat.echowarrior.ModEntities;
import com.yuriscat.echowarrior.entity.RomanLegionaryEchoEntity;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public final class TestEchoSummonerItem extends Item {
	private static final String SUMMONER_ID = "EchoWarriorSummonerId";
	private static final String SPIRIT_ID = "EchoWarriorSpiritId";

	public TestEchoSummonerItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (!(level instanceof ServerLevel serverLevel)) {
			return InteractionResult.SUCCESS;
		}

		UUID summonerId = getOrCreateSummonerId(stack);
		RomanLegionaryEchoEntity current = findBoundSpirit(serverLevel, stack);
		if (player.isShiftKeyDown()) {
			if (current != null) {
				current.dismiss();
			}
			clearSpiritId(stack);
			return InteractionResult.SUCCESS;
		}

		if (current != null && player.getUUID().equals(current.getOwnerUuid())) {
			current.recallTo(player);
			return InteractionResult.SUCCESS;
		}

		if (current != null) {
			current.dismiss();
		}

		RomanLegionaryEchoEntity spirit = ModEntities.ROMAN_LEGIONARY_ECHO.create(serverLevel, EntitySpawnReason.SPAWN_ITEM_USE);
		if (spirit == null) {
			return InteractionResult.FAIL;
		}

		Vec3 forward = player.getLookAngle().multiply(2.0, 0.0, 2.0);
		spirit.snapTo(player.getX() + forward.x, player.getY(), player.getZ() + forward.z, player.getYRot(), 0.0F);
		spirit.bindTo(player, summonerId);
		serverLevel.addFreshEntity(spirit);
		setSpiritId(stack, spirit.getUUID());
		serverLevel.sendParticles(ParticleTypes.SOUL, spirit.getX(), spirit.getY() + 1.0, spirit.getZ(), 24, 0.35, 0.7, 0.35, 0.02);
		serverLevel.playSound(null, spirit.blockPosition(), SoundEvents.SOUL_ESCAPE.value(), SoundSource.PLAYERS, 0.8F, 1.15F);
		return InteractionResult.SUCCESS;
	}

	public static UUID getOrCreateSummonerId(ItemStack stack) {
		CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
		String value = data.copyTag().getStringOr(SUMMONER_ID, "");
		try {
			return UUID.fromString(value);
		} catch (IllegalArgumentException ignored) {
			UUID id = UUID.randomUUID();
			CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putString(SUMMONER_ID, id.toString()));
			return id;
		}
	}

	public static boolean hasSummoner(ItemStack stack, UUID summonerId) {
		if (!(stack.getItem() instanceof TestEchoSummonerItem)) {
			return false;
		}
		String value = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getStringOr(SUMMONER_ID, "");
		return summonerId.toString().equals(value);
	}

	private static RomanLegionaryEchoEntity findBoundSpirit(ServerLevel level, ItemStack stack) {
		String value = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getStringOr(SPIRIT_ID, "");
		try {
			Entity entity = level.getEntity(UUID.fromString(value));
			return entity instanceof RomanLegionaryEchoEntity spirit && spirit.isAlive() ? spirit : null;
		} catch (IllegalArgumentException ignored) {
			return null;
		}
	}

	private static void setSpiritId(ItemStack stack, UUID spiritId) {
		CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putString(SPIRIT_ID, spiritId.toString()));
	}

	private static void clearSpiritId(ItemStack stack) {
		CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.remove(SPIRIT_ID));
	}
}
