# Changelog

All notable development changes to Echo Warrior are recorded here.

## Unreleased

### Planned

- Continue manual playtesting and tune the head-target radius, body-turn speed, and animation responsiveness if required.

### Added

- Head-centred player-gaze acquisition with distance-scaled timing, two-tick mouse tolerance, line-of-sight validation, multiplayer owner priority, and close-range handling for invisible players.
- A mutual-gaze state with randomized hold and renewal durations, occasional glance-away breaks, last-seen-position persistence, combat suppression, and threat interruption.

### Changed

- Mutual gaze now moves pupils first, then the head, and gently aligns the body only when the player lies well outside the forward cone.
- Removed SmartBrainLib's generic always-running look behaviour so it no longer competes with the code-owned visual-attention layer.
- Moved mutual-gaze body correction to the end of the entity tick so vanilla body-rotation control cannot overwrite it.
- Player-to-head visibility now uses a direct block ray to the model's measured head centre instead of the living-entity line-of-sight helper.
- Added `/echo_warrior visual status` to expose gaze acquisition and suppression state during playtesting.

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
