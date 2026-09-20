package com.yuriscat.echowarrior.compat.client;

import com.yuriscat.echowarrior.compat.*;
import com.yuriscat.echowarrior.compat.binding.*;
import com.yuriscat.echowarrior.compat.entity.EchoWarriorEntity1201;
import com.yuriscat.echowarrior.compat.item.*;
import com.yuriscat.echowarrior.compat.menu.SummonerMenu1201;
import com.yuriscat.echowarrior.compat.test.HeroClientFixture1201;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.world.InteractionHand;
import java.util.concurrent.CompletableFuture;

/** Real menu buttons and network state for every hero; previews are rendered by the normal screen. */
public final class HeroMenuClientSelfTest1201 {
    private static int round, phase, expectedSkills, expectedArrow, visibleAfter;
    private static long deadline;
    private static CompletableFuture<HeroClientFixture1201> setup;
    private static CompletableFuture<Void> task;
    private static CompletableFuture<java.util.UUID> entityIdentity;
    private static CompletableFuture<Boolean> combatCheck;
    private static HeroClientFixture1201 fixture;
    private static net.minecraft.world.entity.LivingEntity clockPreview;
    private static int previewAgeAtStart, previewPlayerAgeAtStart;
    private HeroMenuClientSelfTest1201() {}

    public static boolean tick(Minecraft client) {
        if (round == EchoHeroType1201.values().length) return true;
        var server = client.getSingleplayerServer();
        if (phase != 0 && phase != 99 && System.nanoTime() > deadline) finish(client, "Hero UI timeout round=" + round + " phase=" + phase);
        var menu = client.player.containerMenu instanceof SummonerMenu1201 m ? m : null;
        switch (phase) {
            case 0 -> {
                var id = client.player.getUUID();
                var hero = EchoHeroType1201.values()[round];
                setup = server.submit(() -> HeroClientFixture1201.prepare(server.getPlayerList().getPlayer(id), hero));
                advance(1);
            }
            case 1 -> { if (setup.isDone()) { fixture = setup.join(); advance(2); } }
            case 2 -> {
                if (fixture.ids.get(0).equals(SummonerData1201.summonerId(client.player.getInventory().getItem(0)))) {
                    client.player.getInventory().selected = 0;
                    client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND);
                    advance(3);
                }
            }
            case 3 -> {
                if (menu != null && client.screen instanceof SummonerScreen1201 && menu.relicLevel() == 1 && menu.heroType() == round) {
                    check(menu.spiritHealth() == Math.round(fixture.hero.baseMaximumHealth()) && menu.spiritHealth() == menu.spiritMaximumHealth(), "initial full HP");
                    check(menu.skillCount() == fixture.hero.skillCount(), "skill count");
                    checkRelicIcon(client);
                    if (fixture.hero == EchoHeroType1201.GUANDAO_WARRIOR) GuandaoPresentationClientSelfTest1201.run(client);
                    clockPreview = ((SummonerScreen1201)client.screen).previewForSelfTest();
                    check(clockPreview != null, "preview exists");
                    previewAgeAtStart = clockPreview.tickCount;
                    previewPlayerAgeAtStart = client.player.tickCount;
                    advance(30);
                }
            }
            case 30 -> {
                check(client.screen instanceof SummonerScreen1201 screen
                        && screen.previewForSelfTest() == clockPreview, "preview identity stable across frames");
                check(client.level.getEntity(clockPreview.getId()) == null, "preview stays outside world simulation");
                if (client.player.tickCount - previewPlayerAgeAtStart >= 40) {
                    int elapsed = client.player.tickCount - previewPlayerAgeAtStart;
                    check(Math.abs(clockPreview.tickCount - previewAgeAtStart - elapsed) <= 1,
                            "preview whole-tick clock advances with client ticks for every hero");
                    button(client, menu, SummonerMenu1201.BUTTON_SUMMON_OR_DISMISS); advance(4);
                }
            }
            case 4 -> {
                if (menu != null && menu.spiritPresent() && findClientSpirit(client) != null) {
                    check(menu.fuelAmount() == 900, "single summon fuel charge");
                    expectedSkills = menu.enabledSkills() ^ 1;
                    button(client, menu, SummonerMenu1201.BUTTON_SKILL_START); advance(5);
                }
            }
            case 5 -> {
                if (menu != null && menu.enabledSkills() == expectedSkills) {
                    client.player.closeContainer();
                    client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND);
                    advance(6);
                }
            }
            case 6 -> {
                if (menu != null && menu.sourceInventorySlot() == 0 && menu.relicLevel() == 1 && menu.spiritPresent()) {
                    check(menu.enabledSkills() == expectedSkills, "skill disabled after reopen");
                    button(client, menu, SummonerMenu1201.BUTTON_ACTIVITY_START + 2); advance(7);
                }
            }
            case 7 -> {
                if (menu != null && menu.activityMode() == 2) {
                    button(client, menu, SummonerMenu1201.BUTTON_ALERT_START + 1); advance(8);
                }
            }
            case 8 -> {
                if (menu != null && menu.alertMode() == 1) {
                    if (fixture.hero == EchoHeroType1201.EGYPTIAN_ARCHER) {
                        check(menu.egyptianArrowMode() == 0, "default arrow off");
                        expectedArrow = 1;
                        button(client, menu, SummonerMenu1201.BUTTON_SKILL_START + 1);
                        advance(9);
                    } else advance(11);
                }
            }
            case 9 -> {
                if (menu != null && menu.egyptianArrowMode() == expectedArrow) {
                    visibleAfter = client.player.tickCount + 22; advance(10);
                }
            }
            case 10 -> {
                if (client.player.tickCount >= visibleAfter) {
                    if (expectedArrow == 0) advance(11);
                    else {
                        expectedArrow = (expectedArrow + 1) % 3;
                        button(client, menu, SummonerMenu1201.BUTTON_SKILL_START + 1); advance(9);
                    }
                }
            }
            case 11 -> {
                var current = fixture;
                task = server.submit(() -> {
                    var binding = EchoBindingSavedData1201.get(server).get(current.ids.get(0));
                    var spirit = EchoBindingSystem1201.findLoaded(server, binding.spiritId());
                    if (spirit == null || binding.activityMode() != 2 || binding.alertMode() != 1 || (binding.enabledSkills() & 1) != 0)
                        throw new IllegalStateException("Hero buttons did not reach server authority");
                    spirit.livingEntity().setHealth(spirit.livingEntity().getMaxHealth() - 5);
                    EchoBindingSystem1201.track(spirit);
                });
                advance(12);
            }
            case 12 -> {
                if (task.isDone() && menu != null && menu.spiritHealth() == menu.spiritMaximumHealth() - 5) {
                    task.join();
                    visibleAfter = client.player.tickCount + 5;
                    button(client, menu, SummonerMenu1201.BUTTON_SUMMON_OR_DISMISS); advance(13);
                }
            }
            case 13 -> {
                if (menu != null && !menu.spiritPresent() && findClientSpirit(client) == null && client.player.tickCount >= visibleAfter) {
                    check(menu.fuelAmount() == 900, "dismiss doesn't charge again");
                    entityIdentity = null;
                    button(client, menu, SummonerMenu1201.BUTTON_SUMMON_OR_DISMISS); advance(14);
                }
            }
            case 14 -> {
                if (menu != null && menu.spiritPresent() && menu.fuelAmount() == 800 && findClientSpirit(client) != null) {
                    check(menu.spiritHealth() == menu.spiritMaximumHealth(), "resummon restores full HP");
                    var current = fixture;
                    task = server.submit(current::startCombat);
                    advance(15);
                }
            }
            case 15 -> {
                if (task.isDone()) {
                    task.join();
                    var current = fixture;
                    combatCheck = server.submit(current::combatHit);
                    advance(16);
                }
            }
            case 16 -> {
                if (combatCheck.isDone()) {
                    if (combatCheck.join()) finish(client, null);
                    else { var current = fixture; combatCheck = server.submit(current::combatHit); }
                }
            }
            case 99 -> {
                if (task.isDone()) {
                    task.join();
                    EchoWarrior1201.LOGGER.info("[Compat1201] HERO MENU CLIENT SELFTEST PASSED hero={} summon=real preview=rendered preview-clock=40-ticks-detached skills=persisted modes=authority health=synced dismiss=entity-removed inventory=restored combat=actual-ai-hit shift-icon=gui-only", fixture.hero);
                    fixture = null; entityIdentity = null; phase = 0; round++;
                }
            }
            default -> { }
        }
        return round == EchoHeroType1201.values().length;
    }
    private static EchoWarriorEntity1201 findClientSpirit(Minecraft client) {
        if (entityIdentity == null) {
            var server = client.getSingleplayerServer();
            var id = fixture.ids.get(0);
            entityIdentity = server.submit(() -> EchoBindingSavedData1201.get(server).get(id).spiritId());
            return null;
        }
        if (!entityIdentity.isDone()) return null;
        var id = entityIdentity.join();
        for (var entity : client.level.entitiesForRendering()) if (entity instanceof EchoWarriorEntity1201 echo
                && entity.getUUID().equals(id) && !entity.isRemoved()) return echo;
        return null;
    }
    private static void button(Minecraft client, SummonerMenu1201 menu, int id) {
        client.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }
    private static void checkRelicIcon(Minecraft client) {
        var stack = client.player.getInventory().getItem(0);
        var savedTag = stack.getTag() == null ? null : stack.getTag().copy();
        var graphics = new net.minecraft.client.gui.GuiGraphics(client, client.renderBuffers().bufferSource());
        try {
            TooltipShiftState1201.setClientShiftDownSupplier(() -> true);
            graphics.renderItem(stack, -100, -100);
            graphics.flush();
            float expected = switch (fixture.hero) {
                case ROMAN_LEGIONARY -> 0.1F;
                case AZTEC_WARRIOR -> 0.2F;
                case EGYPTIAN_ARCHER -> 0.3F;
                case GUANDAO_WARRIOR -> 0.4F;
                case JAPANESE_SAMURAI -> 0.5F;
            };
            check(SummonerRelicIconProperty1201.lastGuiIcon() == expected, "Shift GUI relic icon");
            var model = client.getItemRenderer().getModel(stack, client.level, client.player, 0);
            check(model == client.getItemRenderer().getItemModelShaper().getItemModel(stack), "Shift must not change held/world model");
            TooltipShiftState1201.setClientShiftDownSupplier(() -> false);
            graphics.renderItem(stack, -100, -100);
            graphics.flush();
            check(SummonerRelicIconProperty1201.lastGuiIcon() == 0, "release Shift restores icon");
            check(java.util.Objects.equals(savedTag, stack.getTag()), "icon rendering is read-only");
        } finally {
            TooltipShiftState1201.setClientShiftDownSupplier(net.minecraft.client.gui.screens.Screen::hasShiftDown);
        }
    }
    private static void advance(int next) { phase = next; deadline = System.nanoTime() + 25_000_000_000L; }
    private static void check(boolean success, String name) { if (!success) throw new IllegalStateException("Hero UI: " + name); }
    private static void finish(Minecraft client, String failure) {
        client.player.closeContainer(); client.setScreen(new PauseScreen(true)); client.mouseHandler.releaseMouse();
        var server = client.getSingleplayerServer();
        task = setup.thenCompose(current -> server.submit(() -> {
            try { if (failure != null) throw new IllegalStateException(failure); }
            finally { current.restore(); }
        }));
        phase = 99;
    }
}
