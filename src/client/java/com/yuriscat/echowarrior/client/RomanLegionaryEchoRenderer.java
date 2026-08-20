package com.yuriscat.echowarrior.client;

import com.geckolib.constant.DataTickets;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.RenderPassInfo;
import com.yuriscat.echowarrior.ModEntities;
import com.yuriscat.echowarrior.entity.RomanLegionaryEchoEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

public final class RomanLegionaryEchoRenderer extends GeoEntityRenderer<RomanLegionaryEchoEntity, EntityRenderState> {
	private static final float EYE_YAW_LIMIT = 34.0F;
	private static final float EYE_PITCH_LIMIT = 24.0F;
	private static final float LOCOMOTION_HEAD_YAW_LIMIT = 15.0F;
	private static final float LOCOMOTION_EYE_YAW_LIMIT = 10.0F;
	private static final float MAX_EYE_X = 0.82F;
	private static final float MAX_EYE_Y = 0.46F;
	private static final float FULL_IDLE_PARENT_COMPENSATION_DEGREES = 3.0F;
	private static final float NO_PARENT_COMPENSATION_DEGREES = 8.0F;

	private static final DataTicket<Integer> ENTITY_ID = DataTickets.create("echo_warrior_entity_id", Integer.class);
	private static final DataTicket<Vec3> ENTITY_POSITION = DataTickets.create("echo_warrior_entity_position", Vec3.class);
	private static final DataTicket<Vec3> ATTENTION_POINT = DataTickets.create("echo_warrior_attention_point", Vec3.class);
	private static final DataTicket<Vec3> EYE_ATTENTION_POINT = DataTickets.create("echo_warrior_eye_attention_point", Vec3.class);
	private static final DataTicket<Float> BODY_YAW = DataTickets.create("echo_warrior_body_yaw", Float.class);
	private static final DataTicket<Byte> REACTION = DataTickets.create("echo_warrior_reaction", Byte.class);
	private static final DataTicket<Long> GAME_TIME = DataTickets.create("echo_warrior_game_time", Long.class);
	private static final DataTicket<Long> BLINK_START = DataTickets.create("echo_warrior_blink_start", Long.class);
	private static final DataTicket<Byte> BLINK_COUNT = DataTickets.create("echo_warrior_blink_count", Byte.class);
	private static final DataTicket<Byte> CURIOUS_TILT = DataTickets.create("echo_warrior_curious_tilt", Byte.class);
	private static final DataTicket<Integer> VISUAL_SEQUENCE = DataTickets.create("echo_warrior_visual_sequence", Integer.class);
	private static final DataTicket<Long> ATTENTION_STARTED_AT = DataTickets.create("echo_warrior_attention_started_at", Long.class);
	private static final DataTicket<Long> CAUGHT_REACTION_START = DataTickets.create("echo_warrior_caught_reaction_start", Long.class);
	private static final DataTicket<Boolean> SHIELD_RAISED = DataTickets.create("echo_warrior_shield_raised", Boolean.class);

	private final Map<Integer, VisualState> visualStates = new HashMap<>();

	public RomanLegionaryEchoRenderer(EntityRendererProvider.Context context) {
		super(context, ModEntities.ROMAN_LEGIONARY_ECHO);
		this.shadowRadius = 0.45F;
		this.shadowStrength = 0.7F;
	}

	@Override
	public void captureDefaultRenderState(RomanLegionaryEchoEntity entity, Void relatedObject, EntityRenderState renderState, float partialTick) {
		super.captureDefaultRenderState(entity, relatedObject, renderState, partialTick);
		renderState.addGeckolibData(ENTITY_ID, entity.getId());
		renderState.addGeckolibData(ENTITY_POSITION, new Vec3(
				Mth.lerp(partialTick, entity.xo, entity.getX()),
				Mth.lerp(partialTick, entity.yo, entity.getY()) + entity.getEyeHeight(),
				Mth.lerp(partialTick, entity.zo, entity.getZ())
		));
		renderState.addGeckolibData(ATTENTION_POINT, entity.getSyncedAttentionPoint());
		renderState.addGeckolibData(EYE_ATTENTION_POINT, entity.getSyncedEyeAttentionPoint());
		renderState.addGeckolibData(BODY_YAW, Mth.rotLerp(partialTick, entity.yBodyRotO, entity.yBodyRot));
		renderState.addGeckolibData(REACTION, entity.getVisualReaction());
		renderState.addGeckolibData(GAME_TIME, entity.level().getGameTime());
		renderState.addGeckolibData(BLINK_START, entity.getBlinkStart());
		renderState.addGeckolibData(BLINK_COUNT, entity.getBlinkCount());
		renderState.addGeckolibData(CURIOUS_TILT, entity.getCuriousTilt());
		renderState.addGeckolibData(VISUAL_SEQUENCE, entity.getVisualSequence());
		renderState.addGeckolibData(ATTENTION_STARTED_AT, entity.getAttentionStartedAt());
		renderState.addGeckolibData(CAUGHT_REACTION_START, entity.getCaughtReactionStart());
		renderState.addGeckolibData(SHIELD_RAISED, entity.isShieldRaised());
	}

