"""Prepare (never launch) a clean production-JAR client inside build/.

Only test-tool dependency: pip install --target build/compatibility-1.20.1-tools
 minecraft-launcher-lib==7.1. No account credentials, global launcher profiles,
 user saves or Gradle remapped/named game jars are used. The real launch and
 process/mouse/exit guards remain in run-test-client.ps1 -Production.
"""
from __future__ import annotations

import argparse
from datetime import datetime, timezone
import hashlib
import importlib.util
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import time
import urllib.request

ROOT = Path(__file__).resolve().parents[1]
TEST_ROOT = ROOT / "build" / "compatibility-1.20.1-production-client"
TOOLS = ROOT / "build" / "compatibility-1.20.1-tools"
sys.path.insert(0, str(TOOLS))
import minecraft_launcher_lib.command as launch_command
import minecraft_launcher_lib.install as install

spec = importlib.util.spec_from_file_location("server_smoke", ROOT / "scripts/smoke-test-1.20.1-servers.py")
server = importlib.util.module_from_spec(spec)
spec.loader.exec_module(server)

CACHE = Path(os.environ.get("GRADLE_USER_HOME", str(Path.home() / ".gradle"))) / "caches"
CACHED_JARS: dict[str, list[Path]] = {}
for cached in (CACHE / "modules-2/files-2.1").rglob("*.jar"):
    CACHED_JARS.setdefault(cached.name, []).append(cached)


def download_file(url, filename, callback, sha1=None, **kwargs):
    """Use the system TLS/proxy stack; never disable certificate or hash checks."""
    target = Path(filename)
    if target.is_file() and (not sha1 or hashlib.sha1(target.read_bytes()).hexdigest() == sha1):
        return True
    target.parent.mkdir(parents=True, exist_ok=True)
    candidates = CACHED_JARS.get(target.name, [])
    if target.name == "1.20.1.jar":
        candidates = [CACHE / "fabric-loom/1.20.1/minecraft-client.jar"]
    if sha1:
        for candidate in candidates:
            if candidate.is_file() and hashlib.sha1(candidate.read_bytes()).hexdigest() == sha1:
                shutil.copy2(candidate, target)
                return True
    if not url:
        raise RuntimeError(f"Missing installer-generated artifact: {target}")
    for attempt in range(3):
        try:
            with urllib.request.urlopen(url, timeout=60) as response:
                data = response.read()
            if sha1 and hashlib.sha1(data).hexdigest() != sha1:
                raise RuntimeError(f"Download hash mismatch: {url}")
            target.write_bytes(data)
            return True
        except (OSError, TimeoutError):
            if attempt == 2:
                raise
            time.sleep(1)


install.download_file = download_file


def json_url(url: str, destination: Path) -> dict:
    if not destination.is_file():
        destination.parent.mkdir(parents=True, exist_ok=True)
        with urllib.request.urlopen(url, timeout=60) as response:
            data = json.load(response)
        destination.write_text(json.dumps(data, indent=2), encoding="utf-8")
    return json.loads(destination.read_text(encoding="utf-8"))


