#!/usr/bin/env python3
"""Audit Echo Warrior localization completeness and release readiness."""

from __future__ import annotations

import argparse
import hashlib
import json
import re
import sys
from collections import Counter
from datetime import date
from pathlib import Path
from typing import Any


PROJECT_ROOT = Path(__file__).resolve().parents[1]
DEFAULT_CONFIG = PROJECT_ROOT / "docs/localization/config.json"
PLACEHOLDER_PATTERN = re.compile(r"%(?:\d+\$)?[a-zA-Z%]")
CONTROL_CHARACTER_PATTERN = re.compile(r"[\x00-\x08\x0b\x0c\x0e-\x1f\x7f]")
TRANSLATION_KEY_VALUE_PATTERN = re.compile(r"^(?:[a-z0-9_]+\.){2,}[a-z0-9_.-]+$")
REPORT_LIMIT = 12

LOCALE_FORBIDDEN_FRAGMENTS: dict[str, dict[str, str]] = {
    "en_us": {
        "raccoon servants": "linu 是古人对猫的爱称，不保留错误的字面动物释义",
    },
    "zh_tw": {
        "自定義": "台灣介面用語應使用「自訂」",
        "影象": "一般圖像語境應使用「圖像」",
        "型別": "一般分類語境應使用「類型」",
        "揹包": "Minecraft 物品語境應使用「背包」",
        "推匯出": "這是「推導出」的錯誤自動轉換",
        "石制": "材質語境應使用「石製」",
        "關上了遠途回聲檢測": "功能開關應使用「關閉」",
        "注：": "台灣書面提示应使用「註：」",
        "賬簿": "台灣書面用語應使用「帳簿」",
        "側發": "髮型語境應使用「側髮」",
        "剋制": "自我節制語境應使用「克制」",
    },
    "ja_jp": {
        "英霊": "本作のエコーは本人の魂ではなく歴史上の戦士の投影なので「エコー」を使用する",
        "霊魂": "本作のエコーを魂や亡霊として表現しない",
        "亡霊": "本作のエコーを魂や亡霊として表現しない",
    },
    "ru_ru": {
        "Призрак": "эхо-воины являются историческими проекциями, а не призраками",
        "призрак": "эхо-воины являются историческими проекциями, а не призраками",
        "Дух отголоска": "для основного понятия следует использовать «Отголосок»",
        "дух отголоска": "для основного понятия следует использовать «отголосок»",
        "слуга-енот": "лину — старинное ласковое название кошки, а не буквальный енот",
    },
    "pt_br": {
        "Fantasma": "os Guerreiros do Eco são projeções históricas, não fantasmas",
        "fantasma": "os Guerreiros do Eco são projeções históricas, não fantasmas",
        "Alma de Eco": "o conceito principal deve usar «Eco», sem tratá-lo como uma alma",
        "alma de Eco": "o conceito principal deve usar «Eco», sem tratá-lo como uma alma",
        "Espírito do Eco": "o conceito principal deve usar «Eco», sem tratá-lo como um espírito",
        "espírito do Eco": "o conceito principal deve usar «Eco», sem tratá-lo como um espírito",
        "servos-guaxinins": "linu é um nome carinhoso antigo para gatos, não uma tradução animal literal",
    },
    "es_es": {
        "Fantasma": "los Guerreros del Eco son proyecciones históricas, no fantasmas",
        "fantasma": "los Guerreros del Eco son proyecciones históricas, no fantasmas",
        "Alma de Eco": "el concepto principal debe usar «Eco», sin tratarlo como un alma",
        "alma de Eco": "el concepto principal debe usar «Eco», sin tratarlo como un alma",
        "Espíritu del Eco": "el concepto principal debe usar «Eco», sin tratarlo como un espíritu",
        "espíritu del Eco": "el concepto principal debe usar «Eco», sin tratarlo como un espíritu",
        "sirvientes mapache": "linu es un apelativo antiguo y cariñoso para los gatos, no una traducción animal literal",
    },
    "es_mx": {
        "Fantasma": "los Guerreros del Eco son proyecciones históricas, no fantasmas",
        "fantasma": "los Guerreros del Eco son proyecciones históricas, no fantasmas",
        "Alma de Eco": "el concepto principal debe usar «Eco», sin tratarlo como un alma",
        "alma de Eco": "el concepto principal debe usar «Eco», sin tratarlo como un alma",
        "Espíritu del Eco": "el concepto principal debe usar «Eco», sin tratarlo como un espíritu",
        "espíritu del Eco": "el concepto principal debe usar «Eco», sin tratarlo como un espíritu",
        "sirvientes mapache": "linu es un apelativo antiguo y cariñoso para los gatos, no una traducción animal literal",
    }
}

