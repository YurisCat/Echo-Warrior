package com.yuriscat.echowarrior.compat.client;

import com.yuriscat.echowarrior.compat.EchoWarrior1211;
import com.yuriscat.echowarrior.compat.ModContent1211;
import com.yuriscat.echowarrior.compat.entity.EgyptianArcherEchoEntity1211;
import com.yuriscat.echowarrior.compat.entity.RomanVisualMath1211;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;

import java.util.HashMap;
import java.util.Map;

/** 1.21.1 renderer-side gaze, blink and expression bridge for the Egyptian archer. */
public final class EgyptianArcherModel1211 extends GeoModel<EgyptianArcherEchoEntity1211> {
    private static final float RANGED_HEAD_FRAME_RELEASE_GRACE_TICKS = 8.0F;
    private static final ResourceLocation MODEL = ModContent1211.id(
            "geo/egyptian_archer_echo.geo.json");
    private static final ResourceLocation TEXTURE = ModContent1211.id(
            "textures/entity/egyptian_archer_echo.png");
    private static final ResourceLocation ANIMATION = ModContent1211.id(
            "animations/egyptian_archer_echo.animation.json");
    private final Map<Integer, VisualState> visualStates = new HashMap<>();

    @Override public ResourceLocation getModelResource(EgyptianArcherEchoEntity1211 entity) { return MODEL; }
    @Override public ResourceLocation getTextureResource(EgyptianArcherEchoEntity1211 entity) { return TEXTURE; }
    @Override public ResourceLocation getAnimationResource(EgyptianArcherEchoEntity1211 entity) { return ANIMATION; }

