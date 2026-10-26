"""Guard the complete red-sand resource/registration path and the scoped Lootr adapter."""
import json
from pathlib import Path
import zipfile

ROOT = Path(__file__).resolve().parents[1]
ASSETS = "assets/echo_warrior/"


def check_assets(read):
    state = json.loads(read(ASSETS + "blockstates/suspicious_red_sand.json"))
    assert set(state["variants"]) == {f"dusted={stage}" for stage in range(4)}
    for stage in range(4):
        entries = state["variants"][f"dusted={stage}"]
        assert len(entries) == 40
        assert {(e["model"], e.get("y", 0)) for e in entries} == {
            (f"echo_warrior:block/suspicious_red_sand_c{pair:02d}_s{stage}", rotation)
            for pair in range(10) for rotation in (0, 90, 180, 270)}
        for pair in range(10):
            name = f"suspicious_red_sand_c{pair:02d}_s{stage}"
            model = json.loads(read(ASSETS + f"models/block/{name}.json"))
            assert model["parent"] == "minecraft:block/cube_all"
            assert model["textures"]["all"] == "echo_warrior:block/" + name
            image = read(ASSETS + f"textures/block/{name}.png")
            assert image[:8] == b"\x89PNG\r\n\x1a\n" and image[16:24] == b"\0\0\0\x10\0\0\0\x10"
    assert json.loads(read(ASSETS + "models/item/suspicious_red_sand.json"))["parent"].startswith(
        "echo_warrior:block/suspicious_red_sand_")
    for locale, value in (("en_us", "Suspicious Red Sand"), ("zh_cn", "可疑的红沙")):
        language = json.loads(read(ASSETS + f"lang/{locale}.json"))
        for kind in ("block", "item"):
            assert language[f"{kind}.echo_warrior.suspicious_red_sand"] == value


def audit_jar(jar: zipfile.ZipFile, version: str):
    check_assets(jar.read)
    tag_dir = "blocks" if version == "1.20.1" else "block"
    assert "echo_warrior:suspicious_red_sand" in json.loads(
        jar.read(f"data/echo_warrior/tags/{tag_dir}/battlefield_brushables.json"))["values"]
    if version == "1.21.1":
        name = "echo_warrior_lootr_1211.mixins.json"
        config = json.loads(jar.read(name))
        assert config["mixins"] == ["LootrSharedArchaeologyMixin1211"]
        for cls in ("integration/LootrMixinPlugin", "integration/LootrArchaeology",
                    "mixin/LootrSharedArchaeologyMixin", "world/LootrArchaeologySelfTest"):
            assert f"com/yuriscat/echowarrior/compat/{cls}1211.class" in jar.namelist()
        assert config["plugin"].endswith(".integration.LootrMixinPlugin1211")
        if "fabric.mod.json" in jar.namelist():
            assert name in json.loads(jar.read("fabric.mod.json"))["mixins"]
        else:
            assert name in jar.read("META-INF/neoforge.mods.toml").decode()


def audit():
    for version, suffix in (("26.1.2", ""), ("1.21.1", "1211"), ("1.20.1", "1201")):
        base = ROOT / "versions" / version if suffix else ROOT
        package = base / "common/src/main/java/com/yuriscat/echowarrior"
        if suffix:
            package /= "compat"
        content = (package / (f"ModContent{suffix}.java" if suffix else "ModBlocks.java")).read_text(encoding="utf-8")
        assert "SUSPICIOUS_RED_SAND" in content and '"suspicious_red_sand"' in content
        assert "BRUSH_SAND_COMPLETED" in content
        system = (package / f"world/BattlefieldSystem{suffix}.java").read_text(encoding="utf-8")
        assert system.index("floor.is(Blocks.RED_SAND)") < system.index("floor.is(BlockTags.SAND)")
        test = (package / f"world/RedSandSelfTest{suffix}.java").read_text(encoding="utf-8")
        assert "brushable.brush(" in test and "hit < 10" in test and "drops.size() == 1" in test
        assert f"RedSandSelfTest{suffix}.run(level)" in (package / f"world/BattlefieldSpacingSelfTest{suffix}.java").read_text(encoding="utf-8")
        if not suffix:
            assert "areEntitiesActuallyLoadedAndTicking" in test and "removeTicketWithRadius" in test
            assert "RedSandSelfTest.prepare" in (package / "world/BattlefieldGenerationSelfTest.java").read_text(encoding="utf-8")
        if version != "1.20.1":
            resources = base / "common/src/main/resources"
            check_assets(lambda path: (resources / path).read_bytes())
        print(f"PASS {version}: red sand registration, terrain selection, resources and real brushing regression")
    adapter = ROOT / "versions/1.21.1/common/src/main/java/com/yuriscat/echowarrior/compat"
    migration = (adapter / "integration/LootrArchaeology1211.java").read_text(encoding="utf-8")
    assert 'getActualOpeners' in migration and 'getMethod("hasBeenOpened")' not in migration
    assert "getInventory(" not in migration and "getChunk(" not in migration
    assert "restored.setLootTable(table, seed)" in migration
    assert "battlefield_guaranteed_relic" in migration
    hook = (adapter / "mixin/LootrSharedArchaeologyMixin1211.java").read_text(encoding="utf-8")
    assert "isSharedTable(table)" in hook and "require = 1" in hook
    print("PASS Lootr: exact shared pools, completed-openers migration, no loot generation or chunk loading")


if __name__ == "__main__":
    import argparse
    parser = argparse.ArgumentParser()
    parser.add_argument("--jar", type=Path, action="append", default=[])
    args = parser.parse_args()
    audit()
    for path in args.jar:
        version = "1.20.1" if "1.20.1" in path.name else "1.21.1" if "1.21.1" in path.name else "26.1.2"
        with zipfile.ZipFile(path) as jar:
            audit_jar(jar, version)
        print(f"PASS packaged archaeology: {path.name}")
