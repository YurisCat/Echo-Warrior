package com.yuriscat.echowarrior.compat.item;

import com.yuriscat.echowarrior.compat.binding.EchoBindingSystem1201;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import com.yuriscat.echowarrior.compat.menu.SummonerMenu1201;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.item.TooltipFlag;
import java.util.List;

public final class EchoSummonerItem1201 extends Item {
    private static final int CONTROL_HINT_COLOR = 0x82999B;
    @Override
    public Component getName(ItemStack stack) {
        ItemStack relic = SummonerData1201.contents(stack).get(7);
        return relic.getItem() instanceof EchoRelicItem1201 relicItem
                ? Component.translatable("item.echo_warrior.test_echo_summoner.bound",
                    Component.translatable(relicItem.heroType().translationKey()))
                : super.getName(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level context, List<Component> tooltip, TooltipFlag flag) {
        Component relic = highlightedTerm("item.echo_warrior.test_echo_summoner.tooltip.term.relic");
        Component echo = plainTerm("item.echo_warrior.test_echo_summoner.tooltip.term.echo");
        tooltip.add(Component.translatable(
                "item.echo_warrior.test_echo_summoner.tooltip.summary", relic, echo
        ).withStyle(ChatFormatting.GRAY));

        if (!TooltipShiftState1201.isShiftDown()) {
            tooltip.add(Component.translatable("item.echo_warrior.test_echo_summoner.tooltip.more_hint")
                    .withStyle(ChatFormatting.DARK_GRAY));
            return;
        }

        Component fuel = plainTerm("item.echo_warrior.test_echo_summoner.tooltip.term.fuel");
        Component quickAction = Component.translatable(
                "item.echo_warrior.test_echo_summoner.tooltip.term.quick_action"
        ).withStyle(style -> style.withColor(CONTROL_HINT_COLOR));
        Component inventory = plainTerm("item.echo_warrior.test_echo_summoner.tooltip.term.inventory");
        ItemStack loadedRelic = SummonerData1201.contents(stack).get(7);
        if (loadedRelic.getItem() instanceof EchoRelicItem1201 relicItem) {
            Component hero = Component.translatable(relicItem.heroType().translationKey())
                    .withStyle(ChatFormatting.GOLD);
            tooltip.add(detailLine("item.echo_warrior.test_echo_summoner.tooltip.detail.current_echo", hero));
        } else {
            tooltip.add(detailLine("item.echo_warrior.test_echo_summoner.tooltip.detail.current_echo.empty"));
        }
        tooltip.add(detailLine("item.echo_warrior.test_echo_summoner.tooltip.detail.healing", echo, fuel));
        tooltip.add(detailLine("item.echo_warrior.test_echo_summoner.tooltip.detail.quick_action", quickAction));
        tooltip.add(detailLine("item.echo_warrior.test_echo_summoner.tooltip.detail.direct_insert", inventory));
    }

    private static Component detailLine(String translationKey, Component... arguments) {
        return Component.literal("+").withStyle(ChatFormatting.GRAY)
                .append(Component.translatable(translationKey, (Object[])arguments)
                        .withStyle(ChatFormatting.GRAY));
    }

    private static Component highlightedTerm(String translationKey) {
        return Component.translatable(translationKey)
                .withStyle(style -> style.withColor(KnowledgeTooltip1201.KNOWLEDGE_COLOR));
    }

    private static Component plainTerm(String translationKey) {
        return Component.translatable(translationKey).withStyle(ChatFormatting.GRAY);
    }
    public static boolean isSamePhysicalSummoner(ItemStack first, ItemStack second) {
        if (!(first.getItem() instanceof EchoSummonerItem1201) || first.getItem() != second.getItem()) return false;
        var id = SummonerData1201.summonerId(first);
        return id != null && id.equals(SummonerData1201.summonerId(second));
    }

    @Override public void onDestroyed(net.minecraft.world.entity.item.ItemEntity entity) {
        if (entity.level() instanceof ServerLevel level) {
            var id = SummonerData1201.summonerId(entity.getItem());
            if (id != null) EchoBindingSystem1201.destroySummoner(level, id);
        }
    }
    public static java.util.List<ItemStack> accessoryStacks(ItemStack summoner) {
        return SummonerData1201.contents(summoner).subList(0, 6);
    }
    public EchoSummonerItem1201() { super(new Properties().stacksTo(1)); }

    @Override public boolean overrideOtherStackedOnMe(ItemStack self, ItemStack other, net.minecraft.world.inventory.Slot slot,
            net.minecraft.world.inventory.ClickAction action, Player player, net.minecraft.world.entity.SlotAccess cursor) {
        if (action != net.minecraft.world.inventory.ClickAction.PRIMARY || other.isEmpty()) return false;
        if (player instanceof ServerPlayer serverPlayer && slot.allowModification(player)) {
            ItemStack feedback = other.copyWithCount(1);
            int inserted = com.yuriscat.echowarrior.compat.menu.SummonerTransfer1201.commit(serverPlayer, self, other, -1);
            if (inserted > 0) {
                cursor.set(other.isEmpty() ? ItemStack.EMPTY : other.copy());
                slot.setChanged();
                com.yuriscat.echowarrior.compat.network.InventoryNetwork1201.feedback(serverPlayer,
                        new com.yuriscat.echowarrior.compat.network.InsertionFeedback1201(player.containerMenu.containerId, slot.index, feedback));
            }
        }
        // Vanilla owns synchronization. No client-side source shrinking or NBT write-back.
        return true;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level instanceof ServerLevel serverLevel && entity instanceof ServerPlayer
                && (SummonerData1201.summonerId(stack) == null || entity.tickCount % 5 == 0)) {
            var binding = EchoBindingSystem1201.synchronize(serverLevel, stack);
            var fuelBefore = binding.contents().get(6).copy();
            if (entity.tickCount % 5 == 0 && binding.convertOneFuel(serverLevel.getGameTime())) {
                binding.mirrorTo(stack);
                if (((ServerPlayer)entity).containerMenu instanceof SummonerMenu1201 menu && menu.matches(stack)) {
                    menu.reportFuelConsumed(fuelBefore);
                }
            }
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer serverPlayer) {
            var binding = EchoBindingSystem1201.synchronize(serverPlayer.serverLevel(), stack);
            if (player.isShiftKeyDown()) {
                if (binding.active()) {
                    EchoBindingSystem1201.recallOrReconstruct(serverPlayer.serverLevel(), player, stack, binding);
                } else {
                    var attempt = EchoBindingSystem1201.summonNew(serverPlayer.serverLevel(), player, stack);
                    if (!attempt.succeeded()) player.displayClientMessage(Component.translatable(switch (attempt.failure()) {
                        case NO_RELIC -> "gui.echo_warrior.summoner.feedback.no_relic";
                        case NOT_ENOUGH_FUEL -> "gui.echo_warrior.summoner.feedback.not_enough_fuel";
                        case NO_SAFE_POSITION -> "gui.echo_warrior.summoner.feedback.no_safe_position";
                        case LIMIT_REACHED -> "gui.echo_warrior.summoner.feedback.limit_reached";
                        default -> "gui.echo_warrior.summoner.feedback.create_failed";
                    }), true);
                }
                return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
            }
            int slot = hand == InteractionHand.MAIN_HAND ? player.getInventory().selected : 40;
            player.openMenu(new SimpleMenuProvider((id, inventory, owner) ->
                    new SummonerMenu1201(id, inventory, slot),
                    Component.translatable("item.echo_warrior.test_echo_summoner")));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
