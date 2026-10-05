# Echo Warrior for Minecraft 1.21.1

This directory is the isolated Minecraft 1.21.1 compatibility build. Minecraft 26.1.2 remains the feature-development baseline in the repository root.

Read `../../docs/VERSION_PORTING_PLAYBOOK.md` before extending or auditing this compatibility line. Current manual acceptance is tracked in `../../docs/COMPATIBILITY_1.21.1_TEST_CHECKLIST.md`.

Current release candidate: `0.2.3` (server-performance fix). Four-loader production-server regression passed on FOXY-NODE; final release checks, file IDs and acceptance boundaries are recorded in `../../docs/releases/0.2.3.md`. Earlier evidence remains tied to its recorded JAR hashes.

## Local battlefield performance fix — 2026-10-05

Locked compass targets now use one region lookup; a 2048-block search uses at most 49 lookups,
independent of explored-world history. Gameplay queries no longer iterate all saved regions.
Archaeology removals use the actual server `BlockBehaviour.onRemove` callback instead of a
once-per-second scan of all active/salvage positions. Chunk-load repair reads only registered
positions in the supplied chunk, including sites crossing region boundaries. Save format,
compass range, sound timing and replacement cooldowns are unchanged; restart to apply the Mixin,
with no new world/chunks required. No wording or encyclopedia change is needed.

Both loader builds and the source/JAR baseline passed. Both isolated **development** dedicated
servers ran `BattlefieldPerformanceSelfTest1211` and shut down normally. The test rejects full Map
iteration with 10,000 historical regions, checks 1/49 lookups and negative/boundary coordinates,
then exercises real block replacements, stage-update negatives, salvage cleanup and chunk-load
entry repair. Logs: `run-fabric-server/logs/latest.log` and `run-neoforge-server/logs/latest.log`
(15:06 Asia/Shanghai); both contain `BATTLEFIELD PERFORMANCE SELFTEST PASSED`.
Fabric retains its existing six data-fixer ERROR messages accepted by the smoke script.
No graphical client or original reporter's pack was tested; Spark improvement in that pack is
still pending. Classify the reusable porting omission as PORT-030. No publishing/version bump.

| Local 0.2.2 JAR (not the published file) | SHA-256 |
| --- | --- |
| Fabric | `8c88646a9c4f70e77328d2a9af25c96b7782c700dd5fd0b97e84a379a730d1b8` |
| NeoForge | `c7f98a9177033b095d4c20931d54bd65eb9695e1dd12c0a02dbcdffc5dcd7569` |

## Historical snow-cover implementation checkpoint — 2026-09-20

Battlefield generation now preserves 1–8 snow layers and their thickness, placing archaeology in the
natural ground below them instead of clearing snow as vegetation. Solid snow blocks are not excavated.
Only new sites change; previously removed snow is not reconstructed. This shared fix also applies to
26.1.2 and 1.20.1. Both loader builds and the artifact baseline passed. `check-battlefield-snow.py`
guards equal terrain policies/call sites across versions; real placement under all eight layer thicknesses
passed on both 1.20.1 production servers. **1.21.1 in-game snow behavior was not run this task**;
manual acceptance remains pending. No publishing or version-number change.

Snow checkpoint SHA-256:

- Fabric: `7098b5f8780ef10dcba3c19e58ef41aa80e49ca7dcdd27a26e6df00071da838d`
- NeoForge: `7b0fd086714ec28bb9d10bdf8bbaabb9f3b0bc88fead941a608fa958725ef2c6`

## Historical departure-effect fix — 2026-09-20

Actual dismissal, relic replacement, summoner destruction and successful reconstruction now call the hero's
`dismiss()` instead of bypassing soul particles/sound with `discard()`. All five heroes additionally send
24 SOUL particles at death tick 20, before vanilla removal; the binding still expires immediately on death.
Vanilla mobs and silent rollback/cleanup remain unchanged. 26.1.2 is untouched, and this version retains
its native wind/mace Guandao finisher sounds; only the 1.20.1 anvil fallback changes to a critical-hit sound.

Both loader builds and the artifact baseline passed. Both isolated **development** dedicated servers ran
`DepartureEffectsSelfTest1211` through the normal selftest command and shut down normally. It captures
real outgoing particle packets for all five heroes across item dismissal, ID dismissal, destruction and death,
including exact count, position, no duplicate playback and vanilla/silent negative cases.
Logs: `run-fabric-server/logs/latest.log` and `run-neoforge-server/logs/latest.log` (11:42–11:43 local).
Fabric still logs its existing six “No data fixer registered” messages; this is not a claim of zero ERROR logs
or a fresh production-client/visual acceptance pass. No graphical client was launched for this targeted task.

Local build hashes (same filenames/version as 0.2.1, **not** the previously published files; no upload):

