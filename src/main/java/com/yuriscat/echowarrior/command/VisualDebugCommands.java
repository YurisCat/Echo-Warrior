package com.yuriscat.echowarrior.command;

import com.mojang.brigadier.CommandDispatcher;
import com.yuriscat.echowarrior.entity.RomanLegionaryEchoEntity;
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
						.then(visualCommand("blink", RomanLegionaryEchoEntity.VisualTestMode.BLINK))
						.then(visualCommand("double_blink", RomanLegionaryEchoEntity.VisualTestMode.DOUBLE_BLINK))
						.then(visualCommand("curious", RomanLegionaryEchoEntity.VisualTestMode.CURIOUS))
						.then(visualCommand("startled", RomanLegionaryEchoEntity.VisualTestMode.STARTLED))
						.then(visualCommand("reset", RomanLegionaryEchoEntity.VisualTestMode.RESET))));
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

		RomanLegionaryEchoEntity echo = player.level()
				.getEntitiesOfClass(
						RomanLegionaryEchoEntity.class,
						player.getBoundingBox().inflate(32.0),
						entity -> player.getUUID().equals(entity.getOwnerUuid())
				)
				.stream()
				.min(Comparator.comparingDouble(player::distanceToSqr))
				.orElse(null);
		if (echo == null) {
			source.sendFailure(Component.literal("No owned Echo Warrior was found within 32 blocks."));
			return 0;
		}

		echo.forceVisualState(mode);
		source.sendSuccess(() -> Component.literal("Forced visual state " + mode.name().toLowerCase() + " on the nearest echo."), false);
		return 1;
	}
}
