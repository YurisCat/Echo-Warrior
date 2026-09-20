package com.yuriscat.echowarrior.compat;

import com.yuriscat.echowarrior.compat.effect.BleedingMobEffect1201;
import com.yuriscat.echowarrior.compat.effect.ObsidianWoundMobEffect1201;
import com.yuriscat.echowarrior.compat.effect.SoldierFormationMobEffect1201;
import com.yuriscat.echowarrior.compat.effect.SunBlessingMobEffect1201;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ModEffects1201 {
    private static final Map<ResourceLocation, MobEffect> EFFECTS = new LinkedHashMap<>();

    private static final MobEffect LEGACY_SOLDIER_FORMATION_VALUE = effect("soldier_formation", new SoldierFormationMobEffect1201());
    private static final MobEffect WEAPONS_RAISED_VALUE = effect("weapons_raised", new SoldierFormationMobEffect1201());
    private static final MobEffect SHIELDS_RAISED_VALUE = effect("shields_raised", new SoldierFormationMobEffect1201());
    private static final MobEffect HUITZILOPOCHTLI_BLESSING_VALUE = effect("huitzilopochtli_blessing", new SunBlessingMobEffect1201());
    private static final MobEffect OBSIDIAN_WOUND_VALUE = effect("obsidian_wound", new ObsidianWoundMobEffect1201());
    private static final MobEffect BLEEDING_VALUE = effect("bleeding", new BleedingMobEffect1201());

    public static MobEffect LEGACY_SOLDIER_FORMATION;
    public static MobEffect WEAPONS_RAISED;
    public static MobEffect SHIELDS_RAISED;
    public static MobEffect HUITZILOPOCHTLI_BLESSING;
    public static MobEffect OBSIDIAN_WOUND;
    public static MobEffect BLEEDING;

    private ModEffects1201() { }

    public static Map<ResourceLocation, MobEffect> effects() { return Map.copyOf(EFFECTS); }

    public static void bindHolders() {
        LEGACY_SOLDIER_FORMATION = LEGACY_SOLDIER_FORMATION_VALUE;
        WEAPONS_RAISED = WEAPONS_RAISED_VALUE;
        SHIELDS_RAISED = SHIELDS_RAISED_VALUE;
        HUITZILOPOCHTLI_BLESSING = HUITZILOPOCHTLI_BLESSING_VALUE;
        OBSIDIAN_WOUND = OBSIDIAN_WOUND_VALUE;
        BLEEDING = BLEEDING_VALUE;
    }

    private static MobEffect effect(String path, MobEffect effect) {
        EFFECTS.put(EchoWarrior1201.id(path), effect);
        return effect;
    }
}
