"""Seed isolated CATTEST copies from successfully stopped 1.20.1 smoke servers.

Never opens or converts a 1.21.1/26.1 world and never overwrites an existing save.
"""

from __future__ import annotations

import json
from pathlib import Path
import shutil

ROOT = Path(__file__).resolve().parents[1]
SERVER_TESTS = ROOT / "build" / "compatibility-1.20.1-smoke"


def main() -> None:
    for loader in ("fabric", "forge"):
        destination = ROOT / "versions" / "1.20.1" / f"run-{loader}" / "saves" / "CATTEST"
        marker = destination / "echo-warrior-smoke-world.json"
        if destination.exists():
            if (not marker.is_file() or not (destination / "level.dat").is_file()
                    or json.loads(marker.read_text())["minecraft"] != "1.20.1"):
                raise RuntimeError(f"Refusing to replace or trust an unmarked world: {destination}")
            print(f"Already prepared (not overwritten): {destination}")
            continue
        for report_file in sorted(SERVER_TESTS.glob("*/report.json"), reverse=True):
            report = json.loads(report_file.read_text(encoding="utf-8"))
            source = report_file.parent / loader / "bootstrap-test-world"
            console_file = report_file.parent / loader / "console-2.log"
            if not report.get("results", {}).get(loader, {}).get("passed") or not console_file.is_file():
                continue
            console = console_file.read_text(encoding="utf-8", errors="replace")
            if ("Starting minecraft server version 1.20.1" not in console
                    or "All dimensions are saved" not in console
                    or not (source / "level.dat").is_file()):
                continue
            shutil.copytree(source, destination, ignore=shutil.ignore_patterns("session.lock"))
            marker.write_text(json.dumps({"minecraft": "1.20.1", "loader": loader,
                                          "source_report": str(report_file)}, indent=2), encoding="utf-8")
            print(f"Prepared new isolated world: {destination}")
            break
        else:
            raise RuntimeError(f"No successfully stopped 1.20.1 {loader} world. Run server smoke tests first.")


if __name__ == "__main__":
    main()
