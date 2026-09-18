package com.yuriscat.echowarrior.compat;

import com.yuriscat.echowarrior.compat.effect.BleedingMobEffect1211;
import com.yuriscat.echowarrior.compat.effect.ObsidianWoundMobEffect1211;
import com.yuriscat.echowarrior.compat.effect.SoldierFormationMobEffect1211;
import com.yuriscat.echowarrior.compat.effect.SunBlessingMobEffect1211;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ModEffects1211 {
    private static final Map<ResourceLocation, MobEffect> EFFECTS = new LinkedHashMap<>();

    private static final MobEffect LEGACY_SOLDIER_FORMATION_VALUE = effect("soldier_formation", new SoldierFormationMobEffect1211());
    private static final MobEffect WEAPONS_RAISED_VALUE = effect("weapons_raised", new SoldierFormationMobEffect1211());
    private static final MobEffect SHIELDS_RAISED_VALUE = effect("shields_raised", new SoldierFormationMobEffect1211());
    private static final MobEffect HUITZILOPOCHTLI_BLESSING_VALUE = effect("huitzilopochtli_blessing", new SunBlessingMobEffect1211());
    private static final MobEffect OBSIDIAN_WOUND_VALUE = effect("obsidian_wound", new ObsidianWoundMobEffect1211());
    private static final MobEffect BLEEDING_VALUE = effect("bleeding", new BleedingMobEffect1211());

    public static Holder<MobEffect> LEGACY_SOLDIER_FORMATION;
    public static Holder<MobEffect> WEAPONS_RAISED;
    public static Holder<MobEffect> SHIELDS_RAISED;
    public static Holder<MobEffect> HUITZILOPOCHTLI_BLESSING;
    public static Holder<MobEffect> OBSIDIAN_WOUND;
    public static Holder<MobEffect> BLEEDING;

    private ModEffects1211() { }

    public static Map<ResourceLocation, MobEffect> effects() { return Map.copyOf(EFFECTS); }

    public static void bindHolders() {
        LEGACY_SOLDIER_FORMATION = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(LEGACY_SOLDIER_FORMATION_VALUE);
        WEAPONS_RAISED = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(WEAPONS_RAISED_VALUE);
        SHIELDS_RAISED = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(SHIELDS_RAISED_VALUE);
        HUITZILOPOCHTLI_BLESSING = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(HUITZILOPOCHTLI_BLESSING_VALUE);
        OBSIDIAN_WOUND = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(OBSIDIAN_WOUND_VALUE);
        BLEEDING = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(BLEEDING_VALUE);
    }

    private static MobEffect effect(String path, MobEffect effect) {
        EFFECTS.put(EchoWarrior1211.id(path), effect);
        return effect;
    }
}
