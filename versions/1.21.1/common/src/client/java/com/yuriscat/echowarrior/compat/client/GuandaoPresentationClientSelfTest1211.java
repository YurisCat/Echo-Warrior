package com.yuriscat.echowarrior.compat.client;

import com.yuriscat.echowarrior.compat.EchoWarrior1211;
import com.yuriscat.echowarrior.compat.ModContent1211;
import com.yuriscat.echowarrior.compat.entity.GuandaoWarriorEchoEntity1211;
import net.minecraft.client.Minecraft;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationState;


/** Drives the shipped GeckoLib processor and shipped animation, not a second Euler interpolator. */
public final class GuandaoPresentationClientSelfTest1211 {
    private GuandaoPresentationClientSelfTest1211() {}

    public static void run(Minecraft client) {
        try {
            for (boolean walking : new boolean[]{false, true}) {
                for (int packetOffset : new int[]{-2, 0, 2}) checkHandoff(client, walking, packetOffset);
            }
            EchoWarrior1211.LOGGER.info("[Compat1211] GUANDAO PRESENTATION SELFTEST PASSED processor=geckolib root=idle-walk packet-order=three");
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("Guandao animation fixture could not set its detached action state", error);
        }
    }

    private static void checkHandoff(Minecraft client, boolean walking, int packetOffset) throws ReflectiveOperationException {
        var entity = ModContent1211.GUANDAO_WARRIOR_ECHO.create(client.level);
        check(entity != null, "detached warrior exists");
        var model = new GuandaoWarriorModel1211();
        model.getBakedModel(model.getModelResource(entity));
        var processor = model.getAnimationProcessor();
        AnimatableManager<GuandaoWarriorEchoEntity1211> manager = entity.getAnimatableInstanceCache().getManagerForId(entity.getId());
        // Custom, non-Minecraft method: no obfuscated names or production-only reflection aliases.
        var actionSetter = GuandaoWarriorEchoEntity1211.class.getDeclaredMethod("setAnimationAction", byte.class, long.class, long.class);
        actionSetter.setAccessible(true);
        boolean sawComboRotation = false;
        try {
            for (int frame = 0; frame <= 520; frame++) {
                double time = frame * 0.25;
                entity.tickCount = (int)time;
                if (frame == 40) {
                    actionSetter.invoke(entity, GuandaoWarriorEchoEntity1211.ANIMATION_ACTION_COMBO, 10L, 112L);
                    manager.tryTriggerAnimation("action", "combo");
                }
                if (frame == 448 + packetOffset * 4)
                    actionSetter.invoke(entity, GuandaoWarriorEchoEntity1211.ANIMATION_ACTION_NONE, 112L, 0L);
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
                            "root must immediately rest after combo: walking=" + walking + " packetOffset=" + packetOffset + " time=" + time + " root=" + root.getRotX() + "," + root.getRotY() + "," + root.getRotZ());
                    check(Math.abs(body.getRotX()) < 0.3 && Math.abs(body.getRotY()) < 0.3 && Math.abs(body.getRotZ()) < 0.3,
                            "body must not blend through Euler half-turn: walking=" + walking + " packetOffset=" + packetOffset + " time=" + time);
                }
            }
            check(sawComboRotation, "test must actually evaluate combo, not only idle");
        } finally {
            entity.discard();
        }
    }

    private static void check(boolean result, String label) {
        if (!result) throw new IllegalStateException("Guandao presentation: " + label);
    }
}
