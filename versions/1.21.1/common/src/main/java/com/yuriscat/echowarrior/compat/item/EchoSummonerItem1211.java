package com.yuriscat.echowarrior.compat.item;

import com.yuriscat.echowarrior.compat.ModContent1211;
import com.yuriscat.echowarrior.compat.binding.EchoBindingSavedData1211;
import com.yuriscat.echowarrior.compat.binding.EchoBindingSystem1211;
import com.yuriscat.echowarrior.compat.entity.EchoWarriorEntity1211;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import com.yuriscat.echowarrior.compat.menu.SummonerMenu1211;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class EchoSummonerItem1211 extends Item {
    private static final String SUMMONER_ID_KEY = "EchoWarriorSummonerId";
    private static final String SPIRIT_ID_KEY = "EchoWarriorSpiritId";
    private static final int CONTROL_HINT_COLOR = 0x82999B;
    private static final int CONTAINER_SIZE = 8;
    private static final int ACCESSORY_SLOT_COUNT = 6;
    private static final int FUEL_SLOT = 6;
    private static final int RELIC_SLOT = 7;

    public EchoSummonerItem1211(Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        ItemStack relic = relicStack(stack);
        return relic.getItem() instanceof EchoRelicItem1211 relicItem
                ? Component.translatable("item.echo_warrior.test_echo_summoner.bound",
                    Component.translatable(relicItem.heroType().translationKey()))
                : super.getName(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        Component relic = highlightedTerm("item.echo_warrior.test_echo_summoner.tooltip.term.relic");
        Component echo = plainTerm("item.echo_warrior.test_echo_summoner.tooltip.term.echo");
        tooltip.add(Component.translatable(
                "item.echo_warrior.test_echo_summoner.tooltip.summary", relic, echo
        ).withStyle(ChatFormatting.GRAY));

        if (!TooltipShiftState1211.isShiftDown()) {
            tooltip.add(Component.translatable("item.echo_warrior.test_echo_summoner.tooltip.more_hint")
                    .withStyle(ChatFormatting.DARK_GRAY));
            return;
        }

        Component fuel = plainTerm("item.echo_warrior.test_echo_summoner.tooltip.term.fuel");
        Component quickAction = Component.translatable(
                "item.echo_warrior.test_echo_summoner.tooltip.term.quick_action"
        ).withStyle(style -> style.withColor(CONTROL_HINT_COLOR));
        Component inventory = plainTerm("item.echo_warrior.test_echo_summoner.tooltip.term.inventory");
        ItemStack loadedRelic = relicStack(stack);
        if (loadedRelic.getItem() instanceof EchoRelicItem1211 relicItem) {
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
                .withStyle(style -> style.withColor(KnowledgeTooltip1211.KNOWLEDGE_COLOR));
    }

    private static Component plainTerm(String translationKey) {
        return Component.translatable(translationKey).withStyle(ChatFormatting.GRAY);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide) return;
        EchoBindingSavedData1211.Binding binding = EchoBindingSystem1211.synchronize((ServerLevel)level, stack);
        if (entity.tickCount % 5 != 0) return;
        SimpleContainer contents = contents(stack);
        ItemStack fuel = contents.getItem(FUEL_SLOT);
        int value = SummonerFuel1211.value(fuel);
        if (value <= 0 || binding.fuel() + value > SummonerFuel1211.CAPACITY) return;
        fuel.shrink(1);
        EchoBindingSystem1211.addFuel((ServerLevel)level, stack, value);
        saveContents(stack, contents);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);
        if (!player.isShiftKeyDown()) {
            if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResultHolder.fail(stack);
            int sourceSlot = hand == InteractionHand.MAIN_HAND ? player.getInventory().selected : Inventory.SLOT_OFFHAND;
            getOrCreateSummonerId(stack);
            player.openMenu(new SimpleMenuProvider(
                    (containerId, inventory, ignored) -> new SummonerMenu1211(containerId, inventory, sourceSlot, stack),
                    stack.getHoverName()));
            return InteractionResultHolder.success(stack);
        }
        ServerLevel serverLevel = (ServerLevel) level;

        SummonResult result = summonOrRecall(serverLevel, player, stack, true);
        return result.succeeded()
                ? InteractionResultHolder.success(stack)
                : InteractionResultHolder.fail(stack);
    }

    private static SummonResult summonOrRecall(
            ServerLevel serverLevel, Player player, ItemStack stack, boolean showActionBarFeedback) {

        EchoBindingSavedData1211.Binding binding = EchoBindingSystem1211.synchronize(serverLevel, stack);
        if (binding.active()) {
            EchoWarriorEntity1211 current = EchoBindingSystem1211.recallOrReconstruct(
                    serverLevel, player, stack, binding);
            if (current == null) return SummonResult.CREATE_FAILED;
            if (showActionBarFeedback) {
                player.displayClientMessage(Component.translatable(
                        "gui.echo_warrior.summoner.feedback.dismissed"), true);
            }
            return SummonResult.RECALLED;
        }

        ItemStack relic = relicStack(stack);
        if (!(relic.getItem() instanceof EchoRelicItem1211)) {
            if (showActionBarFeedback) {
                player.displayClientMessage(Component.translatable(
                        "gui.echo_warrior.summoner.feedback.no_relic"), true);
            }
            return SummonResult.NO_RELIC;
        }
        if (!(player instanceof ServerPlayer serverPlayer)
                || !EchoBindingSystem1211.canAddControllerEcho(
                        serverLevel.getServer(), player.getUUID(), binding.summonerId())) {
            if (showActionBarFeedback) {
                player.displayClientMessage(Component.translatable(
                        "gui.echo_warrior.summoner.feedback.limit_reached"), true);
            }
            return SummonResult.LIMIT_REACHED;
        }
        int summonCost = EchoRelicState1211.summonCost(relic);
        if (!EchoBindingSystem1211.consumeFuel(serverLevel, stack, summonCost)) {
            if (showActionBarFeedback) {
                player.displayClientMessage(Component.translatable(
                        "gui.echo_warrior.summoner.feedback.not_enough_fuel"), true);
            }
            return SummonResult.NOT_ENOUGH_FUEL;
        }

        EchoBindingSystem1211.SpawnAttempt attempt = EchoBindingSystem1211.summonNew(serverLevel, player, stack);
        if (!attempt.succeeded()) {
            EchoBindingSystem1211.addFuel(serverLevel, stack, summonCost);
            SummonResult result = switch (attempt.failure()) {
                case NO_SAFE_POSITION -> SummonResult.NO_SAFE_POSITION;
                case LIMIT_REACHED -> SummonResult.LIMIT_REACHED;
                default -> SummonResult.CREATE_FAILED;
            };
            if (showActionBarFeedback) {
                player.displayClientMessage(Component.translatable(result.translationKey()), true);
            }
            return result;
        }
        EchoBindingSystem1211.noteNewSummon(serverPlayer);
        if (showActionBarFeedback) {
            player.displayClientMessage(Component.translatable(
                    "gui.echo_warrior.summoner.feedback.summoned"), true);
        }
        return SummonResult.SUMMONED;
    }

    public static SummonResult summonFromMenu(ServerPlayer player, ItemStack stack) {
        return summonOrRecall((ServerLevel)player.level(), player, stack, false);
    }

    public enum SummonResult {
        SUMMONED(null),
        RECALLED(null),
        CREATE_FAILED("gui.echo_warrior.summoner.feedback.create_failed"),
        NO_RELIC("gui.echo_warrior.summoner.feedback.no_relic"),
        NOT_ENOUGH_FUEL("gui.echo_warrior.summoner.feedback.not_enough_fuel"),
        NO_SAFE_POSITION("gui.echo_warrior.summoner.feedback.no_safe_position"),
        LIMIT_REACHED("gui.echo_warrior.summoner.feedback.limit_reached");

        private final String translationKey;

        SummonResult(String translationKey) {
            this.translationKey = translationKey;
        }

        public boolean succeeded() {
            return this == SUMMONED || this == RECALLED;
        }

        public String translationKey() {
            return this.translationKey;
        }
    }

    @Override
    public boolean overrideOtherStackedOnMe(ItemStack self, ItemStack other, Slot slot, ClickAction action,
                                             Player player, SlotAccess carriedItem) {
        if (action != ClickAction.PRIMARY || other.isEmpty()) return false;
        boolean fuel = SummonerFuel1211.isFuel(other);
        boolean accessory = EchoSummonerAccessory1211.isAccessory(other);
        boolean relic = other.getItem() instanceof EchoRelicItem1211;
        if (!fuel && !accessory && !relic) {
            player.playSound(SoundEvents.DISPENSER_FAIL, 0.7F, 1.0F);
            return true;
        }
        ItemStack fuelForFeedback = fuel ? other.copyWithCount(1) : ItemStack.EMPTY;
        SummonerMenu1211 openMenu = player.containerMenu instanceof SummonerMenu1211 menu
                && menu.allowsDirectInsertionIntoSource(slot, player) ? menu : null;
        if (player.level().isClientSide) {
            boolean predicted = openMenu == null
                    ? canInsertIntoInternalSlot(self, other)
                    : openMenu.insertIntoOpenSummoner(other);
            if (!predicted) {
                player.playSound(SoundEvents.DISPENSER_FAIL, 0.7F, 1.0F);
                return true;
            }
            player.playSound(SoundEvents.BUNDLE_INSERT, 0.8F, 1.0F);
            if (fuel) SummonerFuelInsertFeedback1211.playFuel(slot, fuelForFeedback);
            else SummonerFuelInsertFeedback1211.playPolish(slot);
            return true;
        }
        if (!slot.allowModification(player) && openMenu == null) {
            player.playSound(SoundEvents.DISPENSER_FAIL, 0.7F, 1.0F);
            return true;
        }
        getOrCreateSummonerId(self);
        if (openMenu == null && player.level() instanceof ServerLevel level) {
            EchoBindingSystem1211.synchronize(level, self);
        }
        int carriedBefore = other.getCount();
        boolean inserted = openMenu == null
                ? insertIntoInternalSlot(self, other)
                : openMenu.insertIntoOpenSummoner(other);
        if (!inserted) {
            player.playSound(SoundEvents.DISPENSER_FAIL, 0.7F, 1.0F);
            return true;
        }
        int moved = carriedBefore - other.getCount();
        if (moved <= 0 || relic && (openMenu == null
                ? relicStack(self).isEmpty()
                : openMenu.directInsertionRelic().isEmpty())) {
            if (moved > 0) other.grow(moved);
            carriedItem.set(other.copy());
            player.playSound(SoundEvents.DISPENSER_FAIL, 0.7F, 1.0F);
            return true;
        }
        if (openMenu == null && player.level() instanceof ServerLevel level) {
            if (relic) EchoBindingSystem1211.updateRelic(level, self);
            if (accessory) EchoBindingSystem1211.updateAccessories(level, self);
        }
        carriedItem.set(other.isEmpty() ? ItemStack.EMPTY : other.copy());
        player.playSound(SoundEvents.BUNDLE_INSERT, 0.8F, 1.0F);
        if (player.containerMenu != null) player.containerMenu.broadcastChanges();
        return true;
    }

    private static boolean canInsertIntoInternalSlot(ItemStack self, ItemStack other) {
        SimpleContainer contents = contents(self);
        if (EchoSummonerAccessory1211.isAccessory(other)) {
            for (int accessorySlot = 0; accessorySlot < ACCESSORY_SLOT_COUNT; accessorySlot++) {
                if (contents.getItem(accessorySlot).isEmpty()
                        && EchoSummonerAccessory1211.canInstall(other, self, accessorySlot, contents)) return true;
            }
            return false;
        }
        if (SummonerFuel1211.isFuel(other)) {
            ItemStack stored = contents.getItem(FUEL_SLOT);
            return stored.isEmpty() || ItemStack.isSameItemSameComponents(stored, other)
                    && stored.getCount() < stored.getMaxStackSize();
        }
        return other.getItem() instanceof EchoRelicItem1211 && contents.getItem(RELIC_SLOT).isEmpty();
    }

    private static boolean insertIntoInternalSlot(ItemStack self, ItemStack other) {
        SimpleContainer contents = contents(self);
        int inserted = 0;
        if (EchoSummonerAccessory1211.isAccessory(other)) {
            for (int accessorySlot = 0; accessorySlot < ACCESSORY_SLOT_COUNT; accessorySlot++) {
                if (!contents.getItem(accessorySlot).isEmpty()
                        || !EchoSummonerAccessory1211.canInstall(other, self, accessorySlot, contents)) continue;
                inserted = 1;
                contents.setItem(accessorySlot, other.copyWithCount(1));
                break;
            }
        } else if (SummonerFuel1211.isFuel(other)) {
            ItemStack stored = contents.getItem(FUEL_SLOT);
            if (stored.isEmpty()) {
                inserted = Math.min(other.getCount(), other.getMaxStackSize());
                contents.setItem(FUEL_SLOT, other.copyWithCount(inserted));
            } else if (ItemStack.isSameItemSameComponents(stored, other)) {
                inserted = Math.min(other.getCount(), stored.getMaxStackSize() - stored.getCount());
                if (inserted > 0) stored.grow(inserted);
            }
        } else if (other.getItem() instanceof EchoRelicItem1211 && contents.getItem(RELIC_SLOT).isEmpty()) {
            inserted = 1;
            contents.setItem(RELIC_SLOT, other.copyWithCount(1));
        }
        if (inserted <= 0) return false;
        other.shrink(inserted);
        saveContents(self, contents);
        return true;
    }

    public static boolean insertIntoInternalSlotForSelfTest(ItemStack summoner, ItemStack carried) {
        return insertIntoInternalSlot(summoner, carried);
    }

    public static boolean isDirectInsertionCandidate(ItemStack carried) {
        return SummonerFuel1211.isFuel(carried)
                || EchoSummonerAccessory1211.isAccessory(carried)
                || carried.getItem() instanceof EchoRelicItem1211;
    }

    public static boolean insertIntoInternalSlotForCreative(ItemStack summoner, ItemStack carried) {
        return insertIntoInternalSlot(summoner, carried);
    }

    public static ItemStack relicStack(ItemStack stack) {
        return contents(stack).getItem(RELIC_SLOT);
    }

    public static void setRelicStack(ItemStack stack, ItemStack relic) {
        SimpleContainer contents = contents(stack);
        contents.setItem(RELIC_SLOT, relic.isEmpty() ? ItemStack.EMPTY : relic.copyWithCount(1));
        saveContents(stack, contents);
    }

    public static List<ItemStack> accessoryStacks(ItemStack stack) {
        SimpleContainer contents = contents(stack);
        return contents.getItems().subList(0, ACCESSORY_SLOT_COUNT).stream().map(ItemStack::copy).toList();
    }

    public static void setAccessoryStacks(ItemStack stack, List<ItemStack> accessories) {
        SimpleContainer contents = contents(stack);
        for (int slot = 0; slot < ACCESSORY_SLOT_COUNT; slot++) {
            ItemStack accessory = slot < accessories.size() ? accessories.get(slot) : ItemStack.EMPTY;
            contents.setItem(slot, accessory.isEmpty() ? ItemStack.EMPTY : accessory.copyWithCount(1));
        }
        saveContents(stack, contents);
    }

    public static UUID getOrCreateSummonerId(ItemStack stack) {
        Optional<UUID> current = readUuid(stack, SUMMONER_ID_KEY);
        if (current.isPresent()) return current.get();
        UUID created = UUID.randomUUID();
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putUUID(SUMMONER_ID_KEY, created));
        return created;
    }

    public static UUID getSummonerId(ItemStack stack) {
        return readUuid(stack, SUMMONER_ID_KEY).orElse(null);
    }

    /** True when two client stacks are synchronized copies of the same physical summoner. */
    public static boolean isSamePhysicalSummoner(ItemStack first, ItemStack second) {
        if (!(first.getItem() instanceof EchoSummonerItem1211)
                || !(second.getItem() instanceof EchoSummonerItem1211)) return false;
        UUID firstId = getSummonerId(first);
        return firstId != null && firstId.equals(getSummonerId(second));
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onDestroyed(ItemEntity itemEntity) {
        if (itemEntity.level() instanceof ServerLevel level) {
            EchoBindingSystem1211.destroySummoner(level, itemEntity.getItem());
        }
    }

    public static UUID replaceSummonerIdForDuplicate(ItemStack stack) {
        UUID replacement = UUID.randomUUID();
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putUUID(SUMMONER_ID_KEY, replacement);
            tag.remove(SPIRIT_ID_KEY);
        });
        return replacement;
    }

    private static Optional<UUID> readUuid(ItemStack stack, String key) {
        var tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return tag.hasUUID(key) ? Optional.of(tag.getUUID(key)) : Optional.empty();
    }

    public static void setSpiritId(ItemStack stack, UUID spiritId) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            if (spiritId == null) tag.remove(SPIRIT_ID_KEY);
            else tag.putUUID(SPIRIT_ID_KEY, spiritId);
        });
    }

    private static SimpleContainer contents(ItemStack stack) {
        SimpleContainer contents = new SimpleContainer(CONTAINER_SIZE);
        stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(contents.getItems());
        return contents;
    }

    private static void saveContents(ItemStack stack, SimpleContainer contents) {
        stack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(contents.getItems()));
    }
}