	@Override
	public void adjustModelBonesForRender(RenderPassInfo<EntityRenderState> renderPass, BoneSnapshots snapshots) {
		super.adjustModelBonesForRender(renderPass, snapshots);

		int entityId = renderPass.getGeckolibData(ENTITY_ID);
		Vec3 entityPosition = renderPass.getGeckolibData(ENTITY_POSITION);
		Vec3 attentionPoint = renderPass.getGeckolibData(ATTENTION_POINT);
		Vec3 eyeAttentionPoint = renderPass.getGeckolibData(EYE_ATTENTION_POINT);
		float bodyYaw = renderPass.getGeckolibData(BODY_YAW);
		byte reaction = renderPass.getGeckolibData(REACTION);
		boolean shieldRaised = renderPass.getGeckolibData(SHIELD_RAISED);
		long gameTime = renderPass.getGeckolibData(GAME_TIME);
		float partialTick = renderPass.renderState().getPartialTick();
		int sequence = renderPass.getGeckolibData(VISUAL_SEQUENCE);
		float attentionAge = gameTime + partialTick - renderPass.getGeckolibData(ATTENTION_STARTED_AT);
		float caughtReactionAge = gameTime + partialTick - renderPass.getGeckolibData(CAUGHT_REACTION_START);

		if (this.visualStates.size() > 256 && !this.visualStates.containsKey(entityId)) {
			this.visualStates.clear();
		}
		VisualState state = this.visualStates.computeIfAbsent(entityId, ignored -> new VisualState());
		float age = renderPass.renderState().ageInTicks;
		float deltaTicks = state.lastAge < 0.0F ? 1.0F : Mth.clamp(age - state.lastAge, 0.0F, 1.0F);
		state.lastAge = age;

		Vec3 headDelta = attentionPoint.subtract(entityPosition);
		double headHorizontal = Math.sqrt(headDelta.x * headDelta.x + headDelta.z * headDelta.z);
		boolean locomotionGaze = reaction == RomanLegionaryEchoEntity.VISUAL_LOCOMOTION;
		float headYawLimit = locomotionGaze ? LOCOMOTION_HEAD_YAW_LIMIT : 75.0F;
		float desiredHeadWorldYaw = shieldRaised || headHorizontal < 1.0E-4 ? bodyYaw : worldYawToward(headDelta);
		float desiredHeadYaw = shieldRaised ? 0.0F
				: Mth.clamp(Mth.wrapDegrees(desiredHeadWorldYaw - bodyYaw), -headYawLimit, headYawLimit);
		float desiredHeadPitch = shieldRaised || headHorizontal < 1.0E-4 ? 0.0F
				: Mth.clamp(worldPitchToward(headDelta, headHorizontal), -35.0F, 40.0F);
		float desiredTilt = !shieldRaised && reaction == RomanLegionaryEchoEntity.VISUAL_CURIOUS
				? renderPass.getGeckolibData(CURIOUS_TILT) * 10.0F
				: 0.0F;

		float headResponsiveness;
		if (shieldRaised) {
			headResponsiveness = 0.45F;
		} else if (reaction == RomanLegionaryEchoEntity.VISUAL_STARTLED || reaction == RomanLegionaryEchoEntity.VISUAL_HURT) {
			headResponsiveness = 0.55F;
		} else if (reaction == RomanLegionaryEchoEntity.VISUAL_CAUGHT) {
			headResponsiveness = 0.36F;
		} else if (reaction == RomanLegionaryEchoEntity.VISUAL_MUTUAL_GAZE) {
			headResponsiveness = attentionAge < 2.0F ? 0.0F : 0.24F;
		} else if (locomotionGaze) {
			headResponsiveness = 0.28F;
		} else {
			headResponsiveness = 0.18F;
		}
		state.headYaw = approach(state.headYaw, desiredHeadYaw, headResponsiveness, deltaTicks);
		state.headPitch = approach(state.headPitch, desiredHeadPitch, headResponsiveness, deltaTicks);
		state.headTilt = approach(state.headTilt, desiredTilt, 0.16F, deltaTicks);

		// Recalculate the target in the head's current local space every frame. This keeps the
		// pupils fixed on the same world target while the head and body rotate underneath them.
		Vec3 eyeDelta = eyeAttentionPoint.subtract(entityPosition);
		double eyeHorizontal = Math.sqrt(eyeDelta.x * eyeDelta.x + eyeDelta.z * eyeDelta.z);
		float desiredEyeWorldYaw = eyeHorizontal < 1.0E-4 ? desiredHeadWorldYaw : worldYawToward(eyeDelta);
		float desiredEyeWorldPitch = eyeHorizontal < 1.0E-4 ? desiredHeadPitch : worldPitchToward(eyeDelta, eyeHorizontal);
		float eyeYawLimit = locomotionGaze ? LOCOMOTION_EYE_YAW_LIMIT : EYE_YAW_LIMIT;
		float eyeTargetYaw = Mth.clamp(Mth.wrapDegrees(desiredEyeWorldYaw - bodyYaw - state.headYaw), -eyeYawLimit, eyeYawLimit);
		float eyeTargetPitch = Mth.clamp(desiredEyeWorldPitch - state.headPitch, -EYE_PITCH_LIMIT, EYE_PITCH_LIMIT);
		// GeckoLib's model-space X runs opposite Minecraft's semantic look yaw for
		// this model: a target on the echo's left needs a positive pupil translation.
		float unrolledEyeX = -eyeTargetYaw / EYE_YAW_LIMIT * MAX_EYE_X;
		float unrolledEyeY = -eyeTargetPitch / EYE_PITCH_LIMIT * MAX_EYE_Y;

		// The eye bones inherit the curious head roll. Counter-rotate the pupil offset so the
		// apparent gaze remains attached to the world-space target instead of rolling with the face.
		float tiltRadians = toRadians(state.headTilt);
		float tiltCos = Mth.cos(tiltRadians);
		float tiltSin = Mth.sin(tiltRadians);
		float desiredEyeX = unrolledEyeX * tiltCos + unrolledEyeY * tiltSin;
		float desiredEyeY = -unrolledEyeX * tiltSin + unrolledEyeY * tiltCos;
		float eyeResponsiveness = reaction == RomanLegionaryEchoEntity.VISUAL_STARTLED || reaction == RomanLegionaryEchoEntity.VISUAL_HURT
				? 0.92F
				: reaction == RomanLegionaryEchoEntity.VISUAL_CAUGHT ? 0.9F
				: reaction == RomanLegionaryEchoEntity.VISUAL_MUTUAL_GAZE ? 0.82F : 0.58F;
		state.eyeX = approach(state.eyeX, desiredEyeX, eyeResponsiveness, deltaTicks);
		state.eyeY = approach(state.eyeY, desiredEyeY, eyeResponsiveness, deltaTicks);

		float desiredPupilScale = switch (reaction) {
			case RomanLegionaryEchoEntity.VISUAL_HURT -> 0.6F;
			case RomanLegionaryEchoEntity.VISUAL_STARTLED -> 0.48F;
			case RomanLegionaryEchoEntity.VISUAL_CAUGHT -> Mth.lerp(
					Mth.clamp((caughtReactionAge - 3.0F) / 7.0F, 0.0F, 1.0F),
					0.8F,
					1.0F
			);
			default -> 1.0F;
		};
		state.pupilScale = approach(state.pupilScale, desiredPupilScale, desiredPupilScale < state.pupilScale ? 0.8F : 0.18F, deltaTicks);

		float convergence = eyeHorizontal < 3.0 && eyeHorizontal > 0.1 ? (float)((3.0 - eyeHorizontal) / 3.0) * 0.09F : 0.0F;
		float blink = reaction == RomanLegionaryEchoEntity.VISUAL_STARTLED
				? 0.0F
				: reaction == RomanLegionaryEchoEntity.VISUAL_HURT
						? calculateHurtBlink(gameTime + partialTick, renderPass.getGeckolibData(BLINK_START))
						: calculateBlink(gameTime + partialTick, renderPass.getGeckolibData(BLINK_START), renderPass.getGeckolibData(BLINK_COUNT));

		// The head inherits the modeler's animated torso chain. Compensate gentle idle sway so the
		// code-owned gaze remains stable, but smoothly retain larger combat and reaction motions.
		float inheritedRotX = snapshots.get("root").map(bone -> bone.getRotX()).orElse(0.0F)
				+ snapshots.get("body_root").map(bone -> bone.getRotX()).orElse(0.0F)
				+ snapshots.get("upper_body_root").map(bone -> bone.getRotX()).orElse(0.0F)
				+ snapshots.get("upper_body").map(bone -> bone.getRotX()).orElse(0.0F);
		float inheritedRotY = snapshots.get("root").map(bone -> bone.getRotY()).orElse(0.0F)
				+ snapshots.get("body_root").map(bone -> bone.getRotY()).orElse(0.0F)
				+ snapshots.get("upper_body_root").map(bone -> bone.getRotY()).orElse(0.0F)
				+ snapshots.get("upper_body").map(bone -> bone.getRotY()).orElse(0.0F);
		float inheritedRotZ = snapshots.get("root").map(bone -> bone.getRotZ()).orElse(0.0F)
				+ snapshots.get("body_root").map(bone -> bone.getRotZ()).orElse(0.0F)
				+ snapshots.get("upper_body_root").map(bone -> bone.getRotZ()).orElse(0.0F)
				+ snapshots.get("upper_body").map(bone -> bone.getRotZ()).orElse(0.0F);
		float inheritedMagnitude = Math.max(Math.abs(inheritedRotX), Math.max(Math.abs(inheritedRotY), Math.abs(inheritedRotZ)));
		float inheritedMagnitudeDegrees = inheritedMagnitude * Mth.RAD_TO_DEG;
		float parentCompensation = shieldRaised ? 1.0F : 1.0F - (float)Mth.smoothstep(Mth.clamp(
				(inheritedMagnitudeDegrees - FULL_IDLE_PARENT_COMPENSATION_DEGREES)
						/ (NO_PARENT_COMPENSATION_DEGREES - FULL_IDLE_PARENT_COMPENSATION_DEGREES),
				0.0F,
				1.0F
		));

		// This Blockbench model's yaw and pitch axes are opposite Minecraft's semantic head angles.
		// Roll is already authored in the expected direction and remains unchanged.
		snapshots.ifPresent("head", bone -> bone.setRotation(
				toRadians(-state.headPitch) - inheritedRotX * parentCompensation,
				toRadians(-state.headYaw) - inheritedRotY * parentCompensation,
				toRadians(state.headTilt) - inheritedRotZ * parentCompensation
		));
		snapshots.ifPresent("left_eye", bone -> bone
				.setTranslation(state.eyeX - convergence, state.eyeY, 0.0F)
				.setScale(state.pupilScale, state.pupilScale, 1.0F));
		snapshots.ifPresent("right_eye", bone -> bone
				.setTranslation(state.eyeX + convergence, state.eyeY, 0.0F)
				.setScale(state.pupilScale, state.pupilScale, 1.0F));
		snapshots.ifPresent("eyebrows", bone -> bone.setTranslation(0.0F, -2.0F * blink, 0.0F));

		if (state.lastSequence != sequence) {
			state.lastSequence = sequence;
			state.lastAge = age;
		}
	}

