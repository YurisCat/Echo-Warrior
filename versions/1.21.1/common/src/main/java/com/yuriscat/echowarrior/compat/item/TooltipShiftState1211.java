package com.yuriscat.echowarrior.compat.item;

import java.util.Objects;
import java.util.function.BooleanSupplier;

public final class TooltipShiftState1211 {
    private static BooleanSupplier shiftDownSupplier = () -> false;

    private TooltipShiftState1211() {
    }

    public static void setClientShiftDownSupplier(BooleanSupplier supplier) {
        shiftDownSupplier = Objects.requireNonNull(supplier);
    }

    public static boolean isShiftDown() {
        return shiftDownSupplier.getAsBoolean();
    }
}
