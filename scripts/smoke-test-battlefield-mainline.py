"""Run the mainline generation regression on FOXY using verified, prepared public runtimes.

The input directory contains fabric/ and neoforge/ official server installations,
candidate mods, and inputs.json (relative filename -> SHA-256). No downloads,
development classpaths, player worlds, or main-machine Minecraft runs are used.
"""

import argparse
import hashlib
import json
import os
from pathlib import Path
import platform
import re
import shutil
import subprocess
import time
import zipfile

MARKER = "BATTLEFIELD GENERATION SELFTEST PASSED replacement=actual distance=preserved unload=actual crowded=backoff"
NO_WINDOW = subprocess.CREATE_NO_WINDOW if os.name == "nt" else 0


def digest(path):
    with path.open("rb") as stream:
        return hashlib.file_digest(stream, "sha256").hexdigest()


def run(args):
    if platform.node().upper() != "FOXY-NODE":
        raise RuntimeError("Long Minecraft runs belong on FOXY-NODE")
    version = subprocess.run([str(args.java), "-version"], capture_output=True, text=True,
                             timeout=15, creationflags=NO_WINDOW)
    if version.returncode or 'version "25' not in version.stderr:
        raise RuntimeError("The mainline server requires Java 25")
    manifest = json.loads((args.input / "inputs.json").read_text(encoding="utf-8"))
    for name, expected in manifest.items():
        relative = Path(name)
        if relative.is_absolute() or ".." in relative.parts or ":" in name or "\\" in name:
            raise RuntimeError(f"Unsafe runtime input: {name}")
        if digest(args.input / relative) != expected:
            raise RuntimeError(f"Runtime input changed: {name}")
    report = {"state": "running", "inputs": manifest, "results": []}
    args.reports.mkdir(parents=True, exist_ok=False)
    args.games.mkdir(parents=True, exist_ok=False)
    lease = args.lock.open("x", encoding="utf-8")
    lease.write(f"battlefield-mainline pid={os.getpid()} source={Path(__file__).resolve()}")
    lease.flush()
    try:
        for loader in ("fabric", "neoforge"):
            game = args.games / loader
            shutil.copytree(args.input / loader, game)
            (game / "eula.txt").write_text("eula=true\n", encoding="utf-8")
            # No players can join; the fixture runs solely through the explicit JVM test flag.
            flat = json.dumps({"biome": "minecraft:plains", "lakes": False, "features": False,
                               "structure_overrides": [], "layers": [
                                   {"height": 1, "block": "minecraft:bedrock"},
                                   {"height": 2, "block": "minecraft:dirt"},
                                   {"height": 1, "block": "minecraft:grass_block"}]}, separators=(",", ":"))
            (game / "server.properties").write_text(
                "server-ip=127.0.0.1\nserver-port=0\nonline-mode=false\n"
                "level-name=battlefield-generation-test\nlevel-type=minecraft:flat\n"
                f"generator-settings={flat}\n"
                "generate-structures=false\nspawn-protection=0\nview-distance=3\nsimulation-distance=3\n"
                "pause-when-empty-seconds=0\nmax-tick-time=60000\n", encoding="utf-8")
            log = args.reports / f"{loader}-console.log"
            arguments = (["-jar", "fabric-server-launch.jar", "nogui"] if loader == "fabric"
                         else ["@libraries/net/neoforged/neoforge/26.1.2.100/win_args.txt", "nogui"])
            started = time.monotonic()
            process = None
            try:
                with log.open("w", encoding="utf-8") as output:
                    process = subprocess.Popen(
                        [str(args.java), "-Xms512M", "-Xmx2G",
                         "-Decho_warrior.battlefield_generation_test=true", *arguments],
                        cwd=game, stdin=subprocess.PIPE, stdout=output, stderr=subprocess.STDOUT,
                        text=True, encoding="utf-8", creationflags=NO_WINDOW)
                    while time.monotonic() - started < args.timeout:
                        text = log.read_text(encoding="utf-8", errors="replace")
                        if process.poll() is not None:
                            raise RuntimeError(f"{loader}: exited before acceptance; inspect {log}")
                        if "Exception in server tick loop" in text or "Encountered an unexpected exception" in text:
                            raise RuntimeError(f"{loader}: runtime failure; inspect {log}")
                        if MARKER in text and "BATTLEFIELD SPACING SELFTEST PASSED" in text:
                            process.stdin.write("stop\n")
                            process.stdin.flush()
                            if process.wait(timeout=60) != 0:
                                raise RuntimeError(f"{loader}: abnormal shutdown")
                            final = log.read_text(encoding="utf-8", errors="replace")
                            if "All dimensions are saved" not in final:
                                raise RuntimeError(f"{loader}: missing normal-save evidence")
                            unexpected = [line for line in final.splitlines()
                                          if re.search(r"\[(?:[^\]\r\n]+/)?(?:ERROR|FATAL)\]", line)
                                          and "No data fixer registered for echo_warrior:" not in line
                                          and not re.search(r"\[Yggdrasil Key Fetcher/ERROR\].*Failed to request yggdrasil public key", line)]
                            if unexpected:
                                raise RuntimeError(f"{loader}: unexpected diagnostics: {unexpected[:3]}")
                            report["results"].append({
                                "loader": loader, "passed": True, "marker": MARKER,
                                "seconds": round(time.monotonic() - started, 2),
                                "normal_save": True, "exit_code": process.returncode,
                                "mods": {p.name: digest(p) for p in sorted((game / "mods").glob("*.jar"))},
                            })
                            break
                        time.sleep(0.5)
                    else:
                        raise TimeoutError(f"{loader}: generation acceptance timed out")
            finally:
                if process is not None:
                    if process.poll() is None:
                        try:
                            process.stdin.write("stop\n")
                            process.stdin.flush()
                            process.wait(timeout=30)
                        except (OSError, subprocess.TimeoutExpired):
                            process.kill()
                            process.wait(timeout=15)
                    process.stdin.close()
                latest = game / "logs/latest.log"
                if latest.is_file():
                    shutil.copy2(latest, args.reports / f"{loader}-latest.log")
                for crash in (game / "crash-reports").glob("*.txt"):
                    shutil.copy2(crash, args.reports / f"{loader}-{crash.name}")
        report["state"] = "passed"
    except BaseException as error:
        report["state"] = "failed"
        report["error"] = str(error)
        raise
    finally:
        (args.reports / "report.json").write_text(json.dumps(report, indent=2), encoding="utf-8")
        lease.close()
        args.lock.unlink()
        archive = args.reports / "evidence.zip"
        with zipfile.ZipFile(archive, "x", zipfile.ZIP_DEFLATED) as output:
            for path in sorted(args.reports.iterdir()):
                if path.is_file() and path != archive:
                    output.write(path, path.name)
        (args.reports / "evidence-sha256.txt").write_text(digest(archive), encoding="ascii")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--input", type=Path, required=True)
    parser.add_argument("--games", type=Path, required=True)
    parser.add_argument("--reports", type=Path, required=True)
    parser.add_argument("--java", type=Path, required=True)
    parser.add_argument("--lock", type=Path, required=True)
    parser.add_argument("--timeout", type=int, default=600)
    run(parser.parse_args())
