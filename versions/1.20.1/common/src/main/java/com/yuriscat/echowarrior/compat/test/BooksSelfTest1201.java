package com.yuriscat.echowarrior.compat.test;

import com.mojang.authlib.GameProfile;
import com.yuriscat.echowarrior.compat.*;
import com.yuriscat.echowarrior.compat.knowledge.*;
import com.yuriscat.echowarrior.compat.tutorial.*;
import com.yuriscat.echowarrior.compat.menu.*;
import com.yuriscat.echowarrior.compat.recipe.KnowledgeFragmentCollectionRecipe1201;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import java.util.*;

/** Deterministic old-NBT, crafting, menu and held-identity checks; no client classes loaded. */
public final class BooksSelfTest1201 {
    private static int checks;
    private BooksSelfTest1201() {}
    public static void run(MinecraftServer server) {
        checks = 0;
        var entries = KnowledgeCatalog1201.entries();
        check(entries.size() == 40 && entries.stream().filter(e -> !e.illustrations().isEmpty()).count() == 27, "40 pages / 27 strong illustrations");
        check(TutorialManualCatalog1201.pageCount() == 44, "44 tutorial pages");
        String first = entries.get(0).id(), second = entries.get(1).id();
        var knowledgeTab = net.minecraft.core.registries.BuiltInRegistries.CREATIVE_MODE_TAB.get(ModContent1201.id("echo_warrior_knowledge"));
        check(knowledgeTab != null, "knowledge tab registered");
        knowledgeTab.buildContents(new CreativeModeTab.ItemDisplayParameters(server.overworld().enabledFeatures(), false, server.registryAccess()));
        var creativeItems = new ArrayList<>(knowledgeTab.getDisplayItems());
        check(creativeItems.size() == 41 && creativeItems.get(0).is(ModContent1201.KNOWLEDGE_FRAGMENT_COLLECTION),
                "creative tab starts with collection, then 40 fragments");
        check(KnowledgeStackData1201.collectionCounts(creativeItems.get(0)).size() == 40
                && KnowledgeStackData1201.totalCount(creativeItems.get(0)) == 40
                && KnowledgeStackData1201.bookmark(creativeItems.get(0)).equals(first), "creative collection contains all pages once");
        for (int page = 0; page < entries.size(); page++) check(KnowledgeStackData1201.fragmentId(creativeItems.get(page + 1))
                .orElseThrow().equals(entries.get(page).id()), "creative fragment ordering " + page);
        for (var entry : entries) {
            ItemStack fragment = KnowledgeStackData1201.fragment(entry.id());
            check(KnowledgeStackData1201.fragmentId(ItemStack.of(fragment.save(new CompoundTag()))).orElseThrow().equals(entry.id()), "fragment NBT " + entry.id());
        }
        ItemStack empty = new ItemStack(ModContent1201.KNOWLEDGE_FRAGMENT_COLLECTION);
        KnowledgeStackData1201.collectionCounts(empty);
        KnowledgeStackData1201.bookmark(empty);
        check(!empty.hasTag(), "read-only collection access");
        ItemStack collection = KnowledgeStackData1201.collection(Map.of(first, 4, second, 3), second);
        collection.getOrCreateTag().putString("ForeignData", "keep");
        var before = collection.copy();
        KnowledgeStackData1201.setBookmark(collection, first);
        check(KnowledgeStackData1201.isSamePhysicalCollection(before, collection), "bookmark not a new held collection");
        var detached = KnowledgeStackData1201.collectionCounts(collection);
        detached.put(first, 99);
        check(KnowledgeStackData1201.totalCount(collection) == 7, "detached counts");
        check("keep".equals(collection.getTag().getString("ForeignData")), "unrelated NBT preserved");
        var roundTrip = ItemStack.of(collection.save(new CompoundTag()));
        check(KnowledgeStackData1201.collectionCounts(roundTrip).equals(KnowledgeStackData1201.collectionCounts(collection)), "collection NBT roundtrip");
        ItemStack manual = new ItemStack(ModContent1201.TUTORIAL_MANUAL);
        check(TutorialManualStackData1201.bookmark(manual) == 0 && !manual.hasTag(), "read-only manual access");
        TutorialManualStackData1201.setBookmark(manual, Integer.MAX_VALUE);
        check(TutorialManualStackData1201.bookmark(manual) == 43, "bounded page");
        check(TutorialManualStackData1201.isSamePhysicalManual(new ItemStack(ModContent1201.TUTORIAL_MANUAL), manual), "bookmark not a new held manual");
        var player = new ServerPlayer(server, server.overworld(), new GameProfile(UUID.randomUUID(), "BooksSelfTest"));
        player.connection = new net.minecraft.server.network.ServerGamePacketListenerImpl(server,
                new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND), player);
        var grid = new TransientCraftingContainer(player.inventoryMenu, 2, 2);
        var recipe = new KnowledgeFragmentCollectionRecipe1201(ModContent1201.id("knowledge_fragment_collection"), CraftingBookCategory.MISC);
        ItemStack stacked = KnowledgeStackData1201.fragment(first); stacked.setCount(64);
        grid.setItem(0, stacked);
        check(!recipe.matches(grid, server.overworld()), "single occupied slot never crafts");
        grid.setItem(1, KnowledgeStackData1201.fragment(second));
        check(recipe.matches(grid, server.overworld()) && KnowledgeStackData1201.totalCount(recipe.assemble(grid, server.registryAccess())) == 2, "one fragment per occupied slot");
        grid.setItem(0, before.copy());
        var result = recipe.assemble(grid, server.registryAccess());
        check(KnowledgeStackData1201.totalCount(result) == 8 && KnowledgeStackData1201.bookmark(result).equals(second), "merge retains first collection bookmark");
        grid.setItem(1, new ItemStack(Items.STONE));
        check(!recipe.matches(grid, server.overworld()), "unrelated ingredients rejected");
        player.getInventory().setItem(0, new ItemStack(Items.DIAMOND));
        player.getInventory().setItem(40, collection);
        var menu = new KnowledgeReaderMenu1201(10, player.getInventory(), 40);
        player.containerMenu = menu;
        check(menu.stillValid(player) && menu.visiblePages().size() == 2 && menu.currentKnowledgeId().equals(first), "offhand collection source");
        check(menu.clickMenuButton(player, KnowledgeReaderMenu1201.BUTTON_NEXT) && KnowledgeStackData1201.bookmark(collection).equals(second), "authoritative bookmark");
        check(!menu.clickMenuButton(player, KnowledgeReaderMenu1201.BUTTON_NEXT), "no wrap after last page");
        menu.clicked(0, 0, ClickType.THROW, player);
        check(player.getInventory().getItem(40) == collection && menu.getCarried().isEmpty(), "locked book cannot be dropped");
        check(menu.clickMenuButton(player, KnowledgeReaderMenu1201.BUTTON_EXTRACT), "extract selected page");
        check(KnowledgeStackData1201.totalCount(collection) == 6 && player.getInventory().countItem(ModContent1201.KNOWLEDGE_FRAGMENT) == 1, "extraction conserves pages");
        var clientMenu = new KnowledgeReaderMenu1201(11, player.getInventory());
        clientMenu.getSlot(0).set(before.copy());
        check(player.getInventory().getItem(0).is(Items.DIAMOND) && player.getInventory().getItem(40) == collection, "client snapshot never overwrites guessed held slot");
        player.getInventory().setItem(40, manual);
        var tutorial = new TutorialManualMenu1201(12, player.getInventory(), 40);
        player.containerMenu = tutorial;
        for (int page = 0; page < 44; page++) {
            check(tutorial.clickMenuButton(player, TutorialManualMenu1201.BUTTON_JUMP_START + page)
                    && tutorial.currentPage() == page && TutorialManualStackData1201.bookmark(manual) == page, "tutorial bookmark " + page);
        }
        check(!tutorial.clickMenuButton(player, TutorialManualMenu1201.BUTTON_NEXT), "tutorial last boundary");
        player.getInventory().setItem(40, new ItemStack(Items.STONE));
        check(!tutorial.stillValid(player) && !tutorial.clickMenuButton(player, TutorialManualMenu1201.BUTTON_PREVIOUS), "missing source rejects buttons");
        EchoWarrior1201.LOGGER.info("[Compat1201] BOOKS SELFTEST PASSED checks={} knowledge=40 tutorial=44", checks);
    }
    private static void check(boolean condition, String message) {
        if (!condition) throw new IllegalStateException("Books selftest: " + message);
        checks++;
    }
}
