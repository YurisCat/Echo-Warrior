"""Prepare official production clients for isolated FOXY acceptance; never launch."""
from __future__ import annotations
import argparse
from datetime import datetime, timezone
import hashlib
import http.client
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import time
import urllib.request
import uuid

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "build/compatibility-1.20.1-tools"))
import minecraft_launcher_lib.command as launch
import minecraft_launcher_lib.install as install


def sha(path: Path, algorithm: str = "sha256") -> str:
    with path.open("rb") as stream:
        return hashlib.file_digest(stream, algorithm).hexdigest()


def quote(value: str) -> str:
    if "\n" in value or "\r" in value:
        raise ValueError("Newline in Java argument")
    return '"' + value.replace("\\", "\\\\").replace('"', '\\"') + '"'


def install_fabric_libraries(profile: dict, runtime: Path, downloader, checksum_reader) -> None:
    """Fabric Maven-only entries have no downloads field; never swallow a failed probe."""
    for library in profile["libraries"]:
        parts = library["name"].split(":")
        if len(parts) not in (3, 4) or any(not re.fullmatch(r"[A-Za-z0-9_.+-]+", p) for p in parts):
            raise ValueError("Invalid Fabric Maven coordinate")
        group, artifact, version = parts[:3]
        classifier = "-" + parts[3] if len(parts) == 4 else ""
        relative = f"{group.replace('.', '/')}/{artifact}/{version}/{artifact}-{version}{classifier}.jar"
        base = library.get("url", "https://libraries.minecraft.net/").rstrip("/") + "/"
        if not base.startswith("https://"):
            raise ValueError("Official libraries require HTTPS")
        url = base + relative
        expected = checksum_reader(url + ".sha1").strip().split()[0]
        if not re.fullmatch(r"[0-9a-fA-F]{40}", expected):
            raise ValueError("Invalid Maven checksum")
        target = runtime / "libraries" / relative
        downloader(url, target, sha1=expected.lower())
        if not target.is_file() or sha(target, "sha1") != expected.lower():
            raise ValueError("Fabric library is absent or changed after preparation")


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--minecraft", choices=["1.20.1", "1.21.1", "26.1.2"], required=True)
    parser.add_argument("--loader", choices=["fabric", "neoforge", "forge"], required=True)
    parser.add_argument("--mode", choices=["smoke", "manual", "multiplayer"], required=True)
    parser.add_argument("--root", type=Path, required=True)
    parser.add_argument("--mods", type=Path, required=True)
    parser.add_argument("--echo-jar", type=Path, required=True)
    parser.add_argument("--world", type=Path)
    parser.add_argument("--server", default="127.0.0.1:25671")
    parser.add_argument("--label", default="")
    parser.add_argument("--username", choices=["EchoPetOwner", "EchoPetGuest"])
    args = parser.parse_args()
    if os.environ.get("COMPUTERNAME", "").upper() != "FOXY-NODE":
        raise RuntimeError("Long acceptance preparation belongs on FOXY-NODE")
    base = args.root.resolve()
    if not base.is_relative_to(Path(r"D:\Games-Terminal\EchoWarrior")):
        raise ValueError("Acceptance requires an isolated game root")
    if (args.minecraft == "1.20.1" and args.loader == "neoforge") or (
            args.minecraft != "1.20.1" and args.loader == "forge"):
        raise ValueError("Unsupported loader pair")
    if args.mode != "multiplayer" and (not args.world or not (args.world / "level.dat").is_file()):
        raise ValueError("A normally saved test world is required")
    if args.mode == "multiplayer":
        if not re.fullmatch(r"127\.0\.0\.1:\d{4,5}", args.server):
            raise ValueError("Only the isolated loopback server is allowed")
    elif args.username:
        raise ValueError("Distinct acceptance identities are limited to multiplayer fixtures")
    jdk = {"1.20.1": 17, "1.21.1": 21, "26.1.2": 25}[args.minecraft]
    java = ROOT / f".toolchains/jdk-{jdk}/bin/java.exe"
    if not java.is_file():
        raise ValueError("Missing pinned project toolchain")
    properties = ROOT / ("gradle.properties" if args.minecraft == "26.1.2"
                         else f"versions/{args.minecraft}/gradle.properties")
    values = dict(line.strip().split("=", 1) for line in properties.read_text().splitlines()
                  if "=" in line and not line.lstrip().startswith("#"))
    runtime = base / "runtime"
    runtime.mkdir(parents=True, exist_ok=True)
    instance = base / (args.label or f"{args.minecraft}-{args.loader}-{args.mode}")
    if instance.exists():
        raise ValueError(f"Fresh acceptance instance required: {instance}")
    cache = Path.home() / ".gradle/caches"
    prior_runtimes = [p for p in Path(r"D:\Games-Terminal\EchoWarrior").glob("pet-acceptance-*/runtime")
                      if p.resolve() != runtime.resolve()]
    cached: dict[str, list[Path]] = {}
    for path in (cache / "modules-2/files-2.1").rglob("*.jar"):
        cached.setdefault(path.name, []).append(path)
    cached.setdefault(args.minecraft + ".jar", []).extend([
        cache / f"fabric-loom/{args.minecraft}/minecraft-client.jar",
        cache / f"fabric-loom/{args.minecraft}/minecraft-client-only.jar",
    ])

    def download(url, filename, callback=None, sha1=None, **kwargs):
        target = Path(filename)
        if sha1 is None and target.is_relative_to(runtime / "libraries"):
            raise ValueError("Runtime libraries require official checksums")
        if target.is_file() and (sha1 is None or sha(target, "sha1") == sha1):
            return True
        target.parent.mkdir(parents=True, exist_ok=True)
        if sha1:
            if target.is_relative_to(runtime):
                relative = target.relative_to(runtime)
                for previous_runtime in prior_runtimes:
                    cached_path = previous_runtime / relative
                    if cached_path.is_file() and sha(cached_path, "sha1") == sha1:
                        shutil.copy2(cached_path, target)
                        return True
            if target.is_relative_to(runtime / "assets/objects"):
                for shared_assets in (cache / "neoformruntime/assets", cache / "fabric-loom/assets"):
                    object_path = shared_assets / "objects" / sha1[:2] / sha1
                    if object_path.is_file() and sha(object_path, "sha1") == sha1:
                        shutil.copy2(object_path, target)
                        return True
            for path in cached.get(target.name, []):
                if path.is_file() and sha(path, "sha1") == sha1:
                    shutil.copy2(path, target)
                    return True
        if not url:
            raise ValueError(f"Missing installer output: {target}")
        for attempt in range(3):
            try:
                with urllib.request.urlopen(url, timeout=60) as response:
                    data = response.read()
                if sha1 and hashlib.sha1(data).hexdigest() != sha1:
                    raise ValueError(f"Official download checksum mismatch: {target}")
                target.write_bytes(data)
                return True
            except (OSError, TimeoutError, http.client.IncompleteRead):
                if attempt == 2:
                    raise
                time.sleep(1)

    install.download_file = download
    manifest = runtime / "manifest.json"
    download("https://piston-meta.mojang.com/mc/game/version_manifest_v2.json", manifest)
    version = next(v for v in json.loads(manifest.read_text())["versions"] if v["id"] == args.minecraft)
    vanilla_path = runtime / f"versions/{args.minecraft}/{args.minecraft}.json"
    download(version["url"], vanilla_path, sha1=version["sha1"])
    vanilla = json.loads(vanilla_path.read_text())
    callback = {"setStatus": lambda message: print(message, flush=True)}
    install.install_libraries(args.minecraft, vanilla["libraries"], str(runtime), callback, max_workers=4)
    client = vanilla["downloads"]["client"]
    download(client["url"], vanilla_path.with_suffix(".jar"), sha1=client["sha1"])
    assets = cache / "neoformruntime/assets"
    index = assets / "indexes" / (vanilla["assets"] + ".json")
    complete = index.is_file() and sha(index, "sha1") == vanilla["assetIndex"]["sha1"]
    if complete:
        complete = all((assets / "objects" / e["hash"][:2] / e["hash"]).is_file()
                       and sha(assets / "objects" / e["hash"][:2] / e["hash"], "sha1") == e["hash"]
                       for e in json.loads(index.read_text())["objects"].values())
    if not complete:
        install.install_assets(vanilla, str(runtime), callback, max_workers=4)
        assets = runtime / "assets"
    if args.loader == "fabric":
        loader_version = "0.19.5"
        profile_id = f"fabric-loader-{loader_version}-{args.minecraft}"
        profile_path = runtime / f"versions/{profile_id}/{profile_id}.json"
        download(f"https://meta.fabricmc.net/v2/versions/loader/{args.minecraft}/{loader_version}/profile/json", profile_path)
        profile = json.loads(profile_path.read_text())
        def checksum_reader(url):
            with urllib.request.urlopen(url, timeout=45) as response:
                return response.read().decode("ascii")
        install_fabric_libraries(profile, runtime, download, checksum_reader)
        # The inherited profile needs the same official vanilla JAR under its ID.
        # Copy the already checksum-verified file instead of downloading it twice.
        shutil.copy2(vanilla_path.with_suffix(".jar"), profile_path.with_suffix(".jar"))
        if sha(profile_path.with_suffix(".jar"), "sha1") != client["sha1"]:
            raise ValueError("Inherited Fabric game JAR checksum mismatch")
    else:
        loader_version = values["neoforge_version" if args.loader == "neoforge" else "forge_version"]
        profile_id = "neoforge-" + loader_version if args.loader == "neoforge" else f"{args.minecraft}-forge-{loader_version}"
        profile_path = runtime / f"versions/{profile_id}/{profile_id}.json"
        if not profile_path.is_file():
            if args.loader == "neoforge":
                url = f"https://maven.neoforged.net/releases/net/neoforged/neoforge/{loader_version}/neoforge-{loader_version}-installer.jar"
            else:
                url = f"https://maven.minecraftforge.net/net/minecraftforge/forge/{args.minecraft}-{loader_version}/forge-{args.minecraft}-{loader_version}-installer.jar"
            installer = runtime / url.rsplit("/", 1)[-1]
            checksum = installer.with_name(installer.name + ".sha1")
            staged_installer = ROOT / "acceptance-installers" / installer.name
            staged_checksum = staged_installer.with_name(staged_installer.name + ".sha1")
            if staged_installer.is_file() and staged_checksum.is_file():
                expected = staged_checksum.read_text().strip()
                if not re.fullmatch(r"[0-9a-f]{40}", expected) or sha(staged_installer, "sha1") != expected:
                    raise ValueError("Transferred official installer checksum mismatch")
                shutil.copy2(staged_installer, installer)
                shutil.copy2(staged_checksum, checksum)
            download(url + ".sha1", checksum)
            download(url, installer, sha1=checksum.read_text().strip().split()[0])
            # Reuse public libraries from the normally stopped official server runtime.
            # The official client installer still validates its required artifacts.
            library_sources = [args.mods.parent / "libraries"]
            if args.world:
                library_sources.append(args.world.parent / "libraries")
            reused = {}
            for library_source in library_sources:
                if not library_source.is_dir():
                    continue
                for library in library_source.rglob("*"):
                    if not library.is_file():
                        continue
                    relative = library.relative_to(library_source)
                    destination = runtime / "libraries" / relative
                    if not destination.exists():
                        destination.parent.mkdir(parents=True, exist_ok=True)
                        shutil.copy2(library, destination)
                        reused[relative.as_posix()] = sha(destination)
            (runtime / f"{profile_id}-reused-server-libraries.json").write_text(json.dumps(reused, indent=2))
            (runtime / "launcher_profiles.json").write_text('{"profiles":{}}')
            with (runtime / f"{profile_id}-install.log").open("w", encoding="utf-8") as output:
                install_option = "--install-client" if args.loader == "neoforge" else "--installClient"
                result = subprocess.run([str(java), "-jar", str(installer), install_option, str(runtime)],
                                        cwd=runtime, stdout=output, stderr=subprocess.STDOUT,
                                        creationflags=subprocess.CREATE_NO_WINDOW, timeout=900)
            if result.returncode or not profile_path.is_file():
                raise RuntimeError(f"Official client installer failed: {profile_id}")
    instance.mkdir()
    (instance / "mods").mkdir()
    (instance / "acceptance-queue").mkdir()
    artifacts = {}
    for path in sorted(args.mods.glob("*.jar")):
        if path.name.startswith("echo-warrior-"):
            continue
        target = instance / "mods" / path.name
        shutil.copy2(path, target)
        artifacts[path.name] = sha(target)
    if not artifacts or not any("pet_home" in name or "domestication" in name for name in artifacts):
        raise ValueError("Acceptance requires the selected official pet mod and dependencies")
    target = instance / "mods" / args.echo_jar.name
    shutil.copy2(args.echo_jar, target)
    artifacts[target.name] = sha(target)
    if args.world:
        shutil.copytree(args.world, instance / "saves/CATTEST", ignore=shutil.ignore_patterns("session.lock"))
    (instance / "options.txt").write_text(
        "onboardAccessibility:false\nlang:zh_cn\nsoundCategory_master:0.2\npauseOnLostFocus:false\n"
        "renderDistance:6\nsimulationDistance:6\nmaxFps:40\nguiScale:2\n", encoding="utf-8")
    username = {
        ("1.20.1", "fabric"): "Echo1201Fabric", ("1.20.1", "forge"): "Echo1201Forge",
        ("1.21.1", "fabric"): "Echo1211Fabric", ("1.21.1", "neoforge"): "Echo1211Neo",
        ("26.1.2", "fabric"): "EchoWarriorDev", ("26.1.2", "neoforge"): "EchoNeoForge",
    }[(args.minecraft, args.loader)]
    if args.username:
        username = args.username
    player_uuid = uuid.UUID(bytes=hashlib.md5(("OfflinePlayer:" + username).encode()).digest(), version=3)
    flags = ["-Xms512M", "-Xmx3072M", "-Dfile.encoding=UTF-8",
             "-Dio.netty.transport.noNative=true", "-Decho_warrior.acceptance_client=true",
             "-Decho_warrior.acceptance_queue=" + str(instance / "acceptance-queue")]
    if args.mode == "smoke":
        flags.append("-Decho_warrior.auto_pause_after_quick_play=true")
    options = {
        "username": username, "uuid": str(player_uuid), "token": "0",
        "executablePath": str(java), "gameDirectory": str(instance),
        "jvmArguments": flags, "launcherName": "EchoWarrior-isolated-acceptance", "launcherVersion": "1",
    }
    options["quickPlayMultiplayer" if args.mode == "multiplayer" else "quickPlaySingleplayer"] = (
        args.server if args.mode == "multiplayer" else "CATTEST")
    command = launch.get_minecraft_command(profile_id, runtime, options)
    command[command.index("--assetsDir") + 1] = str(assets)
    if "--userType" in command:
        command[command.index("--userType") + 1] = "legacy"
    for key in ("--clientId", "--xuid"):
        if key in command:
            command[command.index(key) + 1] = "0"
    if any("userdev" in a or "devlaunchinjector" in a or "remapped_mods" in a for a in command):
        raise ValueError("Development classpath leaked into production acceptance")
    arguments = instance / "client-arguments.txt"
    arguments.write_text("\n".join(map(quote, command[1:])), encoding="utf-8")
    descriptor = {
        "production": True, "minecraft": args.minecraft, "loader": args.loader, "profile": profile_id,
        "java": str(java), "username": username, "run_directory": str(instance),
        "argument_file": str(arguments), "argument_sha256": sha(arguments), "artifacts": artifacts,
        "acceptance_queue": str(instance / "acceptance-queue"), "mode": args.mode,
        "multiplayer_server": args.server if args.mode == "multiplayer" else None,
        "source_world": str(args.world) if args.world else None,
        "prepared_at": datetime.now(timezone.utc).isoformat(),
    }
    (instance / "launch.json").write_text(json.dumps(descriptor, indent=2), encoding="utf-8")
    print(json.dumps({"prepared": str(instance / "launch.json"), "artifacts": artifacts}), flush=True)


if __name__ == "__main__":
    main()
