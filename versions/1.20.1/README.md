# Minecraft 1.20.1 compatibility line

## 2026-10-07 progression and optional TBF integration

Default maximum level is now 100, configurable at startup with `config/echo_warrior-progression.json` (`schemaVersion: 1`, `maxLevel: 100`). Health and base attack keep the fixed `1 + (L - 1) / 29` multiplier; next-level XP remains `15 + 2L`. Lowering the cap preserves cultivated levels and existing XP. The salvage value remains capped at level 30.

FOXY job `20261007T130957Z-66296` passed both ordinary packaged server loaders and the requested official TBF 0.2.3 integration (Forge). The integration keeps the existing binding/UUID/generation transaction intact, supports chest-stored summoners, and passes 100 actual TBF handler cycles with stable list/team identity. Client wheel interaction, previews and real multiplayer remain manual acceptance. Full scope, exact package hashes and collected evidence: [`../../docs/research/tbf-compat-and-growth-2026-10-07.md`](../../docs/research/tbf-compat-and-growth-2026-10-07.md). These are unsubmitted working builds; translation release checks remain pending.


Approved targets: Fabric + Forge, Java 17 for the game and compiled classes.
The repository Gradle Wrapper runs on Java 21; do not downgrade the mainline Java 25 toolchain.

Current stage: **0.2.3 server-performance release**. The exact CI JARs passed four-loader production-server regression on FOXY-NODE. GitHub Release and all six CurseForge files are public; all six Modrinth versions are verified while that project remains under review. Final hashes and platform results are recorded separately.
All five heroes, their shared combat/talent/accessory machinery, original models/animations and the complete summoner GUI
are connected. 43 items, three blocks, one custom block-entity type, three creative tabs and four menus are registered.
All 14 reviewed locales, 31 converted recipes, archaeology/knowledge/recycler loot, compass models/HUD/colors and biome-tinted
brushable blocks are included. The five heroes, books and recycler have passed actual production-server checks.
Exploration placement/safety, actual talent mining/trading, dynamic relic overlays and production-JAR clients are implemented.
The current release evidence is in `../../docs/releases/0.2.3.md`; `0.2.2.md` and the checkpoints below apply only to their recorded JAR hashes.
Manual acceptance is tracked in `docs/COMPATIBILITY_1.20.1_TEST_CHECKLIST.md`, not inferred from automated success.
The author approved public version `0.2.3`; historical `0.2.2-dev.1` checkpoints below were internal test identifiers for the earlier release.

## Local battlefield performance fix — 2026-10-05

The same confirmed porting omission as 1.21.1 is fixed here: locked compass targets use one region
lookup, 2048-block searches at most 49, and actual block-removal callbacks replace the global
once-per-second archaeology poll. Chunk-load repair checks only registered positions in that
chunk, including cross-region sites. Negative coordinates, ordinary/remnant removal, same-block
brushing stages and replacement with another brushable are covered. Existing save data, compass
range, sound timing and generation/cooldown rules are preserved; restart to apply the Mixin,
with no new world/chunks required. No wording or encyclopedia update is needed. PORT-030 records
the reusable lesson; this is an unpublished local rebuild, with no version-number change.

Both loader builds, artifact/source baseline and content parity passed. All **41** corrupted-JAR
rejection tests passed, including missing removal-hook registration, production mapping and
performance self-test class. Both **production** dedicated servers passed two boots, the actual
removal Mixin and `BattlefieldPerformanceSelfTest1201`, and graceful shutdown. The performance
test uses 10,000 historical regions and counts operations instead of asserting timing on this PC.
Full server report (exact staged JAR hashes):
`../../build/compatibility-1.20.1-smoke/20261005T071053Z-53532/report.json`.
No graphical client or original reporter's pack was tested in this task; pack-specific Spark/tick
improvement and multiplayer presentation remain on the manual checklist.

| Local 0.2.2 JAR (not the published file) | SHA-256 |
| --- | --- |
| Fabric | `c2b288e8594e2da14fd01eb1734c58b9df8b0f1c623fda92cabca938b79d5cd2` |
| Forge | `340d731b9a2e65b230180f7db482ad204bcf659abee9901fcca4aa818f00443e` |

## Historical inventory/HUD/snow checkpoint — 2026-09-20

- Direct inventory insertion now plays one local `BUNDLE_INSERT` sound after accepted server feedback,
  for fuel, relics and accessories. The actual SoundManager listener verifies three resolved sounds
  in each of creative catalogue, creative inventory and survival inventory; tests keep master volume muted.
- The knowledge creative tab starts with the complete 40-page collection, followed by 40 individual
  fragments. `BooksSelfTest1201` builds the registered tab and checks order, page counts and bookmark.
- ForgeGui bypassed the vanilla render Mixin. HUD rendering now uses Forge `RenderGuiEvent.Post`
  or Fabric `HudRenderCallback`, with no duplicate common render hook. The six messages and orange pulse
  are tested through actual loader frame callbacks, not by directly invoking the HUD renderer.
- Snow layers are preserved during battlefield generation, including all thicknesses 1–8. The same
  terrain rule is applied to 26.1.2 and 1.21.1; full snow blocks are not excavation floors. Old removed snow
  is not reconstructed. Use newly generated sites/new chunks for this visual check; no world reset needed.
- Both production clients passed the full suite and shut down, releasing all owned processes:
  - Forge: `build/compatibility-1.20.1-production-client/20260920T110538Z-59692/forge/report.json`
  - Fabric: `build/compatibility-1.20.1-production-client/20260920T110858Z-36316/fabric/report.json`
- Both production dedicated servers passed both boots, including real snowy site placement and
  neighbor updates: `build/compatibility-1.20.1-smoke/20260920T110752Z-64572/report.json`.
- `check-1.20.1-runtime-evidence.py` confirms current JARs match all four server boots and both clients.
  Baselines, 38 corrupt-artifact rejection tests, 6 production-launcher tests and 135-module coverage passed.
  `check-battlefield-snow.py` guards identical terrain helpers and their call sites in all three versions.
  The other two version lines passed dual-loader builds; their snow behavior was not separately run in game.
