#!/usr/bin/env python3
"""Export the hand-drawn tutorial manual GUI into runtime PNG assets.

The source Aseprite file stays in human-work. This script only reads the
single-frame, 32-bit RGBA document and exports the named layers used by the
Minecraft client.
"""

from __future__ import annotations

import shutil
import struct
import sys
import zlib
from pathlib import Path

from PIL import Image


ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "human-work" / "非英灵相关" / "教程手册" / "教程手册GUI.ase"
MANUAL_ICON = ROOT / "human-work" / "物品制作" / "回声英灵与你.png"
CREDITS_PORTRAIT = ROOT / "human-work" / "非英灵相关" / "教程手册" / "可爱像素表情.png"
GUI_OUTPUT = ROOT / "src" / "main" / "resources" / "assets" / "echo_warrior" / "textures" / "gui" / "tutorial"
ITEM_OUTPUT = ROOT / "src" / "main" / "resources" / "assets" / "echo_warrior" / "textures" / "item" / "tutorial_manual.png"


def save_shadow(source: Image.Image, output: Path) -> None:
    """Save a brown shadow using only the source texture's non-transparent pixels."""
    shadow = Image.new("RGBA", source.size, (0x35, 0x27, 0x20, 0))
    shadow.putalpha(source.getchannel("A").point(lambda alpha: round(alpha * 85 / 255)))
    shadow.save(output)


def read_string(data: bytes, offset: int) -> tuple[str, int]:
    length = struct.unpack_from("<H", data, offset)[0]
    offset += 2
    return data[offset : offset + length].decode("utf-8"), offset + length


def load_layers(path: Path) -> tuple[dict[str, Image.Image], dict[str, tuple[int, int, int, int]]]:
    data = path.read_bytes()
    _, magic, frames, width, height, depth = struct.unpack_from("<IHHHHH", data, 0)
    if magic != 0xA5E0 or frames != 1 or depth != 32:
        raise ValueError("Tutorial GUI must remain a one-frame 32-bit RGBA Aseprite file")

    offset = 128
    frame_bytes, frame_magic, old_chunks = struct.unpack_from("<IHH", data, offset)
    if frame_magic != 0xF1FA:
        raise ValueError("Invalid Aseprite frame header")
    new_chunks = struct.unpack_from("<I", data, offset + 12)[0]
    chunk_count = new_chunks or old_chunks
    offset += 16

    layer_names: list[str] = []
    layer_types: list[int] = []
    layer_levels: list[int] = []
    layer_images: dict[int, tuple[int, int, Image.Image]] = {}
    for _ in range(chunk_count):
        chunk_start = offset
        chunk_size, chunk_type = struct.unpack_from("<IH", data, offset)
        payload = offset + 6
        chunk_end = chunk_start + chunk_size

        if chunk_type == 0x2004:
            layer_type = struct.unpack_from("<H", data, payload + 2)[0]
            child_level = struct.unpack_from("<H", data, payload + 4)[0]
            cursor = payload + 16
            name, _ = read_string(data, cursor)
            layer_names.append(name)
            layer_types.append(layer_type)
            layer_levels.append(child_level)
        elif chunk_type == 0x2005:
            layer_index, x, y, opacity, cel_type = struct.unpack_from("<HhhBH", data, payload)
            cursor = payload + 16
            if cel_type not in (0, 2):
                raise ValueError(f"Unsupported cel type {cel_type}")
            cel_width, cel_height = struct.unpack_from("<HH", data, cursor)
            cursor += 4
            pixels = data[cursor:chunk_end]
            if cel_type == 2:
                pixels = zlib.decompress(pixels)
            image = Image.frombytes("RGBA", (cel_width, cel_height), pixels)
            if opacity != 255:
                image.putalpha(image.getchannel("A").point(lambda alpha: round(alpha * opacity / 255)))
            layer_images[layer_index] = (x, y, image)

        offset = chunk_end

    result: dict[str, Image.Image] = {}
    layout: dict[str, tuple[int, int, int, int]] = {}
    for index, name in enumerate(layer_names):
        if index not in layer_images:
            continue
        x, y, image = layer_images[index]
        result[name] = image
        layout[name] = (x, y, image.width, image.height)

    # The shapeless marker is a two-layer group. Preserve its 16x16 alignment.
    shapeless_group = next((
        index for index, name in enumerate(layer_names)
        if layer_types[index] == 1 and "无序" in name
    ), None)
    if shapeless_group is not None:
        group_level = layer_levels[shapeless_group]
        children: list[tuple[int, int, Image.Image]] = []
        for index in range(shapeless_group + 1, len(layer_names)):
            if layer_levels[index] <= group_level:
                break
            if index in layer_images:
                children.append(layer_images[index])
        if children:
            base_x = min(x for x, _, _ in children)
            base_y = min(y for _, y, _ in children)
            right = max(x + image.width for x, _, image in children)
            bottom = max(y + image.height for _, y, image in children)
            shapeless = Image.new("RGBA", (right - base_x, bottom - base_y), (0, 0, 0, 0))
            for x, y, image in reversed(children):
                shapeless.alpha_composite(image, (x - base_x, y - base_y))
            result["无序合成组合"] = shapeless
            layout["无序合成组合"] = (base_x, base_y, shapeless.width, shapeless.height)

    if "--inspect" in sys.argv:
        print(f"document={width}x{height}")
        for index, name in enumerate(layer_names):
            kind = "group" if layer_types[index] == 1 else "layer"
            if index in layer_images:
                x, y, image = layer_images[index]
                print(f"{index:02d} level={layer_levels[index]} {kind} {name!r}: x={x} y={y} size={image.width}x{image.height}")
            else:
                print(f"{index:02d} level={layer_levels[index]} {kind} {name!r}")
    return result, layout


def main() -> None:
    layers, _ = load_layers(SOURCE)
    if "--inspect" in sys.argv:
        return
    GUI_OUTPUT.mkdir(parents=True, exist_ok=True)
    ITEM_OUTPUT.parent.mkdir(parents=True, exist_ok=True)

    exports = {
        "背景纸张": "paper_shadow.png",
        "纸张 复制": "paper.png",
        "向前翻页按钮": "previous.png",
        "向后翻页按钮": "next.png",
        "关闭按钮": "close.png",
        "标签页底层条带": "chapter_tab.png",
        "合成表案例": "recipe.png",
        "无序合成组合": "shapeless.png",
    }
    for layer_name, filename in exports.items():
        if layer_name not in layers:
            raise KeyError(f"Missing required tutorial GUI layer: {layer_name}")
        layers[layer_name].save(GUI_OUTPUT / filename)

    save_shadow(layers["向前翻页按钮"], GUI_OUTPUT / "previous_shadow.png")
    save_shadow(layers["向后翻页按钮"], GUI_OUTPUT / "next_shadow.png")
    save_shadow(layers["关闭按钮"], GUI_OUTPUT / "close_shadow.png")

    stale_page_buttons = GUI_OUTPUT / "page_buttons.png"
    if stale_page_buttons.exists():
        stale_page_buttons.unlink()

    shutil.copyfile(MANUAL_ICON, ITEM_OUTPUT)
    shutil.copyfile(CREDITS_PORTRAIT, GUI_OUTPUT / "credits_portrait.png")
    stale_strip = GUI_OUTPUT / "calypso_phone_strip.png"
    if stale_strip.exists():
        stale_strip.unlink()
    print(f"Exported tutorial manual assets to {GUI_OUTPUT.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
