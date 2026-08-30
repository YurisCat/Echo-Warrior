#!/usr/bin/env python3
"""Build the layered Echo Compass textures and models.

The hand-authored dial, highlight, and metal frames stay as independent source
layers. Vanilla's 32 projected needle masks provide the pixel-perfect motion.
The generated needle stays grayscale and receives the final cyan tint at
runtime, avoiding a separate colored texture set for every animation frame.
"""

from __future__ import annotations

import argparse
import io
import json
import zipfile
from pathlib import Path

from PIL import Image


CANVAS_SIZE = (16, 16)
NEEDLE_GRAYS = {
    (255, 20, 20): (255, 255, 255, 255),
    (203, 26, 26): (205, 205, 205, 255),
    (190, 21, 21): (115, 115, 115, 255),
}
POINTER_CYAN = (90, 238, 255)
SOURCE_FILES = {
    "echo_compass_background.png": "罗盘背景.png",
    "echo_compass_highlight.png": "罗盘高光.png",
    "echo_compass_frame_copper.png": "罗盘框_铜.png",
    "echo_compass_frame_gold.png": "罗盘框_金.png",
    "echo_compass_frame_iron.png": "罗盘框_铁.png",
}


def load_rgba(path: Path) -> Image.Image:
    image = Image.open(path).convert("RGBA")
    if image.size != CANVAS_SIZE:
        raise ValueError(f"Expected {path} to be 16x16, got {image.size}")
    return image


def extract_needle(frame: Image.Image) -> Image.Image:
    result = Image.new("RGBA", CANVAS_SIZE, (0, 0, 0, 0))
    source_pixels = frame.load()
    output_pixels = result.load()
    for y in range(CANVAS_SIZE[1]):
        for x in range(CANVAS_SIZE[0]):
            red, green, blue, alpha = source_pixels[x, y]
            if alpha == 0:
                continue
            replacement = NEEDLE_GRAYS.get((red, green, blue))
            if replacement is None and red > green + 40 and red > blue + 40:
                raise ValueError(
                    f"Unmapped vanilla compass needle color {(red, green, blue)} at {(x, y)}"
                )
            if replacement is not None:
                output_pixels[x, y] = replacement
    return result


