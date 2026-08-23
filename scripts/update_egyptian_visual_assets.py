#!/usr/bin/env python3
"""Normalize the Egyptian Archer source and export GeckoLib runtime assets."""

from __future__ import annotations

import base64
import copy
import importlib.util
import math
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
    "backstep_shoot": "animation.egyptian_archer.backstep_shoot",
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
} | {
    f"animation.egyptian_archer.bow_recover_{layer}"
    for layer in ("upper", "lower")
} | {
    "animation.egyptian_archer.reload_bow_upper",
    "animation.egyptian_archer.draw_bow_nock_upper",
    "animation.egyptian_archer.reload_bow_nock_upper",
    "animation.egyptian_archer.draw_bow_pull_upper",
    "animation.egyptian_archer.reload_bow_pull_upper",
    "animation.egyptian_archer.bow_aim_upper",
    "animation.egyptian_archer.un_nock_upper",
    "animation.egyptian_archer.bow_lower_upper",
    "animation.egyptian_archer.melee_attack_upper",
}

SHOOT_READY_TIME = 0.33333
SHOOT_RELEASE_START = 0.08333
RELOAD_SOURCE_START = 0.16667
RELOAD_BLEND_IN = 0.15
DRAW_PULL_START = 1.70833
RIGHT_RELOAD_BONES = {"Arm_Right", "Upper Right Arm", "Lower Right Arm", "bone7"}
RELOAD_HAND_BONE = "Lower Right Arm"
RELOAD_ARROW_BONE = "bone7"
RELOAD_ARROW_VISIBLE_AT = 0.70833
RELOAD_ARROW_BAKE_STEP = 1.0 / 48.0
RELOAD_ARROW_VALIDATION_STEP = 1.0 / 240.0
UNNOCK_BAKE_STEP = 1.0 / 48.0


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

    melee = animation_map[NAMES["melee_attack"]]
    melee_upper = copy.deepcopy(melee)
    melee_upper["bones"] = {
        bone: channels for bone, channels in melee.get("bones", {}).items()
        if bone not in LOWER_BODY_BONES
    }
    animation_map["animation.egyptian_archer.melee_attack_upper"] = melee_upper


def keyframe_pose(keyframe):
    if isinstance(keyframe, dict):
        if "post" in keyframe:
            return keyframe_pose(keyframe["post"])
        if "pre" in keyframe:
            return keyframe_pose(keyframe["pre"])
        if "vector" in keyframe:
            return keyframe["vector"]
    return keyframe


def neutral_keyframe(keyframe):
    result = copy.deepcopy(keyframe)
    if isinstance(result, dict):
        result.pop("lerp_mode", None)
    return result


def time_key(time: float) -> str:
    return f"{time:.5f}".rstrip("0").rstrip(".")


def interpolate_pose(first, second, amount: float):
    first_pose = keyframe_pose(first)
    second_pose = keyframe_pose(second)
    if isinstance(first_pose, list) and isinstance(second_pose, list) \
            and len(first_pose) == len(second_pose):
        vector = [a + (b - a) * amount for a, b in zip(first_pose, second_pose)]
        return {"post": {"vector": vector}}
    return neutral_keyframe(first if amount < 0.5 else second)


def catmull_rom_pose(before, first, second, after, amount: float):
    before_pose = keyframe_pose(before)
    first_pose = keyframe_pose(first)
    second_pose = keyframe_pose(second)
    after_pose = keyframe_pose(after)
    if not all(isinstance(pose, list) for pose in (before_pose, first_pose, second_pose, after_pose)):
        return interpolate_pose(first, second, amount)
    if len({len(before_pose), len(first_pose), len(second_pose), len(after_pose)}) != 1:
        return interpolate_pose(first, second, amount)

    amount_squared = amount * amount
    amount_cubed = amount_squared * amount
    vector = []
    for p0, p1, p2, p3 in zip(before_pose, first_pose, second_pose, after_pose):
        vector.append(0.5 * (2.0 * p1 + (p2 - p0) * amount
                             + (2.0 * p0 - 5.0 * p1 + 4.0 * p2 - p3) * amount_squared
                             + (-p0 + 3.0 * p1 - 3.0 * p2 + p3) * amount_cubed))
    return {"post": {"vector": vector}}


