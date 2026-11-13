"""Prepare official, isolated 26.3 server inputs; never start Minecraft."""
from __future__ import annotations

import argparse
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import urllib.request

ROOT = Path(__file__).resolve().parents[1]
VERSION_ROOT = ROOT / "versions/26.3"
CACHE = ROOT / "build/port-26.3/installers"
NO_WINDOW = subprocess.CREATE_NO_WINDOW if os.name == "nt" else 0
MINECRAFT_METADATA_SHA1 = "9f67e10ed013deeaad20ac3c800652765d493408"
MINECRAFT_METADATA_URL = f"https://piston-meta.mojang.com/v1/packages/{MINECRAFT_METADATA_SHA1}/26.3.json"


def sha(path: Path, algorithm: str = "sha256") -> str:
    with path.open("rb") as stream:
        return hashlib.file_digest(stream, algorithm).hexdigest()


def read(url: str) -> bytes:
    error = None
    for candidate in (url, url + ("&" if "?" in url else "?") + "download=1"):
        try:
            with urllib.request.urlopen(candidate, timeout=90) as response:
                return response.read()
        except OSError as exc:
            error = exc
    raise error


def download(url: str, target: Path, expected: str | None = None) -> Path:
    expected = expected or read(url + ".sha1").decode().strip().split()[0]
    if len(expected) != 40 or any(c not in "0123456789abcdefABCDEF" for c in expected):
        raise ValueError(f"Invalid official SHA-1 for {url}")
    expected = expected.lower()
    if target.is_file() and sha(target, "sha1") == expected:
        return target
    target.parent.mkdir(parents=True, exist_ok=True)
    # Reuse only a byte-verified official artifact, never a development/remapped game JAR.
    for cached in (Path.home() / ".gradle/caches/modules-2/files-2.1").glob(f"*/*/*/{expected}/{target.name}"):
        if sha(cached, "sha1") == expected:
            shutil.copy2(cached, target)
            return target
    data = read(url)
    if hashlib.sha1(data).hexdigest() != expected:
        raise ValueError(f"Official artifact checksum mismatch: {url}")
    target.write_bytes(data)
    return target


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", required=True, type=Path)
    parser.add_argument("--loader", choices=("fabric", "neoforge", "both"), default="both")
    args = parser.parse_args()
    output = args.output.resolve()
    if not output.is_relative_to((ROOT / "build").resolve()) or output.exists():
        raise ValueError("Choose a fresh directory within the repository build directory")
    properties = dict(line.split("=", 1) for line in (VERSION_ROOT / "gradle.properties").read_text().splitlines()
                      if "=" in line and not line.startswith("#"))
    if properties["minecraft_version"] != "26.3":
        raise ValueError("This preparer is pinned to the 26.3 compatibility line")
    java = ROOT / ".toolchains/jdk-25/bin/java.exe"
    if not java.is_file():
        raise ValueError("Project Java 25 toolchain is required")
    output.mkdir(parents=True)
    CACHE.mkdir(parents=True, exist_ok=True)
    caches = Path.home() / ".gradle/caches"
    metadata_bytes = next((p.read_bytes() for p in (
        caches / "neoformruntime/artifacts/minecraft_26.3_version_manifest.json",
        caches / "fabric-loom/26.3/mojang_minecraft_info.json")
        if p.is_file() and sha(p, "sha1") == MINECRAFT_METADATA_SHA1), None)
    if metadata_bytes is None:
        metadata_bytes = read(MINECRAFT_METADATA_URL)
    if hashlib.sha1(metadata_bytes).hexdigest() != MINECRAFT_METADATA_SHA1:
        raise ValueError("Minecraft version metadata checksum mismatch")
    metadata = json.loads(metadata_bytes)
    if metadata["javaVersion"]["majorVersion"] != 25:
        raise ValueError("Unexpected Minecraft Java requirement")
    server = metadata["downloads"]["server"]
    vanilla = CACHE / "minecraft-server-26.3.jar"
    for cached in (caches / "fabric-loom/26.3/minecraft-server.jar",
                   caches / "neoformruntime/artifacts/minecraft_26.3_server.jar"):
        if cached.is_file() and sha(cached, "sha1") == server["sha1"]:
            shutil.copy2(cached, vanilla)
            break
    download(server["url"], vanilla, server["sha1"])

    loaders = ("fabric", "neoforge") if args.loader == "both" else (args.loader,)
    for loader in loaders:
        game = output / loader
        game.mkdir()
        if loader == "fabric":
            installer = download(
                "https://maven.fabricmc.net/net/fabricmc/fabric-installer/1.1.1/fabric-installer-1.1.1.jar",
                CACHE / "fabric-installer-1.1.1.jar")
            shutil.copy2(vanilla, game / "server.jar")
            arguments = ["server", "-mcversion", "26.3", "-loader", properties["loader_version"],
                         "-dir", str(game), "-downloadMinecraft"]
        else:
            nf = properties["neoforge_version"]
            installer = download(
                f"https://maven.neoforged.net/releases/net/neoforged/neoforge/{nf}/neoforge-{nf}-installer.jar",
                CACHE / f"neoforge-{nf}-installer.jar")
            minecraft_library = game / "libraries/net/minecraft/server/26.3/server-26.3.jar"
            minecraft_library.parent.mkdir(parents=True)
            shutil.copy2(vanilla, minecraft_library)
            arguments = ["--installServer", str(game)]
        print(f"Preparing official {loader} runtime...", flush=True)
        with (output.parent / f"{output.name}-{loader}-installer.log").open("w", encoding="utf-8") as log:
            result = subprocess.run([str(java), "-jar", str(installer), *arguments], cwd=game,
                                    stdout=log, stderr=subprocess.STDOUT, timeout=900, creationflags=NO_WINDOW)
        if result.returncode:
            raise RuntimeError(f"Official {loader} installer failed; inspect its log")
        mods = game / "mods"
        mods.mkdir(exist_ok=True)
        for prefix, artifact, value in (
                ("https://dl.cloudsmith.io/public/tslat/sbl/maven/net/tslat",
                 f"smartbrainlib-{loader}-26.3", properties["smartbrainlib_version"]),
                ("https://dl.cloudsmith.io/public/geckolib3/geckolib/maven/com/geckolib",
                 f"geckolib-{loader}-26.3", properties["geckolib_version"])):
            filename = f"{artifact}-{value}.jar"
            download(f"{prefix}/{artifact}/{value}/{filename}", mods / filename)
        if loader == "fabric":
            api = properties["fabric_api_version"]
            filename = f"fabric-api-{api}.jar"
            download(f"https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/{api}/{filename}",
                     mods / filename)
        candidate = VERSION_ROOT / loader / "build/libs" / f"echo-warrior-{loader}-26.3-{properties['mod_version']}.jar"
        if not candidate.is_file():
            raise ValueError(f"Build the candidate first: {candidate}")
        shutil.copy2(candidate, mods / candidate.name)
    shutil.copy2(ROOT / "scripts/smoke-test-battlefield-mainline.py", output)
    shutil.copy2(ROOT / "scripts/server_save_evidence.py", output)
    (output / "runtime.json").write_text(json.dumps({
        "minecraft_version": "26.3", "neoforge_version": properties["neoforge_version"]}, indent=2), encoding="utf-8")
    files = {p.relative_to(output).as_posix(): sha(p) for p in sorted(output.rglob("*"))
             if p.is_file() and not p.name.endswith(".log")}
    (output / "inputs.json").write_text(json.dumps(files, indent=2), encoding="utf-8")
    print(json.dumps({"prepared": str(output), "files": len(files), "loaders": loaders}), flush=True)


if __name__ == "__main__":
    main()
