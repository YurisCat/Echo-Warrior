#!/usr/bin/env python3
"""Import, normalize, export, and validate the Roman legionary art source."""

from __future__ import annotations

import argparse
import base64
import json
from pathlib import Path
import shutil
import sys


ROOT = Path(__file__).resolve().parents[1]
BBMODEL = ROOT / "assets-source/blockbench/roman_legionary.bbmodel"
GEO = ROOT / "src/main/resources/assets/echo_warrior/geckolib/models/entity/roman_legionary_echo.geo.json"
ANIMATION = ROOT / "src/main/resources/assets/echo_warrior/geckolib/animations/entity/roman_legionary_echo.animation.json"
TEXTURE = ROOT / "src/main/resources/assets/echo_warrior/textures/entity/roman_legionary_echo.png"
HANDOFF = ROOT / "outputs/roman_legionary_modeler_master.bbmodel"

HEAD_UUID = "0ffcd800-8165-5d45-463a-9ffd241206a2"
EYELIDS_UUID = "a1687d88-ff6c-44d8-a31b-79cabf137dde"
RIGHT_EYELID_UUID = "fdb79532-7ee6-46fb-b71e-73f36037f67e"
LEFT_EYELID_UUID = "bba96324-2d78-4b55-9ea1-343eabf97bb1"
RIGHT_EYELID_CUBE_UUID = "3988e23b-acde-49f7-925d-5cc736928aec"
LEFT_EYELID_CUBE_UUID = "826d7aac-c2fb-4b62-9307-50aa8619096b"

CODE_OWNED_BONES = {"head", "left_eye", "right_eye", "eyebrows"}
REMOVED_EYELID_BONES = {"eyelids", "left_eyelid", "right_eyelid"}
REMOVED_EYELID_GROUP_UUIDS = {EYELIDS_UUID, RIGHT_EYELID_UUID, LEFT_EYELID_UUID}
REMOVED_EYELID_CUBE_UUIDS = {RIGHT_EYELID_CUBE_UUID, LEFT_EYELID_CUBE_UUID}
ANIMATION_NAMES = {
    "idle": "animation.roman_legionary.idle",
    "animation2": "animation.roman_legionary.walk",
    "attack": "animation.roman_legionary.attack",
    "shield": "animation.roman_legionary.shield_raise",
    "shield_return": "animation.roman_legionary.shield_lower",
    "hurt": "animation.roman_legionary.hurt",
}
REQUIRED_ANIMATIONS = set(ANIMATION_NAMES.values())
GEOMETRY_FIELDS = ("from", "to", "origin", "rotation", "inflate", "uv_offset", "box_uv", "mirror_uv", "faces")


def read_json(path: Path):
    return json.loads(path.read_text(encoding="utf-8-sig"))


def write_json(path: Path, value) -> None:
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def remove_outliner_nodes(nodes: list) -> list:
    cleaned: list = []
    for node in nodes:
        if isinstance(node, dict):
            if node.get("uuid") in REMOVED_EYELID_GROUP_UUIDS:
                continue
            node["children"] = remove_outliner_nodes(node.get("children", []))
        elif node in REMOVED_EYELID_CUBE_UUIDS:
            continue
        cleaned.append(node)
    return cleaned


def canonicalize_import(model: dict, previous: dict) -> None:
    previous_group_names = {entry.get("uuid"): entry.get("name") for entry in previous.get("groups", [])}
    previous_element_names = {entry.get("uuid"): entry.get("name") for entry in previous.get("elements", [])}

    for group in model.get("groups", []):
        if group.get("uuid") in previous_group_names:
            group["name"] = previous_group_names[group["uuid"]]
    for element in model.get("elements", []):
        if element.get("uuid") in previous_element_names:
            element["name"] = previous_element_names[element["uuid"]]

    for animation in model.get("animations", []):
        animation["name"] = ANIMATION_NAMES.get(animation.get("name"), animation.get("name"))
        if animation["name"] == "animation.roman_legionary.shield_lower":
            animation["loop"] = "once"

    textures = model.get("textures", [])
    if textures:
        textures[0]["name"] = "roman_legionary"


def assert_compatible_geometry(model: dict, previous: dict) -> None:
    previous_elements = {entry.get("uuid"): entry for entry in previous.get("elements", [])}
    imported_elements = {entry.get("uuid"): entry for entry in model.get("elements", [])}
    if previous_elements.keys() != imported_elements.keys():
        raise ValueError("Imported model changes element UUIDs; automated GeckoLib geometry merge is unsafe")

    changed: list[str] = []
    for uuid, previous_element in previous_elements.items():
        imported_element = imported_elements[uuid]
        for field in GEOMETRY_FIELDS:
            if previous_element.get(field) != imported_element.get(field):
                changed.append(f"{previous_element.get('name', uuid)}:{field}")
    if changed:
        raise ValueError("Imported model changes cube geometry/UV fields: " + ", ".join(changed))


