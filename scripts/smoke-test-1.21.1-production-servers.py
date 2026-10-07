"""Exercise the packaged 1.21.1 JARs in fresh, loopback-only production servers.

Uses console commands rather than RCON; no reusable password or player save is
transferred. Test EULA acceptance is authorized by PROJECT.md 5.4.1.
"""

from __future__ import annotations

import argparse
from datetime import datetime, timezone
import importlib.util
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import time

ROOT = Path(__file__).resolve().parents[1]
COMPAT = ROOT / "versions/1.21.1"
TEST_ROOT = Path(os.environ.get("ECHO_WARRIOR_TEST_ARTIFACT_ROOT", str(ROOT / "build"))) / "compatibility-1.21.1-production-smoke"
spec = importlib.util.spec_from_file_location("production_helpers", ROOT / "scripts/smoke-test-1.20.1-servers.py")
helpers = importlib.util.module_from_spec(spec)
spec.loader.exec_module(helpers)
helpers.COMPAT = COMPAT
helpers.TEST_ROOT = TEST_ROOT

PERFORMANCE = "BATTLEFIELD PERFORMANCE SELFTEST PASSED regions=10000 lookup=1 search=49 removal=event reload=local"
SUCCESS = "ECHO_WARRIOR_1_21_1_SELFTEST PASS"


def java21() -> str:
    candidates = [
        Path(os.environ["ECHO_WARRIOR_JAVA21_HOME"]) / "bin/java.exe" if os.environ.get("ECHO_WARRIOR_JAVA21_HOME") else None,
        ROOT / ".toolchains/jdk-21/bin/java.exe",
        Path(r"C:\Program Files\Java\jdk-21.0.10\bin\java.exe"),
        Path(shutil.which("java")) if shutil.which("java") else None,
    ]
    for path in candidates:
        if path and path.is_file():
            result = subprocess.run([str(path), "-version"], capture_output=True, text=True,
                                    timeout=15, creationflags=helpers.NO_WINDOW)
            if result.returncode == 0 and 'version "21.' in result.stderr:
                return str(path)
    raise RuntimeError("JDK 21 not found; set ECHO_WARRIOR_JAVA21_HOME")


def install(loader: str, run_dir: Path, values: dict[str, str], java: str) -> list[str]:
    if loader == "fabric":
        return helpers.install(loader, run_dir, values, java)
    version = values["neoforge_version"]
    if helpers.seed_loader_cache(run_dir, values["minecraft_version"], loader, version):
        args = run_dir / "libraries/net/neoforged/neoforge" / version / (
            "win_args.txt" if os.name == "nt" else "unix_args.txt")
        if not args.is_file():
            raise RuntimeError(f"Cached NeoForge installation is incomplete: {args}")
        return [f"@{args}", "nogui"]
    installer = helpers.download(
        f"https://maven.neoforged.net/releases/net/neoforged/neoforge/{version}/neoforge-{version}-installer.jar",
        f"neoforge-{version}-installer.jar",
    )
    log = run_dir / "installer.log"
    with log.open("w", encoding="utf-8") as output:
        result = subprocess.run([java, "-jar", str(installer), "--installServer"], cwd=run_dir,
                                stdout=output, stderr=subprocess.STDOUT, timeout=900,
                                creationflags=helpers.NO_WINDOW)
    if result.returncode:
        raise RuntimeError(f"NeoForge installation failed: {log}")
    args_file = run_dir / "libraries/net/neoforged/neoforge" / version / (
        "win_args.txt" if os.name == "nt" else "unix_args.txt")
    if not args_file.is_file():
        raise RuntimeError(f"NeoForge argument file missing: {args_file}")
    return [f"@{args_file}", "nogui"]


