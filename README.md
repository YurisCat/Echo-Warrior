# Echo Warrior

Fabric mod prototype for Minecraft 26.1.2. The first playable slice adds an animated test summoner and a Roman legionary echo that follows and protects its owner.

> **Temporary collaborator preview:** The current Roman legionary, test summoner, balance values, animations, and test scene are development placeholders shared for rapid review with the model artist. They are not final submission content or final art direction.

## Development

- Java 25 is stored locally in `.toolchains/jdk-25`.
- Build with `gradlew.bat build`.
- From Command Prompt or by double-clicking, start the development client with `scripts\\run-test-client.bat`.
- For the current local playtest scene, use `scripts\\playtest-now.bat`; it compiles, launches Minecraft, and quick-plays into `CATTEST`.
- From PowerShell, either launcher works: `./scripts/run-test-client.ps1` or `./scripts/run-test-client.bat`.
- Runtime worlds under `run/` are intentionally not tracked by Git.

The runtime requires Fabric API, SmartBrainLib 2.0.0, and GeckoLib 5.5.2.

See [`docs/MODELER_PREVIEW.md`](docs/MODELER_PREVIEW.md) before preparing a preview for another computer.
