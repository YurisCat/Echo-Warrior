package com.yuriscat.echowarrior.compat.client;

import com.yuriscat.echowarrior.compat.EchoWarrior1201;
import com.yuriscat.echowarrior.compat.ModContent1201;
import com.yuriscat.echowarrior.compat.entity.GuandaoWarriorEchoEntity1201;
import com.yuriscat.echowarrior.compat.entity.GuandaoValorParticles1201;
import net.minecraft.client.Minecraft;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationState;

import java.util.UUID;

/** Drives the shipped GeckoLib processor and shipped animation, not a second Euler interpolator. */
public final class GuandaoPresentationClientSelfTest1201 {
    private GuandaoPresentationClientSelfTest1201() {}

    public static void run(Minecraft client) {
        try {
            for (boolean walking : new boolean[]{false, true}) {
                for (int packetOffset : new int[]{-2, 0, 2}) checkHandoff(client, walking, packetOffset);
            }
            checkParticles();
            EchoWarrior1201.LOGGER.info("[Compat1201] GUANDAO PRESENTATION SELFTEST PASSED processor=geckolib root=idle-walk packet-order=three particles=per-entity-flame-and-sparks preview=no-world-emission");
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("Guandao animation fixture could not set its detached action state", error);
        }
    }

    private static void checkHandoff(Minecraft client, boolean walking, int packetOffset) throws ReflectiveOperationException {
        var entity = ModContent1201.GUANDAO_WARRIOR_ECHO.create(client.level);
        check(entity != null, "detached warrior exists");
        var model = new GuandaoWarriorModel1201();
        model.getBakedModel(model.getModelResource(entity));
        var processor = model.getAnimationProcessor();
        AnimatableManager<GuandaoWarriorEchoEntity1201> manager = entity.getAnimatableInstanceCache().getManagerForId(entity.getId());
        // Custom, non-Minecraft method: no obfuscated names or production-only reflection aliases.
        var actionSetter = GuandaoWarriorEchoEntity1201.class.getDeclaredMethod("setAnimationAction", byte.class, long.class, long.class);
        actionSetter.setAccessible(true);
        boolean sawComboRotation = false;
        try {
            for (int frame = 0; frame <= 520; frame++) {
                double time = frame * 0.25;
                entity.tickCount = (int)time;
                if (frame == 40) {
                    actionSetter.invoke(entity, GuandaoWarriorEchoEntity1201.ANIMATION_ACTION_COMBO, 10L, 112L);
                    manager.tryTriggerAnimation("action", "combo");
                }
                if (frame == 448 + packetOffset * 4)
                    actionSetter.invoke(entity, GuandaoWarriorEchoEntity1201.ANIMATION_ACTION_NONE, 112L, 0L);
                if (frame == 448) manager.stopTriggeredAnimation("action", "combo");
                boolean moving = walking && time >= 112 + packetOffset;
                var state = new AnimationState<>(entity, moving ? (float)time : 0, moving ? 1 : 0,
                        (float)(time - entity.tickCount), moving);
                processor.tickAnimation(entity, model, manager, time, state, true);
                var root = processor.getBone("Main");
                var body = processor.getBone("Body");
                check(root != null && body != null && processor.getBone("WeaponParticleAnchor") != null, "required model bones");
                if (time > 40 && time < 110 && Math.abs(root.getRotX()) > 1) sawComboRotation = true;
                if (time >= Math.max(112, 112 + packetOffset)) {
                    check(Math.abs(root.getRotX()) < 0.01 && Math.abs(root.getRotY()) < 0.01 && Math.abs(root.getRotZ()) < 0.01,
                            "root must immediately rest after combo: walking=" + walking + " packetOffset=" + packetOffset + " time=" + time);
                    check(Math.abs(body.getRotX()) < 0.3 && Math.abs(body.getRotY()) < 0.3 && Math.abs(body.getRotZ()) < 0.3,
                            "body must not blend through Euler half-turn: walking=" + walking + " packetOffset=" + packetOffset + " time=" + time);
                }
            }
            check(sawComboRotation, "test must actually evaluate combo, not only idle");
        } finally {
            entity.discard();
        }
    }

    private static void checkParticles() {
        var budget = new GuandaoValorParticles1201();
        UUID first = UUID.randomUUID(), second = UUID.randomUUID();
        check(budget.claim(first, 6, 5, false).flames() == 0, "GUI preview does not emit world particles");
        var full = budget.claim(first, 6, 5, true);
        check(full.flames() == 3 && full.sparks() == 8, "full stacks keep flame and add eight sparks");
        check(budget.claim(first, 6, 5, true).flames() == 0, "repeat frame does not duplicate emission");
        check(budget.claim(second, 6, 5, true).flames() == 3, "second warrior has independent emission");
        check(budget.claim(first, 7, 5, true).sparks() == 0, "sparks only every six ticks");
        check(budget.claim(first, 8, 0, true).flames() == 0, "zero stacks has no effect");
        for (int stacks = 1; stacks <= 5; stacks++) {
            int interval = new int[]{0, 6, 4, 3, 2, 1}[stacks];
            int count = 0;
            UUID warrior = UUID.randomUUID();
            for (int tick = 0; tick < 60; tick++) {
                if (budget.claim(warrior, tick, stacks, true).flames() > 0) count++;
            }
            check(count == 60 / interval, "mainline density for stack " + stacks);
        }
    }

    private static void check(boolean result, String label) {
        if (!result) throw new IllegalStateException("Guandao presentation: " + label);
    }
}
