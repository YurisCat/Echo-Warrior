package com.yuriscat.echowarrior.compat.network;

import com.yuriscat.echowarrior.compat.EchoWarrior1211;
import com.yuriscat.echowarrior.compat.ModContent1211;
import com.yuriscat.echowarrior.compat.binding.EchoBindingSystem1211;
import com.yuriscat.echowarrior.compat.item.EchoRelicItem1211;
import com.yuriscat.echowarrior.compat.item.EchoSummonerAccessory1211;
import com.yuriscat.echowarrior.compat.item.EchoSummonerItem1211;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Requests a direct creative-mode insertion without trusting a client-authored
 * replacement for the entire summoner stack.
 */
public record CreativeSummonerInsertionPayload1211(int inventoryMenuSlot, ItemStack carried)
        implements CustomPacketPayload {
    public static final Type<CreativeSummonerInsertionPayload1211> TYPE =
            new Type<>(ModContent1211.id("creative_summoner_insertion"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CreativeSummonerInsertionPayload1211> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT,
                    CreativeSummonerInsertionPayload1211::inventoryMenuSlot,
                    ItemStack.STREAM_CODEC,
                    CreativeSummonerInsertionPayload1211::carried,
                    CreativeSummonerInsertionPayload1211::new);

    public CreativeSummonerInsertionPayload1211 {
        carried = carried.copy();
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ServerPlayer player, CreativeSummonerInsertionPayload1211 payload) {
        if (!player.gameMode.isCreative()) return;
        if (payload.inventoryMenuSlot < 0 || payload.inventoryMenuSlot >= player.inventoryMenu.slots.size()) return;

        Slot slot = player.inventoryMenu.getSlot(payload.inventoryMenuSlot);
        if (slot.container != player.getInventory()) return;
        ItemStack current = slot.getItem();
        ItemStack candidate = payload.carried.copy();
        if (!(current.getItem() instanceof EchoSummonerItem1211)
                || candidate.isEmpty()
                || !EchoSummonerItem1211.isDirectInsertionCandidate(candidate)) return;

        candidate.setCount(Math.min(candidate.getCount(), candidate.getMaxStackSize()));
        boolean relic = candidate.getItem() instanceof EchoRelicItem1211;
        boolean accessory = EchoSummonerAccessory1211.isAccessory(candidate);
        ItemStack updated = current.copy();
        EchoSummonerItem1211.getOrCreateSummonerId(updated);
        if (player.level() instanceof ServerLevel level) {
            EchoBindingSystem1211.synchronize(level, updated);
        }
        if (!EchoSummonerItem1211.insertIntoInternalSlotForCreative(updated, candidate)) {
            player.inventoryMenu.broadcastFullState();
            return;
        }

        slot.setByPlayer(updated);
        slot.setChanged();
        if (player.level() instanceof ServerLevel level) {
            if (relic) EchoBindingSystem1211.updateRelic(level, updated);
            if (accessory) EchoBindingSystem1211.updateAccessories(level, updated);
        }
        player.inventoryMenu.broadcastChanges();
        if (Boolean.getBoolean("echo_warrior.auto_pause_after_quick_play")) {
            EchoWarrior1211.LOGGER.info(
                    "Automated creative summoner insertion applied on server: menuSlot={} summoner={}",
                    payload.inventoryMenuSlot, updated);
        }
    }
}
