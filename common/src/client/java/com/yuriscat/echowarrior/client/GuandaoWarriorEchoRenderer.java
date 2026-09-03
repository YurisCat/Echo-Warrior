package com.yuriscat.echowarrior.client;

import com.geckolib.constant.DataTickets;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.RenderPassInfo;
import com.yuriscat.echowarrior.EchoWarrior;
import com.yuriscat.echowarrior.ModEntities;
import com.yuriscat.echowarrior.entity.GuandaoWarriorEchoEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class GuandaoWarriorEchoRenderer extends GeoEntityRenderer<GuandaoWarriorEchoEntity, EntityRenderState> {
	private static final float EYE_YAW_LIMIT = 34.0F;
	private static final float EYE_PITCH_LIMIT = 24.0F;
	private static final float LOCOMOTION_HEAD_YAW_LIMIT = 15.0F;
	private static final float LOCOMOTION_EYE_YAW_LIMIT = 10.0F;
	private static final float MAX_EYE_X = 0.82F;
	private static final float MAX_EYE_Y = 0.46F;
	private static final float FULL_IDLE_PARENT_COMPENSATION_DEGREES = 3.0F;
	private static final float NO_PARENT_COMPENSATION_DEGREES = 8.0F;
	private static final int ANIMATION_BOUNDARY_DIAGNOSTIC_TICKS = 5;
	private static final int MAX_VALOR_PARTICLE_STACKS = 5;
	private static final String[] ANIMATION_BOUNDARY_DIAGNOSTIC_BONES = {
			"Main", "Body", "Upper_Body2", "Arm_Left", "Arm_Right", "Weapon", "Leg_Left", "Leg_Right"
	};

	private static final DataTicket<Integer> ENTITY_ID = DataTickets.create("echo_warrior_guandao_entity_id", Integer.class);
	private static final DataTicket<Vec3> ENTITY_POSITION = DataTickets.create("echo_warrior_guandao_entity_position", Vec3.class);
	private static final DataTicket<Vec3> ATTENTION_POINT = DataTickets.create("echo_warrior_guandao_attention_point", Vec3.class);
	private static final DataTicket<Vec3> EYE_ATTENTION_POINT = DataTickets.create("echo_warrior_guandao_eye_attention_point", Vec3.class);
	private static final DataTicket<Float> BODY_YAW = DataTickets.create("echo_warrior_guandao_body_yaw", Float.class);
	private static final DataTicket<Byte> REACTION = DataTickets.create("echo_warrior_guandao_reaction", Byte.class);
	private static final DataTicket<Long> GAME_TIME = DataTickets.create("echo_warrior_guandao_game_time", Long.class);
	private static final DataTicket<Long> BLINK_START = DataTickets.create("echo_warrior_guandao_blink_start", Long.class);
	private static final DataTicket<Byte> BLINK_COUNT = DataTickets.create("echo_warrior_guandao_blink_count", Byte.class);
	private static final DataTicket<Byte> CURIOUS_TILT = DataTickets.create("echo_warrior_guandao_curious_tilt", Byte.class);
	private static final DataTicket<Integer> VISUAL_SEQUENCE = DataTickets.create("echo_warrior_guandao_visual_sequence", Integer.class);
	private static final DataTicket<Long> ATTENTION_STARTED_AT = DataTickets.create("echo_warrior_guandao_attention_started_at", Long.class);
	private static final DataTicket<Long> CAUGHT_REACTION_START = DataTickets.create("echo_warrior_guandao_caught_reaction_start", Long.class);
	private static final DataTicket<Integer> VALOR_STACKS = DataTickets.create("echo_warrior_guandao_valor_stacks", Integer.class);
	private static final DataTicket<Boolean> ANIMATION_DEBUG_ENABLED = DataTickets.create("echo_warrior_guandao_animation_debug", Boolean.class);
	private static final DataTicket<Byte> ANIMATION_ACTION = DataTickets.create("echo_warrior_guandao_animation_action", Byte.class);
	private static final DataTicket<Long> ANIMATION_ACTION_STARTED_AT = DataTickets.create("echo_warrior_guandao_animation_started_at", Long.class);
	private static final DataTicket<Long> ANIMATION_ACTION_ENDS_AT = DataTickets.create("echo_warrior_guandao_animation_ends_at", Long.class);
	private static final DataTicket<Boolean> MOVEMENT_ANIMATION_ACTIVE = DataTickets.create("echo_warrior_guandao_movement_animation", Boolean.class);

	private final Map<Integer, VisualState> visualStates = new HashMap<>();
	private final Map<Integer, Long> lastParticleTicks = new HashMap<>();

	public GuandaoWarriorEchoRenderer(EntityRendererProvider.Context context) {
		super(context, ModEntities.GUANDAO_WARRIOR_ECHO);
		this.shadowRadius = 0.5F;
		this.shadowStrength = 0.75F;
	}

	@Override
	public void captureDefaultRenderState(
			GuandaoWarriorEchoEntity entity,
			Void relatedObject,
			EntityRenderState renderState,
			float partialTick
	) {
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
		renderState.addGeckolibData(VALOR_STACKS, entity.getValorStacks());
		renderState.addGeckolibData(ANIMATION_DEBUG_ENABLED, entity.isAnimationDebugEnabled());
		renderState.addGeckolibData(ANIMATION_ACTION, entity.getAnimationActionStateForDiagnostics());
		renderState.addGeckolibData(ANIMATION_ACTION_STARTED_AT, entity.getAnimationActionStartedAtForDiagnostics());
		renderState.addGeckolibData(ANIMATION_ACTION_ENDS_AT, entity.getAnimationActionEndsAtForDiagnostics());
		renderState.addGeckolibData(MOVEMENT_ANIMATION_ACTIVE, entity.isMovementAnimationActiveForDiagnostics());
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
		long gameTime = renderPass.getGeckolibData(GAME_TIME);
		float partialTick = renderPass.renderState().getPartialTick();
		int sequence = renderPass.getGeckolibData(VISUAL_SEQUENCE);
		float attentionAge = gameTime + partialTick - renderPass.getGeckolibData(ATTENTION_STARTED_AT);
		float caughtReactionAge = gameTime + partialTick - renderPass.getGeckolibData(CAUGHT_REACTION_START);

		if (this.visualStates.size() > 256 && !this.visualStates.containsKey(entityId)) this.visualStates.clear();
		VisualState state = this.visualStates.computeIfAbsent(entityId, ignored -> new VisualState());
		float age = renderPass.renderState().ageInTicks;
		registerAnimationBoundaryDiagnostic(snapshots, state, entityId, gameTime, age,
				renderPass.getGeckolibData(ANIMATION_DEBUG_ENABLED),
				renderPass.getGeckolibData(ANIMATION_ACTION),
				renderPass.getGeckolibData(ANIMATION_ACTION_STARTED_AT),
				renderPass.getGeckolibData(ANIMATION_ACTION_ENDS_AT),
				renderPass.getGeckolibData(MOVEMENT_ANIMATION_ACTIVE));
		float deltaTicks = state.lastAge < 0.0F ? 1.0F : Mth.clamp(age - state.lastAge, 0.0F, 1.0F);
		state.lastAge = age;

		Vec3 headDelta = attentionPoint.subtract(entityPosition);
		double headHorizontal = Math.sqrt(headDelta.x * headDelta.x + headDelta.z * headDelta.z);
		boolean locomotionGaze = reaction == GuandaoWarriorEchoEntity.VISUAL_LOCOMOTION;
		float headYawLimit = locomotionGaze ? LOCOMOTION_HEAD_YAW_LIMIT : 75.0F;
		float desiredHeadWorldYaw = headHorizontal < 1.0E-4 ? bodyYaw : worldYawToward(headDelta);
		float desiredHeadYaw = Mth.clamp(Mth.wrapDegrees(desiredHeadWorldYaw - bodyYaw), -headYawLimit, headYawLimit);
		float desiredHeadPitch = headHorizontal < 1.0E-4 ? 0.0F
				: Mth.clamp(worldPitchToward(headDelta, headHorizontal), -35.0F, 40.0F);
		float desiredTilt = reaction == GuandaoWarriorEchoEntity.VISUAL_CURIOUS
				? renderPass.getGeckolibData(CURIOUS_TILT) * 10.0F : 0.0F;

		float headResponsiveness;
		if (reaction == GuandaoWarriorEchoEntity.VISUAL_STARTLED
				|| reaction == GuandaoWarriorEchoEntity.VISUAL_HURT) {
			headResponsiveness = 0.55F;
		} else if (reaction == GuandaoWarriorEchoEntity.VISUAL_CAUGHT) {
			headResponsiveness = 0.36F;
		} else if (reaction == GuandaoWarriorEchoEntity.VISUAL_MUTUAL_GAZE) {
			headResponsiveness = attentionAge < 2.0F ? 0.0F : 0.24F;
		} else if (locomotionGaze) {
			headResponsiveness = 0.28F;
		} else {
			headResponsiveness = 0.18F;
		}
		state.headYaw = approach(state.headYaw, desiredHeadYaw, headResponsiveness, deltaTicks);
		state.headPitch = approach(state.headPitch, desiredHeadPitch, headResponsiveness, deltaTicks);
		state.headTilt = approach(state.headTilt, desiredTilt, 0.16F, deltaTicks);

		Vec3 eyeDelta = eyeAttentionPoint.subtract(entityPosition);
		double eyeHorizontal = Math.sqrt(eyeDelta.x * eyeDelta.x + eyeDelta.z * eyeDelta.z);
		float desiredEyeWorldYaw = eyeHorizontal < 1.0E-4 ? desiredHeadWorldYaw : worldYawToward(eyeDelta);
		float desiredEyeWorldPitch = eyeHorizontal < 1.0E-4 ? desiredHeadPitch : worldPitchToward(eyeDelta, eyeHorizontal);
		float eyeYawLimit = locomotionGaze ? LOCOMOTION_EYE_YAW_LIMIT : EYE_YAW_LIMIT;
		float eyeTargetYaw = Mth.clamp(Mth.wrapDegrees(desiredEyeWorldYaw - bodyYaw - state.headYaw), -eyeYawLimit, eyeYawLimit);
		float eyeTargetPitch = Mth.clamp(desiredEyeWorldPitch - state.headPitch, -EYE_PITCH_LIMIT, EYE_PITCH_LIMIT);
		float unrolledEyeX = -eyeTargetYaw / EYE_YAW_LIMIT * MAX_EYE_X;
		float unrolledEyeY = -eyeTargetPitch / EYE_PITCH_LIMIT * MAX_EYE_Y;
		float tiltRadians = toRadians(state.headTilt);
		float tiltCos = Mth.cos(tiltRadians);
		float tiltSin = Mth.sin(tiltRadians);
		float desiredEyeX = unrolledEyeX * tiltCos + unrolledEyeY * tiltSin;
		float desiredEyeY = -unrolledEyeX * tiltSin + unrolledEyeY * tiltCos;
		float eyeResponsiveness = reaction == GuandaoWarriorEchoEntity.VISUAL_STARTLED
				|| reaction == GuandaoWarriorEchoEntity.VISUAL_HURT ? 0.92F
				: reaction == GuandaoWarriorEchoEntity.VISUAL_CAUGHT ? 0.9F
				: reaction == GuandaoWarriorEchoEntity.VISUAL_MUTUAL_GAZE ? 0.82F : 0.58F;
		state.eyeX = approach(state.eyeX, desiredEyeX, eyeResponsiveness, deltaTicks);
		state.eyeY = approach(state.eyeY, desiredEyeY, eyeResponsiveness, deltaTicks);

		float desiredPupilScale = switch (reaction) {
			case GuandaoWarriorEchoEntity.VISUAL_HURT -> 0.6F;
			case GuandaoWarriorEchoEntity.VISUAL_STARTLED -> 0.48F;
			case GuandaoWarriorEchoEntity.VISUAL_CAUGHT -> Mth.lerp(
					Mth.clamp((caughtReactionAge - 3.0F) / 7.0F, 0.0F, 1.0F), 0.8F, 1.0F);
			default -> 1.0F;
		};
		state.pupilScale = approach(state.pupilScale, desiredPupilScale,
				desiredPupilScale < state.pupilScale ? 0.8F : 0.18F, deltaTicks);

		float convergence = eyeHorizontal < 3.0 && eyeHorizontal > 0.1
				? (float)((3.0 - eyeHorizontal) / 3.0) * 0.09F : 0.0F;
		float blink = reaction == GuandaoWarriorEchoEntity.VISUAL_STARTLED ? 0.0F
				: reaction == GuandaoWarriorEchoEntity.VISUAL_HURT
						? calculateHurtBlink(gameTime + partialTick, renderPass.getGeckolibData(BLINK_START))
						: calculateBlink(gameTime + partialTick, renderPass.getGeckolibData(BLINK_START),
						renderPass.getGeckolibData(BLINK_COUNT));

		float inheritedRotX = snapshots.get("Main").map(bone -> bone.getRotX()).orElse(0.0F)
				+ snapshots.get("Body").map(bone -> bone.getRotX()).orElse(0.0F)
				+ snapshots.get("Upper_Body2").map(bone -> bone.getRotX()).orElse(0.0F);
		float inheritedRotY = snapshots.get("Main").map(bone -> bone.getRotY()).orElse(0.0F)
				+ snapshots.get("Body").map(bone -> bone.getRotY()).orElse(0.0F)
				+ snapshots.get("Upper_Body2").map(bone -> bone.getRotY()).orElse(0.0F);
		float inheritedRotZ = snapshots.get("Main").map(bone -> bone.getRotZ()).orElse(0.0F)
				+ snapshots.get("Body").map(bone -> bone.getRotZ()).orElse(0.0F)
				+ snapshots.get("Upper_Body2").map(bone -> bone.getRotZ()).orElse(0.0F);
		float inheritedMagnitude = Math.max(Math.abs(inheritedRotX), Math.max(Math.abs(inheritedRotY), Math.abs(inheritedRotZ)));
		float inheritedMagnitudeDegrees = inheritedMagnitude * Mth.RAD_TO_DEG;
		float parentCompensation = 1.0F - (float)Mth.smoothstep(Mth.clamp(
				(inheritedMagnitudeDegrees - FULL_IDLE_PARENT_COMPENSATION_DEGREES)
						/ (NO_PARENT_COMPENSATION_DEGREES - FULL_IDLE_PARENT_COMPENSATION_DEGREES), 0.0F, 1.0F));

		snapshots.ifPresent("Head", bone -> bone.setRotation(
				toRadians(-state.headPitch) - inheritedRotX * parentCompensation,
				toRadians(-state.headYaw) - inheritedRotY * parentCompensation,
				toRadians(state.headTilt) - inheritedRotZ * parentCompensation));
		snapshots.ifPresent("Eyes_Left", bone -> bone
				.setTranslation(state.eyeX - convergence, state.eyeY, 0.0F)
				.setScale(state.pupilScale, state.pupilScale, 1.0F));
		snapshots.ifPresent("Eyes_Right", bone -> bone
				.setTranslation(state.eyeX + convergence, state.eyeY, 0.0F)
				.setScale(state.pupilScale, state.pupilScale, 1.0F));
		snapshots.ifPresent("Eyebrow", bone -> bone.setTranslation(0.0F, -2.0F * blink, 0.0F));

		if (state.lastSequence != sequence) {
			state.lastSequence = sequence;
			state.lastAge = age;
		}
		addValorParticles(renderPass, entityId, gameTime);
	}

	private static void registerAnimationBoundaryDiagnostic(
			BoneSnapshots snapshots,
			VisualState state,
			int entityId,
			long gameTime,
			float age,
			boolean debugEnabled,
			byte action,
			long actionStartedAt,
			long actionEndsAt,
			boolean movementAnimationActive
	) {
		byte previousAction = state.lastAnimationDiagnosticAction;
		boolean currentCommitted = isCommittedAction(action);
		boolean previousCommitted = isCommittedAction(previousAction);
		if (!debugEnabled) {
			state.lastAnimationDiagnosticAction = action;
			state.animationDiagnosticUntil = Long.MIN_VALUE;
			state.lastAnimationDiagnosticAge = Float.NEGATIVE_INFINITY;
			state.lastAnimationBonePoses.clear();
			return;
		}

		if (currentCommitted && actionEndsAt - gameTime <= ANIMATION_BOUNDARY_DIAGNOSTIC_TICKS) {
			state.animationDiagnosticContext = action;
			state.animationDiagnosticUntil = Math.max(state.animationDiagnosticUntil,
					actionEndsAt + ANIMATION_BOUNDARY_DIAGNOSTIC_TICKS);
		}
		if (action != previousAction && previousCommitted) {
			state.animationDiagnosticContext = previousAction;
			state.animationDiagnosticUntil = Math.max(state.animationDiagnosticUntil,
					gameTime + ANIMATION_BOUNDARY_DIAGNOSTIC_TICKS);
		}
		if (gameTime > state.animationDiagnosticUntil) {
			state.lastAnimationBonePoses.clear();
			state.lastAnimationDiagnosticAge = Float.NEGATIVE_INFINITY;
		}
		state.lastAnimationDiagnosticAction = action;
		if (gameTime > state.animationDiagnosticUntil
				|| Math.abs(age - state.lastAnimationDiagnosticAge) < 1.0E-4F) return;
		state.lastAnimationDiagnosticAge = age;

		StringBuilder poses = new StringBuilder(720);
		String maxDeltaBone = "none";
		float maxRotationDelta = 0.0F;
		float maxTranslationDelta = 0.0F;
		float maxScaleDelta = 0.0F;
		boolean maxVisibilityChanged = false;
		float maxDeltaScore = 0.0F;
		for (String boneName : ANIMATION_BOUNDARY_DIAGNOSTIC_BONES) {
			var snapshot = snapshots.get(boneName).orElse(null);
			if (snapshot == null) continue;
			BoneDiagnosticPose current = new BoneDiagnosticPose(
					snapshot.getRotX(), snapshot.getRotY(), snapshot.getRotZ(),
					snapshot.getTranslateX(), snapshot.getTranslateY(), snapshot.getTranslateZ(),
					snapshot.getScaleX(), snapshot.getScaleY(), snapshot.getScaleZ(),
					snapshot.isHidden(), snapshot.areChildrenHidden());
			BoneDiagnosticPose previous = state.lastAnimationBonePoses.put(boneName, current);
			if (previous != null) {
				float rotationDelta = current.maximumRotationDeltaDegrees(previous);
				float translationDelta = current.translationDelta(previous);
				float scaleDelta = current.maximumScaleDelta(previous);
				boolean visibilityChanged = current.visibilityChanged(previous);
				float score = Math.max(Math.max(rotationDelta / 10.0F, translationDelta / 0.25F),
						Math.max(scaleDelta / 0.1F, visibilityChanged ? 100.0F : 0.0F));
				if (score > maxDeltaScore) {
					maxDeltaScore = score;
					maxDeltaBone = boneName;
					maxRotationDelta = rotationDelta;
					maxTranslationDelta = translationDelta;
					maxScaleDelta = scaleDelta;
					maxVisibilityChanged = visibilityChanged;
				}
			}
			if (!poses.isEmpty()) poses.append(';');
			poses.append(boneName).append('=').append(String.format(Locale.ROOT,
					"r(%.1f,%.1f,%.1f)p(%.2f,%.2f,%.2f)s(%.2f,%.2f,%.2f)h(%s,%s)",
					current.rotX() * Mth.RAD_TO_DEG, current.rotY() * Mth.RAD_TO_DEG,
					current.rotZ() * Mth.RAD_TO_DEG,
					current.translateX(), current.translateY(), current.translateZ(),
					current.scaleX(), current.scaleY(), current.scaleZ(),
					current.hidden(), current.childrenHidden()));
		}

		EchoWarrior.LOGGER.info(
				"[GuandaoAnimationBonesClient] warrior={} tick={} age={} context={} action={} previousAction={} "
						+ "start={} end={} movementBase={} maxBone={} maxRotDeg={} maxPos={} maxScale={} "
						+ "visibilityChanged={} poses={}",
				entityId, gameTime, String.format(Locale.ROOT, "%.3f", age),
				animationActionName(state.animationDiagnosticContext), animationActionName(action),
				animationActionName(previousAction), actionStartedAt, actionEndsAt, movementAnimationActive,
				maxDeltaBone, String.format(Locale.ROOT, "%.2f", maxRotationDelta),
				String.format(Locale.ROOT, "%.3f", maxTranslationDelta),
				String.format(Locale.ROOT, "%.3f", maxScaleDelta), maxVisibilityChanged, poses);
	}

	private static boolean isCommittedAction(byte action) {
		return action == GuandaoWarriorEchoEntity.ANIMATION_ACTION_ATTACK
				|| action == GuandaoWarriorEchoEntity.ANIMATION_ACTION_COMBO;
	}

	private static String animationActionName(byte action) {
		return switch (action) {
			case GuandaoWarriorEchoEntity.ANIMATION_ACTION_ATTACK -> "attack";
			case GuandaoWarriorEchoEntity.ANIMATION_ACTION_COMBO -> "combo";
			case GuandaoWarriorEchoEntity.ANIMATION_ACTION_HURT -> "hurt";
			default -> "none";
		};
	}

	private record BoneDiagnosticPose(
			float rotX, float rotY, float rotZ,
			float translateX, float translateY, float translateZ,
			float scaleX, float scaleY, float scaleZ,
			boolean hidden, boolean childrenHidden
	) {
		private float maximumRotationDeltaDegrees(BoneDiagnosticPose previous) {
			float x = Math.abs(Mth.wrapDegrees((this.rotX - previous.rotX) * Mth.RAD_TO_DEG));
			float y = Math.abs(Mth.wrapDegrees((this.rotY - previous.rotY) * Mth.RAD_TO_DEG));
			float z = Math.abs(Mth.wrapDegrees((this.rotZ - previous.rotZ) * Mth.RAD_TO_DEG));
			return Math.max(x, Math.max(y, z));
		}

		private float translationDelta(BoneDiagnosticPose previous) {
			float x = this.translateX - previous.translateX;
			float y = this.translateY - previous.translateY;
			float z = this.translateZ - previous.translateZ;
			return Mth.sqrt(x * x + y * y + z * z);
		}

		private float maximumScaleDelta(BoneDiagnosticPose previous) {
			return Math.max(Math.abs(this.scaleX - previous.scaleX),
					Math.max(Math.abs(this.scaleY - previous.scaleY), Math.abs(this.scaleZ - previous.scaleZ)));
		}

		private boolean visibilityChanged(BoneDiagnosticPose previous) {
			return this.hidden != previous.hidden || this.childrenHidden != previous.childrenHidden;
		}
	}

	private void addValorParticles(RenderPassInfo<EntityRenderState> renderPass, int entityId, long gameTime) {
		int stacks = renderPass.getGeckolibData(VALOR_STACKS);
		if (stacks <= 0) return;
		int interval = switch (stacks) {
			case 1 -> 6;
			case 2 -> 4;
			case 3 -> 3;
			case 4 -> 2;
			default -> 1;
		};
		if (gameTime % interval != 0 || this.lastParticleTicks.getOrDefault(entityId, Long.MIN_VALUE) == gameTime) return;
		if (this.lastParticleTicks.size() > 256 && !this.lastParticleTicks.containsKey(entityId)) this.lastParticleTicks.clear();
		this.lastParticleTicks.put(entityId, gameTime);

		ParticleOptions particle = ParticleTypes.FLAME;
		int count = stacks >= 5 ? 3 : stacks >= 4 ? 2 : 1;
		renderPass.addBonePositionListener("WeaponParticleAnchor", (worldPosition, modelPosition, localPosition) -> {
			var level = Minecraft.getInstance().level;
			if (level == null || worldPosition == null) return;
			var random = level.getRandom();
			for (int index = 0; index < count; index++) {
				double spread = stacks >= 3 ? 0.055 : 0.035;
				level.addParticle(particle,
						worldPosition.x + (random.nextDouble() - 0.5) * spread,
						worldPosition.y + (random.nextDouble() - 0.5) * spread,
						worldPosition.z + (random.nextDouble() - 0.5) * spread,
						(random.nextDouble() - 0.5) * 0.015,
						0.012 + random.nextDouble() * 0.018,
						(random.nextDouble() - 0.5) * 0.015);
			}
			if (stacks >= MAX_VALOR_PARTICLE_STACKS && gameTime % 6L == 0L) {
				double phase = random.nextDouble() * Math.PI * 2.0;
				for (int index = 0; index < 8; index++) {
					double angle = phase + index * Math.PI * 0.25;
					double radialX = Math.cos(angle);
					double radialZ = Math.sin(angle);
					ParticleOptions spark = new DustParticleOptions(index % 2 == 0 ? 0xE43A1A : 0xFF9A24, 0.72F);
					level.addParticle(spark,
							worldPosition.x + radialX * 0.045,
							worldPosition.y + (random.nextDouble() - 0.5) * 0.045,
							worldPosition.z + radialZ * 0.045,
							radialX * 0.025,
							0.018 + random.nextDouble() * 0.012,
							radialZ * 0.025);
				}
			}
		});
	}

	private static float calculateBlink(float now, long blinkStart, byte blinkCount) {
		if (blinkCount <= 0) return 0.0F;
		float result = blinkPulse(now - blinkStart);
		if (blinkCount > 1) result = Math.max(result, blinkPulse(now - blinkStart - 4.0F));
		return result;
	}

	private static float blinkPulse(float elapsed) {
		if (elapsed < 0.0F || elapsed > 3.0F) return 0.0F;
		return Mth.sin(elapsed / 3.0F * (float)Math.PI);
	}

	private static float calculateHurtBlink(float now, long blinkStart) {
		float elapsed = now - blinkStart;
		if (elapsed < 0.0F || elapsed > 6.0F) return 0.0F;
		if (elapsed <= 1.6F) return elapsed / 1.6F;
		if (elapsed <= 2.2F) return 1.0F;
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
		private final Map<String, BoneDiagnosticPose> lastAnimationBonePoses = new HashMap<>();
		private byte lastAnimationDiagnosticAction = -1;
		private byte animationDiagnosticContext = GuandaoWarriorEchoEntity.ANIMATION_ACTION_NONE;
		private long animationDiagnosticUntil = Long.MIN_VALUE;
		private float lastAnimationDiagnosticAge = Float.NEGATIVE_INFINITY;
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
