#!/usr/bin/env python3
"""Generate culture-bound battlefield loot tables and item metadata tags."""

from __future__ import annotations

import json
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
DATA = ROOT / "src/main/resources/data/echo_warrior"

CULTURES = {
    "roman": {
        "relic": "echo_warrior:roman_legionary_relic",
        "accessories": {
            "common": ["plate_armor_accessory", "hawkeye_lens_accessory"],
            "uncommon": ["feast_ham_accessory", "light_gathering_magnet_accessory"],
            "rare": ["victors_laurel_accessory"],
        },
    },
    "aztec": {
        "relic": "echo_warrior:aztec_warrior_relic",
        "accessories": {
            "common": [],
            "uncommon": ["spiked_armor_accessory", "fractured_crystal_blade_accessory", "sunwheel_garland_accessory", "training_notes_accessory"],
            "rare": ["crack_ring_hammer_charm_accessory"],
        },
    },
    "egyptian": {
        "relic": "echo_warrior:egyptian_archer_relic",
        "accessories": {
            "common": ["tomato_fish_accessory"],
            "uncommon": ["moondew_bottle_accessory", "hollow_bird_bone_accessory"],
            "rare": ["blood_pact_fang_accessory", "memory_ritual_knife_accessory"],
        },
    },
    "chinese": {
        "relic": "echo_warrior:guandao_warrior_relic",
        "accessories": {
            "common": ["battle_worn_whetstone_accessory", "twin_oath_badge_accessory", "heart_sprout_amber_accessory"],
            "uncommon": [],
            "rare": ["peacemaker_accessory", "cat_bell_fish_charm_accessory"],
        },
    },
    "japanese": {
        "relic": "echo_warrior:japanese_samurai_relic",
        "accessories": {
            "common": ["substitute_doll_accessory", "windchaser_feather_accessory"],
            "uncommon": ["chainmail_armor_accessory", "mountain_burden_blade_accessory", "battle_blindfold_accessory"],
            "rare": [],
        },
    },
}

NEUTRAL_ENTRIES = [
    ("minecraft:iron_ingot", 264000),
    ("echo_warrior:courage_legacy", 26400),
    ("echo_warrior:fortitude_legacy", 26400),
    ("echo_warrior:purity_legacy", 26400),
    ("echo_warrior:wisdom_legacy", 26400),
    ("echo_warrior:craft_legacy", 26400),
    ("minecraft:gold_ingot", 52800),
    ("minecraft:netherite_scrap", 1980),
]


def write_json(path: Path, value: object) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def weighted_accessories(groups: dict[str, list[str]]) -> list[tuple[str, int]]:
    available = [(name, mass) for name, mass in (("common", 60), ("uncommon", 30), ("rare", 10)) if groups[name]]
    available_mass = sum(mass for _, mass in available)
    remaining = 1320
    result: list[tuple[str, int]] = []
    for tier_index, (tier, mass) in enumerate(available):
        tier_total = remaining if tier_index == len(available) - 1 else round(1320 * mass / available_mass)
        remaining -= tier_total
        items = groups[tier]
        item_remaining = tier_total
        for item_index, item in enumerate(items):
            weight = item_remaining if item_index == len(items) - 1 else round(tier_total / len(items))
            item_remaining -= weight
            result.append((f"echo_warrior:{item}", weight))
    return result


def item_entry(name: str, weight: int) -> dict[str, object]:
    return {"type": "minecraft:item", "name": name, "weight": weight}


def knowledge_entry(entry_id: str) -> dict[str, object]:
    return {
        "type": "minecraft:item",
        "name": "echo_warrior:knowledge_fragment",
        "weight": 24750,
        "functions": [{
            "function": "minecraft:set_components",
            "components": {"minecraft:custom_data": {"EchoWarriorKnowledgeId": entry_id}},
        }],
    }


