package com.yuriscat.echowarrior;

import com.yuriscat.echowarrior.menu.KnowledgeReaderMenu;
import com.yuriscat.echowarrior.menu.RecyclerMenu;
import com.yuriscat.echowarrior.menu.SummonerMenu;
import com.yuriscat.echowarrior.menu.TutorialManualMenu;
import com.yuriscat.echowarrior.platform.RegistryRegistrar;
import net.minecraft.world.inventory.MenuType;

public final class ModMenus {
	public static MenuType<SummonerMenu> SUMMONER;
	public static MenuType<KnowledgeReaderMenu> KNOWLEDGE_READER;
	public static MenuType<TutorialManualMenu> TUTORIAL_MANUAL;
	public static MenuType<RecyclerMenu> RECYCLER;

	private ModMenus() {
	}

	public static void register(
			RegistryRegistrar<MenuType<?>> registrar,
			MenuType<SummonerMenu> summoner,
			MenuType<KnowledgeReaderMenu> knowledgeReader,
			MenuType<TutorialManualMenu> tutorialManual,
			MenuType<RecyclerMenu> recycler
	) {
		SUMMONER = cast(registrar.register(EchoWarrior.id("summoner"), summoner));
		KNOWLEDGE_READER = cast(registrar.register(EchoWarrior.id("knowledge_reader"), knowledgeReader));
		TUTORIAL_MANUAL = cast(registrar.register(EchoWarrior.id("tutorial_manual"), tutorialManual));
		RECYCLER = cast(registrar.register(EchoWarrior.id("recycler"), recycler));
	}

	@SuppressWarnings("unchecked")
	private static <T extends net.minecraft.world.inventory.AbstractContainerMenu> MenuType<T> cast(MenuType<?> type) {
		return (MenuType<T>)type;
	}
}
