"""Check the accepted skill copy, semantic color markup and both source locales."""
from collections import Counter
import json
from pathlib import Path
import re
import runpy

ROOT = Path(__file__).resolve().parents[1]
KEYS = {
    "roman_formation": "roman.formation", "roman_endures": "roman.endures",
    "aztec_curse": "aztec.quetzalcoatls_curse", "aztec_wound": "aztec.obsidian_wound",
    "aztec_macuahuitl": "aztec.macuahuitl", "egyptian_cat_god": "egyptian.cat_god",
    "egyptian_leaf_arrow": "egyptian.leaf_arrow", "egyptian_cone_arrow": "egyptian.cone_arrow",
    "guandao_combo": "guandao.combo",
}
EXAMPLE = {"armor": "22", "release": "200", "curse": "2", "bleed_aztec": "4.53",
           "bleed_egyptian": "3.21", "half": "17.66", "threequarters": "26.48",
           "penetration": "85", "pierce": "75", "hit1": "21.63", "hit2": "30.9", "hit4": "55.61"}
TAG = re.compile(r"<(/?)(gray|attack|level)>")
PLACEHOLDER = re.compile(r"\{([a-z0-9_]+)\}")
SIMPLE_STARS = {
    "roman.endures.simple.1": ["armor", "release"],
    "aztec.quetzalcoatls_curse.simple.2": ["curse"],
    "aztec.obsidian_wound.simple.1": ["bleed"],
    "egyptian.leaf_arrow.simple.1": ["bleed"],
    "egyptian.cone_arrow.simple.1": ["penetration", "pierce"],
}


def plain(text):
    stack = []
    for match in TAG.finditer(text):
        if match[1]:
            assert stack and stack.pop() == match[2], f"Unbalanced style markup: {text}"
        else:
            stack.append(match[2])
    assert not stack, f"Unclosed style markup: {text}"
    result = TAG.sub("", text)
    assert "<" not in result and ">" not in result, f"Malformed markup: {text}"
    return result


def check_formula_layout():
    base = "gui.echo_warrior.summoner.skill."
    directory = ROOT / "common/src/main/resources/assets/echo_warrior/lang"
    for path in sorted(directory.glob("*.json")):
        entries = json.loads(path.read_text(encoding="utf-8"))
        mirror = ROOT / "versions/1.21.1/common/src/main/resources/assets/echo_warrior/lang" / path.name
        assert path.read_bytes() == mirror.read_bytes(), f"Locale mirror drifted: {path.name}"
        stars = 0
        for key, raw in entries.items():
            if not key.startswith(base) or not any(mode in key for mode in (".simple.", ".full.")):
                continue
            text = plain(raw)
            if ".full." in key:
                assert "*" not in text, f"Expanded star: {path.name} {key}"
                for value in PLACEHOLDER.finditer(text):
                    assert re.search(r"[)）]\s*[=＝]\s*$", text[:value.start()]), (
                        f"Result detached from its formula: {path.name} {key} {value[0]}")
            else:
                expected = SIMPLE_STARS.get(key.removeprefix(base), [])
                actual = re.findall(r"\{([a-z0-9_]+)\}\s*%?\*", text)
                assert sorted(actual) == sorted(expected), f"Simple star contract: {path.name} {key}"
                assert text.count("*") == len(expected), f"Fixed value starred: {path.name} {key}"
                stars += len(actual)
        assert stars == 7, f"Missing starred formulas: {path.name}"
        assert all(base + f"guandao.combo.full.{i}" in entries for i in range(1, 7))
        assert base + "guandao.combo.full.7" not in entries


def main():
    accepted = json.loads((ROOT / "tools/update-workboard/skill-copy-draft.json").read_text(encoding="utf-8"))
    assert accepted["status"] == "author_accepted" and len(accepted["skills"]) == 9
    en = json.loads((ROOT / "common/src/main/resources/assets/echo_warrior/lang/en_us.json").read_text(encoding="utf-8"))
    zh = json.loads((ROOT / "common/src/main/resources/assets/echo_warrior/lang/zh_cn.json").read_text(encoding="utf-8"))
    for skill in accepted["skills"]:
        prefix = "gui.echo_warrior.summoner.skill." + KEYS[skill["id"]]
        for mode in ["simple", "full"]:
            for index, expected in enumerate(skill[mode], 1):
                key = f"{prefix}.{mode}.{index}"
                text = plain(zh[key])
                plain(en[key])
                assert Counter(PLACEHOLDER.findall(zh[key])) == Counter(PLACEHOLDER.findall(en[key])), key
                for name in PLACEHOLDER.findall(text):
                    value = EXAMPLE["bleed_aztec" if skill["id"] == "aztec_wound" else "bleed_egyptian"] if name == "bleed" else EXAMPLE[name]
                    text = text.replace("{" + name + "}", value)
                assert text == expected, f"Accepted Chinese copy drifted: {key}\n{text}\n{expected}"
            assert f"{prefix}.{mode}.{len(skill[mode]) + 1}" not in zh, f"Unexpected extra copy: {prefix}.{mode}"
        assert prefix + ".description.1" not in zh, f"Old static copy remains: {prefix}"
    assist = json.loads((ROOT / "tools/update-workboard/assist-copy.json").read_text(encoding="utf-8"))
    assert zh["gui.echo_warrior.summoner.section.assist"] == assist["section"]["zh_cn"]
    for mode in assist["modes"]:
        base = "gui.echo_warrior.summoner.assist." + mode["id"]
        assert zh[base + ".name"] == mode["name"] and zh[base + ".description"] == mode["description"]
    # Development source audit only: no release waiver, translation marking or target-language rewrite.
    module = runpy.run_path(str(ROOT / "scripts/check-localization.py"))
    config = module["load_configuration"](ROOT / "docs/localization/config.json")
    state = module["load_state"](ROOT / config["state_path"])
    audit = module["Audit"](config, state)
    audit.load_sources()
    audit.audit_localized_layout()
    assert not audit.hard_errors, "\n".join(audit.hard_errors)
    check_formula_layout()
    print("PASS skill copy: accepted Chinese, nine short/full pairs, balanced colors, matching dynamic placeholders, Assist and source-locale parity")
    print("PASS all locale formula layout: seven simple stars, no expanded stars, every expanded result follows its formula.")
    print("Translation review and visual acceptance remain separate gates; no waiver was applied.")


if __name__ == "__main__":
    main()
