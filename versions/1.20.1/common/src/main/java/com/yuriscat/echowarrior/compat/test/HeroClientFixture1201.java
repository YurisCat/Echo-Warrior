package com.yuriscat.echowarrior.compat.test;

import com.yuriscat.echowarrior.compat.*;
import com.yuriscat.echowarrior.compat.binding.*;
import com.yuriscat.echowarrior.compat.item.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import java.util.*;
import java.util.stream.IntStream;

/** Opt-in fixture with pre-logout rollback. No client class references on the server. */
public final class HeroClientFixture1201 {
    private static final Map<ServerPlayer, HeroClientFixture1201> ACTIVE = new IdentityHashMap<>();
    public final ServerPlayer player;
    public final EchoHeroType1201 hero;
    public final List<UUID> ids = new ArrayList<>();
    public final List<net.minecraft.world.entity.LivingEntity> spirits = new ArrayList<>();
    private net.minecraft.world.entity.monster.Zombie combatTarget;
    private final List<ItemStack> inventory;
    private final Map<BlockPos, BlockState> blocks = new LinkedHashMap<>();
    private final Vec3 position;
    private final float yaw, pitch;
    private final int selected;
    private final GameType mode;
    private final boolean flying;

    private HeroClientFixture1201(ServerPlayer player, EchoHeroType1201 hero) {
        this.player = player;
        this.hero = hero;
        inventory = IntStream.range(0, player.getInventory().getContainerSize()).mapToObj(i -> player.getInventory().getItem(i).copy()).toList();
        selected = player.getInventory().selected;
        mode = player.gameMode.getGameModeForPlayer();
        flying = player.getAbilities().flying;
        position = player.position(); yaw = player.getYRot(); pitch = player.getXRot();
    }

