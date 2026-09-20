package com.yuriscat.echowarrior.compat.menu;

import com.yuriscat.echowarrior.compat.binding.EchoBindingSystem1201;
import com.yuriscat.echowarrior.compat.item.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import java.util.List;

/** Single insertion algorithm for vanilla inventory, creative requests and custom-menu source clicks. */
public final class SummonerTransfer1201 {
    private SummonerTransfer1201() {}
    public static boolean candidate(ItemStack stack) {
        return !stack.isEmpty() && (stack.getItem() instanceof EchoRelicItem1201
                || stack.getItem() instanceof EchoSummonerAccessory1201 || SummonerFuel1201.value(stack) > 0);
    }
    public static int insert(List<ItemStack> slots, ItemStack source) {
        if (!candidate(source) || slots.size() != 8) return 0;
        int target = -1;
        int maximum = 1;
        if (source.getItem() instanceof EchoRelicItem1201) {
            if (slots.get(7).isEmpty()) target = 7;
        } else if (source.getItem() instanceof EchoSummonerAccessory1201) {
            for (int index = 0; index < 6; index++) {
                if (slots.get(index).is(source.getItem())) return 0;
                if (slots.get(index).isEmpty() && target < 0) target = index;
            }
        } else {
            target = 6; maximum = source.getMaxStackSize();
            if (!slots.get(target).isEmpty() && !ItemStack.isSameItemSameTags(slots.get(target), source)) return 0;
        }
        if (target < 0) return 0;
        int count = Math.min(source.getCount(), maximum - slots.get(target).getCount());
        if (count <= 0) return 0;
        slots.set(target, source.copyWithCount(slots.get(target).getCount() + count));
        return count;
    }
    public static int commit(ServerPlayer player, ItemStack summoner, ItemStack source, long revision) {
        var binding = EchoBindingSystem1201.synchronize(player.serverLevel(), summoner);
        if (revision >= 0 && binding.stateRevision() != revision) return 0;
        var slots = new java.util.ArrayList<>(binding.contents());
        int inserted = insert(slots, source);
        if (inserted == 0) return 0;
        EchoRelicState1201.ensureInitialized(slots.get(7), player.getRandom(), player.level().getGameTime());
        if (!binding.commitEquipment(binding.stateRevision(), slots)) return 0;
        source.shrink(inserted);
        binding.mirrorTo(summoner);
        player.getInventory().setChanged();
        return inserted;
    }
}
