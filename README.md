# Echo Warrior

Echo Warrior is a Minecraft 26.1.2 mod about recovering relics from ancient battlefields and summoning warrior echoes from the past. It ships as two separate packages: one for Fabric and one for NeoForge.

Version 0.1.1 includes the Echo Compass and renewable battlefield archaeology loop, a portable fuelled summoner, five playable Echo Warriors, relic growth and talents, 25 accessories, 40 collectible knowledge pages, the Echo Recycler, an in-game tutorial manual, and a web-first interactive encyclopedia. The stable technical mod ID is `echo_warrior`.

## Requirements

Both packages require Minecraft 26.1.2, Java 25, SmartBrainLib 2.0.0, and GeckoLib 5.5.2.

- Fabric package: Fabric Loader 0.19.3 and Fabric API 0.155.2+26.1.2.
- NeoForge package: NeoForge 26.1.2.100.

Install only the JAR matching the chosen loader. Client and server should use the same loader and matching Echo Warrior version. Dependency JARs remain separate and are not bundled inside Echo Warrior.

## License

Echo Warrior uses a mixed license:

- Original Java code and technical tooling are licensed under Apache License 2.0.
- Original models, textures, animations, shaders, audio, UI art, icons, writing, documentation, and promotional material are governed by the custom Echo Warrior Creative Assets License 1.0.
- Attributed fan works and commercial derivative works are broadly permitted. Direct or only lightly modified use of covered assets on a platform where Minecraft content can be sold or placed behind mandatory payment requires prior explicit written permission, even when the particular upload is free.
- Contributor and third-party material is licensed only to the extent its rightsholder has authorized.

See [`LICENSE`](LICENSE), [`LICENSE-CODE`](LICENSE-CODE), [`LICENSE-ASSETS.md`](LICENSE-ASSETS.md), and [`CREDITS.md`](CREDITS.md) for the complete terms and attribution requirements.

## Development

- Shared gameplay code and resources live in `common/`; loader adapters live in `fabric/` and `neoforge/`.
- Daily development remains Fabric-first: build with `gradlew.bat fabricBuild`.
- Double-click `tools\windows\Launch Test Client.bat` to compile Fabric and quick-play into the existing `CATTEST` world.
- Use `scripts\run-neoforge-test-client.ps1` or double-click `tools\windows\Launch NeoForge Test Client.bat` only when an explicit NeoForge check is needed. Its separate world is `CATTEST_NEOFORGE`.
- Build both packages with `gradlew.bat dualBuild`.
- Prepare the two JARs, shared checklist, and generated handoff document with `scripts\build-dual-candidate.ps1` or `tools\windows\Build Dual Package.bat`.
- The interactive encyclopedia lives in `encyclopedia/`; run it with `tools\windows\Start Local Encyclopedia.bat`.
- Runtime worlds under `run/` and `run-neoforge/`, plus `temporary-delivery/`, are intentionally not tracked by Git.

## Tester handoff

Testers receive exactly four files: both loader-specific JARs, the latest HTML test checklist, and a generated handoff document containing versions, dependencies, commit information, SHA-256 values, test priorities, and return requirements. They do not receive a source checkout, JDK, Gradle caches, test worlds, or development launchers.

## Model artist workflow

Model artists clone the same repository as developers and use the shared Windows Fabric test launcher under `tools/windows/`. No separate model-artist updater or force-update package is maintained.

See [`docs/MODELER_PREVIEW.md`](docs/MODELER_PREVIEW.md) before preparing a preview for another computer.
