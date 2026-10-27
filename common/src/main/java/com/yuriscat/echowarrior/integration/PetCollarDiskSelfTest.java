package com.yuriscat.echowarrior.integration;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import java.nio.file.Files;

/** Explicit test-world artifact: real compressed item NBT is consumed on the next JVM boot. */
public final class PetCollarDiskSelfTest {
    private PetCollarDiskSelfTest() {}
    public static void verify(MinecraftServer server, ItemStack relic, double maximum) {
        var path = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)
                .resolve("echo-warrior-pet-collar-selftest.nbt");
        boolean reloaded = Files.isRegularFile(path);
        try {
            if (reloaded) {
                CompoundTag record = net.minecraft.nbt.NbtIo.readCompressed(path, net.minecraft.nbt.NbtAccounter.unlimitedHeap());
                ItemStack saved = ItemStack.CODEC.parse(net.minecraft.resources.RegistryOps.create(net.minecraft.nbt.NbtOps.INSTANCE, server.registryAccess()), record.getCompoundOrEmpty("Relic")).getOrThrow();
                var hero = com.yuriscat.echowarrior.ModEntities.ROMAN_LEGIONARY_ECHO.create(server.overworld(), net.minecraft.world.entity.EntitySpawnReason.SPAWN_ITEM_USE);
                if (hero == null) throw new IllegalStateException("Unable to create saved collar fixture");
                try {
                    hero.applyRelicState(saved, false);
                    PetCollarCompat.restore(hero, saved);
                    if (Math.abs(hero.getMaxHealth() - record.getDoubleOr("Maximum", 0.0)) > 0.001)
                        throw new IllegalStateException("Saved collar lost its health modifier after disk restart");
                } finally { hero.discard(); }
            }
            CompoundTag record = new CompoundTag();
            record.put("Relic", ItemStack.CODEC.encodeStart(net.minecraft.resources.RegistryOps.create(net.minecraft.nbt.NbtOps.INSTANCE, server.registryAccess()), relic).getOrThrow());
            record.putDouble("Maximum", maximum);
            net.minecraft.nbt.NbtIo.writeCompressed(record, path);
            org.slf4j.LoggerFactory.getLogger("echo_warrior").info(
                    "[PetCollarDiskSelfTest] PASS mode={}", reloaded ? "reloaded" : "written");
        } catch (java.io.IOException failure) {
            throw new IllegalStateException("Collar test disk round-trip failed", failure);
        }
    }
}
