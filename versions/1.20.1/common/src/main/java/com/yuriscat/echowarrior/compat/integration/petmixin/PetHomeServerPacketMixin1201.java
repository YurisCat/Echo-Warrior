package com.yuriscat.echowarrior.compat.integration.petmixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

/**
 * Selects the reviewed packet class for the dedicated-server bytecode sanitation in
 * PetCollarMixinPlugin1201. The client keeps its original GUI and packet handler.
 */
@Pseudo
@Mixin(targets = "com.github.yzqdev.pethome.network.PropertiesMessage", remap = false)
public abstract class PetHomeServerPacketMixin1201 {}
