package com.yuriscat.echowarrior;

import com.yuriscat.echowarrior.menu.SummonerMenu;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;

public final class ModMenus {
	public static final ExtendedMenuType<SummonerMenu, Integer> SUMMONER = Registry.register(
			BuiltInRegistries.MENU,
			EchoWarrior.id("summoner"),
			new ExtendedMenuType<>(SummonerMenu::new, ByteBufCodecs.VAR_INT)
	);

	private ModMenus() {
	}

	public static void initialize() {
	}
}
