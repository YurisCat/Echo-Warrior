package com.yuriscat.echowarrior.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.yuriscat.echowarrior.binding.EchoBindingSavedData;
import com.yuriscat.echowarrior.binding.EchoBindingSystem;
import com.yuriscat.echowarrior.item.EchoHeroType;
import com.yuriscat.echowarrior.item.EchoRelicItem;
import com.yuriscat.echowarrior.item.TestEchoSummonerItem;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/** Operator diagnostics and conservative recovery for persistent summoner bindings. */
public final class EchoBindingCommands {
	private EchoBindingCommands() {
	}

	public static LiteralArgumentBuilder<CommandSourceStack> command() {
		return Commands.literal("binding")
				.then(Commands.literal("list").executes(context -> list(context.getSource())))
				.then(Commands.literal("status")
						.then(Commands.argument("summoner_uuid", StringArgumentType.word())
								.executes(context -> status(context.getSource(),
										StringArgumentType.getString(context, "summoner_uuid")))))
				.then(Commands.literal("dismiss")
						.then(Commands.argument("summoner_uuid", StringArgumentType.word())
								.executes(context -> dismiss(context.getSource(),
										StringArgumentType.getString(context, "summoner_uuid")))))
				.then(Commands.literal("repair")
						.then(Commands.argument("summoner_uuid", StringArgumentType.word())
								.executes(context -> repair(context.getSource(),
										StringArgumentType.getString(context, "summoner_uuid")))));
	}

	private static int list(CommandSourceStack source) {
		int total = 0;
		int active = 0;
		int shown = 0;
		for (EchoBindingSavedData.Binding binding : EchoBindingSavedData.get(source.getServer()).bindings()) {
			total++;
			if (!binding.active()) continue;
			active++;
			if (shown++ >= 20) continue;
			source.sendSuccess(() -> shortStatus(binding), false);
		}
		int finalTotal = total;
		int finalActive = active;
		source.sendSuccess(() -> Component.literal("英灵绑定：活跃 " + finalActive + " / 总计 " + finalTotal
				+ (finalActive > 20 ? "（仅列出前 20 条）" : "")), false);
		return active;
	}

	private static int status(CommandSourceStack source, String rawUuid) {
		UUID id = parse(source, rawUuid);
		if (id == null) return 0;
		EchoBindingSavedData.Binding binding = EchoBindingSavedData.get(source.getServer()).get(id);
		if (binding == null) {
			source.sendFailure(Component.literal("未找到召唤器绑定：" + id));
			return 0;
		}
		EchoBindingSavedData.Snapshot snapshot = binding.snapshot();
		source.sendSuccess(() -> shortStatus(binding), false);
		source.sendSuccess(() -> Component.literal("实体=" + value(binding.entityId())
				+ " 维度=" + (snapshot.dimension().isEmpty() ? "未知" : snapshot.dimension())
				+ " 生命=" + snapshot.health() + " 位置="
				+ String.format(java.util.Locale.ROOT, "%.1f %.1f %.1f", snapshot.x(), snapshot.y(), snapshot.z())), false);
		source.sendSuccess(() -> Component.literal("generation=" + binding.generation()
				+ " revision=" + binding.revision() + " stateRevision=" + binding.stateRevision()), false);
		return 1;
	}

	private static int dismiss(CommandSourceStack source, String rawUuid) {
		UUID id = parse(source, rawUuid);
		if (id == null) return 0;
		if (!EchoBindingSystem.dismiss(source.getServer(), id, "operator_command")) {
			source.sendFailure(Component.literal("该绑定不存在或已经不活跃：" + id));
			return 0;
		}
		source.sendSuccess(() -> Component.literal("已遣散英灵绑定：" + id), true);
		return 1;
	}

	private static int repair(CommandSourceStack source, String rawUuid) {
		UUID id = parse(source, rawUuid);
		if (id == null) return 0;
		if (!EchoBindingSystem.forceReconstruct(source.getServer(), id)) {
			source.sendFailure(Component.literal("无法重建：绑定必须活跃、处于跟随状态，且控制者在线并可行动。"));
			return 0;
		}
		source.sendSuccess(() -> Component.literal("已重建跟随英灵：" + id), true);
		return 1;
	}

	private static Component shortStatus(EchoBindingSavedData.Binding binding) {
		ItemStack relic = TestEchoSummonerItem.relicStack(binding.summonerState());
		Component hero = relic.getItem() instanceof EchoRelicItem
				? Component.translatable(EchoHeroType.fromRelic(relic).nameTranslationKey())
				: Component.translatable("command.echo_warrior.binding.no_relic");
		return Component.empty()
				.append(binding.summonerId().toString())
				.append(" [")
				.append(hero)
				.append("] active=" + binding.active() + " controller=" + value(binding.controllerId()));
	}

	private static UUID parse(CommandSourceStack source, String rawUuid) {
		try {
			return UUID.fromString(rawUuid);
		} catch (IllegalArgumentException ignored) {
			source.sendFailure(Component.literal("无效 UUID：" + rawUuid));
			return null;
		}
	}

	private static String value(UUID uuid) {
		return uuid == null ? "无" : uuid.toString();
	}
}
