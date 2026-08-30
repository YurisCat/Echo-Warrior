#!/usr/bin/env python3
"""Split the hand-authored recycler Aseprite GUI into runtime textures."""

from __future__ import annotations

import argparse
from pathlib import Path

from export_aseprite_rgba import apply_opacity, inherited_visibility, parse, render_selected


DEFAULT_LAYER = "提示按钮默认状态"
HOVERED_LAYER = "提示按钮摸到状态"


def export_button(layers, cels, layer_name: str, destination: Path) -> None:
    matches = [layer for layer in layers if layer.name == layer_name]
    if len(matches) != 1:
        raise ValueError(f"Expected one layer named {layer_name!r}, found {len(matches)}")
    layer = matches[0]
    matching_cels = [cel for cel in cels if cel.layer_index == layer.index]
    if len(matching_cels) != 1:
        raise ValueError(f"Expected one cel for layer {layer_name!r}, found {len(matching_cels)}")
    cel = matching_cels[0]
    image = apply_opacity(cel.image, cel.opacity * layer.opacity // 255)
    if image.size != (15, 12):
        raise ValueError(f"Expected {layer_name!r} to be 15x12, got {image.size}")
    image.save(destination)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("source", type=Path)
    parser.add_argument("output", type=Path)
    args = parser.parse_args()

    width, height, layers, cels = parse(args.source)
    if (width, height) != (256, 256):
        raise ValueError(f"Expected a 256x256 GUI canvas, got {(width, height)}")

    args.output.mkdir(parents=True, exist_ok=True)
    button_names = {DEFAULT_LAYER, HOVERED_LAYER}
    visibility = inherited_visibility(layers)
    base_layers = {
        layer.index
        for layer in layers
        if visibility[layer.index] and layer.name not in button_names
    }
    render_selected(width, height, layers, cels, base_layers).save(args.output / "recycler.png")
    export_button(layers, cels, DEFAULT_LAYER, args.output / "recycler_info.png")
    export_button(layers, cels, HOVERED_LAYER, args.output / "recycler_info_hovered.png")

    print(f"Exported recycler GUI from {args.source}")
    for layer in layers:
        print(f"  layer {layer.index}: {layer.name} visible={visibility[layer.index]}")


if __name__ == "__main__":
    main()
