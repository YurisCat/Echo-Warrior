#!/usr/bin/env python3
"""Normalize the Aztec Blockbench source and export GeckoLib runtime assets."""

from __future__ import annotations

import base64
import json
from pathlib import Path
import sys


ROOT = Path(__file__).resolve().parents[1]
BBMODEL = ROOT / "assets-source/blockbench/aztec_warrior_echo.bbmodel"
GEO = ROOT / "src/main/resources/assets/echo_warrior/geckolib/models/entity/aztec_warrior_echo.geo.json"
ANIMATION = ROOT / "src/main/resources/assets/echo_warrior/geckolib/animations/entity/aztec_warrior_echo.animation.json"
TEXTURE = ROOT / "src/main/resources/assets/echo_warrior/textures/entity/aztec_warrior_echo.png"

ANIMATION_NAMES = {
    "animation": "animation.aztec_warrior.idle",
    "animation2": "animation.aztec_warrior.walk",
    "attack": "animation.aztec_warrior.attack",
    "jump": "animation.aztec_warrior.jump",
    "jump attack": "animation.aztec_warrior.pursuit",
    "hurt": "animation.aztec_warrior.hurt",
}
REQUIRED_ANIMATIONS = set(ANIMATION_NAMES.values())


def read_json(path: Path):
    return json.loads(path.read_text(encoding="utf-8-sig"))


def write_json(path: Path, value) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def number(value) -> float:
    if value is None or value == "":
        return 0.0
    return float(value)


def clean_number(value: float):
    rounded = round(number(value), 5)
    return int(rounded) if rounded.is_integer() else rounded


def vector(value, fallback=(0.0, 0.0, 0.0)) -> list[float]:
    if not value:
        return [float(item) for item in fallback]
    return [number(value[index]) for index in range(3)]


def transform_pivot(origin) -> list:
    x, y, z = vector(origin)
    return [clean_number(-x), clean_number(y), clean_number(z)]


def transform_rotation(rotation) -> list:
    x, y, z = vector(rotation)
    return [clean_number(-x), clean_number(-y), clean_number(z)]


def transform_channel(channel: str, values: list) -> list:
    x, y, z = (number(value) for value in values)
    if channel == "rotation":
        result = (-x, -y, z)
    elif channel == "position":
        result = (-x, y, z)
    else:
        result = (x, y, z)
    return [clean_number(value) for value in result]


def time_key(value) -> str:
    rounded = round(number(value), 5)
    return f"{rounded:.5f}".rstrip("0").rstrip(".") + (".0" if rounded == 0 else "")


def repair_jump_attack_rotation(model: dict) -> int:
    groups = {group.get("name"): group.get("uuid") for group in model.get("groups", [])}
    arm_uuid = groups.get("Arm_Right")
    if not arm_uuid:
        raise ValueError("Arm_Right bone is missing")
    repaired = 0
    for animation in model.get("animations", []):
        source_name = animation.get("name")
        normalized_name = ANIMATION_NAMES.get(source_name, source_name)
        animation["name"] = normalized_name
        if normalized_name != "animation.aztec_warrior.pursuit":
            continue
        animator = animation.get("animators", {}).get(arm_uuid)
        if not animator:
            raise ValueError("Pursuit animation does not animate Arm_Right")
        previous = None
        rotation_frames = sorted(
            (frame for frame in animator.get("keyframes", []) if frame.get("channel") == "rotation"),
            key=lambda frame: number(frame.get("time", 0)),
        )
        for frame in rotation_frames:
            for point in frame.get("data_points", []):
                current = number(point.get("x", 0))
                if previous is not None:
                    original = current
                    while current - previous > 180.0:
                        current -= 360.0
                    while current - previous < -180.0:
                        current += 360.0
                    if abs(current - original) > 0.001:
                        repaired += 1
                        point["x"] = clean_number(current)
                previous = current
    return repaired


def export_geometry(model: dict) -> dict:
    groups = {entry.get("uuid"): entry for entry in model.get("groups", [])}
    elements = {entry.get("uuid"): entry for entry in model.get("elements", [])}
    bones: list[dict] = []
    seen_groups: set[str] = set()
    seen_elements: set[str] = set()

    def walk(node, parent_name: str | None = None) -> None:
        if isinstance(node, str):
            return
        if not isinstance(node, dict):
            return
        group = groups.get(node.get("uuid"))
        if group is None:
            return
        group_uuid = group.get("uuid")
        group_name = group.get("name") or group_uuid
        seen_groups.add(group_uuid)
        bone: dict = {"name": group_name, "pivot": transform_pivot(group.get("origin", [0, 0, 0]))}
        if parent_name is not None:
            bone["parent"] = parent_name
        rotation = transform_rotation(group.get("rotation", [0, 0, 0]))
        if any(rotation):
            bone["rotation"] = rotation
        cubes: list[dict] = []
        child_groups: list[dict] = []
        for child in node.get("children", []):
            if isinstance(child, str):
                element = elements.get(child)
                if element is None:
                    continue
                seen_elements.add(child)
                source_from = vector(element.get("from"))
                source_to = vector(element.get("to"))
                cube: dict = {
                    "origin": [
                        clean_number(-source_to[0]),
                        clean_number(source_from[1]),
                        clean_number(source_from[2]),
                    ],
                    "size": [clean_number(source_to[index] - source_from[index]) for index in range(3)],
                    "uv": [clean_number(value) for value in (element.get("uv_offset") or [0, 0])],
                }
                element_rotation = transform_rotation(element.get("rotation", [0, 0, 0]))
                if any(element_rotation):
                    cube["pivot"] = transform_pivot(element.get("origin", [0, 0, 0]))
                    cube["rotation"] = element_rotation
                inflate = number(element.get("inflate", 0))
                if inflate:
                    cube["inflate"] = clean_number(inflate)
                if element.get("mirror_uv"):
                    cube["mirror"] = True
                cubes.append(cube)
            elif isinstance(child, dict):
                child_groups.append(child)
        if cubes:
            bone["cubes"] = cubes
        bones.append(bone)
        for child_group in child_groups:
            walk(child_group, group_name)

    for root_node in model.get("outliner", []):
        walk(root_node)
    missing_groups = set(groups) - seen_groups
    missing_elements = set(elements) - seen_elements
    if missing_groups or missing_elements:
        raise ValueError(
            f"Outliner export incomplete: {len(missing_groups)} groups and {len(missing_elements)} cubes missing"
        )

    resolution = model.get("resolution", {})
    visible = model.get("visible_box", [3, 3.5, 0])
    return {
        "format_version": "1.12.0",
        "minecraft:geometry": [
            {
                "description": {
                    "identifier": "geometry.aztec_warrior",
                    "texture_width": int(resolution.get("width", 128)),
                    "texture_height": int(resolution.get("height", 128)),
                    "visible_bounds_width": max(3.0, number(visible[0]) if len(visible) > 0 else 3.0),
                    "visible_bounds_height": max(3.5, number(visible[1]) if len(visible) > 1 else 3.5),
                    "visible_bounds_offset": [0, 1.25, 0],
                },
                "bones": bones,
            }
        ],
    }


