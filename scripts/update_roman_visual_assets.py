#!/usr/bin/env python3
"""Maintain code-owned facial bones in the Roman legionary Blockbench and GeckoLib assets."""

from __future__ import annotations

import argparse
import json
from pathlib import Path
import shutil
import sys


ROOT = Path(__file__).resolve().parents[1]
BBMODEL = ROOT / "assets-source/blockbench/roman_legionary.bbmodel"
GEO = ROOT / "src/main/resources/assets/echo_warrior/geckolib/models/entity/roman_legionary_echo.geo.json"
ANIMATION = ROOT / "src/main/resources/assets/echo_warrior/geckolib/animations/entity/roman_legionary_echo.animation.json"
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
            animator = animators.get(uuid)
            if animator is not None:
                animator.pop("keyframes", None)


def update_geo(model: dict) -> None:
    geometry = model["minecraft:geometry"][0]
    bones = geometry["bones"]
    bones[:] = [bone for bone in bones if bone.get("name") not in REMOVED_EYELID_BONES]


def update_animation(model: dict) -> None:
    for animation in model.get("animations", {}).values():
        for bone_name in CODE_OWNED_BONES | REMOVED_EYELID_BONES:
            animation.get("bones", {}).pop(bone_name, None)


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

    for animation_name, value in animation.get("animations", {}).items():
        keyed = CODE_OWNED_BONES.intersection(value.get("bones", {}))
        if keyed:
            errors.append(f"Animation {animation_name} controls code-owned bones: {sorted(keyed)}")
    for animation in bbmodel.get("animations", []):
        groups_by_uuid = {entry.get("uuid"): entry.get("name") for entry in bbmodel.get("groups", [])}
        keyed = {
            groups_by_uuid.get(uuid)
            for uuid, animator in animation.get("animators", {}).items()
            if animator.get("keyframes") and groups_by_uuid.get(uuid) in CODE_OWNED_BONES
        }
        if keyed:
            errors.append(f"Blockbench animation {animation.get('name')} controls code-owned bones: {sorted(keyed)}")
    return errors


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("mode", choices=("update", "validate"), nargs="?", default="update")
    args = parser.parse_args()

    bbmodel = read_json(BBMODEL)
    geo = read_json(GEO)
    animation = read_json(ANIMATION)

    if args.mode == "update":
        update_bbmodel(bbmodel)
        update_geo(geo)
        update_animation(animation)
        write_json(BBMODEL, bbmodel)
        write_json(GEO, geo)
        write_json(ANIMATION, animation)
        shutil.copy2(BBMODEL, HANDOFF)

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
