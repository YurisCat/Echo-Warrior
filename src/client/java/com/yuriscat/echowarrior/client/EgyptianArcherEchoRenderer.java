package com.yuriscat.echowarrior.client;

import com.geckolib.constant.DataTickets;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.RenderPassInfo;
import com.yuriscat.echowarrior.EchoWarrior;
import com.yuriscat.echowarrior.ModEntities;
import com.yuriscat.echowarrior.entity.EgyptianArcherEchoEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

public final class EgyptianArcherEchoRenderer extends GeoEntityRenderer<EgyptianArcherEchoEntity, EntityRenderState> {
	private static final float EYE_YAW_LIMIT = 34.0F;
	private static final float EYE_PITCH_LIMIT = 24.0F;
	private static final float LOCOMOTION_HEAD_YAW_LIMIT = 15.0F;
	private static final float LOCOMOTION_EYE_YAW_LIMIT = 10.0F;
	private static final float MAX_EYE_X = 0.82F;
	private static final float MAX_EYE_Y = 0.46F;
	private static final float FULL_IDLE_PARENT_COMPENSATION_DEGREES = 3.0F;
	private static final float NO_PARENT_COMPENSATION_DEGREES = 8.0F;
	private static final float RANGED_HEAD_FRAME_RELEASE_GRACE_TICKS = 8.0F;

	private static final DataTicket<Integer> ENTITY_ID = DataTickets.create("echo_warrior_egyptian_entity_id", Integer.class);
	private static final DataTicket<Vec3> ENTITY_POSITION = DataTickets.create("echo_warrior_egyptian_entity_position", Vec3.class);
	private static final DataTicket<Vec3> ATTENTION_POINT = DataTickets.create("echo_warrior_egyptian_attention_point", Vec3.class);
	private static final DataTicket<Vec3> EYE_ATTENTION_POINT = DataTickets.create("echo_warrior_egyptian_eye_attention_point", Vec3.class);
	private static final DataTicket<Float> BODY_YAW = DataTickets.create("echo_warrior_egyptian_body_yaw", Float.class);
	private static final DataTicket<Byte> REACTION = DataTickets.create("echo_warrior_egyptian_reaction", Byte.class);
	private static final DataTicket<Long> GAME_TIME = DataTickets.create("echo_warrior_egyptian_game_time", Long.class);
	private static final DataTicket<Long> BLINK_START = DataTickets.create("echo_warrior_egyptian_blink_start", Long.class);
	private static final DataTicket<Byte> BLINK_COUNT = DataTickets.create("echo_warrior_egyptian_blink_count", Byte.class);
	private static final DataTicket<Byte> CURIOUS_TILT = DataTickets.create("echo_warrior_egyptian_curious_tilt", Byte.class);
	private static final DataTicket<Integer> VISUAL_SEQUENCE = DataTickets.create("echo_warrior_egyptian_visual_sequence", Integer.class);
	private static final DataTicket<Long> ATTENTION_STARTED_AT = DataTickets.create("echo_warrior_egyptian_attention_started_at", Long.class);
	private static final DataTicket<Long> CAUGHT_REACTION_START = DataTickets.create("echo_warrior_egyptian_caught_reaction_start", Long.class);
	private static final DataTicket<Boolean> COMBAT_GAZE_LOCKED = DataTickets.create("echo_warrior_egyptian_combat_gaze_locked", Boolean.class);
	private static final DataTicket<Integer> COMBAT_GAZE_TARGET_ID = DataTickets.create("echo_warrior_egyptian_combat_gaze_target_id", Integer.class);
	private static final DataTicket<Boolean> RANGED_HEAD_FRAME_STABILIZED = DataTickets.create("echo_warrior_egyptian_ranged_head_frame_stabilized", Boolean.class);
	private static final DataTicket<Byte> ACTION_STATE = DataTickets.create("echo_warrior_egyptian_action_state", Byte.class);

	private final Map<Integer, VisualState> visualStates = new HashMap<>();