- No player-facing language keys or wording changed. The encyclopedia and PROJECT now explicitly document
  snow preservation; the other fixes restore existing behavior. PORT-002/007/014/029 record the reusable lessons.
  No commit, upload or public version-number change. Manual sound, text position and snowy-site visuals remain pending.

| Current artifact | SHA-256 |
| --- | --- |
| `fabric/build/libs/echo-warrior-fabric-1.20.1-0.2.2-dev.1.jar` | `7e3d745ea0ee069d6a1cb9fa34fab85dda7be6c1a21efbe6c60c8f0d877558ce` |
| `forge/build/libs/echo-warrior-forge-1.20.1-0.2.2-dev.1.jar` | `e2ec71103fb73d486910fb76d25e7a3d086de3d77601a4c5f82c6474b2dde74e` |

## Historical departure-effect checkpoint — 2026-09-20

- All five heroes emit 24 SOUL particles just before vanilla death removal at tick 20. Death still
  invalidates the binding immediately and retains the falling pose. Existing dismissal effects are preserved.
- The Guandao final combo strike uses `PLAYER_ATTACK_CRIT` instead of `ANVIL_LAND`.
  Earlier strikes, damage, timing and projectile-parry metallic sound are unchanged.
- Four low-version loader builds/baselines passed (1.20.1 Fabric/Forge and 1.21.1 Fabric/NeoForge).
  The 1.21.1 change and its **development-server** evidence are recorded in that version's README.
- Both **production** 1.20.1 dedicated servers passed the complete suite twice, including disk restart:
  `build/compatibility-1.20.1-smoke/20260920T034316Z-34324/report.json`.
  `DepartureEffectsSelfTest1201` captures actual outgoing particle packets for five heroes and four
  removal paths (item dismissal, ID dismissal, summoner destruction, death), checking count/position,
  no premature death disappearance, no repeats and vanilla/silent negative cases. It also captures
  the four Guandao strike sound packets and requires the critical sound only on the last strike.
- 38 corrupt-artifact rejection tests and 135-module source coverage passed. The new death Mixin
  and packet-observer accessor have production mappings checked by the JAR baseline and actual servers.
- No graphical client was launched for this task. Previous complete client evidence below belongs to
  its older hashes, **not** these new artifacts. This checkpoint is not a fresh production-client,
  visual or audio sign-off. The existing desktop shortcut compiles this source; no new world is needed.

| Historical artifact | SHA-256 |
| --- | --- |
| `fabric/build/libs/echo-warrior-fabric-1.20.1-0.2.2-dev.1.jar` | `7319f43e9a1165467d84d6c11f5151db453f7561410fdaa4781b5813552424d7` |
| `forge/build/libs/echo-warrior-forge-1.20.1-0.2.2-dev.1.jar` | `99a814e3f310c28ee84cd1c5207a7ad9a78f48bfcabb8d271543abe6d88dfdab` |

Reusable lifecycle guidance: PORT-028. Manual checks: death, dismissal, creative destruction, nearby
multiplayer observer and finisher sound. No player wording, gameplay balance or encyclopedia rule changed;
no localization/encyclopedia edits or release upload were performed. Test harness investigation found
Forge's public player list is immutable; the scoped packet observer now uses a mapped accessor and is
always removed in `finally`, rather than assuming Fabric's mutable view.

## Build and test commands

```powershell
.\scripts\build-1.20.1.ps1 -Loader Dual
python scripts/check-1.20.1-baseline.py
python scripts/test_compatibility_1201_baseline.py
python scripts/check-1.20.1-content-parity.py
python scripts/test_1201_production_client.py
python scripts/smoke-test-1.20.1-servers.py --loader both
python scripts/prepare-1.20.1-client-worlds.py
.\scripts\run-test-client.ps1 -TargetVersion 1.20.1 -Loader Fabric -StartupOnly -RequireExistingWorld -TimeoutSeconds 300
.\scripts\run-test-client.ps1 -TargetVersion 1.20.1 -Loader Forge -StartupOnly -RequireExistingWorld -TimeoutSeconds 300
.\scripts\run-test-client.ps1 -TargetVersion 1.20.1 -Loader Fabric -Production -StartupOnly -RequireExistingWorld -TimeoutSeconds 300
.\scripts\run-test-client.ps1 -TargetVersion 1.20.1 -Loader Forge -Production -StartupOnly -RequireExistingWorld -TimeoutSeconds 300
.\scripts\test-client-smoke-log.ps1
```

Manual Forge entry (keeps the game open):

```powershell
.\scripts\run-test-client.ps1 -TargetVersion 1.20.1 -Loader Forge -RequireExistingWorld -PauseOnJoin
```

The desktop shortcut is named **Echo Warrior 1.20.1 Forge 测试端（进世界自动暂停）** and points to
`tools/windows/Launch 1.20.1 Forge Test Client.bat`. It compiles the latest local source, enters the isolated CATTEST,
blocks mouse capture until the pause screen opens, and leaves the game paused for the author. Clicking Resume/Esc
returns to normal mouse control. It neither starts the focus-window helper nor enables automatic shutdown/network fixtures.
`scripts/install-1.20.1-forge-shortcut.ps1` installs this local shortcut without overwriting an existing one;
machine-specific `.lnk` files are not committed. Do not confuse `-PauseOnJoin` with `-StartupOnly`, which runs automation and exits.

Run this command from the repository root. It builds `common`, `fabric` and `forge` in this isolated project.
Forge production output must come from `reobfJar` in `forge/build/libs`, not `forge/build/devlibs`.
Existing 26.1.2 and 1.21.1 Java sources, packages and worlds are not inputs to this bootstrap build.
Original item/GUI textures and reviewed Chinese/English item, talent and accessory text are reused from the mainline resources;
the 30 unchanged generated-item models come from the 1.21.1 resource directory. No high-version Java or world data is imported.
Run clients sequentially. The launcher refuses both running clients and pending `runClient` launches from other workspaces.