def update_bbmodel(model: dict) -> None:
    model["groups"] = [
        entry for entry in model.get("groups", []) if entry.get("uuid") not in REMOVED_EYELID_GROUP_UUIDS
    ]
    model["elements"] = [
        entry for entry in model.get("elements", []) if entry.get("uuid") not in REMOVED_EYELID_CUBE_UUIDS
    ]
    model["outliner"] = remove_outliner_nodes(model.get("outliner", []))

    code_owned_group_ids = {
        entry.get("uuid") for entry in model.get("groups", []) if entry.get("name") in CODE_OWNED_BONES
    }
    if HEAD_UUID not in code_owned_group_ids:
        raise ValueError("Head group is missing from Blockbench groups")

    for animation in model.get("animations", []):
        animators = animation.get("animators", {})
        for uuid in REMOVED_EYELID_GROUP_UUIDS:
            animators.pop(uuid, None)
        for uuid in code_owned_group_ids:
            animators.pop(uuid, None)


def transform_pivot(origin: list) -> list:
    return [clean_number(-number(origin[0])), clean_number(number(origin[1])), clean_number(number(origin[2]))]


def transform_rotation(rotation: list) -> list:
    return [clean_number(-number(rotation[0])), clean_number(-number(rotation[1])), clean_number(number(rotation[2]))]


def update_geo_from_groups(geo: dict, bbmodel: dict) -> None:
    groups = {entry.get("name"): entry for entry in bbmodel.get("groups", [])}
    bones = geo["minecraft:geometry"][0]["bones"]
    bones[:] = [bone for bone in bones if bone.get("name") not in REMOVED_EYELID_BONES]
    for bone in bones:
        group = groups.get(bone.get("name"))
        if group is None:
            continue
        bone["pivot"] = transform_pivot(group.get("origin", [0, 0, 0]))
        rotation = transform_rotation(group.get("rotation", [0, 0, 0]))
        if any(rotation):
            bone["rotation"] = rotation
        else:
            bone.pop("rotation", None)


def number(value) -> float:
    if isinstance(value, (int, float)):
        return float(value)
    text = str(value).strip()
    try:
        return float(text)
    except ValueError as exc:
        raise ValueError(f"Animation expression is not a plain number: {value!r}") from exc


def clean_number(value: float):
    rounded = round(value, 5)
    return int(rounded) if rounded.is_integer() else rounded


def time_key(value) -> str:
    rounded = round(number(value), 5)
    return f"{rounded:.5f}".rstrip("0").rstrip(".") + (".0" if rounded == 0 else "")


def transform_channel(channel: str, values: list) -> list:
    x, y, z = (number(value) for value in values)
    if channel == "rotation":
        transformed = (-x, -y, z)
    elif channel == "position":
        transformed = (-x, y, z)
    else:
        transformed = (x, y, z)
    return [clean_number(value) for value in transformed]


def export_animation(bbmodel: dict) -> dict:
    groups = {entry.get("uuid"): entry.get("name") for entry in bbmodel.get("groups", [])}
    exported = {"format_version": "1.8.0", "animations": {}}

    for source_animation in bbmodel.get("animations", []):
        name = source_animation.get("name")
        animation: dict = {}
        loop = source_animation.get("loop")
        if loop == "loop":
            animation["loop"] = True
        elif loop == "hold":
            animation["loop"] = "hold_on_last_frame"
        animation["animation_length"] = clean_number(number(source_animation.get("length", 0)))
        bones: dict = {}

        for uuid, animator in source_animation.get("animators", {}).items():
            bone_name = groups.get(uuid)
            if bone_name is None or bone_name in CODE_OWNED_BONES | REMOVED_EYELID_BONES:
                continue
            channels: dict = {}
            for keyframe in sorted(animator.get("keyframes", []), key=lambda value: number(value.get("time", 0))):
                channel = keyframe.get("channel")
                if channel not in {"rotation", "position", "scale"}:
                    continue
                points = keyframe.get("data_points", [])
                if not points:
                    continue
                point = points[-1]
                vector = transform_channel(channel, [point.get("x", 0), point.get("y", 0), point.get("z", 0)])
                value: dict = {"post": {"vector": vector}}
                interpolation = keyframe.get("interpolation", "linear")
                if interpolation != "linear":
                    value["lerp_mode"] = interpolation
                channels.setdefault(channel, {})[time_key(keyframe.get("time", 0))] = value
            if channels:
                bones[bone_name] = channels

        animation["bones"] = bones
        exported["animations"][name] = animation

    return exported


