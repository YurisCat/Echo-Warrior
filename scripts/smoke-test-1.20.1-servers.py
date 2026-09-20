"""Run the built JARs in isolated, headless production servers on loopback only.

Local test EULA acceptance is authorized in PROJECT.md 5.4.1. No player world,
client process, global Java setting, firewall rule or release pipeline is changed.
"""

from __future__ import annotations

import argparse
from datetime import datetime, timezone
import hashlib
import json
import os
import re
from pathlib import Path
import shutil
import socket
import subprocess
import sys
import time
import urllib.request
import zipfile

ROOT = Path(__file__).resolve().parents[1]
COMPAT = ROOT / "versions" / "1.20.1"
TEST_ROOT = ROOT / "build" / "compatibility-1.20.1-smoke"
NO_WINDOW = subprocess.CREATE_NO_WINDOW if os.name == "nt" else 0


def config_values() -> dict[str, str]:
    return dict(line.split("=", 1) for line in
                (COMPAT / "gradle.properties").read_text(encoding="utf-8").splitlines()
                if "=" in line and not line.startswith("#"))


def java17() -> str:
    candidates = [
        Path(os.environ["ECHO_WARRIOR_JAVA17_HOME"]) / "bin" / "java.exe"
        if os.environ.get("ECHO_WARRIOR_JAVA17_HOME") else None,
        ROOT / ".toolchains" / "jdk-17" / "bin" / "java.exe",
        Path(r"C:\Program Files\Eclipse Adoptium\jdk-17.0.18.8-hotspot\bin\java.exe"),
        Path(shutil.which("java")) if shutil.which("java") else None,
    ]
    for candidate in candidates:
        if candidate and candidate.is_file():
            version = subprocess.run([str(candidate), "-version"], capture_output=True,
                                     text=True, timeout=15, creationflags=NO_WINDOW)
            if 'version "17.' in version.stderr:
                return str(candidate)
    raise RuntimeError("JDK 17 not found; set ECHO_WARRIOR_JAVA17_HOME")


def download(url: str, name: str) -> Path:
    destination = TEST_ROOT / "downloads" / name
    destination.parent.mkdir(parents=True, exist_ok=True)
    if destination.is_file() and zipfile.is_zipfile(destination):
        return destination
    partial = destination.with_suffix(destination.suffix + ".part")
    print(f"Downloading {name}", flush=True)
    request = urllib.request.Request(url, headers={"User-Agent": "EchoWarrior-local-compat-test/1"})
    with urllib.request.urlopen(request, timeout=60) as response, partial.open("wb") as output:
        shutil.copyfileobj(response, output)
    if not zipfile.is_zipfile(partial):
        raise RuntimeError(f"Invalid downloaded archive: {url}")
    partial.replace(destination)
    return destination


def sha256(path: Path) -> str:
    with path.open("rb") as stream:
        return hashlib.file_digest(stream, "sha256").hexdigest()


def free_port() -> int:
    with socket.socket() as listener:
        listener.bind(("127.0.0.1", 0))
        return listener.getsockname()[1]


def install(loader: str, run_dir: Path, values: dict[str, str], java: str) -> list[str]:
    mc = values["minecraft_version"]
    if loader == "fabric":
        installer = "1.1.1"
        launcher = download(
            f"https://meta.fabricmc.net/v2/versions/loader/{mc}/{values['loader_version']}/{installer}/server/jar",
            f"fabric-server-{mc}-{values['loader_version']}-{installer}.jar",
        )
        return ["-jar", str(launcher), "nogui"]
    forge_version = f"{mc}-{values['forge_version']}"
    installer = download(
        f"https://maven.minecraftforge.net/net/minecraftforge/forge/{forge_version}/forge-{forge_version}-installer.jar",
        f"forge-{forge_version}-installer.jar",
    )
    # The installer, libraries and world live only in this fresh test directory.
    install_log = run_dir / "installer.log"
    with install_log.open("w", encoding="utf-8") as output:
        result = subprocess.run([java, "-jar", str(installer), "--installServer"], cwd=run_dir,
                                stdout=output, stderr=subprocess.STDOUT,
                                timeout=900, creationflags=NO_WINDOW)
    if result.returncode:
        raise RuntimeError(f"Forge installation failed: {install_log}")
    args_file = run_dir / "libraries" / "net" / "minecraftforge" / "forge" / forge_version / (
        "win_args.txt" if os.name == "nt" else "unix_args.txt")
    if not args_file.is_file():
        raise RuntimeError(f"Forge installer did not create {args_file}")
    return [f"@{args_file}", "nogui"]


