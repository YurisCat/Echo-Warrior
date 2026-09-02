from __future__ import annotations

import argparse
import json
import struct
import sys
import zlib
from dataclasses import dataclass
from pathlib import Path

from PIL import Image, ImageChops


ROOT = Path(__file__).resolve().parents[1]
DEFAULT_SOURCE_ASE = ROOT / "human-work" / "英灵召唤器6.ase"
DEFAULT_SOURCE_PNG = ROOT / "human-work" / "英灵召唤器6.png"
DEFAULT_RELIC_PNG = ROOT / "human-work" / "罗马军团战士遗物.png"
DEFAULT_OUTPUT = ROOT / "human-work" / "英灵召唤器6-整理输出"

SOURCE_SIZE = (336, 256)
GUI_SIZE = (241, 201)
MASTER_SIZE = (512, 256)
SHEET_X = 256
TRANSPARENT = (0, 0, 0, 0)


@dataclass
class SourceLayer:
    name: str
    flags: int
    kind: int
    level: int
    opacity: int
    image: Image.Image


@dataclass
class OutputLayer:
    name: str
    level: int
    kind: int = 0
    visible: bool = True
    collapsed: bool = False
    image: Image.Image | None = None

    @property
    def flags(self) -> int:
        flags = 2
        if self.visible:
            flags |= 1
        if self.collapsed:
            flags |= 32
        return flags


def u16(data: bytes, offset: int) -> int:
    return struct.unpack_from("<H", data, offset)[0]


def s16(data: bytes, offset: int) -> int:
    return struct.unpack_from("<h", data, offset)[0]


def u32(data: bytes, offset: int) -> int:
    return struct.unpack_from("<I", data, offset)[0]


