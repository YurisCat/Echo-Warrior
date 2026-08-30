#!/usr/bin/env python3
"""Export the hand-authored knowledge reader backgrounds and controls.

The two Aseprite sources use a 256x256 working canvas so the artist can compare
them directly with Minecraft's vanilla book texture. Runtime assets are cropped
without resampling, while their original canvas positions are printed for the
screen layout constants.
"""

from __future__ import annotations

import argparse
from pathlib import Path

from PIL import Image

from export_aseprite_rgba import (
    apply_opacity,
    composite_layer,
    connected_alpha_components,
    inherited_visibility,
    parse,
)


CONTROL_LAYERS = {"短按钮合集", "关闭按钮"}
BUTTON_SHADOW_RGB = (82, 61, 45)
BUTTON_SHADOW_OPACITY = 0.65


def render_background(source: Path) -> tuple[Image.Image, tuple[int, int, int, int]]:
    width, height, layers, cels = parse(source)
    visible = inherited_visibility(layers)
    result = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    for cel in cels:
        layer = layers[cel.layer_index]
        if not visible[cel.layer_index] or layer.layer_type != 0 or layer.name in CONTROL_LAYERS:
            continue
        rendered = apply_opacity(cel.image, cel.opacity * layer.opacity // 255)
        composite_layer(result, rendered, (cel.x, cel.y), layer.blend_mode)
    bounds = result.getbbox()
    if bounds is None:
        raise ValueError(f"No visible background pixels found in {source}")
    return result.crop(bounds), bounds


def layer_canvas(source: Path, layer_name: str) -> Image.Image:
    width, height, layers, cels = parse(source)
    matching = [layer.index for layer in layers if layer.name == layer_name]
    if len(matching) != 1:
        raise ValueError(f"Expected one {layer_name!r} layer in {source}, found {len(matching)}")
    layer_index = matching[0]
    layer = layers[layer_index]
    canvas = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    for cel in cels:
        if cel.layer_index != layer_index:
            continue
        rendered = apply_opacity(cel.image, cel.opacity * layer.opacity // 255)
        canvas.alpha_composite(rendered, (cel.x, cel.y))
    return canvas


def save_crop(image: Image.Image, bounds: tuple[int, int, int, int], destination: Path) -> None:
    image.crop(bounds).save(destination)


def save_control(image: Image.Image, bounds: tuple[int, int, int, int], output: Path, name: str) -> None:
    control = image.crop(bounds)
    control.save(output / f"knowledge_{name}.png")

    shadow = Image.new("RGBA", control.size, (*BUTTON_SHADOW_RGB, 0))
    shadow.putalpha(control.getchannel("A").point(lambda alpha: round(alpha * BUTTON_SHADOW_OPACITY)))
    shadow.save(output / f"knowledge_{name}_shadow.png")


def dimensions(bounds: tuple[int, int, int, int]) -> str:
    return f"{bounds[2] - bounds[0]}x{bounds[3] - bounds[1]}"


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("collection_source", type=Path)
    parser.add_argument("fragment_source", type=Path)
    parser.add_argument("output", type=Path)
    args = parser.parse_args()
    args.output.mkdir(parents=True, exist_ok=True)

    collection, collection_bounds = render_background(args.collection_source)
    fragment, fragment_bounds = render_background(args.fragment_source)
    collection.save(args.output / "knowledge_collection.png")
    fragment.save(args.output / "knowledge_fragment.png")

    controls = layer_canvas(args.collection_source, "短按钮合集")
    control_bounds = connected_alpha_components(controls)
    if len(control_bounds) != 3:
        raise ValueError(f"Expected three controls, found {len(control_bounds)}")
    previous_bounds, next_bounds, extract_bounds = control_bounds
    save_control(controls, previous_bounds, args.output, "previous")
    save_control(controls, next_bounds, args.output, "next")
    save_control(controls, extract_bounds, args.output, "extract")

    close = layer_canvas(args.collection_source, "关闭按钮")
    close_bounds = close.getbbox()
    if close_bounds is None:
        raise ValueError("No visible close button pixels found")
    save_control(close, close_bounds, args.output, "close")

    for name, bounds in (
        ("collection", collection_bounds),
        ("fragment", fragment_bounds),
        ("previous", previous_bounds),
        ("next", next_bounds),
        ("extract", extract_bounds),
        ("close", close_bounds),
    ):
        print(f"{name:10s} position=({bounds[0]},{bounds[1]}) size={dimensions(bounds)}")


if __name__ == "__main__":
    main()
