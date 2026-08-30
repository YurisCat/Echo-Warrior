#!/usr/bin/env python3
"""Bake and split the Roman legionary's branchable two-hit attack.

The modeler's complete delivery remains untouched.  This script imports only
the two authored attack animations into the project-owned Blockbench source,
then creates three runtime clips on the original 24 FPS grid:

* attack_first:   attack_2 frames 0..13
* attack_follow:  attack_2 frames 13..25, rebased to frame 0
* attack_recover: attack_1 frames 13..19, smoothly re-anchored to attack_2 F13

The source uses Catmull-Rom interpolation.  Directly trimming it would change
the boundary tangents, so every derived clip is baked to per-frame linear keys.
"""

from __future__ import annotations

import argparse
import copy
import hashlib
import json
import math
import uuid
from pathlib import Path
from typing import Any

from update_roman_visual_assets import (
    ANIMATION_NAMES,
    ATTACK_LOCOMOTION_BONES,
    BBMODEL,
    CODE_OWNED_BONES,
    DERIVED_ATTACK_ANIMATIONS,
    assert_compatible_animation_rig,
    assert_compatible_geometry,
    canonicalize_import,
    read_json,
    update_bbmodel,
    write_json,
)


ROOT = Path(__file__).resolve().parents[1]
DEFAULT_DELIVERY = (
    ROOT
    / "assets-source/blockbench/deliveries"
    / "roman_legionary_2026-08-26_full-animation-delivery.bbmodel"
)
FPS = 24
FIRST_END_FRAME = 13
RECOVER_END_FRAME = 19
FOLLOW_END_FRAME = 25
FRAME_EPSILON = 1.0e-4
VALUE_EPSILON = 1.0e-5
UUID_NAMESPACE = uuid.UUID("55a07c39-f160-44db-8f57-58912ea7e652")

ATTACK_NAMES = {
    "animation.roman_legionary.attack",
    "animation.roman_legionary.attack_1",
    "animation.roman_legionary.attack_2",
    "animation.roman_legionary.attack_first",
    "animation.roman_legionary.attack_recover",
    "animation.roman_legionary.attack_follow",
}
DERIVED_NAMES = set(DERIVED_ATTACK_ANIMATIONS)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--input",
        type=Path,
        default=DEFAULT_DELIVERY,
        help="Untouched full Roman Blockbench delivery",
    )
    parser.add_argument(
        "--output",
        type=Path,
        default=BBMODEL,
        help="Project-owned Roman Blockbench source",
    )
    return parser.parse_args()


def canonical_hash(value: Any) -> str:
    encoded = json.dumps(
        value, ensure_ascii=False, sort_keys=True, separators=(",", ":")
    ).encode("utf-8")
    return hashlib.sha256(encoded).hexdigest()


def deterministic_uuid(*parts: object) -> str:
    return str(uuid.uuid5(UUID_NAMESPACE, "|".join(str(part) for part in parts)))


def reuuid_animation(animation: dict[str, Any]) -> dict[str, Any]:
    """Give a copied authored animation project-local, repeatable UUIDs."""
    result = copy.deepcopy(animation)
    name = result["name"]
    result["uuid"] = deterministic_uuid(name, "authored-animation")
    for animator_uuid, animator in result.get("animators", {}).items():
        channel_occurrences: dict[tuple[str, int], int] = {}
        for frame in animator.get("keyframes", []):
            channel = frame.get("channel", "")
            source_frame = source_frame_index(frame.get("time", 0.0))
            key = (channel, source_frame)
            occurrence = channel_occurrences.get(key, 0)
            channel_occurrences[key] = occurrence + 1
            frame["uuid"] = deterministic_uuid(
                name, "authored-key", animator_uuid, channel, source_frame, occurrence
            )
    return result


def frame_seconds(frame: int) -> float:
    return round(frame / FPS, 5)


def source_frame_index(value: object) -> int:
    time = float(value)
    frame = round(time * FPS)
    if abs(time - frame / FPS) > FRAME_EPSILON:
        raise ValueError(f"Animation key {time}s is not on the {FPS} FPS grid")
    return frame


def number_string(value: float) -> str:
    if abs(value) < 1.0e-12:
        value = 0.0
    return format(value, ".12g")


def vector_values(frame: dict[str, Any]) -> list[list[float]]:
    result: list[list[float]] = []
    for point in frame.get("data_points", []):
        result.append([float(point.get(axis, 0.0)) for axis in ("x", "y", "z")])
    if not result:
        raise ValueError("Animation keyframe has no data points")
    return result


