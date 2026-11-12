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
           "penetration": "85", "pierce": "75", "hit1": "21.63", "hit2": "30.90", "hit4": "55.61"}
TAG = re.compile(r"<(/?)(gray|attack|level)>")
PLACEHOLDER = re.compile(r"\{([a-z0-9_]+)\}")


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
    print("PASS skill copy: accepted Chinese, nine short/full pairs, balanced colors, matching dynamic placeholders, Assist and source-locale parity")
    print("Non-source translations remain a separate release gate; no waiver was applied.")


if __name__ == "__main__":
    main()
