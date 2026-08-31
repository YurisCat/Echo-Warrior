package com.yuriscat.echowarrior.client;

import com.geckolib.animation.state.BoneSnapshot;
import com.geckolib.cache.model.GeoBone;
import com.geckolib.constant.DataTickets;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.RenderPassInfo;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.yuriscat.echowarrior.EchoWarrior;
import com.yuriscat.echowarrior.ModEntities;
import com.yuriscat.echowarrior.entity.JapaneseSamuraiEchoEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Renders frozen GeckoLib bone-pose afterimages. Phase two reuses the same
 * snapshots for model-anchored dissolve and optional coloring. The proposed
 * outline pass remains disabled until it can use a real post-process instead
 * of turning custom GeckoLib geometry into a filled silhouette.
 */
public final class JapaneseSamuraiEchoRenderer
		extends GeoEntityRenderer<JapaneseSamuraiEchoEntity, EntityRenderState> {
	private static final float EYE_YAW_LIMIT = 34.0F;
	private static final float EYE_PITCH_LIMIT = 24.0F;
	private static final float LOCOMOTION_HEAD_YAW_LIMIT = 15.0F;
	private static final float LOCOMOTION_EYE_YAW_LIMIT = 10.0F;
	private static final float MAX_EYE_X = 0.82F;
	private static final float MAX_EYE_Y = 0.46F;
	private static final float FULL_IDLE_PARENT_COMPENSATION_DEGREES = 3.0F;
	private static final float NO_PARENT_COMPENSATION_DEGREES = 8.0F;
	private static final int PER_SAMURAI_LIMIT = 8;
	private static final int FALLBACK_THRESHOLD = 48;
	private static final int SOFT_LIMIT = 64;
	private static final int HARD_LIMIT = 96;
	private static final int ZANSHIN_RGB = 0x38BFEF;
	private static final int FUMIKOMI_RGB = 0xE9A72F;
	private static final float DISSOLVE_HOLD_TICKS = 2.0F;
	private static final byte PASS_NONE = 0;
	private static final byte PASS_PHASE_ONE = 1;
	private static final byte PASS_ADVANCED_BASE = 2;
	private static final Identifier SAMURAI_TEXTURE = EchoWarrior.id(
			"textures/entity/japanese_samurai_echo.png");
	private static final Identifier ADVANCED_DETAIL_TEXTURE = EchoWarrior.id(
			"textures/entity/japanese_samurai_afterimage_detail.png");
	private static final Identifier DISSOLVE_MASK = EchoWarrior.id(
			"textures/effect/samurai_afterimage_dissolve.png");
	private static final RenderPipeline ADVANCED_NEUTRAL_PIPELINE = createAdvancedPipeline(
			"samurai_afterimage_neutral", 0xFFFFFF);
	private static final RenderPipeline ADVANCED_ZANSHIN_PIPELINE = createAdvancedPipeline(
			"samurai_afterimage_zanshin", ZANSHIN_RGB);
	private static final RenderPipeline ADVANCED_FUMIKOMI_PIPELINE = createAdvancedPipeline(
			"samurai_afterimage_fumikomi", FUMIKOMI_RGB);
	private static final RenderType ADVANCED_NEUTRAL_RENDER_TYPE = createAdvancedRenderType(
			"samurai_afterimage_neutral", ADVANCED_NEUTRAL_PIPELINE, SAMURAI_TEXTURE);
	private static final RenderType ADVANCED_ZANSHIN_RENDER_TYPE = createAdvancedRenderType(
			"samurai_afterimage_zanshin", ADVANCED_ZANSHIN_PIPELINE, ADVANCED_DETAIL_TEXTURE);
	private static final RenderType ADVANCED_FUMIKOMI_RENDER_TYPE = createAdvancedRenderType(
			"samurai_afterimage_fumikomi", ADVANCED_FUMIKOMI_PIPELINE, ADVANCED_DETAIL_TEXTURE);

	private static final DataTicket<Integer> ENTITY_ID = DataTickets.create(
			"echo_warrior_samurai_afterimage_entity_id", Integer.class);
	private static final DataTicket<Vec3> ENTITY_POSITION = DataTickets.create(
			"echo_warrior_samurai_afterimage_entity_position", Vec3.class);
	private static final DataTicket<Vec3> VISUAL_ENTITY_POSITION = DataTickets.create(
			"echo_warrior_samurai_visual_entity_position", Vec3.class);
	private static final DataTicket<Vec3> ATTENTION_POINT = DataTickets.create(
			"echo_warrior_samurai_attention_point", Vec3.class);
	private static final DataTicket<Vec3> EYE_ATTENTION_POINT = DataTickets.create(
			"echo_warrior_samurai_eye_attention_point", Vec3.class);
	private static final DataTicket<Float> BODY_YAW = DataTickets.create(
			"echo_warrior_samurai_afterimage_body_yaw", Float.class);
	private static final DataTicket<Long> GAME_TIME = DataTickets.create(
			"echo_warrior_samurai_afterimage_game_time", Long.class);
	private static final DataTicket<Byte> REACTION = DataTickets.create(
			"echo_warrior_samurai_reaction", Byte.class);
	private static final DataTicket<Long> BLINK_START = DataTickets.create(
			"echo_warrior_samurai_blink_start", Long.class);
	private static final DataTicket<Byte> BLINK_COUNT = DataTickets.create(
			"echo_warrior_samurai_blink_count", Byte.class);
	private static final DataTicket<Byte> CURIOUS_TILT = DataTickets.create(
			"echo_warrior_samurai_curious_tilt", Byte.class);
	private static final DataTicket<Integer> VISUAL_SEQUENCE = DataTickets.create(
			"echo_warrior_samurai_visual_sequence", Integer.class);
	private static final DataTicket<Long> ATTENTION_STARTED_AT = DataTickets.create(
			"echo_warrior_samurai_attention_started_at", Long.class);
	private static final DataTicket<Long> CAUGHT_REACTION_START = DataTickets.create(
			"echo_warrior_samurai_caught_reaction_start", Long.class);
	private static final DataTicket<Byte> ACTION_STATE = DataTickets.create(
			"echo_warrior_samurai_action_state", Byte.class);
	private static final DataTicket<Integer> EVENT_SEQUENCE = DataTickets.create(
			"echo_warrior_samurai_afterimage_sequence", Integer.class);
	private static final DataTicket<Byte> EVENT_KIND = DataTickets.create(
			"echo_warrior_samurai_afterimage_kind", Byte.class);
	private static final DataTicket<Vec3> EVENT_ORIGIN = DataTickets.create(
			"echo_warrior_samurai_afterimage_origin", Vec3.class);
	private static final DataTicket<Vec3> EVENT_DIRECTION = DataTickets.create(
			"echo_warrior_samurai_afterimage_direction", Vec3.class);
	private static final DataTicket<Float> EVENT_YAW = DataTickets.create(
			"echo_warrior_samurai_afterimage_yaw", Float.class);
	private static final DataTicket<Boolean> EVENT_NEUTRAL = DataTickets.create(
			"echo_warrior_samurai_afterimage_neutral", Boolean.class);
	private static final DataTicket<Boolean> EVENT_ADVANCED = DataTickets.create(
			"echo_warrior_samurai_afterimage_advanced", Boolean.class);
	private static final DataTicket<Byte> AFTERIMAGE_PASS_MODE = DataTickets.create(
			"echo_warrior_samurai_afterimage_pass_mode", Byte.class);
	private static final DataTicket<Boolean> AFTERIMAGE_PASS_NEUTRAL = DataTickets.create(
			"echo_warrior_samurai_afterimage_pass_neutral", Boolean.class);
	private static final DataTicket<Byte> AFTERIMAGE_PASS_KIND = DataTickets.create(
			"echo_warrior_samurai_afterimage_pass_kind", Byte.class);

	private final Map<Integer, Integer> lastSequences = new HashMap<>();
	private final Map<Integer, VisualState> visualStates = new HashMap<>();
	private final List<Afterimage> afterimages = new ArrayList<>();
	private long retryAdvancedRenderingAt = Long.MIN_VALUE;

	public JapaneseSamuraiEchoRenderer(EntityRendererProvider.Context context) {
		super(context, ModEntities.JAPANESE_SAMURAI_ECHO);
		this.shadowRadius = 0.45F;
		this.shadowStrength = 0.7F;
	}

	@Override
	public void captureDefaultRenderState(
			JapaneseSamuraiEchoEntity entity,
			Void relatedObject,
			EntityRenderState renderState,
			float partialTick
	) {
		super.captureDefaultRenderState(entity, relatedObject, renderState, partialTick);
		renderState.addGeckolibData(ENTITY_ID, entity.getId());
		renderState.addGeckolibData(ENTITY_POSITION, new Vec3(
				Mth.lerp(partialTick, entity.xo, entity.getX()),
				Mth.lerp(partialTick, entity.yo, entity.getY()),
				Mth.lerp(partialTick, entity.zo, entity.getZ())
		));
		renderState.addGeckolibData(VISUAL_ENTITY_POSITION, new Vec3(
				Mth.lerp(partialTick, entity.xo, entity.getX()),
				Mth.lerp(partialTick, entity.yo, entity.getY()) + entity.getEyeHeight(),
				Mth.lerp(partialTick, entity.zo, entity.getZ())
		));
		renderState.addGeckolibData(ATTENTION_POINT, entity.getSyncedAttentionPoint());
		renderState.addGeckolibData(EYE_ATTENTION_POINT, entity.getSyncedEyeAttentionPoint());
		renderState.addGeckolibData(BODY_YAW, Mth.rotLerp(partialTick, entity.yBodyRotO, entity.yBodyRot));
		renderState.addGeckolibData(GAME_TIME, entity.level().getGameTime());
		renderState.addGeckolibData(REACTION, entity.getVisualReaction());
		renderState.addGeckolibData(BLINK_START, entity.getBlinkStart());
		renderState.addGeckolibData(BLINK_COUNT, entity.getBlinkCount());
		renderState.addGeckolibData(CURIOUS_TILT, entity.getCuriousTilt());
		renderState.addGeckolibData(VISUAL_SEQUENCE, entity.getVisualSequence());
		renderState.addGeckolibData(ATTENTION_STARTED_AT, entity.getAttentionStartedAt());
		renderState.addGeckolibData(CAUGHT_REACTION_START, entity.getCaughtReactionStart());
		renderState.addGeckolibData(ACTION_STATE, entity.action());
		renderState.addGeckolibData(EVENT_SEQUENCE, entity.afterimageSequence());
		renderState.addGeckolibData(EVENT_KIND, entity.afterimageKind());
		renderState.addGeckolibData(EVENT_ORIGIN, entity.afterimageOrigin());
		renderState.addGeckolibData(EVENT_DIRECTION, entity.afterimageDirection());
		renderState.addGeckolibData(EVENT_YAW, entity.afterimageYaw());
		renderState.addGeckolibData(EVENT_NEUTRAL, entity.isAfterimageNeutral());
		renderState.addGeckolibData(EVENT_ADVANCED, entity.isAfterimageAdvanced());
	}

	@Override
	public void submit(
			EntityRenderState renderState,
			PoseStack poseStack,
			SubmitNodeCollector collector,
			CameraRenderState cameraState
	) {
		long now = renderState.getGeckolibData(GAME_TIME);
		pruneExpired(now + renderState.getPartialTick());

		// This pass evaluates the current animation and captures a pending event.
		super.submit(renderState, poseStack, collector, cameraState);

		Vec3 currentPosition = renderState.getGeckolibData(ENTITY_POSITION);
		float currentYaw = renderState.getGeckolibData(BODY_YAW);
		int currentEntityId = renderState.getGeckolibData(ENTITY_ID);
		int globalAfterimages = this.afterimages.size();
		if (globalAfterimages >= SOFT_LIMIT) {
			clearAfterimagePassData(renderState);
			return;
		}
		for (Afterimage afterimage : List.copyOf(this.afterimages)) {
			if (afterimage.entityId != currentEntityId) continue;
			double age = now + renderState.getPartialTick() - afterimage.createdAt;
			float life = (float)(age / afterimage.lifetime);
			if (life < 0.0F || life >= 1.0F) continue;

			poseStack.pushPose();
			Vec3 offset = afterimage.position.subtract(currentPosition);
			poseStack.translate(offset.x, offset.y, offset.z);
			poseStack.mulPose(Axis.YP.rotationDegrees(currentYaw - afterimage.yaw));
			float stretch = 1.0F + (1.0F - life) * 0.018F;
			poseStack.scale(stretch, 1.0F - (1.0F - life) * 0.008F, stretch);

			boolean advanced = afterimage.advanced && now >= this.retryAdvancedRenderingAt;
			if (advanced) {
				try {
					renderAdvancedAfterimage(renderState, poseStack, collector, cameraState,
							afterimage, (float)age);
				} catch (RuntimeException ignored) {
					// Keep phase one as the compatibility path and periodically retry after
					// resource reloads or transient pipeline failures.
					this.retryAdvancedRenderingAt = now + 100L;
					renderPhaseOneAfterimage(renderState, poseStack, collector, cameraState, afterimage, life);
				}
			} else {
				renderPhaseOneAfterimage(renderState, poseStack, collector, cameraState, afterimage, life);
			}
			poseStack.popPose();
		}
		clearAfterimagePassData(renderState);
	}

	private void renderPhaseOneAfterimage(
			EntityRenderState renderState,
			PoseStack poseStack,
			SubmitNodeCollector collector,
			CameraRenderState cameraState,
			Afterimage afterimage,
			float life
	) {
		int alpha = Math.clamp(Math.round(Mth.lerp(life, afterimage.startAlpha, 0.0F) * 255.0F), 0, 255);
		if (alpha <= 0) return;
		int rgb = afterimage.neutral ? 0xFFFFFF : themedColor(afterimage.kind, ZANSHIN_RGB, FUMIKOMI_RGB);
		renderFrozenPass(renderState, poseStack, collector, cameraState, afterimage,
				PASS_PHASE_ONE, alpha << 24 | rgb);
	}

	private void renderAdvancedAfterimage(
			EntityRenderState renderState,
			PoseStack poseStack,
			SubmitNodeCollector collector,
			CameraRenderState cameraState,
			Afterimage afterimage,
			float age
	) {
		float threshold = dissolveThreshold(age, afterimage.lifetime);
		float opacity = afterimageOpacity(age, afterimage.lifetime, afterimage.startAlpha);
		if (opacity <= 0.0F) return;

		// Red carries the dissolve threshold and alpha carries actual opacity.
		// Minecraft's stock dissolve shader cannot do this because it forcibly
		// resets every surviving fragment to fully opaque.
		int controls = alphaByte(opacity) << 24 | alphaByte(threshold) << 16 | 0xFFFF;
		renderFrozenPass(renderState, poseStack, collector, cameraState, afterimage,
				PASS_ADVANCED_BASE, controls);
	}

	private void renderFrozenPass(
			EntityRenderState renderState,
			PoseStack poseStack,
			SubmitNodeCollector collector,
			CameraRenderState cameraState,
			Afterimage afterimage,
			byte passMode,
			int color
	) {
		renderState.addGeckolibData(AFTERIMAGE_PASS_MODE, passMode);
		renderState.addGeckolibData(AFTERIMAGE_PASS_NEUTRAL, afterimage.neutral);
		renderState.addGeckolibData(AFTERIMAGE_PASS_KIND, afterimage.kind);
		renderState.addGeckolibData(DataTickets.RENDER_COLOR, color);
		renderState.addGeckolibData(DataTickets.PACKED_LIGHT, renderState.lightCoords);
		this.performRenderPass(renderState, poseStack, collector, cameraState,
				List.of((pass, bones) -> applyFrozenPose(afterimage.pose, bones)));
	}

	private static float dissolveThreshold(float age, float lifetime) {
		if (age <= DISSOLVE_HOLD_TICKS) return 1.0F;
		float progress = Mth.clamp((age - DISSOLVE_HOLD_TICKS)
				/ Math.max(1.0F, lifetime - DISSOLVE_HOLD_TICKS), 0.0F, 1.0F);
		float smooth = progress * progress * (3.0F - 2.0F * progress);
		return 1.0F - smooth;
	}

	private static float afterimageOpacity(float age, float lifetime, float startAlpha) {
		float progress = Mth.clamp(age / Math.max(1.0F, lifetime), 0.0F, 1.0F);
		float smooth = progress * progress * (3.0F - 2.0F * progress);
		return startAlpha * (1.0F - smooth);
	}

	private static int alphaByte(float alpha) {
		return Math.clamp(Math.round(alpha * 255.0F), 0, 255);
	}

	private static int themedColor(byte kind, int zanshin, int fumikomi) {
		return kind == JapaneseSamuraiEchoEntity.AFTERIMAGE_FUMIKOMI ? fumikomi : zanshin;
	}

	private static void clearAfterimagePassData(EntityRenderState renderState) {
		renderState.getDataMap().remove(AFTERIMAGE_PASS_MODE);
		renderState.getDataMap().remove(AFTERIMAGE_PASS_NEUTRAL);
		renderState.getDataMap().remove(AFTERIMAGE_PASS_KIND);
		renderState.getDataMap().remove(DataTickets.RENDER_COLOR);
		renderState.getDataMap().remove(DataTickets.PACKED_LIGHT);
	}

	@Override
	public RenderType getRenderType(EntityRenderState renderState, Identifier texture) {
		byte passMode = renderState.getOrDefaultGeckolibData(AFTERIMAGE_PASS_MODE, PASS_NONE);
		boolean neutral = renderState.getOrDefaultGeckolibData(AFTERIMAGE_PASS_NEUTRAL, false);
		byte kind = renderState.getOrDefaultGeckolibData(AFTERIMAGE_PASS_KIND,
				JapaneseSamuraiEchoEntity.AFTERIMAGE_ZANSHIN_REAL);
		return switch (passMode) {
			case PASS_PHASE_ONE -> RenderTypes.entityTranslucent(texture);
			case PASS_ADVANCED_BASE -> neutral
					? ADVANCED_NEUTRAL_RENDER_TYPE
					: kind == JapaneseSamuraiEchoEntity.AFTERIMAGE_FUMIKOMI
							? ADVANCED_FUMIKOMI_RENDER_TYPE
							: ADVANCED_ZANSHIN_RENDER_TYPE;
			default -> super.getRenderType(renderState, texture);
		};
	}

	private static RenderPipeline createAdvancedPipeline(String name, int rgb) {
		return RenderPipelines.register(RenderPipeline.builder(RenderPipelines.ENTITY_SNIPPET)
				.withLocation(EchoWarrior.id("pipeline/" + name))
				.withVertexShader(EchoWarrior.id("core/samurai_afterimage"))
				.withFragmentShader(EchoWarrior.id("core/samurai_afterimage"))
				.withShaderDefine("ALPHA_CUTOUT", 0.1F)
				.withShaderDefine("PER_FACE_LIGHTING")
				.withShaderDefine("AFTERIMAGE_TINT_R", ((rgb >> 16) & 0xFF) / 255.0F)
				.withShaderDefine("AFTERIMAGE_TINT_G", ((rgb >> 8) & 0xFF) / 255.0F)
				.withShaderDefine("AFTERIMAGE_TINT_B", (rgb & 0xFF) / 255.0F)
				.withSampler("Sampler1")
				.withSampler("DissolveMaskSampler")
				.withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
				.withCull(false)
				.build());
	}

	private static RenderType createAdvancedRenderType(
			String name,
			RenderPipeline pipeline,
			Identifier texture
	) {
		RenderSetup setup = RenderSetup.builder(pipeline)
				.withTexture("Sampler0", texture)
				.withTexture("DissolveMaskSampler", DISSOLVE_MASK)
				.useLightmap()
				.useOverlay()
				.affectsCrumbling()
				.sortOnUpload()
				.setOutline(RenderSetup.OutlineProperty.NONE)
				.createRenderSetup();
		return RenderType.create("echo_warrior_" + name, setup);
	}

	@Override
	public void adjustModelBonesForRender(RenderPassInfo<EntityRenderState> renderPass, BoneSnapshots snapshots) {
		super.adjustModelBonesForRender(renderPass, snapshots);
		if (renderPass.getOrDefaultGeckolibData(AFTERIMAGE_PASS_MODE, PASS_NONE) != PASS_NONE) return;
		applyVisualPresentation(renderPass, snapshots);

		int entityId = renderPass.getGeckolibData(ENTITY_ID);
		int sequence = renderPass.getGeckolibData(EVENT_SEQUENCE);
		int previous = this.lastSequences.getOrDefault(entityId, 0);
		if (sequence == previous) return;
		this.lastSequences.put(entityId, sequence);

		Map<String, FrozenBone> pose = capturePose(renderPass, snapshots);
		byte kind = renderPass.getGeckolibData(EVENT_KIND);
		Vec3 origin = renderPass.getGeckolibData(EVENT_ORIGIN);
		Vec3 direction = renderPass.getGeckolibData(EVENT_DIRECTION);
		float yaw = renderPass.getGeckolibData(EVENT_YAW);
		boolean neutral = renderPass.getGeckolibData(EVENT_NEUTRAL);
		boolean advanced = renderPass.getGeckolibData(EVENT_ADVANCED);
		long createdAt = renderPass.getGeckolibData(GAME_TIME);

		if (this.afterimages.size() >= SOFT_LIMIT) return;
		if (kind == JapaneseSamuraiEchoEntity.AFTERIMAGE_ZANSHIN_PHANTOM) {
			int count = this.afterimages.size() >= FALLBACK_THRESHOLD ? 2 : 3;
			for (int stage = 0; stage < count; stage++) {
				addAfterimage(new Afterimage(entityId, origin.add(direction.scale(0.35 * (stage + 1))), yaw,
						kind, neutral, advanced, createdAt, 8.0F - stage,
						0.48F - stage * 0.08F, pose));
			}
		} else {
			addAfterimage(new Afterimage(entityId, origin, yaw, kind, neutral, advanced, createdAt, 10.0F,
					kind == JapaneseSamuraiEchoEntity.AFTERIMAGE_FUMIKOMI ? 0.42F : 0.46F, pose));
		}
	}

	private void applyVisualPresentation(
			RenderPassInfo<EntityRenderState> renderPass,
			BoneSnapshots snapshots
	) {
		int entityId = renderPass.getGeckolibData(ENTITY_ID);
		if (this.visualStates.size() > 256 && !this.visualStates.containsKey(entityId)) this.visualStates.clear();
		VisualState state = this.visualStates.computeIfAbsent(entityId, ignored -> new VisualState());
		float age = renderPass.renderState().ageInTicks;
		float deltaTicks = state.lastAge < 0.0F ? 1.0F : Mth.clamp(age - state.lastAge, 0.0F, 1.0F);
		state.lastAge = age;

		// Japanese combat clips deliberately animate Head and Eyebrow. Ease the
		// procedural pose back to neutral in the background, but never overwrite
		// those authored bones while a committed action is playing.
		if (renderPass.getGeckolibData(ACTION_STATE) != JapaneseSamuraiEchoEntity.ACTION_NONE) {
			state.headYaw = approach(state.headYaw, 0.0F, 0.45F, deltaTicks);
			state.headPitch = approach(state.headPitch, 0.0F, 0.45F, deltaTicks);
			state.headTilt = approach(state.headTilt, 0.0F, 0.45F, deltaTicks);
			state.eyeX = approach(state.eyeX, 0.0F, 0.65F, deltaTicks);
			state.eyeY = approach(state.eyeY, 0.0F, 0.65F, deltaTicks);
			state.pupilScale = approach(state.pupilScale, 1.0F, 0.3F, deltaTicks);
			state.lastSequence = renderPass.getGeckolibData(VISUAL_SEQUENCE);
			return;
		}

		Vec3 entityPosition = renderPass.getGeckolibData(VISUAL_ENTITY_POSITION);
		Vec3 attentionPoint = renderPass.getGeckolibData(ATTENTION_POINT);
		Vec3 eyeAttentionPoint = renderPass.getGeckolibData(EYE_ATTENTION_POINT);
		float bodyYaw = renderPass.getGeckolibData(BODY_YAW);
		byte reaction = renderPass.getGeckolibData(REACTION);
		long gameTime = renderPass.getGeckolibData(GAME_TIME);
		float partialTick = renderPass.renderState().getPartialTick();
		int sequence = renderPass.getGeckolibData(VISUAL_SEQUENCE);
		float attentionAge = gameTime + partialTick - renderPass.getGeckolibData(ATTENTION_STARTED_AT);
		float caughtReactionAge = gameTime + partialTick - renderPass.getGeckolibData(CAUGHT_REACTION_START);

		Vec3 headDelta = attentionPoint.subtract(entityPosition);
		double headHorizontal = Math.sqrt(headDelta.x * headDelta.x + headDelta.z * headDelta.z);
		boolean locomotionGaze = reaction == JapaneseSamuraiEchoEntity.VISUAL_LOCOMOTION;
		float headYawLimit = locomotionGaze ? LOCOMOTION_HEAD_YAW_LIMIT : 75.0F;
		float desiredHeadWorldYaw = headHorizontal < 1.0E-4 ? bodyYaw : worldYawToward(headDelta);
		float desiredHeadYaw = Mth.clamp(Mth.wrapDegrees(desiredHeadWorldYaw - bodyYaw),
				-headYawLimit, headYawLimit);
		float desiredHeadPitch = headHorizontal < 1.0E-4 ? 0.0F
				: Mth.clamp(worldPitchToward(headDelta, headHorizontal), -35.0F, 40.0F);
		float desiredTilt = reaction == JapaneseSamuraiEchoEntity.VISUAL_CURIOUS
				? renderPass.getGeckolibData(CURIOUS_TILT) * 10.0F : 0.0F;

		float headResponsiveness;
		if (reaction == JapaneseSamuraiEchoEntity.VISUAL_STARTLED
				|| reaction == JapaneseSamuraiEchoEntity.VISUAL_HURT) {
			headResponsiveness = 0.55F;
		} else if (reaction == JapaneseSamuraiEchoEntity.VISUAL_CAUGHT) {
			headResponsiveness = 0.36F;
		} else if (reaction == JapaneseSamuraiEchoEntity.VISUAL_MUTUAL_GAZE) {
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
		float desiredEyeWorldPitch = eyeHorizontal < 1.0E-4
				? desiredHeadPitch : worldPitchToward(eyeDelta, eyeHorizontal);
		float eyeYawLimit = locomotionGaze ? LOCOMOTION_EYE_YAW_LIMIT : EYE_YAW_LIMIT;
		float eyeTargetYaw = Mth.clamp(Mth.wrapDegrees(desiredEyeWorldYaw - bodyYaw - state.headYaw),
				-eyeYawLimit, eyeYawLimit);
		float eyeTargetPitch = Mth.clamp(desiredEyeWorldPitch - state.headPitch,
				-EYE_PITCH_LIMIT, EYE_PITCH_LIMIT);
		float unrolledEyeX = -eyeTargetYaw / EYE_YAW_LIMIT * MAX_EYE_X;
		float unrolledEyeY = -eyeTargetPitch / EYE_PITCH_LIMIT * MAX_EYE_Y;
		float tiltRadians = toRadians(state.headTilt);
		float tiltCos = Mth.cos(tiltRadians);
		float tiltSin = Mth.sin(tiltRadians);
		float desiredEyeX = unrolledEyeX * tiltCos + unrolledEyeY * tiltSin;
		float desiredEyeY = -unrolledEyeX * tiltSin + unrolledEyeY * tiltCos;
		float eyeResponsiveness = reaction == JapaneseSamuraiEchoEntity.VISUAL_STARTLED
				|| reaction == JapaneseSamuraiEchoEntity.VISUAL_HURT ? 0.92F
				: reaction == JapaneseSamuraiEchoEntity.VISUAL_CAUGHT ? 0.9F
				: reaction == JapaneseSamuraiEchoEntity.VISUAL_MUTUAL_GAZE ? 0.82F : 0.58F;
		state.eyeX = approach(state.eyeX, desiredEyeX, eyeResponsiveness, deltaTicks);
		state.eyeY = approach(state.eyeY, desiredEyeY, eyeResponsiveness, deltaTicks);

		float desiredPupilScale = switch (reaction) {
			case JapaneseSamuraiEchoEntity.VISUAL_HURT -> 0.6F;
			case JapaneseSamuraiEchoEntity.VISUAL_STARTLED -> 0.48F;
			case JapaneseSamuraiEchoEntity.VISUAL_CAUGHT -> Mth.lerp(
					Mth.clamp((caughtReactionAge - 3.0F) / 7.0F, 0.0F, 1.0F), 0.8F, 1.0F);
			default -> 1.0F;
		};
		state.pupilScale = approach(state.pupilScale, desiredPupilScale,
				desiredPupilScale < state.pupilScale ? 0.8F : 0.18F, deltaTicks);

		float convergence = eyeHorizontal < 3.0 && eyeHorizontal > 0.1
				? (float)((3.0 - eyeHorizontal) / 3.0) * 0.09F : 0.0F;
		float blink = reaction == JapaneseSamuraiEchoEntity.VISUAL_STARTLED ? 0.0F
				: reaction == JapaneseSamuraiEchoEntity.VISUAL_HURT
						? calculateHurtBlink(gameTime + partialTick, renderPass.getGeckolibData(BLINK_START))
						: calculateBlink(gameTime + partialTick, renderPass.getGeckolibData(BLINK_START),
						renderPass.getGeckolibData(BLINK_COUNT));

		float inheritedRotX = inheritedRotation(snapshots, AxisComponent.X);
		float inheritedRotY = inheritedRotation(snapshots, AxisComponent.Y);
		float inheritedRotZ = inheritedRotation(snapshots, AxisComponent.Z);
		float inheritedMagnitude = Math.max(Math.abs(inheritedRotX),
				Math.max(Math.abs(inheritedRotY), Math.abs(inheritedRotZ)));
		float inheritedMagnitudeDegrees = inheritedMagnitude * Mth.RAD_TO_DEG;
		float parentCompensation = 1.0F - (float)Mth.smoothstep(Mth.clamp(
				(inheritedMagnitudeDegrees - FULL_IDLE_PARENT_COMPENSATION_DEGREES)
						/ (NO_PARENT_COMPENSATION_DEGREES - FULL_IDLE_PARENT_COMPENSATION_DEGREES),
				0.0F, 1.0F));

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
	}

	private static float inheritedRotation(BoneSnapshots snapshots, AxisComponent component) {
		float result = 0.0F;
		for (String boneName : List.of("Main", "Body", "Upper_Body", "Upper_Body2")) {
			result += snapshots.get(boneName).map(bone -> switch (component) {
				case X -> bone.getRotX();
				case Y -> bone.getRotY();
				case Z -> bone.getRotZ();
			}).orElse(0.0F);
		}
		return result;
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

	private static Map<String, FrozenBone> capturePose(
			RenderPassInfo<EntityRenderState> renderPass,
			BoneSnapshots snapshots
	) {
		Map<String, FrozenBone> result = new LinkedHashMap<>();
		for (GeoBone bone : renderPass.model().boneLookup().get().values()) {
			BoneSnapshot snapshot = snapshots.get(bone);
			result.put(bone.name(), new FrozenBone(
					snapshot.getScaleX(), snapshot.getScaleY(), snapshot.getScaleZ(),
					snapshot.getTranslateX(), snapshot.getTranslateY(), snapshot.getTranslateZ(),
					snapshot.getRotX(), snapshot.getRotY(), snapshot.getRotZ(),
					snapshot.isHidden(), snapshot.areChildrenHidden()
			));
		}
		return Map.copyOf(result);
	}

	private static void applyFrozenPose(Map<String, FrozenBone> pose, BoneSnapshots snapshots) {
		for (Map.Entry<String, FrozenBone> entry : pose.entrySet()) {
			FrozenBone frozen = entry.getValue();
			snapshots.ifPresent(entry.getKey(), bone -> bone
					.setScale(frozen.scaleX, frozen.scaleY, frozen.scaleZ)
					.setTranslation(frozen.translateX, frozen.translateY, frozen.translateZ)
					.setRotation(frozen.rotX, frozen.rotY, frozen.rotZ)
					.skipRender(frozen.hidden)
					.skipChildrenRender(frozen.childrenHidden));
		}
	}

	private void addAfterimage(Afterimage afterimage) {
		this.afterimages.add(afterimage);
		while (countForEntity(afterimage.entityId) > PER_SAMURAI_LIMIT) {
			for (Iterator<Afterimage> iterator = this.afterimages.iterator(); iterator.hasNext();) {
				if (iterator.next().entityId == afterimage.entityId) {
					iterator.remove();
					break;
				}
			}
		}
		while (this.afterimages.size() > HARD_LIMIT) evictLowestPriority();
	}

	private int countForEntity(int entityId) {
		int count = 0;
		for (Afterimage afterimage : this.afterimages) if (afterimage.entityId == entityId) count++;
		return count;
	}

	private void pruneExpired(double now) {
		this.afterimages.removeIf(afterimage -> now - afterimage.createdAt >= afterimage.lifetime);
		if (this.lastSequences.size() > 256) this.lastSequences.clear();
		if (this.visualStates.size() > 256) this.visualStates.clear();
	}

	private void evictLowestPriority() {
		Vec3 viewer = Minecraft.getInstance().player == null
				? Vec3.ZERO : Minecraft.getInstance().player.position();
		int worstIndex = 0;
		double worstScore = Double.NEGATIVE_INFINITY;
		for (int index = 0; index < this.afterimages.size(); index++) {
			Afterimage candidate = this.afterimages.get(index);
			double score = candidate.position.distanceToSqr(viewer) - index * 0.25;
			if (score > worstScore) {
				worstScore = score;
				worstIndex = index;
			}
		}
		this.afterimages.remove(worstIndex);
	}

	private record Afterimage(
			int entityId,
			Vec3 position,
			float yaw,
			byte kind,
			boolean neutral,
			boolean advanced,
			long createdAt,
			float lifetime,
			float startAlpha,
			Map<String, FrozenBone> pose
	) {}

	private record FrozenBone(
			float scaleX, float scaleY, float scaleZ,
			float translateX, float translateY, float translateZ,
			float rotX, float rotY, float rotZ,
			boolean hidden, boolean childrenHidden
	) {}

	private enum AxisComponent { X, Y, Z }

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
