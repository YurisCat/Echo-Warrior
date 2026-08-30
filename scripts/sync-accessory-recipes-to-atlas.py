#!/usr/bin/env python3
"""Synchronize the 25 accessory recipe JSON files into the web encyclopedia."""

from __future__ import annotations

import json
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
ATLAS_PATH = ROOT / "encyclopedia/content/atlas.zh_cn.json"
RECIPE_ROOT = ROOT / "src/main/resources/data/echo_warrior/recipe"
PUBLIC_ROOT = ROOT / "encyclopedia/public"

LEGACY_NAMES = {
    "echo_warrior:courage_legacy": "勇气的传承",
    "echo_warrior:fortitude_legacy": "坚毅的传承",
    "echo_warrior:purity_legacy": "纯净的传承",
    "echo_warrior:wisdom_legacy": "智慧的传承",
    "echo_warrior:craft_legacy": "工艺的传承",
}

TAG_SLOTS = {
    "#minecraft:wool": ("任意颜色羊毛", "/assets/minecraft/block/white_wool.png"),
    "#minecraft:planks": ("任意木板", "/assets/minecraft/block/oak_planks.png"),
    "#minecraft:leaves": ("任意树叶", "/assets/minecraft/block/oak_leaves.png"),
    "#minecraft:fishes": ("任意原版鱼类", "/assets/minecraft/item/cod.png"),
}

ITEM_NAMES = {
    "minecraft:amethyst_shard": "紫水晶碎片",
    "minecraft:bamboo": "竹子",
    "minecraft:bone": "骨头",
    "minecraft:cactus": "仙人掌",
    "minecraft:cooked_porkchop": "熟猪排",
    "minecraft:copper_ingot": "铜锭",
    "minecraft:cyan_wool": "青色羊毛",
    "minecraft:diamond": "钻石",
    "minecraft:emerald": "绿宝石",
    "minecraft:feather": "羽毛",
    "minecraft:flint": "燧石",
    "minecraft:ghast_tear": "恶魂之泪",
    "minecraft:glass": "玻璃",
    "minecraft:gold_ingot": "金锭",
    "minecraft:gold_nugget": "金粒",
    "minecraft:golden_apple": "金苹果",
    "minecraft:iron_chestplate": "铁胸甲",
    "minecraft:iron_ingot": "铁锭",
    "minecraft:lapis_lazuli": "青金石",
    "minecraft:obsidian": "黑曜石",
    "minecraft:phantom_membrane": "幻翼膜",
    "minecraft:red_dye": "红色染料",
    "minecraft:redstone": "红石粉",
    "minecraft:spyglass": "望远镜",
    "minecraft:stick": "木棍",
    "minecraft:stone": "石头",
    "minecraft:string": "线",
    "minecraft:sunflower": "向日葵",
    "minecraft:terracotta": "陶瓦",
    "minecraft:tuff": "凝灰岩",
}

BLOCK_TEXTURES = {
    "minecraft:cactus": "cactus_side",
    "minecraft:cyan_wool": "cyan_wool",
    "minecraft:glass": "glass",
    "minecraft:obsidian": "obsidian",
    "minecraft:stone": "stone",
    "minecraft:sunflower": "sunflower_front",
    "minecraft:terracotta": "terracotta",
    "minecraft:tuff": "tuff",
}


def ingredient_slot(ingredient: str) -> dict[str, str]:
    if ingredient in TAG_SLOTS:
        name, icon = TAG_SLOTS[ingredient]
        return {"name": name, "icon": icon}
    if ingredient in LEGACY_NAMES:
        item_id = ingredient.split(":", 1)[1]
        return {
            "name": LEGACY_NAMES[ingredient],
            "icon": f"/assets/echo-warrior/item/{item_id}.png",
        }
    if ingredient not in ITEM_NAMES:
        raise KeyError(f"missing encyclopedia name for recipe ingredient {ingredient}")
    item_id = ingredient.split(":", 1)[1]
    if ingredient in BLOCK_TEXTURES:
        icon = f"/assets/minecraft/block/{BLOCK_TEXTURES[ingredient]}.png"
    else:
        icon = f"/assets/minecraft/item/{item_id}.png"
    return {"name": ITEM_NAMES[ingredient], "icon": icon}


def recipe_grid(recipe: dict) -> tuple[list[dict[str, str] | None], bool]:
    grid: list[dict[str, str] | None] = [None] * 9
    shapeless = recipe["type"] == "minecraft:crafting_shapeless"
    if shapeless:
        ingredients = recipe["ingredients"]
        if len(ingredients) > 9:
            raise ValueError("shapeless recipe has more than nine ingredients")
        for index, ingredient in enumerate(ingredients):
            grid[index] = ingredient_slot(ingredient)
        return grid, True

    pattern = recipe["pattern"]
    if len(pattern) > 3 or any(len(row) > 3 for row in pattern):
        raise ValueError(f"recipe pattern exceeds 3x3: {pattern}")
    for row_index, row in enumerate(pattern):
        for column_index, symbol in enumerate(row):
            if symbol != " ":
                grid[row_index * 3 + column_index] = ingredient_slot(recipe["key"][symbol])
    return grid, False


def main() -> None:
    atlas = json.loads(ATLAS_PATH.read_text(encoding="utf-8"))
    accessory_nodes = [node for node in atlas["nodes"] if node["categoryId"] == "accessories"]
    item_nodes = [node for node in accessory_nodes if node["id"] != "accessories_overview"]
    if len(item_nodes) != 25:
        raise RuntimeError(f"expected 25 accessory nodes, found {len(item_nodes)}")

    for node in item_nodes:
        recipe_path = RECIPE_ROOT / f"{node['id']}.json"
        recipe = json.loads(recipe_path.read_text(encoding="utf-8"))
        grid, shapeless = recipe_grid(recipe)
        recipe_section = next(
            section for section in node["article"]["sections"] if section["type"] == "recipe"
        )
        recipe_section.update(
            {
                "title": "合成表",
                "grid": grid,
                "shapeless": shapeless,
                "output": {
                    "name": node["title"],
                    "icon": f"/assets/echo-warrior/item/{node['id']}.png",
                },
            }
        )
        node["article"]["sections"] = [
            section
            for section in node["article"]["sections"]
            if not (section.get("type") == "callout" and section.get("title") == "配方状态")
        ]

    missing_icons = sorted(
        {
            slot["icon"]
            for node in item_nodes
            for section in node["article"]["sections"]
            if section["type"] == "recipe"
            for slot in section["grid"]
            if slot is not None and not (PUBLIC_ROOT / slot["icon"].lstrip("/")).exists()
        }
    )
    if missing_icons:
        raise FileNotFoundError("missing encyclopedia recipe icons: " + ", ".join(missing_icons))

    atlas["meta"]["lastVerified"] = "2026-08-30"
    ATLAS_PATH.write_text(
        json.dumps(atlas, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    print("Synchronized 25 final accessory recipes into atlas.zh_cn.json.")


if __name__ == "__main__":
    main()