The initial server Mixin provides an actual production-mapping check. Set
`-Decho_warrior.compat_bootstrap_test=true` in an isolated server to also check vanilla item NBT round trips,
snapshot separation, UUID/revision preservation and SBL/GeckoLib linkage.
The same flag runs the complete server suite, including actual entities, placement, transactions and disk-restart fixtures.
Use it only in disposable automation worlds. Desktop manual testing does not enable these fixtures.

## Compatibility differences and acceptance boundary

- Targets are **Fabric and Forge**, not NeoForge. Java 17 is the game runtime. Install only one loader's mod JAR.
- Required runtime dependencies: Fabric API 0.92.12+1.20.1 (Fabric only), SmartBrainLib 1.15 and GeckoLib 4.8.4 for the corresponding loader.
- Sounds absent in 1.20.1 use existing vanilla attack-sweep/critical-hit equivalents; gust particles use clouds. The Guandao finisher no longer uses anvil landing; projectile parries retain their metallic sound. No newer vanilla audio is redistributed.
- The bleeding effect reuses the existing hand-drawn wound sprite through an explicit atlas alias.
- Item components, attributes, networking, menus and resources have version-specific adapters. Matching gameplay does not mean identical file formats.
- 134 source modules are accounted for by the structural parity audit, including seven explicitly named API replacements. This audit does not prove behavioral parity by itself.
- Worlds are version-specific. Do not open a 26.1.2 or 1.21.1 world in 1.20.1. Back up existing 1.20.1 worlds. Use fresh chunks for natural battlefield distribution tests.
- Test automation covers deterministic operations and real rendering calls. Animation quality, sound, natural terrain appearance, real two-player latency and third-party compatibility still require the consolidated manual checklist.
- Localization structure is complete (14 files, 622 keys each), but the repository's `docs/localization/state.json` currently has an empty accepted baseline. `--release-gate` reports 12 pending locales and blocks publication. No review records or waivers were fabricated; resolve this separately before a public release.
- This port changes no gameplay specification, controls or balance; no encyclopedia article change is required. Higher-version gameplay sources are unchanged.

## Historical checkpoints

The dated sections below preserve investigation history. Statements such as “not yet ported” apply only to that historical checkpoint.

## Initial checkpoint — 2026-09-19 (before the storage/client work)

- Clean dual build with the existing Gradle 9.5.1 Wrapper succeeded. No replacement Wrapper or shared build-logic change was needed.
- All own classes target Java 17 (major 61). Fabric and Forge metadata, entrypoints and classes remain separated.
- Fabric's refmap uses intermediary names; Forge's uses SRG names and its manifest enables the Mixin config.
- Both actual production JARs, with their published SBL/GeckoLib dependencies, passed two boots each on Java 17. Each boot reached world readiness, ran the five bootstrap checks, saved and stopped normally.
- Seven rejection tests confirm that the artifact audit catches missing/empty/wrong-loader refmaps, wrong Java bytecode, mixed loader metadata, missing entrypoints and missing Forge Mixin manifest configuration.
- Existing 1.21.1 dual-loader baseline still passes. No 26.1.2/1.21.1 gameplay source, JAR, existing world or release workflow was changed.
- No game client was opened. This invocation's test servers were shut down; no other development clients/Gradle daemons were stopped.

Local evidence (ignored build output):
`build/compatibility-1.20.1-smoke/20260919T074545Z-41376/report.json`, with both loaders' `console-1.log` and `console-2.log`.
The report records SHA-256 hashes of the exact tested JARs and dependencies.

| Tested artifact | SHA-256 |
| --- | --- |
| Fabric `0.2.2-dev.1` | `6652a02fcee3763ac1e0c2145aa2548d8d7fe9de2e061e3cb485500c9ba1fd69` |
| Forge `0.2.2-dev.1` | `9748fa69a06d02dbdd88c9b4425e0b96c8ceefe204b190702a5cec8ac6522608` |

These are **bootstrap artifacts, not test-team gameplay packages**. The NBT checks exercise vanilla ItemStack serialization in memory;
they do not yet test a real summoner, SavedData binding or a migrated hero. The repeated boots verify server save/restart, not a
high-version world downgrade.

## Storage and client progress — 2026-09-19

`SummonerData1201` replaces item custom-data/container components with old item NBT:

- Read-only access does not create tags. Contents and writes use independent snapshots.
- Stable summoner/spirit UUIDs, fuel clamping, failed-consumption rollback and duplicate-ID replacement.
- All eight slots are committed together; stale revisions and oversized slots are rejected without changing the original.
- Fuel, unrelated root NBT and opaque nested item metadata survive content changes. Unchanged snapshots do not advance the revision.
- `SummonerStackContents1201` covers summoners, old `BlockEntityTag.Items` containers and bundles. A depth/item limit returns `complete=false`; future destruction checks must retain the binding when visibility is uncertain.

The server self-test uses the **real summoner item**, but vanilla marker items stand in for not-yet-ported relics/accessories.
It checks item NBT and vanilla inventory-packet round trips, stale-snapshot rejection, nested visibility, invalid UUID/slot reads,
and a real save/restart through a **test-only SavedData fixture**. This is not the gameplay binding system.
The two boots require exact counters (`compat_storage_expected_boot=1/2`) so a missing save cannot silently look like a fresh successful test.

Development-client Quick Play has reached CATTEST on both loaders and saved/exited normally. The opt-in automation prevents mouse
capture before world entry, opens the pause menu and calls normal client shutdown. Existing user clients are neither reused as test
evidence nor stopped. Fabric initially reached every lifecycle checkpoint but its audit rejected vanilla's offline-dev-account 401;
the classifier now accepts only that exact authlib message/exception pair, with five regression checks covering the allow/deny boundary.
Both final development-client reruns passed the full audit, including the baked summoner model/texture assertion, mouse release and normal save/exit.
Launch attempts correctly refused another workspace's running game; the final reruns took place after it exited. No external client was stopped.

