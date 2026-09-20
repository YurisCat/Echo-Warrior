package com.yuriscat.echowarrior.compat.menu;

import com.yuriscat.echowarrior.compat.ModContent1201;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;

public final class RecyclerMenu1201 extends ChestMenu {
    public RecyclerMenu1201(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainer(27));
    }

    public RecyclerMenu1201(int containerId, Inventory inventory, Container container) {
        super(ModContent1201.RECYCLER_MENU, containerId, inventory, container, 3);
    }

    public Container recyclerContainer() {
        return getContainer();
    }
}
