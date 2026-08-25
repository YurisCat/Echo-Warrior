package com.yuriscat.echowarrior.client;

import com.geckolib.constant.DataTickets;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.RenderPassInfo;
import com.yuriscat.echowarrior.ModEntities;
import com.yuriscat.echowarrior.entity.GuandaoWarriorEchoEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;

import java.util.HashMap;
import java.util.Map;

public final class GuandaoWarriorEchoRenderer extends GeoEntityRenderer<GuandaoWarriorEchoEntity, EntityRenderState> {
	private static final DataTicket<Integer> ENTITY_ID = DataTickets.create(
			"echo_warrior_guandao_entity_id", Integer.class);
	private static final DataTicket<Integer> VALOR_STACKS = DataTickets.create(
			"echo_warrior_guandao_valor_stacks", Integer.class);
	private static final DataTicket<Long> GAME_TIME = DataTickets.create(
			"echo_warrior_guandao_game_time", Long.class);

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
		renderState.addGeckolibData(VALOR_STACKS, entity.getValorStacks());
		renderState.addGeckolibData(GAME_TIME, entity.level().getGameTime());
	}

	@Override
	public void adjustModelBonesForRender(RenderPassInfo<EntityRenderState> renderPass, BoneSnapshots snapshots) {
		super.adjustModelBonesForRender(renderPass, snapshots);
		int stacks = renderPass.getGeckolibData(VALOR_STACKS);
		if (stacks <= 0) return;

		int entityId = renderPass.getGeckolibData(ENTITY_ID);
		long gameTime = renderPass.getGeckolibData(GAME_TIME);
		int interval = switch (stacks) {
			case 1 -> 6;
			case 2 -> 4;
			case 3 -> 3;
			case 4 -> 2;
			default -> 1;
		};
		if (gameTime % interval != 0 || this.lastParticleTicks.getOrDefault(entityId, Long.MIN_VALUE) == gameTime) {
			return;
		}
		if (this.lastParticleTicks.size() > 256 && !this.lastParticleTicks.containsKey(entityId)) {
			this.lastParticleTicks.clear();
		}
		this.lastParticleTicks.put(entityId, gameTime);

		ParticleOptions particle = stacks >= 5 ? ParticleTypes.SOUL_FIRE_FLAME
				: stacks >= 3 ? ParticleTypes.FLAME : ParticleTypes.SMALL_FLAME;
		int count = stacks >= 4 ? 2 : 1;
		renderPass.addBonePositionListener("WeaponParticleAnchor", (worldPosition, modelPosition, localPosition) -> {
			var level = Minecraft.getInstance().level;
			if (level == null || worldPosition == null) return;
			var random = level.getRandom();
			for (int index = 0; index < count; index++) {
				double spread = stacks >= 3 ? 0.055 : 0.035;
				level.addParticle(
						particle,
						worldPosition.x + (random.nextDouble() - 0.5) * spread,
						worldPosition.y + (random.nextDouble() - 0.5) * spread,
						worldPosition.z + (random.nextDouble() - 0.5) * spread,
						(random.nextDouble() - 0.5) * 0.015,
						0.012 + random.nextDouble() * 0.018,
						(random.nextDouble() - 0.5) * 0.015
				);
			}
		});
	}
}
