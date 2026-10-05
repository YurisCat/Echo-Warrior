"""Package current project files, or execute/collect an isolated FOXY-NODE test job.

Only public project inputs and explicitly selected built JARs are packaged. No
Git authentication, Gradle user directory, private art, client saves or account
configuration is copied. The SSH controller uses the existing pinned host.
"""

from __future__ import annotations

import argparse
from datetime import datetime, timezone
import hashlib
import json
import os
from pathlib import Path
import socket
import subprocess
import sys
import zipfile

ROOT = Path(__file__).resolve().parents[1]
NO_WINDOW = subprocess.CREATE_NO_WINDOW if os.name == "nt" else 0


def sha256(path: Path) -> str:
    with path.open("rb") as stream:
        return hashlib.file_digest(stream, "sha256").hexdigest()


def write_json(path: Path, value: dict) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    temporary = path.with_suffix(".tmp")
    temporary.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    temporary.replace(path)


def package(destination: Path, versions: list[str]) -> None:
    prefixes = ("common/", "fabric/", "neoforge/", "versions/", "gradle/", "scripts/")
    root_files = {"AGENTS.md", "PROJECT.md", "build.gradle", "settings.gradle", "gradle.properties",
                  "gradlew", "gradlew.bat", "LICENSE", "LICENSE-CODE", "LICENSE-ASSETS.md", "NOTICE",
                  "docs/VERSION_PORTING_PLAYBOOK.md", "docs/FOXY_TEST_NODE.md"}
    tracked = subprocess.check_output(["git", "ls-files", "-z"], cwd=ROOT).decode("utf-8").split("\0")
    # Untracked new Java/source guards must travel too, otherwise a dirty fix
    # would accidentally be tested against a clean HEAD checkout.
    new_files = subprocess.check_output(["git", "ls-files", "--others", "--exclude-standard", "-z"], cwd=ROOT).decode("utf-8").split("\0")
    selected = {name for name in tracked if name in root_files or name.startswith(prefixes)}
    selected.update(name for name in new_files if name.startswith(("common/src/", "versions/"))
                    and Path(name).suffix in (".java", ".json", ".gradle", ".properties"))
    selected.update(name for name in new_files if name in {
        "scripts/check-battlefield-performance.py", "scripts/smoke-test-1.21.1-production-servers.py",
        "scripts/foxy-test-worker.py", "scripts/run-foxy-tests.ps1", "docs/FOXY_TEST_NODE.md"})
    artifacts = {}
    for version in versions:
        props = dict(line.split("=", 1) for line in (ROOT / "versions" / version / "gradle.properties").read_text().splitlines()
                     if "=" in line and not line.startswith("#"))
        for loader in (("fabric", "neoforge") if version == "1.21.1" else ("fabric", "forge")):
            name = f"versions/{version}/{loader}/build/libs/{props['archives_base_name']}-{loader}-{version}-{props['mod_version']}.jar"
            if not (ROOT / name).is_file():
                raise RuntimeError(f"Build the selected artifact first: {name}")
            selected.add(name)
            artifacts[name] = sha256(ROOT / name)
    files = {}
    destination.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(destination, "w", compression=zipfile.ZIP_DEFLATED, compresslevel=6) as archive:
        for name in sorted(selected):
            path = ROOT / name
            if not path.is_file():
                continue  # A tracked deletion stays deleted in this fresh snapshot.
            if any(part in {".git", ".ssh", ".codex", ".agents", "human-work", "saves"} for part in Path(name).parts):
                raise RuntimeError(f"Private path selected unexpectedly: {name}")
            files[name] = sha256(path)
            archive.write(path, name)
        manifest = {"created_utc": datetime.now(timezone.utc).isoformat(), "versions": versions,
                    "source_head": subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=ROOT, text=True).strip(),
                    "source_dirty": bool(subprocess.check_output(["git", "status", "--porcelain"], cwd=ROOT)),
                    "artifacts": artifacts, "files": files}
        archive.writestr("foxy-source-manifest.json", json.dumps(manifest, indent=2) + "\n")
    print(json.dumps({"package": str(destination), "sha256": sha256(destination),
                      "files": len(files), "bytes": destination.stat().st_size, "artifacts": artifacts}, indent=2))


