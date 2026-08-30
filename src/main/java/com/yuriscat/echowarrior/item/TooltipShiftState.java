package com.yuriscat.echowarrior.item;

import java.util.Objects;
import java.util.function.BooleanSupplier;

/**
 * Client-provided Shift state shared by common tooltip code.
 *
 * <p>The default remains false so the common item classes stay safe on a
 * dedicated server.</p>
 */
public final class TooltipShiftState {
	private static BooleanSupplier shiftDownSupplier = () -> false;

	private TooltipShiftState() {
	}

	public static void setClientShiftDownSupplier(BooleanSupplier supplier) {
		shiftDownSupplier = Objects.requireNonNull(supplier);
	}

	public static boolean isShiftDown() {
		return shiftDownSupplier.getAsBoolean();
	}
}