LOCALE_REQUIRED_VALUES: dict[str, dict[str, str]] = {
    "en_us": {
        "trait.echo_warrior.raider_slayer.name": "Raider Slayer",
        "trait.echo_warrior.raider_slayer.description": "Deals 20% more damage to raider mobs",
        "gui.echo_warrior.summoner.skill.samurai.zanshin.description.2": "Gain 20% dodge for 1 second after landing an attack, up to 80% total.",
        "gui.echo_warrior.summoner.skill.samurai.zanshin.description.3": "A dodge negates the hit and grants 1 Fumikomi charge; ongoing and environmental damage cannot be dodged.",
        "gui.echo_warrior.tutorial.page.discoveries.body.1": "Each Battlefield Ruins site contains one Echo Relic.",
        "tag.item.echo_warrior.accessories.culture.aztec": "Mexica Accessories",
        "item.echo_warrior.accessory.memory_ritual_knife.effect.2": "Kills have a 0.5% chance to yield 1 random Legacy",
    },
    "zh_cn": {
        "trait.echo_warrior.raider_slayer.name": "袭击者杀手",
        "trait.echo_warrior.raider_slayer.description": "对袭击类生物造成的伤害提高20%",
        "gui.echo_warrior.tutorial.page.discoveries.body.1": "每处战场遗迹都保证有一个英灵遗物。",
    },
    "zh_tw": {
        "itemGroup.echo_warrior.echo_warrior": "英靈回聲",
        "item.echo_warrior.test_echo_summoner": "回聲召喚器",
        "item.echo_warrior.echo_compass": "回聲羅盤",
        "item.echo_warrior.echo_compass.tooltip.battlefield": "戰場遺蹟",
        "item.echo_warrior.legacy.fortitude.term": "回復 II",
        "block.echo_warrior.echo_recycler": "英靈雜物回收箱",
        "item.echo_warrior.tutorial_manual": "《回聲、戰士與你》",
        "trait.echo_warrior.raider_slayer.name": "襲擊者殺手",
        "trait.echo_warrior.raider_slayer.description": "對襲擊類生物造成的傷害提高20%",
        "gui.echo_warrior.tutorial.page.discoveries.body.1": "每處戰場遺蹟都保證有一個英靈遺物。",
    },
    "ja_jp": {
        "itemGroup.echo_warrior.echo_warrior": "エコー・ウォリアー",
        "item.echo_warrior.test_echo_summoner": "エコー召喚器",
        "item.echo_warrior.echo_compass": "エコーコンパス",
        "item.echo_warrior.echo_compass.tooltip.battlefield": "古戦場跡",
        "gui.echo_warrior.summoner.button.dismiss": "送還",
        "block.echo_warrior.echo_recycler": "エコー回収チェスト",
        "item.echo_warrior.tutorial_manual": "『エコー、戦士、そしてあなた』",
        "gui.echo_warrior.summoner.skill.samurai.zanshin.description.2": "攻撃が命中すると1秒間、回避率が20%増加。合計最大80%。",
        "gui.echo_warrior.summoner.skill.samurai.zanshin.description.3": "回避すると攻撃を無効化し、踏み込みを1回分得る。継続ダメージと環境ダメージは回避不可。",
        "gui.echo_warrior.tutorial.page.discoveries.body.1": "どの古戦場跡にも、エコーの遺物が一つあります。",
    },
    "ru_ru": {
        "itemGroup.echo_warrior.echo_warrior": "Воин отголоска",
        "item.echo_warrior.test_echo_summoner": "Устройство призыва отголосков",
        "item.echo_warrior.echo_compass": "Компас отголосков",
        "gui.echo_warrior.tutorial.page.battlefield.title": "Руины поля боя",
        "gui.echo_warrior.summoner.button.dismiss": "Отозвать",
        "block.echo_warrior.echo_recycler": "Сундук переработки эхо-предметов",
        "item.echo_warrior.tutorial_manual": "«Отголоски, воины и вы»",
        "gui.echo_warrior.summoner.skill.samurai.zanshin.description.2": "После попадания атакой шанс уклонения повышается на 20% на 1 секунду, максимум — до 80%.",
        "gui.echo_warrior.summoner.skill.samurai.zanshin.description.3": "Уклонение полностью отменяет удар и даёт 1 заряд Фумикоми; от периодического урона и урона окружающей среды уклониться нельзя.",
        "death.attack.armor_piercing_arrow.player": "%1$s был убит конической стрелой (источник: %2$s)",
        "death.attack.spiked_armor_reflection.player": "%1$s погиб от отражённого урона талисмана обсидиановых шипов (источник: %2$s)",
        "item.echo_warrior.test_echo_summoner.tooltip.detail.healing": "При естественном восстановлении здоровья %s расходует %s",
        "gui.echo_warrior.summoner.attribute.alert_range": "Радиус боя (в блоках): %s",
        "item.echo_warrior.knowledge_fragment.detail.craft": "+ Объедините эти и другие %s, чтобы создать %s!",
        "item.echo_warrior.knowledge_fragment_collection.summary": "Записей: %s · Фрагментов: %s",
        "message.echo_warrior.echo_count_performance_warning": "Сейчас под вашим управлением живых воинов-отголосков: %s. Большие группы могут повлиять на производительность сервера.",
    },
    "pt_br": {
        "itemGroup.echo_warrior.echo_warrior": "Guerreiro do Eco",
        "item.echo_warrior.test_echo_summoner": "Invocador de Ecos",
        "item.echo_warrior.echo_compass": "Bússola dos Ecos",
        "gui.echo_warrior.tutorial.page.battlefield.title": "Ruínas do Campo de Batalha",
        "gui.echo_warrior.summoner.button.dismiss": "Dispensar",
        "block.echo_warrior.echo_recycler": "Baú de Desmonte dos Ecos",
        "item.echo_warrior.tutorial_manual": "Ecos, Guerreiros e Você",
        "trait.echo_warrior.raider_slayer.name": "Matador de Invasores",
        "trait.echo_warrior.raider_slayer.description": "Causa 20% a mais de dano a criaturas de invasão",
        "gui.echo_warrior.summoner.skill.samurai.zanshin.description.2": "Após acertar um ataque, ganha 20% de esquiva por 1 segundo, até 80% no total.",
        "gui.echo_warrior.summoner.skill.samurai.zanshin.description.3": "Uma esquiva anula o golpe e concede 1 carga de Fumikomi; não é possível esquivar de dano contínuo nem de dano ambiental.",
        "item.echo_warrior.accessory.detail.unique": "Não é possível equipar mais de um %s com o mesmo nome",
        "tag.item.echo_warrior.accessories.culture.aztec": "Acessórios Mexicas",
    },
    "es_es": {
        "itemGroup.echo_warrior.echo_warrior": "Guerrero del Eco",
        "item.echo_warrior.test_echo_summoner": "Invocador de Ecos",
        "item.echo_warrior.echo_compass": "Brújula de los Ecos",
        "gui.echo_warrior.tutorial.page.battlefield.title": "Ruinas del campo de batalla",
        "gui.echo_warrior.summoner.button.dismiss": "Retirar",
        "block.echo_warrior.echo_recycler": "Cofre de desguace de ecos",
        "item.echo_warrior.tutorial_manual": "Ecos, guerreros y tú",
        "trait.echo_warrior.raider_slayer.name": "Cazador de asaltantes",
        "trait.echo_warrior.raider_slayer.description": "Inflige un 20 % más de daño a las criaturas de asalto",
        "gui.echo_warrior.summoner.skill.samurai.zanshin.description.2": "Tras acertar un ataque, obtiene un 20 % de esquiva durante 1 segundo, hasta un 80 % en total.",
        "gui.echo_warrior.summoner.skill.samurai.zanshin.description.3": "Una esquiva anula el golpe y concede 1 carga de Fumikomi; no se puede esquivar el daño continuo ni el daño ambiental.",
        "item.echo_warrior.accessory.detail.unique": "No puedes equipar más de un %s con el mismo nombre",
        "tag.item.echo_warrior.accessories.culture.aztec": "Accesorios mexicas",
    },
    "es_mx": {
        "itemGroup.echo_warrior.echo_warrior": "Guerrero del Eco",
        "item.echo_warrior.test_echo_summoner": "Invocador de Ecos",
        "item.echo_warrior.echo_compass": "Brújula de los Ecos",
        "gui.echo_warrior.tutorial.page.battlefield.title": "Ruinas del campo de batalla",
        "gui.echo_warrior.summoner.button.dismiss": "Retirar",
        "block.echo_warrior.echo_recycler": "Cofre de desmantelamiento de ecos",
        "item.echo_warrior.tutorial_manual": "Ecos, guerreros y tú",
        "trait.echo_warrior.raider_slayer.name": "Cazador de asaltantes",
        "trait.echo_warrior.raider_slayer.description": "Inflige un 20 % más de daño a las criaturas de asalto",
        "gui.echo_warrior.summoner.skill.samurai.zanshin.description.2": "Tras acertar un ataque, obtiene un 20 % de esquiva durante 1 segundo, hasta un 80 % en total.",
        "gui.echo_warrior.summoner.skill.samurai.zanshin.description.3": "Una esquiva anula el golpe y concede 1 carga de Fumikomi; no se puede esquivar el daño continuo ni el daño ambiental.",
        "item.echo_warrior.accessory.detail.unique": "No puedes equipar más de un %s con el mismo nombre",
        "item.echo_warrior.echo_compass.tooltip.brush_prefix": "Recuerda llevar una ",
        "tag.item.echo_warrior.accessories.culture.aztec": "Accesorios mexicas",
    }
}

