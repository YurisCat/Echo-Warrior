package com.yuriscat.echowarrior.platform;

import net.minecraft.resources.Identifier;

@FunctionalInterface
public interface RegistryRegistrar<T> {
	T register(Identifier id, T value);
}