def sample_channel(keyframes: dict, time: float):
    ordered = sorted(((float(key), value) for key, value in keyframes.items()), key=lambda entry: entry[0])
    if time <= ordered[0][0]:
        return neutral_keyframe(ordered[0][1])
    if time >= ordered[-1][0]:
        return neutral_keyframe(ordered[-1][1])
    for index in range(1, len(ordered)):
        previous_time, previous = ordered[index - 1]
        next_time, following = ordered[index]
        if time <= next_time:
            span = next_time - previous_time
            amount = 0.0 if span <= 1.0e-6 else (time - previous_time) / span
            interpolation = following.get("lerp_mode", following.get("easing")) \
                if isinstance(following, dict) else None
            if interpolation == "catmullrom":
                before = ordered[index - 2][1] if index >= 2 else previous
                after = ordered[index + 1][1] if index + 1 < len(ordered) else following
                return catmull_rom_pose(before, previous, following, after, amount)
            return interpolate_pose(previous, following, amount)
    return neutral_keyframe(ordered[-1][1])


def sample_vector(animation: dict, bone: str, channel: str, time: float, fallback: list[float]) -> list[float]:
    keyframes = animation.get("bones", {}).get(bone, {}).get(channel)
    if not isinstance(keyframes, dict) or not keyframes:
        return list(fallback)
    pose = keyframe_pose(sample_channel(keyframes, time))
    return [float(value) for value in pose] if isinstance(pose, list) else list(fallback)


def matrix_identity() -> list[list[float]]:
    return [
        [1.0, 0.0, 0.0, 0.0],
        [0.0, 1.0, 0.0, 0.0],
        [0.0, 0.0, 1.0, 0.0],
        [0.0, 0.0, 0.0, 1.0],
    ]


def matrix_multiply(first: list[list[float]], second: list[list[float]]) -> list[list[float]]:
    return [[sum(first[row][index] * second[index][column] for index in range(4))
             for column in range(4)] for row in range(4)]


def translation_matrix(x: float, y: float, z: float) -> list[list[float]]:
    result = matrix_identity()
    result[0][3], result[1][3], result[2][3] = x, y, z
    return result


def rotation_matrix(rotation: list[float]) -> list[list[float]]:
    x, y, z = (math.radians(value) for value in rotation)
    cx, sx = math.cos(x), math.sin(x)
    cy, sy = math.cos(y), math.sin(y)
    cz, sz = math.cos(z), math.sin(z)
    rotate_x = [[1, 0, 0, 0], [0, cx, -sx, 0], [0, sx, cx, 0], [0, 0, 0, 1]]
    rotate_y = [[cy, 0, sy, 0], [0, 1, 0, 0], [-sy, 0, cy, 0], [0, 0, 0, 1]]
    rotate_z = [[cz, -sz, 0, 0], [sz, cz, 0, 0], [0, 0, 1, 0], [0, 0, 0, 1]]
    return matrix_multiply(matrix_multiply(rotate_z, rotate_y), rotate_x)


def rigid_inverse(matrix: list[list[float]]) -> list[list[float]]:
    result = matrix_identity()
    for row in range(3):
        for column in range(3):
            result[row][column] = matrix[column][row]
    translation = [matrix[row][3] for row in range(3)]
    for row in range(3):
        result[row][3] = -sum(result[row][column] * translation[column] for column in range(3))
    return result


