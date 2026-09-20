package com.yuriscat.echowarrior.compat.test;

import com.mojang.authlib.GameProfile;
import com.yuriscat.echowarrior.compat.EchoWarrior1201;
import com.yuriscat.echowarrior.compat.binding.EchoBindingSavedData1201;
import com.yuriscat.echowarrior.compat.entity.EchoWarriorEntity1201;
import com.yuriscat.echowarrior.compat.item.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.trading.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import java.util.*;

/** Calls the concrete vanilla trade and mining entry points, not only the talent helper. */
public final class TalentIntegrationSelfTest1201 {
    private TalentIntegrationSelfTest1201() {}
    public static void run(ServerLevel level, ServerPlayer player, EchoBindingSavedData1201.Binding binding,
                           EchoWarriorEntity1201 echo, List<Entity> fixtures) {
        ItemStack originalRelic = binding.relic().copy();
        var villager = EntityType.VILLAGER.create(level);
        fixtures.add(villager);
        try {
            // ServerPlayer#setPos synchronizes its connection even for an offline fixture.
            player.connection = new net.minecraft.server.network.ServerGamePacketListenerImpl(level.getServer(),
                    new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND), player);
            setTraits(binding, echo, originalRelic, 0);
            var tool = new ItemStack(Items.DIAMOND_PICKAXE);
            int ordinary = mine(level, player, tool, Items.DIAMOND);
            setTraits(binding, echo, originalRelic, EchoTrait1201.LUCKY.mask() | EchoTrait1201.ELOQUENCE.mask());
            check(EchoTalentSystem1201.hasNearbyTalent(player, EchoTrait1201.LUCKY), "owner in aura range");
            var stranger = new ServerPlayer(level.getServer(), level, new GameProfile(UUID.randomUUID(), "TalentStranger"));
            stranger.moveTo(player.getX(), player.getY(), player.getZ(), 0, 0);
            check(!EchoTalentSystem1201.hasNearbyTalent(stranger, EchoTrait1201.LUCKY), "no support for non-owner");
            var savedTool = tool.save(new CompoundTag());
            int lucky = mine(level, player, tool, Items.DIAMOND);
            check(ordinary == 96 && lucky > ordinary && savedTool.equals(tool.save(new CompoundTag())), "actual virtual fortune and immutable real tool");
            tool.enchant(Enchantments.SILK_TOUCH, 1);
            check(mine(level, player, tool, Items.DIAMOND_ORE) == 96, "silk touch retains priority over virtual fortune");
            var oldPos = player.position();
            player.setPos(oldPos.add(100, 0, 0));
            check(!EchoTalentSystem1201.hasNearbyTalent(player, EchoTrait1201.LUCKY), "support range bounded");
            player.setPos(oldPos);
            villager.setVillagerData(villager.getVillagerData().setProfession(VillagerProfession.FARMER));
            villager.moveTo(9, 96, 9, 0, 0); villager.setNoAi(true); level.addFreshEntity(villager);
            var original = new MerchantOffer(new ItemStack(Items.EMERALD, 10), new ItemStack(Items.IRON_INGOT, 4),
                    new ItemStack(Items.DIAMOND), 8, 7, 0);
            // AbstractVillager.overrideOffers is a no-op in 1.20.1 (client proxy API).
            villager.getOffers().clear(); villager.getOffers().add(original);
            villager.mobInteract(player, InteractionHand.MAIN_HAND);
            check(player.containerMenu instanceof MerchantMenu, "concrete villager interaction opens menu");
            var menu = (MerchantMenu)player.containerMenu;
            var offer = menu.getOffers().get(0);
            check(offer.getCostA().getCount() == 8 && offer.getCostB().getCount() == 3
                    && original.getCostA().getCount() == 10 && original.getCostB().getCount() == 4, "session-only 15 percent discount on both inputs");
            menu.getSlot(0).set(new ItemStack(Items.EMERALD, 8));
            menu.getSlot(1).set(new ItemStack(Items.IRON_INGOT, 3));
            menu.clicked(2, 0, ClickType.PICKUP, player);
            check(menu.getCarried().is(Items.DIAMOND) && menu.getSlot(0).getItem().isEmpty()
                    && menu.getSlot(1).getItem().isEmpty() && original.getUses() == 1 && offer.getUses() == 1,
                    "real trade consumes discounted inputs and updates original stock once");
            menu.setCarried(ItemStack.EMPTY); player.closeContainer();
            setTraits(binding, echo, originalRelic, 0);
            villager.mobInteract(player, InteractionHand.MAIN_HAND);
            check(player.containerMenu instanceof MerchantMenu plain && plain.getOffers().get(0).getCostA().getCount() == 10,
                    "reopening without talent restores original price");
            player.closeContainer();
            EchoWarrior1201.LOGGER.info("[Compat1201] TALENT INTEGRATION SELFTEST PASSED mining=actual-{}-to-{} trading=actual-discount-and-stock", ordinary, lucky);
        } finally {
            if (player.containerMenu != player.inventoryMenu) { player.containerMenu.setCarried(ItemStack.EMPTY); player.closeContainer(); }
            binding.persistRelic(originalRelic); echo.applyBindingState(binding, false);
            villager.discard();
        }
    }

    private static int mine(ServerLevel level, ServerPlayer player, ItemStack tool, Item expected) {
        BlockPos pos = new BlockPos(13, 96, 13);
        var bounds = new AABB(pos).inflate(2);
        Set<UUID> before = new HashSet<>();
        for (var item : level.getEntitiesOfClass(ItemEntity.class, bounds)) before.add(item.getUUID());
        try {
            for (int i = 0; i < 96; i++) {
                level.random.setSeed(0xFE1201L + i * 1009L);
                Blocks.DIAMOND_ORE.playerDestroy(level, player, pos, Blocks.DIAMOND_ORE.defaultBlockState(), null, tool);
            }
            return level.getEntitiesOfClass(ItemEntity.class, bounds, item -> !before.contains(item.getUUID()) && item.getItem().is(expected))
                    .stream().mapToInt(item -> item.getItem().getCount()).sum();
        } finally {
            level.getEntitiesOfClass(ItemEntity.class, bounds, item -> !before.contains(item.getUUID())).forEach(Entity::discard);
        }
    }
    private static void setTraits(EchoBindingSavedData1201.Binding binding, EchoWarriorEntity1201 echo, ItemStack original, int mask) {
        var relic = original.copy();
        EchoRelicState1201.setTraitsForSelfTest(relic, mask, EchoBiomeAffinity1201.OPENLAND);
        binding.persistRelic(relic); echo.applyBindingState(binding, false);
    }
    private static void check(boolean success, String label) {
        if (!success) throw new IllegalStateException("Talent integration: " + label);
    }
}
