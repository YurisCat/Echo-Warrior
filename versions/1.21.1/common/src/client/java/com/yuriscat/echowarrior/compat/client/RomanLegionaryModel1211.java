package com.yuriscat.echowarrior.compat.client;

import com.yuriscat.echowarrior.compat.ModContent1211;
import com.yuriscat.echowarrior.compat.entity.RomanLegionaryEchoEntity1211;
import com.yuriscat.echowarrior.compat.entity.RomanVisualMath1211;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;

import java.util.HashMap;
import java.util.Map;

public final class RomanLegionaryModel1211 extends GeoModel<RomanLegionaryEchoEntity1211> {
    private static final float EYE_YAW_LIMIT = 34.0F;
    private static final float EYE_PITCH_LIMIT = 24.0F;
    private static final float MAX_EYE_X = 0.82F;
    private static final float MAX_EYE_Y = 0.46F;
    private static final ResourceLocation MODEL = ModContent1211.id(
            "geo/roman_legionary_echo.geo.json");
    private static final ResourceLocation TEXTURE = ModContent1211.id(
            "textures/entity/roman_legionary_echo.png");
    private static final ResourceLocation ANIMATION = ModContent1211.id(
            "animations/roman_legionary_echo.animation.json");
    private final Map<Integer, VisualState> visualStates = new HashMap<>();

