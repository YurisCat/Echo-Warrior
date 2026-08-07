# Echo Warrior Visual Behaviour Contract

## Purpose

The Roman legionary prototype uses a code-driven visual-attention layer on top of GeckoLib locomotion animations. This layer is cosmetic: it must not change navigation, combat targeting, damage, or summoner binding.

## Code-owned bones

The following bones are reserved and controlled by code in ordinary gameplay:

- `head`
- `left_eye`
- `right_eye`
- `eyebrows`

Animation files must not keyframe these bones unless the project owner explicitly approves a future exception. The authoritative Blockbench source is:

```text
assets-source/blockbench/roman_legionary.bbmodel
```

Model artists must continue from the latest copy of that file rather than an older local branch.

## Attention model

The server chooses one shared attention target so every client sees the same intent. Selection is updated at low frequency and scored by salience:

1. Recent attacker or damage source.
2. Primed or nearby creeper.
3. Current combat target.
4. A player deliberately looking at the echo; this starts the mutual-gaze flow defined below.
5. A living entity rapidly approaching the echo.
6. Owner, players, and nearby living entities.
7. A quiet random point when nothing else is interesting.

Attention only changes presentation. Ordinary observation turns eyes and head; sustained high threats may also turn the body. Eyes lead the head slightly, and close targets receive subtle binocular convergence.

## Mutual gaze contract

The following behaviour was approved on 2026-08-07 and is the implementation contract for player-initiated eye contact:

- The player must look at a spherical virtual head target with a radius of approximately 0.35 blocks, centred on the echo's head. Front, side, and rear observation all count.
- The gaze must have an unobstructed line of sight. A blocked view clears acquisition immediately.
- Within 12 blocks, acquisition requires 10 continuous ticks (0.5 seconds).
- Beyond 12 blocks, required acquisition time is `0.5 + (distance - 12) / 40` seconds. There is no artificial range cap beyond normal entity loading and tracking limits.
- Up to two consecutive missed ticks are tolerated for ordinary mouse movement. A longer miss clears acquisition progress.
- Acquisition is suppressed while the echo has an attack target, or while the echo or its owner has dealt or received damage within the previous three seconds.
- On acquisition, the pupils lead, the head begins following roughly 0.1 seconds later, and the body begins a smooth turn roughly 0.2 seconds later when the player lies outside the body's forward 45-degree cone. The body stops correcting once the player is within roughly 20 degrees of forward.
- The first mutual-gaze episode lasts a random 2-4 seconds. If the player is still looking when it ends, there is a 75% chance to renew for 1-3 seconds and a 25% chance to glance away for 0.5-1.5 seconds before reacquisition is allowed.
- Damage, an active combat target, creepers, and other genuine high-priority threats interrupt mutual gaze immediately.
- When line of sight is lost during an active episode, the echo watches the player's last visible position for 0.5 seconds before returning to ordinary observation.
- Any non-spectator player may trigger mutual gaze. When several players qualify, the owner has priority; otherwise the player with the longest valid gaze duration wins. Invisible players only qualify within four blocks.

Mutual gaze must not leave SmartBrainLib and the renderer fighting over different facing directions. The presentation layer owns eyes and head, while deliberate mutual gaze may request a gentle body-facing correction without changing navigation or combat targeting.

## Reactions

- Normal blink interval: randomized between roughly 2.5 and 6 seconds.
- Occasional double blink: approximately 10% probability.
- Hurt and startled reactions briefly suppress blinking.
- Hurt pupils contract to roughly 60%, then recover quickly.
- Strong surprise contracts pupils to roughly 45-50%.
- Blinking is performed by moving the existing `eyebrows` bone down by two model units; no eyelid geometry is used.
- Safe idle observation may trigger an 8-12 degree curious head tilt.
- Different echoes use independent timing to prevent synchronized gestures.

## Shadow

The entity uses a 0.45-block shadow radius at approximately 70% strength. It should feel grounded while remaining slightly less solid than an ordinary living soldier.

## Development controls

The `/echo_warrior visual` command can force visual states on the nearest owned echo:

```text
/echo_warrior visual blink
/echo_warrior visual double_blink
/echo_warrior visual curious
/echo_warrior visual startled
/echo_warrior visual reset
/echo_warrior visual status
```

`status` reports the observing player's current head-gaze sample, acquisition progress, required ticks, combat suppression, mutual-gaze state, reaction, and body yaw. These commands are testing tools, not player-facing gameplay.

## Asset pipeline

Run the updater after changing the source model:

```text
python scripts/update_roman_visual_assets.py update
python scripts/update_roman_visual_assets.py validate
```

The updater removes obsolete eyelid geometry, reserves the existing eyebrow bone for code-driven blinking, updates GeckoLib runtime assets, and writes a model-artist handoff copy under `outputs/`.
