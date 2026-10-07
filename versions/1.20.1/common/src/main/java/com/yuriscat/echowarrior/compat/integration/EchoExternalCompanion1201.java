package com.yuriscat.echowarrior.compat.integration;

import com.yuriscat.echowarrior.compat.ModContent1201;
import com.yuriscat.echowarrior.compat.binding.*;
import com.yuriscat.echowarrior.compat.item.*;
import com.yuriscat.echowarrior.compat.mixin.EchoBindingExternalInvoker1201;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** Optional external entry point. A current controller and a matching authoritative relic are mandatory. */
public final class EchoExternalCompanion1201 {
    public record Result(boolean succeeded, String failure) {
        public static Result failed(String failure) { return new Result(false, failure); }
    }
    private EchoExternalCompanion1201() {}
    public static UUID entryId(EchoBindingSavedData1201.Binding binding) {
        return UUID.nameUUIDFromBytes(("echo_warrior:tbf:" + binding.summonerId() + ":"
                + EchoRelicState1201.relicId(binding.relic())).getBytes(StandardCharsets.UTF_8));
    }
    public static EchoBindingSavedData1201.Binding resolve(ServerPlayer player, UUID entry) {
        if (!player.server.isSameThread()) throw new IllegalStateException("External Echo operation outside server thread");
        if (!player.isAlive() || player.isSpectator()) return null;
        for (var binding : EchoBindingSavedData1201.get(player.server).bindings()) {
            if (player.getUUID().equals(binding.controllerId())
                    && binding.relic().getItem() instanceof EchoRelicItem1201
                    && entry.equals(entryId(binding))) return binding;
        }
        return null;
    }
    public static Result summon(ServerPlayer player, UUID entry) {
        var binding = resolve(player, entry);
        if (binding == null) return Result.failed("CREATE_FAILED");
        // This mirror is an output buffer for the existing transaction, never inserted into any inventory.
        ItemStack mirror = new ItemStack(ModContent1201.ECHO_SUMMONER);
        mirror.getOrCreateTag().putUUID("EchoWarriorSummonerId", binding.summonerId());
        binding.mirrorTo(mirror);
        if (binding.active()) {
            var spirit = EchoBindingSystem1201.recallOrReconstruct(player.serverLevel(), player, mirror, binding);
            return new Result(spirit != null,
                    spirit == null ? "NO_SAFE_POSITION" : "NONE");
        }
        if (!EchoBindingSystem1201.canAddControllerEcho(player.server, player.getUUID(), binding.summonerId()))
            return Result.failed("LIMIT_REACHED");
        int cost = SummonerFuel1201.summonCost(binding.relic());
        if (binding.fuel() < cost)
            return Result.failed("NOT_ENOUGH_FUEL");
        var result = EchoBindingExternalInvoker1201.echoWarrior$spawn(player.serverLevel(), player, mirror, binding, false, cost);
        return new Result(result.succeeded(), result.failure().name());
    }
    public static boolean dismiss(ServerPlayer player, UUID entry) {
        var binding = resolve(player, entry);
        return binding != null && EchoBindingSystem1201.dismiss(player.server, binding.summonerId());
    }
}
