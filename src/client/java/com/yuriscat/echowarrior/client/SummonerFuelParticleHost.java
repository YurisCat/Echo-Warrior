package com.yuriscat.echowarrior.client;

import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public interface SummonerFuelParticleHost {
	void echoWarrior$spawnFuelInsertionParticles(Slot slot, ItemStack fuel);

	void echoWarrior$spawnInsertionPolish(Slot slot);
}
