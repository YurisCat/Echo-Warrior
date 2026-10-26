"""Validate an LMS item export against a 26.1.2 release JAR and prepare review files.

Does not launch Minecraft, access the network, or submit anything to MCMOD.
PNG validation needs Pillow. Image bytes are extracted unchanged.
"""
import argparse
import base64
import collections
import csv
import hashlib
import html
import io
import json
from pathlib import Path
import re
import zipfile

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]


def save_json(path, value):
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def require(condition, message):
    if not condition:
        raise ValueError(message)


def ingredient(value):
    require(isinstance(value, str) and ":" in value, f"Unsupported ingredient: {value!r}")
    return {"tag" if value.startswith("#") else "item": value.lstrip("#"), "count": 1}


def convert_recipe(name, value):
    kind = value["type"].removeprefix("minecraft:")
    if kind == "crafting_shaped":
        pattern = value["pattern"]
        require(1 <= len(pattern) <= 3 and 1 <= len(pattern[0]) <= 3,
                f"Invalid recipe dimensions: {name}")
        require(len({len(row) for row in pattern}) == 1, f"Unequal recipe rows: {name}")
        # Vanilla removes blank outer rows and columns before assigning crafting slots.
        occupied = [(x, y) for y, row in enumerate(pattern) for x, ch in enumerate(row) if ch != " "]
        require(bool(occupied), f"Empty recipe: {name}")
        left, top = min(x for x, y in occupied), min(y for x, y in occupied)
        inputs = {str((y - top) * 3 + x - left + 1): ingredient(value["key"][pattern[y][x]])
                  for x, y in occupied}
    elif kind == "crafting_shapeless":
        require(1 <= len(value["ingredients"]) <= 9, f"Invalid shapeless recipe: {name}")
        inputs = {str(i + 1): ingredient(v) for i, v in enumerate(value["ingredients"])}
    else:
        return None
    result = value["result"]
    require(not (set(result) - {"id", "count"}), f"Unsupported result components: {name}")
    return {"type": kind, "name": name, "input": inputs,
            "output": {"1": {"item": result["id"], "count": result.get("count", 1)}}}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--items", type=Path, required=True)
    parser.add_argument("--release-jar", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    out = args.output
    require(not out.exists(), "Choose a new output directory; an existing review package is never overwritten.")
    original = args.items.read_bytes()
    records = [json.loads(line) for line in original.decode("utf-8-sig").splitlines() if line.strip()]
    ids = [r["registerName"] for r in records]
    require(len(ids) == len(set(ids)), "Duplicate item registry IDs")
    corrections, pictures, recipes, skipped, raw_recipes = [], {}, [], [], {}
    with zipfile.ZipFile(args.release_jar) as jar:
        lang = {loc: json.loads(jar.read(f"assets/echo_warrior/lang/{loc}.json"))
                for loc in ("zh_cn", "en_us")}
        definitions = {"echo_warrior:" + Path(n).stem for n in jar.namelist()
                       if n.startswith("assets/echo_warrior/items/") and n.endswith(".json")}
        require(set(ids) == definitions, f"Export/JAR item mismatch: {set(ids) ^ definitions}")
        for r in records:
            item_id = r["registerName"].split(":", 1)[1]
            require(re.fullmatch(r"[a-z0-9_]+", item_id), f"Unexpected ID: {item_id}")
            require(r["type"] in ("Item", "Block"), f"Unknown item type: {item_id}")
            for field in ("maxStacksSize", "maxDurability"):
                require(type(r[field]) is int and r[field] >= 0, f"Invalid {field}: {item_id}")
            require(r["maxStacksSize"] >= 1, f"Invalid stack size: {item_id}")
            for field, loc in (("name", "zh_cn"), ("englishName", "en_us")):
                keys = [f"item.echo_warrior.{item_id}", f"block.echo_warrior.{item_id}"]
                if r["type"] == "Block":
                    keys.reverse()
                key = next((key for key in keys if key in lang[loc]), None)
                require(key is not None, f"Missing {loc} name: {item_id}")
                value = lang[loc][key]
                if r[field] != value:
                    corrections.append({"id": r["registerName"], "field": field,
                                        "before": r[field], "after": value, "source": f"{loc}:{key}"})
                    r[field] = value
            tab = "accessories" if item_id.endswith("_accessory") else (
                "knowledge_fragments" if item_id in ("knowledge_fragment", "knowledge_fragment_collection") else "echo_warrior")
            tab_name = lang["zh_cn"][f"itemGroup.echo_warrior.{tab}"]
            corrections.append({"id": r["registerName"], "field": "CreativeTabName",
                                "before": r["CreativeTabName"], "after": tab_name,
                                "source": "ModCreativeTabs.java + zh_cn.json; exclude vanilla search tab"})
            r["CreativeTabName"] = tab_name
            for field, size in (("smallIcon", 32), ("largeIcon", 128)):
                data = base64.b64decode(r[field], validate=True)
                with Image.open(io.BytesIO(data)) as im:
                    require(im.format == "PNG" and im.size == (size, size), f"Invalid {field}: {item_id}")
                    im.verify()
                with Image.open(io.BytesIO(data)) as im:
                    require(im.convert("RGBA").getbbox() is not None, f"Blank {field}: {item_id}")
                pictures[f"{item_id}-{size}.png"] = data
        for name in sorted(jar.namelist()):
            if name.startswith("data/echo_warrior/recipe/") and name.endswith(".json"):
                raw = json.loads(jar.read(name))
                recipe_id = "echo_warrior:" + Path(name).stem
                raw_recipes[recipe_id] = raw
                converted = convert_recipe(recipe_id, raw)
                if converted is None:
                    skipped.append({"id": recipe_id, "serializer": raw["type"], "reason": "Dynamic recipe; document manually"})
                else:
                    recipes.append(converted)
    # All validation finishes before writing the delivery package.
    out.mkdir(parents=True)
    (out / "icons").mkdir()
    (out / "raw").mkdir()
    (out / "raw" / "lms-original.json").write_bytes(original)
    for name, data in pictures.items():
        (out / "icons" / name).write_bytes(data)
    records.sort(key=lambda r: r["registerName"])
    item_path = out / "echo_warrior.json"
    item_path.write_text("".join(json.dumps(r, ensure_ascii=False, separators=(",", ":")) + "\n" for r in records), encoding="utf-8")
    save_json(out / "recipes-candidate.json", {"recipes": recipes, "error": []})
    save_json(out / "raw" / "release-recipes.json", raw_recipes)
    save_json(out / "corrections.json", corrections)
    save_json(out / "dynamic-recipes.json", skipped)
    atlas = json.loads((ROOT / "encyclopedia/content/atlas.zh_cn.json").read_text(encoding="utf-8"))
    nodes = {n["id"]: n for n in atlas["nodes"]}
    aliases = {"test_echo_summoner": "summoner", "knowledge_fragment": "knowledge_fragments",
               "knowledge_fragment_collection": "knowledge_fragments", "suspicious_dirt": "archaeology",
               "suspicious_grass_block": "archaeology"}
    sources, cards = [], []
    with (out / "item-index.csv").open("w", encoding="utf-8-sig", newline="") as f:
        fields = ["registerName", "name", "englishName", "type", "maxStacksSize", "maxDurability", "CreativeTabName", "OredictList"]
        writer = csv.DictWriter(f, fields, extrasaction="ignore")
        writer.writeheader()
        writer.writerows(records)
    for r in records:
        item_id = r["registerName"].split(":")[1]
        node_id = aliases.get(item_id, item_id.removesuffix("_relic"))
        n = nodes.get(node_id)
        sources.append({"registerName": r["registerName"], "name": r["name"], "atlasNode": node_id,
                        "reviewRequired": True, "article": n["article"] if n else None})
        e = html.escape
        cards.append(f'<article><img src="icons/{item_id}-128.png" alt="{e(r["name"])}">'
                     f'<h2>{e(r["name"])}</h2><p>{e(r["englishName"])}</p><code>{e(r["registerName"])}</code>'
                     f'<p>{e(r["CreativeTabName"])} · {e(r["type"])} · 堆叠 {r["maxStacksSize"]}</p></article>')
    save_json(out / "article-sources-for-review.json", sources)
    page = '<!doctype html><html lang="zh-CN"><meta charset="utf-8"><title>英灵回声 · MC百科导出核对</title><style>body{font:16px system-ui;margin:32px;background:#f3f0e8;color:#222}main{display:grid;grid-template-columns:repeat(auto-fit,minmax(290px,1fr));gap:16px}article{padding:20px;background:#fff;border:1px solid #ddd;border-radius:12px}img{width:128px;height:128px;image-rendering:pixelated;background:repeating-conic-gradient(#e6e6e6 0% 25%,#fff 0% 50%) 0/16px 16px}h2{font-size:20px}code{font-size:12px;overflow-wrap:anywhere}</style><h1>英灵回声 0.2.2 · MC百科导出核对</h1><p>43 个注册物品/方块 · 26.1.2 NeoForge · LMS 1.3.10 · 尚未提交网站</p><p>图片为游戏渲染原始 PNG。回收箱名称和创造标签页已按正式版内容校正。知识碎片以注册物品收录，不将 40 页重复建成独立注册物品。</p><main>' + "".join(cards) + '</main></html>'
    (out / "preview.html").write_text(page, encoding="utf-8")
    report = {"item_count": len(records), "types": dict(collections.Counter(r["type"] for r in records)),
              "png_count": len(pictures), "static_recipe_candidates": len(recipes), "dynamic_recipes": skipped,
              "field_corrections": len(corrections), "raw_lms_sha256": hashlib.sha256(original).hexdigest(),
              "upload_json_sha256": hashlib.sha256(item_path.read_bytes()).hexdigest(),
              "release_jar_sha256": hashlib.sha256(args.release_jar.read_bytes()).hexdigest(),
              "recipe_schema_source": "https://github.com/friendlyhj/RecipeDumper/tree/main/src/main/java/cn/mcmod/recipedumper",
              "recipe_status": "Converted from release JAR; not generated by RecipeDumper runtime or accepted by MCMOD",
              "publication_status": "not submitted"}
    save_json(out / "validation.json", report)
    with zipfile.ZipFile(out / "echo-warrior-0.2.2-items-mcmod.zip", "w", zipfile.ZIP_DEFLATED) as bundle:
        bundle.write(item_path, "echo_warrior.json")
    print(json.dumps(report, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
