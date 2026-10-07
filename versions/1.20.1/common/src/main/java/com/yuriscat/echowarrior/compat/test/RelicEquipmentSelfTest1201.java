package com.yuriscat.echowarrior.compat.test;

import com.mojang.authlib.GameProfile;
import com.yuriscat.echowarrior.compat.*;
import com.yuriscat.echowarrior.compat.binding.*;
import com.yuriscat.echowarrior.compat.item.*;
import com.yuriscat.echowarrior.compat.item.EchoAccessoryItem1201.AccessoryType;
import com.yuriscat.echowarrior.compat.menu.SummonerMenu1201;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.UUID;

/** Real registered items, vanilla menu clicks, network NBT and two-boot world persistence. */
public final class RelicEquipmentSelfTest1201 {
    private static int checks;
    private RelicEquipmentSelfTest1201() {}

    public static void run(MinecraftServer server) {
        checks = 0;
        EchoProgressionSelfTest1201.run(server.overworld());
        check(ModContent1201.items().size() == 43, "43 complete registered items including exploration");
        ModContent1201.items().forEach((id, item) -> check(BuiltInRegistries.ITEM.get(id) == item, "registered " + id));
        for (String id : new String[]{"echo_warrior", "echo_warrior_accessories", "echo_warrior_knowledge"}) {
            check(BuiltInRegistries.CREATIVE_MODE_TAB.containsKey(new net.minecraft.resources.ResourceLocation("echo_warrior", id)), "creative tab " + id);
        }
        ItemStack pristine = relic(EchoHeroType1201.ROMAN_LEGIONARY);
        EchoRelicState1201.enabledSkills(pristine);
        EchoRelicState1201.activityMode(pristine);
        EchoRelicProgress1201.level(pristine);
        RelicNbt1201.read(pristine).putInt("Escaped", 1);
        check(!pristine.hasTag(), "read paths do not mutate creative catalogue prototype");
        RelicNbt1201.update(pristine, tag -> {});
        check(!pristine.hasTag(), "no-op update does not create empty tag");
        RelicNbt1201.update(pristine, tag -> tag.putString("Foreign", "preserved"));
        CompoundTag before = pristine.getTag();
        RelicNbt1201.update(pristine, tag -> tag.putString("Foreign", "preserved"));
        check(pristine.getTag() == before, "no-op preserves object identity");
        try {
            RelicNbt1201.update(pristine, tag -> { tag.putString("Foreign", "broken"); throw new IllegalArgumentException("fixture"); });
            throw new IllegalStateException("Mutation exception was swallowed");
        } catch (IllegalArgumentException expected) { check(pristine.getTag() == before, "failed mutation is atomic"); }
        RandomSource random = RandomSource.create(120100L);
        for (var hero : EchoHeroType1201.values()) {
            ItemStack relic = relic(hero);
            RelicNbt1201.update(relic, tag -> tag.putString("Foreign", "preserved"));
            check(EchoRelicState1201.ensureInitialized(relic, random, 500), "initializes " + hero);
            CompoundTag initialized = relic.getTag();
            check(!EchoRelicState1201.ensureInitialized(relic, random, 600) && relic.getTag() == initialized, "initialized once without tag churn");
            check(EchoRelicState1201.activityMode(relic) == EchoRelicState1201.ActivityMode.FOLLOW
                    && EchoRelicState1201.alertMode(relic) == EchoRelicState1201.AlertMode.DEFENSIVE, "default modes");
            check(EchoRelicState1201.enabledSkills(relic) == hero.defaultEnabledSkillsMask(), "default skills");
            EchoRelicState1201.setActivityMode(relic, EchoRelicState1201.ActivityMode.WAIT);
            EchoRelicState1201.setAlertMode(relic, EchoRelicState1201.AlertMode.PEACEFUL);
            EchoRelicState1201.toggleSkill(relic, 0);
            EchoRelicState1201.setLegionCooldownEnd(relic, 9876);
            EchoRelicState1201.consumeShieldCharge(relic, 600);
            check(EchoRelicState1201.shieldCharges(relic, 699) == 2 && EchoRelicState1201.shieldCharges(relic, 700) == 3, "charge boundary");
            var progress = EchoRelicProgress1201.addExperience(relic, 1304);
            check(progress.newLevel() == 29 && EchoRelicProgress1201.experience(relic) == 72, "1304 XP just below level 30");
            EchoRelicProgress1201.addExperience(relic, 1);
            check(EchoRelicProgress1201.level(relic) == 30 && EchoRelicProgress1201.experience(relic) == 0, "1305 XP level 30");
            check(EchoRelicProgress1201.maximumHealth(hero, 30) == hero.maximumHealth() * 2
                    && EchoRelicProgress1201.attackDamage(hero, 30) == hero.attackDamage() * 2, "growth endpoints");
            ItemStack loaded = ItemStack.of(relic.save(new CompoundTag()).copy());
            check(ItemStack.matches(relic, loaded) && EchoRelicState1201.activityMode(loaded) == EchoRelicState1201.ActivityMode.WAIT
                    && !EchoRelicState1201.skillEnabled(loaded, 0) && EchoRelicState1201.legionCooldownEnd(loaded) == 9876, "NBT retains modes/skills/cooldowns");
            FriendlyByteBuf buffer = new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
            try { buffer.writeItem(relic); check(ItemStack.matches(relic, buffer.readItem()), "item packet preserves all state"); }
            finally { buffer.release(); }
            check("preserved".equals(relic.getTag().getString("Foreign")), "state updates preserve foreign NBT");
        }
        ItemStack overflow = relic(EchoHeroType1201.ROMAN_LEGIONARY);
        EchoRelicProgress1201.addExperience(overflow, 16);
        check(EchoRelicProgress1201.addExperience(overflow, Integer.MAX_VALUE).newLevel() == EchoRelicProgress1201.maxLevel(), "large XP addition cannot overflow");
        ItemStack archer = relic(EchoHeroType1201.EGYPTIAN_ARCHER);
        EchoRelicState1201.ensureInitialized(archer, random, 500);
        check(EchoRelicState1201.egyptianArrowMode(archer) == EchoRelicState1201.EgyptianArrowMode.OFF, "arrows default off");
        check(EchoRelicState1201.cycleEgyptianArrowMode(archer, 500) && EchoRelicState1201.skillEnabled(archer, 1), "leaf enables arrow skill");
        check(!EchoRelicState1201.cycleEgyptianArrowMode(archer, 509), "arrow switch cooldown");
        check(EchoRelicState1201.cycleEgyptianArrowMode(archer, 510)
                && EchoRelicState1201.egyptianArrowMode(archer) == EchoRelicState1201.EgyptianArrowMode.CONE, "cone next");
        check(EchoRelicState1201.cycleEgyptianArrowMode(archer, 520) && !EchoRelicState1201.skillEnabled(archer, 1), "off disables arrow skill");
        boolean legalRolls = true;
        for (int roll = 0; roll < 500; roll++) {
            ItemStack stack = relic(EchoHeroType1201.ROMAN_LEGIONARY);
            EchoRelicState1201.ensureInitialized(stack, random, 0);
            int mask = EchoRelicState1201.traitMask(stack);
            legalRolls &= Integer.bitCount(mask) >= 2 && Integer.bitCount(mask) <= 4;
            for (var first : EchoTrait1201.values()) for (var second : EchoTrait1201.values()) {
                if (first != second && (mask & first.mask()) != 0 && (mask & second.mask()) != 0) legalRolls &= !first.conflictsWith(second);
            }
        }
        check(legalRolls, "500 seeded rolls enforce trait count and exclusions");
        menuTransactions(server);
        FixtureSave saved = server.overworld().getDataStorage().computeIfAbsent(FixtureSave::load, FixtureSave::new, "echo_warrior_relic_selftest_1201");
        if (saved.boots > 0) {
            check(EchoRelicState1201.egyptianArrowMode(saved.relic) == EchoRelicState1201.EgyptianArrowMode.CONE
                    && EchoRelicProgress1201.level(saved.relic) == 2
                    && "fixture".equals(saved.relic.getTag().getString("Foreign")), "real two-boot saved relic");
        }
        EchoRelicState1201.cycleEgyptianArrowMode(archer, 530);
        EchoRelicState1201.cycleEgyptianArrowMode(archer, 540);
        EchoRelicProgress1201.addExperience(archer, 17);
        RelicNbt1201.update(archer, tag -> tag.putString("Foreign", "fixture"));
        saved.relic = archer.copy(); saved.boots++; saved.setDirty();
        EchoWarrior1201.LOGGER.info("[Compat1201] RELIC EQUIPMENT SELFTEST PASSED checks={} boot={}", checks, saved.boots);
    }

