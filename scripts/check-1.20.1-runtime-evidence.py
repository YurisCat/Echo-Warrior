"""Read-only audit: the delivered JARs must be the exact passing server/client artifacts."""
import argparse
import hashlib
import json
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]


def read(path):
    return json.loads(path.read_text(encoding="utf-8-sig"))


def audit(server_report: Path, clients: dict[str, Path]) -> None:
    server = read(server_report)
    assert server["stage"] == "full-content-and-disk-restart", "Old partial server suite"
    for loader, client_report in clients.items():
        result = server["results"][loader]
        client = read(client_report)
        assert result["passed"] and client["passed"] and client["cleanup_passed"], "Incomplete run"
        assert client["production"] and client["loader"] == loader, "Wrong client profile"
        assert result["artifacts"] == client["artifacts"], "Client/server package mismatch"
        own = [name for name in result["artifacts"] if name.startswith("echo-warrior-")]
        assert len(own) == 1, "Ambiguous mod artifact"
        jar = ROOT / "versions/1.20.1" / loader / "build/libs" / own[0]
        digest = hashlib.sha256(jar.read_bytes()).hexdigest()
        assert digest == result["artifacts"][own[0]], "JAR changed after testing"
        for boot in (1, 2):
            log = (server_report.parent / loader / f"console-{boot}.log").read_text(encoding="utf-8")
            assert f"HERO DISK SELFTEST PASSED boot={boot}" in log, "Missing disk restart test"
            assert "MELEE REACH SELFTEST PASSED hero=aztec" in log, "Missing custom-reach regression"
            assert "DEPARTURE EFFECTS SELFTEST PASSED heroes=5" in log, "Missing departure particle regression"
            assert "BATTLEFIELD SNOW SELFTEST PASSED layers=1-8" in log, "Missing snow-cover regression"
            assert "All dimensions are saved" in log, "Server did not save"
            assert not re.search(r"\[(?:[^\]\r\n]+/)?(?:ERROR|FATAL)\]", log), "Server error"
        log = (client_report.parent / "logs/latest.log").read_text(encoding="utf-8")
        assert "All dimensions are saved" in log, "Client did not save"
        # Same exact exception as client-smoke-log.ps1, not a blanket auth-error waiver.
        audited = re.sub(r"(?m)^[^\r\n]*\[Render thread/ERROR\][^\r\n]*Failed to verify authentication\r?\n"
                         r"(?=com\.mojang\.authlib\.exceptions\.InvalidCredentialsException: Status: 401(?:\r?\n|$))", "", log)
        assert not re.search(r"\[(?:[^\]\r\n]+/)?(?:ERROR|FATAL)\]", audited), "Production client error"
        assert log.count("HERO MENU CLIENT SELFTEST PASSED hero=") == 5, "Missing hero runtime case"
        assert log.count("preview-clock=40-ticks-detached") == 5, "Missing all-hero preview clock regression"
        assert "GUANDAO PRESENTATION SELFTEST PASSED processor=geckolib" in log, "Missing Guandao animation regression"
        assert log.count("CREATIVE DESTRUCTION CLIENT SELFTEST PASSED case=") == 12, "Missing creative case"
        assert "COMPASS HUD SELFTEST PASSED messages=6 pulse=orange-glyphs path=loader-frame" in log, "Missing real loader HUD callback"
        assert log.count("sound=3-resolved") == 3, "Missing insertion sound cases"
        print(f"PASS {loader}: production client + two server boots + current JAR SHA-256 {digest}")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--server-report", type=Path, required=True)
    parser.add_argument("--fabric-client-report", type=Path, required=True)
    parser.add_argument("--forge-client-report", type=Path, required=True)
    args = parser.parse_args()
    audit(args.server_report, {"fabric": args.fabric_client_report, "forge": args.forge_client_report})
