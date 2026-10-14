package com.yuriscat.echowarrior.compat.world;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Deterministic delayed-generation, pacing, timeout and cleanup tests; no world mutation. */
public final class BattlefieldChunkPreparationSelfTest1201 {
    private BattlefieldChunkPreparationSelfTest1201() {}

    public static void main(String[] args) {
        run();
        System.out.println("BATTLEFIELD CHUNK PREPARATION SELFTEST PASSED");
    }

    public static void run() {
        List<Long> footprint = List.of(0L, 1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L);
        FakeChunks backend = new FakeChunks();
        backend.tickets.add(999L); // Another system's ticket must survive our cleanup.
        var preparation = new BattlefieldChunkPreparation1201(backend, footprint, 100);
        for (int time = 100; time < 300; time++) {
            int before = backend.requests.size();
            require(preparation.tick(time) == BattlefieldChunkPreparation1201.State.WAITING, "missing chunks never reported ready");
            int added = backend.requests.size() - before;
            require(added <= 1 && (added == 0 || (time - 100) % 10 == 0), "one request per ten ticks");
            require(backend.requests.size() <= 9 && backend.tickets.size() <= 10, "bounded footprint including foreign ticket");
            preparation.tick(time); // Reentrant/same-time polling cannot double the budget.
            require(backend.requests.size() == before + added, "same tick cannot request twice");
        }
        require(preparation.owns(0) && !preparation.owns(999), "ownership excludes foreign tickets");
        backend.loaded.addAll(footprint.subList(0, 8));
        require(preparation.tick(300) == BattlefieldChunkPreparation1201.State.WAITING, "eight ready chunks are insufficient");
        backend.loaded.add(8L);
        require(preparation.tick(301) == BattlefieldChunkPreparation1201.State.READY, "delayed generation eventually advances");
        backend.loaded.remove(4L);
        require(preparation.tick(302) == BattlefieldChunkPreparation1201.State.WAITING, "readiness is rechecked before world reads");
        backend.loaded.add(4L);
        require(preparation.tick(303) == BattlefieldChunkPreparation1201.State.READY, "recover without duplicate requests");
        preparation.close();
        preparation.close();
        require(backend.tickets.equals(Set.of(999L)) && backend.releases.size() == 9, "success releases exactly owned tickets once");
        require(!preparation.owns(0) && preparation.tick(304) == BattlefieldChunkPreparation1201.State.EXPIRED, "closed lease never restarts");

        backend = new FakeChunks();
        preparation = new BattlefieldChunkPreparation1201(backend, footprint, 0);
        for (int time = 0; time < 600; time++) preparation.tick(time);
        require(preparation.tick(600) == BattlefieldChunkPreparation1201.State.EXPIRED, "stalled generation times out");
        require(backend.tickets.isEmpty() && backend.releases.size() == 9, "timeout releases all requested chunks");
        backend.loaded.addAll(footprint);
        require(preparation.tick(601) == BattlefieldChunkPreparation1201.State.EXPIRED, "late completion cannot resurrect canceled work");

        backend = new FakeChunks();
        preparation = new BattlefieldChunkPreparation1201(backend, footprint, 0);
        preparation.tick(0);
        preparation.tick(10);
        preparation.close();
        require(backend.requests.size() == 2 && backend.releases.size() == 2 && backend.tickets.isEmpty(),
                "shutdown midway releases only the requested subset");
        var resumed = new BattlefieldChunkPreparation1201(backend, footprint, 0);
        require(resumed.tick(0) == BattlefieldChunkPreparation1201.State.WAITING, "new world starts independently");
        resumed.close();

        FakeChunks failing = new FakeChunks();
        failing.failRetain = true;
        try (var failed = new BattlefieldChunkPreparation1201(failing, footprint, 0)) {
            try {
                failed.tick(0);
                throw new AssertionError("Expected backend failure");
            } catch (IllegalStateException expected) {
                require(expected.getMessage().equals("injected failure"), "expected injected failure");
            }
        }
        require(failing.tickets.isEmpty() && failing.releases.size() == 1, "partially retained chunk released after failure");

        try {
            new BattlefieldChunkPreparation1201(new FakeChunks(), List.of(1L, 1L), 0);
            throw new AssertionError("Duplicate footprint accepted");
        } catch (IllegalArgumentException expected) {
            // Invalid footprints cannot create any tickets.
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException("Battlefield preparation: " + message);
    }

    private static final class FakeChunks implements BattlefieldChunkPreparation1201.Backend {
        final Set<Long> loaded = new HashSet<>();
        final Set<Long> tickets = new HashSet<>();
        final List<Long> requests = new ArrayList<>();
        final List<Long> releases = new ArrayList<>();
        boolean failRetain;
        public void retain(long chunk) {
            require(tickets.add(chunk), "duplicate retain");
            requests.add(chunk);
            if (failRetain) throw new IllegalStateException("injected failure");
        }
        public boolean ready(long chunk) { return loaded.contains(chunk); }
        public void release(long chunk) {
            require(tickets.remove(chunk), "release without ownership");
            releases.add(chunk);
        }
    }
}