def unwrap_rotation_vectors(samples: list[list[list[float]]]) -> None:
    for index in range(1, len(samples)):
        previous = samples[index - 1]
        current = samples[index]
        if len(previous) != len(current):
            raise ValueError("Rotation keyframes have different data-point counts")
        for point_index in range(len(current)):
            for axis in range(3):
                while current[point_index][axis] - previous[point_index][axis] > 180.0:
                    current[point_index][axis] -= 360.0
                while current[point_index][axis] - previous[point_index][axis] < -180.0:
                    current[point_index][axis] += 360.0


def channel_samples(frames: list[dict[str, Any]]) -> tuple[list[int], list[list[list[float]]]]:
    ordered = sorted(frames, key=lambda frame: source_frame_index(frame.get("time", 0.0)))
    indices = [source_frame_index(frame.get("time", 0.0)) for frame in ordered]
    if len(indices) != len(set(indices)):
        raise ValueError(f"Animation channel contains duplicate frame times: {indices}")
    samples = [vector_values(frame) for frame in ordered]
    if ordered[0].get("channel") == "rotation":
        unwrap_rotation_vectors(samples)
    return indices, samples


def catmull_rom(t: float, p0: float, p1: float, p2: float, p3: float) -> float:
    return 0.5 * (
        2.0 * p1
        + (p2 - p0) * t
        + (2.0 * p0 - 5.0 * p1 + 4.0 * p2 - p3) * t * t
        + (3.0 * p1 - p0 - 3.0 * p2 + p3) * t * t * t
    )


def sample_channel(frames: list[dict[str, Any]], target_frame: int) -> list[list[float]]:
    ordered = sorted(frames, key=lambda frame: source_frame_index(frame.get("time", 0.0)))
    indices, values = channel_samples(ordered)
    if target_frame <= indices[0]:
        return copy.deepcopy(values[0])
    if target_frame >= indices[-1]:
        return copy.deepcopy(values[-1])
    if target_frame in indices:
        return copy.deepcopy(values[indices.index(target_frame)])

    right_index = next(index for index, frame in enumerate(indices) if frame > target_frame)
    left_index = right_index - 1
    left_frame = indices[left_index]
    right_frame = indices[right_index]
    amount = (target_frame - left_frame) / (right_frame - left_frame)
    interpolation = ordered[right_index].get("interpolation", "linear")
    if interpolation == "step":
        return copy.deepcopy(values[left_index])
    if interpolation not in {"linear", "catmullrom"}:
        raise ValueError(f"Unsupported animation interpolation: {interpolation!r}")

    point_count = len(values[left_index])
    if len(values[right_index]) != point_count:
        raise ValueError("Animation keyframes have different data-point counts")
    result: list[list[float]] = []
    for point_index in range(point_count):
        point: list[float] = []
        for axis in range(3):
            p1 = values[left_index][point_index][axis]
            p2 = values[right_index][point_index][axis]
            if interpolation == "linear":
                value = p1 + (p2 - p1) * amount
            else:
                p0 = values[max(0, left_index - 1)][point_index][axis]
                p3 = values[min(len(values) - 1, right_index + 1)][point_index][axis]
                value = catmull_rom(amount, p0, p1, p2, p3)
            point.append(value)
        result.append(point)
    return result


def grouped_channels(animator: dict[str, Any]) -> dict[str, list[dict[str, Any]]]:
    channels: dict[str, list[dict[str, Any]]] = {}
    for frame in animator.get("keyframes", []):
        channel = frame.get("channel")
        if channel not in {"rotation", "position"}:
            raise ValueError(f"Unexpected Roman attack channel: {channel!r}")
        channels.setdefault(channel, []).append(frame)
    return channels


def baked_keyframe(
    template: dict[str, Any],
    *,
    animation_name: str,
    animator_uuid: str,
    channel: str,
    frame: int,
    values: list[list[float]],
) -> dict[str, Any]:
    result = copy.deepcopy(template)
    result["uuid"] = deterministic_uuid(animation_name, animator_uuid, channel, frame)
    result["channel"] = channel
    result["time"] = frame_seconds(frame)
    result["interpolation"] = "linear"
    result["data_points"] = [
        {axis: number_string(point[index]) for index, axis in enumerate(("x", "y", "z"))}
        for point in values
    ]
    return result


