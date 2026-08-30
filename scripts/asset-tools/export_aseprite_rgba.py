#!/usr/bin/env python3
"""Export the first frame of a 32-bit RGBA Aseprite file without Aseprite CLI.

This intentionally supports only the subset used by the project's hand-authored
GUI source: normal layers, raw/compressed cels, group visibility, and one frame.
It also writes one full-canvas PNG per cel so button pieces can be reviewed and
cropped without flattening unrelated artwork.
"""

from __future__ import annotations

import argparse
import io
import re
import struct
import zlib
from dataclasses import dataclass
from pathlib import Path

from PIL import Image, ImageChops


@dataclass
class Layer:
    index: int
    name: str
    flags: int
    layer_type: int
    child_level: int
    blend_mode: int
    opacity: int

    @property
    def visible(self) -> bool:
        return bool(self.flags & 1)


@dataclass
class Cel:
    layer_index: int
    x: int
    y: int
    opacity: int
    image: Image.Image


def u16(stream: io.BytesIO) -> int:
    return struct.unpack("<H", stream.read(2))[0]


def i16(stream: io.BytesIO) -> int:
    return struct.unpack("<h", stream.read(2))[0]


def u32(stream: io.BytesIO) -> int:
    return struct.unpack("<I", stream.read(4))[0]


def ase_string(stream: io.BytesIO) -> str:
    return stream.read(u16(stream)).decode("utf-8")


def parse(path: Path) -> tuple[int, int, list[Layer], list[Cel]]:
    data = path.read_bytes()
    stream = io.BytesIO(data)
    file_size = u32(stream)
    magic = u16(stream)
    frames = u16(stream)
    width = u16(stream)
    height = u16(stream)
    depth = u16(stream)
    if file_size != len(data) or magic != 0xA5E0:
        raise ValueError("Not a valid Aseprite file")
    if depth != 32:
        raise ValueError(f"Only 32-bit RGBA Aseprite files are supported, got depth={depth}")
    if frames < 1:
        raise ValueError("Aseprite file contains no frames")
    stream.seek(128)

    frame_start = stream.tell()
    frame_size = u32(stream)
    if u16(stream) != 0xF1FA:
        raise ValueError("Invalid Aseprite frame header")
    old_chunk_count = u16(stream)
    stream.read(2 + 2)
    new_chunk_count = u32(stream)
    chunk_count = new_chunk_count if new_chunk_count else old_chunk_count

    layers: list[Layer] = []
    cels: list[Cel] = []
    for _ in range(chunk_count):
        chunk_start = stream.tell()
        chunk_size = u32(stream)
        chunk_type = u16(stream)
        if chunk_type == 0x2004:
            flags = u16(stream)
            layer_type = u16(stream)
            child_level = u16(stream)
            stream.read(2 + 2)
            blend_mode = u16(stream)
            opacity = stream.read(1)[0]
            stream.read(3)
            name = ase_string(stream)
            layers.append(Layer(len(layers), name, flags, layer_type, child_level, blend_mode, opacity))
        elif chunk_type == 0x2005:
            layer_index = u16(stream)
            x = i16(stream)
            y = i16(stream)
            opacity = stream.read(1)[0]
            cel_type = u16(stream)
            stream.read(2 + 5)
            if cel_type in (0, 2):
                cel_width = u16(stream)
                cel_height = u16(stream)
                byte_count = cel_width * cel_height * 4
                pixels = stream.read(byte_count) if cel_type == 0 else zlib.decompress(
                    stream.read(chunk_start + chunk_size - stream.tell())
                )
                if len(pixels) != byte_count:
                    raise ValueError(f"Unexpected pixel byte count for layer {layer_index}")
                image = Image.frombytes("RGBA", (cel_width, cel_height), pixels)
                cels.append(Cel(layer_index, x, y, opacity, image))
            elif cel_type == 1:
                raise ValueError("Linked cels are not supported by this one-frame exporter")
            else:
                raise ValueError(f"Unsupported cel type {cel_type}")
        stream.seek(chunk_start + chunk_size)

    if stream.tell() != frame_start + frame_size:
        stream.seek(frame_start + frame_size)
    return width, height, layers, cels


def inherited_visibility(layers: list[Layer]) -> list[bool]:
    visible: list[bool] = []
    group_visibility: dict[int, bool] = {}
    for layer in layers:
        for level in list(group_visibility):
            if level >= layer.child_level:
                del group_visibility[level]
        parent_visible = all(group_visibility.values()) if group_visibility else True
        is_visible = parent_visible and layer.visible
        visible.append(is_visible)
        if layer.layer_type == 1:
            group_visibility[layer.child_level] = is_visible
    return visible