def execute(report_root: Path, game_root: Path, tools_root: Path) -> int:
    if socket.gethostname().upper() != "FOXY-NODE":
        raise RuntimeError("This worker must execute on the verified FOXY-NODE")
    manifest = json.loads((ROOT / "foxy-source-manifest.json").read_text(encoding="utf-8"))
    report_root.mkdir(parents=True, exist_ok=True)
    lock = ROOT.parent / "active-test.lock"
    # The controller serializes server/client jobs on this machine. Never erase
    # someone else's lock or stop an unrelated Java process to make room.
    descriptor = os.open(lock, os.O_CREAT | os.O_EXCL | os.O_WRONLY)
    with os.fdopen(descriptor, "w") as output:
        output.write(json.dumps({"pid": os.getpid(), "root": str(ROOT)}))
    results = []
    report = {"host": socket.gethostname(), "pid": os.getpid(), "source_root": str(ROOT),
              "source_head": manifest["source_head"], "source_dirty": manifest["source_dirty"],
              "artifacts": manifest["artifacts"], "game_root": str(game_root),
              "started_utc": datetime.now(timezone.utc).isoformat(), "state": "running",
              "results": results, "client_tested": False, "original_modpack_reproduced": False}
    try:
        write_json(report_root / "status.json", report)
        for name, expected in manifest["files"].items():
            if sha256(ROOT / name) != expected:
                raise RuntimeError(f"Transferred source/artifact hash mismatch: {name}")
        report["verified_files"] = len(manifest["files"])
        env = os.environ.copy()
        env["ECHO_WARRIOR_JAVA17_HOME"] = str(tools_root / "jdk-17")
        env["ECHO_WARRIOR_JAVA21_HOME"] = str(tools_root / "jdk-21")
        env["ECHO_WARRIOR_JAVA25_HOME"] = str(tools_root / "jdk-25")
        env["ECHO_WARRIOR_TEST_ARTIFACT_ROOT"] = str(game_root)
        env["PATH"] = str(Path(sys.executable).parent) + os.pathsep + env["PATH"]
        commands = [("performance-source-guard", [sys.executable, str(ROOT / "scripts/check-battlefield-performance.py")])]
        if "1.21.1" in manifest["versions"]:
            commands.extend([
                ("1.21.1-baseline", [r"C:\Program Files\PowerShell\7\pwsh.exe", "-NoLogo", "-NoProfile", "-File",
                                     str(ROOT / "scripts/check-1.21.1-baseline.ps1"), "-SkipBuild"]),
                ("1.21.1-production", [sys.executable, str(ROOT / "scripts/smoke-test-1.21.1-production-servers.py")]),
            ])
        if "1.20.1" in manifest["versions"]:
            commands.extend([
                ("1.20.1-baseline", [sys.executable, str(ROOT / "scripts/check-1.20.1-baseline.py")]),
                ("1.20.1-content-parity", [sys.executable, str(ROOT / "scripts/check-1.20.1-content-parity.py")]),
                ("1.20.1-baseline-negative-tests", [sys.executable, str(ROOT / "scripts/test_compatibility_1201_baseline.py")]),
                ("1.20.1-production", [sys.executable, str(ROOT / "scripts/smoke-test-1.20.1-servers.py"), "--timeout", "600"]),
            ])
        for name, command in commands:
            report["current_step"] = name
            write_json(report_root / "status.json", report)
            started = datetime.now(timezone.utc)
            log = report_root / f"{name}.log"
            with log.open("w", encoding="utf-8") as output:
                result = subprocess.run(command, cwd=ROOT, env=env, stdout=output, stderr=subprocess.STDOUT,
                                        creationflags=NO_WINDOW)
            results.append({"step": name, "exit_code": result.returncode, "log": str(log),
                            "seconds": round((datetime.now(timezone.utc) - started).total_seconds(), 2)})
            if result.returncode:
                raise RuntimeError(f"Failed step {name}; inspect {log}")
        report["state"] = "passed"
        return 0
    except Exception as error:
        report["state"] = "failed"
        report["error"] = str(error)
        return 1
    finally:
        report["finished_utc"] = datetime.now(timezone.utc).isoformat()
        write_json(report_root / "status.json", report)
        lock.unlink()


def collect(report_root: Path, game_root: Path) -> None:
    status = json.loads((report_root / "status.json").read_text(encoding="utf-8"))
    if status["state"] not in ("passed", "failed"):
        raise RuntimeError("Wait until this job finishes before collecting its evidence")
    destination = report_root / "evidence.zip"
    with zipfile.ZipFile(destination, "w", compression=zipfile.ZIP_DEFLATED) as archive:
        for path in sorted(report_root.glob("*.log")) + [report_root / "status.json", ROOT / "foxy-source-manifest.json"]:
            archive.write(path, "reports/" + path.name)
        # Copy owned diagnostics only, never a test world, account file or config.
        for path in sorted(game_root.rglob("*")):
            if path.is_file() and (path.name == "report.json" or path.name.startswith("console-") and path.suffix == ".log"
                                   or path.name == "installer.log" or path.parent.name in ("logs", "crash-reports") and path.suffix in (".log", ".txt")):
                archive.write(path, "runtime/" + path.relative_to(game_root).as_posix())
    print(json.dumps({"evidence": str(destination), "sha256": sha256(destination)}))


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--package", type=Path)
    parser.add_argument("--versions", nargs="+", choices=("1.21.1", "1.20.1"), default=["1.21.1", "1.20.1"])
    parser.add_argument("--execute", action="store_true")
    parser.add_argument("--collect", action="store_true")
    parser.add_argument("--report-root", type=Path)
    parser.add_argument("--game-root", type=Path)
    parser.add_argument("--tools-root", type=Path, default=Path(r"D:\Tools-Terminal\EchoWarrior"))
    args = parser.parse_args()
    if args.package:
        package(args.package, args.versions)
    elif args.report_root and args.game_root and args.execute:
        return execute(args.report_root, args.game_root, args.tools_root)
    elif args.report_root and args.game_root and args.collect:
        collect(args.report_root, args.game_root)
    else:
        parser.error("Select --package, or --execute/--collect with --report-root and --game-root")
    return 0


if __name__ == "__main__":
    sys.exit(main())
