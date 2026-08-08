# Changelog

All notable development changes to Echo Warrior are recorded here.

## Unreleased

### Planned

- Continue manual playtesting and tune the head-target radius, body-turn speed, and animation responsiveness if required.

### Added

- Head-centred player-gaze acquisition with distance-scaled timing, two-tick mouse tolerance, line-of-sight validation, multiplayer owner priority, and close-range handling for invisible players.
- A mutual-gaze state with randomized hold and renewal durations, occasional glance-away breaks, last-seen-position persistence, combat suppression, and threat interruption.
- Independent pupil, head, and body attention layers with explicit threat priorities and minimum target-hold windows.
- Modeler-authored full-body attack and hurt animations, plus imported shield raise/lower previews.
- Gamemaster-only `/echo_warrior animation` commands for attack, hurt, shield raise, shield lower, and reset previews.
- A repeatable `.bbmodel` import path that normalizes animation and bone names, protects code-owned face bones, extracts the embedded texture, and validates geometry compatibility.
- An owner-only caught-watching reaction with delayed guaranteed activation during sustained mutual gaze, a restrained startle, rapid double blink, eyes-first glance-away, head follow-through, and a final covert look back at the owner.
- Three post-reaction exit behaviours: continued observation, a stationary patrol turn, or a short path-validated walk-away, each with an optional one-time profile glance back at the owner.

### Changed

- Mutual gaze now moves pupils first, then the head, and gently aligns the body only when the player lies well outside the forward cone.
- Removed SmartBrainLib's generic always-running look behaviour so it no longer competes with the code-owned visual-attention layer.
- Moved mutual-gaze body correction to the end of the entity tick so vanilla body-rotation control cannot overwrite it.
- Player-to-head visibility now uses a direct block ray to the model's measured head centre instead of the living-entity line-of-sight helper.
- Added `/echo_warrior visual status` to expose gaze acquisition and suppression state during playtesting.
- Mutual-gaze hold time now begins after the echo finishes facing the player, so a rear-facing turn cannot consume most of the visible eye-contact duration.
- Owner-follow navigation pauses during mutual gaze, and body alignment now converges within five degrees instead of relying on a visibly offset head correction.
- Pupil tracking now recalculates the target in head-local space every rendered frame, with larger safe travel, faster eye-leading response, and roll compensation during curious head tilts.
- Corrected the Blockbench head-bone yaw and pitch mapping so left/right and up/down tracking match the target while preserving the intentional roll-based curious tilt.
- Primed creepers and recent damage sources now override visual attention immediately; nearby unprimed creepers lead with the pupils, delay the head slightly, and only turn the non-combat body under close or rapidly approaching conditions.
- Nearby unprimed creepers now pause mutual gaze for up to one second instead of causing alternating-frame target flicker; a cleared distraction resumes player eye contact, while a persistent one ends it.
- `/echo_warrior visual status` now reports distraction state and the active eye, head, and body attention categories.
- Corrected horizontal pupil translation so the eyes lead toward the target instead of briefly moving in the mirrored direction.
- Updated the Roman legionary idle, walk, texture, and corrected bone pivots from the August 8 modeler delivery.
- SBL melee attacks now trigger the one-second attack animation while retaining the existing sixth-tick damage timing.
- Non-attacking echoes play the half-second full-body hurt animation; attacks retain body priority while code-driven pupil contraction, source tracking, and a short pain blink still communicate damage.
- Stabilized code-driven gaze against subtle modeler-authored idle torso rotation, while preserving larger full-body action motion; idle/walk changes now use a short stop delay and transition to filter navigation micro-movement.
- Ordinary attention now holds a living target or random world-space glance for its full intended duration instead of restarting every awareness scan or treating every anonymous point as the same target.
- Owner following now begins beyond fifteen blocks, settles within five, and reserves safe teleport recovery for distances beyond thirty-two blocks; visual-interaction navigation temporarily owns movement without competing with SBL follow paths.
- Rebalanced caught-watching exits to favour a staged short walk-away, require exit targets to differ clearly from the owner's direction, raise the one-time covert-glance chance, and suppress immediate owner reacquisition for 4-7 seconds after completion.

## 2026-08-07 - Legionary visual prototype baseline

### Added

- Code-driven shared visual attention for the Roman legionary echo.
- Head tracking, pupil tracking and contraction, randomized blinking using the existing eyebrow bone, curious head tilts, hurt and startled reactions, and an entity shadow.
- Visual debug commands for blink, double blink, curious, startled, and reset states.
- Dedicated SmartBrainLib owner-follow behaviour with walking, stopping, and safe teleport recovery.
- One-step `CATTEST` playtest launchers and model-artist preview documentation.
- Runtime GeckoLib model and animation assets plus an authoritative Blockbench source and modeler handoff copy.

### Changed

- Summoned and recalled legionaries now initially face their owner.
- The model source was restored to its original geometry and bone hierarchy. The old idle eyebrow blink track was removed because blinking is now code-owned; no eyelid geometry is present.
- Runtime worlds and temporary delivery packages remain excluded from Git.

### Known issue

- The initial mutual-gaze implementation reacts immediately and can visually oppose the player because normal gaze does not align the body while other AI systems can still change its base facing. The approved replacement is listed under Unreleased.
