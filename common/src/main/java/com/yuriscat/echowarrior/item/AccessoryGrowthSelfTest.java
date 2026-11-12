package com.yuriscat.echowarrior.item;

import com.yuriscat.echowarrior.EchoWarrior;
import com.yuriscat.echowarrior.ModEffects;
import com.yuriscat.echowarrior.ModDamageTypes;
import com.yuriscat.echowarrior.binding.EchoBindingSystem;
import com.yuriscat.echowarrior.entity.EchoWarriorEntity;
import com.yuriscat.echowarrior.menu.SummonerMenu;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Real registered attributes, damage hooks, periodic healing and dismissed menu previews. */
public final class AccessoryGrowthSelfTest {
    private AccessoryGrowthSelfTest() { }

    public static void run(ServerLevel level) {
        List<LivingEntity> fixtures = new ArrayList<>();
        try {
            for (String hero : new String[]{"roman_legionary", "aztec_warrior", "egyptian_archer", "guandao_warrior", "japanese_samurai"}) {
                EchoWarriorEntity echo = (EchoWarriorEntity)create(level, "echo_warrior:" + hero + "_echo", fixtures);
                LivingEntity living = echo.livingEntity();
                double startHealth = living.getAttributeBaseValue(Attributes.MAX_HEALTH);
                double startAttack = living.getAttributeBaseValue(Attributes.ATTACK_DAMAGE);
                for (int lv : new int[]{1, 30, 100}) {
                    double h = startHealth * (1 + (lv - 1) / 29.0);
                    double a = startAttack * (1 + (lv - 1) / 29.0);
                    living.getAttribute(Attributes.MAX_HEALTH).setBaseValue(h);
                    living.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(a);
                    living.setHealth(7);
                    List<ItemStack> tank = items("heart_sprout_amber", "feast_ham", "peacemaker", "fractured_crystal_blade");
                    EchoAccessorySystem.apply(echo, tank);
                    check(close(living.getMaxHealth(), h * 2.15), hero + " level " + lv + " additive health");
                    check(close(living.getAttributeValue(Attributes.ATTACK_DAMAGE), a), "opposite attack percentages cancel");
                    EchoAccessorySystem.apply(echo, tank);
                    check(close(living.getMaxHealth(), h * 2.15) && close(living.getHealth(), 7), "reapply neither compounds nor heals");
                    List<ItemStack> offense = items("battle_worn_whetstone", "mountain_burden_blade", "fractured_crystal_blade",
                            "twin_oath_badge", "battle_blindfold", "memory_ritual_knife");
                    EchoAccessorySystem.apply(echo, offense);
                    check(close(living.getAttributeValue(Attributes.ATTACK_DAMAGE), a * 2.6), "six distinct attack bonuses add");
                    EchoAccessorySystem.apply(echo, items("hollow_bird_bone"));
                    check(close(living.getMaxHealth(), h * 0.75), "negative health scales");
                    EchoAccessorySystem.apply(echo, List.of());
                    check(close(living.getMaxHealth(), h) && close(living.getAttributeValue(Attributes.ATTACK_DAMAGE), a)
                            && close(living.getHealth(), 7), "unequip restores base without healing");
                    EchoAccessorySystem.apply(echo, tank);
                    living.setHealth(living.getMaxHealth());
                    EchoAccessorySystem.apply(echo, List.of());
                    check(close(living.getHealth(), h), "removing health clamps to new maximum");
                }
            }
            testComposition(level, fixtures);
            testHealing(level, fixtures);
            testMenu(level);
            EchoWarrior.LOGGER.info("ACCESSORY GROWTH SELFTEST PASSED heroes=5 levels=1,30,100 stacking=additive healing=fractional windows=preserved menu=preview formation=30pct");
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Accessory fixture reflection failed", ex);
        } finally {
            for (LivingEntity fixture : fixtures) fixture.discard();
        }
    }

