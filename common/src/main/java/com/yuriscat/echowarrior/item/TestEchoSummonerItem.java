package com.yuriscat.echowarrior.item;

import com.yuriscat.echowarrior.ModEntities;
import com.yuriscat.echowarrior.binding.EchoBindingSavedData;
import com.yuriscat.echowarrior.binding.EchoBindingSystem;
import com.yuriscat.echowarrior.menu.SummonerMenu;
import com.yuriscat.echowarrior.entity.EchoWarriorEntity;
import com.yuriscat.echowarrior.platform.PlatformServices;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.ChatFormatting;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public final class TestEchoSummonerItem extends Item {
	private static final String SUMMONER_ID = "EchoWarriorSummonerId";
	private static final String SPIRIT_ID = "EchoWarriorSpiritId";
	private static final int CONTROL_HINT_COLOR = 0x82999B;

	public TestEchoSummonerItem(Properties properties) {
		super(properties);
	}

	@Override
	public Component getName(ItemStack stack) {
		ItemStack relic = relicStack(stack);
		return relic.getItem() instanceof EchoRelicItem
				? Component.translatable(
						"item.echo_warrior.test_echo_summoner.bound",
						Component.translatable(EchoHeroType.fromRelic(relic).nameTranslationKey())
				)
				: Component.translatable(this.getDescriptionId());
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
			Consumer<Component> builder, TooltipFlag flag) {
		Component relic = highlightedTerm("item.echo_warrior.test_echo_summoner.tooltip.term.relic");
		Component echo = plainTerm("item.echo_warrior.test_echo_summoner.tooltip.term.echo");
		builder.accept(Component.translatable(
				"item.echo_warrior.test_echo_summoner.tooltip.summary",
				relic,
				echo
		).withStyle(ChatFormatting.GRAY));

		if (!TooltipShiftState.isShiftDown()) {
			builder.accept(Component.translatable("item.echo_warrior.test_echo_summoner.tooltip.more_hint")
					.withStyle(ChatFormatting.DARK_GRAY));
			return;
		}

		Component fuel = plainTerm("item.echo_warrior.test_echo_summoner.tooltip.term.fuel");
		Component quickAction = Component.translatable(
				"item.echo_warrior.test_echo_summoner.tooltip.term.quick_action"
		).withStyle(style -> style.withColor(CONTROL_HINT_COLOR));
		Component inventory = plainTerm("item.echo_warrior.test_echo_summoner.tooltip.term.inventory");
		builder.accept(detailLine("item.echo_warrior.test_echo_summoner.tooltip.detail.healing", echo, fuel));
		builder.accept(detailLine("item.echo_warrior.test_echo_summoner.tooltip.detail.quick_action", quickAction));
		builder.accept(detailLine("item.echo_warrior.test_echo_summoner.tooltip.detail.direct_insert", inventory));
	}

	private static Component detailLine(String translationKey, Component... arguments) {
		return Component.literal("+").withStyle(ChatFormatting.GRAY)
				.append(Component.translatable(translationKey, (Object[]) arguments)
						.withStyle(ChatFormatting.GRAY));
	}

	private static Component highlightedTerm(String translationKey) {
		return Component.translatable(translationKey)
				.withStyle(style -> style.withColor(KnowledgeTooltip.KNOWLEDGE_COLOR));
	}

	private static Component plainTerm(String translationKey) {
		return Component.translatable(translationKey).withStyle(ChatFormatting.GRAY);
	}

	@Override
	public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, EquipmentSlot slot) {
		if (!(entity instanceof ServerPlayer player)) {
			return;
		}
		EchoBindingSystem.registerOrSynchronize(level, stack);
		if (player.containerMenu instanceof SummonerMenu menu && menu.matchesSummoner(getSummonerId(stack).orElse(null))) return;
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
		EchoBindingSystem.commitPhysicalStack(level, stack);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (!(level instanceof ServerLevel serverLevel)) {
			return InteractionResult.SUCCESS;
		}

		if (!player.isShiftKeyDown()) {
			return openMenu(player, hand, stack);
		}

		EchoBindingSystem.registerOrSynchronize(serverLevel, stack);
		EchoWarriorEntity current = findBoundSpirit(serverLevel, stack);
		if (current != null && current.livingEntity().level() == player.level()
				&& player.getUUID().equals(current.getOwnerUuid())) {
			current.recallTo(player);
			return InteractionResult.SUCCESS;
		}

		return summon(serverLevel, player, stack) == SummonResult.SUMMONED
				? InteractionResult.SUCCESS
				: InteractionResult.FAIL;
	}

	@Override
	public boolean overrideOtherStackedOnMe(
			ItemStack self,
			ItemStack other,
			Slot slot,
			ClickAction clickAction,
			Player player,
			SlotAccess carriedItem
	) {
		if (clickAction != ClickAction.PRIMARY || other.isEmpty()) return false;
		SummonerMenu openMenu = player.containerMenu instanceof SummonerMenu menu
				&& menu.allowsDirectInsertionIntoSource(slot, player)
				? menu
				: null;
		if (!slot.allowModification(player) && openMenu == null) {
			playInsertionSound(player, false);
			return true;
		}
		getOrCreateSummonerId(self);
		if (openMenu == null && player.level() instanceof ServerLevel serverLevel) {
			// A summoner just removed from an unloaded container may carry a stale mirror.
			// Pull authority before applying a direct insertion so it cannot overwrite live state.
			EchoBindingSystem.registerOrSynchronize(serverLevel, self);
		}
		boolean insertedFuel = SummonerFuel.isFuel(other);
		ItemStack fuelForFeedback = insertedFuel ? other.copyWithCount(1) : ItemStack.EMPTY;
		boolean insertedAccessory = EchoSummonerAccessory.isAccessory(other);
		boolean insertedRelic = other.getItem() instanceof EchoRelicItem;
		boolean inserted = openMenu == null
				? insertIntoInternalSlot(self, other)
				: openMenu.insertIntoOpenSummoner(other);
		if (inserted && player.level().isClientSide()) {
			if (insertedFuel) {
				SummonerFuelInsertFeedback.playFuel(slot, fuelForFeedback);
			} else if (insertedAccessory || insertedRelic) {
				SummonerFuelInsertFeedback.playPolish(slot);
			}
		}
		if (inserted && insertedAccessory && openMenu == null && player.level() instanceof ServerLevel serverLevel) {
			EchoBindingSystem.commitPhysicalStack(serverLevel, self);
			EchoWarriorEntity spirit = findBoundSpirit(serverLevel, self);
			if (spirit != null) spirit.applyModuleState();
		} else if (inserted && openMenu == null && player.level() instanceof ServerLevel serverLevel) {
			EchoBindingSystem.commitPhysicalStack(serverLevel, self);
		}
		playInsertionSound(player, inserted);
		if (player.containerMenu != null) player.containerMenu.broadcastChanges();
		return true;
	}

	private static boolean insertIntoInternalSlot(ItemStack summoner, ItemStack carried) {
		SimpleContainer contents = new SimpleContainer(SummonerMenu.CUSTOM_SLOT_COUNT);
		summoner.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(contents.getItems());
		int inserted = 0;
		if (SummonerFuel.isFuel(carried)) {
			ItemStack stored = contents.getItem(SummonerMenu.FUEL_SLOT);
			if (stored.isEmpty()) {
				inserted = Math.min(carried.getCount(), carried.getMaxStackSize());
				contents.setItem(SummonerMenu.FUEL_SLOT, carried.copyWithCount(inserted));
			} else if (ItemStack.isSameItemSameComponents(stored, carried)) {
				inserted = Math.min(carried.getCount(), stored.getMaxStackSize() - stored.getCount());
				if (inserted > 0) stored.grow(inserted);
			}
		} else if (carried.getItem() instanceof EchoRelicItem) {
			if (contents.getItem(SummonerMenu.RELIC_SLOT).isEmpty()) {
				inserted = 1;
				contents.setItem(SummonerMenu.RELIC_SLOT, carried.copyWithCount(1));
			}
		} else if (EchoSummonerAccessory.isAccessory(carried)) {
			for (int moduleSlot = 0; moduleSlot < SummonerMenu.MODULE_SLOT_COUNT; moduleSlot++) {
				if (!contents.getItem(moduleSlot).isEmpty()
						|| !EchoSummonerAccessory.canInstall(carried, summoner, moduleSlot, contents)) continue;
				inserted = 1;
				contents.setItem(moduleSlot, carried.copyWithCount(1));
				break;
			}
		}
		if (inserted <= 0) return false;
		carried.shrink(inserted);
		summoner.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(contents.getItems()));
		return true;
	}

	private static void playInsertionSound(Player player, boolean success) {
		player.playSound(success ? SoundEvents.BUNDLE_INSERT : SoundEvents.BUNDLE_INSERT_FAIL,
				success ? 0.8F : 1.0F,
				success ? 0.8F + player.level().getRandom().nextFloat() * 0.4F : 1.0F);
	}

	private static InteractionResult openMenu(Player player, InteractionHand hand, ItemStack stack) {
		if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.FAIL;
		getOrCreateSummonerId(stack);
		EchoBindingSystem.registerOrSynchronize(serverPlayer.level(), stack);
		int sourceSlot = hand == InteractionHand.MAIN_HAND
				? player.getInventory().getSelectedSlot()
				: Inventory.SLOT_OFFHAND;
		PlatformServices.openIntMenu(serverPlayer, stack.getHoverName(),
				(containerId, inventory) -> new SummonerMenu(containerId, inventory, sourceSlot, stack), sourceSlot);
		return InteractionResult.SUCCESS;
	}

	public static SummonResult summonFromMenu(ServerPlayer player, ItemStack stack) {
		return summon(player.level(), player, stack);
	}

	private static SummonResult summon(ServerLevel level, Player player, ItemStack stack) {
		if (!(stack.getItem() instanceof TestEchoSummonerItem)) {
			return SummonResult.INVALID_SUMMONER;
		}
		EchoBindingSavedData.Binding binding = EchoBindingSystem.registerOrSynchronize(level, stack);
		if (binding.active()) {
			return SummonResult.ALREADY_PRESENT;
		}
		if (!(player instanceof ServerPlayer serverPlayer)
				|| !EchoBindingSystem.canAddControllerEcho(level.getServer(), player.getUUID(), binding.summonerId())) {
			return SummonResult.LIMIT_REACHED;
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
		EchoWarriorEntity spirit = createSpirit(level, relic);
		if (spirit == null) {
			return SummonResult.CREATE_FAILED;
		}

		Vec3 spawnPosition = findSafeSummonPosition(level, player, spirit);
		if (spawnPosition == null) {
			return SummonResult.NO_SAFE_POSITION;
		}
		double spawnX = spawnPosition.x;
		double spawnZ = spawnPosition.z;
		LivingEntity spiritEntity = spirit.livingEntity();
		float facingYaw = yawToward(spawnX, spawnZ, player.getX(), player.getZ());
		spiritEntity.snapTo(spawnX, spawnPosition.y, spawnZ, facingYaw, 0.0F);
		spiritEntity.setYBodyRot(facingYaw);
		spiritEntity.setYHeadRot(facingYaw);
		spirit.bindTo(player, summonerId);
		long generation = EchoBindingSystem.activate(level, stack, serverPlayer, spiritEntity);
		spirit.setBindingGeneration(generation);
		spirit.applyRelicState(relic, true);
		spiritEntity.setHealth(spiritEntity.getMaxHealth());
		if (!level.addFreshEntity(spiritEntity)) {
			EchoBindingSystem.dismiss(level.getServer(), summonerId, "spawn_failed");
			return SummonResult.CREATE_FAILED;
		}
		if (!EchoBindingSystem.consumeFuel(level, summonerId, summonCost)) {
			EchoBindingSystem.dismiss(level.getServer(), summonerId, "fuel_commit_failed");
			return SummonResult.NOT_ENOUGH_FUEL;
		}
		setSpiritId(stack, spiritEntity.getUUID());
		EchoBindingSystem.noteNewSummon(serverPlayer);
		level.sendParticles(ParticleTypes.SOUL, spiritEntity.getX(), spiritEntity.getY() + 1.0, spiritEntity.getZ(), 24, 0.35, 0.7, 0.35, 0.02);
		level.playSound(null, spiritEntity.blockPosition(), SoundEvents.SOUL_ESCAPE.value(), SoundSource.PLAYERS, 0.8F, 1.15F);
		return SummonResult.SUMMONED;
	}

	public static EchoWarriorEntity createSpirit(ServerLevel level, ItemStack relic) {
		if (!(relic.getItem() instanceof EchoRelicItem)) return null;
		return switch (EchoHeroType.fromRelic(relic)) {
			case ROMAN_LEGIONARY -> ModEntities.ROMAN_LEGIONARY_ECHO.create(level, EntitySpawnReason.SPAWN_ITEM_USE);
			case AZTEC_WARRIOR -> ModEntities.AZTEC_WARRIOR_ECHO.create(level, EntitySpawnReason.SPAWN_ITEM_USE);
			case EGYPTIAN_ARCHER -> ModEntities.EGYPTIAN_ARCHER_ECHO.create(level, EntitySpawnReason.SPAWN_ITEM_USE);
			case GUANDAO_WARRIOR -> ModEntities.GUANDAO_WARRIOR_ECHO.create(level, EntitySpawnReason.SPAWN_ITEM_USE);
			case JAPANESE_SAMURAI -> ModEntities.JAPANESE_SAMURAI_ECHO.create(level, EntitySpawnReason.SPAWN_ITEM_USE);
		};
	}

	public static Vec3 findSafeSummonPosition(ServerLevel level, Player player, EchoWarriorEntity spirit) {
		LivingEntity spiritEntity = spirit.livingEntity();
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
				spiritEntity.snapTo(position);
				if (level.noCollision(spiritEntity)) {
					return position;
				}
			}
		}
		return null;
	}

	public static boolean dismissBoundSpirit(ServerPlayer player, ItemStack stack) {
		UUID summonerId = getSummonerId(stack).orElse(null);
		if (summonerId == null || !EchoBindingSystem.dismiss(player.level().getServer(), summonerId, "manual")) return false;
		clearSpiritId(stack);
		return true;
	}

	public static boolean hasBoundSpirit(ServerLevel level, ItemStack stack) {
		UUID summonerId = getSummonerId(stack).orElse(null);
		return summonerId != null && EchoBindingSystem.isActive(level, summonerId);
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

	public static UUID replaceSummonerIdForDuplicate(ItemStack stack) {
		UUID id = UUID.randomUUID();
		CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
			tag.putString(SUMMONER_ID, id.toString());
			tag.remove(SPIRIT_ID);
		});
		return id;
	}

	public static boolean hasSummoner(ItemStack stack, UUID summonerId) {
		if (stack.isEmpty() || !(stack.getItem() instanceof TestEchoSummonerItem)) {
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

	public static List<ItemStack> accessoryStacks(ItemStack summoner) {
		if (!(summoner.getItem() instanceof TestEchoSummonerItem)) return List.of();
		SimpleContainer contents = new SimpleContainer(SummonerMenu.CUSTOM_SLOT_COUNT);
		summoner.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY)
				.copyInto(contents.getItems());
		return contents.getItems().subList(0, SummonerMenu.MODULE_SLOT_COUNT).stream()
				.filter(stack -> !stack.isEmpty())
				.map(ItemStack::copy)
				.toList();
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

	public static EchoWarriorEntity findBoundSpirit(ServerLevel level, ItemStack stack) {
		UUID summonerId = getSummonerId(stack).orElse(null);
		return summonerId == null ? null : EchoBindingSystem.findLoadedSpirit(level.getServer(), summonerId);
	}

	public static void commitCreativeInventoryUpdate(ServerPlayer player, ItemStack stack) {
		if (!(stack.getItem() instanceof TestEchoSummonerItem)) return;
		EchoBindingSystem.commitPhysicalStack(player.level(), stack);
		EchoWarriorEntity spirit = findBoundSpirit(player.level(), stack);
		if (spirit != null) spirit.applyModuleState();
	}

	public static boolean reconstructFromBinding(ServerPlayer controller, EchoBindingSavedData.Binding binding) {
		if (!binding.active() || !controller.getUUID().equals(binding.controllerId())) return false;
		ServerLevel level = controller.level();
		ItemStack relic = relicStack(binding.summonerState());
		EchoWarriorEntity spirit = createSpirit(level, relic);
		if (spirit == null) return false;
		Vec3 position = findSafeSummonPosition(level, controller, spirit);
		if (position == null) return false;
		LivingEntity entity = spirit.livingEntity();
		float yaw = yawToward(position.x, position.z, controller.getX(), controller.getZ());
		entity.snapTo(position.x, position.y, position.z, yaw, 0.0F);
		entity.setYBodyRot(yaw);
		entity.setYHeadRot(yaw);
		spirit.bindTo(controller, binding.summonerId());
		spirit.setBindingGeneration(binding.generation());
		spirit.readMigrationState(binding.snapshot().migrationState());
		spirit.applyRelicState(relic, true);
		EchoBindingSavedData.Snapshot snapshot = binding.snapshot();
		entity.setHealth(snapshot.health() > 0.0F
				? Math.min(entity.getMaxHealth(), snapshot.health()) : entity.getMaxHealth());
		entity.setAbsorptionAmount(Math.max(0.0F, snapshot.absorption()));
		entity.setRemainingFireTicks(Math.max(0, snapshot.remainingFireTicks()));
		entity.setTicksFrozen(Math.max(0, snapshot.ticksFrozen()));
		entity.setAirSupply(snapshot.airSupply());
		if (!level.addFreshEntity(entity)) return false;
		EchoBindingSystem.attachReconstructedEntity(controller, binding, entity);
		level.sendParticles(ParticleTypes.SOUL, entity.getX(), entity.getY() + 1.0, entity.getZ(),
				16, 0.3, 0.6, 0.3, 0.01);
		return true;
	}

	private static void setSpiritId(ItemStack stack, UUID spiritId) {
		CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putString(SPIRIT_ID, spiritId.toString()));
	}

	private static void clearSpiritId(ItemStack stack) {
		CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.remove(SPIRIT_ID));
	}

	private static float yawToward(double fromX, double fromZ, double toX, double toZ) {
		return (float)(Math.atan2(toZ - fromZ, toX - fromX) * 180.0 / Math.PI) - 90.0F;
	}

	public enum SummonResult {
		SUMMONED,
		ALREADY_PRESENT,
		INVALID_SUMMONER,
		CREATE_FAILED,
		NO_RELIC,
		NOT_ENOUGH_FUEL,
		NO_SAFE_POSITION,
		LIMIT_REACHED
	}
}
