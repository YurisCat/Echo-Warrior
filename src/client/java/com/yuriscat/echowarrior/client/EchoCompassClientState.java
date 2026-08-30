package com.yuriscat.echowarrior.client;

import com.yuriscat.echowarrior.item.EchoCompassItem;
import com.yuriscat.echowarrior.network.EchoCompassStatePayload;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public final class EchoCompassClientState {
	private static final double INNER_SPIN_DISTANCE = 32.0;
	private static final double INNER_MIN_TURNS_PER_SECOND = 0.625;
	private static final double INNER_MAX_TURNS_PER_SECOND = 2.5;
	private static final double NO_TARGET_TURNS_PER_SECOND = 2.0;
	private static final double MAX_RENDER_STEP_SECONDS = 0.1;
	private static final double FRAME_FLASH_START_DISTANCE = 24.0;
	private static final double FRAME_SOLID_GOLD_DISTANCE = 2.0;
	private static final double FRAME_MIN_CYCLES_PER_SECOND = 0.45;
	private static final double FRAME_MAX_CYCLES_PER_SECOND = 1.5;

	private static EchoCompassStatePayload.Mode mode = EchoCompassStatePayload.Mode.INACTIVE;
	private static long targetPos;
	private static double spinPhase;
	private static double frameFlashPhase;
	private static float lastAngle;
	private static long lastUpdateNanos = System.nanoTime();
	private static long lastFrameUpdateNanos = System.nanoTime();

	private EchoCompassClientState() {
	}

	public static void accept(EchoCompassStatePayload payload) {
		boolean enteringSpin = !isSpinMode(mode) && isSpinMode(payload.mode());
		boolean enteringInner = !isInnerMode(mode) && isInnerMode(payload.mode());
		if (enteringSpin) spinPhase = lastAngle;
		if (enteringInner) frameFlashPhase = 0.0;
		mode = payload.mode();
		targetPos = payload.targetPos();
		long now = System.nanoTime();
		lastUpdateNanos = now;
		lastFrameUpdateNanos = now;
	}

	public static void clear() {
		mode = EchoCompassStatePayload.Mode.INACTIVE;
		targetPos = 0L;
		spinPhase = 0.0;
		frameFlashPhase = 0.0;
		lastAngle = 0.0F;
		long now = System.nanoTime();
		lastUpdateNanos = now;
		lastFrameUpdateNanos = now;
	}

	public static float angle(
			@Nullable ClientLevel level,
			@Nullable ItemOwner owner
	) {
		if (!isLocalOwner(owner) || level == null || !level.dimension().equals(Level.OVERWORLD)) return 0.0F;
		if (mode == EchoCompassStatePayload.Mode.OUTSIDE) {
			float angle = angleToTarget(owner, BlockPos.of(targetPos));
			spinPhase = angle;
			lastAngle = angle;
			lastUpdateNanos = System.nanoTime();
			return angle;
		}
		if (!isSpinMode(mode)) return 0.0F;

		advanceSpin(owner);
		lastAngle = (float)spinPhase;
		return lastAngle;
	}

	public static boolean usesGoldFrame(
			@Nullable ClientLevel level,
			@Nullable LivingEntity owner
	) {
		if (!(owner instanceof LocalPlayer) || level == null
				|| !level.dimension().equals(Level.OVERWORLD) || !isInnerMode()) return false;
		double distance = owner.position().distanceTo(BlockPos.of(targetPos).getCenter());
		long now = System.nanoTime();
		double elapsed = Math.clamp(
				(now - lastFrameUpdateNanos) / 1_000_000_000.0,
				0.0,
				MAX_RENDER_STEP_SECONDS
		);
		lastFrameUpdateNanos = now;
		if (distance <= FRAME_SOLID_GOLD_DISTANCE) return true;
		if (distance >= FRAME_FLASH_START_DISTANCE) return false;

		double closeness = Math.clamp(
				(FRAME_FLASH_START_DISTANCE - distance)
						/ (FRAME_FLASH_START_DISTANCE - FRAME_SOLID_GOLD_DISTANCE),
				0.0,
				1.0
		);
		double cyclesPerSecond = FRAME_MIN_CYCLES_PER_SECOND
				+ (FRAME_MAX_CYCLES_PER_SECOND - FRAME_MIN_CYCLES_PER_SECOND) * closeness;
		frameFlashPhase = positiveModulo(frameFlashPhase + elapsed * cyclesPerSecond, 1.0);
		return frameFlashPhase >= 0.5;
	}

	public static boolean usesIronFrame(
			ItemStack stack,
			@Nullable ClientLevel level,
			@Nullable LivingEntity owner
	) {
		if (owner instanceof LocalPlayer && level != null
				&& level.dimension().equals(Level.OVERWORLD) && isInnerMode()) return false;
		return !EchoCompassItem.isOutsideSoundEnabled(stack);
	}

	public static boolean hasDirectionalPulseTarget() {
		return mode == EchoCompassStatePayload.Mode.OUTSIDE || isInnerMode();
	}

	public static BlockPos directionalPulseTarget() {
		return BlockPos.of(targetPos);
	}

	public static boolean isInsideBattlefieldMode() {
		return isInnerMode();
	}

	private static void advanceSpin(ItemOwner owner) {
		long now = System.nanoTime();
		double elapsed = Math.clamp((now - lastUpdateNanos) / 1_000_000_000.0, 0.0, MAX_RENDER_STEP_SECONDS);
		lastUpdateNanos = now;

		double turnsPerSecond = NO_TARGET_TURNS_PER_SECOND;
		if (mode == EchoCompassStatePayload.Mode.INNER || mode == EchoCompassStatePayload.Mode.SALVAGE) {
			Vec3 target = BlockPos.of(targetPos).getCenter();
			double distance = owner.position().distanceTo(target);
			double closeness = Math.clamp(1.0 - distance / INNER_SPIN_DISTANCE, 0.0, 1.0);
			turnsPerSecond = INNER_MIN_TURNS_PER_SECOND
					+ (INNER_MAX_TURNS_PER_SECOND - INNER_MIN_TURNS_PER_SECOND) * closeness;
		}
		spinPhase = positiveModulo(spinPhase + elapsed * turnsPerSecond, 1.0);
	}

	private static float angleToTarget(ItemOwner owner, BlockPos position) {
		Vec3 target = position.getCenter();
		Vec3 ownerPosition = owner.position();
		double targetAngle = Math.atan2(target.z() - ownerPosition.z(), target.x() - ownerPosition.x())
				/ (Math.PI * 2.0);
		double ownerRotation = positiveModulo(owner.getVisualRotationYInDegrees() / 360.0, 1.0);
		return (float)positiveModulo(targetAngle - ownerRotation + 0.75, 1.0);
	}

	private static boolean isLocalOwner(@Nullable ItemOwner owner) {
		return owner != null && owner.asLivingEntity() instanceof LocalPlayer;
	}

	private static boolean isSpinMode(EchoCompassStatePayload.Mode candidate) {
		return candidate == EchoCompassStatePayload.Mode.INNER
				|| candidate == EchoCompassStatePayload.Mode.SALVAGE
				|| candidate == EchoCompassStatePayload.Mode.NO_TARGET;
	}

	private static boolean isInnerMode() {
		return isInnerMode(mode);
	}

	private static boolean isInnerMode(EchoCompassStatePayload.Mode candidate) {
		return candidate == EchoCompassStatePayload.Mode.INNER
				|| candidate == EchoCompassStatePayload.Mode.SALVAGE;
	}

	private static double positiveModulo(double value, double modulus) {
		return value - Math.floor(value / modulus) * modulus;
	}
}
