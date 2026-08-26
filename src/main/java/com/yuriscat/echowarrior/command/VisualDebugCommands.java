package com.yuriscat.echowarrior.command;

import com.mojang.brigadier.CommandDispatcher;
import com.yuriscat.echowarrior.entity.GuandaoWarriorEchoEntity;
import com.yuriscat.echowarrior.entity.JapaneseSamuraiEchoEntity;
import com.yuriscat.echowarrior.entity.RomanLegionaryEchoEntity;
import com.yuriscat.echowarrior.item.EchoRelicItem;
import com.yuriscat.echowarrior.item.EchoRelicState;
import com.yuriscat.echowarrior.item.TestEchoSummonerItem;
import net.minecraft.world.item.ItemStack;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;

import java.util.Comparator;

public final class VisualDebugCommands {
	private VisualDebugCommands() {
	}

	public static void initialize() {
		CommandRegistrationCallback.EVENT.register((dispatcher, context, selection) -> register(dispatcher));
	}

	private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
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
						.then(Commands.literal("reroll_traits").executes(context -> rerollTraits(context.getSource())))));
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
				? "武士闪避残影已切换为原始贴图色调。"
				: "武士闪避残影已恢复残心青色／踏込金色。"), false);
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
				? "武士残影 2A 溶解与全亮效果已开启。"
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
