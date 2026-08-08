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

The server maintains three related but independent attention layers so every client sees the same intent without forcing the entire body to snap toward every interesting object:

- **Pupils:** react first and remain locked to the selected world-space target.
- **Head:** follows the pupil target after the reaction-specific delay, clamped to roughly 75 degrees left or right.
- **Body:** turns only for mutual gaze or a sufficiently urgent threat. Active combat movement and facing always take precedence over cosmetic body attention.

Visual priority, from highest to lowest:

1. Primed or ignited creeper.
2. Actual recent damage source.
3. Unprimed creeper within eight blocks.
4. Current combat target.
5. A player in the mutual-gaze flow defined below.
6. A living entity rapidly approaching the echo.
7. Owner, players, nearby living entities, or a quiet random point.

The pupils hold a selected target for at least 0.25 seconds and the head for at least 0.5 seconds. A primed creeper or actual damage source may override either hold immediately. This prevents ordinary candidates from causing flicker while retaining an immediate startle response.

Layer-specific threat responses:

- A primed creeper moves the pupils and head immediately. Outside combat, the body turns when the creeper is within six blocks, or after 0.3 seconds if the target remains behind the head's useful viewing arc.
- An actual damage source moves the pupils and head immediately. Outside combat, the body begins turning after 0.15 seconds.
- An unprimed creeper within eight blocks moves the pupils immediately and the head after 0.2 seconds. The body only turns if it enters four blocks or approaches rapidly.
- A rapidly approaching living entity moves the pupils immediately and the head after roughly 0.1 seconds. Outside combat, the body only turns if its current motion predicts arrival within three blocks in roughly half a second.
- During active combat, the eyes and head may acknowledge a more urgent visual target, but body orientation remains owned by combat AI.

Pupil position is recalculated every rendered frame from the attention point in the head's current local coordinate space. The eyes therefore remain locked to the same world-space target while the head and body move. As the face becomes aligned, the pupils naturally approach the centre because the target itself has moved to the centre of the local view; this is not treated as releasing attention. Curious head roll is compensated so a tilted head does not drag the pupils away from their target.

The current Blockbench `head` bone uses yaw and pitch axes opposite to Minecraft's semantic look angles. The renderer negates yaw and pitch only at the final bone-application boundary; target selection, pupil tracking, and body-facing math remain in normal Minecraft coordinates. The roll axis is not inverted, preserving the occasional curious head tilt.

## Mutual gaze contract

The following behaviour was approved on 2026-08-07 and is the implementation contract for player-initiated eye contact:

- The player must look at a spherical virtual head target with a radius of approximately 0.35 blocks, centred on the echo's head. Front, side, and rear observation all count.
- The gaze must have an unobstructed line of sight. A blocked view clears acquisition immediately.
- Within 12 blocks, acquisition requires 10 continuous ticks (0.5 seconds).
- Beyond 12 blocks, required acquisition time is `0.5 + (distance - 12) / 40` seconds. There is no artificial range cap beyond normal entity loading and tracking limits.
- Up to two consecutive missed ticks are tolerated for ordinary mouse movement. A longer miss clears acquisition progress.
- Acquisition is suppressed while the echo has an attack target, or while the echo or its owner has dealt or received damage within the previous three seconds.
- On acquisition, the pupils lead, the head begins following roughly 0.1 seconds later, and the body begins a smooth turn roughly 0.2 seconds later. The body corrects to within roughly five degrees so the face visibly points at the player.
- Pupil travel is intentionally more visible than the original prototype, using up to roughly 0.82 model units horizontally and 0.46 vertically while remaining inside the two-by-two eye area.
- The 2-4 second first mutual-gaze timer begins only after facing alignment is complete; time spent turning does not consume the eye-contact duration. If the player is still looking when it ends, there is a 75% chance to renew for 1-3 seconds and a 25% chance to glance away for 0.5-1.5 seconds before reacquisition is allowed.
- A primed creeper, an actual damage source, or active combat interrupts mutual gaze immediately.
- An unprimed creeper within eight blocks temporarily steals pupil/head attention and pauses the mutual-gaze hold timer. If the distraction clears within one second, the echo resumes looking at the player; if it persists longer, the mutual-gaze episode ends.
- When line of sight is lost during an active episode, the echo watches the player's last visible position for 0.5 seconds before returning to ordinary observation.
- Any non-spectator player may trigger mutual gaze. When several players qualify, the owner has priority; otherwise the player with the longest valid gaze duration wins. Invisible players only qualify within four blocks.

Mutual gaze must not leave SmartBrainLib and the renderer fighting over different facing directions. The presentation layer owns eyes and head, while deliberate mutual gaze may request a gentle body-facing correction without changing navigation or combat targeting.
Ordinary owner-follow navigation pauses during an active mutual-gaze episode and resumes afterwards, preventing movement steering from immediately pulling the body away.

## Reactions

- Normal blink interval: randomized between roughly 2.5 and 6 seconds.
- Occasional double blink: approximately 10% probability.
- Startled reactions briefly suppress ordinary blinking.
- Hurt pupils contract to roughly 60%, then recover quickly.
- Hurt triggers a code-owned pain blink: the eyebrows close the eyes at roughly 0.08 seconds and reopen them by roughly 0.3 seconds. This eye response still plays when a body attack animation prevents the full-body hurt animation from taking over.
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

`status` reports the observing player's current head-gaze sample, acquisition progress, required ticks, combat suppression, mutual-gaze and distraction state, current eye/head/body attention kinds, reaction, and body yaw. These commands are testing tools, not player-facing gameplay.

Model animation previews use a separate command branch:

```text
/echo_warrior animation attack
/echo_warrior animation hurt
/echo_warrior animation shield_raise
/echo_warrior animation shield_lower
/echo_warrior animation reset
```

Previewed attack animations do not deal damage. Shield raise/lower remain development-only previews until the shield skill is designed.

## Asset pipeline

Run the updater after changing the source model:

```text
python scripts/update_roman_visual_assets.py import "path/to/modeler-delivery.bbmodel"
python scripts/update_roman_visual_assets.py update
python scripts/update_roman_visual_assets.py validate
```

Import mode accepts a complete modeler delivery, preserves the project's canonical UUID-to-bone naming, normalizes animation names, strips code-owned face keyframes, updates compatible GeckoLib pivots and animations, extracts the embedded runtime texture, and writes a model-artist handoff copy under `outputs/`. It deliberately stops if cube geometry, UVs, or UUIDs changed in a way that requires a reviewed full geometry export.