    @Override
    public ResourceLocation getModelResource(RomanLegionaryEchoEntity1211 animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(RomanLegionaryEchoEntity1211 animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(RomanLegionaryEchoEntity1211 animatable) {
        return ANIMATION;
    }

    @Override
    public void setCustomAnimations(RomanLegionaryEchoEntity1211 entity, long instanceId,
                                    AnimationState<RomanLegionaryEchoEntity1211> animationState) {
        super.setCustomAnimations(entity, instanceId, animationState);
        if (this.visualStates.size() > 256 && !this.visualStates.containsKey(entity.getId())) {
            this.visualStates.clear();
        }
        VisualState state = this.visualStates.computeIfAbsent(entity.getId(), ignored -> new VisualState());
        float partialTick = animationState.getPartialTick();
        float age = entity.tickCount + partialTick;
        float deltaTicks = state.lastAge < 0.0F ? 1.0F : Mth.clamp(age - state.lastAge, 0.0F, 1.0F);
        state.lastAge = age;

        Vec3 entityPosition = new Vec3(Mth.lerp(partialTick, entity.xo, entity.getX()),
                Mth.lerp(partialTick, entity.yo, entity.getY()) + entity.getEyeHeight(),
                Mth.lerp(partialTick, entity.zo, entity.getZ()));
        float bodyYaw = entity.yBodyRotO
                + Mth.wrapDegrees(entity.yBodyRot - entity.yBodyRotO) * partialTick;
        byte reaction = entity.getVisualReaction();
        float visualTime = entity.level().getGameTime() + partialTick;
        float attentionAge = visualTime - entity.getAttentionStartedAt();
        float caughtReactionAge = visualTime - entity.getCaughtReactionStart();
        boolean locomotion = reaction == RomanLegionaryEchoEntity1211.VISUAL_LOCOMOTION;
        boolean shieldRaised = entity.isShieldRaised();

        Vec3 headDelta = entity.getSyncedAttentionPoint().subtract(entityPosition);
        double headHorizontal = Math.sqrt(headDelta.x * headDelta.x + headDelta.z * headDelta.z);
        float desiredHeadWorldYaw = shieldRaised || headHorizontal < 1.0E-4
                ? bodyYaw : RomanVisualMath1211.worldYawToward(headDelta);
        float desiredHeadYaw = shieldRaised ? 0.0F : Mth.clamp(
                Mth.wrapDegrees(desiredHeadWorldYaw - bodyYaw), locomotion ? -15.0F : -75.0F,
                locomotion ? 15.0F : 75.0F);
        float desiredHeadPitch = shieldRaised || headHorizontal < 1.0E-4 ? 0.0F
                : RomanVisualMath1211.worldPitchToward(headDelta, headHorizontal);
        float desiredTilt = !shieldRaised && reaction == RomanLegionaryEchoEntity1211.VISUAL_CURIOUS
                ? entity.getCuriousTilt() * 10.0F : 0.0F;
        float headResponsiveness = shieldRaised ? 0.45F
                : reaction == RomanLegionaryEchoEntity1211.VISUAL_STARTLED
                || reaction == RomanLegionaryEchoEntity1211.VISUAL_HURT ? 0.55F
                : reaction == RomanLegionaryEchoEntity1211.VISUAL_CAUGHT ? 0.36F
                : reaction == RomanLegionaryEchoEntity1211.VISUAL_MUTUAL_GAZE
                ? attentionAge < 2.0F ? 0.0F : 0.24F
                : locomotion ? 0.28F : 0.18F;
        state.headYaw = RomanVisualMath1211.approach(state.headYaw, desiredHeadYaw, headResponsiveness, deltaTicks);
        state.headPitch = RomanVisualMath1211.approach(state.headPitch, desiredHeadPitch, headResponsiveness, deltaTicks);
        state.headTilt = RomanVisualMath1211.approach(state.headTilt, desiredTilt, 0.16F, deltaTicks);

        Vec3 eyeDelta = entity.getSyncedEyeAttentionPoint().subtract(entityPosition);
        double eyeHorizontal = Math.sqrt(eyeDelta.x * eyeDelta.x + eyeDelta.z * eyeDelta.z);
        float eyeWorldYaw = eyeHorizontal < 1.0E-4
                ? desiredHeadWorldYaw : RomanVisualMath1211.worldYawToward(eyeDelta);
        float eyeWorldPitch = eyeHorizontal < 1.0E-4
                ? desiredHeadPitch : RomanVisualMath1211.worldPitchToward(eyeDelta, eyeHorizontal);
        float eyeYawLimit = locomotion ? 10.0F : EYE_YAW_LIMIT;
        float eyeYaw = Mth.clamp(Mth.wrapDegrees(eyeWorldYaw - bodyYaw - state.headYaw),
                -eyeYawLimit, eyeYawLimit);
        float eyePitch = Mth.clamp(eyeWorldPitch - state.headPitch, -EYE_PITCH_LIMIT, EYE_PITCH_LIMIT);
        float unrolledEyeX = -eyeYaw / EYE_YAW_LIMIT * MAX_EYE_X;
        float unrolledEyeY = -eyePitch / EYE_PITCH_LIMIT * MAX_EYE_Y;
        float tiltRadians = RomanVisualMath1211.toRadians(state.headTilt);
        float desiredEyeX = unrolledEyeX * Mth.cos(tiltRadians) + unrolledEyeY * Mth.sin(tiltRadians);
        float desiredEyeY = -unrolledEyeX * Mth.sin(tiltRadians) + unrolledEyeY * Mth.cos(tiltRadians);
        float eyeResponsiveness = reaction == RomanLegionaryEchoEntity1211.VISUAL_STARTLED
                || reaction == RomanLegionaryEchoEntity1211.VISUAL_HURT ? 0.92F
                : reaction == RomanLegionaryEchoEntity1211.VISUAL_CAUGHT ? 0.9F
                : reaction == RomanLegionaryEchoEntity1211.VISUAL_MUTUAL_GAZE ? 0.82F : 0.58F;
        state.eyeX = RomanVisualMath1211.approach(state.eyeX, desiredEyeX, eyeResponsiveness, deltaTicks);
        state.eyeY = RomanVisualMath1211.approach(state.eyeY, desiredEyeY, eyeResponsiveness, deltaTicks);

        float desiredPupilScale = reaction == RomanLegionaryEchoEntity1211.VISUAL_HURT ? 0.6F
                : reaction == RomanLegionaryEchoEntity1211.VISUAL_STARTLED ? 0.48F
                : reaction == RomanLegionaryEchoEntity1211.VISUAL_CAUGHT
                ? Mth.lerp(Mth.clamp((caughtReactionAge - 3.0F) / 7.0F, 0.0F, 1.0F), 0.8F, 1.0F)
                : 1.0F;
        state.pupilScale = RomanVisualMath1211.approach(state.pupilScale, desiredPupilScale,
                desiredPupilScale < state.pupilScale ? 0.8F : 0.18F, deltaTicks);
        float convergence = eyeHorizontal < 3.0 && eyeHorizontal > 0.1
                ? (float)((3.0 - eyeHorizontal) / 3.0) * 0.09F : 0.0F;
        float now = visualTime;
        float blink = reaction == RomanLegionaryEchoEntity1211.VISUAL_STARTLED ? 0.0F
                : reaction == RomanLegionaryEchoEntity1211.VISUAL_HURT
                ? RomanVisualMath1211.calculateHurtBlink(now, entity.getBlinkStart())
                : RomanVisualMath1211.calculateBlink(now, entity.getBlinkStart(), entity.getBlinkCount());

        float inheritedRotX = rotation("root", Axis.X) + rotation("body_root", Axis.X)
                + rotation("upper_body_root", Axis.X) + rotation("upper_body", Axis.X);
        float inheritedRotY = rotation("root", Axis.Y) + rotation("body_root", Axis.Y)
                + rotation("upper_body_root", Axis.Y) + rotation("upper_body", Axis.Y);
        float inheritedRotZ = rotation("root", Axis.Z) + rotation("body_root", Axis.Z)
                + rotation("upper_body_root", Axis.Z) + rotation("upper_body", Axis.Z);
        float largestInherited = Math.max(Math.abs(inheritedRotX),
                Math.max(Math.abs(inheritedRotY), Math.abs(inheritedRotZ)));
        float compensation = RomanVisualMath1211.parentCompensation(largestInherited, shieldRaised);

        getBone("head").ifPresent(bone -> bone.updateRotation(
                RomanVisualMath1211.toRadians(-state.headPitch) - inheritedRotX * compensation,
                RomanVisualMath1211.toRadians(-state.headYaw) - inheritedRotY * compensation,
                RomanVisualMath1211.toRadians(state.headTilt) - inheritedRotZ * compensation));
        getBone("left_eye").ifPresent(bone -> {
            bone.updatePosition(state.eyeX - convergence, state.eyeY, 0.0F);
            bone.updateScale(state.pupilScale, state.pupilScale, 1.0F);
        });
        getBone("right_eye").ifPresent(bone -> {
            bone.updatePosition(state.eyeX + convergence, state.eyeY, 0.0F);
            bone.updateScale(state.pupilScale, state.pupilScale, 1.0F);
        });
        getBone("eyebrows").ifPresent(bone -> bone.updatePosition(0.0F, -2.0F * blink, 0.0F));
        state.lastSequence = entity.getVisualSequence();
    }

    private float rotation(String boneName, Axis axis) {
        GeoBone bone = getAnimationProcessor().getBone(boneName);
        if (bone == null) return 0.0F;
        return switch (axis) {
            case X -> bone.getRotX();
            case Y -> bone.getRotY();
            case Z -> bone.getRotZ();
        };
    }

    private enum Axis { X, Y, Z }

    private static final class VisualState {
        private float headYaw;
        private float headPitch;
        private float headTilt;
        private float eyeX;
        private float eyeY;
        private float pupilScale = 1.0F;
        private float lastAge = -1.0F;
        private int lastSequence = -1;
    }
}