def stage_mods(loader: str, run_dir: Path, values: dict[str, str]) -> dict[str, str]:
    mc = values["minecraft_version"]
    mod_dir = run_dir / "mods"
    mod_dir.mkdir()
    artifact = COMPAT / loader / "build" / "libs" / (
        f"{values['archives_base_name']}-{loader}-{mc}-{values['mod_version']}.jar")
    shutil.copy2(artifact, mod_dir / artifact.name)
    dependencies = [
        (f"https://dl.cloudsmith.io/public/tslat/sbl/maven/net/tslat/smartbrainlib/"
         f"SmartBrainLib-{loader}-{mc}/{values['smartbrainlib_version']}/"
         f"SmartBrainLib-{loader}-{mc}-{values['smartbrainlib_version']}.jar",
         f"SmartBrainLib-{loader}-{mc}-{values['smartbrainlib_version']}.jar"),
        (f"https://dl.cloudsmith.io/public/geckolib3/geckolib/maven/software/bernie/geckolib/"
         f"geckolib-{loader}-{mc}/{values['geckolib_version']}/"
         f"geckolib-{loader}-{mc}-{values['geckolib_version']}.jar",
         f"geckolib-{loader}-{mc}-{values['geckolib_version']}.jar"),
    ]
    if loader == "fabric":
        api = values["fabric_api_version"]
        dependencies.append((f"https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/{api}/fabric-api-{api}.jar",
                             f"fabric-api-{api}.jar"))
    for url, filename in dependencies:
        shutil.copy2(download(url, filename), mod_dir / filename)
    return {path.name: sha256(path) for path in sorted(mod_dir.glob("*.jar"))}


