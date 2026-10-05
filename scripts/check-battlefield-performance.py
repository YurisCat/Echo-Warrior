"""Guard bounded compass indexing and event-driven archaeology on the compatibility lines."""

import json
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]


def method(source: str, name: str) -> str:
    matches = list(re.finditer(r"(?:public|private)\s+(?:static\s+)?[^\n{]+\b" + name + r"\([^;]*?\)\s*\{", source))
    assert matches, f"Missing method: {name}"
    match = matches[-1]
    start = match.end()
    depth = 1
    for end in range(start, len(source)):
        depth += (source[end] == "{") - (source[end] == "}")
        if depth == 0:
            return source[start:end]
    raise AssertionError(f"Unclosed method: {name}")


def audit() -> None:
    for version, suffix in (("1.21.1", "1211"), ("1.20.1", "1201")):
        base = ROOT / "versions" / version / "common/src/main"
        java = base / "java/com/yuriscat/echowarrior/compat"
        saved = (java / f"world/BattlefieldSavedData{suffix}.java").read_text(encoding="utf-8")
        for name in ("findActiveByCenter", "nearestActive", "removeBrushableAt", "trackedBrushablesInChunk"):
            body = method(saved, name)
            assert "this.regions.get(" in body, f"{version}/{name}: missing region lookup"
            assert "this.regions.values()" not in body and "this.regions.entrySet()" not in body, (
                f"{version}/{name}: global region scan on gameplay path")
        assert "Math.floorDiv" in method(saved, "findActiveByCenter"), "Negative region coordinates need floor division"
        assert "maximumDistance / regionSize" in method(saved, "nearestActive"), "Search must respect radius"
        assert "this.regions.values()" in method(saved, "nearestKnownActive"), "Admin locate must remain global"
        assert "nearestActive(" not in method(saved, "nearestKnownActive"), "Admin locate must not walk a world-sized grid"

        system = (java / f"world/BattlefieldSystem{suffix}.java").read_text(encoding="utf-8")
        assert "detectRemovedBrushables" not in system, f"{version}: global block polling returned"
        assert "reconcileLoadedChunk(level, chunk)" in method(system, "noteChunk"), "Missing legacy-save repair"
        reconcile = method(system, "reconcileLoadedChunk")
        assert "trackedBrushablesInChunk(chunk.getPos())" in reconcile and "chunk.getBlockState(pos)" in reconcile
        assert "getChunk" not in reconcile and "hasChunk" not in reconcile, "Repair must only read supplied loaded chunk"
        assert "isBrushable(replacement)" in method(system, "onBrushableRemoved"), "Replacing with another brushable still counts"
        mixin_name = f"BlockBehaviourMixin{suffix}"
        mixins = json.loads((base / f"resources/echo_warrior_{suffix}.mixins.json").read_text(encoding="utf-8"))
        assert mixin_name in mixins["mixins"], "Removal hook must run on dedicated servers"
        hook = (java / f"mixin/{mixin_name}.java").read_text(encoding="utf-8")
        assert 'method = "onRemove"' in hook and f"BattlefieldSystem{suffix}.onBrushableRemoved" in hook
        test = (java / f"world/BattlefieldPerformanceSelfTest{suffix}.java").read_text(encoding="utf-8")
        assert "forbidFullScan" in test and "regions.lookups == 49" in test and "regions.lookups == 1" in test
        assert "level.setBlock(relic, Blocks.GRASS_BLOCK.defaultBlockState(), 3)" in test, "Test must invoke actual removal"
        entry = java / ("command/CompatSelfTestCommand1211.java" if suffix == "1211" else "EchoWarrior1201.java")
        assert f"BattlefieldPerformanceSelfTest{suffix}.run(" in entry.read_text(encoding="utf-8")
        print(f"PASS {version}: bounded index, event removal, local reload repair, runtime self-test wired")

    for version, suffix, java in (
        ("26.1.2", "", ROOT / "common/src/main/java/com/yuriscat/echowarrior/world"),
        ("1.21.1", "1211", ROOT / "versions/1.21.1/common/src/main/java/com/yuriscat/echowarrior/compat/world"),
        ("1.20.1", "1201", ROOT / "versions/1.20.1/common/src/main/java/com/yuriscat/echowarrior/compat/world"),
    ):
        compass = (java / f"EchoCompassSystem{suffix}.java").read_text(encoding="utf-8")
        assert not re.search(r"\.(?:getChunk\w*|hasChunk\w*|requestForceGeneration)\(", compass), f"{version}: compass accesses chunks"
        assert "SEARCH_INTERVAL = 40L" in compass, f"{version}: compass search frequency changed"


if __name__ == "__main__":
    audit()
