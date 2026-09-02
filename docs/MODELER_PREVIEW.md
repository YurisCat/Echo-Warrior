# Model Artist Preview

## Status

This repository currently contains a temporary gameplay and model-integration prototype for the Echo Warrior project.

The following are placeholders and may be replaced or substantially revised:

- Roman legionary model, texture, scale, and animation timing.
- Test Echo Summoner artwork and behavior.
- Entity attributes, follow distances, combat behavior, particles, and sounds.
- The `CATTEST` development scene and its layout.

The prototype exists to verify the complete Blockbench → GeckoLib → Minecraft workflow and to make visual feedback fast. It should not be treated as final submission content.

## Files for the model artist

- Editable Blockbench source: `assets-source/blockbench/roman_legionary.bbmodel`
- Current model-artist handoff master: `outputs/roman_legionary_modeler_master.bbmodel`
- Reserved-bone reference (easy-to-view PNG): `outputs/roman_legionary_visual_bones_reference.png`
- Reserved-bone reference (editable SVG): `outputs/roman_legionary_visual_bones_reference.svg`
- Facial-control change note: `outputs/roman_legionary_visual_changes.txt`
- Runtime entity texture: `src/main/resources/assets/echo_warrior/textures/entity/roman_legionary_echo.png`
- Exported GeckoLib model and animations: `src/main/resources/assets/echo_warrior/geckolib/`
- Direct local playtest launcher: `tools/windows/Launch Test Client.bat`

## Current Roman legionary animations

- `animation.roman_legionary.idle`: 3-second loop; used automatically while stationary.
- `animation.roman_legionary.walk`: 1-second loop; used automatically while moving.
- `animation.roman_legionary.attack_1`: the modeler's original 0.79167-second single-slash source, retained for reference.
- `animation.roman_legionary.attack_2`: the modeler's original 1.04167-second two-slash source, retained for reference.
- `animation.roman_legionary.attack_first`: 0.54167 seconds, baked from `attack_2` frames 0-13; every normal attack starts here. Runtime ownership is upper-body-only so locomotion can drive the legs during a real forward step.
- `animation.roman_legionary.attack_follow`: 0.5 seconds, baked from `attack_2` frames 13-25; used only while the original target remains valid after the first hit, and likewise layered over locomotion.
- `animation.roman_legionary.attack_recover`: 0.25 seconds, based on `attack_1` frames 13-19; its first frame is smoothly re-anchored to the exact terminal pose of `attack_first`, while the stationary lower body returns through the idle layer.
- `animation.roman_legionary.hurt`: 0.5-second full-body reaction; does not interrupt an attack already in progress.
- `animation.roman_legionary.shield_raise`: holds the raised-shield pose for shield skills and previews.
- `animation.roman_legionary.shield_lower`: returns from the shield pose after shield skills and previews.

The three derived attack clips are sampled on the original 24 FPS grid and baked to linear per-frame keys so trimming the source Catmull-Rom curves cannot change the cloak, helmet, or weapon-arm tangents at the branch. They intentionally omit `root`, `body_root`, the lower-body/leg chain, and the three waist-cloth bones; the locomotion controller supplies idle or walk motion there while the attack controller owns the upper body. The original `attack_1` and `attack_2` clips remain untouched as modeler references. The game also strips animation keyframes for `head`, `left_eye`, `right_eye`, and `eyebrows`; those bones remain controlled by the procedural expression system.

Use the following commands on the nearest owned echo to inspect one-shot animations without changing gameplay damage or shield behaviour:

```text
/echo_warrior animation attack
/echo_warrior animation attack_recover
/echo_warrior animation hurt
/echo_warrior animation shield_raise
/echo_warrior animation shield_lower
/echo_warrior animation reset
```

`attack` previews the complete first-hit → follow-up chain. `attack_recover` previews the first-hit → early recovery branch. Neither command applies gameplay damage.

## Git and test-world policy

Source code, editable art sources, exported runtime assets, documentation, and launcher scripts belong in Git.

The complete `run/` directory does not belong in normal Git history. Minecraft region files and player data are binary, change frequently, create noisy repository growth, and may contain machine- or player-specific state.

If another computer temporarily needs the exact `CATTEST` scene, distribute a versioned ZIP snapshot outside normal Git history, then extract it to:

```text
run/saves/CATTEST
```

For the longer term, replace the hand-maintained world snapshot with a small reproducible test structure or setup command. That will let collaborators recreate the required scene after cloning without carrying an entire mutable save in the repository.

## Current portability limitation

The project-local Java 25 runtime under `.toolchains/jdk-25` is intentionally ignored by Git. A fresh clone on another computer therefore requires a compatible Java 25 runtime or a future bootstrap/setup script before the development launcher can run.
