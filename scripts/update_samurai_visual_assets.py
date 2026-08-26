#!/usr/bin/env python3
"""Normalize the Japanese Samurai Blockbench source and export GeckoLib assets."""

from __future__ import annotations

import base64
import random
import sys
from pathlib import Path

import update_aztec_visual_assets as exporter
from PIL import Image, ImageFilter


ROOT = Path(__file__).resolve().parents[1]
BBMODEL = ROOT / "assets-source/blockbench/japanese_samurai_echo.bbmodel"
SOURCE_TEXTURE = ROOT / "assets-source/textures/entity/japanese_samurai_echo.png"
GEO = ROOT / "src/main/resources/assets/echo_warrior/geckolib/models/entity/japanese_samurai_echo.geo.json"
ANIMATION = ROOT / "src/main/resources/assets/echo_warrior/geckolib/animations/entity/japanese_samurai_echo.animation.json"
TEXTURE = ROOT / "src/main/resources/assets/echo_warrior/textures/entity/japanese_samurai_echo.png"
AFTERIMAGE_DETAIL_TEXTURE = (
    ROOT / "src/main/resources/assets/echo_warrior/textures/entity/japanese_samurai_afterimage_detail.png"
)
AFTERIMAGE_SILHOUETTE_TEXTURE = (
    ROOT / "src/main/resources/assets/echo_warrior/textures/entity/japanese_samurai_afterimage_silhouette.png"
)
AFTERIMAGE_DISSOLVE_MASK = (
    ROOT / "src/main/resources/assets/echo_warrior/textures/effect/samurai_afterimage_dissolve.png"
)

ANIMATION_NAMES = {
    "idle": "animation.japanese_samurai.idle",
    "walk": "animation.japanese_samurai.walk",
    "hurt": "animation.japanese_samurai.hurt",
    "attack_1": "animation.japanese_samurai.attack_1",
    "attack_2": "animation.japanese_samurai.attack_2",
    "attack_3": "animation.japanese_samurai.attack_3",
    "stab_attack": "animation.japanese_samurai.stab",
    "dash_forward": "animation.japanese_samurai.dash_forward",
    "dash_backward": "animation.japanese_samurai.dash_backward",
    "attack_first": "animation.japanese_samurai.attack_first",
    "attack_recover": "animation.japanese_samurai.attack_recover",
    "attack_follow": "animation.japanese_samurai.attack_follow",
}
REQUIRED_ANIMATIONS = set(ANIMATION_NAMES.values())
WEAPON_BONE_NAME = "Weapon"
MIN_WEAPON_THICKNESS = 0.25


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


def normalize_weapon_geometry(model: dict) -> None:
    groups = {entry.get("uuid"): entry for entry in model.get("groups", [])}
    elements = {entry.get("uuid"): entry for entry in model.get("elements", [])}
    weapon_group = next((entry for entry in groups.values() if entry.get("name") == WEAPON_BONE_NAME), None)
    if weapon_group is None:
        raise ValueError(f"Missing {WEAPON_BONE_NAME} bone")

    weapon_node = None

    def find_node(nodes: list) -> None:
        nonlocal weapon_node
        for node in nodes:
            if not isinstance(node, dict):
                continue
            if node.get("uuid") == weapon_group.get("uuid"):
                weapon_node = node
                return
            find_node(node.get("children", []))
            if weapon_node is not None:
                return

    find_node(model.get("outliner", []))
    if weapon_node is None:
        raise ValueError(f"Missing {WEAPON_BONE_NAME} outliner node")

    for child in weapon_node.get("children", []):
        if not isinstance(child, str):
            continue
        element = elements.get(child)
        if element is None:
            continue
        source_from = [float(value) for value in element.get("from", [0, 0, 0])]
        source_to = [float(value) for value in element.get("to", [0, 0, 0])]
        for axis in range(3):
            if abs(source_to[axis] - source_from[axis]) > 1e-6:
                continue
            center = (source_from[axis] + source_to[axis]) / 2
            source_from[axis] = center - MIN_WEAPON_THICKNESS / 2
            source_to[axis] = center + MIN_WEAPON_THICKNESS / 2
        element["from"] = [exporter.clean_number(value) for value in source_from]
        element["to"] = [exporter.clean_number(value) for value in source_to]


