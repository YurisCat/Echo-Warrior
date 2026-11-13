package com.yuriscat.echowarrior.client;

import com.yuriscat.echowarrior.EchoWarrior;
import com.yuriscat.echowarrior.item.EchoHeroType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderer;
import java.util.HashSet;

/** Exercise the same detached factory and actual renderer used by the summoner. */
public final class EchoPreviewClientSelfTest {
    private EchoPreviewClientSelfTest() { }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void run(Minecraft client) {
        var ids = new HashSet<Integer>();
        for (var hero : EchoHeroType.values()) {
            for (int repeat = 0; repeat < 2; repeat++) {
                var entity = EchoPreviewFactory.create(client.level, hero);
                try {
                    int id = entity.getId();
                    if (id >= 0 || !ids.add(id) || client.level.getEntity(id) != null)
                        throw new IllegalStateException("Preview identity leaked into world or another preview: " + hero);
                    EntityRenderer renderer = client.getEntityRenderDispatcher().getRenderer(entity);
                    // GeckoLib creates its state from the entity; its parameterless
                    // vanilla compatibility method intentionally returns null.
                    var state = renderer.createRenderState(entity, 0.0F);
                    if (state == null) throw new IllegalStateException("Preview renderer returned no state: " + hero);
                } finally {
                    entity.discard();
                }
            }
        }
        EchoWarrior.LOGGER.info("ECHO PREVIEW CLIENT SELFTEST PASSED heroes=5 instances=10 detached=negative-ids renderer=actual");
    }
}