TUTORIAL_LAYOUT_FILES = (
    PROJECT_ROOT / "common/src/client/java/com/yuriscat/echowarrior/client/TutorialManualScreen.java",
    PROJECT_ROOT
    / "versions/1.21.1/common/src/client/java/com/yuriscat/echowarrior/compat/client/TutorialManualScreen1211.java",
)
TUTORIAL_LAYOUT_REQUIRED_SNIPPETS = (
    "if (lines.size() * 9 > ACCESSORY_EFFECT_HEIGHT)",
    "if (lineCount * lineHeight + paragraphGaps * paragraphGap > available)",
    "if (width > maximumWidth)",
    "usesWordWrappedTeamLayout()",
)
KNOWLEDGE_LAYOUT_FILES = (
    PROJECT_ROOT / "common/src/client/java/com/yuriscat/echowarrior/client/KnowledgeReaderScreen.java",
    PROJECT_ROOT
    / "versions/1.21.1/common/src/client/java/com/yuriscat/echowarrior/compat/client/KnowledgeReaderScreen1211.java",
)
KNOWLEDGE_LAYOUT_REQUIRED_SNIPPETS = (
    "lines.size() * BODY_LINE_HEIGHT",
    "MINIMUM_BODY_SCALE_PERCENT",
    "if (width <= CONTENT_WIDTH)",
)
SUMMONER_LAYOUT_FILES = (
    PROJECT_ROOT / "common/src/client/java/com/yuriscat/echowarrior/client/SummonerPreviewScreen.java",
    PROJECT_ROOT
    / "versions/1.21.1/common/src/client/java/com/yuriscat/echowarrior/compat/client/SummonerScreen1211.java",
)
SUMMONER_LAYOUT_REQUIRED_SNIPPETS = (
    "drawFittedText(graphics, heroName",
    "drawCenteredFittedText(graphics",
    "SUMMON_BUTTON_WIDTH - 8",
)
RECYCLER_LAYOUT_FILES = (
    PROJECT_ROOT / "common/src/client/java/com/yuriscat/echowarrior/client/RecyclerScreen.java",
    PROJECT_ROOT
    / "versions/1.21.1/common/src/client/java/com/yuriscat/echowarrior/compat/client/RecyclerScreen1211.java",
)
RECYCLER_LAYOUT_REQUIRED_SNIPPETS = (
    "this.titleLabelX = -1000",
    "int width = this.font.width(sequence)",
    "INFO_X - INFO_HIT_PADDING - TITLE_ICON_GAP - TITLE_X",
    "graphics.pose().scale(scale, scale",
)

