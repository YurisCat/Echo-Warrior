package com.yuriscat.echowarrior.compat.combat;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Creeper;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class ShieldChargeCreeperControl1211 {
    private static final Map<UUID, Long> DISORIENTED_UNTIL = new HashMap<>();

    private ShieldChargeCreeperControl1211() {
    }

    public static void disorient(ServerLevel level, Creeper creeper, long durationTicks) {
        long until = level.getGameTime() + Math.max(1L, durationTicks);
        DISORIENTED_UNTIL.merge(creeper.getUUID(), until, Math::max);
        suppress(creeper);
    }

    public static void tick(ServerLevel level) {
        long now = level.getGameTime();
        Iterator<Map.Entry<UUID, Long>> iterator = DISORIENTED_UNTIL.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Long> entry = iterator.next();
            if (now >= entry.getValue()) {
                iterator.remove();
                continue;
            }
            Entity found = level.getEntity(entry.getKey());
            if (found instanceof Creeper creeper && creeper.isAlive()) suppress(creeper);
        }
    }

    public static boolean isDisoriented(Creeper creeper, long now) {
        Long until = DISORIENTED_UNTIL.get(creeper.getUUID());
        return until != null && now < until;
    }

    public static void enforceForSelfTest(Creeper creeper) {
        suppress(creeper);
    }

    public static void clearForSelfTest(UUID creeperId) {
        DISORIENTED_UNTIL.remove(creeperId);
    }

    private static void suppress(Creeper creeper) {
        creeper.setTarget(null);
        creeper.setSwellDir(-1);
    }
}