	public EgyptianArcherEchoRenderer(EntityRendererProvider.Context context) {
		super(context, ModEntities.EGYPTIAN_ARCHER_ECHO);
		this.shadowRadius = 0.45F;
		this.shadowStrength = 0.7F;
	}

	@Override
	public void captureDefaultRenderState(EgyptianArcherEchoEntity entity, Void relatedObject, EntityRenderState renderState, float partialTick) {
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
		renderState.addGeckolibData(COMBAT_GAZE_LOCKED, entity.isCombatGazeLocked());
		renderState.addGeckolibData(COMBAT_GAZE_TARGET_ID, entity.getCombatGazeTargetIdForDiagnostics());
		renderState.addGeckolibData(RANGED_HEAD_FRAME_STABILIZED, entity.isRangedHeadFrameStabilized());
		renderState.addGeckolibData(ACTION_STATE, entity.getActionStateForDiagnostics());
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
		boolean shieldRaised = false;
		long gameTime = renderPass.getGeckolibData(GAME_TIME);
		float partialTick = renderPass.renderState().getPartialTick();
		int sequence = renderPass.getGeckolibData(VISUAL_SEQUENCE);
		float attentionAge = gameTime + partialTick - renderPass.getGeckolibData(ATTENTION_STARTED_AT);
		float caughtReactionAge = gameTime + partialTick - renderPass.getGeckolibData(CAUGHT_REACTION_START);
		boolean combatGazeLocked = renderPass.getGeckolibData(COMBAT_GAZE_LOCKED);
		int combatGazeTargetId = renderPass.getGeckolibData(COMBAT_GAZE_TARGET_ID);
		boolean rangedHeadFrameStabilized = renderPass.getGeckolibData(RANGED_HEAD_FRAME_STABILIZED);
		byte actionState = renderPass.getGeckolibData(ACTION_STATE);

		if (this.visualStates.size() > 256 && !this.visualStates.containsKey(entityId)) {
			this.visualStates.clear();
		}
		VisualState state = this.visualStates.computeIfAbsent(entityId, ignored -> new VisualState());
		float age = renderPass.renderState().ageInTicks;
		float deltaTicks = state.lastAge < 0.0F ? 1.0F : Mth.clamp(age - state.lastAge, 0.0F, 1.0F);
		state.lastAge = age;
		if (rangedHeadFrameStabilized) {
			state.rangedHeadFrameStabilizedUntilAge = age + RANGED_HEAD_FRAME_RELEASE_GRACE_TICKS;
		}
		rangedHeadFrameStabilized = rangedHeadFrameStabilized
				|| age < state.rangedHeadFrameStabilizedUntilAge;

		Vec3 headDelta = attentionPoint.subtract(entityPosition);
		double headHorizontal = Math.sqrt(headDelta.x * headDelta.x + headDelta.z * headDelta.z);
		boolean locomotionGaze = reaction == EgyptianArcherEchoEntity.VISUAL_LOCOMOTION;
		float headYawLimit = locomotionGaze ? LOCOMOTION_HEAD_YAW_LIMIT : 75.0F;
		float desiredHeadWorldYaw = shieldRaised || headHorizontal < 1.0E-4 ? bodyYaw : worldYawToward(headDelta);
		float desiredHeadYaw = shieldRaised ? 0.0F
				: Mth.clamp(Mth.wrapDegrees(desiredHeadWorldYaw - bodyYaw), -headYawLimit, headYawLimit);
		float desiredHeadPitch = shieldRaised || headHorizontal < 1.0E-4 ? 0.0F
				: Mth.clamp(worldPitchToward(headDelta, headHorizontal), -35.0F, 40.0F);
		float desiredTilt = !combatGazeLocked && !shieldRaised && reaction == EgyptianArcherEchoEntity.VISUAL_CURIOUS
				? renderPass.getGeckolibData(CURIOUS_TILT) * 10.0F
				: 0.0F;

		float headResponsiveness;
		if (combatGazeLocked || rangedHeadFrameStabilized) {
			headResponsiveness = 0.58F;
		} else if (shieldRaised) {
			headResponsiveness = 0.45F;
		} else if (reaction == EgyptianArcherEchoEntity.VISUAL_STARTLED || reaction == EgyptianArcherEchoEntity.VISUAL_HURT) {
			headResponsiveness = 0.55F;
		} else if (reaction == EgyptianArcherEchoEntity.VISUAL_CAUGHT) {
			headResponsiveness = 0.36F;
		} else if (reaction == EgyptianArcherEchoEntity.VISUAL_MUTUAL_GAZE) {
			headResponsiveness = attentionAge < 2.0F ? 0.0F : 0.24F;
		} else if (locomotionGaze) {
			headResponsiveness = 0.28F;
		} else {
			headResponsiveness = 0.12F;
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
		float eyeResponsiveness = combatGazeLocked ? 0.88F
				: reaction == EgyptianArcherEchoEntity.VISUAL_STARTLED || reaction == EgyptianArcherEchoEntity.VISUAL_HURT
				? 0.92F
				: reaction == EgyptianArcherEchoEntity.VISUAL_CAUGHT ? 0.9F
				: reaction == EgyptianArcherEchoEntity.VISUAL_MUTUAL_GAZE ? 0.82F : 0.58F;
		state.eyeX = approach(state.eyeX, desiredEyeX, eyeResponsiveness, deltaTicks);
		state.eyeY = approach(state.eyeY, desiredEyeY, eyeResponsiveness, deltaTicks);

		float desiredPupilScale = switch (reaction) {
			case EgyptianArcherEchoEntity.VISUAL_HURT -> 0.6F;
			case EgyptianArcherEchoEntity.VISUAL_STARTLED -> 0.48F;
			case EgyptianArcherEchoEntity.VISUAL_CAUGHT -> Mth.lerp(
					Mth.clamp((caughtReactionAge - 3.0F) / 7.0F, 0.0F, 1.0F),
					0.8F,
					1.0F
			);
			default -> 1.0F;
		};
		state.pupilScale = approach(state.pupilScale, desiredPupilScale, desiredPupilScale < state.pupilScale ? 0.8F : 0.18F, deltaTicks);

		float convergence = eyeHorizontal < 3.0 && eyeHorizontal > 0.1 ? (float)((3.0 - eyeHorizontal) / 3.0) * 0.09F : 0.0F;
		float blink = reaction == EgyptianArcherEchoEntity.VISUAL_STARTLED
				? 0.0F
				: reaction == EgyptianArcherEchoEntity.VISUAL_HURT
						? calculateHurtBlink(gameTime + partialTick, renderPass.getGeckolibData(BLINK_START))
						: calculateBlink(gameTime + partialTick, renderPass.getGeckolibData(BLINK_START), renderPass.getGeckolibData(BLINK_COUNT));

		// The head inherits the modeler's animated torso chain. Gentle non-combat sway can use a
		// scalar blend, but combat needs the exact inverse rotation. Draw/reload contains large
		// rotations on several axes, where subtracting Euler angles is not a valid inverse and can
		// make the head suddenly look aside after repeated shots.
		float inheritedRotX = snapshots.get("Main").map(bone -> bone.getRotX()).orElse(0.0F)
				+ snapshots.get("Upper_Body").map(bone -> bone.getRotX()).orElse(0.0F)
				+ snapshots.get("Upper_Body2").map(bone -> bone.getRotX()).orElse(0.0F);
		float inheritedRotY = snapshots.get("Main").map(bone -> bone.getRotY()).orElse(0.0F)
				+ snapshots.get("Upper_Body").map(bone -> bone.getRotY()).orElse(0.0F)
				+ snapshots.get("Upper_Body2").map(bone -> bone.getRotY()).orElse(0.0F);
		float inheritedRotZ = snapshots.get("Main").map(bone -> bone.getRotZ()).orElse(0.0F)
				+ snapshots.get("Upper_Body").map(bone -> bone.getRotZ()).orElse(0.0F)
				+ snapshots.get("Upper_Body2").map(bone -> bone.getRotZ()).orElse(0.0F);
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
		float localHeadX;
		float localHeadY;
		float localHeadZ;
		if (combatGazeLocked || rangedHeadFrameStabilized) {
			float[] parentRotation = inheritedRotation(snapshots);
			float[] desiredWorldRotation = rotationMatrix(
					toRadians(-state.headPitch), toRadians(-state.headYaw), toRadians(state.headTilt));
			float[] localRotation = multiplyRotation(transposeRotation(parentRotation), desiredWorldRotation);
			float[] localEuler = extractEulerZyx(localRotation);
			localHeadX = localEuler[0];
			localHeadY = localEuler[1];
			localHeadZ = localEuler[2];
		} else {
			localHeadX = toRadians(-state.headPitch) - inheritedRotX * parentCompensation;
			localHeadY = toRadians(-state.headYaw) - inheritedRotY * parentCompensation;
			localHeadZ = toRadians(state.headTilt) - inheritedRotZ * parentCompensation;
		}
		snapshots.ifPresent("Head", bone -> bone.setRotation(localHeadX, localHeadY, localHeadZ));
		snapshots.ifPresent("Eyes_Left", bone -> bone
				.setTranslation(state.eyeX - convergence, state.eyeY, 0.0F)
				.setScale(state.pupilScale, state.pupilScale, 1.0F));
		snapshots.ifPresent("Eyes_Right", bone -> bone
				.setTranslation(state.eyeX + convergence, state.eyeY, 0.0F)
				.setScale(state.pupilScale, state.pupilScale, 1.0F));
		snapshots.ifPresent("Eyebrow", bone -> bone.setTranslation(0.0F, -2.0F * blink, 0.0F));
		registerWorldSpaceGazeDiagnostic(renderPass, state, entityId, gameTime, actionState,
				combatGazeLocked, combatGazeTargetId, attentionPoint);

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

	private static float[] inheritedRotation(BoneSnapshots snapshots) {
		float[] result = identityRotation();
		for (String boneName : new String[] {"Main", "Upper_Body", "Upper_Body2"}) {
			var snapshot = snapshots.get(boneName).orElse(null);
			if (snapshot == null) continue;
			result = multiplyRotation(result, rotationMatrix(
					snapshot.getBone().baseRotX() + snapshot.getRotX(),
					snapshot.getBone().baseRotY() + snapshot.getRotY(),
					snapshot.getBone().baseRotZ() + snapshot.getRotZ()));
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

	private static void registerWorldSpaceGazeDiagnostic(RenderPassInfo<EntityRenderState> renderPass,
			VisualState state, int entityId, long gameTime, byte actionState, boolean combatGazeLocked,
			int targetId, Vec3 attentionPoint) {
		state.diagnosticHeadWorld = null;
		state.diagnosticEyesWorld = null;
		state.diagnosticHatWorld = null;
		state.diagnosticEvaluated = false;
		state.diagnosticEntityId = entityId;
		state.diagnosticGameTime = gameTime;
		state.diagnosticActionState = actionState;
		state.diagnosticCombatLocked = combatGazeLocked;
		state.diagnosticTargetId = targetId;
		state.diagnosticAttentionPoint = attentionPoint;
		renderPass.addBonePositionListener("Head", (world, model, render) -> {
			state.diagnosticHeadWorld = world;
			logWorldSpaceCombatGaze(state);
		});
		renderPass.addBonePositionListener("Eyes", (world, model, render) -> {
			state.diagnosticEyesWorld = world;
			logWorldSpaceCombatGaze(state);
		});
		renderPass.addBonePositionListener("Hat", (world, model, render) -> {
			state.diagnosticHatWorld = world;
			logWorldSpaceCombatGaze(state);
		});
	}

	private static void logWorldSpaceCombatGaze(VisualState state) {
		if (state.diagnosticEvaluated || state.diagnosticHeadWorld == null
				|| state.diagnosticEyesWorld == null || state.diagnosticHatWorld == null) return;
		state.diagnosticEvaluated = true;

		if (!state.diagnosticCombatLocked || state.diagnosticTargetId < 0) {
			state.diagnosticErrorTicks = 0;
			state.lastDiagnosticTargetId = state.diagnosticTargetId;
			return;
		}
		if (state.lastDiagnosticTargetId != state.diagnosticTargetId) {
			state.lastDiagnosticTargetId = state.diagnosticTargetId;
			state.diagnosticTargetChangedAt = state.diagnosticGameTime;
			state.diagnosticErrorTicks = 0;
		}

		Vec3 upAxis = state.diagnosticHatWorld.subtract(state.diagnosticHeadWorld);
		Vec3 eyeAxis = state.diagnosticEyesWorld.subtract(state.diagnosticHeadWorld);
		if (upAxis.lengthSqr() < 1.0E-8 || eyeAxis.lengthSqr() < 1.0E-8) return;
		Vec3 up = upAxis.normalize();
		Vec3 renderedForward = eyeAxis.subtract(up.scale(eyeAxis.dot(up)));
		Vec3 desiredForward = state.diagnosticAttentionPoint.subtract(state.diagnosticHeadWorld);
		if (renderedForward.lengthSqr() < 1.0E-8 || desiredForward.lengthSqr() < 1.0E-8) return;
		renderedForward = renderedForward.normalize();
		desiredForward = desiredForward.normalize();
		float worldError = (float)Math.toDegrees(Math.acos(Mth.clamp(
				(float)renderedForward.dot(desiredForward), -1.0F, 1.0F)));

		boolean settledAfterRetarget = state.diagnosticGameTime - state.diagnosticTargetChangedAt >= 5L;
		if (settledAfterRetarget && worldError > 22.0F) {
			if (state.lastDiagnosticErrorTick != state.diagnosticGameTime) {
				state.lastDiagnosticErrorTick = state.diagnosticGameTime;
				state.diagnosticErrorTicks++;
			}
			if (state.diagnosticErrorTicks >= 2 && state.diagnosticGameTime >= state.nextDiagnosticAt) {
				EchoWarrior.LOGGER.info(
						"[EgyptianArcherGazeClient] archer={} tick={} action={} target={} worldError={} forward={} desired={}",
						state.diagnosticEntityId, state.diagnosticGameTime, state.diagnosticActionState,
						state.diagnosticTargetId, worldError, renderedForward, desiredForward);
				state.nextDiagnosticAt = state.diagnosticGameTime + 10L;
			}
		} else {
			state.diagnosticErrorTicks = 0;
		}
	}

	private static final class VisualState {
		private float headYaw;
		private float headPitch;
		private float headTilt;
		private float eyeX;
		private float eyeY;
		private float pupilScale = 1.0F;
		private float lastAge = -1.0F;
		private float rangedHeadFrameStabilizedUntilAge = Float.NEGATIVE_INFINITY;
		private int lastSequence = -1;
		private Vec3 diagnosticHeadWorld;
		private Vec3 diagnosticEyesWorld;
		private Vec3 diagnosticHatWorld;
		private Vec3 diagnosticAttentionPoint = Vec3.ZERO;
		private int diagnosticEntityId;
		private long diagnosticGameTime;
		private byte diagnosticActionState;
		private boolean diagnosticCombatLocked;
		private int diagnosticTargetId = -1;
		private boolean diagnosticEvaluated;
		private int lastDiagnosticTargetId = -1;
		private long diagnosticTargetChangedAt = Long.MIN_VALUE;
		private long lastDiagnosticErrorTick = Long.MIN_VALUE;
		private int diagnosticErrorTicks;
		private long nextDiagnosticAt;
	}
}
