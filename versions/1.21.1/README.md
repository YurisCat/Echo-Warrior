# Echo Warrior for Minecraft 1.21.1

This directory is the isolated Minecraft 1.21.1 compatibility build. Minecraft 26.1.2 remains the feature-development baseline in the repository root.

Read `../../docs/VERSION_PORTING_PLAYBOOK.md` before extending or auditing this compatibility line. Current manual acceptance is tracked in `../../docs/COMPATIBILITY_1.21.1_TEST_CHECKLIST.md`.

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