	private static float calculateBlink(float now, long blinkStart, byte blinkCount) {
		if (blinkCount <= 0) {
			return 0.0F;
		}
		float result = blinkPulse(now - blinkStart);
		if (blinkCount > 1) {
			result = Math.max(result, blinkPulse(now - blinkStart - 4.0F));
		}
		return result;
	}

	private static float blinkPulse(float elapsed) {
		if (elapsed < 0.0F || elapsed > 3.0F) {
			return 0.0F;
		}
		return Mth.sin(elapsed / 3.0F * (float)Math.PI);
	}

	private static float calculateHurtBlink(float now, long blinkStart) {
		float elapsed = now - blinkStart;
		if (elapsed < 0.0F || elapsed > 6.0F) {
			return 0.0F;
		}
		if (elapsed <= 1.6F) {
			return elapsed / 1.6F;
		}
		if (elapsed <= 2.2F) {
			return 1.0F;
		}
		return 1.0F - (elapsed - 2.2F) / 3.8F;
	}

	private static float approach(float current, float target, float responsiveness, float deltaTicks) {
		float weight = 1.0F - (float)Math.pow(1.0F - responsiveness, Math.max(0.0F, deltaTicks));
		return Mth.lerp(weight, current, target);
	}

	private static float toRadians(float degrees) {
		return degrees * ((float)Math.PI / 180.0F);
	}

	private static float worldYawToward(Vec3 delta) {
		return (float)(Math.atan2(delta.z, delta.x) * 180.0 / Math.PI) - 90.0F;
	}

	private static float worldPitchToward(Vec3 delta, double horizontal) {
		return Mth.clamp((float)(-Math.atan2(delta.y, horizontal) * 180.0 / Math.PI), -35.0F, 40.0F);
	}

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