def bone_local_matrix(bone: dict, animation: dict, time: float) -> list[list[float]]:
    position = sample_vector(animation, bone["name"], "position", time, [0.0, 0.0, 0.0])
    animated_rotation = sample_vector(animation, bone["name"], "rotation", time, [0.0, 0.0, 0.0])
    base_source = [float(value) for value in bone.get("rotation", [0.0, 0.0, 0.0])]
    # GeckoLib bakes Bedrock/Blockbench X and Y rotations with inverted signs,
    # and mirrors the geometry pivot's X coordinate before rendering. The old
    # retarget validator used source-space signs, so it only proved its own
    # incorrect matrix convention and produced the visible second-shot offset.
    base_rotation = [-base_source[0], -base_source[1], base_source[2]]
    animation_rotation = [-animated_rotation[0], -animated_rotation[1], animated_rotation[2]]
    rotation = [base_rotation[index] + animation_rotation[index] for index in range(3)]
    source_pivot = [float(value) for value in bone.get("pivot", [0.0, 0.0, 0.0])]
    pivot = [-source_pivot[0] / 16.0, source_pivot[1] / 16.0, source_pivot[2] / 16.0]
    translated = translation_matrix(-position[0] / 16.0, position[1] / 16.0, position[2] / 16.0)
    to_pivot = translation_matrix(*pivot)
    from_pivot = translation_matrix(*(-value for value in pivot))
    return matrix_multiply(matrix_multiply(matrix_multiply(translated, to_pivot), rotation_matrix(rotation)), from_pivot)


def bone_world_matrix(animation: dict, bone_name: str, bones: dict[str, dict], time: float) -> list[list[float]]:
    lineage = []
    bone = bones[bone_name]
    while bone is not None:
        lineage.append(bone)
        parent_name = bone.get("parent")
        bone = bones.get(parent_name) if parent_name else None
    result = matrix_identity()
    for ancestor in reversed(lineage):
        result = matrix_multiply(result, bone_local_matrix(ancestor, animation, time))
    return result


def unwrap_degrees(value: float, reference: float) -> float:
    while value - reference > 180.0:
        value -= 360.0
    while value - reference < -180.0:
        value += 360.0
    return value


def decompose_root_bone(matrix: list[list[float]], bone: dict, reference_rotation: list[float]) \
        -> tuple[list[float], list[float]]:
    # GeckoLib applies rotations as Rz * Ry * Rx. Recover that exact order so
    # the root-level held arrow can be retargeted into the reload hand's frame.
    y = math.asin(max(-1.0, min(1.0, -matrix[2][0])))
    if abs(math.cos(y)) > 1.0e-6:
        x = math.atan2(matrix[2][1], matrix[2][2])
        z = math.atan2(matrix[1][0], matrix[0][0])
    else:
        x = math.atan2(-matrix[1][2], matrix[1][1])
        z = 0.0
    total_rotation = [math.degrees(x), math.degrees(y), math.degrees(z)]
    base_source = [float(value) for value in bone.get("rotation", [0.0, 0.0, 0.0])]
    base_rotation = [-base_source[0], -base_source[1], base_source[2]]
    gecko_animation_rotation = [total_rotation[index] - base_rotation[index] for index in range(3)]
    source_animation_rotation = [
        -gecko_animation_rotation[0],
        -gecko_animation_rotation[1],
        gecko_animation_rotation[2],
    ]
    animated_rotation = [unwrap_degrees(source_animation_rotation[index], reference_rotation[index])
                         for index in range(3)]

    source_pivot = [float(value) for value in bone.get("pivot", [0.0, 0.0, 0.0])]
    pivot = [-source_pivot[0] / 16.0, source_pivot[1] / 16.0, source_pivot[2] / 16.0]
    rotated_pivot = [sum(matrix[row][column] * pivot[column] for column in range(3)) for row in range(3)]
    animation_translation = [matrix[row][3] - pivot[row] + rotated_pivot[row] for row in range(3)]
    animated_position = [
        -animation_translation[0] * 16.0,
        animation_translation[1] * 16.0,
        animation_translation[2] * 16.0,
    ]
    return animated_position, animated_rotation


