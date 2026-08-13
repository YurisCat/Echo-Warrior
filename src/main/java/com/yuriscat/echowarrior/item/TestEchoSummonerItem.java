package com.yuriscat.echowarrior.item;

import com.yuriscat.echowarrior.ModEntities;
import com.yuriscat.echowarrior.menu.SummonerMenu;
import com.yuriscat.echowarrior.entity.RomanLegionaryEchoEntity;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
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
import net.minecraft.world.entity.EquipmentSlot;
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
import java.util.List;
import java.util.UUID;

public final class TestEchoSummonerItem extends Item {
	private static final String SUMMONER_ID = "EchoWarriorSummonerId";
	private static final String SPIRIT_ID = "EchoWarriorSpiritId";

	public TestEchoSummonerItem(Properties properties) {
		super(properties);
	}

	@Override
	public Component getName(ItemStack stack) {
		return relicStack(stack).getItem() instanceof EchoRelicItem
				? Component.literal("罗马军团兵召唤器")
				: Component.literal("英灵之魂召唤器");
	}

	@Override
	public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, EquipmentSlot slot) {
		if (!(entity instanceof ServerPlayer player) || player.containerMenu instanceof SummonerMenu menu && menu.matchesSummoner(getSummonerId(stack).orElse(null))) {
			return;
		}
		if (player.tickCount % 5 != 0) {
			return;
		}
		SimpleContainer contents = new SimpleContainer(SummonerMenu.CUSTOM_SLOT_COUNT);
		stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(contents.getItems());
		ItemStack input = contents.getItem(SummonerMenu.FUEL_SLOT);
		int value = SummonerFuel.value(input);
		if (value <= 0 || SummonerFuel.amount(stack) + value > SummonerFuel.CAPACITY) {
			return;
		}
		input.shrink(1);
		SummonerFuel.setAmount(stack, SummonerFuel.amount(stack) + value);
		stack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(contents.getItems()));
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (!(level instanceof ServerLevel serverLevel)) {
			return InteractionResult.SUCCESS;
		}

		RomanLegionaryEchoEntity current = findBoundSpirit(serverLevel, stack);
		if (player.isShiftKeyDown()) {
			return openMenu(player, hand, stack);
		}

		if (current != null && player.getUUID().equals(current.getOwnerUuid())) {
			current.recallTo(player);
			return InteractionResult.SUCCESS;
		}

		return summon(serverLevel, player, stack) == SummonResult.SUMMONED
				? InteractionResult.SUCCESS
				: InteractionResult.FAIL;
	}

	private static InteractionResult openMenu(Player player, InteractionHand hand, ItemStack stack) {
		if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.FAIL;
		getOrCreateSummonerId(stack);
		int sourceSlot = hand == InteractionHand.MAIN_HAND
				? player.getInventory().getSelectedSlot()
				: Inventory.SLOT_OFFHAND;
		serverPlayer.openMenu(new ExtendedMenuProvider<Integer>() {
			@Override
			public Integer getScreenOpeningData(ServerPlayer openingPlayer) {
				return sourceSlot;
			}

			@Override
			public Component getDisplayName() {
				return Component.literal("英灵之魂召唤器");
			}

			@Override
			public SummonerMenu createMenu(int containerId, Inventory inventory, Player openingPlayer) {
				return new SummonerMenu(containerId, inventory, sourceSlot, stack);
			}
		});
		return InteractionResult.SUCCESS;
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
		ItemStack relic = relicStack(stack);
		if (!(relic.getItem() instanceof EchoRelicItem)) {
			return SummonResult.NO_RELIC;
		}
		EchoRelicState.ensureInitialized(relic, level.getRandom(), level.getGameTime());
		int summonCost = SummonerFuel.summonCost(relic);
		if (SummonerFuel.amount(stack) < summonCost) {
			return SummonResult.NOT_ENOUGH_FUEL;
		}

		UUID summonerId = getOrCreateSummonerId(stack);
		RomanLegionaryEchoEntity spirit = ModEntities.ROMAN_LEGIONARY_ECHO.create(level, EntitySpawnReason.SPAWN_ITEM_USE);
		if (spirit == null) {
			return SummonResult.CREATE_FAILED;
		}

		Vec3 spawnPosition = findSafeSummonPosition(level, player, spirit);
		if (spawnPosition == null) {
			return SummonResult.NO_SAFE_POSITION;
		}
		double spawnX = spawnPosition.x;
		double spawnZ = spawnPosition.z;
		float facingYaw = RomanLegionaryEchoEntity.yawToward(spawnX, spawnZ, player.getX(), player.getZ());
		spirit.snapTo(spawnX, spawnPosition.y, spawnZ, facingYaw, 0.0F);
		spirit.setYBodyRot(facingYaw);
		spirit.setYHeadRot(facingYaw);
		spirit.bindTo(player, summonerId);
		spirit.applyRelicState(relic, true);
		if (!level.addFreshEntity(spirit)) {
			return SummonResult.CREATE_FAILED;
		}
		SummonerFuel.consume(stack, summonCost);
		setSpiritId(stack, spirit.getUUID());
		level.sendParticles(ParticleTypes.SOUL, spirit.getX(), spirit.getY() + 1.0, spirit.getZ(), 24, 0.35, 0.7, 0.35, 0.02);
		level.playSound(null, spirit.blockPosition(), SoundEvents.SOUL_ESCAPE.value(), SoundSource.PLAYERS, 0.8F, 1.15F);
		return SummonResult.SUMMONED;
	}

	private static Vec3 findSafeSummonPosition(ServerLevel level, Player player, RomanLegionaryEchoEntity spirit) {
		Vec3 forward = player.getLookAngle().multiply(2.0, 0.0, 2.0);
		List<Vec3> candidates = new java.util.ArrayList<>();
		candidates.add(new Vec3(player.getX() + forward.x, player.getY(), player.getZ() + forward.z));
		for (int radius = 1; radius <= 4; radius++) {
			for (int dx = -radius; dx <= radius; dx++) {
				for (int dz = -radius; dz <= radius; dz++) {
					if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
					candidates.add(new Vec3(player.getX() + dx + 0.5, player.getY(), player.getZ() + dz + 0.5));
				}
			}
		}
		for (Vec3 candidate : candidates) {
			for (int dy : new int[] {0, 1, -1}) {
				Vec3 position = candidate.add(0.0, dy, 0.0);
				BlockPos feet = BlockPos.containing(position);
				BlockPos floor = feet.below();
				if (!level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP)
						|| level.getFluidState(feet).is(FluidTags.LAVA)) {
					continue;
				}
				spirit.snapTo(position);
				if (level.noCollision(spirit)) {
					return position;
				}
			}
		}
		return null;
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

	public static void setRelicStack(ItemStack summoner, ItemStack relic) {
		if (!(summoner.getItem() instanceof TestEchoSummonerItem)) return;
		SimpleContainer contents = new SimpleContainer(SummonerMenu.CUSTOM_SLOT_COUNT);
		summoner.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(contents.getItems());
		contents.setItem(SummonerMenu.RELIC_SLOT, relic);
		summoner.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(contents.getItems()));
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
		CREATE_FAILED,
		NO_RELIC,
		NOT_ENOUGH_FUEL,
		NO_SAFE_POSITION
	}
}
