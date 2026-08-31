package com.yuriscat.echowarrior;

import com.yuriscat.echowarrior.menu.SummonerMenu;
import com.yuriscat.echowarrior.menu.KnowledgeReaderMenu;
import com.yuriscat.echowarrior.menu.RecyclerMenu;
import com.yuriscat.echowarrior.menu.TutorialManualMenu;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;

public final class ModMenus {
	public static final ExtendedMenuType<SummonerMenu, Integer> SUMMONER = Registry.register(
			BuiltInRegistries.MENU,
			EchoWarrior.id("summoner"),
			new ExtendedMenuType<>(SummonerMenu::new, ByteBufCodecs.VAR_INT)
	);
	public static final ExtendedMenuType<KnowledgeReaderMenu, Integer> KNOWLEDGE_READER = Registry.register(
			BuiltInRegistries.MENU,
			EchoWarrior.id("knowledge_reader"),
			new ExtendedMenuType<>(KnowledgeReaderMenu::new, ByteBufCodecs.VAR_INT)
	);
	public static final ExtendedMenuType<TutorialManualMenu, Integer> TUTORIAL_MANUAL = Registry.register(
			BuiltInRegistries.MENU,
			EchoWarrior.id("tutorial_manual"),
			new ExtendedMenuType<>(TutorialManualMenu::new, ByteBufCodecs.VAR_INT)
	);
	public static final MenuType<RecyclerMenu> RECYCLER = Registry.register(
			BuiltInRegistries.MENU,
			EchoWarrior.id("recycler"),
			new MenuType<>(RecyclerMenu::new, FeatureFlags.VANILLA_SET)
	);

	private ModMenus() {
	}

	public static void initialize() {
	}
}
