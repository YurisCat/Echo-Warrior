package com.yuriscat.echowarrior.compat.item;

import com.yuriscat.echowarrior.compat.EchoWarrior1211;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

/** Dedicated-server-safe pending queue for client-only inventory feedback. */
public final class SummonerFuelInsertFeedback1211 {
    private static final int MAX_PENDING_FEEDBACK = 32;
    private static final long MAX_PENDING_NANOS = 2_000_000_000L;
    private static final int MAX_DIAGNOSTIC_LINES = 48;
    private static final Queue<PendingFeedback> PENDING = new ConcurrentLinkedQueue<>();
    private static final AtomicInteger PENDING_COUNT = new AtomicInteger();
    private static final AtomicInteger DIAGNOSTIC_LINES = new AtomicInteger();

    private SummonerFuelInsertFeedback1211() {
    }

    public static void playFuel(Slot slot, ItemStack fuel) {
        enqueue(slot, new Feedback(Effect.FUEL, fuel.copyWithCount(1)));
    }

    public static void playPolish(Slot slot) {
        enqueue(slot, new Feedback(Effect.POLISH, ItemStack.EMPTY));
    }

    public static int drain(BiConsumer<Slot, Feedback> consumer) {
        Objects.requireNonNull(consumer);
        int delivered = 0;
        long now = System.nanoTime();
        PendingFeedback pending;
        while ((pending = PENDING.poll()) != null) {
            PENDING_COUNT.updateAndGet(count -> Math.max(0, count - 1));
            if (now - pending.createdAtNanos() > MAX_PENDING_NANOS) {
                diagnostic("expired", pending.slot(), pending.feedback());
                continue;
            }
            diagnostic("drain", pending.slot(), pending.feedback());
            consumer.accept(pending.slot(), pending.feedback());
            delivered++;
        }
        return delivered;
    }

    private static void enqueue(Slot slot, Feedback feedback) {
        Objects.requireNonNull(slot);
        while (PENDING_COUNT.get() >= MAX_PENDING_FEEDBACK) {
            PendingFeedback discarded = PENDING.poll();
            if (discarded == null) break;
            PENDING_COUNT.updateAndGet(count -> Math.max(0, count - 1));
            diagnostic("overflow", discarded.slot(), discarded.feedback());
        }
        PENDING.add(new PendingFeedback(slot, feedback, System.nanoTime()));
        PENDING_COUNT.incrementAndGet();
        diagnostic("enqueue", slot, feedback);
    }

    private static void diagnostic(String stage, Slot slot, Feedback feedback) {
        if (!Boolean.getBoolean("echo_warrior.summoner_insertion_diagnostics")
                || DIAGNOSTIC_LINES.getAndIncrement() >= MAX_DIAGNOSTIC_LINES) return;
        EchoWarrior1211.LOGGER.info(
                "[SummonerInsertionFeedback] stage={} effect={} slotIndex={} slotX={} slotY={} pending={}",
                stage, feedback.effect(), slot.index, slot.x, slot.y, PENDING_COUNT.get());
    }

    public enum Effect {
        FUEL,
        POLISH
    }

    public record Feedback(Effect effect, ItemStack item) {
    }

    private record PendingFeedback(Slot slot, Feedback feedback, long createdAtNanos) {
    }
}