SYNCED_UI_TRANSLATION_PAIRS = tuple(
    (
        f"hero.echo_warrior.{hero_id}",
        f"gui.echo_warrior.tutorial.page.{hero_id}.title",
    )
    for hero_id in (
        "roman_legionary",
        "aztec_warrior",
        "egyptian_archer",
        "guandao_warrior",
        "japanese_samurai",
    )
) + tuple(
    (
        f"gui.echo_warrior.summoner.skill.roman.{suffix}",
        f"gui.echo_warrior.tutorial.skill.roman.{suffix}",
    )
    for suffix in (
        "formation.name",
        "formation.description.1",
        "formation.description.2",
        "charge.name",
        "charge.description.1",
        "charge.description.2",
        "endures.name",
        "endures.description.1",
        "endures.description.2",
        "bulwark.name",
        "bulwark.description.1",
    )
)


def read_json(path: Path) -> Any:
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except FileNotFoundError as error:
        raise ValueError(f"Missing file: {path.relative_to(PROJECT_ROOT)}") from error
    except json.JSONDecodeError as error:
        raise ValueError(
            f"Invalid JSON in {path.relative_to(PROJECT_ROOT)}: "
            f"line {error.lineno}, column {error.colno}: {error.msg}"
        ) from error


def read_language(path: Path) -> dict[str, str]:
    value = read_json(path)
    if not isinstance(value, dict):
        raise ValueError(f"Language file must contain a JSON object: {path}")
    invalid = [key for key, text in value.items() if not isinstance(key, str) or not isinstance(text, str)]
    if invalid:
        raise ValueError(f"Language file contains non-string entries: {path}")
    return value