    private static void testComposition(ServerLevel level, List<LivingEntity> fixtures) {
        EchoWarriorEntity echo = (EchoWarriorEntity)create(level, "echo_warrior:roman_legionary_echo", fixtures);
        var living = echo.livingEntity();
        var attack = living.getAttribute(Attributes.ATTACK_DAMAGE);
        attack.setBaseValue(10);
        attack.addTransientModifier(new AttributeModifier(EchoWarrior.id("accessory_test_flat"), 5, AttributeModifier.Operation.ADD_VALUE));
        EchoAccessorySystem.apply(echo, items("battle_worn_whetstone"));
        check(close(attack.getValue(), 17), "accessory uses base, not another modifier's additive bonus");
        living.addEffect(new MobEffectInstance(ModEffects.WEAPONS_RAISED, 100));
        check(close(attack.getValue(), 22.1), "new formation includes accessory and other flat attack");
        EchoAccessorySystem.apply(echo, items("battle_worn_whetstone"));
        check(close(attack.getValue(), 22.1), "reapply under formation does not feed back");
        living.removeEffect(ModEffects.WEAPONS_RAISED);
        check(close(attack.getValue(), 17), "formation removal keeps accessory");
        SimpleContainer contents = new SimpleContainer(6);
        contents.setItem(0, item("battle_blindfold_accessory"));
        contents.setItem(1, item("hawkeye_lens_accessory"));
        check(close(EchoAccessorySystem.proactiveRange(contents, 16, false), 4), "rejected lens rule change remains rejected");
        EchoAccessorySystem.apply(echo, items("fractured_crystal_blade"));
        check(close(living.getAttributeValue(Attributes.ARMOR), 2), "crystal retains fixed -6 armor");
    }

    private static void testHealing(ServerLevel level, List<LivingEntity> fixtures) throws ReflectiveOperationException {
        EchoWarriorEntity echo = (EchoWarriorEntity)create(level, "echo_warrior:roman_legionary_echo", fixtures);
        LivingEntity living = echo.livingEntity();
        var player = player(level);
        ItemStack summoner = summoner(items("blood_pact_fang", "sunwheel_garland", "moondew_bottle"), item("roman_legionary_relic"));
        var binding = EchoBindingSystem.registerOrSynchronize(level, summoner);
        echo.bindTo(player, binding.summonerId());
        living.getAttribute(Attributes.MAX_HEALTH).setBaseValue(132.41379310344828);
        EchoAccessorySystem.apply(echo, items("heart_sprout_amber", "feast_ham", "peacemaker"));
        check(close(living.getMaxHealth(), 284.6896551724138), "healing fixture maximum");
        LivingEntity target = create(level, "minecraft:zombie", fixtures);
        living.setPos(0, 100, 0); target.setPos(1, 100, 0);
        living.setHealth(10);
        long seed = 0;
        for (; seed < 100000; seed++) {
            living.getRandom().setSeed(seed);
            if (living.getRandom().nextFloat() < 0.30F) break;
        }
        check(seed < 100000, "deterministic successful roll found");
        living.getRandom().setSeed(seed);
        var source = level.damageSources().mobAttack(living);
        check(target.hurtServer(level, source, 1), "real direct damage accepted");
        check(close(living.getHealth(), 26.23448275862069), "real damage hook heals using final max health");
        EchoAccessorySystem.afterDamage(target, source, 1, 1, false);
        check(close(living.getHealth(), 26.23448275862069), "same attack window does not heal twice");

        // A distinct projectile is a fresh window, but excluded/blocked/zero damage must not consume it.
        var arrow = (net.minecraft.world.entity.projectile.Projectile)BuiltInRegistries.ENTITY_TYPE
                .getValue(net.minecraft.resources.Identifier.parse("minecraft:arrow"))
                .create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        arrow.setOwner(living);
        var arrowSource = level.damageSources().arrow((net.minecraft.world.entity.projectile.arrow.Arrow)arrow, living);
        float before = living.getHealth();
        living.getRandom().setSeed(seed);
        EchoAccessorySystem.afterDamage(target, arrowSource, 1, 1, true);
        EchoAccessorySystem.afterDamage(target, arrowSource, 0, 0, false);
        EchoAccessorySystem.afterDamage(target, ModDamageTypes.source(level, ModDamageTypes.BLEEDING, arrow, living), 1, 1, false);
        check(close(living.getHealth(), before), "blocked, zero and bleeding do not heal");
        EchoAccessorySystem.afterDamage(target, arrowSource, 1, 1, false);
        check(close(living.getHealth(), before + 16.23448275862069), "eligible distinct projectile heals independently");
        arrow.discard();

        long due = Math.floorMod(living.getUUID().getLeastSignificantBits(), 50L);
        List<ItemStack> both = items("sunwheel_garland", "moondew_bottle");
        living.setHealth(10);
        EchoAccessorySystem.tickPeriodicHealing(level, living, both, due + 1, 0);
        check(close(living.getHealth(), 10), "periodic interval retained");
        EchoAccessorySystem.tickPeriodicHealing(level, living, items("moondew_bottle"), due, 0);
        EchoAccessorySystem.tickPeriodicHealing(level, living, items("sunwheel_garland"), due, 13000);
        check(close(living.getHealth(), 10), "wrong time never heals");
        EchoAccessorySystem.tickPeriodicHealing(level, living, both, due, 0);
        check(close(living.getHealth(), 13.846896551724139), "day uses final max health and heals only once");
        EchoAccessorySystem.tickPeriodicHealing(level, living, both, due + 50, 13000);
        check(close(living.getHealth(), 17.693793103448278), "night heals once with fractional precision");
        var nether = level.getServer().getLevel(net.minecraft.world.level.Level.NETHER);
        check(nether != null, "nether fixture available");
        EchoAccessorySystem.tickPeriodicHealing(nether, living, both, due + 100, 0);
        check(close(living.getHealth(), 17.693793103448278), "dimension without skylight does not heal");
        living.setHealth(living.getMaxHealth() - 0.25F);
        EchoAccessorySystem.tickPeriodicHealing(level, living, both, due, 0);
        check(close(living.getHealth(), living.getMaxHealth()), "periodic recovery clamps at full health");
    }