def retarget_reload_arrow(draw: dict, reload_animation: dict, geometry: dict, right_offset: float) -> None:
    """Keep the root-level held arrow attached to the animated right hand.

    The modeler's held arrow (bone7) is a top-level bone, while the right hand
    inherits Main -> Upper_Body -> Upper_Body2 -> Arm -> Arm_Right. Reloading
    deliberately keeps the bow-side torso in its ready pose, so blindly copying
    bone7's authored global keyframes separates it from the hand. Bake the
    original hand-relative arrow transform into the reload animation instead.
    """
    bones = {bone["name"]: bone for bone in geometry["minecraft:geometry"][0]["bones"]}
    arrow_bone = bones[RELOAD_ARROW_BONE]
    position_frames: dict[str, dict] = {}
    rotation_frames: dict[str, dict] = {}
    validation_samples: list[tuple[float, float]] = []
    source_end = float(draw["animation_length"])
    source_time = RELOAD_ARROW_VISIBLE_AT
    sample_times = []
    while source_time < source_end - 1.0e-6:
        sample_times.append(source_time)
        source_time += RELOAD_ARROW_BAKE_STEP
    sample_times.append(source_end)

    for source_time in sample_times:
        reload_time = source_time + right_offset
        source_hand = bone_world_matrix(draw, RELOAD_HAND_BONE, bones, source_time)
        reload_hand = bone_world_matrix(reload_animation, RELOAD_HAND_BONE, bones, reload_time)
        source_arrow = bone_world_matrix(draw, RELOAD_ARROW_BONE, bones, source_time)
        retargeted_arrow = matrix_multiply(
            matrix_multiply(reload_hand, rigid_inverse(source_hand)), source_arrow)
        reference_rotation = sample_vector(draw, RELOAD_ARROW_BONE, "rotation", source_time, [0.0, 0.0, 0.0])
        position, rotation = decompose_root_bone(retargeted_arrow, arrow_bone, reference_rotation)
        verification_animation = {"bones": {RELOAD_ARROW_BONE: {
            "position": {"0": {"post": {"vector": position}}},
            "rotation": {"0": {"post": {"vector": rotation}}},
        }}}
        rebuilt_arrow = bone_local_matrix(arrow_bone, verification_animation, 0.0)
        matrix_error = max(abs(retargeted_arrow[row][column] - rebuilt_arrow[row][column])
                           for row in range(4) for column in range(4))
        if matrix_error > 1.0e-4:
            raise ValueError(f"Reload arrow retarget decomposition drifted by {matrix_error:.6f}")
        key = time_key(reload_time)
        position_frames[key] = {"post": {"vector": [round(value, 5) for value in position]}}
        rotation_frames[key] = {"post": {"vector": [round(value, 5) for value in rotation]}}
        validation_samples.append((source_time, float(key)))

    reload_arrow = reload_animation["bones"][RELOAD_ARROW_BONE]
    reload_arrow["position"] = position_frames
    reload_arrow["rotation"] = rotation_frames
    for source_time, reload_time in validation_samples:
        source_relative = matrix_multiply(
            rigid_inverse(bone_world_matrix(draw, RELOAD_HAND_BONE, bones, source_time)),
            bone_world_matrix(draw, RELOAD_ARROW_BONE, bones, source_time))
        reload_relative = matrix_multiply(
            rigid_inverse(bone_world_matrix(reload_animation, RELOAD_HAND_BONE, bones, reload_time)),
            bone_world_matrix(reload_animation, RELOAD_ARROW_BONE, bones, reload_time))
        relative_error = max(abs(source_relative[row][column] - reload_relative[row][column])
                             for row in range(4) for column in range(4))
        if relative_error > 2.0e-3:
            raise ValueError(f"Reload arrow left the hand frame by {relative_error:.6f}")

    worst_interpolated_error = 0.0
    reload_time = RELOAD_ARROW_VISIBLE_AT + right_offset
    while reload_time <= float(reload_animation["animation_length"]) + 1.0e-6:
        source_time = reload_time - right_offset
        source_relative = matrix_multiply(
            rigid_inverse(bone_world_matrix(draw, RELOAD_HAND_BONE, bones, source_time)),
            bone_world_matrix(draw, RELOAD_ARROW_BONE, bones, source_time))
        reload_relative = matrix_multiply(
            rigid_inverse(bone_world_matrix(reload_animation, RELOAD_HAND_BONE, bones, reload_time)),
            bone_world_matrix(reload_animation, RELOAD_ARROW_BONE, bones, reload_time))
        worst_interpolated_error = max(worst_interpolated_error, max(
            abs(source_relative[row][column] - reload_relative[row][column])
            for row in range(4) for column in range(4)))
        reload_time += RELOAD_ARROW_VALIDATION_STEP
    if worst_interpolated_error > 1.0e-2:
        raise ValueError(f"Reload arrow interpolation left the hand frame by {worst_interpolated_error:.6f}")


