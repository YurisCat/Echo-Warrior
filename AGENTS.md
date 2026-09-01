# Codex project instructions

## Project root

- Treat this directory as the only project root for the ModJam project.
- Do not use or recreate any former ModJam workspace outside this repository root.

## Source of truth

- Read `PROJECT.md` before making architectural or gameplay decisions.
- Keep `PROJECT.md` updated when scope, tooling, testing workflow, or core design changes materially.
- Do not silently promote post-MVP ideas into required work.

## Build and test workflow

- Use the repository Gradle Wrapper (`gradlew.bat`) instead of a globally installed Gradle.
- The target development JDK is Java 25; do not build the 26.1 project with the machine's current Java 17 runtime.
- When the user asks to test or launch Minecraft, use `scripts/run-test-client.ps1` once it exists.
- The test launcher should compile, launch the development client, and quick-play into `CATTEST`.
- Avoid launching a second development client when one is already running.
- Inspect `run/logs/latest.log` and crash reports after failures.
- Prefer automated checks and GameTests for deterministic behavior; reserve manual testing for visuals, sound, controls, and game feel.

## Reload policy

- Prefer resource reloads for assets and data reloads for data-driven content.
- Attempt debugger HotSwap only when the change is compatible with it.
- Restart the client for registry, class-structure, entity-type, major Mixin, or unsafe state changes.
- State clearly when a new world or new chunks are required for world-generation testing.

## Scope control

- Complete the three-hero MVP before random relic affixes, automatic summoning, lethal-damage protection, damage sharing, or additional heroes.
- Treat performance, multiplayer correctness, save compatibility, and clear player feedback as part of feature completion.

## Assets and contest compliance

- Do not generate AI project avatars or CurseForge gallery artwork for the submission.
- Preserve source files for manually created models, textures, audio, and promotional material.

## Interactive encyclopedia

- The web-first interactive encyclopedia lives in `encyclopedia/`.
- Treat `encyclopedia/content/atlas.zh_cn.json` as the current source of truth for encyclopedia categories, node positions, relationships, and Chinese articles.
- Player-visible gameplay, controls, balance values, or status changes must update the relevant encyclopedia entry in the same task, or explicitly record why no encyclopedia change is needed.
- Keep the encyclopedia content schema renderer-neutral so a later Minecraft-native screen can consume generated data without embedding a web browser.
- The in-game encyclopedia is deferred work; do not add a new runtime dependency or promote it into the contest MVP without an explicit decision.