def placeholder_signature(text: str) -> Counter[str]:
    return Counter(PLACEHOLDER_PATTERN.findall(text))


def source_fingerprint(en_text: str, zh_text: str) -> str:
    payload = json.dumps(
        {"en_us": en_text, "zh_cn": zh_text},
        ensure_ascii=False,
        sort_keys=True,
        separators=(",", ":"),
    ).encode("utf-8")
    return hashlib.sha256(payload).hexdigest()


def summarize_keys(keys: list[str]) -> str:
    if not keys:
        return ""
    shown = ", ".join(keys[:REPORT_LIMIT])
    remaining = len(keys) - REPORT_LIMIT
    return f"{shown}{f'，另有 {remaining} 项' if remaining > 0 else ''}"


def load_configuration(path: Path) -> dict[str, Any]:
    config = read_json(path)
    if config.get("schema_version") != 1:
        raise ValueError(f"Unsupported localization config schema: {config.get('schema_version')}")
    return config


def load_state(path: Path) -> dict[str, Any]:
    if not path.exists():
        return {"schema_version": 1, "accepted": {}}
    state = read_json(path)
    if state.get("schema_version") != 1 or not isinstance(state.get("accepted"), dict):
        raise ValueError(f"Unsupported localization state schema: {path}")
    return state


