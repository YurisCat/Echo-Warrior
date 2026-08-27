package com.yuriscat.echowarrior.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.List;
import java.util.function.Consumer;

public final class EchoModuleItem extends Item implements EchoSummonerModule {
	private final ModuleType type;

	public EchoModuleItem(Properties properties, ModuleType type) {
		super(properties);
		this.type = type;
	}

	public ModuleType type() {
		return this.type;
	}

	@Override
	public boolean canInstall(ItemStack module, ItemStack summoner, int moduleSlot, List<ItemStack> installedModules) {
		return installedModules.stream().noneMatch(stack -> stack.is(this));
	}

	@Override
	public void appendHoverText(
			ItemStack stack,
			TooltipContext context,
			TooltipDisplay display,
			Consumer<Component> builder,
			TooltipFlag flag
	) {
		builder.accept(Component.translatable("item.echo_warrior.module.echo_only"));
		builder.accept(Component.translatable("item.echo_warrior.module." + this.type.id + ".effect"));
	}

	public enum ModuleType {
		PLATE_ARMOR("plate_armor"),
		CHAINMAIL_ARMOR("chainmail_armor"),
		SPIKED_ARMOR("spiked_armor");

		private final String id;

		ModuleType(String id) {
			this.id = id;
		}

		public String id() {
			return this.id;
		}
	}
}
