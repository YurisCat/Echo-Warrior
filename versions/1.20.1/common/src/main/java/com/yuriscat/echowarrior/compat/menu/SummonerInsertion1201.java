package com.yuriscat.echowarrior.compat.menu;

import com.yuriscat.echowarrior.compat.ModContent1201;
import com.yuriscat.echowarrior.compat.binding.EchoBindingSavedData1201;
import com.yuriscat.echowarrior.compat.binding.EchoBindingSystem1201;
import com.yuriscat.echowarrior.compat.item.SummonerData1201;
import com.yuriscat.echowarrior.compat.item.SummonerFuel1201;
import com.yuriscat.echowarrior.compat.network.SummonerInsertRequest1201;
import com.yuriscat.echowarrior.compat.network.SummonerInsertResult1201;
import com.yuriscat.echowarrior.compat.network.SummonerInsertResult1201.Outcome;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.List;

/** Shared server transaction behind future menu/creative adapters. First supported category: fuel. */
public final class SummonerInsertion1201 {
    private SummonerInsertion1201() {}

    public static SummonerInsertResult1201 handle(ServerPlayer player, SummonerInsertRequest1201 request) {
        if (!player.server.isSameThread()) throw new IllegalStateException("Inventory transaction off server thread");
        if (request.requestId() < 0 || request.expectedRevision() < 0 || request.summonerId() == null
                || request.count() < 1 || request.count() > 64 || !inventorySlot(request.summonerSlot())
                || !inventorySlot(request.sourceSlot()) || request.summonerSlot() == request.sourceSlot()) {
            return reject(request, Outcome.INVALID_REQUEST, -1);
        }
        if (!player.isAlive() || player.isSpectator()) return reject(request, Outcome.PLAYER_UNAVAILABLE, -1);
        if (player.containerMenu != player.inventoryMenu || player.containerMenu.containerId != request.menuId()) {
            return reject(request, Outcome.WRONG_MENU, -1);
        }
        ItemStack summoner = player.getInventory().getItem(request.summonerSlot());
        if (!summoner.is(ModContent1201.ECHO_SUMMONER)
                || !request.summonerId().equals(SummonerData1201.summonerId(summoner))) {
            return reject(request, Outcome.WRONG_SUMMONER, -1);
        }
        EchoBindingSavedData1201 data = EchoBindingSavedData1201.get(player.server);
        if (data.get(request.summonerId()) == null) return reject(request, Outcome.NOT_REGISTERED, -1);
        EchoBindingSavedData1201.Binding binding = EchoBindingSystem1201.synchronize(player.serverLevel(), summoner);
        if (!binding.summonerId().equals(request.summonerId())) return reject(request, Outcome.WRONG_SUMMONER, -1);
        if (binding.stateRevision() != request.expectedRevision()) {
            return reject(request, Outcome.STALE_REVISION, binding.stateRevision());
        }
        ItemStack source = player.getInventory().getItem(request.sourceSlot());
        if (source.isEmpty() || source.getCount() < request.count()) {
            return reject(request, Outcome.INVALID_SOURCE, binding.stateRevision());
        }
        // Relics/accessories remain fail-closed until their real item types and eligibility rules are ported.
        if (SummonerFuel1201.value(source) == 0) return reject(request, Outcome.UNSUPPORTED_ITEM, binding.stateRevision());
        List<ItemStack> contents = new ArrayList<>(binding.contents());
        ItemStack stored = contents.get(SummonerData1201.FUEL_SLOT);
        if (!stored.isEmpty() && !ItemStack.isSameItemSameTags(stored, source)) {
            return reject(request, Outcome.NO_SPACE, binding.stateRevision());
        }
        int inserted = Math.min(request.count(), source.getMaxStackSize() - stored.getCount());
        if (inserted <= 0) return reject(request, Outcome.NO_SPACE, binding.stateRevision());
        if (stored.isEmpty()) contents.set(SummonerData1201.FUEL_SLOT, source.copyWithCount(inserted));
        else stored.grow(inserted);
        // Same server task: commit succeeds before consuming the physical source, never client NBT.
        if (!binding.commitContents(request.expectedRevision(), contents)) {
            return reject(request, Outcome.STALE_REVISION, binding.stateRevision());
        }
        source.shrink(inserted);
        if (source.isEmpty()) player.getInventory().setItem(request.sourceSlot(), ItemStack.EMPTY);
        binding.mirrorTo(summoner);
        player.getInventory().setChanged();
        return new SummonerInsertResult1201(request.requestId(), Outcome.INSERTED, inserted, binding.stateRevision());
    }

    private static boolean inventorySlot(int slot) { return slot >= 0 && slot < 36 || slot == 40; }
    private static SummonerInsertResult1201 reject(SummonerInsertRequest1201 request, Outcome outcome, long revision) {
        return new SummonerInsertResult1201(request.requestId(), outcome, 0, revision);
    }
}