def clip_animation(source: dict, start: float, end: float) -> dict:
    clipped = {
        "animation_length": end - start,
        "loop": "once",
        "bones": {},
    }
    for bone, channels in source.get("bones", {}).items():
        clipped_channels = {}
        for channel, keyframes in channels.items():
            if not isinstance(keyframes, dict) or not keyframes:
                clipped_channels[channel] = copy.deepcopy(keyframes)
                continue
            result = {"0": sample_channel(keyframes, start)}
            for key, value in sorted(keyframes.items(), key=lambda entry: float(entry[0])):
                source_time = float(key)
                if start < source_time < end:
                    result[time_key(source_time - start)] = copy.deepcopy(value)
            result[time_key(end - start)] = sample_channel(keyframes, end)
            clipped_channels[channel] = result
        clipped["bones"][bone] = clipped_channels
    return clipped


def static_pose_animation(source: dict, time: float) -> dict:
    ready = {
        "animation_length": 1.0,
        "loop": True,
        "bones": {},
    }
    for bone, channels in source.get("bones", {}).items():
        ready["bones"][bone] = {
            channel: {"0": sample_channel(keyframes, time)}
            if isinstance(keyframes, dict) and keyframes else copy.deepcopy(keyframes)
            for channel, keyframes in channels.items()
        }
    return ready


def reverse_baked_animation(source: dict, step: float) -> dict:
    """Bake a stable reverse clip instead of relying on reversed spline metadata."""
    duration = float(source["animation_length"])
    reversed_animation = {
        "animation_length": duration,
        "loop": "once",
        "bones": {},
    }
    sample_times = [0.0]
    sample_time = step
    while sample_time < duration - 1.0e-6:
        sample_times.append(sample_time)
        sample_time += step
    sample_times.append(duration)
    for bone, channels in source.get("bones", {}).items():
        reversed_channels = {}
        for channel, keyframes in channels.items():
            if not isinstance(keyframes, dict) or not keyframes:
                reversed_channels[channel] = copy.deepcopy(keyframes)
                continue
            reversed_channels[channel] = {
                time_key(time): sample_channel(keyframes, duration - time)
                for time in sample_times
            }
        reversed_animation["bones"][bone] = reversed_channels
    return reversed_animation


def normalize_held_arrow_visibility(model: dict) -> None:
    """Prevent the held arrow's visibility curve from scaling past one."""
    for animation in model.get("animations", []):
        if animation.get("name") != NAMES["draw_bow"]:
            continue
        for animator in animation.get("animators", {}).values():
            if animator.get("name") != RELOAD_ARROW_BONE:
                continue
            for keyframe in animator.get("keyframes", []):
                if keyframe.get("channel") == "scale":
                    keyframe["interpolation"] = "linear"


