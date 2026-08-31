# Echo Warrior

Echo Warrior is a Fabric mod for Minecraft 26.1.2 about recovering relics from ancient battlefields and summoning warrior echoes from the past.

The current development build includes the Echo Compass and battlefield archaeology loop, a portable fuelled summoner, five playable Echo Warriors, relic growth and talents, 25 accessories, 40 collectible knowledge pages, the Echo Recycler, an in-game tutorial manual, and a web-first interactive encyclopedia. The final public name and submission artwork are still undecided; the stable technical mod ID remains `echo_warrior`.

## Development

- Java 25 is stored locally in `.toolchains/jdk-25`.
- Build with `gradlew.bat build`.
- From Command Prompt or by double-clicking, start the development client with `scripts\\run-test-client.bat`; it compiles and quick-plays into `CATTEST` when that world exists.
- `scripts\\playtest-now.bat` requires the existing `CATTEST` world and is the shortest local playtest entry point.
- From PowerShell, use `./scripts/run-test-client.ps1` or `./scripts/playtest-now.ps1`.
- The interactive encyclopedia lives in `encyclopedia/`; run it with `启动本地百科.bat`.
- Runtime worlds under `run/` are intentionally not tracked by Git.

## Tester handoff

- The Chinese tester guide is `TESTER_GUIDE.html` in the project root.
- A self-contained Windows x64 tester package can include PortableGit, Java 25, `CATTEST`, and a project-local offline Gradle cache.
- Testers use `首次安装.bat` once, `强制更新.bat` for destructive source synchronization, and `启动测试.bat` to launch the preserved local test environment.

## Model artist handoff

- `MODELER_GUIDE.html` documents the separate portable model-artist toolkit.
- The model-artist ZIP carries PortableGit and update/launch helpers only; it intentionally excludes the repository, Java, Gradle caches, dependencies, and test worlds.
- `安全更新项目.bat` refuses to update over local work. `强制覆盖更新（自动备份）.bat` stashes uncommitted work and preserves displaced local commits before resetting to remote `main`.
- Both update BAT files use the process-local proxy `http://127.0.0.1:7897` for GitHub access without changing system or global Git proxy settings.
- Keep the extracted toolkit outside the Git repository. It remembers the selected existing project directory after the first run.

The runtime requires Fabric API, SmartBrainLib 2.0.0, and GeckoLib 5.5.2.

See [`docs/MODELER_PREVIEW.md`](docs/MODELER_PREVIEW.md) before preparing a preview for another computer.
