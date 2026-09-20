package com.yuriscat.echowarrior.compat.mixin;

import com.yuriscat.echowarrior.compat.EchoWarrior1201;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
abstract class MinecraftServerMixin1201 {
    @Inject(method = "stopServer", at = @At("HEAD"))
    private void echoWarrior$restoreTestProtection(CallbackInfo callback) {
        com.yuriscat.echowarrior.compat.test.HeroDiskRestartSelfTest1201.clear();
        com.yuriscat.echowarrior.compat.test.HeroClientFixture1201.restoreAll((MinecraftServer)(Object)this);
        com.yuriscat.echowarrior.compat.test.ClientTestProtection1201.restore((MinecraftServer)(Object)this);
        com.yuriscat.echowarrior.compat.binding.CreativeSummonerDestroyTracker1201.clearAll();
        com.yuriscat.echowarrior.compat.entity.EchoAuraAuditSystem1201.clear();
        com.yuriscat.echowarrior.compat.world.BattlefieldSystem1201.clear();
        com.yuriscat.echowarrior.compat.world.EchoCompassSystem1201.clear();
    }
    @Inject(method = "tickServer", at = @At("RETURN"))
    private void echoWarrior$confirmCreativeDestruction(java.util.function.BooleanSupplier hasTimeLeft, CallbackInfo callback) {
        com.yuriscat.echowarrior.compat.binding.CreativeSummonerDestroyTracker1201.tick((MinecraftServer)(Object)this);
        MinecraftServer server = (MinecraftServer)(Object)this;
        com.yuriscat.echowarrior.compat.recycler.RecyclerSystem1201.tick(server);
        com.yuriscat.echowarrior.compat.world.BattlefieldSystem1201.tick(server);
        com.yuriscat.echowarrior.compat.world.EchoCompassSystem1201.tick(server);
        for (var level : server.getAllLevels()) {
            com.yuriscat.echowarrior.compat.combat.ShieldChargeCreeperControl1201.tick(level);
            com.yuriscat.echowarrior.compat.entity.CatGodCreeperSystem1201.tickPanickingCreepers(level);
            com.yuriscat.echowarrior.compat.item.EchoAccessorySystem1201.tickLevel(level);
            com.yuriscat.echowarrior.compat.item.EchoTalentSystem1201.tickLevel(level);
        }
        com.yuriscat.echowarrior.compat.binding.EchoBindingSystem1201.tick(server);
        com.yuriscat.echowarrior.compat.entity.EchoAuraAuditSystem1201.tick(server);
        com.yuriscat.echowarrior.compat.test.HeroDiskRestartSelfTest1201.tick(server);
    }
    @Inject(method = "loadLevel", at = @At("RETURN"))
    private void echoWarrior$checkBootstrap(CallbackInfo callback) {
        EchoWarrior1201.onServerLevelLoaded((MinecraftServer)(Object)this);
    }
}
