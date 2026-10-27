package com.yuriscat.echowarrior.compat.client;

import net.minecraft.network.chat.Component;

/** ForgeGui inherits the vanilla message state but renders through its own overlay event. */
public interface PetFeedbackOverlayAccess1201 {
    Component echoWarrior$petOverlayMessage();
    int echoWarrior$petOverlayTime();
    boolean echoWarrior$petOverlayAnimated();
}
