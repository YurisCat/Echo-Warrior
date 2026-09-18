package com.yuriscat.echowarrior.compat.client;

import com.yuriscat.echowarrior.compat.ModContent1211;
import com.yuriscat.echowarrior.compat.entity.JapaneseSamuraiEchoEntity1211;
import com.yuriscat.echowarrior.compat.entity.RomanVisualMath1211;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;

import java.util.HashMap;
import java.util.Map;

/** 1.21.1 renderer-side gaze, blink and expression bridge for the Japanese samurai. */
public final class JapaneseSamuraiModel1211 extends GeoModel<JapaneseSamuraiEchoEntity1211> {
    private static final ResourceLocation MODEL = ModContent1211.id(
            "geo/japanese_samurai_echo.geo.json");
    private static final ResourceLocation TEXTURE = ModContent1211.id(
            "textures/entity/japanese_samurai_echo.png");
    private static final ResourceLocation ANIMATION = ModContent1211.id(
            "animations/japanese_samurai_echo.animation.json");
    private final Map<Integer, VisualState> visualStates = new HashMap<>();

    @Override public ResourceLocation getModelResource(JapaneseSamuraiEchoEntity1211 entity) { return MODEL; }
    @Override public ResourceLocation getTextureResource(JapaneseSamuraiEchoEntity1211 entity) { return TEXTURE; }
    @Override public ResourceLocation getAnimationResource(JapaneseSamuraiEchoEntity1211 entity) { return ANIMATION; }