    @Override
    public void setCustomAnimations(EgyptianArcherEchoEntity1211 entity, long instanceId,
                                    AnimationState<EgyptianArcherEchoEntity1211> animationState) {
        super.setCustomAnimations(entity, instanceId, animationState);
        if (this.visualStates.size() > 256 && !this.visualStates.containsKey(entity.getId())) this.visualStates.clear();
        VisualState state = this.visualStates.computeIfAbsent(entity.getId(), ignored -> new VisualState());
        logRangedAnimationDiagnostics(entity, instanceId, animationState, state);
        float partial = animationState.getPartialTick();
        float age = entity.tickCount + partial;
        float delta = state.lastAge < 0.0F ? 1.0F : Mth.clamp(age - state.lastAge, 0.0F, 1.0F);
        state.lastAge = age;
        boolean combatGazeLocked = entity.isCombatGazeLocked();
        boolean rangedHeadFrameStabilized = entity.isRangedHeadFrameStabilized();
        if (rangedHeadFrameStabilized) {
            state.rangedHeadFrameStabilizedUntilAge = age + RANGED_HEAD_FRAME_RELEASE_GRACE_TICKS;
        }
        rangedHeadFrameStabilized = rangedHeadFrameStabilized
                || age < state.rangedHeadFrameStabilizedUntilAge;
        getBone("bone7").ifPresent(bone -> bone.setHidden(entity.getActionStateForDiagnostics() == 0));

        Vec3 eyeOrigin = new Vec3(Mth.lerp(partial, entity.xo, entity.getX()),
                Mth.lerp(partial, entity.yo, entity.getY()) + entity.getEyeHeight(),
                Mth.lerp(partial, entity.zo, entity.getZ()));
        float bodyYaw = entity.yBodyRotO + Mth.wrapDegrees(entity.yBodyRot - entity.yBodyRotO) * partial;
        byte reaction = entity.getVisualReaction();
        boolean locomotion = reaction == EgyptianArcherEchoEntity1211.VISUAL_LOCOMOTION;
        Vec3 headDelta = entity.getSyncedAttentionPoint().subtract(eyeOrigin);
        double horizontal = Math.sqrt(headDelta.x * headDelta.x + headDelta.z * headDelta.z);
        float worldYaw = horizontal < 1.0E-4 ? bodyYaw : RomanVisualMath1211.worldYawToward(headDelta);
        float desiredYaw = Mth.clamp(Mth.wrapDegrees(worldYaw - bodyYaw), locomotion ? -15.0F : -75.0F,
                locomotion ? 15.0F : 75.0F);
        float desiredPitch = horizontal < 1.0E-4 ? 0.0F
                : RomanVisualMath1211.worldPitchToward(headDelta, horizontal);
        float desiredTilt = !combatGazeLocked && reaction == EgyptianArcherEchoEntity1211.VISUAL_CURIOUS
                ? entity.getCuriousTilt() * 10.0F : 0.0F;
        float responsiveness = combatGazeLocked || rangedHeadFrameStabilized ? 0.58F
                : reaction == EgyptianArcherEchoEntity1211.VISUAL_STARTLED
                || reaction == EgyptianArcherEchoEntity1211.VISUAL_HURT ? 0.55F
                : reaction == EgyptianArcherEchoEntity1211.VISUAL_CAUGHT ? 0.36F
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
        float eyeResponsiveness = combatGazeLocked ? 0.88F : 0.58F;
        state.eyeX = RomanVisualMath1211.approach(state.eyeX, desiredEyeX, eyeResponsiveness, delta);
        state.eyeY = RomanVisualMath1211.approach(state.eyeY, desiredEyeY, eyeResponsiveness, delta);

        float inheritedX = rotation("Main", Axis.X) + rotation("Upper_Body", Axis.X) + rotation("Upper_Body2", Axis.X);
        float inheritedY = rotation("Main", Axis.Y) + rotation("Upper_Body", Axis.Y) + rotation("Upper_Body2", Axis.Y);
        float inheritedZ = rotation("Main", Axis.Z) + rotation("Upper_Body", Axis.Z) + rotation("Upper_Body2", Axis.Z);
        float compensation = RomanVisualMath1211.parentCompensation(
                Math.max(Math.abs(inheritedX), Math.max(Math.abs(inheritedY), Math.abs(inheritedZ))), false);
        float relaxedHeadX = RomanVisualMath1211.toRadians(-state.headPitch) - inheritedX * compensation;
        float relaxedHeadY = RomanVisualMath1211.toRadians(-state.headYaw) - inheritedY * compensation;
        float relaxedHeadZ = RomanVisualMath1211.toRadians(state.headTilt) - inheritedZ * compensation;
        float[] parentRotation = inheritedRotation();
        float[] desiredWorldRotation = rotationMatrix(
                RomanVisualMath1211.toRadians(-state.headPitch),
                RomanVisualMath1211.toRadians(-state.headYaw),
                RomanVisualMath1211.toRadians(state.headTilt));
        float[] localRotation = multiplyRotation(transposeRotation(parentRotation), desiredWorldRotation);
        float[] exactHead = extractEulerZyx(localRotation);
        boolean exactHeadFrame = combatGazeLocked || rangedHeadFrameStabilized;
        if (exactHeadFrame) state.exactHeadFrameBlend = 1.0F;
        else state.exactHeadFrameBlend = RomanVisualMath1211.approach(
                state.exactHeadFrameBlend, 0.0F, 0.16F, delta);
        getBone("Head").ifPresent(bone -> bone.updateRotation(
                lerpRadians(relaxedHeadX, exactHead[0], state.exactHeadFrameBlend),
                lerpRadians(relaxedHeadY, exactHead[1], state.exactHeadFrameBlend),
                lerpRadians(relaxedHeadZ, exactHead[2], state.exactHeadFrameBlend)));
        float convergence = pupilHorizontal < 3.0 && pupilHorizontal > 0.1
                ? (float)((3.0 - pupilHorizontal) / 3.0) * 0.09F : 0.0F;
        getBone("Eyes_Left").ifPresent(bone -> bone.updatePosition(state.eyeX - convergence, state.eyeY, 0.0F));
        getBone("Eyes_Right").ifPresent(bone -> bone.updatePosition(state.eyeX + convergence, state.eyeY, 0.0F));
        float now = entity.level().getGameTime() + partial;
        float blink = reaction == EgyptianArcherEchoEntity1211.VISUAL_STARTLED ? 0.0F
                : reaction == EgyptianArcherEchoEntity1211.VISUAL_HURT
                ? RomanVisualMath1211.calculateHurtBlink(now, entity.getBlinkStart())
                : RomanVisualMath1211.calculateBlink(now, entity.getBlinkStart(), entity.getBlinkCount());
        getBone("Eyebrow").ifPresent(bone -> bone.updatePosition(0.0F, -2.0F * blink, 0.0F));
    }