class Audit:
    def __init__(self, config: dict[str, Any], state: dict[str, Any]) -> None:
        self.config = config
        self.state = state
        self.hard_errors: list[str] = []
        self.pending: list[str] = []
        self.info: list[str] = []
        self.locale_hard_errors: dict[str, list[str]] = {}
        self.locale_pending: dict[str, list[str]] = {}
        self.languages: dict[tuple[int, str], dict[str, str]] = {}
        self.source_hashes: dict[str, str] = {}

    def add_hard(self, message: str, locale: str | None = None) -> None:
        self.hard_errors.append(message)
        if locale:
            self.locale_hard_errors.setdefault(locale, []).append(message)

    def add_pending(self, message: str, locale: str | None = None) -> None:
        self.pending.append(message)
        if locale:
            self.locale_pending.setdefault(locale, []).append(message)

    def language_path(self, directory_index: int, locale: str) -> Path:
        directory = PROJECT_ROOT / self.config["language_directories"][directory_index]
        return directory / f"{locale}.json"

    def audit_localized_layout(self) -> None:
        for path in TUTORIAL_LAYOUT_FILES:
            try:
                source = path.read_text(encoding="utf-8")
            except FileNotFoundError:
                self.add_hard(
                    f"教程书多语言布局实现缺失：{path.relative_to(PROJECT_ROOT)}。"
                )
                continue

            relative = path.relative_to(PROJECT_ROOT)
            if "isEnglishLanguage()" in source:
                self.add_hard(
                    f"{relative} 仍按英文语言代码特判文本适配；"
                    "应按实际宽高溢出决定缩放。"
                )
            missing = [
                snippet
                for snippet in TUTORIAL_LAYOUT_REQUIRED_SNIPPETS
                if snippet not in source
            ]
            if missing:
                self.add_hard(
                    f"{relative} 缺少多语言布局保护：{', '.join(missing)}。"
                )

        for path in KNOWLEDGE_LAYOUT_FILES:
            try:
                source = path.read_text(encoding="utf-8")
            except FileNotFoundError:
                self.add_hard(
                    f"知识碎片多语言布局实现缺失：{path.relative_to(PROJECT_ROOT)}。"
                )
                continue

            relative = path.relative_to(PROJECT_ROOT)
            if "isEnglishLanguage()" in source or "isEnglish()" in source:
                self.add_hard(
                    f"{relative} 仍按英文语言代码特判文本适配；"
                    "应按实际宽高溢出决定缩放。"
                )
            missing = [
                snippet
                for snippet in KNOWLEDGE_LAYOUT_REQUIRED_SNIPPETS
                if snippet not in source
            ]
            if missing:
                self.add_hard(
                    f"{relative} 缺少知识碎片多语言布局保护：{', '.join(missing)}。"
                )

        for path in SUMMONER_LAYOUT_FILES:
            try:
                source = path.read_text(encoding="utf-8")
            except FileNotFoundError:
                self.add_hard(
                    f"召唤器多语言布局实现缺失：{path.relative_to(PROJECT_ROOT)}。"
                )
                continue

            relative = path.relative_to(PROJECT_ROOT)
            missing = [
                snippet
                for snippet in SUMMONER_LAYOUT_REQUIRED_SNIPPETS
                if snippet not in source
            ]
            if missing:
                self.add_hard(
                    f"{relative} 缺少召唤器多语言布局保护：{', '.join(missing)}。"
                )

        for path in RECYCLER_LAYOUT_FILES:
            try:
                source = path.read_text(encoding="utf-8")
            except FileNotFoundError:
                self.add_hard(
                    f"回收箱多语言布局实现缺失：{path.relative_to(PROJECT_ROOT)}。"
                )
                continue

            relative = path.relative_to(PROJECT_ROOT)
            missing = [
                snippet
                for snippet in RECYCLER_LAYOUT_REQUIRED_SNIPPETS
                if snippet not in source
            ]
            if missing:
                self.add_hard(
                    f"{relative} 缺少回收箱标题与右侧图标的防重叠保护：{', '.join(missing)}。"
                )

    def validate_language_values(
        self,
        locale: str,
        language: dict[str, str],
        *,
        target_locale: bool,
    ) -> None:
        empty = sorted(key for key, value in language.items() if not value.strip())
        if empty:
            self.add_hard(
                f"{locale} 存在 {len(empty)} 个空文本：{summarize_keys(empty)}。",
                locale if target_locale else None,
            )

        control_characters = sorted(
            key for key, value in language.items() if CONTROL_CHARACTER_PATTERN.search(value)
        )
        if control_characters:
            self.add_hard(
                f"{locale} 存在 {len(control_characters)} 个异常控制字符："
                f"{summarize_keys(control_characters)}。",
                locale if target_locale else None,
            )

        unresolved_keys = sorted(
            key for key, value in language.items() if TRANSLATION_KEY_VALUE_PATTERN.fullmatch(value)
        )
        if unresolved_keys:
            self.add_hard(
                f"{locale} 存在 {len(unresolved_keys)} 个疑似未解析翻译键："
                f"{summarize_keys(unresolved_keys)}。",
                locale if target_locale else None,
            )

        for runtime_key, tutorial_key in SYNCED_UI_TRANSLATION_PAIRS:
            runtime_value = language.get(runtime_key)
            tutorial_value = language.get(tutorial_key)
            if runtime_value is not None and tutorial_value is not None and runtime_value != tutorial_value:
                self.add_hard(
                    f"{locale} 重复界面文案不一致：{runtime_key}=「{runtime_value}」，"
                    f"{tutorial_key}=「{tutorial_value}」。",
                    locale if target_locale else None,
                )

        for fragment, guidance in LOCALE_FORBIDDEN_FRAGMENTS.get(locale, {}).items():
            matches = sorted(key for key, value in language.items() if fragment in value)
            if matches:
                self.add_hard(
                    f"{locale} 出现自动转换风险词「{fragment}」："
                    f"{summarize_keys(matches)}；{guidance}。",
                    locale if target_locale else None,
                )

        for key, expected in LOCALE_REQUIRED_VALUES.get(locale, {}).items():
            actual = language.get(key)
            if actual is not None and actual != expected:
                self.add_hard(
                    f"{locale} 固定术语或审校语义不一致：{key} 应为「{expected}」，"
                    f"实际为「{actual}」。",
                    locale if target_locale else None,
                )

    def load_sources(self) -> None:
        source_locales = self.config["source_locales"]
        directories = self.config["language_directories"]
        for directory_index, _ in enumerate(directories):
            for locale in source_locales:
                path = self.language_path(directory_index, locale)
                try:
                    language = read_language(path)
                    self.languages[(directory_index, locale)] = language
                    self.validate_language_values(locale, language, target_locale=False)
                except ValueError as error:
                    self.add_hard(str(error))

        if self.hard_errors:
            return

        main_en = self.languages[(0, "en_us")]
        main_zh = self.languages[(0, "zh_cn")]
        en_keys = set(main_en)
        zh_keys = set(main_zh)
        if en_keys != zh_keys:
            self.add_hard(
                "核心语言键不一致："
                f"en_us 独有 {summarize_keys(sorted(en_keys - zh_keys)) or '无'}；"
                f"zh_cn 独有 {summarize_keys(sorted(zh_keys - en_keys)) or '无'}。"
            )

        shared_keys = sorted(en_keys & zh_keys)
        for key in shared_keys:
            en_signature = placeholder_signature(main_en[key])
            zh_signature = placeholder_signature(main_zh[key])
            if en_signature != zh_signature:
                self.add_hard(
                    f"核心语言占位符不一致：{key}，en_us={dict(en_signature)}，"
                    f"zh_cn={dict(zh_signature)}。"
                )
            self.source_hashes[key] = source_fingerprint(main_en[key], main_zh[key])

        for directory_index in range(1, len(directories)):
            for locale in source_locales:
                candidate = self.languages[(directory_index, locale)]
                canonical = self.languages[(0, locale)]
                if candidate != canonical:
                    self.add_hard(
                        f"兼容线的 {locale}.json 与主线不一致："
                        f"{self.config['language_directories'][directory_index]}。"
                    )

        self.info.append(f"核心语言：{len(shared_keys)} 个键，主线与兼容线已检查。")

    def audit_locale(self, locale: str, label: str) -> None:
        if self.hard_errors and not self.source_hashes:
            return

        canonical_path = self.language_path(0, locale)
        if not canonical_path.exists():
            self.add_pending(f"{locale}（{label}）：语言文件尚未创建。", locale)
            return

        try:
            canonical = read_language(canonical_path)
        except ValueError as error:
            self.add_hard(str(error), locale)
            return
        self.languages[(0, locale)] = canonical
        self.validate_language_values(locale, canonical, target_locale=True)

        source_keys = set(self.source_hashes)
        locale_keys = set(canonical)
        missing = sorted(source_keys - locale_keys)
        extra = sorted(locale_keys - source_keys)
        if missing:
            self.add_pending(
                f"{locale}（{label}）：缺少 {len(missing)} 个键：{summarize_keys(missing)}。",
                locale,
            )
        if extra:
            self.add_pending(
                f"{locale}（{label}）：存在 {len(extra)} 个已删除或未知键：{summarize_keys(extra)}。",
                locale,
            )

        main_en = self.languages[(0, "en_us")]
        for key in sorted(source_keys & locale_keys):
            expected = placeholder_signature(main_en[key])
            actual = placeholder_signature(canonical[key])
            if expected != actual:
                self.add_hard(
                    f"{locale} 占位符不一致：{key}，应为 {dict(expected)}，"
                    f"实际为 {dict(actual)}。",
                    locale,
                )

        for directory_index in range(1, len(self.config["language_directories"])):
            path = self.language_path(directory_index, locale)
            if not path.exists():
                self.add_pending(
                    f"{locale}（{label}）：兼容线缺少 {path.relative_to(PROJECT_ROOT)}。",
                    locale,
                )
                continue
            try:
                candidate = read_language(path)
            except ValueError as error:
                self.add_hard(str(error), locale)
                continue
            self.languages[(directory_index, locale)] = candidate
            if candidate != canonical:
                self.add_hard(
                    f"{locale} 主线与兼容线内容不一致：{path.relative_to(PROJECT_ROOT)}。",
                    locale,
                )

        accepted = self.state.get("accepted", {}).get(locale)
        accepted_hashes = accepted.get("source_hashes", {}) if isinstance(accepted, dict) else {}
        if not accepted_hashes:
            self.add_pending(
                f"{locale}（{label}）：尚未记录作者接受的翻译基线。",
                locale,
            )
        else:
            new_or_changed = sorted(
                key for key, digest in self.source_hashes.items() if accepted_hashes.get(key) != digest
            )
            removed = sorted(key for key in accepted_hashes if key not in self.source_hashes)
            if new_or_changed:
                self.add_pending(
                    f"{locale}（{label}）：有 {len(new_or_changed)} 个新增或源文本已变化的键："
                    f"{summarize_keys(new_or_changed)}。",
                    locale,
                )
            if removed:
                self.add_pending(
                    f"{locale}（{label}）：基线中有 {len(removed)} 个源键已删除："
                    f"{summarize_keys(removed)}。",
                    locale,
                )

        identical_to_english = sorted(
            key
            for key in source_keys & locale_keys
            if canonical[key] == main_en[key] and any(character.isalpha() for character in main_en[key])
        )
        if identical_to_english:
            self.info.append(
                f"{locale}：{len(identical_to_english)} 个值与英文完全相同，"
                "其中可能包含专名；翻译审校时应抽查。"
            )

    def audit_aliases(self) -> None:
        aliases = self.config.get("exact_aliases", {})
        labels = {entry["code"]: entry["label"] for entry in self.config["target_locales"]}
        for locale, base_locale in aliases.items():
            for directory_index in range(len(self.config["language_directories"])):
                if (directory_index, locale) not in self.languages or (
                    directory_index, base_locale
                ) not in self.languages:
                    continue
                locale_path = self.language_path(directory_index, locale)
                base_path = self.language_path(directory_index, base_locale)
                if locale_path.read_bytes() != base_path.read_bytes():
                    self.add_pending(
                        f"{locale}（{labels.get(locale, locale)}）在 "
                        f"{locale_path.parent.relative_to(PROJECT_ROOT)} 未与基准 "
                        f"{base_locale} 逐字节同步；如需地区化差异，应先修改 config.json。",
                        locale,
                    )

    def run(self) -> None:
        self.audit_localized_layout()
        self.load_sources()
        if self.hard_errors and not self.source_hashes:
            return
        for entry in self.config["target_locales"]:
            self.audit_locale(entry["code"], entry["label"])
        self.audit_aliases()