def export_afterimage_effect_textures() -> None:
    with Image.open(TEXTURE) as source_image:
        source = source_image.convert("RGBA")

    detail_pixels: list[tuple[int, int, int, int]] = []
    silhouette_pixels: list[tuple[int, int, int, int]] = []
    for red, green, blue, alpha in source.get_flattened_data():
        luminance = round(0.2126 * red + 0.7152 * green + 0.0722 * blue)
        # Preserve the full source luminance range. The renderer supplies the
        # saturated cyan/gold energy color, while this texture keeps armor,
        # cloth, and blade contrast readable instead of lifting everything
        # into the 191-255 near-white range.
        detail = round(255 * (luminance / 255) ** 0.85)
        detail_pixels.append((detail, detail, detail, alpha))
        silhouette_pixels.append((255, 255, 255, alpha))

    detail_image = Image.new("RGBA", source.size)
    detail_image.putdata(detail_pixels)
    silhouette_image = Image.new("RGBA", source.size)
    silhouette_image.putdata(silhouette_pixels)

    rng = random.Random(0x5A4D55524149)
    fine = Image.new("L", source.size)
    fine.putdata([rng.randrange(256) for _ in range(source.width * source.height)])
    fine = fine.filter(ImageFilter.GaussianBlur(radius=1.25))
    coarse = Image.new("L", (16, 16))
    coarse.putdata([rng.randrange(256) for _ in range(16 * 16)])
    coarse = coarse.resize(source.size, Image.Resampling.BICUBIC).filter(
        ImageFilter.GaussianBlur(radius=1.0)
    )

    combined: list[float] = []
    fine_data = list(fine.get_flattened_data())
    coarse_data = list(coarse.get_flattened_data())
    for y in range(source.height):
        for x in range(source.width):
            index = y * source.width + x
            directional = 255.0 * (1.0 - x / max(1, source.width - 1))
            combined.append(0.55 * fine_data[index] + 0.15 * coarse_data[index] + 0.30 * directional)
    minimum = min(combined)
    span = max(1.0, max(combined) - minimum)
    mask_alpha = [round((value - minimum) * 255.0 / span) for value in combined]
    dissolve_mask = Image.new("RGBA", source.size)
    dissolve_mask.putdata([(255, 255, 255, alpha) for alpha in mask_alpha])

    for path, image in (
        (AFTERIMAGE_DETAIL_TEXTURE, detail_image),
        (AFTERIMAGE_SILHOUETTE_TEXTURE, silhouette_image),
        (AFTERIMAGE_DISSOLVE_MASK, dissolve_mask),
    ):
        path.parent.mkdir(parents=True, exist_ok=True)
        image.save(path, format="PNG", optimize=False)


def main() -> int:
    exporter.BBMODEL = BBMODEL
    exporter.SOURCE_TEXTURE = SOURCE_TEXTURE
    exporter.GEO = GEO
    exporter.ANIMATION = ANIMATION
    exporter.TEXTURE = TEXTURE

    model = exporter.read_json(BBMODEL)
    extract_source_texture(model)
    model["name"] = "japanese_samurai_echo"
    model["model_identifier"] = "japanese_samurai_echo"
    for animation in model.get("animations", []):
        source_name = animation.get("name")
        animation["name"] = ANIMATION_NAMES.get(source_name, source_name)

    normalize_weapon_geometry(model)
    exporter.embed_source_texture(model)
    texture = model["textures"][0]
    texture["name"] = "japanese_samurai_echo"

    geometry = exporter.export_geometry(model)
    runtime_geometry = geometry["minecraft:geometry"][0]
    runtime_geometry["description"]["identifier"] = "geometry.japanese_samurai"
    animations = exporter.export_animations(model)

    errors: list[str] = []
    names = set(animations.get("animations", {}))
    if REQUIRED_ANIMATIONS - names:
        errors.append(f"Missing animations: {sorted(REQUIRED_ANIMATIONS - names)}")
    bones = runtime_geometry.get("bones", [])
    if len(bones) != len(model.get("groups", [])):
        errors.append(
            f"Runtime bone count {len(bones)} != source group count {len(model.get('groups', []))}"
        )
    weapon_bone = next((bone for bone in bones if bone.get("name") == WEAPON_BONE_NAME), None)
    if weapon_bone is None:
        errors.append(f"Runtime model is missing {WEAPON_BONE_NAME} bone")
    else:
        weapon_cubes = weapon_bone.get("cubes", [])
        if len(weapon_cubes) != 8:
            errors.append(f"Runtime {WEAPON_BONE_NAME} cube count is {len(weapon_cubes)}, expected 8")
        for index, cube in enumerate(weapon_cubes):
            if any(float(size) <= 0 for size in cube.get("size", [])):
                errors.append(f"Runtime {WEAPON_BONE_NAME} cube {index} still has zero thickness")
            if not isinstance(cube.get("uv"), dict):
                errors.append(f"Runtime {WEAPON_BONE_NAME} cube {index} lost its per-face UV mapping")

    exporter.write_json(BBMODEL, model)
    exporter.write_json(GEO, geometry)
    exporter.write_json(ANIMATION, animations)
    exporter.export_texture(model)
    export_afterimage_effect_textures()
    if not TEXTURE.exists() or TEXTURE.read_bytes() != SOURCE_TEXTURE.read_bytes():
        errors.append("Runtime texture does not match the canonical source texture")
    for generated_texture in (
        AFTERIMAGE_DETAIL_TEXTURE,
        AFTERIMAGE_SILHOUETTE_TEXTURE,
        AFTERIMAGE_DISSOLVE_MASK,
    ):
        if not generated_texture.exists():
            errors.append(f"Missing generated afterimage texture: {generated_texture}")

    if errors:
        print("Japanese Samurai visual asset validation failed:", file=sys.stderr)
        for error in errors:
            print(f"- {error}", file=sys.stderr)
        return 1
    print(
        f"Japanese Samurai visual assets synchronized: "
        f"{len(bones)} bones, {len(names)} animations."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
