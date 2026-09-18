package com.yuriscat.echowarrior.compat.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.yuriscat.echowarrior.compat.block.entity.RecyclerChestBlockEntity1211;
import com.yuriscat.echowarrior.compat.world.BattlefieldSavedData1211;
import com.yuriscat.echowarrior.compat.world.BattlefieldSystem1211;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.Set;

/** Player-facing admin helpers used by the shared manual regression checklist. */
public final class GameplayTestCommands1211 {
    private GameplayTestCommands1211() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("echo_warrior")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("battlefield")
                        .then(Commands.literal("locate")
                                .executes(context -> locateBattlefield(context.getSource())))
                        .then(Commands.literal("teleport")
                                .executes(context -> teleportToBattlefield(context.getSource())))
                        .then(Commands.literal("generate")
                                .executes(context -> generateBattlefields(context.getSource(), 1))
                                .then(Commands.argument("count", IntegerArgumentType.integer(1, 8))
                                        .executes(context -> generateBattlefields(context.getSource(),
                                                IntegerArgumentType.getInteger(context, "count"))))))
                .then(Commands.literal("recycler")
                        .then(Commands.literal("trigger")
                                .executes(context -> triggerRecycler(context.getSource())))
                        .then(Commands.literal("effect")
                                .then(recyclerEffectCommand("normal", RecyclerChestBlockEntity1211.EffectTier.NORMAL))
                                .then(recyclerEffectCommand("rare", RecyclerChestBlockEntity1211.EffectTier.RARE))
                                .then(recyclerEffectCommand("super", RecyclerChestBlockEntity1211.EffectTier.SUPER))
                                .then(recyclerEffectCommand("failure", RecyclerChestBlockEntity1211.EffectTier.FAILURE))))
                .then(EchoBindingCommands1211.command()));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> recyclerEffectCommand(
            String name, RecyclerChestBlockEntity1211.EffectTier tier) {
        return Commands.literal(name).executes(context -> playRecyclerEffect(context.getSource(), tier));
    }

    private static int triggerRecycler(CommandSourceStack source) {
        RecyclerChestBlockEntity1211 recycler = lookedAtRecycler(source);
        ServerPlayer player = source.getPlayer();
        if (recycler == null || player == null) return 0;
        if (!recycler.triggerManual((ServerLevel)player.level())) {
            source.sendFailure(Component.literal("该回收箱正在封存，或其中没有可回收的物品。"));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("已立即尝试一次回收；这次测试不会占用自然午夜回收次数。"), false);
        return 1;
    }

    private static int playRecyclerEffect(CommandSourceStack source, RecyclerChestBlockEntity1211.EffectTier tier) {
        RecyclerChestBlockEntity1211 recycler = lookedAtRecycler(source);
        ServerPlayer player = source.getPlayer();
        if (recycler == null || player == null) return 0;
        recycler.playDebugEffect((ServerLevel)player.level(), tier);
        source.sendSuccess(() -> Component.literal("已播放回收箱 " + tier.name().toLowerCase() + " 特效。"), false);
        return 1;
    }

    private static RecyclerChestBlockEntity1211 lookedAtRecycler(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("该指令必须由玩家执行。"));
            return null;
        }
        HitResult hit = player.pick(8.0, 1.0F, false);
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK
                || !(player.level().getBlockEntity(blockHit.getBlockPos())
                instanceof RecyclerChestBlockEntity1211 recycler)) {
            source.sendFailure(Component.literal("请看向 8 格内的英灵杂物回收箱。"));
            return null;
        }
        return recycler;
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
        BattlefieldSavedData1211 data = BattlefieldSavedData1211.get(overworld);
        BattlefieldSavedData1211.ActiveSite site = data.nearestKnownActive(player.blockPosition());
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
        BattlefieldSavedData1211 data = BattlefieldSavedData1211.get(overworld);
        BattlefieldSavedData1211.ActiveSite site = data.nearestKnownActive(player.blockPosition());
        if (site == null) return reportNoActiveBattlefield(source, data);
        BlockPos destination = findSafeTeleportDestination(overworld, player, site.center());
        if (destination == null) {
            source.sendFailure(Component.literal("遗迹中心 32～50 格范围内没有找到安全落点；未执行传送。"));
            return 0;
        }
        player.teleportTo(overworld, destination.getX() + 0.5, destination.getY(), destination.getZ() + 0.5,
                Set.of(), player.getYRot(), player.getXRot());
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
        if (!BattlefieldSystem1211.requestForceGeneration(overworld, player, count, 2048)) {
            source.sendFailure(Component.literal("你已有一个战场遗迹生成任务正在执行，请等待它完成。"));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("已开始在 2048 格内分批寻找安全区域并生成 " + count
                + " 处战场遗迹。任务会逐步报告进度，并继续遵守正式区域与安全检查。"), false);
        return 1;
    }

    private static BlockPos findSafeTeleportDestination(ServerLevel level, ServerPlayer player, BlockPos center) {
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

    private static int reportNoActiveBattlefield(CommandSourceStack source, BattlefieldSavedData1211 data) {
        int waiting = data.count(BattlefieldSavedData1211.Status.WAITING);
        int cooldown = data.count(BattlefieldSavedData1211.Status.COOLDOWN);
        int replacements = data.replacementJobCount();
        source.sendFailure(Component.literal("当前索引中没有已生成且未完成的战场遗迹；等待生成区域 "
                + waiting + " 个，冷却中区域 " + cooldown + " 个，异地补位任务 " + replacements + " 个。"));
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
}
