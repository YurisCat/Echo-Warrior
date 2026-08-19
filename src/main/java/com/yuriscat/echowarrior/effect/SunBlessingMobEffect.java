package com.yuriscat.echowarrior.effect;

import com.yuriscat.echowarrior.EchoWarrior;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public final class SunBlessingMobEffect extends MobEffect {
	private static final AttributeModifier ATTACK_BONUS = new AttributeModifier(
			EchoWarrior.id("huitzilopochtli_attack"),
			0.15,
			AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
	);

	public SunBlessingMobEffect() {
		super(MobEffectCategory.BENEFICIAL, 0xF2A12B);
	}

	@Override
	public void addAttributeModifiers(AttributeMap attributes, int amplifier) {
		var attackDamage = attributes.getInstance(Attributes.ATTACK_DAMAGE);
		if (attackDamage == null) return;
		attackDamage.removeModifier(ATTACK_BONUS.id());
		attackDamage.addPermanentModifier(ATTACK_BONUS);
	}

	@Override
	public void removeAttributeModifiers(AttributeMap attributes) {
		var attackDamage = attributes.getInstance(Attributes.ATTACK_DAMAGE);
		if (attackDamage != null) attackDamage.removeModifier(ATTACK_BONUS.id());
	}
}
