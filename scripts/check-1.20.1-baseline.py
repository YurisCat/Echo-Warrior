"""Audit full-content *production* artifacts, not unremapped dev JARs or visual acceptance."""

from __future__ import annotations

import json
from pathlib import Path
import re
import struct
import sys
import tomllib
import zipfile

ROOT = Path(__file__).resolve().parents[1]
COMPAT = ROOT / "versions" / "1.20.1"
RELIC_IDS = tuple(name + "_relic" for name in (
    "roman_legionary", "aztec_warrior", "egyptian_archer", "guandao_warrior", "japanese_samurai"))
ACCESSORY_IDS = tuple(name + "_accessory" for name in (
    "plate_armor", "chainmail_armor", "spiked_armor", "battle_worn_whetstone", "mountain_burden_blade",
    "fractured_crystal_blade", "twin_oath_badge", "battle_blindfold", "crack_ring_hammer_charm", "victors_laurel",
    "blood_pact_fang", "memory_ritual_knife", "substitute_doll", "heart_sprout_amber", "feast_ham", "peacemaker",
    "sunwheel_garland", "moondew_bottle", "tomato_fish", "cat_bell_fish_charm", "light_gathering_magnet",
    "training_notes", "hawkeye_lens", "windchaser_feather", "hollow_bird_bone"))
HERO_CLASSES = ("RomanLegionary", "AztecWarrior", "EgyptianArcher", "GuandaoWarrior", "JapaneseSamurai")
LEGACY_IDS = tuple(name + "_legacy" for name in ("courage", "fortitude", "purity", "wisdom", "craft"))
BOOK_IDS = ("knowledge_fragment", "knowledge_fragment_collection", "tutorial_manual")


def properties() -> dict[str, str]:
    return dict(
        line.split("=", 1)
        for line in (COMPAT / "gradle.properties").read_text(encoding="utf-8").splitlines()
        if line and not line.startswith("#") and "=" in line
    )


def artifact(loader: str, config: dict[str, str]) -> Path:
    return COMPAT / loader / "build" / "libs" / (
        f"{config['archives_base_name']}-{loader}-1.20.1-{config['mod_version']}.jar"
    )


def require(condition: bool, message: str) -> None:
    if not condition:
        raise AssertionError(message)


def resource_matches(payload: bytes, source: Path) -> bool:
    # CI checks out LF text; a Windows source snapshot may use CRLF. JSON
    # formatting is not gameplay data. Keep arrays/values exact and binaries
    # byte-identical rather than accepting an arbitrary resource difference.
    expected = source.read_bytes()
    return json.loads(payload) == json.loads(expected) if source.suffix == ".json" else payload == expected