    public static HeroClientFixture1201 prepare(ServerPlayer player, EchoHeroType1201 hero) {
        if (!Boolean.getBoolean("echo_warrior.auto_pause_after_quick_play")) throw new IllegalStateException("Not an automated client");
        if (ACTIVE.containsKey(player) || player.containerMenu != player.inventoryMenu || !player.inventoryMenu.getCarried().isEmpty())
            throw new IllegalStateException("Hero fixture requires empty cursor and closed menu");
        var fixture = new HeroClientFixture1201(player, hero);
        ACTIVE.put(player, fixture);
        try {
            var level = player.serverLevel();
            // Temporary platform above current terrain, never overwrite a block entity or its inventory.
            int y = Math.min(level.getMaxBuildHeight() - 8, Math.max(player.getBlockY() + 24, 200));
            BlockPos center = new BlockPos(player.getBlockX(), y, player.getBlockZ());
            for (int dx = -6; dx <= 6; dx++) for (int dz = -6; dz <= 6; dz++) for (int dy = 0; dy <= 4; dy++) {
                BlockPos pos = center.offset(dx, dy, dz);
                if (!level.getBlockState(pos).isAir()) throw new IllegalStateException("Hero fixture platform is not empty: " + pos);
            }
            for (int dx = -6; dx <= 6; dx++) for (int dz = -6; dz <= 6; dz++) {
                BlockPos pos = center.offset(dx, 0, dz);
                fixture.blocks.put(pos, level.getBlockState(pos));
                level.setBlock(pos, Blocks.STONE.defaultBlockState(), 2);
            }
            player.setGameMode(GameType.CREATIVE);
            player.connection.teleport(center.getX() + .5, y + 1, center.getZ() + .5, 0, 0);
            fixture.addSummoner(0, hero, false);
            player.getInventory().selected = 0;
            player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket(0));
            player.inventoryMenu.broadcastChanges();
            return fixture;
        } catch (RuntimeException error) { fixture.restore(); throw error; }
    }

    public ItemStack addSummoner(int slot, EchoHeroType1201 type, boolean spawn) {
        var stack = new ItemStack(ModContent1201.ECHO_SUMMONER);
        var relic = new ItemStack(ModContent1201.relic(type));
        EchoRelicState1201.ensureInitialized(relic, player.getRandom(), player.level().getGameTime());
        EchoRelicState1201.setTraitsForSelfTest(relic, 0, EchoBiomeAffinity1201.OPENLAND);
        EchoRelicState1201.setActivityMode(relic, EchoRelicState1201.ActivityMode.WAIT);
        EchoRelicState1201.setAlertMode(relic, EchoRelicState1201.AlertMode.PEACEFUL);
        var binding = EchoBindingSystem1201.synchronize(player.serverLevel(), stack);
        ids.add(binding.summonerId());
        var contents = new ArrayList<>(binding.contents()); contents.set(7, relic);
        binding.commitEquipment(binding.stateRevision(), contents);
        binding.setFuel(1000); binding.mirrorTo(stack);
        player.getInventory().setItem(slot, stack);
        if (spawn) {
            var attempt = EchoBindingSystem1201.summonNew(player.serverLevel(), player, stack);
            if (!attempt.succeeded()) throw new IllegalStateException("Hero fixture spawn: " + attempt.failure());
            spirits.add(attempt.spirit().livingEntity());
        }
        return stack;
    }

    public void restore() {
        if (ACTIVE.remove(player) != this) return;
        player.containerMenu.setCarried(ItemStack.EMPTY);
        if (player.containerMenu != player.inventoryMenu) player.closeContainer();
        ids.forEach(id -> EchoBindingSystem1201.destroySummoner(player.serverLevel(), id));
        spirits.forEach(net.minecraft.world.entity.Entity::discard);
        if (combatTarget != null) combatTarget.discard();
        for (var level : player.server.getAllLevels()) {
            var drops = new ArrayList<net.minecraft.world.entity.Entity>();
            for (var entity : level.getAllEntities()) if (entity instanceof net.minecraft.world.entity.item.ItemEntity item
                    && SummonerStackContents1201.scan(item.getItem()).ids().stream().anyMatch(ids::contains)) drops.add(item);
            drops.forEach(net.minecraft.world.entity.Entity::discard);
        }
        blocks.forEach((pos, state) -> player.serverLevel().setBlock(pos, state, 2));
        for (int i = 0; i < inventory.size(); i++) player.getInventory().setItem(i, inventory.get(i).copy());
        player.getInventory().selected = selected;
        player.setGameMode(mode);
        player.getAbilities().flying = flying;
        player.onUpdateAbilities();
        player.connection.teleport(position.x, position.y, position.z, yaw, pitch);
        player.setDeltaMovement(Vec3.ZERO); player.resetFallDistance();
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket(selected));
        player.inventoryMenu.broadcastChanges();
    }
    public static void restoreBeforeLogout(ServerPlayer player) {
        var fixture = ACTIVE.get(player); if (fixture != null) fixture.restore();
    }
    public void startCombat() {
        var binding = EchoBindingSavedData1201.get(player.server).get(ids.get(0));
        EchoBindingSystem1201.setAlertMode(player.serverLevel(), binding, EchoRelicState1201.AlertMode.AGGRESSIVE.ordinal());
        EchoBindingSystem1201.setActivityMode(player.serverLevel(), binding, EchoRelicState1201.ActivityMode.FOLLOW.ordinal());
        combatTarget = net.minecraft.world.entity.EntityType.ZOMBIE.create(player.serverLevel());
        combatTarget.setNoAi(true);
        combatTarget.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new ItemStack(net.minecraft.world.item.Items.DIAMOND_HELMET));
        combatTarget.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(400);
        combatTarget.setHealth(400);
        combatTarget.moveTo(player.getX() + 3, player.getY(), player.getZ() + 3, 0, 0);
        player.serverLevel().addFreshEntity(combatTarget);
    }
    public boolean combatHit() {
        if (combatTarget == null || combatTarget.getHealth() >= 400) return false;
        return combatTarget.getLastHurtByMob() instanceof com.yuriscat.echowarrior.compat.entity.EchoWarriorEntity1201 echo
                && ids.get(0).equals(echo.getSummonerId());
    }
    public static void restoreAll(MinecraftServer server) {
        new ArrayList<>(ACTIVE.values()).stream().filter(f -> f.player.server == server).forEach(HeroClientFixture1201::restore);
    }
}
