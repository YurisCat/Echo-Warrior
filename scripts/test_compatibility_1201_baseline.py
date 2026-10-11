"""Ensure the baseline rejects broken copies of built JARs; never edits originals."""

import importlib.util
import json
from pathlib import Path
import tempfile
import unittest
import zipfile

SCRIPT = Path(__file__).with_name("check-1.20.1-baseline.py")
SPEC = importlib.util.spec_from_file_location("compat1201_baseline", SCRIPT)
BASELINE = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(BASELINE)


class BaselineRejectionTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.config = BASELINE.properties()
        cls.contents = {}
        for loader in ("fabric", "forge"):
            with zipfile.ZipFile(BASELINE.artifact(loader, cls.config)) as jar:
                cls.contents[loader] = {name: jar.read(name) for name in jar.namelist()}

    def rejected(self, loader, mutate, message):
        entries = self.contents[loader].copy()
        mutate(entries)
        with tempfile.TemporaryDirectory(prefix="echo1201-baseline-") as temporary:
            jar_path = Path(temporary) / "invalid-copy.jar"
            with zipfile.ZipFile(jar_path, "w") as jar:
                for name, payload in entries.items():
                    jar.writestr(name, payload)
            with self.assertRaisesRegex(AssertionError, message):
                BASELINE.audit(loader, self.config, jar_path)

    def test_json_formatting_and_line_endings_are_not_resource_changes(self):
        for loader in ("fabric", "forge"):
            for newline in ("\n", "\r\n"):
                with self.subTest(loader=loader, newline=repr(newline)):
                    entries = {name: json.dumps(json.loads(payload), ensure_ascii=False, indent=2,
                                               sort_keys=True).replace("\n", newline).encode("utf-8")
                               if name.endswith(".json") else payload
                               for name, payload in self.contents[loader].items()}
                    with tempfile.TemporaryDirectory(prefix="echo1201-formatting-") as temporary:
                        jar_path = Path(temporary) / "equivalent-copy.jar"
                        with zipfile.ZipFile(jar_path, "w") as jar:
                            for name, payload in entries.items():
                                jar.writestr(name, payload)
                        BASELINE.audit(loader, self.config, jar_path)

    def test_changed_hero_geometry_is_rejected(self):
        def mutate(entries):
            path = "assets/echo_warrior/geo/aztec_warrior_echo.geo.json"
            model = json.loads(entries[path])
            model["minecraft:geometry"][0]["description"]["identifier"] = "geometry.changed"
            entries[path] = json.dumps(model).encode()
        self.rejected("fabric", mutate, "Missing or divergent hero resource")

    def test_changed_archaeology_blockstate_is_rejected(self):
        self.rejected("forge", lambda entries: entries.update({
            "assets/echo_warrior/blockstates/suspicious_grass_block.json":
            b'{"variants":{"":{"model":"minecraft:block/stone"}}}'}), "Missing archaeology resource")

    def test_changed_compass_model_is_rejected(self):
        def mutate(entries):
            path = "assets/echo_warrior/models/item/echo_compass.json"
            model = json.loads(entries[path])
            model["parent"] = "minecraft:block/stone"
            entries[path] = json.dumps(model).encode()
        self.rejected("fabric", mutate, "Missing compass model")

    def test_changed_knowledge_catalog_is_rejected(self):
        def mutate(entries):
            path = "data/echo_warrior/knowledge/entries.json"
            catalog = json.loads(entries[path])
            catalog["test_only_modified"] = True
            entries[path] = json.dumps(catalog).encode()
        self.rejected("forge", mutate, "Missing or divergent knowledge catalog")

    def test_changed_entity_texture_bytes_are_rejected(self):
        def mutate(entries):
            path = next(name for name in entries if name.startswith("assets/echo_warrior/textures/entity/") and name.endswith(".png"))
            entries[path] = b"changed" + entries[path][7:]
        self.rejected("fabric", mutate, "Missing or divergent hero resource")

    def test_missing_refmap(self):
        self.rejected("forge", lambda entries: entries.pop("echo_warrior_1201.refmap.json"), "missing")

    def test_missing_tbf_presence_guard(self):
        self.rejected("forge", lambda entries: entries.pop(
            "com/yuriscat/echowarrior/compat/integration/mixin/TbfPresenceProbeMixin1201.class"), "Missing TBF")

    def test_missing_tbf_manual_tracking_guard(self):
        self.rejected("forge", lambda entries: entries.pop(
            "com/yuriscat/echowarrior/compat/integration/mixin/TbfManualTrackingMixin1201.class"), "Missing TBF")

    def test_removed_tbf_manual_tracking_registration(self):
        def mutate(entries):
            path = "echo_warrior_tbf_forge_1201.mixins.json"
            config = json.loads(entries[path])
            config["mixins"].remove("TbfManualTrackingMixin1201")
            entries[path] = json.dumps(config).encode()
        self.rejected("forge", mutate, "Divergent optional TBF")

    def test_missing_tbf_api_contract(self):
        self.rejected("fabric", lambda entries: entries.pop(
            "com/yuriscat/echowarrior/compat/integration/TbfCompatibility1201.class"), "Missing TBF")

    def test_removed_tbf_owner_restore_registration(self):
        def mutate(entries):
            path = "echo_warrior_tbf_forge_1201.mixins.json"
            config = json.loads(entries[path])
            config["mixins"].remove("TbfSnapshotOwnerMixin1201")
            entries[path] = json.dumps(config).encode()
        self.rejected("forge", mutate, "Divergent optional TBF")

    def test_missing_recycler_client_interaction_guard(self):
        self.rejected("forge", lambda entries: entries.pop(
            "com/yuriscat/echowarrior/compat/client/RecyclerClientSelfTest1201.class"), "missing")

    def test_missing_growth_selector(self):
        def mutate(entries):
            path = "echo_warrior_1201.mixins.json"
            config = json.loads(entries[path])
            config.pop("plugin")
            entries[path] = json.dumps(config).encode()
        self.rejected("forge", mutate, "Missing conditional growth Mixin selector")

    def test_missing_apothic_growth_class(self):
        self.rejected("forge", lambda entries: entries.pop(
            "com/yuriscat/echowarrior/compat/mixin/ApothicGrowthEntityMixin1201.class"),
            "Missing Apothic growth compatibility class")

    def test_missing_growth_selector_class(self):
        self.rejected("fabric", lambda entries: entries.pop(
            "com/yuriscat/echowarrior/compat/integration/GrowthMixinPlugin1201.class"),
            "Missing Apothic growth compatibility class")

    def test_missing_apothic_growth_registration(self):
        def mutate(entries):
            path = "echo_warrior_1201.mixins.json"
            config = json.loads(entries[path])
            config["mixins"].remove("ApothicGrowthEntityMixin1201")
            entries[path] = json.dumps(config).encode()
        self.rejected("forge", mutate, "Unexpected server Mixin list")

    def test_missing_bleeding_effect_sprite(self):
        self.rejected("fabric", lambda entries: entries.pop("assets/minecraft/atlases/mob_effects.json"), "Missing bleeding effect atlas alias")

    def test_missing_compass_frame(self):
        self.rejected("fabric", lambda entries: entries.pop("assets/echo_warrior/textures/item/echo_compass/echo_compass_pointer_17.png"), "Missing compass pointer")

    def test_missing_brushable_model(self):
        self.rejected("forge", lambda entries: entries.pop("assets/echo_warrior/blockstates/suspicious_grass_block.json"), "Missing archaeology resource")

    def test_missing_repair_recipe(self):
        self.rejected("fabric", lambda entries: entries.pop("data/echo_warrior/recipes/craft_legacy_repair.json"), "Missing/wrong old recipe")

    def test_lost_knowledge_loot_nbt(self):
        def mutate(entries):
            path = "data/echo_warrior/loot_tables/gameplay/knowledge_fragment/roman.json"
            table = json.loads(entries[path])
            table["pools"][0]["entries"][0].pop("functions")
            entries[path] = json.dumps(table).encode()
        self.rejected("forge", mutate, "Missing/wrong old loot table")

    def test_missing_held_book_authority(self):
        self.rejected("forge", lambda entries: entries.pop("com/yuriscat/echowarrior/compat/menu/HeldBookMenu1201.class"), "Missing exploration class")

    def test_missing_brushable_runtime_hook(self):
        def mutate(entries):
            path = "echo_warrior_1201.refmap.json"
            mapping = json.loads(entries[path])
            mapping["mappings"]["com/yuriscat/echowarrior/compat/mixin/BrushableTypeMixin1201"].pop("isValid")
            entries[path] = json.dumps(mapping).encode()
        self.rejected("forge", mutate, "Missing exploration runtime mapping")

    def test_missing_battlefield_removal_mapping(self):
        def mutate(entries):
            path = "echo_warrior_1201.refmap.json"
            mapping = json.loads(entries[path])
            mapping["mappings"]["com/yuriscat/echowarrior/compat/mixin/BlockBehaviourMixin1201"].pop("onRemove")
            entries[path] = json.dumps(mapping).encode()
        self.rejected("forge", mutate, "Missing exploration runtime mapping")

    def test_missing_battlefield_removal_registration(self):
        def mutate(entries):
            path = "echo_warrior_1201.mixins.json"
            mixins = json.loads(entries[path])
            mixins["mixins"].remove("BlockBehaviourMixin1201")
            entries[path] = json.dumps(mixins).encode()
        self.rejected("fabric", mutate, "Unexpected server Mixin list")

    def test_missing_battlefield_performance_guard(self):
        self.rejected("fabric", lambda entries: entries.pop(
            "com/yuriscat/echowarrior/compat/world/BattlefieldPerformanceSelfTest1201.class"), "missing")

    def test_missing_hero_model_class(self):
        self.rejected("fabric", lambda entries: entries.pop(
            "com/yuriscat/echowarrior/compat/client/EgyptianArcherModel1201.class"), "Missing hero class")

    def test_missing_hero_animation(self):
        self.rejected("forge", lambda entries: entries.pop(
            "assets/echo_warrior/animations/egyptian_archer_echo.animation.json"), "Missing or divergent hero resource")

    def test_missing_guandao_root_rest_track(self):
        def mutate(entries):
            path = "assets/echo_warrior/animations/guandao_warrior_echo.animation.json"
            animation = json.loads(entries[path])
            animation["animations"]["animation.guandao_warrior.idle"]["bones"]["Main"].pop("rotation")
            entries[path] = json.dumps(animation).encode()
        self.rejected("forge", mutate, "Guandao root-rest conversion")

    def test_changed_guandao_combo_rejected(self):
        def mutate(entries):
            path = "assets/echo_warrior/animations/guandao_warrior_echo.animation.json"
            animation = json.loads(entries[path])
            animation["animations"]["animation.guandao_warrior.combo"]["animation_length"] = 1
            entries[path] = json.dumps(animation).encode()
        self.rejected("fabric", mutate, "Guandao root-rest conversion")

    def test_missing_legacy_model(self):
        self.rejected("fabric", lambda entries: entries.pop(
            "assets/echo_warrior/models/item/courage_legacy.json"), "Missing equipment resource")

    def test_missing_actual_damage_hook(self):
        def mutate(entries):
            path = "echo_warrior_1201.refmap.json"
            mapping = json.loads(entries[path])
            mapping["mappings"]["com/yuriscat/echowarrior/compat/mixin/LivingCombatEventsMixin1201"].pop("actuallyHurt")
            entries[path] = json.dumps(mapping).encode()
        self.rejected("forge", mutate, "Missing hero runtime mapping")

    def test_missing_relic_model(self):
        self.rejected("forge", lambda entries: entries.pop(
            "assets/echo_warrior/models/item/egyptian_archer_relic.json"), "Missing equipment resource")

    def test_wrong_accessory_texture_binding(self):
        def mutate(entries):
            path = "assets/echo_warrior/models/item/plate_armor_accessory.json"
            model = json.loads(entries[path]); model["textures"]["layer0"] = "minecraft:item/paper"
            entries[path] = json.dumps(model).encode()
        self.rejected("fabric", mutate, "Wrong equipment texture binding")

    def test_missing_talent_description(self):
        def mutate(entries):
            path = "assets/echo_warrior/lang/zh_cn.json"
            language = json.loads(entries[path]); language.pop("trait.echo_warrior.courage.description")
            entries[path] = json.dumps(language).encode()
        self.rejected("forge", mutate, "Missing or divergent reviewed translation")

    def test_missing_hong_kong_locale(self):
        self.rejected("forge", lambda entries: entries.pop("assets/echo_warrior/lang/zh_hk.json"), "Missing reviewed locale")

    def test_missing_knowledge_catalog(self):
        self.rejected("fabric", lambda entries: entries.pop("data/echo_warrior/knowledge/entries.json"), "Missing or divergent knowledge catalog")

    def test_missing_old_recipe_path(self):
        self.rejected("forge", lambda entries: entries.pop("data/echo_warrior/recipes/knowledge_fragment_collection.json"), "Missing old-format collection recipe")

    def test_missing_book_model(self):
        self.rejected("fabric", lambda entries: entries.pop("assets/echo_warrior/models/item/tutorial_manual.json"), "Missing equipment resource")

    def test_missing_world_authority(self):
        for loader in ("fabric", "forge"):
            with self.subTest(loader=loader):
                self.rejected(loader, lambda entries: entries.pop(
                    "com/yuriscat/echowarrior/compat/binding/EchoBindingSavedData1201.class"), "missing")

    def test_empty_refmap(self):
        self.rejected("forge", lambda entries: entries.update({
            "echo_warrior_1201.refmap.json": b'{"mappings": {}}'}), "empty production refmap")

    def test_missing_shift_override(self):
        def mutate(entries):
            path = "assets/echo_warrior/models/item/test_echo_summoner.json"
            model = json.loads(entries[path])
            model.pop("overrides")
            entries[path] = json.dumps(model).encode()
        self.rejected("forge", mutate, "summoner Shift icon overrides")

    def test_missing_gui_item_scope(self):
        def mutate(entries):
            path = "echo_warrior_1201.refmap.json"
            mapping = json.loads(entries[path])
            gui = mapping["mappings"]["com/yuriscat/echowarrior/compat/mixin/GuiGraphicsMixin1201"]
            for key in list(gui):
                if key.startswith("renderItem("):
                    gui.pop(key)
            entries[path] = json.dumps(mapping).encode()
        self.rejected("fabric", mutate, "GUI-only item display mapping")

    def test_client_mixin_on_server(self):
        def mutate(entries):
            config = json.loads(entries["echo_warrior_1201.mixins.json"])
            config["mixins"].append(config["client"].pop())
            entries["echo_warrior_1201.mixins.json"] = json.dumps(config).encode()
        self.rejected("forge", mutate, "Unexpected server Mixin list")

    def test_missing_menu_texture(self):
        self.rejected("forge", lambda entries: entries.pop(
            "assets/echo_warrior/textures/gui/summoner/summoner_screen.png"), "missing")

    def test_missing_menu_translation(self):
        def mutate(entries):
            path = "assets/echo_warrior/lang/zh_cn.json"
            language = json.loads(entries[path])
            language.pop("gui.echo_warrior.summoner.button.summon")
            entries[path] = json.dumps(language).encode()
        self.rejected("fabric", mutate, "Missing or divergent reviewed translation")

    def test_missing_menu_sync_mapping(self):
        def mutate(entries):
            path = "echo_warrior_1201.refmap.json"
            mapping = json.loads(entries[path])
            mapping["mappings"].pop("com/yuriscat/echowarrior/compat/mixin/ClientMenuSyncMixin1201")
            entries[path] = json.dumps(mapping).encode()
        self.rejected("forge", mutate, "Missing menu synchronization mapping")

    def test_wrong_java_target(self):
        def mutate(entries):
            name = "com/yuriscat/echowarrior/compat/EchoWarrior1201.class"
            entries[name] = entries[name][:6] + (65).to_bytes(2, "big") + entries[name][8:]
        self.rejected("fabric", mutate, "Not Java 17")

    def test_missing_death_particle_mapping(self):
        def mutate(entries):
            path = "echo_warrior_1201.refmap.json"
            mapping = json.loads(entries[path])
            mapping["mappings"]["com/yuriscat/echowarrior/compat/mixin/LivingEntityMixin1201"].pop("tickDeath")
            entries[path] = json.dumps(mapping).encode()
        self.rejected("forge", mutate, "Missing hero runtime mapping")

    def test_missing_departure_packet_guard(self):
        self.rejected("fabric", lambda entries: entries.pop(
            "com/yuriscat/echowarrior/compat/test/DepartureEffectsSelfTest1201.class"), "Missing departure packet guard")

    def test_missing_final_inventory_tick(self):
        def mutate(entries):
            path = "echo_warrior_1201.refmap.json"
            mapping = json.loads(entries[path])
            mapping["mappings"]["com/yuriscat/echowarrior/compat/mixin/AbstractContainerScreenMixin1201"].pop("tick")
            entries[path] = json.dumps(mapping).encode()
        self.rejected("forge", mutate, "Missing inventory lifecycle mapping")

    def test_loader_metadata_leak(self):
        self.rejected("forge", lambda entries: entries.update({"fabric.mod.json": b"{}"}),
                      "Fabric metadata in Forge")

    def test_missing_creative_drop_snapshot_mapping(self):
        def mutate(entries):
            path = "echo_warrior_1201.refmap.json"
            mapping = json.loads(entries[path])
            mapping["mappings"]["com/yuriscat/echowarrior/compat/mixin/CreativeModeInventoryScreenMixin1201"].pop(
                "Lnet/minecraft/world/inventory/Slot;getItem()Lnet/minecraft/world/item/ItemStack;")
            entries[path] = json.dumps(mapping).encode()
        self.rejected("forge", mutate, "Missing inventory lifecycle mapping")

    def test_wrong_fabric_entrypoint(self):
        def mutate(entries):
            metadata = json.loads(entries["fabric.mod.json"])
            metadata["entrypoints"]["main"] = ["not.present.Entrypoint"]
            entries["fabric.mod.json"] = json.dumps(metadata).encode()
        self.rejected("fabric", mutate, "Missing Fabric entrypoint")

    def test_missing_forge_manifest(self):
        def mutate(entries):
            manifest = entries["META-INF/MANIFEST.MF"]
            entries["META-INF/MANIFEST.MF"] = manifest.replace(b"MixinConfigs:", b"UnusedConfig:")
        self.rejected("forge", mutate, "Missing Forge Mixin manifest")

    def test_srg_refmap_replaced_with_fabric_mapping(self):
        self.rejected("forge", lambda entries: entries.update({
            "echo_warrior_1201.refmap.json": self.contents["fabric"]["echo_warrior_1201.refmap.json"]}),
            "not SRG-mapped")


if __name__ == "__main__":
    unittest.main()