def apply_opacity(image: Image.Image, opacity: int) -> Image.Image:
    if opacity >= 255:
        return image
    result = image.copy()
    alpha = result.getchannel("A").point(lambda value: value * opacity // 255)
    result.putalpha(alpha)
    return result


def composite_layer(backdrop: Image.Image, source: Image.Image, position: tuple[int, int], blend_mode: int) -> None:
    if blend_mode == 0:
        backdrop.alpha_composite(source, position)
        return
    if blend_mode != 1:
        raise ValueError(f"Unsupported visible Aseprite blend mode {blend_mode}")

    overlay = Image.new("RGBA", backdrop.size, (0, 0, 0, 0))
    overlay.alpha_composite(source, position)
    base_rgb = backdrop.convert("RGB")
    source_rgb = overlay.convert("RGB")
    multiplied = ImageChops.multiply(base_rgb, source_rgb).convert("RGBA")
    multiplied.putalpha(overlay.getchannel("A"))
    backdrop.alpha_composite(multiplied)


def safe_name(name: str) -> str:
    cleaned = re.sub(r"[^0-9A-Za-z._-]+", "_", name).strip("_")
    return cleaned or "layer"


def connected_alpha_components(image: Image.Image) -> list[tuple[int, int, int, int]]:
    alpha = image.getchannel("A")
    remaining = {
        (x, y)
        for y in range(image.height)
        for x in range(image.width)
        if alpha.getpixel((x, y)) > 0
    }
    components: list[tuple[int, int, int, int]] = []
    while remaining:
        start = remaining.pop()
        pending = [start]
        points = [start]
        while pending:
            x, y = pending.pop()
            for neighbor_x in range(x - 1, x + 2):
                for neighbor_y in range(y - 1, y + 2):
                    neighbor = (neighbor_x, neighbor_y)
                    if neighbor in remaining:
                        remaining.remove(neighbor)
                        pending.append(neighbor)
                        points.append(neighbor)
        xs = [point[0] for point in points]
        ys = [point[1] for point in points]
        components.append((min(xs), min(ys), max(xs) + 1, max(ys) + 1))
    return sorted(components, key=lambda box: (box[1], box[0]))


def render_selected(width: int, height: int, layers: list[Layer], cels: list[Cel], selected: set[int]) -> Image.Image:
    result = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    for cel in cels:
        if cel.layer_index not in selected:
            continue
        layer = layers[cel.layer_index]
        rendered = apply_opacity(cel.image, cel.opacity * layer.opacity // 255)
        composite_layer(result, rendered, (cel.x, cel.y), layer.blend_mode)
    return result


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("source", type=Path)
    parser.add_argument("output", type=Path)
    args = parser.parse_args()

    width, height, layers, cels = parse(args.source)
    args.output.mkdir(parents=True, exist_ok=True)
    layer_dir = args.output / "layers"
    layer_dir.mkdir(exist_ok=True)

    visible = inherited_visibility(layers)
    composite = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    for cel in cels:
        layer = layers[cel.layer_index]
        canvas = Image.new("RGBA", (width, height), (0, 0, 0, 0))
        rendered = apply_opacity(cel.image, cel.opacity * layer.opacity // 255)
        canvas.alpha_composite(rendered, (cel.x, cel.y))
        canvas.save(layer_dir / f"{cel.layer_index:02d}_{safe_name(layer.name)}.png")
        if visible[cel.layer_index]:
            composite_layer(composite, rendered, (cel.x, cel.y), layer.blend_mode)

    composite.save(args.output / "full.png")

    # Stable named exports for the project's hand-authored knowledge reader.
    collection = render_selected(width, height, layers, cels, {3, 4, 6, 7, 8, 9})
    single = render_selected(width, height, layers, cels, {14, 15, 16, 17})
    for name, image in (("knowledge_collection", collection), ("knowledge_fragment", single)):
        bounds = image.getbbox()
        if bounds is None:
            raise ValueError(f"No visible pixels found for {name}")
        image.crop(bounds).save(args.output / f"{name}.png")

    cel_by_layer = {cel.layer_index: cel for cel in cels}
    button_canvas = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    button_cel = cel_by_layer[19]
    button_canvas.alpha_composite(button_cel.image, (button_cel.x, button_cel.y))
    button_boxes = connected_alpha_components(button_canvas)
    if len(button_boxes) != 3:
        raise ValueError(f"Expected three knowledge buttons, found {len(button_boxes)}")
    for name, bounds in zip(("knowledge_previous", "knowledge_next", "knowledge_extract"), button_boxes):
        button_canvas.crop(bounds).save(args.output / f"{name}.png")

    close_canvas = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    close_cel = cel_by_layer[21]
    close_canvas.alpha_composite(close_cel.image, (close_cel.x, close_cel.y))
    close_bounds = close_canvas.getbbox()
    if close_bounds is None:
        raise ValueError("No visible close button pixels found")
    close_canvas.crop(close_bounds).save(args.output / "knowledge_close.png")
    for layer in layers:
        print(
            f"{layer.index:02d} level={layer.child_level} type={layer.layer_type} "
            f"visible={visible[layer.index]} blend={layer.blend_mode} opacity={layer.opacity:3d} {layer.name}"
        )


if __name__ == "__main__":
    main()