def values_equal(left: list[list[float]], right: list[list[float]]) -> bool:
    if len(left) != len(right):
        return False
    return all(
        abs(left_point[axis] - right_point[axis]) <= VALUE_EPSILON
        for left_point, right_point in zip(left, right)
        for axis in range(3)
    )


def shortest_rotation_delta(target: float, source: float) -> float:
    delta = target - source
    while delta > 180.0:
        delta -= 360.0
    while delta < -180.0:
        delta += 360.0
    return delta


def make_baked_segment(
    source: dict[str, Any],
    *,
    name: str,
    start_frame: int,
    end_frame: int,
    recovery_anchor: dict[tuple[str, str], list[list[float]]] | None = None,
    excluded_animator_uuids: set[str] | None = None,
) -> dict[str, Any]:
    result = copy.deepcopy(source)
    result["uuid"] = deterministic_uuid(name, "animation")
    result["name"] = name
    result["length"] = frame_seconds(end_frame - start_frame)
    result["loop"] = "once"
    result["snapping"] = FPS
    result["selected"] = False
    result["saved"] = False
    result.pop("path", None)

    excluded_animator_uuids = excluded_animator_uuids or set()
    for animator_uuid in list(result.get("animators", {})):
        if animator_uuid in excluded_animator_uuids:
            result["animators"].pop(animator_uuid)
            continue
        animator = result["animators"][animator_uuid]
        source_animator = source["animators"][animator_uuid]
        baked: list[dict[str, Any]] = []
        for channel, frames in grouped_channels(source_animator).items():
            template = frames[0]
            sampled: list[list[list[float]]] = []
            for source_frame in range(start_frame, end_frame + 1):
                values = sample_channel(frames, source_frame)
                if recovery_anchor is not None:
                    key = (animator_uuid, channel)
                    anchor = recovery_anchor[key]
                    source_start = sample_channel(frames, start_frame)
                    progress = (source_frame - start_frame) / (end_frame - start_frame)
                    weight = 1.0 - 3.0 * progress * progress + 2.0 * progress * progress * progress
                    adjusted: list[list[float]] = []
                    for point_index, point in enumerate(values):
                        adjusted_point: list[float] = []
                        for axis, value in enumerate(point):
                            if channel == "rotation":
                                delta = shortest_rotation_delta(
                                    anchor[point_index][axis], source_start[point_index][axis]
                                )
                            else:
                                delta = anchor[point_index][axis] - source_start[point_index][axis]
                            adjusted_point.append(value + weight * delta)
                        adjusted.append(adjusted_point)
                    values = adjusted
                sampled.append(values)

            if all(values_equal(sampled[0], value) for value in sampled[1:]):
                sampled = sampled[:1]
            for destination_frame, values in enumerate(sampled):
                baked.append(
                    baked_keyframe(
                        template,
                        animation_name=name,
                        animator_uuid=animator_uuid,
                        channel=channel,
                        frame=destination_frame,
                        values=values,
                    )
                )
        baked.sort(key=lambda frame: (float(frame["time"]), frame["channel"]))
        animator["keyframes"] = baked
    return result


def pose_at_frame(animation: dict[str, Any], frame: int) -> dict[tuple[str, str], list[list[float]]]:
    pose: dict[tuple[str, str], list[list[float]]] = {}
    for animator_uuid, animator in animation.get("animators", {}).items():
        for channel, frames in grouped_channels(animator).items():
            pose[(animator_uuid, channel)] = sample_channel(frames, frame)
    return pose


def max_pose_difference(
    left: dict[tuple[str, str], list[list[float]]],
    right: dict[tuple[str, str], list[list[float]]],
) -> float:
    if left.keys() != right.keys():
        raise ValueError("Derived attack pose channel sets differ")
    maximum = 0.0
    for key in left:
        left_points = left[key]
        right_points = right[key]
        if len(left_points) != len(right_points):
            raise ValueError(f"Derived attack data-point counts differ for {key}")
        for left_point, right_point in zip(left_points, right_points):
            for axis in range(3):
                maximum = max(maximum, abs(left_point[axis] - right_point[axis]))
    return maximum


def animation_frame_length(animation: dict[str, Any]) -> int:
    return source_frame_index(animation.get("length", 0.0))


def validate_source_attack(animation: dict[str, Any], expected_frames: int) -> None:
    if animation.get("loop") != "once":
        raise ValueError(f"{animation['name']} must be authored as a once animation")
    if int(animation.get("snapping", FPS)) != FPS:
        raise ValueError(f"{animation['name']} must use {FPS} FPS snapping")
    if animation_frame_length(animation) != expected_frames:
        raise ValueError(
            f"{animation['name']} is {animation_frame_length(animation)} frames; expected {expected_frames}"
        )
    for animator in animation.get("animators", {}).values():
        for frame in animator.get("keyframes", []):
            source_frame_index(frame.get("time", 0.0))