def export_animations(model: dict) -> dict:
    groups = {entry.get("uuid"): entry.get("name") for entry in model.get("groups", [])}
    result = {"format_version": "1.8.0", "animations": {}}
    for source in model.get("animations", []):
        name = source.get("name")
        animation: dict = {"animation_length": clean_number(number(source.get("length", 0)))}
        if source.get("loop") == "loop":
            animation["loop"] = True
        elif source.get("loop") == "hold":
            animation["loop"] = "hold_on_last_frame"
        bones: dict = {}
        for uuid, animator in source.get("animators", {}).items():
            bone_name = groups.get(uuid)
            if not bone_name:
                continue
            channels: dict = {}
            for frame in sorted(animator.get("keyframes", []), key=lambda item: number(item.get("time", 0))):
                channel = frame.get("channel")
                if channel not in {"rotation", "position", "scale"}:
                    continue
                points = frame.get("data_points", [])
                if not points:
                    continue
                point = points[-1]
                value: dict = {
                    "post": {
                        "vector": transform_channel(
                            channel,
                            [point.get("x", 0), point.get("y", 0), point.get("z", 0)],
                        )
                    }
                }
                interpolation = frame.get("interpolation", "linear")
                if interpolation != "linear":
                    value["lerp_mode"] = interpolation
                channels.setdefault(channel, {})[time_key(frame.get("time", 0))] = value
            if channels:
                bones[bone_name] = channels
        animation["bones"] = bones
        result["animations"][name] = animation
    return result


def export_texture(model: dict) -> None:
    textures = model.get("textures", [])
    if not textures:
        raise ValueError("Blockbench source has no embedded texture")
    source = str(textures[0].get("source", ""))
    if "," not in source:
        raise ValueError("Embedded texture is not a data URI")
    TEXTURE.parent.mkdir(parents=True, exist_ok=True)
    TEXTURE.write_bytes(base64.b64decode(source.split(",", 1)[1]))


def validate(model: dict, geo: dict, animations: dict) -> list[str]:
    errors: list[str] = []
    names = set(animations.get("animations", {}))
    if REQUIRED_ANIMATIONS - names:
        errors.append(f"Missing animations: {sorted(REQUIRED_ANIMATIONS - names)}")
    bones = geo.get("minecraft:geometry", [{}])[0].get("bones", [])
    if len(bones) != len(model.get("groups", [])):
        errors.append(f"Runtime bone count {len(bones)} != source group count {len(model.get('groups', []))}")
    pursuit = next((item for item in model.get("animations", []) if item.get("name") == "animation.aztec_warrior.pursuit"), None)
    arm_uuid = next((item.get("uuid") for item in model.get("groups", []) if item.get("name") == "Arm_Right"), None)
    if pursuit and arm_uuid:
        frames = sorted(
            (frame for frame in pursuit.get("animators", {}).get(arm_uuid, {}).get("keyframes", [])
             if frame.get("channel") == "rotation"),
            key=lambda frame: number(frame.get("time", 0)),
        )
        values = [number(frame.get("data_points", [{}])[-1].get("x", 0)) for frame in frames]
        if any(abs(right - left) > 180.0 for left, right in zip(values, values[1:])):
            errors.append("Arm_Right pursuit rotation still contains a >180 degree interpolation jump")
    if not TEXTURE.exists() or TEXTURE.stat().st_size == 0:
        errors.append("Runtime texture is missing")
    return errors


def main() -> int:
    model = read_json(BBMODEL)
    model["name"] = "aztec_warrior_echo"
    model["model_identifier"] = "aztec_warrior_echo"
    if model.get("textures"):
        model["textures"][0]["name"] = "aztec_warrior_echo"
    repaired = repair_jump_attack_rotation(model)
    geometry = export_geometry(model)
    animations = export_animations(model)
    write_json(BBMODEL, model)
    write_json(GEO, geometry)
    write_json(ANIMATION, animations)
    export_texture(model)
    errors = validate(model, geometry, animations)
    if errors:
        print("Aztec visual asset validation failed:", file=sys.stderr)
        for error in errors:
            print(f"- {error}", file=sys.stderr)
        return 1
    print(f"Aztec visual assets synchronized; repaired {repaired} wrapped Arm_Right keyframes.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
