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
DERIVED_ATTACK_ANIMATIONS = {
    "animation.roman_legionary.attack_first",
    "animation.roman_legionary.attack_recover",
    "animation.roman_legionary.attack_follow",
}
ATTACK_LOCOMOTION_BONES = {
    "root",
    "body_root",
    "lower_body",
    "hips",
    "legs",
    "left_leg",
    "left_upper_leg",
    "left_lower_leg",
    "right_leg",
    "right_upper_leg",
    "right_lower_leg",
    "waist_cloth_center",
    "waist_cloth_left",
    "waist_cloth_right",
}
ANIMATION_NAMES = {
    "idle": "animation.roman_legionary.idle",
    "animation2": "animation.roman_legionary.walk",
    "attack": "animation.roman_legionary.attack",
    "attack_1": "animation.roman_legionary.attack_1",
    "attack_2": "animation.roman_legionary.attack_2",
    "attack_first": "animation.roman_legionary.attack_first",
    "attack_recover": "animation.roman_legionary.attack_recover",
    "attack_follow": "animation.roman_legionary.attack_follow",
    "shield": "animation.roman_legionary.shield_raise",
    "shield_return": "animation.roman_legionary.shield_lower",
    "hurt": "animation.roman_legionary.hurt",
}
REQUIRED_ANIMATIONS = {
    "animation.roman_legionary.idle",
    "animation.roman_legionary.walk",
    "animation.roman_legionary.attack_1",
    "animation.roman_legionary.attack_2",
    "animation.roman_legionary.attack_first",
    "animation.roman_legionary.attack_recover",
    "animation.roman_legionary.attack_follow",
    "animation.roman_legionary.shield_raise",
    "animation.roman_legionary.shield_lower",
    "animation.roman_legionary.hurt",
}
LOCOMOTION_ANIMATIONS = {
    "animation.roman_legionary.idle",
    "animation.roman_legionary.walk",
}
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
    previous_element_entries = previous.get("elements", [])
    imported_element_entries = model.get("elements", [])
    previous_elements = {entry.get("uuid"): entry for entry in previous_element_entries}
    imported_elements = {entry.get("uuid"): entry for entry in imported_element_entries}
    if len(previous_elements) != len(previous_element_entries):
        raise ValueError("Project source contains duplicate element UUIDs")
    if len(imported_elements) != len(imported_element_entries):
        raise ValueError("Imported model contains duplicate element UUIDs")
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


def outliner_parent_maps(model: dict) -> tuple[dict[str, str | None], dict[str, str | None]]:
    group_ids = {entry.get("uuid") for entry in model.get("groups", [])}
    element_ids = {entry.get("uuid") for entry in model.get("elements", [])}
    group_parents: dict[str, str | None] = {}
    element_parents: dict[str, str | None] = {}

    def visit(nodes: list, parent: str | None) -> None:
        for node in nodes:
            if isinstance(node, dict):
                uuid = node.get("uuid")
                next_parent = parent
                if uuid in group_ids:
                    if uuid in group_parents:
                        raise ValueError(f"Blockbench group appears more than once in outliner: {uuid}")
                    group_parents[uuid] = parent
                    next_parent = uuid
                visit(node.get("children", []), next_parent)
            elif node in element_ids:
                if node in element_parents:
                    raise ValueError(f"Blockbench element appears more than once in outliner: {node}")
                element_parents[node] = parent

    visit(model.get("outliner", []), None)
    missing_groups = group_ids - group_parents.keys()
    missing_elements = element_ids - element_parents.keys()
    if missing_groups:
        raise ValueError(f"Blockbench groups are missing from outliner: {sorted(missing_groups)}")
    if missing_elements:
        raise ValueError(f"Blockbench elements are missing from outliner: {sorted(missing_elements)}")
    return group_parents, element_parents


