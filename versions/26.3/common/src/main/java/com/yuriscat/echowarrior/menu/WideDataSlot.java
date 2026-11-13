package com.yuriscat.echowarrior.menu;

import net.minecraft.world.inventory.DataSlot;
import java.util.function.Consumer;

/** Two unsigned 16-bit words, including on loaders that truncate container data to short. */
final class WideDataSlot {
    private final DataSlot low = DataSlot.standalone();
    private final DataSlot high = DataSlot.standalone();
    void register(Consumer<DataSlot> register) { register.accept(low); register.accept(high); }
    int get() { return (low.get() & 0xffff) | ((high.get() & 0xffff) << 16); }
    void set(int value) { low.set(value & 0xffff); high.set((value >>> 16) & 0xffff); }
}
