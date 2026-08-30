#!/usr/bin/env python3
"""Export stable knowledge-page illustrations from item textures.

Only entries explicitly marked with ``illustration_binding: high`` are emitted.
The current baseline preserves each source texture's original RGBA pixels. This
keeps Minecraft's original item colors and transparent silhouette without the
old runtime square overlay or an automatic paper/ink tint. A second texture is
exported with the same RGB values and 50% alpha for the page's idle state.
"""

from __future__ import annotations

import argparse
import io
import json
import zipfile
from pathlib import Path

from PIL import Image


MINECRAFT_TEXTURES = {
    "minecraft:cocoa_beans": "assets/minecraft/textures/item/cocoa_beans.png",
    "minecraft:feather": "assets/minecraft/textures/item/feather.png",
    "minecraft:bow": "assets/minecraft/textures/item/bow.png",
    "minecraft:arrow": "assets/minecraft/textures/item/arrow.png",
    "minecraft:crossbow": "assets/minecraft/textures/item/crossbow_standby.png",
    "minecraft:bamboo": "assets/minecraft/textures/item/bamboo.png",
    "minecraft:fire_charge": "assets/minecraft/textures/item/fire_charge.png",
}
IDLE_OPACITY = 0.5


def output_name(resource: str) -> str:
    return resource.replace(":", "_").replace("/", "_") + ".png"


def faded_output_name(resource: str) -> str:
    return resource.replace(":", "_").replace("/", "_") + "_faded.png"


def load_source(resource: str, client_jar: zipfile.ZipFile, project_assets: Path) -> Image.Image:
    if resource in MINECRAFT_TEXTURES:
        with client_jar.open(MINECRAFT_TEXTURES[resource]) as source:
            return Image.open(io.BytesIO(source.read())).convert("RGBA")
    namespace, path = resource.split(":", 1)
    if namespace == "echo_warrior":
        return Image.open(project_assets / "textures" / "item" / f"{path}.png").convert("RGBA")
    raise ValueError(f"No offline illustration source configured for {resource}")


def prepare_illustration(source: Image.Image) -> Image.Image:
    return source.copy()


def prepare_faded_illustration(source: Image.Image) -> Image.Image:
    faded = source.copy()
    faded.putalpha(source.getchannel("A").point(lambda alpha: round(alpha * IDLE_OPACITY)))
    return faded


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("catalog", type=Path)
    parser.add_argument("minecraft_client_jar", type=Path)
    parser.add_argument("project_assets", type=Path)
    parser.add_argument("output", type=Path)
    args = parser.parse_args()

    catalog = json.loads(args.catalog.read_text(encoding="utf-8"))
    resources: list[str] = []
    for entry in catalog["entries"]:
        if entry.get("illustration_binding") != "high":
            continue
        for illustration in entry.get("illustrations", []):
            if illustration.get("type") != "item":
                raise ValueError(f"Offline processing currently expects item illustrations: {entry['id']}")
            resource = illustration["resource"]
            if resource not in resources:
                resources.append(resource)

    args.output.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(args.minecraft_client_jar) as client_jar:
        for resource in resources:
            source = load_source(resource, client_jar, args.project_assets)
            if source.size != (16, 16):
                raise ValueError(f"Expected a 16x16 source for {resource}, found {source.size}")
            destination = args.output / output_name(resource)
            prepare_illustration(source).save(destination)
            faded_destination = args.output / faded_output_name(resource)
            prepare_faded_illustration(source).save(faded_destination)
            print(f"{resource} -> {destination}, {faded_destination}")


if __name__ == "__main__":
    main()
