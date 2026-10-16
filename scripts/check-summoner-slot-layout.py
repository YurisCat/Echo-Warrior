"""Check menu and tooltip coordinates against the authored GUI export manifest."""

import ast
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
PACKAGE = "com/yuriscat/echowarrior"
TEXTURE = "assets/echo_warrior/textures/gui/summoner/summoner_screen.png"


def coordinates(source: str) -> list[int]:
    match = re.search(r"\bMODULE_SLOT_X\s*=\s*\{([^}]+)\}", source)
    assert match, "Missing explicit accessory slot coordinates"
    return [int(value.strip()) for value in match[1].split(",")]


def audit() -> None:
    # Read the export metadata without importing the asset generator or requiring
    # Pillow/the private Aseprite sources on CI and isolated test snapshots.
    tree = ast.parse((ROOT / "scripts/build-summoner-gui-v6.py").read_text(encoding="utf-8"))
    manifest = next(node for node in tree.body
                    if isinstance(node, ast.FunctionDef) and node.name == "build_manifest")
    layout = ast.literal_eval(manifest.body[0].value)["modules"]
    assert layout["slot_size"] == [18, 18], "Re-audit the slot border/content convention"
    expected_x = [x + 1 for x, _ in layout["positions"]]
    expected_y = {y + 1 for _, y in layout["positions"]}
    assert len(expected_x) == 6 and len(expected_y) == 1
    expected_y = expected_y.pop()

    main = ROOT / "common/src"
    menu = (main / f"main/java/{PACKAGE}/menu/SummonerMenu.java").read_text(encoding="utf-8")
    screen = (main / f"client/java/{PACKAGE}/client/SummonerPreviewScreen.java").read_text(encoding="utf-8")
    assert coordinates(menu) == expected_x, "26.1.2: menu differs from GUI export"
    assert coordinates(screen) == expected_x, "26.1.2: tooltip differs from GUI export"
    assert re.search(rf"MODULE_SLOT_Y\s*=\s*{expected_y}\s*;", menu)
    assert f"y(Element.MODULES, {expected_y}), 16, 16)" in screen
    reference_texture = (main / f"main/resources/{TEXTURE}").read_bytes()
    print("PASS 26.1.2: accessory slots and tooltips match the authored GUI layout")

    for version, suffix in (("1.21.1", "1211"), ("1.20.1", "1201")):
        source = ROOT / f"versions/{version}/common/src"
        menu = (source / f"main/java/{PACKAGE}/compat/menu/SummonerMenu{suffix}.java").read_text(encoding="utf-8")
        screen = (source / f"client/java/{PACKAGE}/compat/client/SummonerScreen{suffix}.java").read_text(encoding="utf-8")
        assert coordinates(menu) == expected_x, f"{version}: menu differs from GUI export"
        constructor = "Slot(this.summonerContainer" if suffix == "1211" else "AccessorySlot(contents"
        assert f"new {constructor}, index, MODULE_SLOT_X[index], {expected_y})" in menu, \
            f"{version}: slot construction bypasses the authored coordinates"
        assert "MODULE_SLOT_X" not in screen, f"{version}: tooltip duplicates slot coordinates"
        assert "Slot slot = this.menu.getSlot(index);" in screen
        assert "!slot.hasItem()" in screen
        assert "this.isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY)" in screen, \
            f"{version}: empty-slot tooltip must follow the actual menu slot"
        # 1.20.1's build imports the 1.21.1 GUI texture.
        texture = ROOT / f"versions/1.21.1/common/src/main/resources/{TEXTURE}"
        assert texture.read_bytes() == reference_texture, f"{version}: GUI texture differs from mainline"
        print(f"PASS {version}: all six accessory slots match the GUI; tooltips use actual slots")


if __name__ == "__main__":
    audit()