def prepare_runtime(loader: str, values: dict, java: str) -> tuple[Path, str, Path]:
    runtime = TEST_ROOT / "runtime"
    vanilla_file = runtime / "versions/1.20.1/1.20.1.json"
    manifest = json_url("https://piston-meta.mojang.com/mc/game/version_manifest_v2.json", runtime / "manifest.json")
    version = next(entry for entry in manifest["versions"] if entry["id"] == "1.20.1")
    vanilla = json_url(version["url"], vanilla_file)
    if hashlib.sha1(vanilla_file.read_bytes()).hexdigest() != version["sha1"]:
        # json_url serializes JSON, so compare the original download for the pinned Mojang hash.
        if not download_file(version["url"], str(vanilla_file), {}, sha1=version["sha1"], overwrite=True):
            raise RuntimeError("Could not verify vanilla version metadata")
        vanilla = json.loads(vanilla_file.read_text(encoding="utf-8"))
    callback = {"setStatus": lambda status: print(status, flush=True)}
    install.install_libraries("1.20.1", vanilla["libraries"], str(runtime), callback, max_workers=4)
    client = vanilla["downloads"]["client"]
    download_file(client["url"], str(vanilla_file.with_suffix(".jar")), {}, sha1=client["sha1"])

    # Assets are immutable official content. Reuse only a complete, hash-verified cache;
    # otherwise let the installer fetch its own copy under this test runtime.
    assets = Path(os.environ.get("GRADLE_USER_HOME", str(Path.home() / ".gradle"))) / "caches/neoformruntime/assets"
    index = assets / "indexes" / (vanilla["assets"] + ".json")
    complete = index.is_file() and hashlib.sha1(index.read_bytes()).hexdigest() == vanilla["assetIndex"]["sha1"]
    if complete:
        for entry in json.loads(index.read_text())["objects"].values():
            path = assets / "objects" / entry["hash"][:2] / entry["hash"]
            if not path.is_file() or hashlib.sha1(path.read_bytes()).hexdigest() != entry["hash"]:
                complete = False
                break
    if not complete:
        install.install_assets(vanilla, str(runtime), callback, max_workers=4)
        assets = runtime / "assets"

    if loader == "fabric":
        version_id = f"fabric-loader-{values['loader_version']}-1.20.1"
        profile = json_url(f"https://meta.fabricmc.net/v2/versions/loader/1.20.1/{values['loader_version']}/profile/json",
                           runtime / "versions" / version_id / (version_id + ".json"))
        install.install_libraries(version_id, profile["libraries"], str(runtime), callback, max_workers=4)
        # The command builder's inherited profile id is also its vanilla jar path.
        # Keep this an exact official client jar, never a Gradle merged/remapped one.
        download_file(client["url"], str(runtime / "versions" / version_id / (version_id + ".jar")),
                      {}, sha1=client["sha1"])
    else:
        version_id = f"1.20.1-forge-{values['forge_version']}"
        stamp = runtime / (version_id + ".installed")
        if not stamp.is_file():
            installer = server.download(
                f"https://maven.minecraftforge.net/net/minecraftforge/forge/1.20.1-{values['forge_version']}/forge-1.20.1-{values['forge_version']}-installer.jar",
                f"forge-1.20.1-{values['forge_version']}-installer.jar")
            profiles = runtime / "launcher_profiles.json"
            if not profiles.exists():
                profiles.write_text('{"profiles": {}}', encoding="utf-8")
            with (runtime / "forge-client-installer.log").open("w", encoding="utf-8") as output:
                result = subprocess.run([java, "-jar", str(installer), "--installClient", str(runtime)],
                                        cwd=runtime, stdout=output, stderr=subprocess.STDOUT,
                                        creationflags=server.NO_WINDOW, timeout=900)
            if result.returncode:
                raise RuntimeError(f"Forge client installation failed: {runtime / 'forge-client-installer.log'}")
            stamp.write_text("official client installer completed\n", encoding="utf-8")
    return runtime, version_id, assets


def quote_java_argument(argument: str) -> str:
    if "\n" in argument or "\r" in argument:
        raise ValueError("Newlines are not allowed in Java argument files")
    return '"' + argument.replace("\\", "\\\\").replace('"', '\\"') + '"'