    private static void testMenu(ServerLevel level) {
        var player = player(level);
        ItemStack relic = item("roman_legionary_relic");
        EchoRelicState.ensureInitialized(relic, player.getRandom(), level.getGameTime());
        EchoRelicProgress.addExperience(relic, 1305);
        ItemStack summoner = summoner(items("heart_sprout_amber", "feast_ham", "peacemaker", "battle_worn_whetstone"), relic);
        player.getInventory().setItem(0, summoner);
        var menu = new SummonerMenu(91, player.getInventory(), 0);
        player.containerMenu = menu;
        menu.broadcastChanges();
        check(menu.spiritMaximumHealth() == 1290, "dismissed menu previews 60 times 2.15");
        double expectedAttack = EchoRelicState.attackDamage(relic) * 0.8;
        check(menu.spiritAttackDamage() == Math.round(expectedAttack * 10), "dismissed attack preview adds positive and negative percentages");
        check(close(menu.skillAttackDamage(), expectedAttack), "new skill tooltip uses accessory-adjusted preview");
        menu.removed(player);
    }

    private static ServerPlayer player(ServerLevel level) {
        return new ServerPlayer(level.getServer(), level,
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "AccessoryTest"),
                net.minecraft.server.level.ClientInformation.createDefault());
    }
    private static ItemStack summoner(List<ItemStack> accessories, ItemStack relic) {
        ItemStack result = item("test_echo_summoner");
        var slots = new ArrayList<ItemStack>();
        for (int i = 0; i < 8; i++) slots.add(i < accessories.size() ? accessories.get(i).copy() : ItemStack.EMPTY);
        slots.set(7, relic.copy());
        result.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(slots));
        return result;
    }
    private static ItemStack item(String path) { return new ItemStack(BuiltInRegistries.ITEM.getValue(EchoWarrior.id(path))); }
    private static List<ItemStack> items(String... names) {
        return java.util.Arrays.stream(names).map(name -> item(name + "_accessory")).toList();
    }
    private static LivingEntity create(ServerLevel level, String id, List<LivingEntity> fixtures) {
        var result = (LivingEntity)BuiltInRegistries.ENTITY_TYPE.getValue(net.minecraft.resources.Identifier.parse(id))
                .create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        check(result != null, "registered entity " + id);
        fixtures.add(result);
        return result;
    }
    private static boolean close(double a, double b) { return Math.abs(a - b) < 0.0001; }
    private static void check(boolean ok, String message) {
        if (!ok) throw new IllegalStateException("Accessory growth: " + message);
    }
}
