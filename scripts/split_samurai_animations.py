#!/usr/bin/env python3
"""Create branchable samurai combo animations from the Blockbench source model.

The authored attack animations remain untouched. Three derived clips are added:

* attack_first:   attack_1 from 0.000s through the branch pose.
* attack_recover: attack_1 from the branch pose through its idle recovery.
* attack_follow:  attack_2 from the branch pose through the second slash.

Both suffix clips are forced to begin at the exact terminal pose of attack_first,
so the game can select either branch without a one-frame pose discontinuity.
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


SPLIT_TIME = 0.625
EPSILON = 1.0e-7
DERIVED_NAMES = {"attack_first", "attack_recover", "attack_follow"}


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--input",
        type=Path,
        default=Path(r"E:\Download-Terminal\TencentDownload\日本_追加前后闪避.bbmodel"),
        help="Original Blockbench model",
    )
    parser.add_argument(
        "--output",
        type=Path,
        default=Path("assets-source/blockbench/japanese_samurai_echo.bbmodel"),
        help="Project-owned Blockbench model with derived animation clips",
    )
    return parser.parse_args()


def canonical_hash(value: Any) -> str:
    encoded = json.dumps(
        value, ensure_ascii=False, sort_keys=True, separators=(",", ":")
    ).encode("utf-8")
    return hashlib.sha256(encoded).hexdigest()


def fresh_uuid() -> str:
    return str(uuid.uuid4())


def number_string(value: float) -> str:
    if abs(value) < 1.0e-12:
        value = 0.0
    return format(value, ".12g")


def frame_time(frame: dict[str, Any]) -> float:
    return float(frame.get("time", 0.0))


def clone_frame(frame: dict[str, Any], *, time: float | None = None) -> dict[str, Any]:
    result = copy.deepcopy(frame)
    result["uuid"] = fresh_uuid()
    if time is not None:
        result["time"] = time
    return result


def interpolate_data_points(
    left: list[dict[str, Any]], right: list[dict[str, Any]], ratio: float
) -> list[dict[str, Any]]:
    if len(left) != len(right):
        raise ValueError("Cannot interpolate keyframes with different data-point counts")

    result: list[dict[str, Any]] = []
    for left_point, right_point in zip(left, right):
        point = copy.deepcopy(left_point)
        for axis in ("x", "y", "z"):
            if axis not in left_point or axis not in right_point:
                continue
            left_value = float(left_point[axis])
            right_value = float(right_point[axis])
            point[axis] = number_string(left_value + (right_value - left_value) * ratio)
        result.append(point)
    return result


def evaluate_channel(frames: list[dict[str, Any]], time: float) -> dict[str, Any]:
    ordered = sorted(frames, key=frame_time)
    if not ordered:
        raise ValueError("Cannot evaluate an empty animation channel")

    for frame in ordered:
        if math.isclose(frame_time(frame), time, abs_tol=EPSILON):
            return clone_frame(frame, time=time)

    if time <= frame_time(ordered[0]):
        return clone_frame(ordered[0], time=time)
    if time >= frame_time(ordered[-1]):
        return clone_frame(ordered[-1], time=time)

    for left, right in zip(ordered, ordered[1:]):
        left_time = frame_time(left)
        right_time = frame_time(right)
        if left_time < time < right_time:
            if right.get("interpolation", "linear") != "linear":
                raise ValueError(
                    f"Unsupported interpolation {right.get('interpolation')!r} at {right_time}s"
                )
            ratio = (time - left_time) / (right_time - left_time)
            result = clone_frame(left, time=time)
            result["interpolation"] = "linear"
            result["data_points"] = interpolate_data_points(
                left.get("data_points", []), right.get("data_points", []), ratio
            )
            return result

    raise AssertionError(f"Failed to evaluate channel at {time}s")


def frames_by_channel(animator: dict[str, Any]) -> dict[str, list[dict[str, Any]]]:
    channels: dict[str, list[dict[str, Any]]] = {}
    for frame in animator.get("keyframes", []):
        channels.setdefault(frame["channel"], []).append(frame)
    return channels


def pose_at(animation: dict[str, Any], time: float) -> dict[tuple[str, str], dict[str, Any]]:
    pose: dict[tuple[str, str], dict[str, Any]] = {}
    for animator_uuid, animator in animation.get("animators", {}).items():
        for channel, frames in frames_by_channel(animator).items():
            pose[(animator_uuid, channel)] = evaluate_channel(frames, time)
    return pose


def make_segment(
    source: dict[str, Any],
    *,
    name: str,
    start: float,
    end: float,
    start_pose: dict[tuple[str, str], dict[str, Any]] | None = None,
    end_pose: dict[tuple[str, str], dict[str, Any]] | None = None,
) -> dict[str, Any]:
    result = copy.deepcopy(source)
    result["uuid"] = fresh_uuid()
    result["name"] = name
    result["length"] = end - start
    result["selected"] = False
    result["saved"] = False

    for animator_uuid, animator in result.get("animators", {}).items():
        original_animator = source["animators"][animator_uuid]
        retained = []
        for frame in original_animator.get("keyframes", []):
            time = frame_time(frame)
            if start - EPSILON <= time <= end + EPSILON:
                retained.append(clone_frame(frame, time=max(0.0, time - start)))

        # Boundary poses replace any authored frame at the same instant. This
        # avoids duplicate-time keys and gives both branches one canonical pose.
        if start_pose is not None:
            retained = [frame for frame in retained if frame_time(frame) > EPSILON]
            for (pose_animator_uuid, channel), frame in start_pose.items():
                if pose_animator_uuid == animator_uuid:
                    inserted = clone_frame(frame, time=0.0)
                    inserted["channel"] = channel
                    retained.append(inserted)

        segment_length = end - start
        if end_pose is not None:
            retained = [
                frame
                for frame in retained
                if not math.isclose(frame_time(frame), segment_length, abs_tol=EPSILON)
            ]
            for (pose_animator_uuid, channel), frame in end_pose.items():
                if pose_animator_uuid == animator_uuid:
                    inserted = clone_frame(frame, time=segment_length)
                    inserted["channel"] = channel
                    retained.append(inserted)

        retained.sort(key=lambda frame: (frame_time(frame), frame.get("channel", "")))
        animator["keyframes"] = retained

    return result


def data_point_values(frame: dict[str, Any]) -> tuple[float, ...]:
    values: list[float] = []
    for point in frame.get("data_points", []):
        for axis in ("x", "y", "z"):
            if axis in point:
                values.append(float(point[axis]))
    return tuple(values)


def max_pose_difference(
    left: dict[tuple[str, str], dict[str, Any]],
    right: dict[tuple[str, str], dict[str, Any]],
) -> float:
    if left.keys() != right.keys():
        missing_left = sorted(set(right) - set(left))
        missing_right = sorted(set(left) - set(right))
        raise ValueError(
            f"Pose channels differ; left missing={missing_left}, right missing={missing_right}"
        )

    maximum = 0.0
    for key in left:
        left_values = data_point_values(left[key])
        right_values = data_point_values(right[key])
        if len(left_values) != len(right_values):
            raise ValueError(f"Pose data-point shapes differ for {key}")
        for left_value, right_value in zip(left_values, right_values):
            maximum = max(maximum, abs(left_value - right_value))
    return maximum


def validate(
    source_original_hashes: dict[str, str], model: dict[str, Any]
) -> dict[str, Any]:
    animations = model.get("animations", [])
    names = [animation["name"] for animation in animations]
    if len(names) != len(set(names)):
        raise ValueError("Animation names are not unique")

    by_name = {animation["name"]: animation for animation in animations}
    expected_lengths = {
        "attack_first": SPLIT_TIME,
        "attack_recover": 1.125 - SPLIT_TIME,
        "attack_follow": 1.875 - SPLIT_TIME,
    }
    for name, expected_length in expected_lengths.items():
        animation = by_name[name]
        actual_length = float(animation["length"])
        if not math.isclose(actual_length, expected_length, abs_tol=EPSILON):
            raise ValueError(f"{name} length {actual_length} != {expected_length}")
        for animator in animation.get("animators", {}).values():
            for frame in animator.get("keyframes", []):
                time = frame_time(frame)
                if time < -EPSILON or time > actual_length + EPSILON:
                    raise ValueError(f"{name} has out-of-bounds keyframe at {time}s")

    all_uuids: list[str] = []
    for animation in animations:
        all_uuids.append(animation["uuid"])
        for animator in animation.get("animators", {}).values():
            all_uuids.extend(
                frame["uuid"] for frame in animator.get("keyframes", []) if "uuid" in frame
            )
    if len(all_uuids) != len(set(all_uuids)):
        raise ValueError("Animation/keyframe UUIDs are not globally unique")

    for original_name, original_hash in source_original_hashes.items():
        if canonical_hash(by_name[original_name]) != original_hash:
            raise ValueError(f"Original animation {original_name} was modified")

    first_end = pose_at(by_name["attack_first"], float(by_name["attack_first"]["length"]))
    recover_start = pose_at(by_name["attack_recover"], 0.0)
    follow_start = pose_at(by_name["attack_follow"], 0.0)
    return {
        "animation_count": len(animations),
        "first_to_recover_max_pose_difference": max_pose_difference(first_end, recover_start),
        "first_to_follow_max_pose_difference": max_pose_difference(first_end, follow_start),
        "lengths": expected_lengths,
    }


def main() -> None:
    args = parse_args()
    with args.input.open("r", encoding="utf-8-sig") as source_file:
        model = json.load(source_file)

    animations = model.get("animations", [])
    animations = [animation for animation in animations if animation.get("name") not in DERIVED_NAMES]
    model["animations"] = animations
    by_name = {animation["name"]: animation for animation in animations}
    if "attack_1" not in by_name or "attack_2" not in by_name:
        raise ValueError("The source model must contain attack_1 and attack_2")

    source_original_hashes = {
        animation["name"]: canonical_hash(animation) for animation in animations
    }
    attack_1 = by_name["attack_1"]
    attack_2 = by_name["attack_2"]
    if float(attack_1["length"]) <= SPLIT_TIME or float(attack_2["length"]) <= SPLIT_TIME:
        raise ValueError("Split time must be inside both source animations")

    canonical_branch_pose = pose_at(attack_1, SPLIT_TIME)
    attack_first = make_segment(
        attack_1,
        name="attack_first",
        start=0.0,
        end=SPLIT_TIME,
        end_pose=canonical_branch_pose,
    )
    attack_recover = make_segment(
        attack_1,
        name="attack_recover",
        start=SPLIT_TIME,
        end=float(attack_1["length"]),
        start_pose=canonical_branch_pose,
    )
    attack_follow = make_segment(
        attack_2,
        name="attack_follow",
        start=SPLIT_TIME,
        end=float(attack_2["length"]),
        start_pose=canonical_branch_pose,
    )
    model["animations"].extend([attack_first, attack_recover, attack_follow])

    report = validate(source_original_hashes, model)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    with args.output.open("w", encoding="utf-8", newline="\n") as output_file:
        json.dump(model, output_file, ensure_ascii=False, indent=2)
        output_file.write("\n")

    print(json.dumps({"input": str(args.input), "output": str(args.output), **report}, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
