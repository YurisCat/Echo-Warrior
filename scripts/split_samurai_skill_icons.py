#!/usr/bin/env python3
"""Archive and split the Japanese Samurai 3x3 skill icon sheet."""

from __future__ import annotations

import argparse
import shutil
from pathlib import Path

from PIL import Image


ROOT = Path(__file__).resolve().parents[1]
SOURCE_SHEET = (
    ROOT
    / "assets-source/textures/gui/summoner/skills/japanese_samurai_skills.png"
)
OUTPUT_DIRECTORIES = (
    ROOT
    / "assets-source/textures/gui/summoner/skills/japanese_samurai",
    ROOT
    / "src/main/resources/assets/echo_warrior/textures/gui/summoner/skills/japanese_samurai",
    ROOT
    / "encyclopedia/public/assets/echo-warrior/gui/skills/japanese_samurai",
)

# Numpad positions in the authored 3x3 sheet. Each cell is exactly 16x16.
ICONS = {
    "zanshin.png": (0, 0),       # 7: 残心
    "zan.png": (2, 0),           # 9: 斩
    "stab.png": (1, 1),          # 5: 刺
    "fumikomi.png": (0, 2),      # 1: 踏込
}


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--input",
        type=Path,
        required=True,
        help="Authored 48x48 PNG containing the 3x3 icon grid",
    )
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    with Image.open(args.input) as source:
        source.load()
        if source.size != (48, 48):
            raise ValueError(f"Expected a 48x48 sheet, got {source.size}")
        if source.mode != "RGBA":
            source = source.convert("RGBA")

        SOURCE_SHEET.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(args.input, SOURCE_SHEET)

        for output_directory in OUTPUT_DIRECTORIES:
            output_directory.mkdir(parents=True, exist_ok=True)
            for filename, (column, row) in ICONS.items():
                left = column * 16
                top = row * 16
                icon = source.crop((left, top, left + 16, top + 16))
                if icon.size != (16, 16):
                    raise AssertionError(f"Unexpected crop size for {filename}: {icon.size}")
                icon.save(output_directory / filename, format="PNG", optimize=False)

    print(f"Archived source sheet: {SOURCE_SHEET}")
    for filename, (column, row) in ICONS.items():
        print(f"{filename}: cell ({column}, {row}), 16x16")


if __name__ == "__main__":
    main()