    private static void menuTransactions(MinecraftServer server) {
        ServerPlayer player = new ServerPlayer(server, server.overworld(), new GameProfile(UUID.randomUUID(), "RelicMenuTest"));
        ItemStack summoner = new ItemStack(ModContent1201.ECHO_SUMMONER);
        player.getInventory().setItem(0, summoner);
        var binding = EchoBindingSystem1201.synchronize(server.overworld(), summoner);
        try {
            SummonerMenu1201 menu = new SummonerMenu1201(12, player.getInventory(), 0);
            player.containerMenu = menu;
            ItemStack relic = relic(EchoHeroType1201.ROMAN_LEGIONARY);
            EchoRelicState1201.ensureInitialized(relic, RandomSource.create(22), 0);
            EchoRelicProgress1201.addExperience(relic, 100);
            EchoRelicState1201.toggleSkill(relic, 0);
            menu.setCarried(relic.copy());
            long revision = binding.stateRevision();
            menu.clicked(35, 0, ClickType.PICKUP, player);
            check(menu.getCarried().isEmpty() && ItemStack.matches(binding.contents().get(7), relic)
                    && binding.stateRevision() == revision + 1, "relic onto locked summoner with all state");
            ItemStack other = relic(EchoHeroType1201.EGYPTIAN_ARCHER);
            menu.setCarried(other);
            menu.clicked(35, 0, ClickType.PICKUP, player);
            check(menu.getCarried() == other && binding.stateRevision() == revision + 1, "occupied relic bundle insertion preserves cursor");
            menu.setCarried(new ItemStack(ModContent1201.accessory(AccessoryType.PLATE_ARMOR)));
            menu.clicked(35, 0, ClickType.PICKUP, player);
            check(menu.getCarried().isEmpty() && binding.contents().get(0).is(ModContent1201.accessory(AccessoryType.PLATE_ARMOR)), "accessory first free slot");
            ItemStack duplicate = new ItemStack(ModContent1201.accessory(AccessoryType.PLATE_ARMOR));
            duplicate.setHoverName(Component.literal("Renamed fixture"));
            menu.setCarried(duplicate);
            revision = binding.stateRevision();
            menu.clicked(1, 0, ClickType.PICKUP, player);
            menu.clicked(35, 0, ClickType.PICKUP, player);
            check(menu.getCarried() == duplicate && binding.contents().get(1).isEmpty()
                    && binding.stateRevision() == revision, "renaming cannot bypass duplicate rule; failed clicks do not consume");
            menu.setCarried(ItemStack.EMPTY);
            for (int index = 1; index < 6; index++) {
                player.getInventory().setItem(9, new ItemStack(ModContent1201.accessory(AccessoryType.values()[index])));
                menu.clicked(8, 0, ClickType.QUICK_MOVE, player);
            }
            check(binding.contents().subList(0, 6).stream().noneMatch(ItemStack::isEmpty), "all six accessory slots usable");
            player.getInventory().setItem(9, new ItemStack(ModContent1201.accessory(AccessoryType.TWIN_OATH_BADGE)));
            menu.clicked(8, 0, ClickType.QUICK_MOVE, player);
            check(player.getInventory().getItem(9).getCount() == 1, "full accessory slots preserve source");
            var invalid = new java.util.ArrayList<>(binding.contents());
            invalid.set(1, duplicate.copy());
            revision = binding.stateRevision();
            check(!binding.commitEquipment(revision, invalid) && binding.stateRevision() == revision, "authority independently rejects duplicate NBT variant");
            UUID spirit = UUID.randomUUID();
            long generation = binding.activate(player.getUUID(), spirit, binding.snapshot());
            menu.broadcastChanges();
            menu.clicked(0, 0, ClickType.PICKUP, player);
            check(!menu.getCarried().isEmpty() && binding.matches(spirit, generation), "accessory removal does not dismiss hero");
            menu.clicked(9, 0, ClickType.PICKUP, player);
            menu.removed(player);
            menu = new SummonerMenu1201(13, player.getInventory(), 0); player.containerMenu = menu;
            check(menu.getSlot(0).getItem().isEmpty() && player.getInventory().getItem(10).getCount() == 1, "accessory not restored after close/reopen");
            var sameIdentity = new java.util.ArrayList<>(binding.contents());
            EchoRelicState1201.toggleSkill(sameIdentity.get(7), 1);
            check(binding.commitEquipment(binding.stateRevision(), sameIdentity) && binding.matches(spirit, generation), "skill edit preserves active identity");
            menu.broadcastChanges();
            revision = binding.stateRevision();
            menu.clicked(7, 0, ClickType.PICKUP, player);
            check(!binding.active() && binding.spiritId() == null && !binding.matches(spirit, generation)
                    && binding.stateRevision() == revision + 1, "relic withdrawal and invalidation share one revision");
            check(EchoRelicProgress1201.level(menu.getCarried()) == EchoRelicProgress1201.level(relic)
                    && !EchoRelicState1201.skillEnabled(menu.getCarried(), 0), "withdrawal preserves growth and skill choice");
            menu.clicked(7, 0, ClickType.PICKUP, player);
            generation = binding.activate(player.getUUID(), spirit, binding.snapshot()); menu.broadcastChanges();
            menu.setCarried(other);
            revision = binding.stateRevision();
            menu.clicked(7, 0, ClickType.PICKUP, player);
            check(!binding.active() && binding.stateRevision() == revision + 1
                    && EchoHeroType1201.fromRelic(binding.contents().get(7)) == EchoHeroType1201.EGYPTIAN_ARCHER
                    && EchoHeroType1201.fromRelic(menu.getCarried()) == EchoHeroType1201.ROMAN_LEGIONARY, "direct replacement returns old relic and invalidates binding atomically");
            check(!binding.track(spirit, generation, binding.snapshot()), "late old spirit cannot restore a replaced relic");
            menu.setCarried(ItemStack.EMPTY);
        } finally {
            player.containerMenu = player.inventoryMenu;
            EchoBindingSavedData1201.get(server).remove(binding.summonerId());
        }
    }

    private static ItemStack relic(EchoHeroType1201 hero) { return new ItemStack(ModContent1201.relic(hero)); }
    private static void check(boolean condition, String label) {
        if (!condition) throw new IllegalStateException("Relic/equipment test failed: " + label);
        checks++;
    }
    private static final class FixtureSave extends SavedData {
        private int boots;
        private ItemStack relic = ItemStack.EMPTY;
        static FixtureSave load(CompoundTag tag) {
            FixtureSave result = new FixtureSave(); result.boots = tag.getInt("Boots");
            result.relic = ItemStack.of(tag.getCompound("Relic").copy()); return result;
        }
        @Override public CompoundTag save(CompoundTag tag) {
            tag.putInt("Boots", boots); tag.put("Relic", relic.save(new CompoundTag())); return tag;
        }
    }
}
