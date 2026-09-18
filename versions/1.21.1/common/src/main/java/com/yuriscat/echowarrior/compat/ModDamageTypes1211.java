package com.yuriscat.echowarrior.compat;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;

public final class ModDamageTypes1211 {
    public static final ResourceKey<DamageType> OBSIDIAN_WOUND = key("obsidian_wound");
    public static final ResourceKey<DamageType> BLEEDING = key("bleeding");
    public static final ResourceKey<DamageType> ARMOR_PIERCING_ARROW = key("armor_piercing_arrow");
    public static final ResourceKey<DamageType> ROMAN_FIRST_STRIKE = key("roman_first_strike");
    public static final ResourceKey<DamageType> ROMAN_FOLLOWUP = key("roman_followup");
    public static final ResourceKey<DamageType> SAMURAI_FIRST_SLASH = key("samurai_first_slash");
    public static final ResourceKey<DamageType> SAMURAI_STAB = key("samurai_stab");
    public static final ResourceKey<DamageType> SPIKED_ARMOR_REFLECTION = key("spiked_armor_reflection");

    private ModDamageTypes1211() { }

    private static ResourceKey<DamageType> key(String path) {
        return ResourceKey.create(Registries.DAMAGE_TYPE, EchoWarrior1211.id(path));
    }

    public static DamageSource source(ServerLevel level, ResourceKey<DamageType> type) {
        return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(type));
    }

    public static DamageSource source(ServerLevel level, ResourceKey<DamageType> type, Entity directEntity) {
        return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(type), directEntity);
    }

    public static DamageSource source(ServerLevel level, ResourceKey<DamageType> type,
                                      Entity directEntity, Entity causingEntity) {
        return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(type), directEntity, causingEntity);
    }
}