def audit(loader: str, config: dict[str, str], jar_path: Path | None = None) -> Path:
    jar_path = artifact(loader, config) if jar_path is None else jar_path
    with zipfile.ZipFile(jar_path) as jar:
        names = set(jar.namelist())
        required = {
            "pack.mcmeta", "echo_warrior_1201.mixins.json", "echo_warrior_1201.refmap.json",
            "com/yuriscat/echowarrior/compat/EchoWarrior1201.class",
            "com/yuriscat/echowarrior/compat/entity/behavior/MeleeReachSelfTest1201.class",
            "com/yuriscat/echowarrior/compat/mixin/MinecraftServerMixin1201.class",
            "com/yuriscat/echowarrior/compat/mixin/BlockBehaviourMixin1201.class",
            "com/yuriscat/echowarrior/compat/world/BattlefieldPerformanceSelfTest1201.class",
            "com/yuriscat/echowarrior/compat/mixin/MinecraftClientMixin1201.class",
            "com/yuriscat/echowarrior/compat/mixin/MouseHandlerMixin1201.class",
            "com/yuriscat/echowarrior/compat/mixin/ClientMenuSyncMixin1201.class",
            "com/yuriscat/echowarrior/compat/client/AutomatedTestController1201.class",
            "com/yuriscat/echowarrior/compat/client/NetworkClientSelfTest1201.class",
            "com/yuriscat/echowarrior/compat/client/MenuClientSelfTest1201.class",
            "com/yuriscat/echowarrior/compat/test/JoinPausePolicy1201.class",
            "com/yuriscat/echowarrior/compat/menu/SummonerInsertion1201.class",
            "com/yuriscat/echowarrior/compat/menu/SummonerMenu1201.class",
            "com/yuriscat/echowarrior/compat/client/SummonerScreen1201.class",
            "com/yuriscat/echowarrior/compat/test/SummonerMenuSelfTest1201.class",
            "com/yuriscat/echowarrior/compat/mixin/ServerMenuClickMixin1201.class",
            "com/yuriscat/echowarrior/compat/network/EchoNetwork1201.class",
            "com/yuriscat/echowarrior/compat/network/SummonerInsertRequest1201.class",
            "com/yuriscat/echowarrior/compat/network/SummonerInsertResult1201.class",
            "com/yuriscat/echowarrior/compat/test/NetworkTransactionSelfTest1201.class",
            "com/yuriscat/echowarrior/compat/item/SummonerData1201.class",
            "com/yuriscat/echowarrior/compat/item/EchoSummonerItem1201.class",
            "com/yuriscat/echowarrior/compat/binding/EchoBindingSavedData1201.class",
            "com/yuriscat/echowarrior/compat/binding/EchoBindingSystem1201.class",
            "com/yuriscat/echowarrior/compat/binding/SummonerIdentityTracker1201.class",
            "com/yuriscat/echowarrior/compat/item/SummonerStackContents1201.class",
            "com/yuriscat/echowarrior/compat/test/SummonerStorageSelfTest1201.class",
            "com/yuriscat/echowarrior/compat/test/BindingAuthoritySelfTest1201.class",
            "assets/echo_warrior/models/item/test_echo_summoner.json",
            "assets/echo_warrior/textures/item/test_echo_summoner.png",
            "assets/echo_warrior/textures/gui/summoner/summoner_screen.png",
            "assets/echo_warrior/textures/gui/summoner/bars/fuel_fill.png",
            "assets/echo_warrior/textures/gui/summoner/slot_hints/fuel_slot_hint.png",
            "assets/echo_warrior/textures/gui/summoner/slot_hints/relic_slot_hint.png",
            "assets/echo_warrior/textures/gui/summoner/widgets/skill_empty.png",
            "assets/echo_warrior/lang/en_us.json", "assets/echo_warrior/lang/zh_cn.json",
            "META-INF/LICENSE", "META-INF/LICENSE-CODE", "META-INF/LICENSE-ASSETS.md",
            "META-INF/NOTICE", "META-INF/CREDITS.md",
        }
        require(required <= names, f"{loader}: missing {sorted(required - names)}")
        for path in ("EchoHeroType1201", "ModCreativeTabs1201", "item/EchoRelicItem1201", "item/EchoAccessoryItem1201",
                     "item/EchoRelicProgress1201", "item/EchoRelicState1201", "item/RelicNbt1201",
                     "item/EchoTrait1201", "item/EchoBiomeAffinity1201", "item/EchoSummonerAccessory1201",
                     "item/TooltipShiftState1201", "test/RelicEquipmentSelfTest1201"):
            require(f"com/yuriscat/echowarrior/compat/{path}.class" in names, f"Missing equipment class: {path}")
        for hero in HERO_CLASSES:
            for suffix in (f"entity/{hero}EchoEntity1201", f"client/{hero}Model1201", f"client/{hero}Renderer1201"):
                require(f"com/yuriscat/echowarrior/compat/{suffix}.class" in names, f"Missing hero class: {suffix}")
        require("com/yuriscat/echowarrior/compat/test/HeroLifecycleSelfTest1201.class" in names, "Missing hero lifecycle guard")
        require("com/yuriscat/echowarrior/compat/test/DepartureEffectsSelfTest1201.class" in names, "Missing departure packet guard")
        for item_id in RELIC_IDS + ACCESSORY_IDS + LEGACY_IDS + BOOK_IDS:
            model_path = f"assets/echo_warrior/models/item/{item_id}.json"
            texture_path = f"assets/echo_warrior/textures/item/{item_id}.png"
            require(model_path in names and texture_path in names, f"Missing equipment resource: {item_id}")
            model = json.loads(jar.read(model_path))
            require(model.get("parent") == "minecraft:item/generated"
                    and model.get("textures", {}).get("layer0") == f"echo_warrior:item/{item_id}", "Wrong equipment texture binding")
            source_texture = ROOT / "common/src/main/resources" / texture_path
            require(jar.read(texture_path) == source_texture.read_bytes(), "Authored equipment texture changed")
        # Authored JSON must preserve its data; GUI/entity images preserve bytes.
        old_assets = ROOT / "versions/1.21.1/common/src/main/resources/assets/echo_warrior"
        for directory in ("geo", "animations", "textures/entity", "textures/effect", "textures/mob_effect"):
            sources = (old_assets / directory).glob("*.png") if directory == "textures/entity" else (old_assets / directory).rglob("*")
            for source in sources:
                if not source.is_file():
                    continue
                path = "assets/echo_warrior/" + source.relative_to(old_assets).as_posix()
                if path == "assets/echo_warrior/animations/guandao_warrior_echo.animation.json":
                    expected = json.loads(source.read_bytes())
                    for action in ("idle", "walk"):
                        bones = expected["animations"][f"animation.guandao_warrior.{action}"]["bones"]
                        bones.setdefault("Main", {})["rotation"] = [0, 0, 0]
                    require(path in names and json.loads(jar.read(path)) == expected,
                            "Missing or divergent Guandao root-rest conversion (only idle/walk Main rotation may differ)")
                else:
                    require(path in names and resource_matches(jar.read(path), source), f"Missing or divergent hero resource: {path}")
        gui_source = ROOT / "common/src/main/resources/assets/echo_warrior/textures/gui/summoner"
        for source in gui_source.rglob("*.png"):
            path = "assets/echo_warrior/textures/gui/summoner/" + source.relative_to(gui_source).as_posix()
            require(path in names and jar.read(path) == source.read_bytes(), f"Missing or divergent hero resource: {path}")
        manifest = jar.read("META-INF/MANIFEST.MF").decode().replace("\r\n ", "")
        for part in ("knowledge/KnowledgeCatalog1201", "knowledge/KnowledgeStackData1201", "tutorial/TutorialManualStackData1201",
                     "menu/KnowledgeReaderMenu1201", "menu/TutorialManualMenu1201", "client/KnowledgeReaderScreen1201",
                     "client/TutorialManualScreen1201", "test/BooksSelfTest1201", "client/BooksClientSelfTest1201", "recipe/KnowledgeFragmentCollectionRecipe1201"):
            require(f"com/yuriscat/echowarrior/compat/{part}.class" in names, f"Missing book class: {part}")
        book_catalog = "data/echo_warrior/knowledge/entries.json"
        require(book_catalog in names and resource_matches(jar.read(book_catalog), ROOT / "versions/1.21.1/common/src/main/resources" / book_catalog), "Missing or divergent knowledge catalog")
        require("data/echo_warrior/recipes/knowledge_fragment_collection.json" in names, "Missing old-format collection recipe")
        for directory in ("knowledge", "tutorial"):
            source_dir = ROOT / f"common/src/main/resources/assets/echo_warrior/textures/gui/{directory}"
            for source in source_dir.rglob("*.png"):
                path = f"assets/echo_warrior/textures/gui/{directory}/" + source.relative_to(source_dir).as_posix()
                require(path in names and jar.read(path) == source.read_bytes(), f"Missing or divergent book texture: {path}")
        for source in (ROOT / "common/src/main/resources/assets/echo_warrior/lang").glob("*.json"):
            path = "assets/echo_warrior/lang/" + source.name
            require(path in names, f"Missing reviewed locale: {source.stem}")
            expected = json.loads(source.read_text(encoding="utf-8"))
            actual = json.loads(jar.read(path))
            for key, value in expected.items():
                require(actual.get(key) == value, f"Missing or divergent reviewed translation (equipment included): {source.stem}/{key}")
        require(f"Implementation-Version: {config['mod_version']}" in manifest, "Wrong version")
        require("Built-On-Minecraft: 1.20.1" in manifest, "Wrong game version")
        require("Echo-Warrior-Port-Stage: full-content-candidate" in manifest, "Missing internal stage marker")
        require("META-INF/neoforge.mods.toml" not in names, "NeoForge metadata leaked into old Forge")
        require(not any("1211" in name for name in names), "1.21.1 classes leaked into port")
        for name in names:
            if name.startswith("com/yuriscat/") and name.endswith(".class"):
                magic, _minor, major = struct.unpack(">IHH", jar.read(name)[:8])
                require(magic == 0xCAFEBABE and major == 61, f"Not Java 17 bytecode: {name}")
        mixin = json.loads(jar.read("echo_warrior_1201.mixins.json"))
        require(mixin["required"] and mixin["injectors"]["defaultRequire"] == 1,
                "Missing injections must fail, not silently pass")
        require(mixin["compatibilityLevel"] == "JAVA_17", "Wrong Mixin Java level")
        require(mixin["mixins"] == ["BlockBehaviourMixin1201", "MinecraftServerMixin1201", "ServerMenuClickMixin1201", "PlayerListMixin1201", "PlayerListAccessor1201",
                                    "ServerGamePacketListenerMixin1201", "BlockTalentMixin1201", "CreeperMixin1201",
                                    "ExperienceOrbTalentMixin1201", "FishingHookTalentMixin1201", "LivingEntityMixin1201",
                                    "LivingEntityAccessoryMixin1201", "LivingCombatEventsMixin1201", "MerchantTalentMixin1201",
                                    "PlayerTalentMixin1201", "ShulkerBulletAccessor1201", "ItemEntityLifetimeMixin1201",
                                    "RecyclerBreakMixin1201", "RecyclerExplosionMixin1201", "BrushableTypeMixin1201", "PlacedBlockTrackerMixin1201",
                                    "GeneratedChunkMixin1201"], "Unexpected server Mixin list")
        require(mixin["client"] == ["MinecraftClientMixin1201", "MouseHandlerMixin1201", "ClientMenuSyncMixin1201",
                                    "CreativeModeSlotWrapperAccessor1201", "CreativeModeInventoryScreenInvoker1201",
                                    "CreativeModeInventoryScreenMixin1201", "AbstractContainerScreenMixin1201", "ItemInHandRendererMixin1201", "RecyclerItemRendererMixin1201",
                                    "GuiMixin1201", "GuiGraphicsMixin1201", "GuiGraphicsInvoker1201", "ClientColorsAccessor1201"],
                "Client-only Mixins must be separated from server loading")
        refmap = json.loads(jar.read(mixin["refmap"]))
        require(bool(refmap.get("mappings")), f"{loader}: empty production refmap")
        for mixin_name, method in (("BrushableTypeMixin1201", "isValid"), ("PlacedBlockTrackerMixin1201", "placeBlock"),
                                   ("BlockBehaviourMixin1201", "onRemove"),
                                   ("GuiMixin1201", "setOverlayMessage")):
            require(bool(refmap["mappings"].get(f"com/yuriscat/echowarrior/compat/mixin/{mixin_name}", {}).get(method)),
                    f"Missing exploration runtime mapping: {mixin_name}/{method}")
        for part in ("menu/HeldBookMenu1201", "test/RecyclerSelfTest1201", "world/BattlefieldSystem1201",
                     "world/BattlefieldSavedData1201", "world/EchoCompassSystem1201", "client/EchoCompassClientProperties1201",
                     "client/EchoCompassPulseHud1201", "client/CompassHudClientSelfTest1201", "recipe/CraftLegacyRepairRecipe1201", "knowledge/KnowledgeLootSystem1201"):
            require(f"com/yuriscat/echowarrior/compat/{part}.class" in names, f"Missing exploration class: {part}")
        for directory in ("models/block", "blockstates", "textures/block"):
            for source in (old_assets / directory).rglob("*.json" if "textures" not in directory else "*.png"):
                path = "assets/echo_warrior/" + source.relative_to(old_assets).as_posix()
                require(path in names and resource_matches(jar.read(path), source), f"Missing archaeology resource: {path}")
        for source in (old_assets / "models/item").glob("echo_compass*.json"):
            path = "assets/echo_warrior/models/item/" + source.name
            require(path in names and resource_matches(jar.read(path), source), f"Missing compass model: {path}")
        for frame in range(32):
            path = f"assets/echo_warrior/textures/item/echo_compass/echo_compass_pointer_{frame:02d}.png"
            require(path in names, f"Missing compass pointer: {frame}")
        summoner_model = "assets/echo_warrior/models/item/test_echo_summoner.json"
        require(json.loads(jar.read(summoner_model)) == json.loads((old_assets / "models/item/test_echo_summoner.json").read_text()),
                "Missing/wrong summoner Shift icon overrides")
        require("com/yuriscat/echowarrior/compat/client/SummonerRelicIconProperty1201.class" in names,
                "Missing summoner Shift predicate")
        gui_mapping = refmap["mappings"].get("com/yuriscat/echowarrior/compat/mixin/GuiGraphicsMixin1201", {})
        require(any(key.startswith("renderItem(") and value for key, value in gui_mapping.items()),
                "Missing GUI-only item display mapping")
        recipe_sources = ROOT / "versions/1.21.1/common/src/main/resources/data/echo_warrior/recipe"
        for source in recipe_sources.glob("*.json"):
            expected_recipe = json.loads(source.read_text(encoding="utf-8"))
            if isinstance(expected_recipe.get("result"), dict) and "id" in expected_recipe["result"]:
                expected_recipe["result"]["item"] = expected_recipe["result"].pop("id")
            path = f"data/echo_warrior/recipes/{source.name}"
            require(path in names and json.loads(jar.read(path)) == expected_recipe, f"Missing/wrong old recipe: {source.stem}")
        def legacy_loot(node):
            if isinstance(node, list):
                return [legacy_loot(value) for value in node]
            if not isinstance(node, dict):
                return node
            result = {key: legacy_loot(value) for key, value in node.items()}
            if result.get("function") == "minecraft:set_components":
                component = result.pop("components")
                require(set(component) == {"minecraft:custom_data"}, "Unsupported loot component")
                result["function"] = "minecraft:set_nbt"
                result["tag"] = component["minecraft:custom_data"]
            if result.get("type") == "minecraft:loot_table" and "value" in result:
                result["name"] = result.pop("value")
            if result.get("function") == "minecraft:set_nbt" and isinstance(result.get("tag"), str):
                result["tag"] = json.loads(result["tag"])
            return result
        loot_sources = ROOT / "versions/1.21.1/common/src/main/resources/data/echo_warrior/loot_table"
        for source in loot_sources.rglob("*.json"):
            relative = source.relative_to(loot_sources).as_posix()
            if relative == "blocks/echo_recycler.json":
                continue
            path = "data/echo_warrior/loot_tables/" + relative
            require(path in names and legacy_loot(json.loads(jar.read(path))) == legacy_loot(json.loads(source.read_text(encoding="utf-8"))),
                    f"Missing/wrong old loot table: {relative}")
        for mixin_name, method in (("RecyclerBreakMixin1201", "destroyBlock"), ("RecyclerExplosionMixin1201", "finalizeExplosion"),
                                   ("RecyclerItemRendererMixin1201", "renderByItem")):
            require(bool(refmap["mappings"].get(f"com/yuriscat/echowarrior/compat/mixin/{mixin_name}", {}).get(method)),
                    f"Missing recycler runtime mapping: {mixin_name}/{method}")
        for path in ("assets/echo_warrior/models/item/echo_recycler.json", "assets/echo_warrior/models/block/echo_recycler.json",
                     "assets/echo_warrior/blockstates/echo_recycler.json", "assets/echo_warrior/textures/entity/chest/recycler.png",
                     "data/echo_warrior/loot_tables/blocks/echo_recycler.json", "data/echo_warrior/recipes/echo_recycler.json"):
            require(path in names, f"Missing recycler resource: {path}")
        require(json.loads(jar.read("assets/echo_warrior/models/item/echo_recycler.json")).get("parent") == "minecraft:builtin/entity", "Recycler must use authored chest/latch renderer")
        block_drop = json.loads(jar.read("data/echo_warrior/loot_tables/blocks/echo_recycler.json"))
        require(block_drop["pools"][0]["entries"][0]["functions"][0]["function"] == "minecraft:copy_name", "Recycler needs old-NBT name copy")
        for mixin_name, method in (("LivingEntityMixin1201", "die"), ("LivingEntityMixin1201", "tickDeath"), ("PlayerListAccessor1201", "players"), ("LivingCombatEventsMixin1201", "actuallyHurt"),
                                   ("MerchantTalentMixin1201", "startTrading"), ("PlayerTalentMixin1201", "addAdditionalSaveData"),
                                   ("ItemEntityLifetimeMixin1201", "tick"), ("BlockTalentMixin1201", "playerDestroy")):
            target = f"com/yuriscat/echowarrior/compat/mixin/{mixin_name}"
            require(bool(refmap["mappings"].get(target, {}).get(method)), f"Missing hero runtime mapping: {mixin_name}/{method}")
        for mixin_name, method in (("ServerMenuClickMixin1201", "handleContainerClick"),
                                   ("ClientMenuSyncMixin1201", "handleContainerSetSlot")):
            target = f"com/yuriscat/echowarrior/compat/mixin/{mixin_name}"
            require(bool(refmap["mappings"].get(target, {}).get(method)), "Missing menu synchronization mapping")
        for mixin_name, method in (("AbstractContainerScreenMixin1201", "tick"), ("ItemInHandRendererMixin1201", "tick"),
                                   ("CreativeModeInventoryScreenMixin1201", "slotClicked"),
                                   ("CreativeModeInventoryScreenMixin1201", "handleHotbarLoadOrSave"),
                                   ("CreativeModeInventoryScreenMixin1201", "Lnet/minecraft/world/inventory/Slot;getItem()Lnet/minecraft/world/item/ItemStack;"),
                                   ("ServerGamePacketListenerMixin1201", "handleSetCreativeModeSlot"),
                                   ("PlayerListMixin1201", "remove")):
            target = f"com/yuriscat/echowarrior/compat/mixin/{mixin_name}"
            require(bool(refmap["mappings"].get(target, {}).get(method)), "Missing inventory lifecycle mapping")
        require(json.loads(jar.read("pack.mcmeta"))["pack"]["pack_format"] == 15,
                "Wrong resource pack format")
        effect_atlas = "assets/minecraft/atlases/mob_effects.json"
        require(effect_atlas in names, "Missing bleeding effect atlas alias")
        require({"type": "minecraft:single", "resource": "echo_warrior:mob_effect/obsidian_wound",
                 "sprite": "echo_warrior:bleeding"} in json.loads(jar.read(effect_atlas))["sources"],
                "Missing bleeding effect atlas alias")
        for locale in ("en_us", "zh_cn"):
            language = json.loads(jar.read(f"assets/echo_warrior/lang/{locale}.json"))
            require(bool(language.get("item.echo_warrior.test_echo_summoner")), "Summoner needs both source locales")
            source = json.loads((ROOT / f"common/src/main/resources/assets/echo_warrior/lang/{locale}.json").read_text(encoding="utf-8"))
            keys = {f"item.echo_warrior.{item_id}" for item_id in RELIC_IDS + ACCESSORY_IDS}
            keys.update(("itemGroup.echo_warrior.echo_warrior", "itemGroup.echo_warrior.accessories"))
            keys.update(key for key in source if key.startswith(("trait.echo_warrior.", "hero.echo_warrior.",
                                                                 "tooltip.echo_warrior.relic.", "item.echo_warrior.accessory.")))
            for key in keys:
                require(bool(language.get(key)) and language[key] == source.get(key), f"Missing or divergent equipment translation: {locale}/{key}")
        background = jar.read("assets/echo_warrior/textures/gui/summoner/summoner_screen.png")
        require(struct.unpack(">II", background[16:24]) == (241, 201), "Authored menu size changed")

        if loader == "fabric":
            require("META-INF/mods.toml" not in names, "Forge metadata in Fabric JAR")
            require(not any("/compat/forge/" in name for name in names), "Forge classes in Fabric JAR")
            metadata = json.loads(jar.read("fabric.mod.json"))
            require(metadata["id"] == "echo_warrior" and metadata["version"] == config["mod_version"],
                    "Fabric identity mismatch")
            require(metadata["depends"]["minecraft"] == "1.20.1", "Fabric target too broad")
            require(metadata["depends"]["java"] == ">=17", "Fabric Java mismatch")
            for dependency in ("geckolib", "smartbrainlib", "fabric-api", "fabricloader"):
                require(dependency in metadata["depends"], f"Missing Fabric dependency: {dependency}")
            for entrypoint in metadata["entrypoints"]["main"]:
                require(entrypoint.replace(".", "/") + ".class" in names, "Missing Fabric entrypoint")
            require(metadata["entrypoints"]["client"] == ["com.yuriscat.echowarrior.compat.fabric.EchoClient1201Fabric"],
                    "Missing isolated Fabric client networking entrypoint")
            for entrypoint in metadata["entrypoints"]["client"]:
                require(entrypoint.replace(".", "/") + ".class" in names, "Missing Fabric client entrypoint class")
            require("com/yuriscat/echowarrior/compat/fabric/EchoNetworking1201Fabric.class" in names,
                    "Missing Fabric networking bridge")
            require("method_" in json.dumps(refmap), "Fabric refmap is not intermediary-mapped")
        else:
            require("fabric.mod.json" not in names, "Fabric metadata in Forge JAR")
            require(not any("/compat/fabric/" in name for name in names), "Fabric classes in Forge JAR")
            metadata = tomllib.loads(jar.read("META-INF/mods.toml").decode())
            require(metadata["mods"][0]["version"] == config["mod_version"], "Forge version mismatch")
            dependencies = {item["modId"]: item for item in metadata["dependencies"]["echo_warrior"]}
            require(set(dependencies) == {"forge", "minecraft", "geckolib", "smartbrainlib"},
                    "Forge dependency set mismatch")
            require(all(item["mandatory"] for item in dependencies.values()), "Optional required dependency")
            require(dependencies["minecraft"]["versionRange"] == "[1.20.1,1.20.2)", "Forge target too broad")
            require("MixinConfigs: echo_warrior_1201.mixins.json" in manifest, "Missing Forge Mixin manifest")
            require("com/yuriscat/echowarrior/compat/forge/EchoNetworking1201Forge.class" in names
                    and "com/yuriscat/echowarrior/compat/forge/EchoClient1201Forge.class" in names,
                    "Missing Forge networking bridge/client initializer")
            require(bool(re.search(r"m_\d+_", json.dumps(refmap))), "Forge refmap is not SRG-mapped")
    print(f"PASS {loader}: {jar_path}")
    return jar_path


def main() -> int:
    import runpy
    runpy.run_path(str(ROOT / "scripts/check-battlefield-snow.py"), run_name="__main__")
    runpy.run_path(str(ROOT / "scripts/check-battlefield-performance.py"), run_name="__main__")
    config = properties()
    for loader in ("fabric", "forge"):
        audit(loader, config)
    print("Minecraft 1.20.1 full-content artifact baseline passed (runtime and visual acceptance are separate).")
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except (AssertionError, OSError, KeyError, ValueError, zipfile.BadZipFile) as error:
        print(f"FAIL: {error}", file=sys.stderr)
        sys.exit(1)