def assert_compatible_animation_rig(model: dict, previous: dict) -> None:
    previous_group_entries = previous.get("groups", [])
    imported_group_entries = model.get("groups", [])
    previous_groups = {entry.get("uuid"): entry for entry in previous_group_entries}
    imported_groups = {entry.get("uuid"): entry for entry in imported_group_entries}
    if len(previous_groups) != len(previous_group_entries):
        raise ValueError("Project source contains duplicate group UUIDs")
    if len(imported_groups) != len(imported_group_entries):
        raise ValueError("Imported model contains duplicate group UUIDs")
    if previous_groups.keys() != imported_groups.keys():
        raise ValueError("Imported model changes group UUIDs; selective animation merge is unsafe")

    changed: list[str] = []
    for uuid, previous_group in previous_groups.items():
        imported_group = imported_groups[uuid]
        for field in ("origin", "rotation"):
            if previous_group.get(field) != imported_group.get(field):
                changed.append(f"{previous_group.get('name', uuid)}:{field}")
    if changed:
        raise ValueError("Imported model changes animation rig pivots: " + ", ".join(changed))

    previous_group_parents, previous_element_parents = outliner_parent_maps(previous)
    imported_group_parents, imported_element_parents = outliner_parent_maps(model)
    if previous_group_parents != imported_group_parents:
        raise ValueError("Imported model changes group parent hierarchy; selective animation merge is unsafe")
    if previous_element_parents != imported_element_parents:
        raise ValueError("Imported model changes cube-to-group assignments; selective animation merge is unsafe")


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
        if animation_name in DERIVED_ATTACK_ANIMATIONS:
            locomotion_keyed = ATTACK_LOCOMOTION_BONES.intersection(value.get("bones", {}))
            if locomotion_keyed:
                errors.append(
                    f"Layered attack {animation_name} controls locomotion bones: {sorted(locomotion_keyed)}"
                )
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
        if source_animation.get("name") in DERIVED_ATTACK_ANIMATIONS:
            locomotion_keyed = {
                groups_by_uuid.get(uuid)
                for uuid, animator in source_animation.get("animators", {}).items()
                if animator.get("keyframes") and groups_by_uuid.get(uuid) in ATTACK_LOCOMOTION_BONES
            }
            if locomotion_keyed:
                errors.append(
                    f"Layered Blockbench attack {source_animation.get('name')} controls locomotion bones: "
                    f"{sorted(locomotion_keyed)}"
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


def import_locomotion(source: Path) -> None:
    previous = read_json(BBMODEL)
    model = read_json(source)
    assert_compatible_geometry(model, previous)
    assert_compatible_animation_rig(model, previous)
    canonicalize_import(model, previous)
    update_bbmodel(model)

    replacements: dict[str, dict] = {}
    for animation in model.get("animations", []):
        name = animation.get("name")
        if name not in LOCOMOTION_ANIMATIONS:
            continue
        if name in replacements:
            raise ValueError(f"Imported model contains duplicate locomotion animation: {name}")
        replacements[name] = animation

    missing = LOCOMOTION_ANIMATIONS - replacements.keys()
    if missing:
        raise ValueError(f"Imported model is missing locomotion animations: {sorted(missing)}")

    previous_animations = previous.get("animations", [])
    existing = {animation.get("name") for animation in previous_animations}
    missing = LOCOMOTION_ANIMATIONS - existing
    if missing:
        raise ValueError(f"Project source is missing locomotion animations: {sorted(missing)}")

    for index, current in enumerate(previous_animations):
        name = current.get("name")
        replacement = replacements.get(name)
        if replacement is None:
            continue
        for field in ("uuid", "saved", "path"):
            if field in current:
                replacement[field] = current[field]
            else:
                replacement.pop(field, None)
        replacement.pop("group_name", None)
        previous_animations[index] = replacement

    update_bbmodel(previous)
    animation = export_animation(previous)
    errors = validate(previous, read_json(GEO), animation)
    if errors:
        raise ValueError("Selective locomotion import failed validation: " + "; ".join(errors))
    write_json(BBMODEL, previous)
    write_json(ANIMATION, animation)
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
    parser.add_argument(
        "mode",
        choices=("import", "import-locomotion", "update", "validate"),
        nargs="?",
        default="update",
    )
    parser.add_argument("source", nargs="?", type=Path)
    args = parser.parse_args()

    if args.mode in {"import", "import-locomotion"}:
        if args.source is None:
            parser.error(f"{args.mode} mode requires a source .bbmodel path")
        if args.mode == "import":
            import_model(args.source.resolve())
        else:
            import_locomotion(args.source.resolve())
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