    @Override
    public void setCustomAnimations(JapaneseSamuraiEchoEntity1211 entity, long instanceId,
                                    AnimationState<JapaneseSamuraiEchoEntity1211> animationState) {
        super.setCustomAnimations(entity, instanceId, animationState);
        if (this.visualStates.size() > 256 && !this.visualStates.containsKey(entity.getId())) this.visualStates.clear();
        VisualState state = this.visualStates.computeIfAbsent(entity.getId(), ignored -> new VisualState());
        float partial = animationState.getPartialTick();
        float age = entity.tickCount + partial;
        float delta = state.lastAge < 0.0F ? 1.0F : Mth.clamp(age - state.lastAge, 0.0F, 1.0F);
        state.lastAge = age;

        Vec3 eyeOrigin = new Vec3(Mth.lerp(partial, entity.xo, entity.getX()),
                Mth.lerp(partial, entity.yo, entity.getY()) + entity.getEyeHeight(),
                Mth.lerp(partial, entity.zo, entity.getZ()));
        float bodyYaw = entity.yBodyRotO + Mth.wrapDegrees(entity.yBodyRot - entity.yBodyRotO) * partial;
        byte reaction = entity.getVisualReaction();
        boolean locomotion = reaction == JapaneseSamuraiEchoEntity1211.VISUAL_LOCOMOTION;
        Vec3 headDelta = entity.getSyncedAttentionPoint().subtract(eyeOrigin);
        double horizontal = Math.sqrt(headDelta.x * headDelta.x + headDelta.z * headDelta.z);
        float worldYaw = horizontal < 1.0E-4 ? bodyYaw : RomanVisualMath1211.worldYawToward(headDelta);
        float desiredYaw = Mth.clamp(Mth.wrapDegrees(worldYaw - bodyYaw), locomotion ? -15.0F : -75.0F,
                locomotion ? 15.0F : 75.0F);
        float desiredPitch = horizontal < 1.0E-4 ? 0.0F
                : RomanVisualMath1211.worldPitchToward(headDelta, horizontal);
        float desiredTilt = reaction == JapaneseSamuraiEchoEntity1211.VISUAL_CURIOUS
                ? entity.getCuriousTilt() * 10.0F : 0.0F;
        float responsiveness = reaction == JapaneseSamuraiEchoEntity1211.VISUAL_STARTLED
                || reaction == JapaneseSamuraiEchoEntity1211.VISUAL_HURT ? 0.55F
                : reaction == JapaneseSamuraiEchoEntity1211.VISUAL_CAUGHT ? 0.36F
                : locomotion ? 0.28F : 0.18F;
        state.headYaw = RomanVisualMath1211.approach(state.headYaw, desiredYaw, responsiveness, delta);
        state.headPitch = RomanVisualMath1211.approach(state.headPitch, desiredPitch, responsiveness, delta);
        state.headTilt = RomanVisualMath1211.approach(state.headTilt, desiredTilt, 0.16F, delta);

        Vec3 pupilDelta = entity.getSyncedEyeAttentionPoint().subtract(eyeOrigin);
        double pupilHorizontal = Math.sqrt(pupilDelta.x * pupilDelta.x + pupilDelta.z * pupilDelta.z);
        float pupilWorldYaw = pupilHorizontal < 1.0E-4 ? worldYaw : RomanVisualMath1211.worldYawToward(pupilDelta);
        float pupilWorldPitch = pupilHorizontal < 1.0E-4 ? desiredPitch
                : RomanVisualMath1211.worldPitchToward(pupilDelta, pupilHorizontal);
        float eyeLimit = locomotion ? 10.0F : 34.0F;
        float desiredEyeX = -Mth.clamp(Mth.wrapDegrees(pupilWorldYaw - bodyYaw - state.headYaw),
                -eyeLimit, eyeLimit) / 34.0F * 0.82F;
        float desiredEyeY = -Mth.clamp(pupilWorldPitch - state.headPitch, -24.0F, 24.0F) / 24.0F * 0.46F;
        state.eyeX = RomanVisualMath1211.approach(state.eyeX, desiredEyeX, 0.58F, delta);
        state.eyeY = RomanVisualMath1211.approach(state.eyeY, desiredEyeY, 0.58F, delta);

        float inheritedX = rotation("Main", Axis.X) + rotation("Body", Axis.X) + rotation("Upper_Body2", Axis.X);
        float inheritedY = rotation("Main", Axis.Y) + rotation("Body", Axis.Y) + rotation("Upper_Body2", Axis.Y);
        float inheritedZ = rotation("Main", Axis.Z) + rotation("Body", Axis.Z) + rotation("Upper_Body2", Axis.Z);
        float compensation = RomanVisualMath1211.parentCompensation(
                Math.max(Math.abs(inheritedX), Math.max(Math.abs(inheritedY), Math.abs(inheritedZ))), false);
        getBone("Head").ifPresent(bone -> bone.updateRotation(
                RomanVisualMath1211.toRadians(-state.headPitch) - inheritedX * compensation,
                RomanVisualMath1211.toRadians(-state.headYaw) - inheritedY * compensation,
                RomanVisualMath1211.toRadians(state.headTilt) - inheritedZ * compensation));
        float convergence = pupilHorizontal < 3.0 && pupilHorizontal > 0.1
                ? (float)((3.0 - pupilHorizontal) / 3.0) * 0.09F : 0.0F;
        getBone("Eyes_Left").ifPresent(bone -> bone.updatePosition(state.eyeX - convergence, state.eyeY, 0.0F));
        getBone("Eyes_Right").ifPresent(bone -> bone.updatePosition(state.eyeX + convergence, state.eyeY, 0.0F));
        float now = entity.level().getGameTime() + partial;
        float blink = reaction == JapaneseSamuraiEchoEntity1211.VISUAL_STARTLED ? 0.0F
                : reaction == JapaneseSamuraiEchoEntity1211.VISUAL_HURT
                ? RomanVisualMath1211.calculateHurtBlink(now, entity.getBlinkStart())
                : RomanVisualMath1211.calculateBlink(now, entity.getBlinkStart(), entity.getBlinkCount());
        getBone("Eyebrow").ifPresent(bone -> bone.updatePosition(0.0F, -2.0F * blink, 0.0F));
    }

    private float rotation(String name, Axis axis) {
        GeoBone bone = getAnimationProcessor().getBone(name);
        if (bone == null) return 0.0F;
        return switch (axis) { case X -> bone.getRotX(); case Y -> bone.getRotY(); case Z -> bone.getRotZ(); };
    }

    private enum Axis { X, Y, Z }
    private static final class VisualState {
        float headYaw;
        float headPitch;
        float headTilt;
        float eyeX;
        float eyeY;
        float lastAge = -1.0F;
    }
}
