package com.yuriscat.echowarrior.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.yuriscat.echowarrior.entity.GuandaoWarriorEchoEntity;
import com.yuriscat.echowarrior.entity.JapaneseSamuraiEchoEntity;
import com.yuriscat.echowarrior.entity.RomanLegionaryEchoEntity;
import com.yuriscat.echowarrior.item.EchoRelicItem;
import com.yuriscat.echowarrior.item.EchoRelicState;
import com.yuriscat.echowarrior.item.TestEchoSummonerItem;
import com.yuriscat.echowarrior.world.BattlefieldSavedData;
import com.yuriscat.echowarrior.world.BattlefieldSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

import java.util.Comparator;
import java.util.Set;

public final class VisualDebugCommands {
	private VisualDebugCommands() {
	}

	public static void initialize() {
		CommandRegistrationCallback.EVENT.register((dispatcher, context, selection) -> register(dispatcher));
	}

	private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		var battlefieldCommands = Commands.literal("battlefield")
				.then(Commands.literal("locate")
						.executes(context -> locateBattlefield(context.getSource())))
				.then(Commands.literal("teleport")
						.executes(context -> teleportToBattlefield(context.getSource())))
				.then(Commands.literal("generate")
						.executes(context -> generateBattlefields(context.getSource(), 1))
						.then(Commands.argument("count", IntegerArgumentType.integer(1, 8))
								.executes(context -> generateBattlefields(context.getSource(),
										IntegerArgumentType.getInteger(context, "count")))));
		dispatcher.register(Commands.literal("echo_warrior")
				.requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
				.then(Commands.literal("visual")
						.then(Commands.literal("status").executes(context -> executeStatus(context.getSource())))
						.then(Commands.literal("samurai_afterimage_neutral")
								.then(Commands.literal("on")
										.executes(context -> setSamuraiAfterimageNeutral(context.getSource(), true)))
								.then(Commands.literal("off")
										.executes(context -> setSamuraiAfterimageNeutral(context.getSource(), false))))
						.then(Commands.literal("samurai_afterimage_advanced")
								.then(Commands.literal("on")
										.executes(context -> setSamuraiAfterimageAdvanced(context.getSource(), true)))
								.then(Commands.literal("off")
										.executes(context -> setSamuraiAfterimageAdvanced(context.getSource(), false))))
						.then(Commands.literal("samurai_afterimage_outline")
								.then(Commands.literal("on")
										.executes(context -> setSamuraiAfterimageOutline(context.getSource(), true)))
								.then(Commands.literal("off")
										.executes(context -> setSamuraiAfterimageOutline(context.getSource(), false))))
						.then(visualCommand("blink", RomanLegionaryEchoEntity.VisualTestMode.BLINK))
						.then(visualCommand("double_blink", RomanLegionaryEchoEntity.VisualTestMode.DOUBLE_BLINK))
						.then(visualCommand("curious", RomanLegionaryEchoEntity.VisualTestMode.CURIOUS))
						.then(visualCommand("startled", RomanLegionaryEchoEntity.VisualTestMode.STARTLED))
						.then(visualCommand("exit_look", RomanLegionaryEchoEntity.VisualTestMode.EXIT_LOOK))
						.then(visualCommand("exit_turn", RomanLegionaryEchoEntity.VisualTestMode.EXIT_TURN))
						.then(visualCommand("exit_walk", RomanLegionaryEchoEntity.VisualTestMode.EXIT_WALK))
						.then(visualCommand("exit_secondary", RomanLegionaryEchoEntity.VisualTestMode.EXIT_SECONDARY))
						.then(visualCommand("reset", RomanLegionaryEchoEntity.VisualTestMode.RESET)))
				.then(Commands.literal("animation")
						.then(animationCommand("attack", RomanLegionaryEchoEntity.AnimationTestMode.ATTACK))
						.then(animationCommand("hurt", RomanLegionaryEchoEntity.AnimationTestMode.HURT))
						.then(animationCommand("shield_raise", RomanLegionaryEchoEntity.AnimationTestMode.SHIELD_RAISE))
						.then(animationCommand("shield_lower", RomanLegionaryEchoEntity.AnimationTestMode.SHIELD_LOWER))
						.then(animationCommand("reset", RomanLegionaryEchoEntity.AnimationTestMode.RESET)))
				.then(Commands.literal("animation_debug")
						.then(Commands.literal("guandao")
								.then(Commands.literal("on")
										.executes(context -> setGuandaoAnimationDebug(context.getSource(), true)))
								.then(Commands.literal("off")
										.executes(context -> setGuandaoAnimationDebug(context.getSource(), false)))))
				.then(Commands.literal("relic")
						.then(Commands.literal("reroll_traits").executes(context -> rerollTraits(context.getSource()))))
				.then(battlefieldCommands));
	}

	private static int locateBattlefield(CommandSourceStack source) {
		ServerPlayer player = source.getPlayer();
		if (player == null) {
			source.sendFailure(Component.literal("该指令必须由玩家执行。"));
			return 0;
		}
		ServerLevel overworld = source.getServer().getLevel(Level.OVERWORLD);
		if (overworld == null) {
			source.sendFailure(Component.literal("当前服务器没有可用的主世界。"));
			return 0;
		}
		BattlefieldSavedData data = BattlefieldSavedData.get(overworld);
		BattlefieldSavedData.ActiveSite site = data.nearestKnownActive(player.blockPosition());
		if (site == null) return reportNoActiveBattlefield(source, data);

		long distance = Math.round(Math.sqrt(horizontalDistanceSqr(player.blockPosition(), site.center())));
		source.sendSuccess(() -> Component.literal("最近的未完成战场遗迹：中心 "
				+ coordinates(site.center()) + "，保底方块 " + coordinates(site.relic())
				+ "，水平距离约 " + distance + " 格。"), false);
		return 1;
	}

	private static int teleportToBattlefield(CommandSourceStack source) {
		ServerPlayer player = source.getPlayer();
		if (player == null) {
			source.sendFailure(Component.literal("该指令必须由玩家执行。"));
			return 0;
		}
		ServerLevel overworld = source.getServer().getLevel(Level.OVERWORLD);
		if (overworld == null) {
			source.sendFailure(Component.literal("当前服务器没有可用的主世界。"));
			return 0;
		}
		BattlefieldSavedData data = BattlefieldSavedData.get(overworld);
		BattlefieldSavedData.ActiveSite site = data.nearestKnownActive(player.blockPosition());
		if (site == null) return reportNoActiveBattlefield(source, data);

		BlockPos destination = findSafeTeleportDestination(overworld, player, site.center());
		if (destination == null) {
			source.sendFailure(Component.literal("遗迹中心 32～50 格范围内没有找到安全落点；未执行传送。"));
			return 0;
		}
		boolean success = player.teleportTo(overworld,
				destination.getX() + 0.5, destination.getY(), destination.getZ() + 0.5,
				Set.of(), player.getYRot(), player.getXRot(), true);
		if (!success) {
			source.sendFailure(Component.literal("无法传送到战场遗迹。"));
			return 0;
		}
		source.sendSuccess(() -> Component.literal("已传送到遗迹中心 " + coordinates(site.center())
				+ " 外 32～50 格的安全位置 " + coordinates(destination) + "，可测试罗盘指针。"), false);
		return 1;
	}

	private static int generateBattlefields(CommandSourceStack source, int count) {
		ServerPlayer player = source.getPlayer();
		if (player == null) {
			source.sendFailure(Component.literal("该指令必须由玩家执行。"));
			return 0;
		}
		ServerLevel overworld = source.getServer().getLevel(Level.OVERWORLD);
		if (overworld == null) {
			source.sendFailure(Component.literal("当前服务器没有可用的主世界。"));
			return 0;
		}
		if (!BattlefieldSystem.requestForceGeneration(overworld, player, count, 2048)) {
			source.sendFailure(Component.literal("你已有一个战场遗迹生成任务正在执行，请等待它完成。"));
			return 0;
		}
		source.sendSuccess(() -> Component.literal("已开始在 2048 格内分批寻找安全区域并生成 " + count
				+ " 处战场遗迹。任务会逐步报告进度，继续遵守正式区域与安全检查，也不会永久强加载区块。"), false);
		return 1;
	}

	private static BlockPos findSafeTeleportDestination(
			ServerLevel level,
			ServerPlayer player,
			BlockPos center
	) {
		for (int attempt = 0; attempt < 64; attempt++) {
			double angle = player.getRandom().nextDouble() * Math.PI * 2.0;
			double radius = 32.0 + player.getRandom().nextDouble() * 18.0;
			int x = (int)Math.floor(center.getX() + Math.cos(angle) * radius);
			int z = (int)Math.floor(center.getZ() + Math.sin(angle) * radius);
			level.getChunk(Math.floorDiv(x, 16), Math.floorDiv(z, 16));
			int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
			BlockPos feet = new BlockPos(x, y, z);
			if (!level.getWorldBorder().isWithinBounds(feet)) continue;
			BlockPos floor = feet.below();
			BlockState floorState = level.getBlockState(floor);
			BlockState feetState = level.getBlockState(feet);
			BlockState headState = level.getBlockState(feet.above());
			if (!floorState.getFluidState().isEmpty() || floorState.is(BlockTags.LEAVES)
					|| floorState.is(BlockTags.LOGS) || !floorState.isFaceSturdy(level, floor, Direction.UP)
					|| !feetState.getFluidState().isEmpty() || !headState.getFluidState().isEmpty()
					|| !feetState.getCollisionShape(level, feet).isEmpty()
					|| !headState.getCollisionShape(level, feet.above()).isEmpty()) continue;
			double dx = feet.getX() + 0.5 - player.getX();
			double dy = feet.getY() - player.getY();
			double dz = feet.getZ() + 0.5 - player.getZ();
			AABB destinationBox = player.getBoundingBox().move(dx, dy, dz);
			if (level.noCollision(player, destinationBox)) return feet;
		}
		return null;
	}

	private static int reportNoActiveBattlefield(CommandSourceStack source, BattlefieldSavedData data) {
		int waiting = data.count(BattlefieldSavedData.Status.WAITING);
		int cooldown = data.count(BattlefieldSavedData.Status.COOLDOWN);
		source.sendFailure(Component.literal("当前索引中没有已生成且未完成的战场遗迹；等待生成区域 "
				+ waiting + " 个，冷却中区域 " + cooldown + " 个。请等待自然加载区块中的生成尝试。"));
		return 0;
	}

	private static String coordinates(BlockPos pos) {
		return pos.getX() + " " + pos.getY() + " " + pos.getZ();
	}

	private static double horizontalDistanceSqr(BlockPos first, BlockPos second) {
		double dx = first.getX() - second.getX();
		double dz = first.getZ() - second.getZ();
		return dx * dx + dz * dz;
	}

	private static int rerollTraits(CommandSourceStack source) {
		ServerPlayer player = source.getPlayer();
		if (player == null) return 0;
		ItemStack held = player.getMainHandItem();
		ItemStack relic = held.getItem() instanceof EchoRelicItem ? held : TestEchoSummonerItem.relicStack(held);
		if (!(relic.getItem() instanceof EchoRelicItem)) {
			source.sendFailure(Component.literal("主手需要拿着英灵遗物，或装有遗物的召唤器。"));
			return 0;
		}
		int mask = EchoRelicState.rerollTraits(relic, player.getRandom(), player.level().getGameTime());
		if (held.getItem() instanceof TestEchoSummonerItem) TestEchoSummonerItem.setRelicStack(held, relic);
		source.sendSuccess(() -> Component.literal("已重新随机遗物天赋，掩码=" + mask), false);
		return 1;
	}

	private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> visualCommand(
			String name,
			RomanLegionaryEchoEntity.VisualTestMode mode
	) {
		return Commands.literal(name).executes(context -> execute(context.getSource(), mode));
	}

	private static int execute(CommandSourceStack source, RomanLegionaryEchoEntity.VisualTestMode mode) {
		ServerPlayer player = source.getPlayer();
		if (player == null) {
			source.sendFailure(Component.literal("This command must be used by a player."));
			return 0;
		}

		RomanLegionaryEchoEntity echo = findNearestOwnedEcho(player);
		if (echo == null) {
			source.sendFailure(Component.literal("No owned Echo Warrior was found within 32 blocks."));
			return 0;
		}

		echo.forceVisualState(mode);
		source.sendSuccess(() -> Component.literal("Forced visual state " + mode.name().toLowerCase() + " on the nearest echo."), false);
		return 1;
	}

	private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> animationCommand(
			String name,
			RomanLegionaryEchoEntity.AnimationTestMode mode
	) {
		return Commands.literal(name).executes(context -> executeAnimation(context.getSource(), mode));
	}

	private static int executeAnimation(CommandSourceStack source, RomanLegionaryEchoEntity.AnimationTestMode mode) {
		ServerPlayer player = source.getPlayer();
		if (player == null) {
			source.sendFailure(Component.literal("This command must be used by a player."));
			return 0;
		}

		RomanLegionaryEchoEntity echo = findNearestOwnedEcho(player);
		if (echo == null) {
			source.sendFailure(Component.literal("No owned Echo Warrior was found within 32 blocks."));
			return 0;
		}

		echo.forceAnimationState(mode);
		source.sendSuccess(() -> Component.literal("Forced animation " + mode.name().toLowerCase() + " on the nearest echo."), false);
		return 1;
	}

	private static int executeStatus(CommandSourceStack source) {
		ServerPlayer player = source.getPlayer();
		if (player == null) {
			source.sendFailure(Component.literal("This command must be used by a player."));
			return 0;
		}

		RomanLegionaryEchoEntity echo = findNearestOwnedEcho(player);
		if (echo == null) {
			source.sendFailure(Component.literal("No owned Echo Warrior was found within 32 blocks."));
			return 0;
		}

		source.sendSuccess(() -> Component.literal(echo.describeGazeDebug(player)), false);
		return 1;
	}

	private static int setGuandaoAnimationDebug(CommandSourceStack source, boolean enabled) {
		ServerPlayer player = source.getPlayer();
		if (player == null) {
			source.sendFailure(Component.literal("This command must be used by a player."));
			return 0;
		}

		GuandaoWarriorEchoEntity echo = findNearestOwnedGuandao(player);
		if (echo == null) {
			source.sendFailure(Component.literal("32 格内没有属于你的关刀甲胄战士。"));
			return 0;
		}

		echo.setAnimationDebugEnabled(enabled);
		source.sendSuccess(() -> Component.literal("关刀动画诊断已" + (enabled ? "开启" : "关闭") + "。"), false);
		return 1;
	}

	private static int setSamuraiAfterimageNeutral(CommandSourceStack source, boolean neutral) {
		ServerPlayer player = source.getPlayer();
		if (player == null) {
			source.sendFailure(Component.literal("This command must be used by a player."));
			return 0;
		}

		JapaneseSamuraiEchoEntity echo = findNearestOwnedSamurai(player);
		if (echo == null) {
			source.sendFailure(Component.literal("32 格内没有属于你的日本武士。"));
			return 0;
		}

		echo.setAfterimageNeutral(neutral);
		source.sendSuccess(() -> Component.literal(neutral
				? "武士闪避残影已关闭额外附色，使用原始贴图。"
				: "武士闪避残影已开启残心青色／踏込金色附色。"), false);
		return 1;
	}

	private static int setSamuraiAfterimageAdvanced(CommandSourceStack source, boolean enabled) {
		ServerPlayer player = source.getPlayer();
		if (player == null) {
			source.sendFailure(Component.literal("This command must be used by a player."));
			return 0;
		}

		JapaneseSamuraiEchoEntity echo = findNearestOwnedSamurai(player);
		if (echo == null) {
			source.sendFailure(Component.literal("32 格内没有属于你的日本武士。"));
			return 0;
		}

		echo.setAfterimageAdvanced(enabled);
		source.sendSuccess(() -> Component.literal(enabled
				? "武士残影 2A 贴图溶解效果已开启。"
				: "武士残影 2A 已关闭；2B 外轮廓也已同步关闭。"), false);
		return 1;
	}

	private static int setSamuraiAfterimageOutline(CommandSourceStack source, boolean enabled) {
		ServerPlayer player = source.getPlayer();
		if (player == null) {
			source.sendFailure(Component.literal("This command must be used by a player."));
			return 0;
		}

		JapaneseSamuraiEchoEntity echo = findNearestOwnedSamurai(player);
		if (echo == null) {
			source.sendFailure(Component.literal("32 格内没有属于你的日本武士。"));
			return 0;
		}
		if (enabled) {
			echo.setAfterimageOutline(false);
			source.sendFailure(Component.literal("武士残影 2B 已暂时停用：当前 GeckoLib 通道会把描边错误渲染成白色剪影。"));
			return 0;
		}

		echo.setAfterimageOutline(false);
		source.sendSuccess(() -> Component.literal("武士残影 2B 外轮廓保持关闭。"), false);
		return 1;
	}

	private static RomanLegionaryEchoEntity findNearestOwnedEcho(ServerPlayer player) {
		return player.level()
				.getEntitiesOfClass(
						RomanLegionaryEchoEntity.class,
						player.getBoundingBox().inflate(32.0),
						entity -> player.getUUID().equals(entity.getOwnerUuid())
				)
				.stream()
				.min(Comparator.comparingDouble(player::distanceToSqr))
				.orElse(null);
	}

	private static GuandaoWarriorEchoEntity findNearestOwnedGuandao(ServerPlayer player) {
		return player.level()
				.getEntitiesOfClass(
						GuandaoWarriorEchoEntity.class,
						player.getBoundingBox().inflate(32.0),
						entity -> player.getUUID().equals(entity.getOwnerUuid())
				)
				.stream()
				.min(Comparator.comparingDouble(player::distanceToSqr))
				.orElse(null);
	}

	private static JapaneseSamuraiEchoEntity findNearestOwnedSamurai(ServerPlayer player) {
		return player.level()
				.getEntitiesOfClass(
						JapaneseSamuraiEchoEntity.class,
						player.getBoundingBox().inflate(32.0),
						entity -> player.getUUID().equals(entity.getOwnerUuid())
				)
				.stream()
				.min(Comparator.comparingDouble(player::distanceToSqr))
				.orElse(null);
	}
}
