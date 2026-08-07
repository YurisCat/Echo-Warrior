# Changelog

All notable development changes to Echo Warrior are recorded here.

## Unreleased

### Planned

- Replace instant player-gaze detection with the approved head-target acquisition and mutual-gaze state flow documented in `docs/VISUAL_BEHAVIOR.md`.
- Reconcile SmartBrainLib look behaviour, body facing, and client bone transforms so an echo does not appear to look away from a watching player.

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