def add_reload_animation(animation_map: dict, ready_source: dict, geometry: dict) -> tuple[dict, float]:
    draw = animation_map[f"{NAMES['draw_bow']}_upper"]
    reload_length = float(draw["animation_length"]) - RELOAD_SOURCE_START + RELOAD_BLEND_IN
    right_offset = RELOAD_BLEND_IN - RELOAD_SOURCE_START
    left_draw_start = DRAW_PULL_START + right_offset
    reload_animation = {
        "animation_length": reload_length,
        "loop": "hold_on_last_frame",
        "bones": {},
    }
    for bone, channels in draw.get("bones", {}).items():
        ready_channels = ready_source.get("bones", {}).get(bone, {})
        built_channels = {}
        for channel, keyframes in channels.items():
            if not isinstance(keyframes, dict) or not keyframes:
                built_channels[channel] = copy.deepcopy(keyframes)
                continue
            ready_keyframes = ready_channels.get(channel)
            ready_pose = sample_channel(ready_keyframes, SHOOT_READY_TIME) \
                if isinstance(ready_keyframes, dict) and ready_keyframes else sample_channel(keyframes, 0.0)
            result = {"0": ready_pose}
            if bone in RIGHT_RELOAD_BONES:
                result[time_key(RELOAD_BLEND_IN)] = sample_channel(keyframes, RELOAD_SOURCE_START)
                for key, value in sorted(keyframes.items(), key=lambda entry: float(entry[0])):
                    source_time = float(key)
                    if source_time > RELOAD_SOURCE_START:
                        result[time_key(source_time + right_offset)] = copy.deepcopy(value)
            else:
                # The bow side stays in the post-shot ready pose while the right
                # hand retrieves and mounts the next arrow. Only after the arrow
                # reaches the string does it blend directly toward the final full
                # draw. Sampling the first-draw timeline here made the left arm
                # visibly snap back to its initial pose on every second shot.
                result[time_key(left_draw_start)] = copy.deepcopy(ready_pose)
                result[time_key(reload_length)] = sample_channel(keyframes, float(draw["animation_length"]))
            built_channels[channel] = result
        reload_animation["bones"][bone] = built_channels
    retarget_reload_arrow(draw, reload_animation, geometry, right_offset)
    animation_map["animation.egyptian_archer.reload_bow_upper"] = reload_animation
    return reload_animation, left_draw_start


def add_post_shot_layers(animations: dict, geometry: dict) -> None:
    """Build semantic nock, draw, aim, release, un-nock, and lower clips."""
    animation_map = animations.get("animations", {})
    first_draw_upper = copy.deepcopy(animation_map[f"{NAMES['draw_bow']}_upper"])
    original_shoot_upper = copy.deepcopy(animation_map[f"{NAMES['shoot']}_upper"])
    reload_upper, reload_pull_start = add_reload_animation(animation_map, original_shoot_upper, geometry)

    animation_map["animation.egyptian_archer.draw_bow_nock_upper"] = clip_animation(
        first_draw_upper, 0.0, DRAW_PULL_START)
    animation_map["animation.egyptian_archer.draw_bow_pull_upper"] = clip_animation(
        first_draw_upper, DRAW_PULL_START, float(first_draw_upper["animation_length"]))
    reload_nock = clip_animation(reload_upper, 0.0, reload_pull_start)
    animation_map["animation.egyptian_archer.reload_bow_nock_upper"] = reload_nock
    animation_map["animation.egyptian_archer.reload_bow_pull_upper"] = clip_animation(
        reload_upper, reload_pull_start, float(reload_upper["animation_length"]))
    animation_map["animation.egyptian_archer.bow_aim_upper"] = static_pose_animation(
        first_draw_upper, float(first_draw_upper["animation_length"]))
    animation_map["animation.egyptian_archer.un_nock_upper"] = reverse_baked_animation(
        reload_nock, UNNOCK_BAKE_STEP)
    animation_map[f"{NAMES['shoot']}_upper"] = clip_animation(
        original_shoot_upper, SHOOT_RELEASE_START, SHOOT_READY_TIME)
    animation_map["animation.egyptian_archer.bow_lower_upper"] = clip_animation(
        original_shoot_upper, SHOOT_READY_TIME, float(original_shoot_upper["animation_length"]))


def phase_boundary_error(first: dict, second: dict, ignored_bones: set[str] | None = None) -> float:
    ignored = ignored_bones or set()
    defaults = {"position": [0.0, 0.0, 0.0], "rotation": [0.0, 0.0, 0.0], "scale": [1.0, 1.0, 1.0]}
    worst = 0.0
    bones = set(first.get("bones", {})) | set(second.get("bones", {}))
    for bone in bones - ignored:
        channels = set(first.get("bones", {}).get(bone, {})) \
            | set(second.get("bones", {}).get(bone, {}))
        for channel in channels:
            fallback = defaults.get(channel, [0.0, 0.0, 0.0])
            first_pose = sample_vector(first, bone, channel, float(first["animation_length"]), fallback)
            second_pose = sample_vector(second, bone, channel, 0.0, fallback)
            worst = max(worst, max(abs(left - right) for left, right in zip(first_pose, second_pose)))
    return worst


