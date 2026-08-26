package com.yuriscat.echowarrior.client;

import com.geckolib.animation.state.BoneSnapshot;
import com.geckolib.cache.model.GeoBone;
import com.geckolib.constant.DataTickets;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.RenderPassInfo;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.yuriscat.echowarrior.EchoWarrior;
import com.yuriscat.echowarrior.ModEntities;
import com.yuriscat.echowarrior.entity.JapaneseSamuraiEchoEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
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
 * snapshots for model-anchored dissolve and full-bright coloring. The proposed
 * outline pass remains disabled until it can use a real post-process instead
 * of turning custom GeckoLib geometry into a filled silhouette.
 */
public final class JapaneseSamuraiEchoRenderer
		extends GeoEntityRenderer<JapaneseSamuraiEchoEntity, EntityRenderState> {
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
	private static final Identifier ADVANCED_DETAIL_TEXTURE = EchoWarrior.id(
			"textures/entity/japanese_samurai_afterimage_detail.png");
	private static final Identifier DISSOLVE_MASK = EchoWarrior.id(
			"textures/effect/samurai_afterimage_dissolve.png");

	private static final DataTicket<Integer> ENTITY_ID = DataTickets.create(
			"echo_warrior_samurai_afterimage_entity_id", Integer.class);
	private static final DataTicket<Vec3> ENTITY_POSITION = DataTickets.create(
			"echo_warrior_samurai_afterimage_entity_position", Vec3.class);
	private static final DataTicket<Float> BODY_YAW = DataTickets.create(
			"echo_warrior_samurai_afterimage_body_yaw", Float.class);
	private static final DataTicket<Long> GAME_TIME = DataTickets.create(
			"echo_warrior_samurai_afterimage_game_time", Long.class);
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

	private final Map<Integer, Integer> lastSequences = new HashMap<>();
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
		renderState.addGeckolibData(BODY_YAW, Mth.rotLerp(partialTick, entity.yBodyRotO, entity.yBodyRot));
		renderState.addGeckolibData(GAME_TIME, entity.level().getGameTime());
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
		int baseRgb = afterimage.neutral ? 0xFFFFFF : themedColor(afterimage.kind, ZANSHIN_RGB, FUMIKOMI_RGB);

		// A previous second full-model pass attempted to leave a narrow bright
		// dissolve rim. GeckoLib submitted that pass in front of the detailed
		// geometry and turned the afterimage into a white cutout. Keep the useful
		// UV-anchored dissolve, but draw only the textured body under scene light.
		renderFrozenPass(renderState, poseStack, collector, cameraState, afterimage,
				PASS_ADVANCED_BASE, alphaByte(threshold) << 24 | baseRgb);
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

	private static int alphaByte(float alpha) {
		return Math.clamp(Math.round(alpha * 255.0F), 0, 255);
	}

	private static int themedColor(byte kind, int zanshin, int fumikomi) {
		return kind == JapaneseSamuraiEchoEntity.AFTERIMAGE_FUMIKOMI ? fumikomi : zanshin;
	}

	private static void clearAfterimagePassData(EntityRenderState renderState) {
		renderState.getDataMap().remove(AFTERIMAGE_PASS_MODE);
		renderState.getDataMap().remove(AFTERIMAGE_PASS_NEUTRAL);
		renderState.getDataMap().remove(DataTickets.RENDER_COLOR);
		renderState.getDataMap().remove(DataTickets.PACKED_LIGHT);
	}

	@Override
	public RenderType getRenderType(EntityRenderState renderState, Identifier texture) {
		byte passMode = renderState.getOrDefaultGeckolibData(AFTERIMAGE_PASS_MODE, PASS_NONE);
		boolean neutral = renderState.getOrDefaultGeckolibData(AFTERIMAGE_PASS_NEUTRAL, false);
		return switch (passMode) {
			case PASS_PHASE_ONE -> RenderTypes.entityTranslucent(texture);
			case PASS_ADVANCED_BASE -> RenderTypes.entityCutoutDissolve(
					neutral ? texture : ADVANCED_DETAIL_TEXTURE, DISSOLVE_MASK);
			default -> super.getRenderType(renderState, texture);
		};
	}

	@Override
	public void adjustModelBonesForRender(RenderPassInfo<EntityRenderState> renderPass, BoneSnapshots snapshots) {
		super.adjustModelBonesForRender(renderPass, snapshots);
		if (renderPass.getOrDefaultGeckolibData(AFTERIMAGE_PASS_MODE, PASS_NONE) != PASS_NONE) return;

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
}