def write_model(path: Path, texture: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(
        json.dumps(
            {
                "parent": "minecraft:item/generated",
                "textures": {"layer0": texture},
            },
            ensure_ascii=False,
            indent=2,
        )
        + "\n",
        encoding="utf-8",
    )


def write_static_fallback_model(path: Path) -> None:
    path.write_text(
        json.dumps(
            {
                "parent": "minecraft:item/generated",
                "textures": {
                    "layer0": "echo_warrior:item/echo_compass/echo_compass_background",
                    "layer1": "echo_warrior:item/echo_compass/echo_compass_pointer_16",
                    "layer2": "echo_warrior:item/echo_compass/echo_compass_highlight",
                    "layer3": "echo_warrior:item/echo_compass/echo_compass_frame_copper",
                },
            },
            ensure_ascii=False,
            indent=2,
        )
        + "\n",
        encoding="utf-8",
    )


def pointer_model(index: int) -> dict[str, object]:
    return {
        "type": "minecraft:model",
        "model": f"echo_warrior:item/echo_compass_pointer_{index:02d}",
        "tints": [{"type": "echo_warrior:echo_compass_pointer"}],
    }


def write_item_definition(path: Path) -> None:
    entries: list[dict[str, object]] = [{"threshold": 0.0, "model": pointer_model(16)}]
    for step in range(1, 32):
        entries.append(
            {
                "threshold": step - 0.5,
                "model": pointer_model((16 + step) % 32),
            }
        )
    entries.append({"threshold": 31.5, "model": pointer_model(16)})

    definition = {
        "model": {
            "type": "minecraft:composite",
            "models": [
                {
                    "type": "minecraft:model",
                    "model": "echo_warrior:item/echo_compass_background",
                },
                {
                    "type": "minecraft:range_dispatch",
                    "property": "echo_warrior:echo_compass_angle",
                    "scale": 32.0,
                    "entries": entries,
                    "fallback": pointer_model(16),
                },
                {
                    "type": "minecraft:model",
                    "model": "echo_warrior:item/echo_compass_highlight",
                },
                {
                    "type": "minecraft:condition",
                    "property": "echo_warrior:echo_compass_gold_frame",
                    "on_true": {
                        "type": "minecraft:model",
                        "model": "echo_warrior:item/echo_compass_frame_gold",
                    },
                    "on_false": {
                        "type": "minecraft:condition",
                        "property": "echo_warrior:echo_compass_iron_frame",
                        "on_true": {
                            "type": "minecraft:model",
                            "model": "echo_warrior:item/echo_compass_frame_iron",
                        },
                        "on_false": {
                            "type": "minecraft:model",
                            "model": "echo_warrior:item/echo_compass_frame_copper",
                        },
                    },
                },
            ],
        }
    }
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(definition, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def composite_frame(
    background: Image.Image,
    needle: Image.Image,
    highlight: Image.Image,
    frame: Image.Image,
) -> Image.Image:
    result = Image.new("RGBA", CANVAS_SIZE, (0, 0, 0, 0))
    for layer in (background, needle, highlight, frame):
        result.alpha_composite(layer)
    return result


def tint_needle(needle: Image.Image, color: tuple[int, int, int]) -> Image.Image:
    result = Image.new("RGBA", CANVAS_SIZE, (0, 0, 0, 0))
    source_pixels = needle.load()
    output_pixels = result.load()
    for y in range(CANVAS_SIZE[1]):
        for x in range(CANVAS_SIZE[0]):
            red, green, blue, alpha = source_pixels[x, y]
            if alpha == 0:
                continue
            output_pixels[x, y] = (
                red * color[0] // 255,
                green * color[1] // 255,
                blue * color[2] // 255,
                alpha,
            )
    return result


def write_preview(
    path: Path,
    background: Image.Image,
    needles: list[Image.Image],
    highlight: Image.Image,
    frame: Image.Image,
    tint: tuple[int, int, int],
) -> None:
    columns = 8
    rows = 4
    sheet = Image.new("RGBA", (columns * 16, rows * 16), (48, 48, 48, 255))
    for index, needle in enumerate(needles):
        composed = composite_frame(background, tint_needle(needle, tint), highlight, frame)
        sheet.alpha_composite(composed, ((index % columns) * 16, (index // columns) * 16))
    path.parent.mkdir(parents=True, exist_ok=True)
    sheet.resize((columns * 16 * 8, rows * 16 * 8), Image.Resampling.NEAREST).save(path)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("source", type=Path, help="Directory containing the hand-authored compass layers")
    parser.add_argument("minecraft_client_jar", type=Path)
    parser.add_argument("texture_output", type=Path)
    parser.add_argument("model_output", type=Path)
    parser.add_argument("item_definition_output", type=Path)
    parser.add_argument("--preview-output", type=Path)
    args = parser.parse_args()

    args.texture_output.mkdir(parents=True, exist_ok=True)
    args.model_output.mkdir(parents=True, exist_ok=True)
    write_item_definition(args.item_definition_output)

    layers: dict[str, Image.Image] = {}
    for output_name, source_name in SOURCE_FILES.items():
        layer = load_rgba(args.source / source_name)
        layer.save(args.texture_output / output_name)
        layers[output_name] = layer
        write_model(
            args.model_output / f"{Path(output_name).stem}.json",
            f"echo_warrior:item/echo_compass/{Path(output_name).stem}",
        )

    needles: list[Image.Image] = []
    with zipfile.ZipFile(args.minecraft_client_jar) as client_jar:
        for index in range(32):
            entry = f"assets/minecraft/textures/item/compass_{index:02d}.png"
            with client_jar.open(entry) as source:
                vanilla_frame = Image.open(io.BytesIO(source.read())).convert("RGBA")
            if vanilla_frame.size != CANVAS_SIZE:
                raise ValueError(f"Expected {entry} to be 16x16, got {vanilla_frame.size}")
            needle = extract_needle(vanilla_frame)
            if needle.getbbox() is None:
                raise ValueError(f"No needle pixels found in {entry}")
            needles.append(needle)
            texture_name = f"echo_compass_pointer_{index:02d}"
            needle.save(args.texture_output / f"{texture_name}.png")
            write_model(
                args.model_output / f"{texture_name}.json",
                f"echo_warrior:item/echo_compass/{texture_name}",
            )

    write_static_fallback_model(args.model_output / "echo_compass.json")

    if args.preview_output is not None:
        write_preview(
            args.preview_output / "echo_compass_copper_32_frames.png",
            layers["echo_compass_background.png"],
            needles,
            layers["echo_compass_highlight.png"],
            layers["echo_compass_frame_copper.png"],
            POINTER_CYAN,
        )
        write_preview(
            args.preview_output / "echo_compass_gold_32_frames.png",
            layers["echo_compass_background.png"],
            needles,
            layers["echo_compass_highlight.png"],
            layers["echo_compass_frame_gold.png"],
            POINTER_CYAN,
        )
        write_preview(
            args.preview_output / "echo_compass_iron_32_frames.png",
            layers["echo_compass_background.png"],
            needles,
            layers["echo_compass_highlight.png"],
            layers["echo_compass_frame_iron.png"],
            POINTER_CYAN,
        )


if __name__ == "__main__":
    main()
