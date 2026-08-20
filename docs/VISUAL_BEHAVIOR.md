# Echo Warrior Visual Behaviour Contract

## Purpose

The Roman legionary and Aztec warrior echoes use the same code-driven visual-attention layer on top of GeckoLib locomotion animations. This layer is cosmetic: it must not change navigation, combat targeting, damage, or summoner binding. Their timing, probability, priority, and interruption rules are intentionally identical; only model-specific bone names and torso ancestry differ.

## Code-owned bones

The following Roman legionary bones are reserved and controlled by code in ordinary gameplay:

- `head`
- `left_eye`
- `right_eye`
- `eyebrows`

The corresponding Aztec warrior mappings are:

- `Head`
- `Eyes_Left`
- `Eyes_Right`
- `Eyebrow`

The renderer also compensates the Roman `root`/torso ancestry and the Aztec `Main` → `Body` → `Upper_Body2` ancestry independently. Model-specific axis signs and ancestor compensation belong in each renderer; gameplay attention data remains shared in behaviour.

Animation files must not introduce new face keyframes that fight the runtime layer unless the project owner explicitly approves a future exception. The authoritative Blockbench sources are:

```text
assets-source/blockbench/roman_legionary.bbmodel
assets-source/blockbench/aztec_warrior_echo.bbmodel
```

Model artists must continue from the latest matching source file rather than an older local branch.

## Attention model

The server maintains three related but independent attention layers so every client sees the same intent without forcing the entire body to snap toward every interesting object:

- **Pupils:** react first and remain locked to the selected world-space target.
- **Head:** follows the pupil target after the reaction-specific delay, clamped to roughly 75 degrees left or right while stationary; ordinary locomotion uses the narrower rule below.
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

Ordinary attention retains its selected living entity or anonymous world-space glance point for the candidate's full 1.5-4 second duration. Re-scanning the same living entity updates its eye position without restarting the attention episode, while newly generated anonymous points are not treated as the same target merely because both lack an entity. This prevents the ordinary two-tick awareness scan from repeatedly restarting or redirecting the head.

During ordinary walking, following, wandering, returning to an activity anchor, or swimming, locomotion temporarily replaces the owner as the baseline attention target. The synthetic attention point stays approximately six blocks ahead along current horizontal movement, falling back to body facing while a path has begun but movement is still accelerating. Head yaw is limited to roughly 15 degrees and residual pupil yaw to roughly 10 degrees, so the echo looks where it is going instead of continuously walking sideways while staring at the owner. This locomotion attention refreshes in short episodes and releases within roughly 0.3 seconds after movement ends. Combat targets, creepers, actual damage sources, rapidly approaching entities, mutual gaze, and the dedicated caught-watching exit presentation retain their existing higher priority and may override the locomotion limit.

Layer-specific threat responses:

- A primed creeper moves the pupils and head immediately. Outside combat, the body turns when the creeper is within six blocks, or after 0.3 seconds if the target remains behind the head's useful viewing arc.
- An actual damage source moves the pupils and head immediately. Outside combat, the body begins turning after 0.15 seconds.
- An unprimed creeper within eight blocks moves the pupils immediately and the head after 0.2 seconds. The body only turns if it enters four blocks or approaches rapidly.
- A rapidly approaching living entity moves the pupils immediately and the head after roughly 0.1 seconds. Outside combat, the body only turns if its current motion predicts arrival within three blocks in roughly half a second.
- During active combat, the eyes and head may acknowledge a more urgent visual target, but body orientation remains owned by combat AI.

Pupil position is recalculated every rendered frame from the attention point in the head's current local coordinate space. The eyes therefore remain locked to the same world-space target while the head and body move. As the face becomes aligned, the pupils naturally approach the centre because the target itself has moved to the centre of the local view; this is not treated as releasing attention. Curious head roll is compensated so a tilted head does not drag the pupils away from their target.

The current Blockbench `head`/`Head` bones use yaw and pitch axes opposite to Minecraft's semantic look angles. Each renderer negates yaw and pitch only at the final bone-application boundary; target selection, pupil tracking, and body-facing math remain in normal Minecraft coordinates. The roll axis is not inverted, preserving the occasional curious head tilt.

The modeler's idle animation may retain subtle motion on torso ancestors of `head`. The renderer reads the final animated `root`/torso chain and compensates inherited rotations up to roughly three degrees so code-driven gaze does not oscillate with breathing motion. Compensation fades out between roughly three and eight degrees, preserving larger attack, hurt, and shield motions. Locomotion also holds the walk state for four ticks after movement ceases and blends movement changes over three ticks, filtering navigation micro-movement without delaying the start of walking.

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

### Owner catches the echo watching

The owner-only caught-watching reaction is a restrained moment of being found out, not fear or overt shyness. It is eligible only when both pupils and head have already watched the bound owner for at least 0.4 seconds before that owner completes the normal 0.5-second head-gaze acquisition. A player who looks first and causes the echo to turn toward them receives ordinary mutual gaze instead.

Once eligible, continuous owner gaze always produces the reaction, but its delay is selected per episode:

- 30% immediately after acquisition.
- 45% after another 1-2 seconds.
- 20% after another 2-4 seconds.
- 5% after another 4-6 seconds.

Two ordinary missed gaze ticks are tolerated while waiting; a longer miss cancels the pending reaction. The reaction occurs at most once per continuous mutual-gaze episode and, once started, establishes an 8-15 second random cooldown whether it completes or is interrupted.