def export_texture(bbmodel: dict) -> None:
    textures = bbmodel.get("textures", [])
    if not textures:
        raise ValueError("Imported Blockbench file does not contain an embedded texture")
    source = str(textures[0].get("source", ""))
    if "," not in source:
        raise ValueError("Embedded Blockbench texture is not a data URI")
    TEXTURE.write_bytes(base64.b64decode(source.split(",", 1)[1]))


def validate(bbmodel: dict, geo: dict, animation: dict) -> list[str]:
    errors: list[str] = []
    groups = {entry.get("name"): entry for entry in bbmodel.get("groups", [])}
    for name in CODE_OWNED_BONES:
        if name not in groups:
            errors.append(f"Blockbench group missing: {name}")
    for name in REMOVED_EYELID_BONES:
        if name in groups:
            errors.append(f"Removed Blockbench eyelid group still exists: {name}")

    bones = {entry.get("name"): entry for entry in geo["minecraft:geometry"][0].get("bones", [])}
    for name in CODE_OWNED_BONES:
        if name not in bones:
            errors.append(f"Runtime bone missing: {name}")
    for name in REMOVED_EYELID_BONES:
        if name in bones:
            errors.append(f"Removed runtime eyelid bone still exists: {name}")

    animation_names = set(animation.get("animations", {}))
    missing_animations = REQUIRED_ANIMATIONS - animation_names
    if missing_animations:
        errors.append(f"Runtime animations missing: {sorted(missing_animations)}")

    for animation_name, value in animation.get("animations", {}).items():
        keyed = CODE_OWNED_BONES.intersection(value.get("bones", {}))
        if keyed:
            errors.append(f"Animation {animation_name} controls code-owned bones: {sorted(keyed)}")
    for source_animation in bbmodel.get("animations", []):
        groups_by_uuid = {entry.get("uuid"): entry.get("name") for entry in bbmodel.get("groups", [])}
        keyed = {
            groups_by_uuid.get(uuid)
            for uuid, animator in source_animation.get("animators", {}).items()
            if animator.get("keyframes") and groups_by_uuid.get(uuid) in CODE_OWNED_BONES
        }
        if keyed:
            errors.append(
                f"Blockbench animation {source_animation.get('name')} controls code-owned bones: {sorted(keyed)}"
            )
    return errors


def import_model(source: Path) -> None:
    previous = read_json(BBMODEL)
    model = read_json(source)
    assert_compatible_geometry(model, previous)
    canonicalize_import(model, previous)
    update_bbmodel(model)

    geo = read_json(GEO)
    update_geo_from_groups(geo, model)
    animation = export_animation(model)

    write_json(BBMODEL, model)
    write_json(GEO, geo)
    write_json(ANIMATION, animation)
    export_texture(model)
    shutil.copy2(BBMODEL, HANDOFF)


def update_existing() -> None:
    bbmodel = read_json(BBMODEL)
    for animation in bbmodel.get("animations", []):
        if animation.get("name") == "animation.roman_legionary.shield_lower":
            animation["loop"] = "once"
    update_bbmodel(bbmodel)
    geo = read_json(GEO)
    update_geo_from_groups(geo, bbmodel)
    animation = export_animation(bbmodel)
    write_json(BBMODEL, bbmodel)
    write_json(GEO, geo)
    write_json(ANIMATION, animation)
    export_texture(bbmodel)
    shutil.copy2(BBMODEL, HANDOFF)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("mode", choices=("import", "update", "validate"), nargs="?", default="update")
    parser.add_argument("source", nargs="?", type=Path)
    args = parser.parse_args()

    if args.mode == "import":
        if args.source is None:
            parser.error("import mode requires a source .bbmodel path")
        import_model(args.source.resolve())
    elif args.mode == "update":
        update_existing()

    bbmodel = read_json(BBMODEL)
    geo = read_json(GEO)
    animation = read_json(ANIMATION)
    errors = validate(bbmodel, geo, animation)
    if errors:
        print("Roman visual asset validation failed:", file=sys.stderr)
        for error in errors:
            print(f"- {error}", file=sys.stderr)
        return 1

    print("Roman visual assets are synchronized and valid.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