    private void logRangedAnimationDiagnostics(EgyptianArcherEchoEntity1211 entity, long instanceId,
                                                AnimationState<EgyptianArcherEchoEntity1211> animationState,
                                                VisualState visualState) {
        if (!EgyptianArcherEchoEntity1211.isRangedAnimationDiagnosticsEnabled()) return;
        AnimatableManager<EgyptianArcherEchoEntity1211> manager =
                entity.getAnimatableInstanceCache().getManagerForId(instanceId);
        AnimationController<EgyptianArcherEchoEntity1211> controller =
                manager.getAnimationControllers().get("action");
        if (controller == null) return;
        byte action = entity.getActionStateForDiagnostics();
        long gameTick = entity.level().getGameTime();
        String stage = controller.getCurrentAnimation() == null
                ? "none" : controller.getCurrentAnimation().animation().name();
        String controllerState = controller.getAnimationState().name();
        boolean rangedAction = action >= 1 && action <= 4;
        boolean boundaryChanged = action != visualState.lastLoggedAction
                || !stage.equals(visualState.lastLoggedStage)
                || !controllerState.equals(visualState.lastLoggedControllerState);
        if (!boundaryChanged && (!rangedAction || gameTick < visualState.nextAnimationDiagnosticTick)) return;
        EchoWarrior1211.LOGGER.info(
                "[EgyptianArcherAnimationClient] archer={} tick={} action={} duration={} interval={} reload={} "
                        + "controllerState={} stage={} triggered={} speed={} modelTick={}",
                entity.getId(), gameTick, entity.getActionNameForDiagnostics(),
                entity.getActionDurationTicksForDiagnostics(), entity.getAttackIntervalForDiagnostics(),
                entity.isReloadStyleForDiagnostics(), controllerState, stage,
                controller.getTriggeredAnimation() != null, controller.getAnimationSpeed(),
                animationState.getAnimationTick());
        visualState.lastLoggedAction = action;
        visualState.lastLoggedStage = stage;
        visualState.lastLoggedControllerState = controllerState;
        visualState.nextAnimationDiagnosticTick = gameTick + 5L;
    }

    private float rotation(String name, Axis axis) {
        GeoBone bone = getAnimationProcessor().getBone(name);
        if (bone == null) return 0.0F;
        return switch (axis) { case X -> bone.getRotX(); case Y -> bone.getRotY(); case Z -> bone.getRotZ(); };
    }

    private float[] inheritedRotation() {
        float[] result = identityRotation();
        for (String boneName : new String[] {"Main", "Upper_Body", "Upper_Body2"}) {
            GeoBone bone = getAnimationProcessor().getBone(boneName);
            if (bone == null) continue;
            result = multiplyRotation(result, rotationMatrix(bone.getRotX(), bone.getRotY(), bone.getRotZ()));
        }
        return result;
    }

    private static float[] identityRotation() {
        return new float[] {1.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 1.0F};
    }

    private static float[] rotationMatrix(float x, float y, float z) {
        float cx = Mth.cos(x), sx = Mth.sin(x);
        float cy = Mth.cos(y), sy = Mth.sin(y);
        float cz = Mth.cos(z), sz = Mth.sin(z);
        return new float[] {
                cz * cy, cz * sy * sx - sz * cx, cz * sy * cx + sz * sx,
                sz * cy, sz * sy * sx + cz * cx, sz * sy * cx - cz * sx,
                -sy, cy * sx, cy * cx
        };
    }

    private static float[] multiplyRotation(float[] first, float[] second) {
        float[] result = new float[9];
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                result[row * 3 + column] = first[row * 3] * second[column]
                        + first[row * 3 + 1] * second[3 + column]
                        + first[row * 3 + 2] * second[6 + column];
            }
        }
        return result;
    }

    private static float[] transposeRotation(float[] matrix) {
        return new float[] {
                matrix[0], matrix[3], matrix[6],
                matrix[1], matrix[4], matrix[7],
                matrix[2], matrix[5], matrix[8]
        };
    }

    private static float[] extractEulerZyx(float[] matrix) {
        float y = (float)Math.asin(Mth.clamp(-matrix[6], -1.0F, 1.0F));
        float x;
        float z;
        if (Math.abs(Mth.cos(y)) > 1.0E-5F) {
            x = (float)Math.atan2(matrix[7], matrix[8]);
            z = (float)Math.atan2(matrix[3], matrix[0]);
        } else {
            x = (float)Math.atan2(-matrix[5], matrix[4]);
            z = 0.0F;
        }
        return new float[] {x, y, z};
    }

    private static float lerpRadians(float from, float to, float weight) {
        float delta = Mth.wrapDegrees((to - from) * Mth.RAD_TO_DEG) * Mth.DEG_TO_RAD;
        return from + delta * Mth.clamp(weight, 0.0F, 1.0F);
    }

    private enum Axis { X, Y, Z }
    private static final class VisualState {
        float headYaw;
        float headPitch;
        float headTilt;
        float eyeX;
        float eyeY;
        float lastAge = -1.0F;
        float rangedHeadFrameStabilizedUntilAge = Float.NEGATIVE_INFINITY;
        float exactHeadFrameBlend;
        byte lastLoggedAction = Byte.MIN_VALUE;
        String lastLoggedStage = "";
        String lastLoggedControllerState = "";
        long nextAnimationDiagnosticTick = Long.MIN_VALUE;
    }
}
