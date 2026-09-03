package com.yuriscat.echowarrior.menu;

import com.yuriscat.echowarrior.ModMenus;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;

public final class RecyclerMenu extends ChestMenu {
	public RecyclerMenu(int containerId, Inventory inventory) {
		this(containerId, inventory, new SimpleContainer(27));
	}

	public RecyclerMenu(int containerId, Inventory inventory, Container container) {
		super(ModMenus.RECYCLER, containerId, inventory, container, 3);
	}
}