Final storage/server evidence: `build/compatibility-1.20.1-smoke/20260919T081720Z-41800/report.json` (exact tested JAR hashes and per-loader logs).
Both production loaders passed: 28 storage checks on first boot and 29 on restart (the extra disk-reload assertion),
plus the five original bootstrap checks on each boot. All four server boots saved and stopped normally with no ERROR/FATAL entries.
Client lifecycle logs: `versions/1.20.1/run-{fabric,forge}/logs/latest.log` and their `automated-client-smoke-gradle.*.log` files.
The artifact audit has eight rejection cases, including client-only Mixins being incorrectly moved onto the server.

## World-authority foundation — 2026-09-19

`EchoBindingSavedData1201` now owns the eight contents slots, fuel and fractional consumption, state revision,
controller/entity UUIDs, generation and a detached entity migration snapshot. Its file is
`data/echo_warrior_bindings_1201.dat` in the overworld; dimensions share the same authority.
The registered summoner's server-side inventory callback reaches this authority. Initial contents/fuel can be
imported once, but item-provided spirit UUIDs and revision numbers never grant control or override an existing record.
Subsequent synchronization only writes world state back into the item, preserving unrelated item NBT.

- Whole-content CAS rejects stale revisions. Invalid writes and unchanged snapshots do not mark SavedData dirty.
- Every real authority change marks its owning SavedData dirty automatically, including mutations after disk loading.
- Entity tracking requires both UUID and generation. Older generations cannot replace the snapshot or dismiss a replacement.
- Position/health snapshots are persisted without bumping the equipment revision; movement must not make every menu action stale.
- The migration snapshot currently stores dimension, position, health, absorption, fire, freezing, air and opaque retained-state NBT.
  Actual hero-specific snapshot capture/restoration is still pending.
- Visible physical duplicate tracking preserves the first observed inventory/cursor location, rekeys a later copy and clears its
  spirit mirror. A same-slot Java object replacement or ordinary move does not rekey the original.
  The tracker belongs to one world's SavedData, not a static process-wide cache.
- Confirmed record removal invalidates already-held binding handles. This is an authority primitive, **not yet the creative
  destruction workflow**; real click intent, slot-removal evidence and deferred global visibility checks are still required.

`BindingAuthoritySelfTest1201` runs alongside the previous storage tests under the opt-in server flag. It tests the
authority/CAS/mirror boundary, independent nested NBT, dirty/no-op behavior, fractional fuel, duplicate identity policy,
generation rejection, entity snapshot serialization and removed handles. A fixed binding in the **real gameplay SavedData**
must survive a full server stop/restart while no item is loaded; a stale item on the second boot must recover that authority.
Another binding must remain deleted after restart. The item callback is also invoked with a server-side player object.
Duplicate slot tests use a controlled location map, **not real client creative clicks**; entity UUIDs/snapshots in this suite
are data fixtures, **not spawned heroes**. The smoke runner requires separate storage and binding success markers for each boot.

Final evidence: `build/compatibility-1.20.1-smoke/20260919T085637Z-52440/report.json`.
Both production loaders passed two boots: 45 binding assertions on first boot and 47 on restart,
alongside the previous 28/29 storage assertions and five bootstrap checks on each boot. All four boots saved and stopped
normally, with no ERROR/FATAL entries. The final artifact audit has nine rejection tests (including missing authority classes).
Existing 1.21.1 baseline and the five client-log classifier tests still pass. This invocation opened no client and left no
test server/installer process running; other workspaces' games and Java daemons were not stopped.

| Tested artifact | SHA-256 |
| --- | --- |
| Fabric `0.2.2-dev.1` | `234a12948c0e02038caaeb435629aa57b5ff028233e06b036887dd3d341c4a84` |
| Forge `0.2.2-dev.1` | `efeb9000bbf82bc427d2d426f615ed4ebf920adc804ccea3dbaa82294cb03ff6` |

## First network transaction and safe manual launch — 2026-09-19

Fabric now uses its 1.20.1 play-networking receivers; Forge uses a versioned `SimpleChannel` with explicit packet directions.
Both enqueue inventory work on the server thread and return a bounded result packet on the client thread.
The first transaction moves existing fuel from a **server-owned player inventory slot** into the summoner's internal fuel slot.
Its fixed-size request contains IDs, slots, count and expected revision, never authoritative client ItemStack/NBT data.
The server checks life/spectator state, current menu, physical summoner identity, registration, revision, source count/type,
NBT-compatible stacking and remaining space. It commits all eight slots before consuming the physical source;
replays/stale requests, unavailable space and unsupported item categories consume nothing.

This is the backend for future menu/creative adapters, not yet a mouse-click feature. Fuel charging from the internal buffer,
relic/accessory insertion and removal, custom menu widgets and actual creative cursor/slot mapping remain pending.
The initial transaction only accepts the player's inventory menu; an open chest/custom menu cannot reuse those indices.
No source locale text or published gameplay rule changed; encyclopedia and translation review state remain unchanged.

