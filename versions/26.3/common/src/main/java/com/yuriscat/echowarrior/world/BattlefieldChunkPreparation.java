package com.yuriscat.echowarrior.world;

import java.util.ArrayList;
import java.util.List;

/**
 * Server-thread-only, bounded lease on a candidate's 3 by 3 chunk footprint.
 * The backend must never wait for generation. Minecraft owns the generation work.
 */
final class BattlefieldChunkPreparation implements AutoCloseable {
    static final int REQUEST_INTERVAL = 10;
    static final int TIMEOUT = 600;
    enum State { WAITING, READY, EXPIRED }

    interface Backend {
        void retain(long chunk);
        boolean ready(long chunk);
        void release(long chunk);
    }

    private final Backend backend;
    private final List<Long> targets;
    private final List<Long> retained = new ArrayList<>();
    private final long startedAt;
    private long nextRequestAt;
    private boolean closed;

    BattlefieldChunkPreparation(Backend backend, List<Long> targets, long now) {
        if (targets.isEmpty() || targets.size() > 9 || targets.stream().distinct().count() != targets.size()) {
            throw new IllegalArgumentException("A generation attempt needs 1..9 distinct chunks");
        }
        this.backend = backend;
        this.targets = List.copyOf(targets);
        this.startedAt = now;
        this.nextRequestAt = now;
    }

    State tick(long now) {
        if (closed) return State.EXPIRED;
        if (now - startedAt >= TIMEOUT || now < startedAt) {
            close();
            return State.EXPIRED;
        }
        if (retained.size() < targets.size() && now >= nextRequestAt) {
            long chunk = targets.get(retained.size());
            // Record ownership first: even a backend exception must be cleaned up by close().
            retained.add(chunk);
            backend.retain(chunk);
            nextRequestAt = now + REQUEST_INTERVAL;
        }
        if (retained.size() != targets.size()) return State.WAITING;
        for (long chunk : retained) if (!backend.ready(chunk)) return State.WAITING;
        return State.READY;
    }

    boolean owns(long chunk) {
        return !closed && retained.contains(chunk);
    }

    @Override
    public void close() {
        if (closed) return;
        closed = true;
        for (long chunk : retained) backend.release(chunk);
        retained.clear();
    }
}
