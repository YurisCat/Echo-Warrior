package com.yuriscat.echowarrior.compat.binding;

import com.yuriscat.echowarrior.compat.item.SummonerData1201;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/** Tracks observed physical slots, never Java ItemStack object identity across sync replacements. */
public final class SummonerIdentityTracker1201 {
    private final Map<UUID, Location> canonical = new HashMap<>();

    public UUID resolve(ItemStack stack, Location observed, Function<Location, ItemStack> lookup) {
        UUID id = SummonerData1201.getOrCreateSummonerId(stack);
        // Detached snapshots cannot prove duplication and must not claim an inventory location.
        if (observed == null) return id;
        Location previous = canonical.get(id);
        if (previous != null && !previous.equals(observed)
                && id.equals(SummonerData1201.summonerId(lookup.apply(previous)))) {
            id = SummonerData1201.replaceDuplicateId(stack);
        }
        canonical.put(id, observed);
        return id;
    }

    public void forget(UUID id) { canonical.remove(id); }

    /** Slot -1 denotes the currently open menu's carried stack. */
    public record Location(UUID playerId, int slot) {}
}