def write_state(path: Path, state: dict[str, Any]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    temporary = path.with_suffix(path.suffix + ".tmp")
    temporary.write_text(
        json.dumps(state, ensure_ascii=False, indent=2, sort_keys=True) + "\n",
        encoding="utf-8",
    )
    temporary.replace(path)


def mark_current(
    audit: Audit,
    state_path: Path,
    requested_locales: list[str],
) -> None:
    targets = [entry["code"] for entry in audit.config["target_locales"]]
    locales = targets if "all" in requested_locales else requested_locales
    unknown = sorted(set(locales) - set(targets))
    if unknown:
        raise ValueError(f"Unknown target locales: {', '.join(unknown)}")

    blocked: list[str] = []
    for locale in locales:
        if audit.locale_hard_errors.get(locale):
            blocked.extend(audit.locale_hard_errors[locale])
        structural_pending = [
            issue
            for issue in audit.locale_pending.get(locale, [])
            if "翻译基线" not in issue and "新增或源文本已变化" not in issue and "基线中" not in issue
        ]
        blocked.extend(structural_pending)
    if blocked:
        raise ValueError("Cannot accept localization baseline:\n- " + "\n- ".join(blocked))

    accepted = audit.state.setdefault("accepted", {})
    for locale in locales:
        accepted[locale] = {
            "accepted_on": date.today().isoformat(),
            "source_hashes": dict(sorted(audit.source_hashes.items())),
        }
    write_state(state_path, audit.state)
    print(f"已更新翻译基线：{', '.join(locales)}。")


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--config", type=Path, default=DEFAULT_CONFIG)
    parser.add_argument(
        "--allow-pending",
        action="store_true",
        help="Explicitly allow missing or stale non-core translations. Hard errors still fail.",
    )
    parser.add_argument(
        "--release-gate",
        action="store_true",
        help="Label this audit as a CurseForge/Modrinth release gate.",
    )
    parser.add_argument(
        "--mark-current",
        action="append",
        default=[],
        metavar="LOCALE",
        help="Record one fully reviewed locale as current; use 'all' for every target.",
    )
    return parser


def main() -> int:
    args = build_parser().parse_args()
    try:
        config_path = args.config.resolve()
        config = load_configuration(config_path)
        state_path = (PROJECT_ROOT / config["state_path"]).resolve()
        state = load_state(state_path)
        audit = Audit(config, state)
        audit.run()

        heading = "发布本地化门禁" if args.release_gate else "本地化审计"
        print(f"== {heading} ==")
        for message in audit.info:
            print(f"[信息] {message}")
        for message in audit.hard_errors:
            print(f"[错误] {message}")
        for message in audit.pending:
            print(f"[待处理] {message}")

        if args.mark_current:
            if audit.hard_errors:
                raise ValueError("存在硬错误，不能更新翻译基线。")
            mark_current(audit, state_path, args.mark_current)
            return 0

        print(
            f"结果：{len(audit.hard_errors)} 个硬错误，"
            f"{len(audit.pending)} 个待处理项。"
        )
        if audit.hard_errors:
            return 2
        if audit.pending and not args.allow_pending:
            print(
                "发布已暂停：请先更新翻译，或在作者明确同意后使用 "
                "--allow-pending 放行本次发布。"
            )
            return 1
        if audit.pending:
            print("作者已明确放行：本次发布允许非核心本地化缺失或过期。")
        return 0
    except (OSError, ValueError) as error:
        print(f"Localization audit failed: {error}", file=sys.stderr)
        return 2


if __name__ == "__main__":
    raise SystemExit(main())
