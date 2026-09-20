package com.yuriscat.echowarrior.compat.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import java.util.function.Consumer;

/** Copy-on-write replacement for CUSTOM_DATA; reads and no-op writes never mutate a stack. */
public final class RelicNbt1201 {
    private RelicNbt1201() {}
    public static CompoundTag read(ItemStack stack) {
        return stack.hasTag() ? stack.getTag().copy() : new CompoundTag();
    }
    public static void update(ItemStack stack, Consumer<CompoundTag> mutation) {
        CompoundTag original = stack.getTag();
        CompoundTag updated = read(stack);
        mutation.accept(updated);
        if (original == null ? !updated.isEmpty() : !original.equals(updated)) stack.setTag(updated.copy());
    }
}