def validate_result(
    model: dict[str, Any],
    *,
    non_attack_hashes: dict[str, str],
    model_without_animations_hash: str,
) -> dict[str, Any]:
    animations = model.get("animations", [])
    names = [animation.get("name") for animation in animations]
    if len(names) != len(set(names)):
        raise ValueError("Roman animation names are not unique")
    by_name = {animation["name"]: animation for animation in animations}

    expected_frames = {
        "animation.roman_legionary.attack_1": RECOVER_END_FRAME,
        "animation.roman_legionary.attack_2": FOLLOW_END_FRAME,
        "animation.roman_legionary.attack_first": FIRST_END_FRAME,
        "animation.roman_legionary.attack_recover": RECOVER_END_FRAME - FIRST_END_FRAME,
        "animation.roman_legionary.attack_follow": FOLLOW_END_FRAME - FIRST_END_FRAME,
    }
    for name, frame_length in expected_frames.items():
        animation = by_name.get(name)
        if animation is None:
            raise ValueError(f"Roman animation missing after split: {name}")
        if animation_frame_length(animation) != frame_length:
            raise ValueError(f"{name} has the wrong frame length")

    for name, expected_hash in non_attack_hashes.items():
        if canonical_hash(by_name[name]) != expected_hash:
            raise ValueError(f"Unrelated Roman animation changed: {name}")

    without_animations = copy.deepcopy(model)
    without_animations.pop("animations", None)
    if canonical_hash(without_animations) != model_without_animations_hash:
        raise ValueError("Roman geometry, rig, texture, or metadata changed during attack split")

    groups = {entry.get("uuid"): entry.get("name") for entry in model.get("groups", [])}
    non_attack_uuids: set[str] = set()
    attack_uuids: list[str] = []
    for animation in animations:
        uuid_bucket = attack_uuids if animation["name"] in ATTACK_NAMES else None
        if uuid_bucket is not None:
            uuid_bucket.append(animation["uuid"])
        else:
            non_attack_uuids.add(animation["uuid"])
        for animator_uuid, animator in animation.get("animators", {}).items():
            if groups.get(animator_uuid) in CODE_OWNED_BONES and animator.get("keyframes"):
                raise ValueError(f"{animation['name']} controls code-owned bone {groups[animator_uuid]}")
            if (
                animation["name"] in DERIVED_NAMES
                and groups.get(animator_uuid) in ATTACK_LOCOMOTION_BONES
                and animator.get("keyframes")
            ):
                raise ValueError(
                    f"{animation['name']} controls locomotion-layer bone {groups[animator_uuid]}"
                )
            channel_times: dict[str, list[float]] = {}
            for frame in animator.get("keyframes", []):
                if uuid_bucket is not None:
                    uuid_bucket.append(frame["uuid"])
                else:
                    non_attack_uuids.add(frame["uuid"])
                channel_times.setdefault(frame["channel"], []).append(float(frame["time"]))
            if animation["name"] in DERIVED_NAMES:
                for channel, times in channel_times.items():
                    if times != sorted(set(times)):
                        raise ValueError(f"{animation['name']} {channel} keys are not strictly increasing")
                    if times and (times[0] < 0.0 or times[-1] > float(animation["length"]) + FRAME_EPSILON):
                        raise ValueError(f"{animation['name']} {channel} contains an out-of-range key")
                    for time in times:
                        source_frame_index(time)
                if any(
                    frame.get("interpolation") != "linear"
                    for frame in animator.get("keyframes", [])
                ):
                    raise ValueError(f"{animation['name']} still contains non-linear keys")
    if len(attack_uuids) != len(set(attack_uuids)):
        raise ValueError("Imported/derived Roman attack UUIDs are not unique")
    collisions = sorted(set(attack_uuids).intersection(non_attack_uuids))
    if collisions:
        raise ValueError(f"Roman attack UUIDs collide with preserved animations: {collisions}")

    first = by_name["animation.roman_legionary.attack_first"]
    recover = by_name["animation.roman_legionary.attack_recover"]
    follow = by_name["animation.roman_legionary.attack_follow"]
    first_end = pose_at_frame(first, FIRST_END_FRAME)
    recover_start = pose_at_frame(recover, 0)
    follow_start = pose_at_frame(follow, 0)
    recover_difference = max_pose_difference(first_end, recover_start)
    follow_difference = max_pose_difference(first_end, follow_start)
    if recover_difference > VALUE_EPSILON or follow_difference > VALUE_EPSILON:
        raise ValueError("Roman attack branch poses do not match exactly")

    return {
        "animation_count": len(animations),
        "first_frames": FIRST_END_FRAME,
        "recover_frames": RECOVER_END_FRAME - FIRST_END_FRAME,
        "follow_frames": FOLLOW_END_FRAME - FIRST_END_FRAME,
        "first_to_recover_max_pose_difference": recover_difference,
        "first_to_follow_max_pose_difference": follow_difference,
    }


