from __future__ import annotations

import argparse
import json
import struct
import zipfile
import zlib
from dataclasses import dataclass
from pathlib import Path

from PIL import Image, ImageDraw


ROOT = Path(__file__).resolve().parents[1]
DEFAULT_SOURCE_ASE = ROOT / "human-work" / "英灵召唤器3.ase"
DEFAULT_SOURCE_PNG = ROOT / "human-work" / "英灵召唤器3.png"
DEFAULT_RELIC_PNG = ROOT / "human-work" / "罗马军团战士遗物.png"
DEFAULT_OUTPUT = ROOT / "human-work" / "英灵召唤器3-整理输出"

GUI_WIDTH = 240
GUI_HEIGHT = 210
MASTER_WIDTH = 512
MASTER_HEIGHT = 256
SHEET_X = 256

WHITE = (255, 255, 255, 255)
LIGHT = (198, 198, 198, 255)
MID = (162, 162, 162, 255)
GRAY = (139, 139, 139, 255)
DARK = (55, 55, 55, 255)
BLACK = (0, 0, 0, 255)
PURPLE = (151, 141, 201, 255)
PURPLE_DARK = (85, 73, 160, 255)
PURPLE_PANEL = (57, 55, 86, 255)
PURPLE_DEEP = (29, 26, 49, 255)
RED = (139, 52, 52, 255)
RED_DARK = (77, 25, 31, 255)
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
        flags = 2  # editable
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
    width = u16(data, 8)
    height = u16(data, 10)
    depth = u16(data, 12)
    if depth != 32:
        raise ValueError(f"Expected a 32-bit RGBA Aseprite file, got {depth}-bit")

    layer_meta: list[dict[str, int | str]] = []
    cels: list[tuple[int, int, int, int, int, int, bytes]] = []
    pos = 128
    for _frame in range(u16(data, 6)):
        frame_size = u32(data, pos)
        chunks = u32(data, pos + 12) or u16(data, pos + 6)
        chunk_pos = pos + 16
        for _ in range(chunks):
            size = u32(data, chunk_pos)
            chunk_type = u16(data, chunk_pos + 4)
            if chunk_type == 0x2004:
                name_len = u16(data, chunk_pos + 22)
                name = data[chunk_pos + 24 : chunk_pos + 24 + name_len].decode("utf-8")
                layer_meta.append(
                    {
                        "name": name,
                        "flags": u16(data, chunk_pos + 6),
                        "kind": u16(data, chunk_pos + 8),
                        "level": u16(data, chunk_pos + 10),
                        "opacity": data[chunk_pos + 18],
                    }
                )
            elif chunk_type == 0x2005:
                layer_index = u16(data, chunk_pos + 6)
                x = s16(data, chunk_pos + 8)
                y = s16(data, chunk_pos + 10)
                opacity = data[chunk_pos + 12]
                cel_type = u16(data, chunk_pos + 13)
                if cel_type == 2:
                    cel_width = u16(data, chunk_pos + 22)
                    cel_height = u16(data, chunk_pos + 24)
                    raw = zlib.decompress(data[chunk_pos + 26 : chunk_pos + size])
                    cels.append((layer_index, x, y, cel_width, cel_height, opacity, raw))
            chunk_pos += size
        pos += frame_size

    layers: list[SourceLayer] = []
    for index, meta in enumerate(layer_meta):
        canvas = Image.new("RGBA", (width, height), TRANSPARENT)
        for layer_index, x, y, cel_width, cel_height, opacity, raw in cels:
            if layer_index != index:
                continue
            cel = Image.frombytes("RGBA", (cel_width, cel_height), raw)
            if opacity != 255:
                alpha = cel.getchannel("A").point(lambda value: value * opacity // 255)
                cel.putalpha(alpha)
            canvas.alpha_composite(cel, (x, y))
        layers.append(
            SourceLayer(
                name=str(meta["name"]),
                flags=int(meta["flags"]),
                kind=int(meta["kind"]),
                level=int(meta["level"]),
                opacity=int(meta["opacity"]),
                image=canvas,
            )
        )
    return width, height, layers


def image_at(size: tuple[int, int], sprite: Image.Image, xy: tuple[int, int]) -> Image.Image:
    canvas = Image.new("RGBA", size, TRANSPARENT)
    canvas.alpha_composite(sprite, xy)
    return canvas


def compose(size: tuple[int, int], images: list[Image.Image]) -> Image.Image:
    result = Image.new("RGBA", size, TRANSPARENT)
    for image in images:
        result.alpha_composite(image)
    return result


def crop_sprite(image: Image.Image, box: tuple[int, int, int, int]) -> Image.Image:
    return image.crop(box).copy()


def icon_canvas(sprite: Image.Image, target_size: tuple[int, int] = (16, 16)) -> Image.Image:
    result = Image.new("RGBA", target_size, TRANSPARENT)
    bbox = sprite.getbbox()
    if not bbox:
        return result
    sprite = sprite.crop(bbox)
    x = (target_size[0] - sprite.width) // 2
    y = (target_size[1] - sprite.height) // 2
    result.alpha_composite(sprite, (x, y))
    return result


def make_attack_speed_icon() -> Image.Image:
    image = Image.new("RGBA", (16, 16), TRANSPARENT)
    draw = ImageDraw.Draw(image)
    # A small blade plus three speed streaks. This is deliberately a replaceable prototype.
    outline = (49, 42, 66, 255)
    blade = (225, 225, 236, 255)
    shine = (255, 255, 255, 255)
    gold = (231, 180, 54, 255)
    for x, y in [(4, 11), (5, 10), (6, 9), (7, 8), (8, 7), (9, 6), (10, 5)]:
        draw.point((x - 1, y), fill=outline)
        draw.point((x, y + 1), fill=outline)
        draw.point((x, y), fill=blade)
    draw.point((10, 5), fill=shine)
    draw.line((3, 12, 6, 12), fill=outline)
    draw.line((4, 13, 5, 13), fill=gold)
    draw.line((9, 3, 13, 3), fill=gold)
    draw.line((10, 5, 14, 5), fill=gold)
    draw.line((11, 7, 14, 7), fill=gold)
    return image


def make_mode_button(state: str, source_states: dict[str, Image.Image]) -> Image.Image:
    if state in source_states:
        return source_states[state].copy()
    image = source_states["default"].copy().convert("RGBA")
    if state == "disabled":
        pixels = []
        for r, g, b, a in image.getdata():
            gray = int((r + g + b) / 3)
            gray = (gray + 150) // 2
            pixels.append((gray, gray, gray, a))
        image.putdata(pixels)
        draw = ImageDraw.Draw(image)
        draw.rectangle((1, 1, 16, 16), outline=(125, 125, 125, 255))
    return image


def make_skill_frame(state: str) -> Image.Image:
    image = Image.new("RGBA", (18, 18), TRANSPARENT)
    draw = ImageDraw.Draw(image)
    if state == "hover":
        outer, inner, fill = (222, 219, 236, 255), PURPLE, PURPLE_PANEL
    elif state == "user_disabled":
        outer, inner, fill = (112, 74, 80, 255), DARK, PURPLE_DEEP
    elif state == "unavailable":
        outer, inner, fill = GRAY, DARK, (73, 73, 77, 255)
    else:
        outer, inner, fill = DARK, (88, 83, 123, 255), PURPLE_DEEP
    draw.rectangle((0, 0, 17, 17), fill=outer)
    draw.rectangle((1, 1, 16, 16), fill=inner)
    draw.rectangle((2, 2, 15, 15), fill=fill)
    if state == "user_disabled":
        draw.line((3, 14, 14, 3), fill=(70, 18, 25, 255), width=3)
        draw.line((3, 14, 14, 3), fill=(190, 62, 67, 255), width=1)
    return image


def make_talent_frame(state: str) -> Image.Image:
    image = Image.new("RGBA", (18, 18), TRANSPARENT)
    draw = ImageDraw.Draw(image)
    border = PURPLE if state == "hover" else DARK
    fill = (46, 43, 55, 255)
    draw.rectangle((0, 0, 17, 17), fill=fill)
    draw.line((0, 0, 5, 0), fill=border)
    draw.line((0, 0, 0, 5), fill=border)
    draw.line((12, 0, 17, 0), fill=border)
    draw.line((17, 0, 17, 5), fill=border)
    draw.line((0, 12, 0, 17), fill=border)
    draw.line((0, 17, 5, 17), fill=border)
    draw.line((17, 12, 17, 17), fill=border)
    draw.line((12, 17, 17, 17), fill=border)
    if state == "hover":
        draw.rectangle((1, 1, 16, 16), outline=(184, 177, 217, 255))
    return image


def make_confirm_button(hover_button: Image.Image) -> Image.Image:
    image = hover_button.copy().convert("RGBA")
    converted = []
    for r, g, b, a in image.getdata():
        luminance = (r + g + b) // 3
        converted.append(
            (
                min(210, 60 + luminance // 2),
                min(105, 20 + luminance // 4),
                min(110, 25 + luminance // 4),
                a,
            )
        )
    image.putdata(converted)
    return image


def three_pixel_bar(source: Image.Image) -> Image.Image:
    source = source.convert("RGBA")
    if source.height == 3:
        return source.copy()
    result = Image.new("RGBA", (source.width, 3), TRANSPARENT)
    result.paste(source.crop((0, 0, source.width, 1)), (0, 0))
    result.paste(source.crop((0, source.height - 1, source.width, source.height)), (0, 1))
    result.paste(source.crop((0, 0, source.width, 1)), (0, 2))
    return result


class MinecraftFont:
    def __init__(self, client_jar: Path, unifont_zip: Path):
        with zipfile.ZipFile(client_jar) as archive:
            self.ascii = Image.open(
                archive.open("assets/minecraft/textures/font/ascii.png")
            ).convert("RGBA")
        self.unifont: dict[int, str] = {}
        with zipfile.ZipFile(unifont_zip) as archive:
            hex_names = [name for name in archive.namelist() if name.endswith(".hex")]
            if not hex_names:
                raise ValueError(f"No .hex font inside {unifont_zip}")
            for line in archive.read(hex_names[0]).decode("ascii").splitlines():
                code, bits = line.split(":", 1)
                self.unifont[int(code, 16)] = bits.strip()

    def ascii_glyph(self, char: str) -> tuple[Image.Image, int]:
        code = ord(char)
        if code >= 256:
            raise KeyError(char)
        x = (code % 16) * 8
        y = (code // 16) * 8
        glyph = self.ascii.crop((x, y, x + 8, y + 8))
        if char == " ":
            return Image.new("RGBA", (0, 8), TRANSPARENT), 4
        alpha = glyph.getchannel("A")
        bbox = alpha.getbbox()
        if not bbox:
            return glyph, 4
        width = bbox[2]
        return glyph, width + 1

    def unihex_glyph(self, char: str) -> tuple[Image.Image, int]:
        bits = self.unifont.get(ord(char))
        if bits is None:
            return self.ascii_glyph("?")
        bit_count = len(bits) * 4
        width = bit_count // 16
        if width not in (8, 16, 24, 32):
            return self.ascii_glyph("?")
        value = int(bits, 16)
        image = Image.new("L", (width, 16), 0)
        pixels = image.load()
        for row in range(16):
            for col in range(width):
                shift = bit_count - 1 - (row * width + col)
                pixels[col, row] = 255 if (value >> shift) & 1 else 0
        if 0x3200 <= ord(char) <= 0x9FFF or 0xFF01 <= ord(char) <= 0xFF5E:
            left, right = 0, 15
        else:
            bbox = image.getbbox()
            if not bbox:
                return Image.new("RGBA", (0, 8), TRANSPARENT), 4
            left, right = bbox[0], bbox[2] - 1
        cropped = image.crop((left, 0, right + 1, 16))
        logical_width = max(1, (right - left + 1) // 2)
        cropped = cropped.resize((logical_width, 8), Image.Resampling.BOX)
        rgba = Image.new("RGBA", cropped.size, WHITE)
        rgba.putalpha(cropped)
        advance = (right - left + 1) / 2 + 1
        return rgba, int(advance)

    def glyph(self, char: str) -> tuple[Image.Image, int]:
        if ord(char) < 256:
            return self.ascii_glyph(char)
        return self.unihex_glyph(char)

    def width(self, text: str) -> int:
        return sum(self.glyph(char)[1] for char in text)

    def draw(
        self,
        target: Image.Image,
        xy: tuple[int, int],
        text: str,
        color: tuple[int, int, int, int],
        shadow: bool = False,
        max_width: int | None = None,
    ) -> int:
        if max_width is not None and self.width(text) > max_width:
            suffix = "..."
            allowed = max(0, max_width - self.width(suffix))
            trimmed = ""
            for char in text:
                if self.width(trimmed + char) > allowed:
                    break
                trimmed += char
            text = trimmed + suffix
        x, y = xy
        if shadow:
            shadow_color = (
                color[0] // 4,
                color[1] // 4,
                color[2] // 4,
                color[3],
            )
            self._draw_raw(target, (x + 1, y + 1), text, shadow_color)
        return self._draw_raw(target, (x, y), text, color)

    def _draw_raw(
        self,
        target: Image.Image,
        xy: tuple[int, int],
        text: str,
        color: tuple[int, int, int, int],
    ) -> int:
        x, y = xy
        start = x
        for char in text:
            glyph, advance = self.glyph(char)
            if glyph.width:
                colored = Image.new("RGBA", glyph.size, color)
                colored.putalpha(glyph.getchannel("A"))
                target.alpha_composite(colored, (x, y))
            x += advance
        return x - start

    def draw_centered(
        self,
        target: Image.Image,
        rect: tuple[int, int, int, int],
        text: str,
        color: tuple[int, int, int, int],
        shadow: bool = False,
    ) -> None:
        left, top, right, bottom = rect
        width = self.width(text)
        x = left + (right - left - width) // 2
        y = top + (bottom - top - 9) // 2
        self.draw(target, (x, y), text, color, shadow=shadow)


def find_font_assets() -> tuple[Path, Path]:
    gradle = Path.home() / ".gradle" / "caches" / "fabric-loom"
    client = gradle / "26.1.2" / "minecraft-client-only.jar"
    index_path = gradle / "assets" / "indexes" / "26.1.2-30.json"
    index = json.loads(index_path.read_text(encoding="utf-8"))
    object_info = index["objects"]["minecraft/font/unifont.zip"]
    digest = object_info["hash"]
    unifont = gradle / "assets" / "objects" / digest[:2] / digest
    return client, unifont


def make_text_references(font: MinecraftFont) -> tuple[Image.Image, Image.Image]:
    chinese = Image.new("RGBA", (MASTER_WIDTH, MASTER_HEIGHT), TRANSPARENT)
    english = Image.new("RGBA", (MASTER_WIDTH, MASTER_HEIGHT), TRANSPARENT)
    dark_text = (64, 64, 64, 255)
    header_text = WHITE

    # Chinese, shown by default. Deliberately draw 30/60 even though it overflows.
    font.draw(chinese, (8, 3), "罗马军团兵召唤器", dark_text, max_width=160)
    font.draw(chinese, (63, 15), "罗马军团兵", dark_text, max_width=68)
    font.draw(chinese, (138, 15), "Lv.12", dark_text)
    for xy, value in [
        ((73, 30), "30/60"),
        ((110, 30), "1.0秒"),
        ((147, 30), "16格"),
        ((73, 43), "12"),
        ((110, 43), "8"),
        ((147, 43), "100%"),
    ]:
        font.draw(chinese, xy, value, dark_text)
    font.draw(chinese, (63, 56), "技能", header_text)
    font.draw(chinese, (9, 89), "升级模块", dark_text)
    font.draw(chinese, (180, 55), "天赋", header_text)
    font.draw(chinese, (180, 87), "行动模式", header_text)
    font.draw(chinese, (180, 120), "警戒状态", header_text)
    font.draw_centered(chinese, (178, 151, 234, 169), "召唤英灵", WHITE, shadow=True)
    font.draw(chinese, (180, 16), "遗物已载入", dark_text, max_width=54)
    font.draw(chinese, (189, 28), "未召唤", dark_text, max_width=45)

    # English guide, hidden by default. Short button labels are intentional.
    font.draw(english, (8, 3), "Roman Legionary Summoner", dark_text, max_width=160)
    font.draw(english, (63, 15), "Roman Legionary", dark_text, max_width=70)
    font.draw(english, (138, 15), "Lv.12", dark_text)
    for xy, value in [
        ((73, 30), "30/60"),
        ((110, 30), "1.0s"),
        ((147, 30), "16m"),
        ((73, 43), "12"),
        ((110, 43), "8"),
        ((147, 43), "100%"),
    ]:
        font.draw(english, xy, value, dark_text)
    font.draw(english, (63, 56), "Skills", header_text)
    font.draw(english, (9, 89), "Modules", dark_text)
    font.draw(english, (180, 55), "Talents", header_text, max_width=54)
    font.draw(english, (180, 87), "Activity", header_text, max_width=54)
    font.draw(english, (180, 120), "Stance", header_text, max_width=54)
    font.draw_centered(english, (178, 151, 234, 169), "Summon", WHITE, shadow=True)
    font.draw(english, (180, 16), "Relic Loaded", dark_text, max_width=54)
    font.draw(english, (180, 28), "Not Summoned", dark_text, max_width=54)
    return chinese, english


def make_guides(font: MinecraftFont) -> dict[str, Image.Image]:
    bounds = Image.new("RGBA", (MASTER_WIDTH, MASTER_HEIGHT), TRANSPARENT)
    draw = ImageDraw.Draw(bounds)
    draw.rectangle((0, 0, GUI_WIDTH - 1, GUI_HEIGHT - 1), outline=(255, 70, 70, 180))
    draw.rectangle((SHEET_X, 0, MASTER_WIDTH - 1, MASTER_HEIGHT - 1), outline=(70, 210, 255, 180))

    hitboxes = Image.new("RGBA", (MASTER_WIDTH, MASTER_HEIGHT), TRANSPARENT)
    draw = ImageDraw.Draw(hitboxes)
    for i in range(5):
        x = 61 + i * 19
        draw.rectangle((x, 68, x + 17, 85), outline=(255, 180, 40, 180))
    for y in (100, 133):
        for i in range(3):
            x = 178 + i * 19
            draw.rectangle((x, y, x + 17, y + 17), outline=(255, 180, 40, 180))
    draw.rectangle((178, 151, 233, 168), outline=(255, 180, 40, 180))
    draw.rectangle((178, 183, 195, 200), outline=(255, 180, 40, 180))
    draw.rectangle((216, 183, 233, 200), outline=(255, 180, 40, 180))

    model = Image.new("RGBA", (MASTER_WIDTH, MASTER_HEIGHT), TRANSPARENT)
    draw = ImageDraw.Draw(model)
    draw.rectangle((7, 14, 58, 85), outline=(80, 220, 255, 200))
    draw.line((7, 49, 58, 49), fill=(80, 220, 255, 100))
    draw.line((32, 14, 32, 85), fill=(80, 220, 255, 100))
    font.draw(model, (9, 76), "3D模型范围", (80, 220, 255, 220), max_width=48)

    bars = Image.new("RGBA", (MASTER_WIDTH, MASTER_HEIGHT), TRANSPARENT)
    draw = ImageDraw.Draw(bars)
    draw.rectangle((8, 122, 167, 124), outline=(120, 255, 100, 220))
    draw.rectangle((179, 174, 232, 176), outline=(100, 240, 255, 220))
    return {
        "GUI与素材区有效范围": bounds,
        "按钮点击范围": hitboxes,
        "3D模型可视范围": model,
        "经验与燃料裁切范围": bars,
    }


def put_sprite(layer: Image.Image, sprite: Image.Image, x: int, y: int) -> None:
    layer.alpha_composite(sprite, (x, y))


def build_layers(
    source_layers: dict[str, Image.Image],
    original_png: Image.Image,
    relic: Image.Image,
    font: MinecraftFont,
) -> tuple[list[OutputLayer], dict[str, Image.Image], dict[str, object]]:
    base_size = original_png.size
    static_original = compose(
        base_size,
        [
            source_layers["打底"],
            source_layers["右侧拓展栏"],
            source_layers["召唤GUI组件"],
        ],
    )

    # Split model viewport out of the static layer.
    model_box = (7, 14, 59, 86)
    model_region = static_original.crop(model_box)
    model_background_crop = Image.new("RGBA", model_region.size, TRANSPARENT)
    model_frame_crop = Image.new("RGBA", model_region.size, TRANSPARENT)
    region_pixels = model_region.load()
    bg_pixels = model_background_crop.load()
    frame_pixels = model_frame_crop.load()
    for y in range(model_region.height):
        for x in range(model_region.width):
            pixel = region_pixels[x, y]
            if pixel[:3] == (0, 0, 0) and pixel[3]:
                bg_pixels[x, y] = pixel
            elif pixel != LIGHT and pixel[3]:
                frame_pixels[x, y] = pixel

    static_crop = static_original.crop((0, 0, GUI_WIDTH, GUI_HEIGHT))
    static_draw = ImageDraw.Draw(static_crop)
    static_draw.rectangle((7, 14, 58, 85), fill=LIGHT)
    # Remove old slot-like frames from regions now rendered as dynamic controls.
    static_draw.rectangle((61, 68, 154, 85), fill=LIGHT)
    static_draw.rectangle((178, 67, 233, 85), fill=LIGHT)
    static_draw.rectangle((178, 100, 233, 117), fill=LIGHT)
    static_draw.rectangle((178, 133, 233, 150), fill=LIGHT)

    static_layer = image_at((MASTER_WIDTH, MASTER_HEIGHT), static_crop, (0, 0))
    model_background = image_at(
        (MASTER_WIDTH, MASTER_HEIGHT), model_background_crop, (model_box[0], model_box[1])
    )
    model_frame = image_at(
        (MASTER_WIDTH, MASTER_HEIGHT), model_frame_crop, (model_box[0], model_box[1])
    )

    # Existing small button states.
    mode_source = source_layers["默认触摸按下按钮底板"]
    mode_sources = {
        "default": crop_sprite(mode_source, (243, 130, 261, 148)),
        "hover": crop_sprite(mode_source, (265, 130, 283, 148)),
        "selected": crop_sprite(mode_source, (287, 130, 305, 148)),
    }
    mode_states = {
        name: make_mode_button(name, mode_sources)
        for name in ("default", "hover", "selected", "disabled")
    }

    long_source = source_layers["召唤，召回按钮"]
    long_states = {
        "default": crop_sprite(long_source, (178, 151, 234, 169)),
        "hover": crop_sprite(long_source, (242, 151, 298, 169)),
        "disabled": crop_sprite(long_source, (242, 173, 298, 191)),
    }
    long_states["confirm"] = make_confirm_button(long_states["hover"])

    skill_states = {
        state: make_skill_frame(state)
        for state in ("default", "hover", "user_disabled", "unavailable")
    }
    talent_states = {
        state: make_talent_frame(state) for state in ("default", "hover")
    }

    exp_bg = three_pixel_bar(crop_sprite(source_layers["经验条背景"], (8, 122, 168, 124)))
    exp_fill = three_pixel_bar(
        crop_sprite(source_layers["经验条填充"], (174, 252, 334, 254))
    )
    fuel_fill = crop_sprite(source_layers["燃料槽填充图"], (279, 246, 333, 249))
    fuel_bg = static_original.crop((178, 173, 234, 178))

    exp_bg_layer = Image.new("RGBA", (MASTER_WIDTH, MASTER_HEIGHT), TRANSPARENT)
    put_sprite(exp_bg_layer, exp_bg, 8, 122)
    exp_fill_layer = Image.new("RGBA", (MASTER_WIDTH, MASTER_HEIGHT), TRANSPARENT)
    put_sprite(exp_fill_layer, exp_fill.crop((0, 0, 96, 3)), 8, 122)
    put_sprite(exp_fill_layer, exp_fill, SHEET_X + 8, 131)
    fuel_fill_layer = Image.new("RGBA", (MASTER_WIDTH, MASTER_HEIGHT), TRANSPARENT)
    put_sprite(fuel_fill_layer, fuel_fill.crop((0, 0, 38, 3)), 179, 174)
    put_sprite(fuel_fill_layer, fuel_fill, SHEET_X + 9, 143)

    # Dynamic component layers serve both the left preview and the right source sheet.
    mode_layers: dict[str, Image.Image] = {}
    state_sheet_x = {"default": 8, "hover": 30, "selected": 52, "disabled": 74}
    for state, sprite in mode_states.items():
        layer = Image.new("RGBA", (MASTER_WIDTH, MASTER_HEIGHT), TRANSPARENT)
        put_sprite(layer, sprite, SHEET_X + state_sheet_x[state], 8)
        mode_layers[state] = layer
    # Preview: follow and passive defense selected.
    for index, x in enumerate((178, 197, 216)):
        put_sprite(mode_layers["selected" if index == 0 else "default"], mode_states["selected" if index == 0 else "default"], x, 100)
        put_sprite(mode_layers["selected" if index == 1 else "default"], mode_states["selected" if index == 1 else "default"], x, 133)

    skill_layers: dict[str, Image.Image] = {}
    skill_sheet_x = {
        "default": 8,
        "hover": 30,
        "user_disabled": 52,
        "unavailable": 74,
    }
    for state, sprite in skill_states.items():
        layer = Image.new("RGBA", (MASTER_WIDTH, MASTER_HEIGHT), TRANSPARENT)
        put_sprite(layer, sprite, SHEET_X + skill_sheet_x[state], 32)
        skill_layers[state] = layer
    for i in range(5):
        put_sprite(skill_layers["default"], skill_states["default"], 61 + i * 19, 68)

    talent_layers: dict[str, Image.Image] = {}
    for state, sprite in talent_states.items():
        layer = Image.new("RGBA", (MASTER_WIDTH, MASTER_HEIGHT), TRANSPARENT)
        put_sprite(layer, sprite, SHEET_X + (8 if state == "default" else 30), 56)
        talent_layers[state] = layer
    for i in range(2):
        put_sprite(talent_layers["default"], talent_states["default"], 178 + i * 19, 67)

    summon_layers: dict[str, Image.Image] = {}
    long_positions = {
        "default": (8, 80),
        "hover": (68, 80),
        "disabled": (8, 102),
        "confirm": (68, 102),
    }
    for state, sprite in long_states.items():
        layer = Image.new("RGBA", (MASTER_WIDTH, MASTER_HEIGHT), TRANSPARENT)
        sx, sy = long_positions[state]
        put_sprite(layer, sprite, SHEET_X + sx, sy)
        summon_layers[state] = layer
    put_sprite(summon_layers["default"], long_states["default"], 178, 151)

    bars_component_layer = Image.new("RGBA", (MASTER_WIDTH, MASTER_HEIGHT), TRANSPARENT)
    put_sprite(bars_component_layer, exp_bg, SHEET_X + 8, 126)
    put_sprite(bars_component_layer, fuel_bg, SHEET_X + 8, 137)

    # Independent icons.
    attribute_source = source_layers["基础属性图标"]
    attribute_icons = {
        "health": icon_canvas(attribute_source.crop((61, 29, 80, 41))),
        "attack_speed": make_attack_speed_icon(),
        "perception_range": icon_canvas(attribute_source.crop((133, 29, 152, 41))),
        "attack_damage": icon_canvas(attribute_source.crop((61, 41, 80, 53))),
        "armor": icon_canvas(attribute_source.crop((97, 41, 116, 53))),
        "movement_speed": icon_canvas(attribute_source.crop((133, 41, 152, 53))),
    }
    attr_layer = Image.new("RGBA", (MASTER_WIDTH, MASTER_HEIGHT), TRANSPARENT)
    attr_preview_positions = [(59, 27), (96, 27), (133, 27), (59, 40), (96, 40), (133, 40)]
    for index, (name, sprite) in enumerate(attribute_icons.items()):
        put_sprite(attr_layer, sprite, *attr_preview_positions[index])
        put_sprite(attr_layer, sprite, SHEET_X + 8 + index * 18, 152)

    skill_source = source_layers["技能图标"]
    skill_icons = {
        "soldier_formation": crop_sprite(skill_source, (62, 69, 78, 85)),
        "shield_charge": crop_sprite(skill_source, (81, 69, 97, 85)),
        "legion_endures": crop_sprite(skill_source, (100, 69, 116, 85)),
    }
    skill_icon_layer = Image.new("RGBA", (MASTER_WIDTH, MASTER_HEIGHT), TRANSPARENT)
    for index, sprite in enumerate(skill_icons.values()):
        put_sprite(skill_icon_layer, sprite, 62 + index * 19, 69)
        put_sprite(skill_icon_layer, sprite, SHEET_X + 8 + index * 18, 172)

    talent_source = source_layers["天赋图标"]
    talent_icons = {
        "bad_temper": crop_sprite(talent_source, (179, 68, 195, 84)),
        "sturdy": crop_sprite(talent_source, (198, 68, 214, 84)),
        "ordinary_disabled_reference": crop_sprite(talent_source, (217, 68, 233, 84)),
    }
    talent_icon_layer = Image.new("RGBA", (MASTER_WIDTH, MASTER_HEIGHT), TRANSPARENT)
    put_sprite(talent_icon_layer, talent_icons["bad_temper"], 179, 68)
    put_sprite(talent_icon_layer, talent_icons["sturdy"], 198, 68)
    for index, sprite in enumerate(talent_icons.values()):
        put_sprite(talent_icon_layer, sprite, SHEET_X + 8 + index * 18, 192)

    mode_source_icons = source_layers["行动模式图标"]
    activity_icons = {
        "follow": crop_sprite(mode_source_icons, (180, 102, 196, 118)),
        "wait": crop_sprite(mode_source_icons, (198, 102, 214, 118)),
        "wander": crop_sprite(mode_source_icons, (216, 102, 232, 118)),
    }
    alert_icons = {
        "aggressive": crop_sprite(mode_source_icons, (180, 133, 196, 149)),
        "defensive": crop_sprite(mode_source_icons, (198, 133, 214, 149)),
        "peaceful": crop_sprite(mode_source_icons, (216, 133, 232, 149)),
    }
    activity_layer = Image.new("RGBA", (MASTER_WIDTH, MASTER_HEIGHT), TRANSPARENT)
    alert_layer = Image.new("RGBA", (MASTER_WIDTH, MASTER_HEIGHT), TRANSPARENT)
    for index, sprite in enumerate(activity_icons.values()):
        put_sprite(activity_layer, sprite, 180 + index * 18, 102)
        put_sprite(activity_layer, sprite, SHEET_X + 8 + index * 18, 212)
    for index, sprite in enumerate(alert_icons.values()):
        put_sprite(alert_layer, sprite, 180 + index * 18, 135)
        put_sprite(alert_layer, sprite, SHEET_X + 8 + index * 18, 232)

    relic = relic.convert("RGBA")
    relic_layer = Image.new("RGBA", (MASTER_WIDTH, MASTER_HEIGHT), TRANSPARENT)
    put_sprite(relic_layer, relic, 217, 184)
    put_sprite(relic_layer, relic, SHEET_X + 134, 152)

    empty_hint_fuel = Image.new("RGBA", (16, 16), TRANSPARENT)
    empty_hint_relic = Image.new("RGBA", (16, 16), TRANSPARENT)

    chinese_text, english_text = make_text_references(font)
    guides = make_guides(font)

    layers: list[OutputLayer] = []

    def group(name: str, level: int, visible: bool = True, collapsed: bool = True) -> None:
        layers.append(OutputLayer(name=name, level=level, kind=1, visible=visible, collapsed=collapsed))

    def normal(name: str, level: int, image: Image.Image, visible: bool = True) -> None:
        layers.append(OutputLayer(name=name, level=level, image=image, visible=visible))

    group("00_原稿备份", 0, visible=False, collapsed=True)
    normal(
        "英灵召唤器3_原始合成",
        1,
        image_at((MASTER_WIDTH, MASTER_HEIGHT), original_png, (0, 0)),
    )

    group("01_GUI完整预览_240x210", 0, visible=True, collapsed=False)
    group("静态背景", 1, visible=True, collapsed=True)
    normal("主界面静态背景", 2, static_layer)
    normal("模型窗口_背景", 2, model_background)
    normal("模型窗口_边框", 2, model_frame)
    normal("经验条_空槽背景", 2, exp_bg_layer)

    group("02_动态组件素材区", 0, visible=True, collapsed=True)
    group("模式按钮_四种状态", 1, visible=True, collapsed=True)
    normal("模式按钮_默认", 2, mode_layers["default"])
    normal("模式按钮_悬浮", 2, mode_layers["hover"])
    normal("模式按钮_选中", 2, mode_layers["selected"])
    normal("模式按钮_禁用", 2, mode_layers["disabled"])
    group("技能按钮与遮罩", 1, visible=True, collapsed=True)
    normal("技能底板_启用", 2, skill_layers["default"])
    normal("技能底板_悬浮", 2, skill_layers["hover"])
    normal("技能底板_玩家禁用", 2, skill_layers["user_disabled"])
    normal("技能底板_不可操作", 2, skill_layers["unavailable"])
    group("天赋展示框", 1, visible=True, collapsed=True)
    normal("天赋底板_默认", 2, talent_layers["default"])
    normal("天赋底板_悬浮", 2, talent_layers["hover"])
    group("召唤按钮_四种状态", 1, visible=True, collapsed=True)
    normal("召唤按钮_默认", 2, summon_layers["default"])
    normal("召唤按钮_悬浮", 2, summon_layers["hover"])
    normal("召唤按钮_禁用", 2, summon_layers["disabled"])
    normal("召唤按钮_确认收回", 2, summon_layers["confirm"])
    group("经验与燃料", 1, visible=True, collapsed=True)
    normal("经验与燃料_空槽素材", 2, bars_component_layer)
    normal("经验条_填充预览与素材", 2, exp_fill_layer)
    normal("燃料条_填充预览与素材", 2, fuel_fill_layer)

    group("03_独立图标", 0, visible=True, collapsed=True)
    group("六项基础属性", 1, visible=True, collapsed=True)
    normal("基础属性图标_六个", 2, attr_layer)
    group("英灵技能", 1, visible=True, collapsed=True)
    normal("罗马军团兵_三个技能图标", 2, skill_icon_layer)
    group("遗物天赋", 1, visible=True, collapsed=True)
    normal("天赋图标_坏脾气与壮硕_平凡暂不启用", 2, talent_icon_layer)
    group("行动与警戒模式", 1, visible=True, collapsed=True)
    normal("行动模式图标_跟随等待闲逛", 2, activity_layer)
    normal("警戒状态图标_主动被动和平", 2, alert_layer)
    group("遗物与空槽提示", 1, visible=True, collapsed=True)
    normal("罗马军团兵遗物_参考", 2, relic_layer)
    normal("燃料槽_空槽提示图标_待绘制", 2, image_at((MASTER_WIDTH, MASTER_HEIGHT), empty_hint_fuel, (SHEET_X + 156, 152)))
    normal("遗物槽_空槽提示图标_待绘制", 2, image_at((MASTER_WIDTH, MASTER_HEIGHT), empty_hint_relic, (SHEET_X + 176, 152)))

    group("90_设计参考_默认折叠", 0, visible=True, collapsed=True)
    normal("中文原版字体参考_默认显示", 1, chinese_text, visible=True)
    normal("英文原版字体参考_默认隐藏", 1, english_text, visible=False)
    for name, image in guides.items():
        normal(name + "_默认隐藏", 1, image, visible=False)

    group("99_说明_默认隐藏", 0, visible=False, collapsed=True)
    note = Image.new("RGBA", (MASTER_WIDTH, MASTER_HEIGHT), TRANSPARENT)
    font.draw(note, (SHEET_X + 8, 8), "详细导出说明见同目录README", WHITE)
    normal("详细说明见README", 1, note)

    assets = {
        "static_screen": compose(
            (MASTER_WIDTH, MASTER_HEIGHT),
            [static_layer, model_background, model_frame, exp_bg_layer],
        ).crop((0, 0, GUI_WIDTH, GUI_HEIGHT)),
        "attribute_icons": attribute_icons,
        "skill_icons": skill_icons,
        "talent_icons": talent_icons,
        "activity_icons": activity_icons,
        "alert_icons": alert_icons,
        "mode_states": mode_states,
        "skill_states": skill_states,
        "talent_states": talent_states,
        "long_states": long_states,
        "experience_background": exp_bg,
        "experience_fill": exp_fill,
        "fuel_background": fuel_bg,
        "fuel_fill": fuel_fill,
        "fuel_hint": empty_hint_fuel,
        "relic_hint": empty_hint_relic,
        "relic": relic,
    }

    manifest = {
        "master_canvas": {"width": MASTER_WIDTH, "height": MASTER_HEIGHT},
        "gui_preview": {"x": 0, "y": 0, "width": GUI_WIDTH, "height": GUI_HEIGHT},
        "component_sheet": {"x": SHEET_X, "y": 0, "width": 256, "height": 256},
        "widgets": {
            "mode_buttons": {
                state: {"x": x, "y": 8, "width": 18, "height": 18}
                for state, x in state_sheet_x.items()
            },
            "skill_buttons": {
                "enabled": {"x": 8, "y": 32, "width": 18, "height": 18},
                "hover": {"x": 30, "y": 32, "width": 18, "height": 18},
                "user_disabled": {"x": 52, "y": 32, "width": 18, "height": 18},
                "unavailable": {"x": 74, "y": 32, "width": 18, "height": 18},
            },
            "talent_frames": {
                "default": {"x": 8, "y": 56, "width": 18, "height": 18},
                "hover": {"x": 30, "y": 56, "width": 18, "height": 18},
            },
            "summon_buttons": {
                state: {"x": xy[0], "y": xy[1], "width": 56, "height": 18}
                for state, xy in long_positions.items()
            },
            "experience_background": {"x": 8, "y": 126, "width": 160, "height": 3},
            "experience_fill": {"x": 8, "y": 131, "width": 160, "height": 3},
            "fuel_background": {"x": 8, "y": 137, "width": 56, "height": 5},
            "fuel_fill": {"x": 9, "y": 143, "width": 54, "height": 3},
        },
    }
    return layers, assets, manifest


def layer_chunk(layer: OutputLayer, canvas_size: tuple[int, int]) -> bytes:
    name = layer.name.encode("utf-8")
    payload = struct.pack(
        "<HHHHHHB3xH",
        layer.flags,
        layer.kind,
        layer.level,
        canvas_size[0],
        canvas_size[1],
        0,
        255,
        len(name),
    ) + name
    size = 6 + len(payload)
    return struct.pack("<IH", size, 0x2004) + payload


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
    size = 6 + len(payload)
    return struct.pack("<IH", size, 0x2005) + payload


def write_ase(path: Path, layers: list[OutputLayer]) -> None:
    chunks: list[bytes] = []
    for layer in layers:
        chunks.append(layer_chunk(layer, (MASTER_WIDTH, MASTER_HEIGHT)))
    for index, layer in enumerate(layers):
        if layer.kind == 0 and layer.image is not None:
            chunk = cel_chunk(index, layer.image)
            if chunk is not None:
                chunks.append(chunk)

    frame_payload = b"".join(chunks)
    frame_size = 16 + len(frame_payload)
    frame_header = struct.pack(
        "<IHHH2xI",
        frame_size,
        0xF1FA,
        len(chunks),
        100,
        len(chunks),
    )
    frame = frame_header + frame_payload

    header = bytearray(128)
    struct.pack_into("<H", header, 4, 0xA5E0)
    struct.pack_into("<H", header, 6, 1)
    struct.pack_into("<H", header, 8, MASTER_WIDTH)
    struct.pack_into("<H", header, 10, MASTER_HEIGHT)
    struct.pack_into("<H", header, 12, 32)
    struct.pack_into("<I", header, 14, 1)
    struct.pack_into("<H", header, 18, 100)
    header[28] = 0
    struct.pack_into("<H", header, 32, 0)
    header[34] = 1
    header[35] = 1
    struct.pack_into("<h", header, 36, 0)
    struct.pack_into("<h", header, 38, 0)
    struct.pack_into("<H", header, 40, 16)
    struct.pack_into("<H", header, 42, 16)
    file_size = 128 + len(frame)
    struct.pack_into("<I", header, 0, file_size)
    path.write_bytes(bytes(header) + frame)


def render_visible_layers(layers: list[OutputLayer]) -> Image.Image:
    result = Image.new("RGBA", (MASTER_WIDTH, MASTER_HEIGHT), TRANSPARENT)
    group_visibility: dict[int, bool] = {}
    for layer in layers:
        for level in list(group_visibility):
            if level >= layer.level:
                del group_visibility[level]
        parent_visible = all(group_visibility.values())
        effective = parent_visible and layer.visible
        if layer.kind == 1:
            group_visibility[layer.level] = effective
        elif effective and layer.image is not None:
            result.alpha_composite(layer.image)
    return result


def save_assets(output: Path, assets: dict[str, Image.Image | dict[str, Image.Image]], manifest: dict[str, object]) -> None:
    game = output / "游戏资源参考"
    (game / "attributes").mkdir(parents=True, exist_ok=True)
    (game / "skills" / "roman_legionary").mkdir(parents=True, exist_ok=True)
    (game / "traits").mkdir(parents=True, exist_ok=True)
    (game / "modes" / "activity").mkdir(parents=True, exist_ok=True)
    (game / "modes" / "alert").mkdir(parents=True, exist_ok=True)
    (game / "slot_hints").mkdir(parents=True, exist_ok=True)
    (game / "items").mkdir(parents=True, exist_ok=True)

    assert isinstance(assets["static_screen"], Image.Image)
    assets["static_screen"].save(game / "summoner_screen.png")

    widgets = Image.new("RGBA", (256, 256), TRANSPARENT)
    mode_states = assets["mode_states"]
    assert isinstance(mode_states, dict)
    for state, x in {"default": 8, "hover": 30, "selected": 52, "disabled": 74}.items():
        widgets.alpha_composite(mode_states[state], (x, 8))
    skill_states = assets["skill_states"]
    assert isinstance(skill_states, dict)
    for state, x in {"default": 8, "hover": 30, "user_disabled": 52, "unavailable": 74}.items():
        widgets.alpha_composite(skill_states[state], (x, 32))
    talent_states = assets["talent_states"]
    assert isinstance(talent_states, dict)
    widgets.alpha_composite(talent_states["default"], (8, 56))
    widgets.alpha_composite(talent_states["hover"], (30, 56))
    long_states = assets["long_states"]
    assert isinstance(long_states, dict)
    for state, xy in {
        "default": (8, 80),
        "hover": (68, 80),
        "disabled": (8, 102),
        "confirm": (68, 102),
    }.items():
        widgets.alpha_composite(long_states[state], xy)
    for key, xy in [
        ("experience_background", (8, 126)),
        ("experience_fill", (8, 131)),
        ("fuel_background", (8, 137)),
        ("fuel_fill", (9, 143)),
    ]:
        sprite = assets[key]
        assert isinstance(sprite, Image.Image)
        widgets.alpha_composite(sprite, xy)
    widgets.save(game / "summoner_widgets.png")
    (game / "summoner_widgets_manifest.json").write_text(
        json.dumps(manifest, ensure_ascii=False, indent=2), encoding="utf-8"
    )

    mappings = [
        ("attribute_icons", game / "attributes"),
        ("skill_icons", game / "skills" / "roman_legionary"),
        ("talent_icons", game / "traits"),
        ("activity_icons", game / "modes" / "activity"),
        ("alert_icons", game / "modes" / "alert"),
    ]
    for key, directory in mappings:
        sprites = assets[key]
        assert isinstance(sprites, dict)
        for name, sprite in sprites.items():
            sprite.save(directory / f"{name}.png")
    assert isinstance(assets["fuel_hint"], Image.Image)
    assert isinstance(assets["relic_hint"], Image.Image)
    assert isinstance(assets["relic"], Image.Image)
    assets["fuel_hint"].save(game / "slot_hints" / "fuel_slot_hint.png")
    assets["relic_hint"].save(game / "slot_hints" / "relic_slot_hint.png")
    assets["relic"].save(game / "items" / "roman_legionary_relic.png")


def main() -> None:
    parser = argparse.ArgumentParser(description="Build the editable summoner GUI Aseprite master")
    parser.add_argument("--source-ase", type=Path, default=DEFAULT_SOURCE_ASE)
    parser.add_argument("--source-png", type=Path, default=DEFAULT_SOURCE_PNG)
    parser.add_argument("--relic-png", type=Path, default=DEFAULT_RELIC_PNG)
    parser.add_argument("--output", type=Path, default=DEFAULT_OUTPUT)
    args = parser.parse_args()

    args.output.mkdir(parents=True, exist_ok=True)
    width, height, parsed_layers = read_ase(args.source_ase)
    if (width, height) != (336, 256):
        raise ValueError(f"Unexpected source canvas: {(width, height)}")
    source_layers = {layer.name: layer.image for layer in parsed_layers}
    required = {
        "打底",
        "右侧拓展栏",
        "召唤GUI组件",
        "默认触摸按下按钮底板",
        "基础属性图标",
        "行动模式图标",
        "召唤，召回按钮",
        "技能图标",
        "天赋图标",
        "经验条背景",
        "经验条填充",
        "燃料槽填充图",
    }
    missing = required.difference(source_layers)
    if missing:
        raise ValueError(f"Missing source layers: {sorted(missing)}")

    original_png = Image.open(args.source_png).convert("RGBA")
    relic = Image.open(args.relic_png).convert("RGBA")
    client_jar, unifont_zip = find_font_assets()
    font = MinecraftFont(client_jar, unifont_zip)
    layers, assets, manifest = build_layers(source_layers, original_png, relic, font)

    master_path = args.output / "英灵召唤器_GUI工作母版.ase"
    preview_path = args.output / "英灵召唤器_GUI工作母版.png"
    gui_zoom_path = args.output / "GUI实际区域_4倍预览.png"
    attributes_zoom_path = args.output / "基础属性区域_8倍预览.png"
    write_ase(master_path, layers)
    preview = render_visible_layers(layers)
    preview.save(preview_path)
    preview.crop((0, 0, GUI_WIDTH, GUI_HEIGHT)).resize(
        (GUI_WIDTH * 4, GUI_HEIGHT * 4), Image.Resampling.NEAREST
    ).save(gui_zoom_path)
    preview.crop((58, 26, 171, 54)).resize(
        (113 * 8, 28 * 8), Image.Resampling.NEAREST
    ).save(attributes_zoom_path)
    save_assets(args.output, assets, manifest)

    # Validate the generated Aseprite file can be parsed and has the intended canvas.
    check_width, check_height, check_layers = read_ase(master_path)
    if (check_width, check_height) != (MASTER_WIDTH, MASTER_HEIGHT):
        raise RuntimeError("Generated master canvas failed validation")
    if len(check_layers) != len(layers):
        raise RuntimeError(
            f"Generated layer count mismatch: {len(check_layers)} != {len(layers)}"
        )

    summary = {
        "source": str(args.source_ase),
        "master": str(master_path),
        "preview": str(preview_path),
        "gui_zoom_preview": str(gui_zoom_path),
        "attributes_zoom_preview": str(attributes_zoom_path),
        "layer_count": len(layers),
        "canvas": [MASTER_WIDTH, MASTER_HEIGHT],
        "game_reference_directory": str(args.output / "游戏资源参考"),
    }
    (args.output / "生成信息.json").write_text(
        json.dumps(summary, ensure_ascii=False, indent=2), encoding="utf-8"
    )
    print(json.dumps(summary, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
