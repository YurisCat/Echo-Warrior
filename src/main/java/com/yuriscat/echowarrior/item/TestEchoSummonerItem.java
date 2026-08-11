package com.yuriscat.echowarrior.item;

import com.yuriscat.echowarrior.ModEntities;
import com.yuriscat.echowarrior.menu.SummonerMenu;
import com.yuriscat.echowarrior.entity.RomanLegionaryEchoEntity;
import com.yuriscat.echowarrior.progress.EchoExperienceSystem;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
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

		RomanLegionaryEchoEntity current = findBoundSpirit(serverLevel, stack);
		if (player.isShiftKeyDown()) {
			if (!(player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)) {
				return InteractionResult.FAIL;
			}
			int sourceSlot = hand == InteractionHand.MAIN_HAND
					? player.getInventory().getSelectedSlot()
					: Inventory.SLOT_OFFHAND;
			serverPlayer.openMenu(new ExtendedMenuProvider<Integer>() {
				@Override
				public Integer getScreenOpeningData(net.minecraft.server.level.ServerPlayer openingPlayer) {
					return sourceSlot;
				}

				@Override
				public Component getDisplayName() {
					return Component.literal("罗马军团兵召唤器");
				}

				@Override
				public SummonerMenu createMenu(int containerId, Inventory inventory, Player openingPlayer) {
					return new SummonerMenu(containerId, inventory, sourceSlot, stack);
				}
			});
			return InteractionResult.SUCCESS;
		}

		if (current != null && player.getUUID().equals(current.getOwnerUuid())) {
			current.recallTo(player);
			return InteractionResult.SUCCESS;
		}

		return summon(serverLevel, player, stack) == SummonResult.SUMMONED
				? InteractionResult.SUCCESS
				: InteractionResult.FAIL;
	}

	public static SummonResult summonFromMenu(ServerPlayer player, ItemStack stack) {
		return summon(player.level(), player, stack);
	}

	private static SummonResult summon(ServerLevel level, Player player, ItemStack stack) {
		if (!(stack.getItem() instanceof TestEchoSummonerItem)) {
			return SummonResult.INVALID_SUMMONER;
		}
		RomanLegionaryEchoEntity current = findBoundSpirit(level, stack);
		if (current != null) {
			return SummonResult.ALREADY_PRESENT;
		}

		UUID summonerId = getOrCreateSummonerId(stack);
		RomanLegionaryEchoEntity spirit = ModEntities.ROMAN_LEGIONARY_ECHO.create(level, EntitySpawnReason.SPAWN_ITEM_USE);
		if (spirit == null) {
			return SummonResult.CREATE_FAILED;
		}

		Vec3 forward = player.getLookAngle().multiply(2.0, 0.0, 2.0);
		double spawnX = player.getX() + forward.x;
		double spawnZ = player.getZ() + forward.z;
		float facingYaw = RomanLegionaryEchoEntity.yawToward(spawnX, spawnZ, player.getX(), player.getZ());
		spirit.snapTo(spawnX, player.getY(), spawnZ, facingYaw, 0.0F);
		spirit.setYBodyRot(facingYaw);
		spirit.setYHeadRot(facingYaw);
		spirit.bindTo(player, summonerId);
		EchoExperienceSystem.applyRelicProgress(spirit, relicStack(stack), false);
		if (!level.addFreshEntity(spirit)) {
			return SummonResult.CREATE_FAILED;
		}
		setSpiritId(stack, spirit.getUUID());
		level.sendParticles(ParticleTypes.SOUL, spirit.getX(), spirit.getY() + 1.0, spirit.getZ(), 24, 0.35, 0.7, 0.35, 0.02);
		level.playSound(null, spirit.blockPosition(), SoundEvents.SOUL_ESCAPE.value(), SoundSource.PLAYERS, 0.8F, 1.15F);
		return SummonResult.SUMMONED;
	}

	public static boolean dismissBoundSpirit(ServerPlayer player, ItemStack stack) {
		RomanLegionaryEchoEntity spirit = findBoundSpirit(player.level(), stack);
		if (spirit == null || !player.getUUID().equals(spirit.getOwnerUuid())) {
			clearSpiritId(stack);
			return false;
		}
		spirit.dismiss();
		clearSpiritId(stack);
		return true;
	}

	public static boolean hasBoundSpirit(ServerLevel level, ItemStack stack) {
		return findBoundSpirit(level, stack) != null;
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

	public static ItemStack findSummonerStack(Player player, UUID summonerId) {
		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			ItemStack candidate = player.getInventory().getItem(slot);
			if (hasSummoner(candidate, summonerId)) {
				return candidate;
			}
		}
		return ItemStack.EMPTY;
	}

	public static ItemStack relicStack(ItemStack summoner) {
		if (!(summoner.getItem() instanceof TestEchoSummonerItem)) {
			return ItemStack.EMPTY;
		}
		SimpleContainer contents = new SimpleContainer(SummonerMenu.CUSTOM_SLOT_COUNT);
		summoner.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY)
				.copyInto(contents.getItems());
		return contents.getItem(SummonerMenu.RELIC_SLOT);
	}

	public static Optional<UUID> getSummonerId(ItemStack stack) {
		if (!(stack.getItem() instanceof TestEchoSummonerItem)) {
			return Optional.empty();
		}
		String value = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
				.copyTag()
				.getStringOr(SUMMONER_ID, "");
		try {
			return Optional.of(UUID.fromString(value));
		} catch (IllegalArgumentException ignored) {
			return Optional.empty();
		}
	}

	public static RomanLegionaryEchoEntity findBoundSpirit(ServerLevel level, ItemStack stack) {
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

	public enum SummonResult {
		SUMMONED,
		ALREADY_PRESENT,
		INVALID_SUMMONER,
		CREATE_FAILED
	}
}
