package com.yuriscat.echowarrior.client;

import com.geckolib.animation.AnimationProcessor;
import com.geckolib.animation.state.BoneSnapshot;
import com.geckolib.constant.DataTickets;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.base.BoneSnapshots;
import com.yuriscat.echowarrior.EchoWarrior;
import com.yuriscat.echowarrior.ModEntities;
import com.yuriscat.echowarrior.entity.GuandaoWarriorEchoEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.EntitySpawnReason;
import org.joml.Quaternionf;

import java.util.HashMap;

/** Exercises GeckoLib 5's actual controller extraction and per-frame bone snapshots. */
public final class GuandaoPresentationClientSelfTest {

	private GuandaoPresentationClientSelfTest() {}

	public static void run(Minecraft client) {
		try {
			for (boolean walking : new boolean[]{false, true}) {
				for (int packetOffset : new int[]{-2, 0, 2}) checkHandoff(client, walking, packetOffset);
			}
			EchoWarrior.LOGGER.info("GUANDAO PRESENTATION SELFTEST PASSED processor=geckolib5 root=idle-walk packet-order=three");
		} catch (ReflectiveOperationException error) {
			throw new IllegalStateException("Guandao animation fixture could not set its detached action state", error);
		}
	}

	private static void checkHandoff(Minecraft client, boolean walking, int packetOffset) throws ReflectiveOperationException {
		var entity = ModEntities.GUANDAO_WARRIOR_ECHO.create(client.level, EntitySpawnReason.LOAD);
		check(entity != null, "detached warrior exists");
		var model = new DefaultedEntityGeoModel<GuandaoWarriorEchoEntity>(ModEntities.GUANDAO_WARRIOR_ECHO);
		var baked = model.getBakedModel(model.getModelResource(new EntityRenderState()));
		var manager = entity.getAnimatableInstanceCache().getManagerForId(entity.getId());
		var actionSetter = GuandaoWarriorEchoEntity.class.getDeclaredMethod("setAnimationAction", byte.class, long.class, long.class);
		actionSetter.setAccessible(true);
		boolean sawComboRotation = false;
		try {
			for (int frame = 0; frame <= 520; frame++) {
				double time = frame * 0.25;
				entity.tickCount = (int)time;
				if (frame == 40) {
					actionSetter.invoke(entity, GuandaoWarriorEchoEntity.ANIMATION_ACTION_COMBO, 10L, 112L);
					manager.tryTriggerAnimation("action", "combo");
				}
				if (frame == 448 + packetOffset * 4)
					actionSetter.invoke(entity, GuandaoWarriorEchoEntity.ANIMATION_ACTION_NONE, 112L, 0L);
				if (frame == 448) manager.stopTriggeredAnimation("action", "combo");
				var state = new EntityRenderState();
				state.ageInTicks = (float)time;
				state.addGeckolibData(DataTickets.PARTIAL_TICK, (float)(time - entity.tickCount));
				state.addGeckolibData(DataTickets.ANIMATABLE_MANAGER, manager);
				state.addGeckolibData(DataTickets.IS_MOVING, walking && time >= 112 + packetOffset);
				AnimationProcessor.extractControllerStates(entity, state, model);
				// GeckoLib 5 creates fresh snapshots each render pass; never reuse GeckoLib 4's bone-reset model.
				var snapshots = new HashMap<String, BoneSnapshot>();
				BoneSnapshots bones = name -> baked.getBone(name).map(bone ->
						snapshots.computeIfAbsent(name, ignored -> BoneSnapshot.create(bone)));
				for (var controller : state.getGeckolibData(DataTickets.ANIMATION_CONTROLLER_STATES))
					AnimationProcessor.createBoneSnapshots(controller, bones);
				var root = bones.get("Main").orElseThrow();
				var body = bones.get("Body").orElseThrow();
				check(baked.getBone("WeaponParticleAnchor").isPresent(), "required particle anchor");
				if (time > 40 && time < 110 && Math.abs(root.getRotX()) > 1) sawComboRotation = true;
				if (time >= Math.max(112, 112 + packetOffset)) {
					// The authored endpoint is nearly identity even though its Euler axes
					// are all near +/-180 degrees. Test the pose, not its representation.
					check(rotationAngle(root) < 0.05,
							"root must rest after combo: walking=" + walking + " packetOffset=" + packetOffset + " time=" + time
									+ " root=" + root.getRotX() + "," + root.getRotY() + "," + root.getRotZ() + " angle=" + rotationAngle(root));
					check(rotationAngle(body) < 0.52,
							"body must not blend through Euler half-turn: walking=" + walking + " packetOffset=" + packetOffset + " time=" + time);
				}
			}
			check(sawComboRotation, "test must actually evaluate combo, not only idle");
		} finally {
			entity.discard();
		}
	}

	private static double rotationAngle(BoneSnapshot bone) {
		var rotation = new Quaternionf().rotationZYX(bone.getRotZ(), bone.getRotY(), bone.getRotX());
		return 2 * Math.acos(Math.min(1, Math.abs(rotation.w())));
	}

	private static void check(boolean result, String label) {
		if (!result) throw new IllegalStateException("Guandao presentation: " + label);
	}
}
