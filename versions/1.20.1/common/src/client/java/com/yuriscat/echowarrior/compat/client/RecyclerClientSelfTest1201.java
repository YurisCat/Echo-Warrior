package com.yuriscat.echowarrior.compat.client;

import com.yuriscat.echowarrior.compat.EchoHeroType1201;
import com.yuriscat.echowarrior.compat.EchoWarrior1201;
import com.yuriscat.echowarrior.compat.ModContent1201;
import com.yuriscat.echowarrior.compat.block.entity.RecyclerChestBlockEntity1201;
import com.yuriscat.echowarrior.compat.menu.RecyclerMenu1201;
import com.yuriscat.echowarrior.compat.test.HeroClientFixture1201;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import java.util.concurrent.CompletableFuture;

/** Opt-in controller only: real chest use/click packets, lid animation and fixture rollback. */
public final class RecyclerClientSelfTest1201 {
    private static int phase, frames;
    private static long deadline;
    private static CompletableFuture<?> task;
    private static HeroClientFixture1201 fixture;
    private static BlockPos recyclerPos, vanillaPos;
    private static RuntimeException failure;

    private RecyclerClientSelfTest1201() {}

    public static boolean tick(Minecraft client) {
        if (phase == 100) return true;
        if (phase == 99) {
            if (!task.isDone()) return false;
            task.join();
            phase = 100;
            if (failure != null) throw failure;
            EchoWarrior1201.LOGGER.info("[Compat1201] RECYCLER CLIENT SELFTEST PASSED menu=27-slots deposit=3-diamonds withdraw=conserved lid=open-close fixture=restored");
            return true;
        }
        var server = client.getSingleplayerServer();
        try {
            if (phase != 0 && System.nanoTime() > deadline)
                throw new IllegalStateException("Recycler client timeout in phase " + phase);
            switch (phase) {
                case 0 -> {
                    client.setScreen(null);
                    var id = client.player.getUUID();
                    task = server.submit(() -> {
                        fixture = HeroClientFixture1201.prepare(server.getPlayerList().getPlayer(id), EchoHeroType1201.ROMAN_LEGIONARY);
                        recyclerPos = fixture.player.blockPosition().south(2).west();
                        vanillaPos = recyclerPos.east(2);
                        var level = fixture.player.serverLevel();
                        level.setBlock(recyclerPos, ModContent1201.ECHO_RECYCLER.defaultBlockState()
                                .setValue(ChestBlock.FACING, Direction.NORTH), 3);
                        level.setBlock(vanillaPos, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH), 3);
                        fixture.player.getInventory().clearContent();
                        fixture.player.getInventory().setItem(0, new ItemStack(ModContent1201.ECHO_RECYCLER_ITEM));
                        fixture.player.getInventory().setItem(1, new ItemStack(Items.CHEST));
                        fixture.player.getInventory().setItem(3, new ItemStack(Items.DIAMOND, 3));
                        fixture.player.inventoryMenu.broadcastChanges();
                    });
                    advance(1);
                }
                case 1 -> {
                    if (!task.isDone()) return false;
                    task.join();
                    if (!(client.level.getBlockEntity(recyclerPos) instanceof RecyclerChestBlockEntity1201)
                            || !client.player.getMainHandItem().is(ModContent1201.ECHO_RECYCLER_ITEM)) return false;
                    client.player.setYRot(0);
                    client.player.setXRot(25);
                    advance(2);
                }
                case 2 -> {
                    if (++frames < 20) return false;
                    capture(client, "echo-recycler-world.png");
                    client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND,
                            new BlockHitResult(Vec3.atCenterOf(recyclerPos), Direction.NORTH, recyclerPos, false));
                    advance(3);
                }
                case 3 -> {
                    if (!(client.player.containerMenu instanceof RecyclerMenu1201 menu)) return false;
                    check(menu.getRowCount() == 3 && menu.slots.size() == 63, "real 27-slot menu");
                    var chest = (RecyclerChestBlockEntity1201)client.level.getBlockEntity(recyclerPos);
                    if (chest.getOpenNess(1) < .9F) return false;
                    // Chest slots 0..26, inventory 27..53, hotbar 54..62.
                    check(menu.getSlot(57).getItem().is(Items.DIAMOND), "server inventory synced");
                    client.gameMode.handleInventoryMouseClick(menu.containerId, 57, 0, ClickType.QUICK_MOVE, client.player);
                    advance(4);
                }
                case 4 -> {
                    var menu = client.player.containerMenu;
                    if (!menu.getSlot(0).getItem().is(Items.DIAMOND) || menu.getSlot(0).getItem().getCount() != 3) return false;
                    if (++frames < 20) return false;
                    capture(client, "echo-recycler-menu.png");
                    task = server.submit(() -> {
                        var chest = (RecyclerChestBlockEntity1201)fixture.player.serverLevel().getBlockEntity(recyclerPos);
                        check(chest.getItem(0).is(Items.DIAMOND) && chest.getItem(0).getCount() == 3,
                                "actual server chest received three diamonds");
                    });
                    advance(5);
                }
                case 5 -> {
                    if (!task.isDone()) return false;
                    task.join();
                    client.gameMode.handleInventoryMouseClick(client.player.containerMenu.containerId, 0, 0, ClickType.QUICK_MOVE, client.player);
                    advance(6);
                }
                case 6 -> {
                    if (!client.player.containerMenu.getSlot(0).getItem().isEmpty()) return false;
                    task = server.submit(() -> {
                        var chest = (RecyclerChestBlockEntity1201)fixture.player.serverLevel().getBlockEntity(recyclerPos);
                        check(chest.isEmpty() && fixture.player.getInventory().countItem(Items.DIAMOND) == 3,
                                "actual withdrawal conserves diamonds");
                    });
                    advance(7);
                }
                case 7 -> {
                    if (!task.isDone()) return false;
                    task.join();
                    client.player.closeContainer();
                    advance(8);
                }
                case 8 -> {
                    var chest = (RecyclerChestBlockEntity1201)client.level.getBlockEntity(recyclerPos);
                    if (chest.getOpenNess(1) > .01F) return false;
                    finish(client, null);
                }
            }
        } catch (RuntimeException error) {
            finish(client, error);
        }
        return false;
    }

    private static void advance(int next) {
        phase = next;
        frames = 0;
        deadline = System.nanoTime() + 15_000_000_000L;
    }

    private static void finish(Minecraft client, RuntimeException error) {
        failure = error;
        client.setScreen(null);
        task = client.getSingleplayerServer().submit(() -> {
            if (fixture == null) return;
            var level = fixture.player.serverLevel();
            for (BlockPos pos : new BlockPos[]{recyclerPos, vanillaPos}) {
                if (pos == null) continue;
                if (level.getBlockEntity(pos) instanceof net.minecraft.world.Container container) container.clearContent();
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            }
            fixture.restore();
        });
        phase = 99;
    }

    private static void capture(Minecraft client, String name) {
        if (Boolean.getBoolean("echo_warrior.recycler_screenshots")) {
            Screenshot.grab(client.gameDirectory, name, client.getMainRenderTarget(), ignored -> {});
            EchoWarrior1201.LOGGER.info("[Compat1201] RECYCLER SCREENSHOT {}", name);
        }
    }

    private static void check(boolean value, String label) {
        if (!value) throw new IllegalStateException("Recycler client: " + label);
    }
}
