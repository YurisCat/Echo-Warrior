package com.yuriscat.echowarrior.compat;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import com.yuriscat.echowarrior.compat.test.SummonerStorageSelfTest1201;
import com.yuriscat.echowarrior.compat.test.BindingAuthoritySelfTest1201;
import com.yuriscat.echowarrior.compat.test.NetworkTransactionSelfTest1201;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.tslat.smartbrainlib.api.SmartBrainOwner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.bernie.geckolib.animatable.GeoEntity;

import java.util.UUID;

/** Common entrypoint for the isolated, in-development 1.20.1 port. */
public final class EchoWarrior1201 {
    public static final String MOD_ID = "echo_warrior";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static String loader;

    private EchoWarrior1201() {}
    public static net.minecraft.resources.ResourceLocation id(String path) { return ModContent1201.id(path); }

    public static void initialize(String loaderName) {
        loader = loaderName;
        LOGGER.info("[Compat1201] Initialized {} internal compatibility build; full-port acceptance is pending", loader);
    }

    public static void onServerLevelLoaded(MinecraftServer server) {
        if (loader == null) throw new IllegalStateException("Compatibility entrypoint did not run");
        // This marker is emitted from a real vanilla-method injection, so a production
        // server proves more than just the presence of a JSON/refmap in the JAR.
        LOGGER.info("[Compat1201] Server Mixin applied on {}", loader);
        if (Boolean.getBoolean("echo_warrior.compat_bootstrap_test")) {
            runBootstrapSelfTest();
            SummonerStorageSelfTest1201.run(server);
            BindingAuthoritySelfTest1201.run(server);
            NetworkTransactionSelfTest1201.run(server);
            com.yuriscat.echowarrior.compat.test.SummonerMenuSelfTest1201.run(server);
            com.yuriscat.echowarrior.compat.test.RelicEquipmentSelfTest1201.run(server);
            com.yuriscat.echowarrior.compat.test.CreativeInsertionSelfTest1201.run(server);
            com.yuriscat.echowarrior.compat.test.HeroLifecycleSelfTest1201.run(server);
            com.yuriscat.echowarrior.compat.test.BooksSelfTest1201.run(server);
            com.yuriscat.echowarrior.compat.test.RecyclerSelfTest1201.run(server);
            com.yuriscat.echowarrior.compat.test.ExplorationSelfTest1201.run(server);
            com.yuriscat.echowarrior.compat.world.ExplorationIntegrationSelfTest1201.run(server);
            com.yuriscat.echowarrior.compat.test.HeroDiskRestartSelfTest1201.prepare(server);
        }
    }

    public static void runBootstrapSelfTest() {
        UUID identity = UUID.fromString("dc791472-1033-4449-a73f-10b96d9070da");
        ItemStack original = new ItemStack(Items.PAPER);
        CompoundTag data = original.getOrCreateTag();
        data.putUUID("SummonerUuid", identity);
        data.putLong("StateRevision", 42L);
        ItemStack restored = ItemStack.of(original.save(new CompoundTag()));
        require(restored.is(Items.PAPER) && restored.getCount() == 1, "item round trip");
        require(identity.equals(restored.getOrCreateTag().getUUID("SummonerUuid")), "UUID round trip");
        require(restored.getOrCreateTag().getLong("StateRevision") == 42L, "revision round trip");
        restored.getOrCreateTag().putLong("StateRevision", 43L);
        require(data.getLong("StateRevision") == 42L, "NBT snapshots do not alias");
        require(SmartBrainOwner.class.isInterface() && GeoEntity.class.isInterface(), "dependency linkage");
        LOGGER.info("[Compat1201] BOOTSTRAP SELFTEST PASSED loader={} checks=5", loader);
    }

    private static void require(boolean condition, String check) {
        if (!condition) throw new IllegalStateException("[Compat1201] Failed: " + check);
    }
}