def run_once(java: str, arguments: list[str], run_dir: Path, attempt: int, timeout: int) -> None:
    console_log = run_dir / f"console-{attempt}.log"
    expected = "BOOTSTRAP SELFTEST PASSED"
    process = None
    try:
        with console_log.open("w", encoding="utf-8") as output:
            process = subprocess.Popen(
                [java, "-Xms512M", "-Xmx2G", "-Decho_warrior.compat_bootstrap_test=true",
                 f"-Decho_warrior.compat_storage_expected_boot={attempt}", *arguments],
                cwd=run_dir, stdin=subprocess.PIPE, stdout=output, stderr=subprocess.STDOUT,
                text=True, encoding="utf-8", creationflags=NO_WINDOW,
            )
            deadline = time.monotonic() + timeout
            while time.monotonic() < deadline:
                text = console_log.read_text(encoding="utf-8", errors="replace")
                if process.poll() is not None:
                    raise RuntimeError(f"Server exited before acceptance: {console_log}")
                if (expected in text and "Server Mixin applied" in text
                        and re.search(rf"STORAGE SELFTEST PASSED checks=\d+ boot={attempt}\b", text)
                        and re.search(rf"BINDING SELFTEST PASSED checks=\d+ boot={attempt}\b", text)
                        and "NETWORK TRANSACTION SELFTEST PASSED" in text
                        and "MENU SELFTEST PASSED" in text
                        and re.search(rf"RELIC EQUIPMENT SELFTEST PASSED checks=\d+ boot={attempt}\b", text)
                        and re.search(r"CREATIVE INSERT SELFTEST PASSED checks=\d+", text)
                        and re.search(r"HERO LIFECYCLE SELFTEST PASSED checks=\d+ heroes=5", text)
                        and "DEPARTURE EFFECTS SELFTEST PASSED heroes=5 modes=item-id-destroy-death packets=24-soul once=true vanilla=unchanged combo=critical" in text
                        and "MELEE REACH SELFTEST PASSED hero=aztec start=2.4 delayed=2.4 reject=2.6 vertical=checked wall=checked" in text
                        and re.search(rf"HERO DISK SELFTEST PASSED boot={attempt} heroes=5 arrow=1 migration=", text)
                        and re.search(r"TALENT INTEGRATION SELFTEST PASSED mining=actual-96-to-\d+ trading=actual-discount-and-stock", text)
                        and re.search(r"BOOKS SELFTEST PASSED checks=\d+ knowledge=40 tutorial=44", text)
                        and re.search(r"RECYCLER SELFTEST PASSED checks=\d+ transaction=nbt-restored loot=actual sealed=protected", text)
                        and re.search(r"EXPLORATION SELFTEST PASSED checks=\d+ recipes=31 cultures=5 brushing=actual", text)
                        and "EXPLORATION INTEGRATION SELFTEST PASSED placement=actual safety=edited-fluid-tree-slope-unloaded compass=new-instances" in text
                        and "BATTLEFIELD SNOW SELFTEST PASSED layers=1-8 placement=actual cover=preserved solid-snow=untouched" in text
                        and 'For help, type "help"' in text):
                    assert process.stdin is not None
                    process.stdin.write("stop\n")
                    process.stdin.flush()
                    if process.wait(timeout=60) != 0:
                        raise RuntimeError(f"Server shutdown failed: {console_log}")
                    final = console_log.read_text(encoding="utf-8", errors="replace")
                    if "Stopping server" not in final or "All dimensions are saved" not in final:
                        raise RuntimeError(f"Normal save/shutdown markers missing: {console_log}")
                    if ("Failed: " in final or "Exception in server tick loop" in final
                            or re.search(r"\[(?:[^\]\r\n]+/)?(?:ERROR|FATAL)\]", final)):
                        raise RuntimeError(f"Self-test or server exception: {console_log}")
                    print(f"PASS {run_dir.name} boot {attempt}: production Mixin + storage + world authority/restart + dependencies + shutdown", flush=True)
                    return
                time.sleep(0.5)
            raise TimeoutError(f"Server startup timed out: {console_log}")
    finally:
        # Only this invocation's direct Java child is stopped, never unrelated clients/daemons.
        if process is not None:
            if process.poll() is None:
                try:
                    assert process.stdin is not None
                    process.stdin.write("stop\n")
                    process.stdin.flush()
                    process.wait(timeout=15)
                except (BrokenPipeError, OSError, subprocess.TimeoutExpired):
                    process.kill()
                    process.wait(timeout=15)
            if process.stdin:
                process.stdin.close()


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--loader", choices=("both", "fabric", "forge"), default="both")
    parser.add_argument("--timeout", type=int, default=240)
    args = parser.parse_args()
    subprocess.run([sys.executable, str(ROOT / "scripts" / "check-1.20.1-baseline.py")], check=True)
    values = config_values()
    java = java17()
    run_root = TEST_ROOT / (datetime.now(timezone.utc).strftime("%Y%m%dT%H%M%SZ") + f"-{os.getpid()}")
    run_root.mkdir(parents=True, exist_ok=False)
    results = {}
    report = {"stage": "full-content-and-disk-restart", "java": java, "version": values["mod_version"],
              "results": results, "client_tested": False}
    try:
        for loader in (("fabric", "forge") if args.loader == "both" else (args.loader,)):
            run_dir = run_root / loader
            run_dir.mkdir()
            (run_dir / "eula.txt").write_text("eula=true\n", encoding="utf-8")
            (run_dir / "server.properties").write_text(
                f"server-ip=127.0.0.1\nserver-port={free_port()}\nonline-mode=false\n"
                "level-name=bootstrap-test-world\nview-distance=2\nsimulation-distance=2\n"
                "spawn-protection=0\nmax-players=1\nenable-rcon=false\nenable-query=false\n"
                "level-type=minecraft:flat\ngenerate-structures=false\nsync-chunk-writes=true\n"
                'generator-settings={"biome":"minecraft:plains","layers":['
                '{"block":"minecraft:bedrock","height":1},{"block":"minecraft:dirt","height":2},'
                '{"block":"minecraft:grass_block","height":1}],"lakes":false,"features":false,'
                '"structure_overrides":[]}\n',
                encoding="utf-8",
            )
            results[loader] = {"passed": False, "artifacts": stage_mods(loader, run_dir, values)}
            print(f"Preparing production {loader}: {run_dir}", flush=True)
            arguments = install(loader, run_dir, values, java)
            for attempt in (1, 2):
                run_once(java, arguments, run_dir, attempt, args.timeout)
            results[loader]["passed"] = True
        return 0
    finally:
        (run_root / "report.json").write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
        print(f"Report: {run_root / 'report.json'}", flush=True)


if __name__ == "__main__":
    try:
        sys.exit(main())
    except (OSError, RuntimeError, AssertionError, subprocess.SubprocessError) as error:
        print(f"FAIL: {error}", file=sys.stderr)
        sys.exit(1)