| Local JAR | SHA-256 |
| --- | --- |
| `fabric/build/libs/echo-warrior-fabric-1.21.1-0.2.1.jar` | `b37137fdfb9d149b8f16a7e2b172fac2db4732ed62764f64818afbc56f0764b1` |
| `neoforge/build/libs/echo-warrior-neoforge-1.21.1-0.2.1.jar` | `447a3554eca50c40156aec696e522662dda10f02a6944294b973816aecb85032` |

Presentation-only restoration/addition; no player wording or gameplay values changed, so no localization or
encyclopedia change is needed. Reusable lifecycle guidance is PORT-028; visuals and sound remain on the manual checklist.

Build both loader packages from the repository root:

```powershell
.\scripts\build-1.21.1.ps1
```

Build one loader only:

```powershell
.\scripts\build-1.21.1.ps1 -Loader Fabric
.\scripts\build-1.21.1.ps1 -Loader NeoForge
```

Build and validate both loader JARs without launching Minecraft:

```powershell
.\scripts\check-1.21.1-baseline.ps1
```

Run the isolated dedicated-server startup and five-hero compatibility self-test for both loaders:

```powershell
.\scripts\smoke-test-1.21.1-servers.ps1
```

Run an automated client startup and resource-loading smoke test for either loader:

```powershell
.\scripts\run-test-client.ps1 -TargetVersion 1.21.1 -Loader Fabric -StartupOnly
.\scripts\run-test-client.ps1 -TargetVersion 1.21.1 -Loader NeoForge -StartupOnly
```

The compatibility runtime directories are isolated under this directory and must never reuse the 26.1.2 `CATTEST` worlds.

## Current five-hero compatibility line

The initial Roman Legionary vertical slice has expanded into the current five-hero compatibility line. Implemented on both loaders:

- Roman Legionary relic, Echo Summoner, and Roman Legionary entity registration.
- Server-authoritative binding SavedData with duplicate-generation rejection, fuel, mode, skill, charge, cooldown, dimension, position, and health persistence.
- Functional summoner menu with relic/fuel insertion, 1000-point fuel store, 100-point summon cost, activity/alert controls, and four skill toggles.
- Six open accessory slots with duplicate-item rejection and the five Roman accessories: Plate Armor, Hawkeye Lens, Feast Ham, Light-Gathering Magnet, and Victor's Laurel. Installed state is server-authoritative, persists across restart, immediately refreshes a loaded Echo, and decorates health, armor, movement, targeting, kill XP, and kill healing.
- Shift-right-click summon, loaded-entity recall, and unloaded/cross-dimension reconstruction.
- SmartBrainLib follow/wait/wander and alert behaviours, target retention, friendly-fire filtering, and a two-stage melee attack with bounded safe tracking that stops at walls, hazards, fluids, ledges, and activity boundaries.
- Soldier Formation, Legionary Bulwark, Shield Charge, The Legion Endures, and fuel-powered out-of-combat healing. Shield Charge includes moving-segment projectile interception, shulker-bullet rehoming, and 40-tick creeper target/fuse suppression.
- GeckoLib 4 renderer and idle, walk, two-stage attack, hurt, and shield animation states.
- The Roman procedural visual-life layer: server-synchronized world-space attention, combat/damage/creeper/rapid-approach priority, forward locomotion gaze with the 15-degree head and 10-degree residual-eye limits, natural and double blinks, hurt blink and pupil contraction, safe-idle curious tilt, player-initiated mutual gaze, owner-caught-watching reactions, safe look/turn/walk-away exits, post-exit owner avoidance, animated-parent compensation, and a 70%-strength shadow.
- Dedicated-server assertions for registry IDs, item components, authoritative fuel/relic/accessories, duplicate accessory rejection, accessory calculations, modes, skill state, charges, cooldown, binding generations, complete binding NBT round-trips, attributes, entity NBT round-trips, procedural blink/gaze and mutual-gaze acquisition math, attack-tracking hazard rejection, moving interception geometry, shulker-bullet rehoming, and persistent creeper suppression.
- Automated Fabric and NeoForge client startup checks through title-screen resource loading.
- Aztec Warrior, Egyptian Archer, Chinese Guandao Warrior, and Japanese Samurai entities, relics, skills, combat state, renderers, models, animations, and persistence.
- Battlefield archaeology, the Echo Compass, knowledge fragments and collection, the 44-page tutorial manual, the Echo Salvage Chest, five legacies, 25 accessories, relic growth, and 22 relic talents.
- Source/JAR regression checks for localization parity, resources, recipes, tags, client feedback, stale-menu protection, safe summoning, and loader metadata isolation.

The current candidate is intended to match the approved existing 26.1.2 gameplay scope, with target-version-specific implementations where APIs and resource formats differ. Automated builds, JAR audits, and dedicated-server checks do not replace manual acceptance of GUI, animation, sound, controls, and multiplayer feel; use the compatibility checklist for final sign-off.

Compatibility-only engineering does not change player-visible design or balance values, so it does not require an encyclopedia update unless a port also changes actual gameplay behavior.
