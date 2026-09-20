package com.yuriscat.echowarrior.compat.network;

import com.yuriscat.echowarrior.compat.item.SummonerData1201;
import com.yuriscat.echowarrior.compat.menu.SummonerTransfer1201;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import java.util.UUID;

/** Creative players may supply their virtual cursor, never the summoner's contents or identity. */
public record CreativeInsertRequest1201(int requestId, int slot, UUID summonerId, long revision, ItemStack carried) {
    public CreativeInsertRequest1201 { carried = carried.copy(); }
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeInt(requestId); buffer.writeInt(slot); buffer.writeUUID(summonerId);
        buffer.writeLong(revision); buffer.writeItem(carried);
    }
    public static CreativeInsertRequest1201 decode(FriendlyByteBuf buffer) {
        if (buffer.readableBytes() > 65536) throw new IllegalArgumentException("Oversized creative insert request");
        var result = new CreativeInsertRequest1201(buffer.readInt(), buffer.readInt(), buffer.readUUID(), buffer.readLong(), buffer.readItem());
        if (buffer.isReadable()) throw new IllegalArgumentException("Trailing creative insert bytes");
        return result;
    }
    public static CreativeInsertReply1201 handle(ServerPlayer player, CreativeInsertRequest1201 request) {
        if (!player.server.isSameThread()) throw new IllegalStateException("Creative insertion off server thread");
        int count = 0;
        if (player.gameMode.isCreative() && player.isAlive() && !player.isSpectator()
                && player.containerMenu == player.inventoryMenu && request.revision >= 0
                && request.slot >= 0 && request.slot < player.inventoryMenu.slots.size()
                && request.carried.getCount() > 0 && request.carried.getCount() <= request.carried.getMaxStackSize()) {
            var slot = player.inventoryMenu.getSlot(request.slot);
            ItemStack summoner = slot.getItem();
            if (slot.container == player.getInventory() && slot.allowModification(player)
                    && request.summonerId.equals(SummonerData1201.summonerId(summoner))) {
                count = SummonerTransfer1201.commit(player, summoner, request.carried.copy(), request.revision);
                if (count > 0) slot.setChanged();
            }
        }
        player.inventoryMenu.broadcastChanges();
        return new CreativeInsertReply1201(request.requestId, count);
    }
}
