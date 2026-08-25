#!/usr/bin/env python3
"""Normalize the Guandao Warrior Blockbench source and export GeckoLib assets."""

from __future__ import annotations

import base64
import sys
from pathlib import Path

import update_aztec_visual_assets as exporter


ROOT = Path(__file__).resolve().parents[1]
BBMODEL = ROOT / "assets-source/blockbench/guandao_warrior_echo.bbmodel"
SOURCE_TEXTURE = ROOT / "assets-source/textures/entity/guandao_warrior_echo.png"
GEO = ROOT / "src/main/resources/assets/echo_warrior/geckolib/models/entity/guandao_warrior_echo.geo.json"
ANIMATION = ROOT / "src/main/resources/assets/echo_warrior/geckolib/animations/entity/guandao_warrior_echo.animation.json"
TEXTURE = ROOT / "src/main/resources/assets/echo_warrior/textures/entity/guandao_warrior_echo.png"

ANIMATION_NAMES = {
    "idle": "animation.guandao_warrior.idle",
    "animation2": "animation.guandao_warrior.walk",
    "attack": "animation.guandao_warrior.attack",
    "技能": "animation.guandao_warrior.combo",
    "hurt": "animation.guandao_warrior.hurt",
}
REQUIRED_ANIMATIONS = set(ANIMATION_NAMES.values())


def extract_source_texture(model: dict) -> None:
    if SOURCE_TEXTURE.exists():
        return
    textures = model.get("textures", [])
    if not textures:
        raise ValueError("Blockbench source has no embedded texture")
    source = str(textures[0].get("source", ""))
    if "," not in source:
        raise ValueError("Embedded texture is not a data URI")
    SOURCE_TEXTURE.parent.mkdir(parents=True, exist_ok=True)
    SOURCE_TEXTURE.write_bytes(base64.b64decode(source.split(",", 1)[1]))


def main() -> int:
    exporter.BBMODEL = BBMODEL
    exporter.SOURCE_TEXTURE = SOURCE_TEXTURE
    exporter.GEO = GEO
    exporter.ANIMATION = ANIMATION
    exporter.TEXTURE = TEXTURE

    model = exporter.read_json(BBMODEL)
    extract_source_texture(model)
    model["name"] = "guandao_warrior_echo"
    model["model_identifier"] = "guandao_warrior_echo"
    for animation in model.get("animations", []):
        source_name = animation.get("name")
        animation["name"] = ANIMATION_NAMES.get(source_name, source_name)

    exporter.embed_source_texture(model)
    texture = model["textures"][0]
    texture["name"] = "guandao_warrior_echo"

    geometry = exporter.export_geometry(model)
    runtime_geometry = geometry["minecraft:geometry"][0]
    runtime_geometry["description"]["identifier"] = "geometry.guandao_warrior"
    runtime_geometry["bones"].append(
        {
            "name": "WeaponParticleAnchor",
            "parent": "Weapon",
            "pivot": [-5.5, 13, -17],
        }
    )
    animations = exporter.export_animations(model)

    errors: list[str] = []
    names = set(animations.get("animations", {}))
    if REQUIRED_ANIMATIONS - names:
        errors.append(f"Missing animations: {sorted(REQUIRED_ANIMATIONS - names)}")
    bones = geometry.get("minecraft:geometry", [{}])[0].get("bones", [])
    if len(bones) != len(model.get("groups", [])) + 1:
        errors.append(
            f"Runtime bone count {len(bones)} != source group count {len(model.get('groups', []))} plus particle anchor"
        )

    exporter.write_json(BBMODEL, model)
    exporter.write_json(GEO, geometry)
    exporter.write_json(ANIMATION, animations)
    exporter.export_texture(model)
    if not TEXTURE.exists() or TEXTURE.read_bytes() != SOURCE_TEXTURE.read_bytes():
        errors.append("Runtime texture does not match the canonical source texture")

    if errors:
        print("Guandao visual asset validation failed:", file=sys.stderr)
        for error in errors:
            print(f"- {error}", file=sys.stderr)
        return 1
    print(f"Guandao visual assets synchronized: {len(bones)} bones, {len(names)} animations.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