Production server evidence: `build/compatibility-1.20.1-smoke/20260919T092749Z-82164/report.json`.
Both loaders passed both boots with 28 new network/transaction/pause-policy assertions per boot, alongside the existing
45/47 binding, 28/29 storage and five bootstrap checks. All four servers saved/stopped normally without ERROR/FATAL.
Exact JAR hashes are recorded in that report (it supersedes the previous checkpoint's hashes).

The manual Forge route was independently launched with **only** `-PauseOnJoin`: it logged a released mouse and the integrated
server's pause at 17:28:52, remained open longer than the automation's 12-second auto-close delay, and was closed at 17:29:41 with an ordinary window-close
request to the verified owned process. It saved all dimensions and exited with code 0. Its preserved log is
`build/compatibility-1.20.1-smoke/20260919T092749Z-82164/clients/forge-manual-pause.log`.

Automated client transport tests temporarily save two inventory slots and the game mode, send a real insertion request and an exact
replay in survival and creative modes, verify authoritative conservation (12 source items become 7 outside + 5 inside), then restore
the inventory/mode and remove the fixture binding. They run only under `-StartupOnly`, not the author's desktop shortcut.
They do **not** execute a creative GUI click or prove third-party inventory mod compatibility.

Both development-client reruns passed the full audit: Forge at 17:30:47 and Fabric at 17:31:55 logged successful survival and
creative network checks, then saved/exited normally. Preserved logs are `clients/forge-network-smoke.log` and
`clients/fabric-network-smoke.log` under the same evidence directory. The launcher confirmed ownership-scoped process cleanup;
a final process inspection found no remaining client or server from this invocation. Nine artifact rejection tests, five log-classifier
checks and the existing 1.21.1 baseline also passed. Production-JAR client testing is still distinct and pending.

## Fuel menu checkpoint — 2026-09-19

Right-clicking the summoner in either hand now opens its registered menu on both loaders. The screen reuses the author's
241 x 201 background, slot hints, empty skill frames and fuel bar; all eight custom slots and 36 player slots retain their
original coordinates. This is explicitly the **fuel slice**, not the complete hero screen: relic/accessory slots reject
interaction, and a Chinese/English internal-stage notice explains what is not available. No replacement skill art was drawn.
The item is currently obtained with `/give @s echo_warrior:test_echo_summoner`; creative tabs are not ported yet.

- Fuel supports normal cursor insertion, right-click half withdrawal, Shift transfer, and left-click insertion onto the
  locked summoner inside this menu. Ordinary non-fuel inventory Shift transfers still work between inventory and hotbar.
- The physical source is locked against pickup, Shift, drop and number/offhand swaps. Losing its UUID invalidates the menu.
- A server click commits all eight contents together. Client prediction never saves item NBT; closing does not flush a
  stale snapshot. Authority changes reload the menu, and stale world revisions or vanilla click state IDs are rejected.
- Fuel charging runs from the server item callback every five ticks, even with the menu closed. One rotten flesh gives
  20 energy, soul sand/soil gives 50, capacity is 1000. Insufficient headroom preserves the item. Buffer consumption and
  energy gain share one authority revision; duplicate callbacks in the same world tick cannot charge twice.
- A client-only adapter acknowledges the cursor packet's state ID for this menu. Vanilla 1.20.1 updates the cursor stack
  without that ID; strict stale checking would otherwise reject the next legitimate placement.
- Forge creates the menu factory in its registry event, not the mod constructor. Earlier construction indirectly loaded
  the static Item field while the registry was frozen; that failed candidate was rejected, not signed off.

Final production server report: `build/compatibility-1.20.1-smoke/20260919T112637Z-65492/report.json`.
Both production loaders passed two boots, each with 29 menu assertions plus the existing 5 bootstrap, 28/29 storage,
45/47 authority and 28 network/pause-policy checks. All four saves/shutdowns completed with no ERROR/FATAL entries.
The report records the exact hashes; this checkpoint supersedes older artifact hashes above.

`MenuClientSelfTest1201` uses the real item-use/open-menu and vanilla click packets in survival and creative modes:
12 items are inserted, withdrawn to the cursor, returned to inventory, the menu is closed/reopened, then an intentionally
stale click is rejected. It checks the server's final contents and restores inventory, selected hand, mode and fixture binding.
This tests a **custom container while in creative mode**, not the separate creative catalogue/inventory-screen click adapter.
The launcher requires both menu-test success markers in addition to the previous transport/model/save checks.

The exit-code audit also reproduced a Windows PowerShell 5.1 issue: after polling a batch process, `ExitCode` could become
null for both success (0) and failure (7). Retaining the process handle at launch fixes this; the new
`scripts/test-client-process-exit.ps1` reproduces the distinction and checks the launcher guard. Null is never accepted as success.
Twelve JAR rejection tests (including a missing menu-sync refmap entry), five log-classifier checks and the existing
1.21.1 artifact baseline pass.

Final development-client acceptance passed on both loaders with the corrected exit-code audit: Forge menu checks at
19:30:15 and Fabric at 19:32:12–13, followed by normal save/exit and ownership-scoped cleanup. Preserved latest/stdout/stderr
logs are under `clients/` in the final production-server evidence directory above. Both survival and creative menu and
network rounds passed. A final process inspection found no matching project client/server/runClient still running.
The report's `client_tested: false` deliberately refers to **production-JAR clients**, which these development runs do not prove.

Reusable lessons were added to playbook PORT-002, PORT-006 and PORT-013. These restore existing rules on a new version;
no encyclopedia gameplay entry or published 26.1.2/1.21.1 code/localization was changed. The two temporary internal UI strings
exist in both mandatory source locales; full translated resources and visual acceptance remain future porting work.

Optional manual checks for this slice (not required before the next development task):

- [ ] Use the desktop Forge shortcut; confirm automatic pause, then resume and right-click the summoner.
- [ ] Check original layout, item hover tooltips and fuel bar at different GUI scales.
- [ ] Move fuel in/out, Shift-transfer, close/reopen and confirm no restoration of withdrawn items.
- [ ] Leave fuel charging with the GUI closed; confirm full capacity leaves excess items in the buffer.
- [ ] Try moving/dropping/swapping the source summoner while its GUI is open; it must remain locked.

Still absent from this slice: fuel transfer particles, same-identity held-item animation suppression, full hero controls,
relics/accessories and their feedback, as well as all creative catalogue insertion/destruction paths.

## Relic and accessory checkpoint — 2026-09-19

This supersedes the fuel-only checkpoint above. All five relics and 25 accessories are registered on both loaders;
the original Echoes/accessories creative categories expose the available items. Knowledge content and its third tab remain pending.
Model/texture bindings and both source locales are checked against existing authored/reviewed resources, not newly redrawn or retranslated.
The artifact audit checks talent descriptions as well as names; 15 damaged-artifact rejection tests guard the checks themselves.

- Per-relic state now uses detached, copy-on-write NBT: one-time identity/trait initialization, level/XP, activity/alert modes,
  enabled skills, Egyptian off/leaf/cone selection, charge timers and cooldowns. Catalogue prototypes stay uninitialized.
  Same-value updates preserve the existing tag; failed updates preserve all state and other mods' NBT.
- The six accessory slots and relic slot support normal cursor clicks and Shift transfer. Left-clicking the locked source
  summoner **inside its own menu** inserts eligible equipment into an empty slot. Same-type accessories remain unique even
  if renamed. Full/incompatible slots leave the source untouched.
- Relic removal or replacement saves the eight contents and invalidates the old binding in one revision. Editing the same
  relic's skill state does not dismiss it. This is an authority test, **not evidence of actual entity removal**, which needs the entity port.
- Source wording is reused during `processResources`, with local internal-stage warnings layered on top. Non-source locales
  and the complete 14-locale resource matrix are not yet included. The published branches' wording/review state is unchanged.

Production evidence: `build/compatibility-1.20.1-smoke/20260919T115103Z-38196/report.json`.
Both production loaders passed two normal boots/saves/stops. Each boot ran 114/115 new relic/equipment assertions plus the existing
5 bootstrap, 28/29 storage, 45/47 authority, 28 network and 29 fuel/menu assertions. The new suite also verifies 500 seeded talent
rolls, exact level-cap/charge boundaries, unchanged external NBT, item network serialization and real two-boot relic persistence.

Development-client automation checks all 31 baked item models, textures and localized names, then uses real item-use and vanilla
click packets to install/retrieve a grown relic and an accessory in survival and creative modes. It closes/reopens the menu,
checks there are no restored items, and restores the original inventory/mode. It does not test creative catalogue clicks.
Both development-client runs passed and exited normally: Forge menu rounds at 19:52:00–01 and final save at 19:52:11;
Fabric menu rounds at 19:53:36–37 and final save at 19:53:48 (local UTC+8). Latest/stdout/stderr logs are preserved in
`clients/` alongside the production report. Both launcher invocations returned exit code 0 and confirmed owned-process cleanup.
Final process inspection found no remaining project test client/server/runClient processes. After the final resource-task rebuild,
the JAR hashes still match the tested production report. The 1.21.1 baseline and five log-classifier tests also passed.
Production-JAR client verification remains pending; these are development-client runs and do not change the report's `client_tested: false`.

Optional manual checks (development can continue without them):

- [ ] Use the desktop Forge shortcut; confirm it still pauses on entry, then resume.
- [ ] Check both creative categories and Shift tooltips on relics/accessories.
- [ ] Open the summoner, insert/remove a relic and six different accessories; close/reopen and confirm contents.
- [ ] Try a duplicate accessory, including an anvil-renamed copy; it should stay outside.

Still absent: direct bundle-like insertion from player/creative inventory screens, their insertion particles and destruction paths,
same-identity held-item animation suppression, hero controls/entities/combat, full content and translated locale matrix.

## Direct inventory insertion checkpoint — 2026-09-19

This supersedes the missing direct-insertion/feedback portion above. Survival inventory, creative catalogue hotbar,
creative inventory wrappers and the locked source inside the summoner menu now share one insertion planner.
Creative requests identify the real inventory slot, UUID and revision; they never supply authoritative summoner contents.
The server commits equipment, then acknowledges the inserted count. Only that ACK consumes the creative cursor and plays
the original fuel particles or alpha-masked diagonal polish. Failed/duplicate/full/stale requests do not consume the source.
Normal inventory feedback is also sent only after successful server mutation; fake players without a client skip visual networking.

Production report: `build/compatibility-1.20.1-smoke/20260919T130726Z-45840/report.json`.
Both production JARs passed two boots/saves/stops, including 28 new creative-request assertions and all previous suites.
Sixteen damaged-artifact rejection tests pass. Production-client verification remains pending.
Both development clients passed actual catalogue/wrapper/survival clicks with grown relic NBT, accessory and fuel conservation;
they additionally require submitted nonzero polish pixels, not just an enqueue log. Forge checks finished at 21:10:11 and
Fabric at approximately 21:05:50 (UTC+8), followed by normal exit and owned-process cleanup; logs are preserved under `clients/`.
The Fabric development run preceded the server-only null-connection feedback guard; production Fabric was rebuilt and tested after it.

The three-screen test caught a real lifecycle difference: `InventoryScreen.containerTick()` does not call its superclass,
so the inherited sweep clock stayed at zero. Feedback now ages in the shared final `AbstractContainerScreen.tick()`.
ACK feedback uses the actual clicked slot, never a later hovered slot. Playbook PORT-007/008 records these guards.
The launcher now keeps a single-line `options.txt` as an array under Windows PowerShell 5.1; eight executable cases verify
repeat runs do not concatenate preference keys or change unrelated preferences.
Automated inventory fixtures temporarily protect the player from hostile mobs; protection is restored before logout/save,
with a stop fallback. An early experimental fixture saved its temporary flag; only the marked Forge CATTEST player/level data
were restored, with backups at `build/test-fixture-recovery/20260919-210850/`. A subsequent full Forge run and read-only NBT
check confirmed both flags are false. No published worlds or other workspaces were modified.

Optional final manual checks: observe the fuel particles/diagonal polish at several GUI scales in all three inventory screens;
try rapid insertion, failed duplicate accessory insertion, and cursor changes under network latency. Rendering assertions do
not certify visual timing or taste. No gameplay rule, source wording or encyclopedia article changed.

## Creative destruction checkpoint — 2026-09-19

Both development clients now pass 12 actual creative-screen/network cases: catalogue Shift deletion, cursor-to-catalogue,
single/Shift trash, cursor on close, preset overwrite and nested-container deletion; Q/outside drops, inventory Shift moves,
unproven requests and incomplete visibility scans retain the binding. Drop cases additionally conserve exactly one physical
summoner. Forge finished at 21:29:32 and Fabric at 21:31:22 (UTC+8); both saved and exited normally without mouse capture.
Logs are preserved in `build/compatibility-1.20.1-smoke/20260919T133145Z-11096/clients/`.
These are binding-record assertions, not yet actual living-hero assertions. Hero removal must extend them in the next slice.

Server confirmation requires the same player's observed creative slot removal plus a bounded candidate packet and a complete
visibility scan after a tick boundary. A shared scan budget fails closed (retains bindings); arbitrary client UUIDs alone do
not authorize removal. Seventeen artifact rejection tests and eight launcher options tests pass. Forge CATTEST's persisted
temporary protection flags were read back as false after the tests. The desktop manual path remains unchanged.

The Q-drop test found the old catalogue's live ItemStack reference being emptied before its drop packet; a scoped read snapshot
fix preserves vanilla packet/drop handling. PORT-005 records the cause, safeguards and remaining manual checks. No existing
gameplay rule or encyclopedia article changed. Production-server revalidation for this checkpoint is recorded alongside its logs.

## Historical remaining scope — before the final regression

Actual **production-JAR clients** still need independent verification: development client success and production server success do not prove production client Mixins.
Therefore stage 0's full production-client exit condition is not yet signed off.

Earlier dated checkpoints above record their scope at that time; the latest checkpoint below supersedes their unfinished-hero notes.
Remaining work includes exploration behavior and modified-chunk safety, dynamic relic item overlays, real disk/chunk reload
with heroes/projectiles, dedicated production-client mapping/render checks and a consolidated visual/multiplayer checklist.
All actions must continue to preserve possession, menu identity, eligible slots, revision and physical item conservation.
No gameplay/control/balance rule is intentionally changed by this port; encyclopedia articles therefore need no changes.

## Hero, books and recycler checkpoint — 2026-09-20

Production report `build/compatibility-1.20.1-smoke/20260920T004958Z-11308/report.json` passed both loaders, two boots each:
128 hero lifecycle assertions, 107 book assertions and 49 real recycler assertions, in addition to earlier storage/menu/network suites.
Recycler checks roll actual loot, round-trip a pending sealed transaction through NBT, reject player/explosion destruction
while sealed, commit once and preserve input on insufficient space. This is not yet disk/chunk unload acceptance.

Fabric development client on September 20 at 09:01 (UTC+8) passed 43 baked item models, 12 creative cases with actual entities,
all five heroes with actual AI damage, and four book cases (tutorial/knowledge in main/offhand). All tutorial pages and
knowledge bookmark/reopen/extraction were exercised; temporary inventory was restored and the game saved/exited.
The wrapper then incorrectly rejected the changed initialization prose (`bootstrap` versus `internal compatibility build`).
The marker matcher now keys on the version and loader, with ten log-classification tests. Whole-launcher revalidation is pending.

Books now synchronize the server's actual source inventory index and only mirror a received slot snapshot, regardless of packet order.
They never guess main/offhand from the item type. Knowledge hover tooltips are deferred to absolute screen coordinates after content.
Recycler item rendering lazily uses the vanilla chest parts including the lock, with the vanilla no-world orientation.

Exploration code and all 31 old-format recipes compile on both loaders. The first exploration selftest uncovered correlated
first draws from sequential random seeds in its eight-page loot coverage fixture; its fixed random-seed stream is being revalidated.
No exploration runtime pass is claimed from compilation or file presence.

Planning and reusable rules:

- `docs/COMPATIBILITY_1.20.1_ASSESSMENT.md`
- `docs/VERSION_PORTING_PLAYBOOK.md`
- `PROJECT.md`, section 4.3

Confirmed rollback baseline on GitHub before development: `c9b521cbd23b7e8e0e45d284a72d6a2fca96094c`
(`main` and `codex/release-0.2.1-localization`), Echo Warrior 0.2.1.

## Initial full-content automated checkpoint — 2026-09-20 (superseded)

This historical handoff checkpoint superseded the unfinished-scope notes above. Use the newer regression checkpoint below for current hashes.
Five heroes and all existing content are ported to Fabric + Forge. Both final production artifacts passed the checks below.
This is an internal test candidate, not a public release or a claim that human acceptance is complete.

| Artifact under this version's directory | SHA-256 |
| --- | --- |
| `fabric/build/libs/echo-warrior-fabric-1.20.1-0.2.2-dev.1.jar` | `5118688f83c6fd50af1605e8ab56f5b66d9b0fecfb07cc2c710499ac189c8e96` |
| `forge/build/libs/echo-warrior-forge-1.20.1-0.2.2-dev.1.jar` | `1ca041e5f0f495f0bc799a2db5fbb3a6b6846f951f00ee262eb101f2b4aa19a8` |

Exact local evidence (paths relative to the repository root):

- Both production dedicated servers, two boots each: `build/compatibility-1.20.1-smoke/20260920T021037Z-82892/report.json`.
- Fabric production client: `build/compatibility-1.20.1-production-client/20260920T021227Z-75704/fabric/report.json`.
- Forge production client: `build/compatibility-1.20.1-production-client/20260920T021040Z-73224/forge/report.json`.
- Each client report records `passed=true`, `cleanup_passed=true`, dependency hashes and the official installed profile.
- `check-1.20.1-runtime-evidence.py` verified that both clients, the dedicated servers and the current deliverable JARs have identical artifact hashes. Clients ran sequentially and saved/exited normally; no owned client remained running after the suite.

Passed regression coverage:

- Dual Wrapper build; Java 17 classes, production refmaps, loader separation and complete resources; 34 corrupted-artifact rejection tests.
- Structural coverage of all 134 1.21.1 common Java modules, with seven explicit old-API replacements. Runtime evidence below is independent of this structural check.
- Both server boots: storage/world authority, network transactions, 289 menu assertions, relic/accessory state, creative authority, 129 hero lifecycle assertions, 107 book assertions, 49 recycler assertions and 1330 exploration assertions.
- Actual 96-block normal/lucky/silk mining paths and real discounted villager trades, including second costs, stock consumption, close/reopen and original-tool preservation.
- Actual safe-site placement, six to nine brushable blocks with one guaranteed relic, modified/fluid/tree/slope/unloaded rejection and unchanged-state synchronization to newly acquired compasses.
- Five real heroes and one arrow saved by vanilla entity chunks on boot one, read from disk on boot two, then actual cross-dimension reconstruction with health, absorption, fuel, UUID/owner/generation checks. Old generations and their arrows are invalidated.
- Nested shulker destruction versus ordinary spilled-item retention, expiration and void destruction; menu event feedback across two seven-bit sequence wraps through real vanilla packet serialization.
- Both installed clients: all 43 baked item models, six status sprites, tinted compass/grass and real recycler/brushable rendering; real survival and both creative inventory insertion paths with visible polish pixels; 12 creative lifecycle cases with real heroes; five real AI combat hits, skill/mode persistence, health and GUI-only Shift overlays; four main/offhand book scenarios.
- Test-tool checks: six offline production-preparation tests, eight options cases, ten log-classification cases, actual child-process exit-code checks and PowerShell syntax checks. Existing 1.21.1 dual build/baseline also passed; higher-version gameplay sources were not changed.

Server logs contain no ERROR/FATAL. Installed offline test accounts can emit vanilla authlib's exact
`Failed to verify authentication` / `InvalidCredentialsException: Status: 401` pair; only that narrow,
tested exception is classified as expected. Other errors (including DFU or profile-key failures) still fail the 1.20.1 audit.

Investigation notes, not gameplay changes: the disk fixture initially compared HP while the Aztec sun aura was healing;
it now disables that skill only on the test relic. NoAI alone does not stop a global aura. A trading fixture initially used
the no-op vanilla `overrideOffers`; it now installs real offers and executes a real menu trade. Both fixes strengthen tests,
not alter the mod's healing or trade rules. Metadata now reuses the mainline description instead of the old bootstrap warning.

Remaining author-facing work is the consolidated manual checklist, especially animation/sound quality, natural terrain,
real two-player network conditions and third-party mod combinations. The desktop Forge pause-on-join shortcut is present;
its test world's prior temporary invulnerability flags were read back as false. No CF/Modrinth upload, Git commit/push,
release tag, localization waiver or translation-baseline approval was performed. Resolve the empty localization acceptance
record before preparing a public release. No new gameplay specification means no encyclopedia content change.

Recheck the exact handoff artifacts:

```powershell
python scripts/check-1.20.1-runtime-evidence.py --server-report build/compatibility-1.20.1-smoke/20260920T021037Z-82892/report.json --fabric-client-report build/compatibility-1.20.1-production-client/20260920T021227Z-75704/fabric/report.json --forge-client-report build/compatibility-1.20.1-production-client/20260920T021040Z-73224/forge/report.json
```

## Previous presentation/reach regression checkpoint — 2026-09-20

Four author-reported issues are addressed in **1.20.1 only**, still internal `0.2.2-dev.1`:

- All five GUI previews now advance their detached animation age without running entity AI/world simulation. The old fixed `tickCount` made GeckoLib's partial-tick time rewind every tick.
- Guandao blade particles use the current-frame `WeaponParticleAnchor` world matrix. Full valor retains flame and adds red/gold sparks; emission budgets are per warrior and exclude GUI-only entities.
- Guandao idle/walk explicitly drive the root to its rest rotation. A short action-release window avoids blending a full-body Euler-equivalent +/-180-degree pose into walking. The authored combo, damage timing and normal idle/walk smoothing remain intact.
- The SBL 1.15 melee adapter now uses the hero predicate at both start and delayed damage, without a hidden vanilla-reach cap. Aztec stays at the mainline `2.25 + target half-width`; sweep radii remain 2.5/3.0. Guandao's shared adapter also honors its existing predicate. This is parity restoration, not a balance increase.

| Artifact at this checkpoint (not the later departure-effect build) | SHA-256 |
| --- | --- |
| `fabric/build/libs/echo-warrior-fabric-1.20.1-0.2.2-dev.1.jar` | `cdce4862246d92fdeb9253c2da94d518658e3df353021cc308f8023fea20f3c8` |
| `forge/build/libs/echo-warrior-forge-1.20.1-0.2.2-dev.1.jar` | `2fe31e77beaac08772fa49ab183a29c94340a19a71714749ede6c775654c6717` |

Evidence (relative to repository root):

- Both production dedicated servers, two boots each: `build/compatibility-1.20.1-smoke/20260920T025038Z-53408/report.json`.
- Forge production client: `build/compatibility-1.20.1-production-client/20260920T025108Z-67272/forge/report.json`.
- Fabric production client: `build/compatibility-1.20.1-production-client/20260920T025329Z-42648/fabric/report.json`.
- The reach test first failed on the old adapter at the expected 2.4-block start assertion: `build/compatibility-1.20.1-smoke/20260920T024233Z-80380/forge/console-1.log`. After the fix both loaders pass start, delayed hit, moving-target, out-of-range, wall, vertical-overlap, dead/absent-target and cooldown checks.
- Both clients pass the five-hero 40-tick detached preview checks. `GuandaoPresentationClientSelfTest1201` uses the installed GeckoLib processor/resources, checking combo-to-idle/walk and three action-state/stop ordering cases at quarter-tick intervals, plus particle scheduling, per-entity independence and preview exclusion.
- Dual build, full artifact audit, **36** corrupted-artifact rejection tests, 134-module coverage check, ten log-classification cases, eight options cases and launcher PowerShell syntax passed. Exact-hash audit matches the final JARs to both client and server runs. No higher-version build was rerun in this task; no higher-version gameplay sources were changed.
- Clients ran sequentially with mouse capture disabled, saved and exited; both reports record cleanup success and a final process check found no Minecraft client. No desktop shortcut, player world, Git remote or publication was modified.

Remaining manual acceptance is limited by these tests: observe all preview hands/weapons continuously, blade attachment during movement, multiple simultaneous warriors, combo recovery and moving enemies. Particle scheduling and bone checks do **not** establish human visual acceptance. The corresponding same-source 1.21.1 issues are recorded for a future explicit synchronization, not claimed fixed here.
Reusable causes/guards are in PORT-003, PORT-026 and PORT-027. No new wording or gameplay specification was introduced; mandatory source locales and encyclopedia content need no changes.

```powershell
python scripts/check-1.20.1-runtime-evidence.py --server-report build/compatibility-1.20.1-smoke/20260920T025038Z-53408/report.json --fabric-client-report build/compatibility-1.20.1-production-client/20260920T025329Z-42648/fabric/report.json --forge-client-report build/compatibility-1.20.1-production-client/20260920T025108Z-67272/forge/report.json
```
