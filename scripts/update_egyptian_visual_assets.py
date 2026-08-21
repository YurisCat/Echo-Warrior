#!/usr/bin/env python3
"""Normalize the Egyptian Archer source and export GeckoLib runtime assets."""

from __future__ import annotations

import base64
import copy
import importlib.util
from pathlib import Path
import sys


ROOT = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location("aztec_exporter", ROOT / "scripts/update_aztec_visual_assets.py")
exporter = importlib.util.module_from_spec(spec)
assert spec.loader is not None
spec.loader.exec_module(exporter)

BBMODEL = ROOT / "assets-source/blockbench/egyptian_archer_echo.bbmodel"
SOURCE_TEXTURE = ROOT / "assets-source/textures/entity/egyptian_archer_echo.png"
GEO = ROOT / "src/main/resources/assets/echo_warrior/geckolib/models/entity/egyptian_archer_echo.geo.json"
ANIMATION = ROOT / "src/main/resources/assets/echo_warrior/geckolib/animations/entity/egyptian_archer_echo.animation.json"
TEXTURE = ROOT / "src/main/resources/assets/echo_warrior/textures/entity/egyptian_archer_echo.png"
NAMES = {
    "idle": "animation.egyptian_archer.idle",
    "walk": "animation.egyptian_archer.walk",
    "hurt": "animation.egyptian_archer.hurt",
    "draw_bow": "animation.egyptian_archer.draw_bow",
    "backstep_jump": "animation.egyptian_archer.backstep_jump",
    "melee_attack": "animation.egyptian_archer.melee_attack",
    "shoot": "animation.egyptian_archer.shoot",
}
LOWER_BODY_BONES = {
    "Main",
    "DownBody",
    "Hip",
    "bone9",
    "Leg",
    "Leg_Right",
    "bone",
    "Leg_Right_Upper",
    "bone2",
    "Leg_Right_Lower",
    "Leg_Left",
    "Leg_Left_Upper",
    "bone3",
    "Leg_Left_Lower",
}
DERIVED_ANIMATIONS = {
    f"{NAMES[short_name]}_{layer}"
    for short_name in ("draw_bow", "shoot")
    for layer in ("upper", "lower")
}


def extract_texture(model: dict) -> None:
    textures = model.get("textures", [])
    source = str(textures[0].get("source", "")) if textures else ""
    if "," not in source:
        raise ValueError("Egyptian Archer Blockbench source has no embedded texture")
    content = base64.b64decode(source.split(",", 1)[1])
    SOURCE_TEXTURE.parent.mkdir(parents=True, exist_ok=True)
    SOURCE_TEXTURE.write_bytes(content)
    TEXTURE.parent.mkdir(parents=True, exist_ok=True)
    TEXTURE.write_bytes(content)


def add_ranged_animation_layers(animations: dict) -> None:
    animation_map = animations.get("animations", {})
    for short_name in ("draw_bow", "shoot"):
        source_name = NAMES[short_name]
        source = animation_map[source_name]
        upper = copy.deepcopy(source)
        lower = copy.deepcopy(source)
        upper["bones"] = {
            bone: channels for bone, channels in source.get("bones", {}).items()
            if bone not in LOWER_BODY_BONES
        }
        lower["bones"] = {
            bone: channels for bone, channels in source.get("bones", {}).items()
            if bone in LOWER_BODY_BONES
        }
        animation_map[f"{source_name}_upper"] = upper
        animation_map[f"{source_name}_lower"] = lower


def main() -> int:
    model = exporter.read_json(BBMODEL)
    model["name"] = "egyptian_archer_echo"
    model["model_identifier"] = "egyptian_archer_echo"
    for animation in model.get("animations", []):
        animation["name"] = NAMES.get(animation.get("name"), animation.get("name"))
        if animation["name"] == "animation.egyptian_archer.shoot":
            animation["loop"] = "once"
    geometry = exporter.export_geometry(model)
    geometry["minecraft:geometry"][0]["description"]["identifier"] = "geometry.egyptian_archer"
    animations = exporter.export_animations(model)
    add_ranged_animation_layers(animations)
    extract_texture(model)
    exporter.write_json(BBMODEL, model)
    exporter.write_json(GEO, geometry)
    exporter.write_json(ANIMATION, animations)
    missing = (set(NAMES.values()) | DERIVED_ANIMATIONS) - set(animations.get("animations", {}))
    if missing:
        print(f"Missing Egyptian Archer animations: {sorted(missing)}", file=sys.stderr)
        return 1
    print(f"Egyptian Archer assets synchronized: {len(geometry['minecraft:geometry'][0]['bones'])} bones, {len(animations['animations'])} animations.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
