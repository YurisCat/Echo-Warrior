package com.yuriscat.echowarrior.entity;

import com.yuriscat.echowarrior.item.EchoHeroType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

public interface EchoWarriorEntity extends OwnableEntity {
	LivingEntity livingEntity();

	EchoHeroType heroType();

	@Nullable UUID getOwnerUuid();

	@Nullable UUID getSummonerUuid();

	void bindTo(Player owner, UUID summonerUuid);

	void applyRelicState(ItemStack relic, boolean resetAnchor);

	void recallTo(Player player);

	void dismiss();

	boolean shouldFollowOwner();

	default boolean isFollowMovementSuppressed() { return false; }

	default boolean isFormationActive() { return false; }

	default boolean isShieldBondActive() { return false; }

	default boolean isLegionEnduresActive() { return false; }
}