def run_once(java: str, arguments: list[str], run_dir: Path, timeout: int) -> dict:
    log = run_dir / "console-1.log"
    process = None
    started = time.monotonic()
    command_sent = False
    try:
        with log.open("w", encoding="utf-8") as output:
            process = subprocess.Popen([java, "-Xms512M", "-Xmx2G", *helpers.joint_test_flags(), *arguments], cwd=run_dir,
                                       stdin=subprocess.PIPE, stdout=output, stderr=subprocess.STDOUT,
                                       text=True, encoding="utf-8", creationflags=helpers.NO_WINDOW)
            while time.monotonic() - started < timeout:
                text = log.read_text(encoding="utf-8", errors="replace")
                if process.poll() is not None:
                    raise RuntimeError(f"Server exited before acceptance: {log}")
                if "ECHO_WARRIOR_1_21_1_SELFTEST FAIL" in text or "Exception in server tick loop" in text:
                    raise RuntimeError(f"Server self-test failed: {log}")
                if not command_sent and re.search(r"Done \([0-9.]+s\)!", text):
                    process.stdin.write("echo_warrior_compat selftest\n")
                    process.stdin.flush()
                    command_sent = True
                if command_sent and SUCCESS in text and PERFORMANCE in text:
                    if "DEPARTURE EFFECTS SELFTEST PASSED heroes=5" not in text:
                        raise RuntimeError(f"Departure regression guard missing: {log}")
                    process.stdin.write("stop\n")
                    process.stdin.flush()
                    if process.wait(timeout=60) != 0:
                        raise RuntimeError(f"Server shutdown failed: {log}")
                    final = log.read_text(encoding="utf-8", errors="replace")
                    if helpers.joint_test_flags() and "[TbfJointSelfTest] PASS" not in final:
                        raise RuntimeError(f"Installed TBF joint-test acceptance missing: {log}")
                    if "Stopping server" not in final or "All dimensions are saved" not in final:
                        raise RuntimeError(f"Normal save/shutdown markers missing: {log}")
                    # Same narrow exclusions as the existing 1.21.1 development smoke.
                    allowed = [line for line in final.splitlines() if re.search(
                        r"No data fixer registered for echo_warrior:(?:roman_legionary_echo|aztec_warrior_echo|guandao_warrior_echo|japanese_samurai_echo|egyptian_archer_echo|egyptian_archer_arrow)\b"
                        r"|\[Yggdrasil Key Fetcher/ERROR\].*Failed to request yggdrasil public key", line)]
                    unexpected = "\n".join(line for line in final.splitlines() if line not in allowed)
                    if re.search(r"\[(?:[^\]\r\n]+/)?(?:ERROR|FATAL)\]", unexpected):
                        raise RuntimeError(f"Unexpected server ERROR/FATAL: {log}")
                    print(f"PASS {run_dir.name}: packaged Mixin + performance + compatibility + graceful shutdown", flush=True)
                    return {"seconds": round(time.monotonic() - started, 2),
                            "performance_marker": PERFORMANCE, "allowed_diagnostics": allowed}
                time.sleep(0.5)
            raise TimeoutError(f"Server acceptance timed out: {log}")
    finally:
        if process:
            if process.poll() is None:
                try:
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
    parser.add_argument("--loader", choices=("both", "fabric", "neoforge"), default="both")
    parser.add_argument("--timeout", type=int, default=600)
    args = parser.parse_args()
    values = helpers.config_values()
    java = java21()
    run_root = TEST_ROOT / (datetime.now(timezone.utc).strftime("%Y%m%dT%H%M%SZ") + f"-{os.getpid()}")
    run_root.mkdir(parents=True, exist_ok=False)
    results = {}
    report = {"stage": "production-compatibility-and-battlefield-performance", "java": java,
              "minecraft": "1.21.1", "version": values["mod_version"], "results": results,
              "client_tested": False}
    try:
        for loader in (("fabric", "neoforge") if args.loader == "both" else (args.loader,)):
            run_dir = run_root / loader
            run_dir.mkdir()
            (run_dir / "eula.txt").write_text("eula=true\n", encoding="utf-8")
            (run_dir / "server.properties").write_text(
                f"server-ip=127.0.0.1\nserver-port={helpers.free_port()}\nonline-mode=false\n"
                "level-name=performance-test-world\nview-distance=2\nsimulation-distance=2\n"
                "spawn-protection=0\nmax-players=1\nenable-rcon=false\nenable-query=false\n"
                "level-type=minecraft:flat\ngenerate-structures=false\nsync-chunk-writes=true\n"
                'generator-settings={"biome":"minecraft:plains","layers":['
                '{"block":"minecraft:bedrock","height":1},{"block":"minecraft:dirt","height":2},'
                '{"block":"minecraft:grass_block","height":1}],"lakes":false,"features":false,"structure_overrides":[]}\n',
                encoding="utf-8",
            )
            results[loader] = {"passed": False, "artifacts": helpers.stage_mods(loader, run_dir, values)}
            print(f"Preparing production {loader}: {run_dir}", flush=True)
            results[loader].update(run_once(java, install(loader, run_dir, values, java), run_dir, args.timeout))
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