def stage_profile_overrides(profile: Path | None, run: Path) -> dict[str, str]:
    """Copy explicit pack configuration/data, recording the exact pre-launch inputs."""
    if profile is None:
        return {}
    profile = profile.resolve(strict=True)
    hashes = {}
    for folder in ("config", "defaultconfigs", "datapacks"):
        source = profile / folder
        if not source.exists():
            continue
        if source.is_symlink() or not source.is_dir():
            raise ValueError(f"Expected an ordinary profile directory: {source}")
        for path in source.rglob("*"):
            if path.is_symlink() or not path.resolve().is_relative_to(profile):
                raise ValueError(f"Profile input escapes its directory: {path}")
            if path.is_file():
                hashes[path.relative_to(profile).as_posix()] = hashlib.sha256(path.read_bytes()).hexdigest()
        shutil.copytree(source, run / folder)
    return hashes


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--loader", choices=("fabric", "forge"), required=True)
    parser.add_argument("--forge-version", help="Isolated production-client loader override")
    args = parser.parse_args()
    values, java = server.config_values(), server.java17()
    if args.forge_version:
        if args.loader != "forge" or not re.fullmatch(r"\d+\.\d+\.\d+", args.forge_version):
            parser.error("--forge-version requires --loader forge and a numeric major.minor.patch")
        values["forge_version"] = args.forge_version
    runtime, version, assets = prepare_runtime(args.loader, values, java)
    run = TEST_ROOT / (datetime.now(timezone.utc).strftime("%Y%m%dT%H%M%SZ") + f"-{os.getpid()}") / args.loader
    run.mkdir(parents=True, exist_ok=False)
    artifacts = server.stage_mods(args.loader, run, values)
    profile_path = os.environ.get("ECHO_WARRIOR_TEST_PROFILE_OVERRIDES")
    profile_files = stage_profile_overrides(Path(profile_path) if profile_path else None, run)
    heap_mib = int(os.environ.get("ECHO_WARRIOR_TEST_CLIENT_HEAP_MIB", "3072"))
    if not 1024 <= heap_mib <= 16384:
        raise ValueError("Test client heap must be between 1024 and 16384 MiB")
    for report_path in sorted(server.TEST_ROOT.glob("*/report.json"), reverse=True):
        report = json.loads(report_path.read_text())
        source = report_path.parent / args.loader / "bootstrap-test-world"
        log = report_path.parent / args.loader / "console-2.log"
        if (report.get("results", {}).get(args.loader, {}).get("passed") and source.is_dir()
                and log.is_file() and "All dimensions are saved" in log.read_text(encoding="utf-8", errors="replace")):
            shutil.copytree(source, run / "saves/CATTEST", ignore=shutil.ignore_patterns("session.lock"))
            break
    else:
        raise RuntimeError("A normally stopped production server world is required")
    (run / "options.txt").write_text("version:3465\nonboardAccessibility:false\nlang:zh_cn\nsoundCategory_master:0.0\n", encoding="utf-8")
    command = launch_command.get_minecraft_command(version, runtime, {
        "username": "Echo1201" + args.loader.title(), "uuid": "00000000000000000000000000001201", "token": "0",
        "executablePath": java, "gameDirectory": str(run), "quickPlaySingleplayer": "CATTEST",
        "jvmArguments": ["-Xms512M", f"-Xmx{heap_mib}M", "-Decho_warrior.auto_pause_after_quick_play=true"],
        "launcherName": "EchoWarrior-local-production-test", "launcherVersion": "1",
    })
    command[command.index("--assetsDir") + 1] = str(assets)
    # Match the development launch's local test account; do not request MSA profile keys.
    command[command.index("--userType") + 1] = "legacy"
    for parameter in ("--clientId", "--xuid"):
        if parameter in command:
            command[command.index(parameter) + 1] = "0"
    if "--quickPlaySingleplayer" not in command:
        raise RuntimeError("Production launch is missing safe quick-play")
    if any("userdev" in arg or "devlaunchinjector" in arg or "remapped_mods" in arg for arg in command):
        raise RuntimeError("A development classpath leaked into the production launch")
    arguments = run / "client-arguments.txt"
    arguments.write_text("\n".join(quote_java_argument(arg) for arg in command[1:]), encoding="utf-8")
    descriptor = {"java": java, "argument_file": str(arguments), "run_directory": str(run),
                  "loader": args.loader, "profile": version, "artifacts": artifacts,
                  "source_world_report": str(report_path), "production": True,
                  "profile_files": profile_files, "heap_mib": heap_mib}
    (run / "launch.json").write_text(json.dumps(descriptor, indent=2), encoding="utf-8")
    output = TEST_ROOT / ("latest-" + args.loader + "-launch.json")
    output.write_text(json.dumps(descriptor, indent=2), encoding="utf-8")
    print(f"Prepared production client: {output}", flush=True)


if __name__ == "__main__":
    main()