def main() -> None:
    args = parse_args()
    base = read_json(args.output)
    delivery = read_json(args.input)
    assert_compatible_geometry(delivery, base)
    assert_compatible_animation_rig(delivery, base)
    canonicalize_import(delivery, base)
    update_bbmodel(delivery)

    source_by_name = {
        animation.get("name"): animation for animation in delivery.get("animations", [])
    }
    attack_1 = source_by_name.get(ANIMATION_NAMES["attack_1"])
    attack_2 = source_by_name.get(ANIMATION_NAMES["attack_2"])
    if attack_1 is None or attack_2 is None:
        raise ValueError("The Roman delivery must contain attack_1 and attack_2")
    validate_source_attack(attack_1, RECOVER_END_FRAME)
    validate_source_attack(attack_2, FOLLOW_END_FRAME)
    if attack_1.get("animators", {}).keys() != attack_2.get("animators", {}).keys():
        raise ValueError("Roman attack_1 and attack_2 use different animator sets")
    if pose_at_frame(attack_1, 0).keys() != pose_at_frame(attack_2, 0).keys():
        raise ValueError("Roman attack_1 and attack_2 use different channel sets")

    base_animations = base.get("animations", [])
    non_attack_hashes = {
        animation["name"]: canonical_hash(animation)
        for animation in base_animations
        if animation.get("name") not in ATTACK_NAMES
    }
    model_without_animations = copy.deepcopy(base)
    model_without_animations.pop("animations", None)
    model_without_animations_hash = canonical_hash(model_without_animations)

    group_uuids_by_name = {
        entry.get("name"): entry.get("uuid") for entry in base.get("groups", [])
    }
    missing_locomotion_bones = ATTACK_LOCOMOTION_BONES - group_uuids_by_name.keys()
    if missing_locomotion_bones:
        raise ValueError(
            "Roman rig is missing attack locomotion-layer bones: "
            + ", ".join(sorted(missing_locomotion_bones))
        )
    attack_locomotion_uuids = {
        group_uuids_by_name[name] for name in ATTACK_LOCOMOTION_BONES
    }

    canonical_branch_pose = pose_at_frame(attack_2, FIRST_END_FRAME)
    attack_first = make_baked_segment(
        attack_2,
        name="animation.roman_legionary.attack_first",
        start_frame=0,
        end_frame=FIRST_END_FRAME,
        excluded_animator_uuids=attack_locomotion_uuids,
    )
    attack_follow = make_baked_segment(
        attack_2,
        name="animation.roman_legionary.attack_follow",
        start_frame=FIRST_END_FRAME,
        end_frame=FOLLOW_END_FRAME,
        excluded_animator_uuids=attack_locomotion_uuids,
    )
    attack_recover = make_baked_segment(
        attack_1,
        name="animation.roman_legionary.attack_recover",
        start_frame=FIRST_END_FRAME,
        end_frame=RECOVER_END_FRAME,
        recovery_anchor=canonical_branch_pose,
        excluded_animator_uuids=attack_locomotion_uuids,
    )

    base["animations"] = [
        animation
        for animation in base_animations
        if animation.get("name") not in ATTACK_NAMES
    ]
    base["animations"].extend(
        [reuuid_animation(attack_1), reuuid_animation(attack_2), attack_first, attack_recover, attack_follow]
    )
    update_bbmodel(base)
    report = validate_result(
        base,
        non_attack_hashes=non_attack_hashes,
        model_without_animations_hash=model_without_animations_hash,
    )
    write_json(args.output, base)
    print(
        json.dumps(
            {"input": str(args.input), "output": str(args.output), **report},
            ensure_ascii=False,
            indent=2,
        )
    )


if __name__ == "__main__":
    main()