The 1.6-2.2 second presentation is code-driven:

1. Freeze on the owner for roughly 0.12-0.18 seconds with a mild pupil contraction to about 80%.
2. Perform a fast double blink over roughly 0.35 seconds.
3. Move the pupils first toward a randomly selected point 35-55 degrees to either upper side of the owner; the head follows roughly 0.1 seconds later with a small upward pitch while the body stays put.
4. Hold the false distant focus briefly, then move only the pupils back to the owner's live eye position for a 0.2-0.35 second covert glance.
5. If the owner is still staring, snap the pupils back to the distant point more quickly; otherwise allow a slightly longer glance before returning to ordinary attention.

Combat, actual damage, a creeper, a rapidly approaching living entity, lost visibility, or the owner pulling beyond roughly 16 blocks interrupts the presentation immediately. Ordinary harmless bystanders do not. The system never starts this presentation for non-owner players and does not add a separate full-body animation or override combat movement.

After the main caught-watching presentation, the echo enters a 2-4 second owner-exclusion exit instead of immediately selecting the owner again:

- 15% continue watching another target without turning the body.
- 25% turn the body roughly 45-90 degrees and pretend to inspect the surroundings.
- 60% turn away and casually walk 1.5-3 blocks at roughly 65-75% of normal follow speed.

Every exit focus must be at least 70 degrees away from the direction to the owner. A harmless living target that lies too close to the owner's direction is rejected in favour of another target or a generated side point. Walking chooses either side at a random 70-130 degree angle from the owner, and stages the motion visibly: pupils move first, the head follows, the body turns for 0.25-0.5 seconds, and navigation begins only afterwards. The echo does not automatically turn back on arrival.

Walking is attempted only while the echo begins within nine blocks of the owner and the destination remains within twelve blocks; path failure falls back to the stationary turn. The exit is cancelled when the owner reaches fifteen blocks so normal following can resume. Owner following itself starts beyond fifteen blocks, stops within five, and uses safe teleport recovery only beyond thirty-two blocks.

Each exit has a 75% chance to perform one secondary profile glance after committing to the chosen focus; a walking exit must at least begin moving before this glance becomes eligible. Pupils move to the owner's live eye position first; the head follows by only 8-15 degrees roughly 0.1 seconds later while the body remains committed to the exit direction and walking never pauses. If the owner is still staring, the glance lasts 0.15-0.25 seconds and snaps away; otherwise it may last 0.35-0.55 seconds. The echo continues or completes its exit without turning back, and no exit can produce a third glance or a loop.

After a completed exit, ordinary owner observation and new mutual-gaze acquisition are suppressed for another random 4-7 seconds. The last away focus is retained and regenerated to remain at least 70 degrees from the owner if necessary. Following, protection and combat remain functional. Active combat, actual damage, creeper alarm, or a rapidly approaching living entity cancels this visual avoidance immediately.

## Reactions

- Normal blink interval: randomized between roughly 2.5 and 6 seconds.
- Occasional double blink: approximately 10% probability.
- Startled reactions briefly suppress ordinary blinking.
- Hurt pupils contract to roughly 60%, then recover quickly.
- Hurt triggers a code-owned pain blink: the eyebrows close the eyes at roughly 0.08 seconds and reopen them by roughly 0.3 seconds. This eye response still plays when a body attack animation prevents the full-body hurt animation from taking over.
- Strong surprise contracts pupils to roughly 45-50%.
- Blinking is performed by moving the Roman `eyebrows` or Aztec `Eyebrow` bone down by two model units; no eyelid geometry is used.
- Safe idle observation may trigger an 8-12 degree curious head tilt.
- Different echoes use independent timing to prevent synchronized gestures.

## Shadow

The entity uses a 0.45-block shadow radius at approximately 70% strength. It should feel grounded while remaining slightly less solid than an ordinary living soldier.

## Development controls

The `/echo_warrior visual` command currently forces visual states on the nearest owned Roman legionary. The Aztec warrior uses the same runtime state machine, but command selection has not yet been generalized:

```text
/echo_warrior visual blink
/echo_warrior visual double_blink
/echo_warrior visual curious
/echo_warrior visual startled
/echo_warrior visual exit_look
/echo_warrior visual exit_turn
/echo_warrior visual exit_walk
/echo_warrior visual exit_secondary
/echo_warrior visual reset
/echo_warrior visual status
```

The four `exit_*` commands deterministically preview the three post-reaction branches and the stationary turn with its optional secondary profile glance. They bypass natural branch probability but retain path validation and normal interruption rules.

`status` reports the observing player's current head-gaze sample, acquisition progress, required ticks, combat suppression, mutual-gaze, caught-reaction and exit state, remaining post-exit owner-avoidance ticks, current eye/head/body attention kinds, reaction, and body yaw. These commands are testing tools, not player-facing gameplay.

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

For the Aztec warrior model and texture, use:

```text
python scripts/update_aztec_visual_assets.py update
python scripts/update_aztec_visual_assets.py validate
```

Import mode accepts a complete modeler delivery, preserves the project's canonical UUID-to-bone naming, normalizes animation names, strips code-owned face keyframes, updates compatible GeckoLib pivots and animations, extracts the embedded runtime texture, and writes a model-artist handoff copy under `outputs/`. It deliberately stops if cube geometry, UVs, or UUIDs changed in a way that requires a reviewed full geometry export.
