"""Cross-version source guard; real placement/neighbor-update coverage lives in the 1.20.1 server suite."""
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]


def method(source: str, name: str) -> str:
    start = re.search(r"(?:private )?static [^\n]+\b" + name + r"\([^\n]+\) \{", source)
    assert start, f"Missing terrain method: {name}"
    opening = source.index("{", start.start())
    depth = 1
    end = opening + 1
    while depth:
        depth += (source[end] == "{") - (source[end] == "}")
        end += 1
    body = re.sub(r"//[^\n]*", "", source[opening:end])
    return re.sub(r"\s+", "", body)


def audit() -> None:
    sources = {"26.1.2": ROOT / "common/src/main/java/com/yuriscat/echowarrior/world/BattlefieldSystem.java"}
    for version, suffix in (("1.21.1", "1211"), ("1.20.1", "1201")):
        sources[version] = ROOT / f"versions/{version}/common/src/main/java/com/yuriscat/echowarrior/compat/world/BattlefieldSystem{suffix}.java"
    reference = None
    for version, path in sources.items():
        source = path.read_text(encoding="utf-8-sig")
        rules = {name: method(source, name) for name in ("naturalSurface", "clearVegetation", "isClearable", "isNaturalFloor")}
        assert "state.is(Blocks.SNOW))break;" in rules["clearVegetation"], f"{version}: snow cleared as vegetation"
        assert "is(Blocks.SNOW)?surface.below():surface" in rules["naturalSurface"], f"{version}: thick snow treated as floor"
        assert "state.is(Blocks.SNOW)||" in rules["isClearable"], f"{version}: snowy site rejected"
        assert "SNOW" not in rules["isNaturalFloor"], f"{version}: solid snow must not be excavated"
        for name in ("isSafeSite", "placeSite"):
            body = method(source, name)
            assert "naturalSurface(level,center.getX()+dx,center.getZ()+dz)" in body, f"{version}: {name} bypasses cover handling"
            assert "getHeight(" not in body, f"{version}: {name} uses unadjusted heightmap"
        assert "clearVegetation(level,floor.above())" in method(source, "placeSite")
        if reference is not None:
            assert rules == reference, f"{version}: terrain policy differs from mainline"
        reference = rules
        print(f"PASS {version}: identical snow-preserving terrain policy and both placement call sites")


if __name__ == "__main__":
    audit()
