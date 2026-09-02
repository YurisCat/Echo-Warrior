# Echo Warrior

Echo Warrior is a Fabric mod for Minecraft 26.1.2 about recovering relics from ancient battlefields and summoning warrior echoes from the past.

Version 0.1.0 is the first public release. It includes the Echo Compass and renewable battlefield archaeology loop, a portable fuelled summoner, five playable Echo Warriors, relic growth and talents, 25 accessories, 40 collectible knowledge pages, the Echo Recycler, an in-game tutorial manual, and a web-first interactive encyclopedia. The stable technical mod ID is `echo_warrior`.

## Requirements

- Minecraft 26.1.2
- Fabric Loader 0.19.3 or newer
- Fabric API 0.155.2+26.1.2 or newer compatible version
- SmartBrainLib 2.0.0 or newer compatible version
- GeckoLib 5.5.2 or newer compatible version
- Java 25

Install the mod and its required dependencies in the `mods` folder of a compatible Fabric client or server. Client and server should use matching mod versions for multiplayer.

## License

Echo Warrior uses a mixed license:

- Original Java code and technical tooling are licensed under Apache License 2.0.
- Original models, textures, animations, shaders, audio, UI art, icons, writing, documentation, and promotional material are governed by the custom Echo Warrior Creative Assets License 1.0.
- Attributed fan works and commercial derivative works are broadly permitted. Direct or only lightly modified use of covered assets on a platform where Minecraft content can be sold or placed behind mandatory payment requires prior explicit written permission, even when the particular upload is free.
- Contributor and third-party material is licensed only to the extent its rightsholder has authorized.

See [`LICENSE`](LICENSE), [`LICENSE-CODE`](LICENSE-CODE), [`LICENSE-ASSETS.md`](LICENSE-ASSETS.md), and [`CREDITS.md`](CREDITS.md) for the complete terms and attribution requirements.

## Development

- Java 25 is stored locally in `.toolchains/jdk-25`.
- Build with `gradlew.bat build`.
- From Command Prompt or by double-clicking, use `tools\\windows\\Launch Test Client.bat`; it compiles and quick-plays into the existing `CATTEST` world.
- From PowerShell, use `./scripts/run-test-client.ps1` or `./scripts/playtest-now.ps1`.
- The interactive encyclopedia lives in `encyclopedia/`; run it with `tools\\windows\\Start Local Encyclopedia.bat`.
- Runtime worlds under `run/` are intentionally not tracked by Git.

## Tester handoff

- Testers receive the current mod JAR, its SHA-256 checksum, and the latest HTML test checklist.
- Testers do not receive a source checkout, PortableGit, a JDK, Gradle caches, project update scripts, or development-client launchers.

## Model artist workflow

- Model artists clone the same repository as developers and use the shared Windows test launcher under `tools/windows/`.
- No separate model-artist updater, PortableGit bundle, or force-update package is maintained.

The runtime requires Fabric API, SmartBrainLib 2.0.0, and GeckoLib 5.5.2.

See [`docs/MODELER_PREVIEW.md`](docs/MODELER_PREVIEW.md) before preparing a preview for another computer.
