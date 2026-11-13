package com.yuriscat.echowarrior.client;

import com.yuriscat.echowarrior.ModEntities;
import com.yuriscat.echowarrior.item.EchoHeroType;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import java.util.concurrent.atomic.AtomicInteger;

/** UI-only entities never enter ClientLevel or acquire a server/network identity. */
public final class EchoPreviewFactory {
    private static final AtomicInteger NEXT_ID = new AtomicInteger(-1);

    private EchoPreviewFactory() { }

    public static LivingEntity create(ClientLevel level, EchoHeroType hero) {
        LivingEntity entity = switch (hero) {
            case ROMAN_LEGIONARY -> ModEntities.ROMAN_LEGIONARY_ECHO.create(level, EntitySpawnReason.LOAD);
            case AZTEC_WARRIOR -> ModEntities.AZTEC_WARRIOR_ECHO.create(level, EntitySpawnReason.LOAD);
            case EGYPTIAN_ARCHER -> ModEntities.EGYPTIAN_ARCHER_ECHO.create(level, EntitySpawnReason.LOAD);
            case GUANDAO_WARRIOR -> ModEntities.GUANDAO_WARRIOR_ECHO.create(level, EntitySpawnReason.LOAD);
            case JAPANESE_SAMURAI -> ModEntities.JAPANESE_SAMURAI_ECHO.create(level, EntitySpawnReason.LOAD);
        };
        if (entity == null) throw new IllegalStateException("Missing registered preview entity: " + hero);
        // Since 26.3, a detached client entity has ID 0 and getId() rejects it.
        // Negative IDs keep animation caches distinct without colliding with server entities.
        entity.setId(NEXT_ID.getAndUpdate(id -> id == Integer.MIN_VALUE ? -1 : id - 1));
        return entity;
    }
}