def read_ase(path: Path) -> tuple[int, int, list[SourceLayer]]:
    data = path.read_bytes()
    if u16(data, 4) != 0xA5E0:
        raise ValueError(f"Not an Aseprite file: {path}")
    width, height, depth = u16(data, 8), u16(data, 10), u16(data, 12)
    if depth != 32:
        raise ValueError(f"Expected RGBA Aseprite, got {depth}-bit")

    metadata: list[dict[str, int | str]] = []
    cels: list[tuple[int, int, int, int, int, int, bytes]] = []
    pos = 128
    for _ in range(u16(data, 6)):
        frame_size = u32(data, pos)
        chunks = u32(data, pos + 12) or u16(data, pos + 6)
        chunk_pos = pos + 16
        for _ in range(chunks):
            size = u32(data, chunk_pos)
            chunk_type = u16(data, chunk_pos + 4)
            if chunk_type == 0x2004:
                name_len = u16(data, chunk_pos + 22)
                metadata.append(
                    {
                        "name": data[
                            chunk_pos + 24 : chunk_pos + 24 + name_len
                        ].decode("utf-8"),
                        "flags": u16(data, chunk_pos + 6),
                        "kind": u16(data, chunk_pos + 8),
                        "level": u16(data, chunk_pos + 10),
                        "opacity": data[chunk_pos + 18],
                    }
                )
            elif chunk_type == 0x2005:
                layer_index = u16(data, chunk_pos + 6)
                x, y = s16(data, chunk_pos + 8), s16(data, chunk_pos + 10)
                opacity = data[chunk_pos + 12]
                cel_type = u16(data, chunk_pos + 13)
                if cel_type == 2:
                    cel_width = u16(data, chunk_pos + 22)
                    cel_height = u16(data, chunk_pos + 24)
                    raw = zlib.decompress(data[chunk_pos + 26 : chunk_pos + size])
                    cels.append(
                        (layer_index, x, y, cel_width, cel_height, opacity, raw)
                    )
            chunk_pos += size
        pos += frame_size

    layers: list[SourceLayer] = []
    for index, meta in enumerate(metadata):
        canvas = Image.new("RGBA", (width, height), TRANSPARENT)
        for layer_index, x, y, cw, ch, opacity, raw in cels:
            if layer_index != index:
                continue
            cel = Image.frombytes("RGBA", (cw, ch), raw)
            if opacity != 255:
                cel.putalpha(cel.getchannel("A").point(lambda v: v * opacity // 255))
            canvas.alpha_composite(cel, (x, y))
        layers.append(
            SourceLayer(
                str(meta["name"]),
                int(meta["flags"]),
                int(meta["kind"]),
                int(meta["level"]),
                int(meta["opacity"]),
                canvas,
            )
        )
    return width, height, layers


def compose(size: tuple[int, int], images: list[Image.Image]) -> Image.Image:
    result = Image.new("RGBA", size, TRANSPARENT)
    for image in images:
        result.alpha_composite(image)
    return result


def image_at(sprite: Image.Image, xy: tuple[int, int]) -> Image.Image:
    canvas = Image.new("RGBA", MASTER_SIZE, TRANSPARENT)
    canvas.alpha_composite(sprite, xy)
    return canvas


def layer_chunk(layer: OutputLayer) -> bytes:
    name = layer.name.encode("utf-8")
    payload = struct.pack(
        "<HHHHHHB3xH",
        layer.flags,
        layer.kind,
        layer.level,
        MASTER_SIZE[0],
        MASTER_SIZE[1],
        0,
        255,
        len(name),
    ) + name
    return struct.pack("<IH", 6 + len(payload), 0x2004) + payload


def cel_chunk(layer_index: int, image: Image.Image) -> bytes | None:
    bbox = image.getbbox()
    if not bbox:
        return None
    crop = image.crop(bbox).convert("RGBA")
    compressed = zlib.compress(crop.tobytes(), level=9)
    payload = struct.pack(
        "<HhhBHh5xHH",
        layer_index,
        bbox[0],
        bbox[1],
        255,
        2,
        0,
        crop.width,
        crop.height,
    ) + compressed
    return struct.pack("<IH", 6 + len(payload), 0x2005) + payload


def write_ase(path: Path, layers: list[OutputLayer]) -> None:
    chunks = [layer_chunk(layer) for layer in layers]
    for index, layer in enumerate(layers):
        if layer.kind == 0 and layer.image is not None:
            cel = cel_chunk(index, layer.image)
            if cel is not None:
                chunks.append(cel)
    payload = b"".join(chunks)
    frame_size = 16 + len(payload)
    frame = struct.pack("<IHHH2xI", frame_size, 0xF1FA, len(chunks), 100, len(chunks)) + payload
    header = bytearray(128)
    struct.pack_into("<I", header, 0, 128 + len(frame))
    struct.pack_into("<H", header, 4, 0xA5E0)
    struct.pack_into("<H", header, 6, 1)
    struct.pack_into("<H", header, 8, MASTER_SIZE[0])
    struct.pack_into("<H", header, 10, MASTER_SIZE[1])
    struct.pack_into("<H", header, 12, 32)
    struct.pack_into("<I", header, 14, 1)
    struct.pack_into("<H", header, 18, 100)
    header[34] = 1
    header[35] = 1
    struct.pack_into("<H", header, 40, 16)
    struct.pack_into("<H", header, 42, 16)
    path.write_bytes(bytes(header) + frame)


def render_visible_layers(layers: list[OutputLayer]) -> Image.Image:
    result = Image.new("RGBA", MASTER_SIZE, TRANSPARENT)
    group_visibility: dict[int, bool] = {}
    for layer in layers:
        for level in list(group_visibility):
            if level >= layer.level:
                del group_visibility[level]
        effective = layer.visible and all(group_visibility.values())
        if layer.kind == 1:
            group_visibility[layer.level] = effective
        elif effective and layer.image is not None:
            result.alpha_composite(layer.image)
    return result


def crop(image: Image.Image, box: tuple[int, int, int, int]) -> Image.Image:
    return image.crop(box).copy()


def build_assets(source: dict[str, Image.Image]) -> dict[str, object]:
    background = compose(
        SOURCE_SIZE,
        [source["打底"], source["右侧拓展栏"]],
    )
    static = compose(
        SOURCE_SIZE,
        [
            source["打底"],
            source["右侧拓展栏"],
            source["召唤GUI组件"],
            source["经验条背景"],
            source["技能框底图"],
            source["装饰花纹"],
        ],
    ).crop((0, 0, *GUI_SIZE))
    background_gui = background.crop((0, 0, *GUI_SIZE))
    # Dynamic controls have their own PNGs. Restore the panel beneath them while
    # preserving the source-authored outer frames for the two progress bars.
    for box in [
        (61, 71, 81, 91),
        (83, 71, 103, 91),
        (105, 71, 125, 91),
        (127, 71, 147, 91),
        (149, 71, 169, 91),
        (178, 90, 196, 108),
        (197, 90, 215, 108),
        (216, 90, 234, 108),
        (178, 123, 196, 141),
        (197, 123, 215, 141),
        (216, 123, 234, 141),
        (178, 143, 234, 162),
    ]:
        static.paste(background_gui.crop(box), box)
    static.paste(background_gui.crop((8, 114, 168, 116)), (8, 114))
    static.paste(background_gui.crop((179, 165, 233, 168)), (179, 165))

    attrs = source["基础属性图标"]
    attributes = {
        "health": crop(attrs, (61, 19, 72, 30)),
        "level": crop(attrs, (134, 19, 145, 30)),
        "attack_damage": crop(attrs, (61, 32, 72, 43)),
        "attack_speed": crop(attrs, (116, 32, 127, 43)),
        "armor": crop(attrs, (61, 45, 72, 56)),
        "movement_speed": crop(attrs, (116, 45, 127, 56)),
        "alert_range": crop(attrs, (61, 58, 72, 69)),
        "summon_cost_ratio": crop(attrs, (116, 58, 127, 69)),
    }

    traits_source = source["天赋图标"]
    # Source order in 英灵召唤器6.ase: 坏脾气、壮硕、瘦削、慵懒、勇气。
    # Keep exported filenames semantic so runtime code never needs an inverted mapping.
    trait_names = ["bad_temper", "sturdy", "skinny", "lazy", "courage"]
    traits = {
        name: crop(traits_source, (113 + i * 11, 6, 124 + i * 11, 17))
        for i, name in enumerate(trait_names)
    }

    skills_source = source["技能图标"]
    skills = {
        "soldier_formation": crop(skills_source, (63, 73, 79, 89)),
        "shield_charge": crop(skills_source, (85, 73, 101, 89)),
        "legion_endures": crop(skills_source, (107, 73, 123, 89)),
    }

    modes_source = source["行动模式图标"]
    activity = {
        name: crop(modes_source, (179 + i * 19, 91, 195 + i * 19, 107))
        for i, name in enumerate(["follow", "wait", "wander"])
    }
    alert = {
        name: crop(modes_source, (179 + i * 19, 124, 195 + i * 19, 140))
        for i, name in enumerate(["aggressive", "defensive", "peaceful"])
    }

    mode_source = source["默认触摸按下按钮底板"]
    mode_states = {
        "default": crop(mode_source, (243, 120, 261, 138)),
        "hover": crop(mode_source, (265, 120, 283, 138)),
        "pressed": crop(mode_source, (287, 120, 305, 138)),
    }
    skill_source = source["技能框底图"]
    skill_states = {
        "occupied": crop(skill_source, (61, 71, 81, 91)),
        "empty": crop(skill_source, (127, 71, 147, 91)),
    }
    summon_source = source["召唤，召回按钮 复制"]
    summon_states = {
        "default": crop(summon_source, (178, 143, 234, 162)),
        "hover": crop(summon_source, (243, 144, 299, 163)),
        "pressed": crop(summon_source, (243, 165, 299, 184)),
    }
    bars = {
        "experience_background": crop(source["经验条背景"], (8, 114, 168, 116)),
        "experience_fill": crop(source["经验条填充"], (174, 252, 334, 254)),
        "fuel_background": crop(source["召唤GUI组件"], (179, 165, 233, 168)),
        "fuel_fill": crop(source["燃料槽填充图"], (279, 248, 333, 251)),
    }
    return {
        "static": static,
        "attributes": attributes,
        "traits": traits,
        "skills": skills,
        "activity": activity,
        "alert": alert,
        "mode_states": mode_states,
        "skill_states": skill_states,
        "summon_states": summon_states,
        "bars": bars,
    }


def build_master_layers(
    original: Image.Image, assets: dict[str, object]
) -> list[OutputLayer]:
    layers: list[OutputLayer] = []

    def group(name: str, level: int, visible: bool = True) -> None:
        layers.append(
            OutputLayer(name, level, kind=1, visible=visible, collapsed=True)
        )

    def normal(name: str, level: int, image: Image.Image, visible: bool = True) -> None:
        layers.append(OutputLayer(name, level, image=image, visible=visible))

    static = assets["static"]
    assert isinstance(static, Image.Image)
    attributes = assets["attributes"]
    traits = assets["traits"]
    skills = assets["skills"]
    activity = assets["activity"]
    alert = assets["alert"]
    modes = assets["mode_states"]
    skill_states = assets["skill_states"]
    summon = assets["summon_states"]
    bars = assets["bars"]
    assert all(isinstance(value, dict) for value in [attributes, traits, skills, activity, alert, modes, skill_states, summon, bars])

    group("00_原稿备份_默认隐藏", 0, visible=False)
    normal("英灵召唤器6_原始合成图", 1, image_at(original, (0, 0)))

    group("01_GUI完整预览_241x201", 0)
    normal("静态GUI底图_严格241x201", 1, image_at(static, (0, 0)))

    attr_preview = Image.new("RGBA", MASTER_SIZE, TRANSPARENT)
    attr_positions = [
        (61, 19), (134, 19), (61, 32), (116, 32),
        (61, 45), (116, 45), (61, 58), (116, 58),
    ]
    for sprite, xy in zip(attributes.values(), attr_positions):
        attr_preview.alpha_composite(sprite, xy)
    normal("基础属性图标_八项", 1, attr_preview)

    trait_preview = Image.new("RGBA", MASTER_SIZE, TRANSPARENT)
    # The design supports 0-4 traits and anchors them against x=168 from right to left.
    for sprite, x in zip(list(traits.values())[:4], [157, 146, 135, 124]):
        trait_preview.alpha_composite(sprite, (x, 6))
    normal("天赋图标_最多四个_从右向左占位", 1, trait_preview)

    skill_frame_preview = Image.new("RGBA", MASTER_SIZE, TRANSPARENT)
    for i in range(5):
        state = "occupied" if i < 3 else "empty"
        skill_frame_preview.alpha_composite(skill_states[state], (61 + i * 22, 71))
    normal("技能底板_三个有技能_两个空槽", 1, skill_frame_preview)
    skill_icon_preview = Image.new("RGBA", MASTER_SIZE, TRANSPARENT)
    for i, sprite in enumerate(skills.values()):
        skill_icon_preview.alpha_composite(sprite, (63 + i * 22, 73))
    normal("技能图标_罗马军团兵", 1, skill_icon_preview)

    mode_preview = Image.new("RGBA", MASTER_SIZE, TRANSPARENT)
    for row_y, icons, selected in [(90, activity, 0), (123, alert, 1)]:
        for i, sprite in enumerate(icons.values()):
            x = 178 + i * 19
            mode_preview.alpha_composite(modes["pressed" if i == selected else "default"], (x, row_y))
            mode_preview.alpha_composite(sprite, (x + 1, row_y + 1))
    normal("行动与警戒模式_示例选中", 1, mode_preview)
    normal("召唤召回按钮_默认状态", 1, image_at(summon["default"], (178, 143)))

    bar_preview = Image.new("RGBA", MASTER_SIZE, TRANSPARENT)
    bar_preview.alpha_composite(bars["experience_background"], (8, 114))
    bar_preview.alpha_composite(crop(bars["experience_fill"], (0, 0, 96, 2)), (8, 114))
    bar_preview.alpha_composite(bars["fuel_background"], (179, 165))
    bar_preview.alpha_composite(crop(bars["fuel_fill"], (0, 0, 34, 3)), (179, 165))
    normal("经验与燃料_填充示例", 1, bar_preview)

    group("02_独立组件素材区", 0)
    sheet = Image.new("RGBA", MASTER_SIZE, TRANSPARENT)
    x0 = SHEET_X + 8
    for i, sprite in enumerate(modes.values()):
        sheet.alpha_composite(sprite, (x0 + i * 22, 8))
    for i, sprite in enumerate(skill_states.values()):
        sheet.alpha_composite(sprite, (x0 + i * 24, 34))
    for i, sprite in enumerate(summon.values()):
        sheet.alpha_composite(sprite, (x0, 60 + i * 23))
    sheet.alpha_composite(bars["experience_background"], (x0, 132))
    sheet.alpha_composite(bars["experience_fill"], (x0, 137))
    sheet.alpha_composite(bars["fuel_background"], (x0, 143))
    sheet.alpha_composite(bars["fuel_fill"], (x0, 149))
    normal("按钮与进度条_原图裁切", 1, sheet)

    icon_sheet = Image.new("RGBA", MASTER_SIZE, TRANSPARENT)
    for i, sprite in enumerate(attributes.values()):
        icon_sheet.alpha_composite(sprite, (x0 + i * 14, 160))
    for i, sprite in enumerate(traits.values()):
        icon_sheet.alpha_composite(sprite, (x0 + i * 14, 174))
    for i, sprite in enumerate(skills.values()):
        icon_sheet.alpha_composite(sprite, (x0 + i * 18, 190))
    for i, sprite in enumerate(activity.values()):
        icon_sheet.alpha_composite(sprite, (x0 + i * 18, 208))
    for i, sprite in enumerate(alert.values()):
        icon_sheet.alpha_composite(sprite, (x0 + i * 18, 226))
    normal("属性天赋技能模式_独立图标", 1, icon_sheet)
    return layers


def save_png_tree(base: Path, assets: dict[str, object], relic: Image.Image | None) -> None:
    directories = [
        base / "widgets",
        base / "bars",
        base / "attributes",
        base / "traits",
        base / "skills" / "roman_legionary",
        base / "modes" / "activity",
        base / "modes" / "alert",
    ]
    if relic is not None:
        directories.append(base / "items")
    for directory in directories:
        directory.mkdir(parents=True, exist_ok=True)

    static = assets["static"]
    assert isinstance(static, Image.Image)
    static.save(base / "summoner_screen.png")
    mappings = [
        ("mode_states", base / "widgets", "mode_"),
        ("skill_states", base / "widgets", "skill_"),
        ("summon_states", base / "widgets", "summon_"),
        ("attributes", base / "attributes", ""),
        ("traits", base / "traits", ""),
        ("skills", base / "skills" / "roman_legionary", ""),
        ("activity", base / "modes" / "activity", ""),
        ("alert", base / "modes" / "alert", ""),
    ]
    for key, directory, prefix in mappings:
        sprites = assets[key]
        assert isinstance(sprites, dict)
        for name, sprite in sprites.items():
            sprite.save(directory / f"{prefix}{name}.png")
    bars = assets["bars"]
    assert isinstance(bars, dict)
    bars["experience_background"].save(base / "bars" / "experience_background.png")
    bars["experience_fill"].save(base / "bars" / "experience_fill.png")
    bars["fuel_background"].save(base / "bars" / "fuel_background.png")
    bars["fuel_fill"].save(base / "bars" / "fuel_fill.png")
    if relic is not None:
        relic.save(base / "items" / "roman_legionary_relic.png")


def build_manifest() -> dict[str, object]:
    return {
        "source_canvas": {"width": 336, "height": 256},
        "gui": {"x": 0, "y": 0, "width": 241, "height": 201},
        "model_viewport": {"outer": [7, 19, 51, 72], "inner": [8, 20, 49, 70]},
        "name": {"box": [7, 6, 162, 11], "reserved_trait_area": [124, 6, 44, 11]},
        "traits": {
            "maximum": 4,
            "layout": "right_to_left",
            "positions": [[157, 6], [146, 6], [135, 6], [124, 6]],
            "sprite_size": [11, 11],
            "generation_weights_percent": {"0": 10, "1": 25, "2": 40, "3": 20, "4": 5},
        },
        "attributes": {
            "health": [61, 19, 71, 11],
            "level": [134, 19, 35, 11],
            "attack_damage": [61, 32, 53, 11],
            "attack_speed": [116, 32, 53, 11],
            "armor": [61, 45, 53, 11],
            "movement_speed": [116, 45, 53, 11],
            "alert_range": [61, 58, 53, 11],
            "summon_cost_ratio": [116, 58, 53, 11],
        },
        "skills": {
            "frame_positions": [[61, 71], [83, 71], [105, 71], [127, 71], [149, 71]],
            "frame_size": [20, 20],
            "icon_size": [16, 16],
        },
        "modules": {
            "positions": [[7, 93], [36, 93], [65, 93], [93, 93], [122, 93], [151, 93]],
            "slot_size": [18, 18],
        },
        "inventory": {
            "columns": 9,
            "rows": 3,
            "origin": [7, 119],
            "slot_size": [18, 18],
            "hotbar_origin": [7, 176],
        },
        "modes": {
            "activity_origin": [178, 90],
            "alert_origin": [178, 123],
            "spacing_x": 19,
            "button_size": [18, 18],
        },
        "summon_button": {"position": [178, 143], "size": [56, 19]},
        "experience": {"position": [8, 114], "size": [160, 2], "outer": [7, 113, 162, 4]},
        "fuel": {"position": [179, 165], "size": [54, 3], "outer": [178, 164, 56, 5]},
        "input_slots": {
            "fuel": [178, 171, 18, 18],
            "relic": [216, 171, 18, 18],
        },
        "mechanics": {
            "summon_cost_ratio_affects": ["summon_cost", "natural_healing_fuel_cost"]
        },
    }


README = """# 英灵召唤器 GUI v6 整理输出

本目录由 `human-work/英灵召唤器6.ase` 自动生成。原始 ASE 与 PNG 不会被覆盖。

## 核心尺寸

- 游戏静态 GUI：严格 241×201。
- 经验条可填充区域：严格 160×2，游戏坐标 (8, 114)。
- 燃料条可填充区域：严格 54×3，游戏坐标 (179, 165)。
- 召唤/召回按钮：56×19。
- 模式按钮：18×18。
- 技能底板：20×20；技能图标：16×16。
- 属性、天赋图标：11×11。

## 技能框

`widgets/skill_occupied.png` 与 `widgets/skill_empty.png` 都是直接从原稿图层裁切，未按色号重画。前三个示例槽使用白色内边的 occupied，后两个使用 #C6C6C6 内边的 empty。

## 天赋

原稿中保留了五个天赋图标案例，分别导出为 bad_temper、lazy、courage、skinny、sturdy。实际规则为随机生成 0～4 个，2 个权重最高；GUI 固定预留最右四格并从右向左排列。没有天赋时不显示图标。

## 游戏资源

`游戏资源参考` 使用与运行时资源相同的独立 PNG 目录结构，供人工检查。`manifest.json` 记录所有运行时坐标。

安全生成工作母版和参考资源（不会覆盖正式游戏资源）：

    python scripts/build-summoner-gui-v6.py

确认参考资源正确后，才显式指定正式游戏资源目录：

    python scripts/build-summoner-gui-v6.py --game-output src/main/resources/assets/echo_warrior/textures/gui/summoner
"""


def main() -> None:
    parser = argparse.ArgumentParser(description="拆分英灵召唤器 v6 GUI 并建立中文图层母版")
    parser.add_argument("--source-ase", type=Path, default=DEFAULT_SOURCE_ASE)
    parser.add_argument("--source-png", type=Path, default=DEFAULT_SOURCE_PNG)
    parser.add_argument("--relic-png", type=Path, default=DEFAULT_RELIC_PNG)
    parser.add_argument("--output", type=Path, default=DEFAULT_OUTPUT)
    parser.add_argument(
        "--game-output",
        type=Path,
        default=None,
        help=(
            "Optional runtime asset directory. Omit this argument to generate only "
            "the review copy under --output."
        ),
    )
    args = parser.parse_args()

    width, height, parsed = read_ase(args.source_ase)
    if (width, height) != SOURCE_SIZE:
        raise ValueError(f"Unexpected source canvas: {(width, height)}")
    source = {layer.name: layer.image for layer in parsed}
    required = {
        "打底", "右侧拓展栏", "召唤GUI组件", "默认触摸按下按钮底板",
        "行动模式图标", "召唤，召回按钮 复制", "经验条背景", "经验条填充",
        "燃料槽填充图", "技能框底图", "技能图标", "基础属性图标",
        "天赋图标", "装饰花纹",
    }
    missing = required.difference(source)
    if missing:
        raise ValueError(f"Missing source layers: {sorted(missing)}")

    original = Image.open(args.source_png).convert("RGBA")
    parsed_composite = compose(SOURCE_SIZE, [layer.image for layer in parsed])
    if ImageChops.difference(parsed_composite, original).getbbox() is not None:
        raise RuntimeError("ASE layer composite differs from the supplied PNG")

    relic = Image.open(args.relic_png).convert("RGBA") if args.relic_png.exists() else None
    assets = build_assets(source)
    layers = build_master_layers(original, assets)
    manifest = build_manifest()

    args.output.mkdir(parents=True, exist_ok=True)
    master_path = args.output / "英灵召唤器6_GUI工作母版.ase"
    master_preview = args.output / "英灵召唤器6_GUI工作母版.png"
    game_preview = args.output / "GUI游戏预览_4倍.png"
    write_ase(master_path, layers)
    preview = render_visible_layers(layers)
    preview.save(master_preview)
    preview.crop((0, 0, *GUI_SIZE)).resize(
        (GUI_SIZE[0] * 4, GUI_SIZE[1] * 4), Image.Resampling.NEAREST
    ).save(game_preview)

    reference = args.output / "游戏资源参考"
    save_png_tree(reference, assets, relic)
    if args.game_output is not None:
        save_png_tree(args.game_output, assets, relic)
    (args.output / "manifest.json").write_text(
        json.dumps(manifest, ensure_ascii=False, indent=2), encoding="utf-8"
    )
    (reference / "manifest.json").write_text(
        json.dumps(manifest, ensure_ascii=False, indent=2), encoding="utf-8"
    )
    (args.output / "README.md").write_text(README, encoding="utf-8")

    check_w, check_h, check_layers = read_ase(master_path)
    if (check_w, check_h) != MASTER_SIZE or len(check_layers) != len(layers):
        raise RuntimeError("Generated Aseprite master failed validation")
    if assets["static"].size != GUI_SIZE:
        raise RuntimeError("summoner_screen.png is not exactly 241x201")

    summary = {
        "source": str(args.source_ase),
        "output": str(args.output),
        "game_output": str(args.game_output) if args.game_output is not None else None,
        "master": str(master_path),
        "master_layers": len(layers),
        "gui_size": list(GUI_SIZE),
    }
    (args.output / "生成信息.json").write_text(
        json.dumps(summary, ensure_ascii=False, indent=2), encoding="utf-8"
    )
    print(json.dumps(summary, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    try:
        main()
    except Exception as exc:
        print(f"ERROR: {exc}", file=sys.stderr)
        raise
