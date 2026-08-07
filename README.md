# Echo Warrior

Fabric mod prototype for Minecraft 26.1.2. The first playable slice adds an animated test summoner and a Roman legionary echo that follows and protects its owner.

## Development

- Java 25 is stored locally in `.toolchains/jdk-25`.
- Build with `gradlew.bat build`.
- From Command Prompt or by double-clicking, start the development client with `scripts\\run-test-client.bat`.
- From PowerShell, either launcher works: `./scripts/run-test-client.ps1` or `./scripts/run-test-client.bat`.
- Create a test world named `CFMJ-Test-World` on the first launch. Later launches quick-play directly into it.

The runtime requires Fabric API, SmartBrainLib 2.0.0, and GeckoLib 5.5.2.