def validate_phase_animations(animation_map: dict) -> None:
    names = {
        short_name: animation_map[f"animation.egyptian_archer.{short_name}"]
        for short_name in (
            "draw_bow_nock_upper", "draw_bow_pull_upper", "reload_bow_nock_upper",
            "reload_bow_pull_upper", "bow_aim_upper", "shoot_upper", "un_nock_upper",
            "bow_lower_upper",
        )
    }
    boundaries = (
        ("draw_bow_nock_upper", "draw_bow_pull_upper", set()),
        ("reload_bow_nock_upper", "reload_bow_pull_upper", set()),
        ("draw_bow_pull_upper", "bow_aim_upper", set()),
        ("bow_aim_upper", "shoot_upper", set()),
        # The held arrow is scale-zero at both sides of this boundary; its hidden
        # transform can differ without producing a visible discontinuity.
        ("un_nock_upper", "bow_lower_upper", {RELOAD_ARROW_BONE}),
    )
    for first_name, second_name, ignored_bones in boundaries:
        error = phase_boundary_error(names[first_name], names[second_name], ignored_bones)
        if error > 1.0e-4:
            raise ValueError(f"Animation phase boundary {first_name}->{second_name} drifted by {error:.6f}")

    for animation_name in (f"{NAMES['draw_bow']}_upper", "animation.egyptian_archer.reload_bow_upper"):
        animation = animation_map[animation_name]
        time = 0.0
        while time <= float(animation["animation_length"]) + 1.0e-6:
            scale = sample_vector(animation, RELOAD_ARROW_BONE, "scale", time, [1.0, 1.0, 1.0])
            if min(scale) < -1.0e-5 or max(scale) > 1.00001:
                raise ValueError(f"Held arrow visibility scale overshot in {animation_name} at {time:.5f}: {scale}")
            time += RELOAD_ARROW_VALIDATION_STEP

    release_scale = sample_vector(names["shoot_upper"], RELOAD_ARROW_BONE, "scale",
                                  float(names["shoot_upper"]["animation_length"]) * 0.5,
                                  [1.0, 1.0, 1.0])
    if max(release_scale) > 1.0e-4:
        raise ValueError(f"Held arrow has not disappeared at the release midpoint: {release_scale}")


def add_bow_recovery_layers(animations: dict) -> None:
    animation_map = animations.get("animations", {})
    for layer in ("upper", "lower"):
        source = animation_map[f"{NAMES['draw_bow']}_{layer}"]
        recovery = {
            "animation_length": 0.4,
            "loop": "once",
            "bones": {},
        }
        for bone, channels in source.get("bones", {}).items():
            recovered_channels = {}
            for channel, keyframes in channels.items():
                if not isinstance(keyframes, dict) or not keyframes:
                    recovered_channels[channel] = copy.deepcopy(keyframes)
                    continue
                ordered = sorted(keyframes.items(), key=lambda entry: float(entry[0]))
                recovered_channels[channel] = {
                    "0.0": copy.deepcopy(ordered[-1][1]),
                    "0.4": copy.deepcopy(ordered[0][1]),
                }
            recovery["bones"][bone] = recovered_channels
        animation_map[f"animation.egyptian_archer.bow_recover_{layer}"] = recovery


def main() -> int:
    model = exporter.read_json(BBMODEL)
    model["name"] = "egyptian_archer_echo"
    model["model_identifier"] = "egyptian_archer_echo"
    for animation in model.get("animations", []):
        animation["name"] = NAMES.get(animation.get("name"), animation.get("name"))
        if animation["name"] in {
                "animation.egyptian_archer.shoot",
                "animation.egyptian_archer.backstep_shoot",
        }:
            animation["loop"] = "once"
    normalize_held_arrow_visibility(model)
    geometry = exporter.export_geometry(model)
    geometry["minecraft:geometry"][0]["description"]["identifier"] = "geometry.egyptian_archer"
    animations = exporter.export_animations(model)
    add_ranged_animation_layers(animations)
    add_post_shot_layers(animations, geometry)
    add_bow_recovery_layers(animations)
    validate_phase_animations(animations.get("animations", {}))
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