def load_knowledge_by_culture() -> dict[str, list[str]]:
    catalog_path = DATA / "knowledge/entries.json"
    catalog = json.loads(catalog_path.read_text(encoding="utf-8"))
    grouped = {culture: [] for culture in CULTURES}
    ordered_entries = sorted(catalog["entries"], key=lambda entry: (list(CULTURES).index(entry["culture"]), entry["order"]))
    for entry in ordered_entries:
        culture = entry["culture"]
        if culture not in grouped:
            raise ValueError(f"Unknown knowledge culture {culture}")
        grouped[culture].append(entry["id"])
    for culture, ids in grouped.items():
        if len(ids) != 8:
            raise ValueError(f"{culture} has {len(ids)} knowledge entries, expected 8")
    return grouped


def generate_chest_knowledge_tables(knowledge_by_culture: dict[str, list[str]]) -> None:
    """Generate the two-stage neutral roll: culture first, then one of its eight pages."""
    base_dir = DATA / "loot_table/gameplay/knowledge_fragment"
    root_entries = []
    for culture in CULTURES:
        culture_entries = []
        for entry_id in knowledge_by_culture[culture]:
            entry = knowledge_entry(entry_id)
            entry["weight"] = 1
            culture_entries.append(entry)
        write_json(base_dir / f"{culture}.json", {
            "type": "minecraft:chest",
            "pools": [{"rolls": 1, "entries": culture_entries}],
            "random_sequence": f"echo_warrior:gameplay/knowledge_fragment/{culture}",
        })
        root_entries.append({
            "type": "minecraft:loot_table",
            "value": f"echo_warrior:gameplay/knowledge_fragment/{culture}",
            "weight": 1,
        })
    write_json(base_dir / "random.json", {
        "type": "minecraft:chest",
        "pools": [{"rolls": 1, "entries": root_entries}],
        "random_sequence": "echo_warrior:gameplay/knowledge_fragment/random",
    })


def main() -> None:
    loot_dir = DATA / "loot_table/archaeology"
    knowledge_by_culture = load_knowledge_by_culture()
    all_by_rarity = {"common": [], "uncommon": [], "rare": []}
    for culture, config in CULTURES.items():
        entries = [item_entry(name, weight) for name, weight in NEUTRAL_ENTRIES]
        entries.extend(knowledge_entry(entry_id) for entry_id in knowledge_by_culture[culture])
        entries.append(item_entry(config["relic"], 9900))
        accessory_weights = weighted_accessories(config["accessories"])
        entries.extend(item_entry(name, weight) for name, weight in accessory_weights)
        total = sum(entry["weight"] for entry in entries)
        if total != 660000:
            raise ValueError(f"{culture} battlefield weights total {total}, expected 660000")
        write_json(loot_dir / f"battlefield_common_{culture}.json", {
            "type": "minecraft:archaeology",
            "pools": [{"rolls": 1, "entries": entries}],
            "random_sequence": f"echo_warrior:archaeology/battlefield_common_{culture}",
        })
        write_json(loot_dir / f"battlefield_guaranteed_{culture}.json", {
            "type": "minecraft:archaeology",
            "pools": [{"rolls": 1, "entries": [item_entry(config["relic"], 1)]}],
            "random_sequence": f"echo_warrior:archaeology/battlefield_guaranteed_{culture}",
        })

        culture_values = []
        for rarity, items in config["accessories"].items():
            tagged = [f"echo_warrior:{item}" for item in items]
            culture_values.extend(tagged)
            all_by_rarity[rarity].extend(tagged)
        write_json(DATA / f"tags/item/accessories/culture/{culture}.json", {"replace": False, "values": culture_values})

    for rarity, values in all_by_rarity.items():
        write_json(DATA / f"tags/item/accessories/rarity/{rarity}.json", {"replace": False, "values": values})
    write_json(DATA / "tags/item/recyclable_knowledge.json", {
        "replace": False,
        "values": ["echo_warrior:knowledge_fragment", "echo_warrior:knowledge_fragment_collection"],
    })
    generate_chest_knowledge_tables(knowledge_by_culture)


if __name__ == "__main__":
    main()
