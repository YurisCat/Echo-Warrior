"""Deterministic release guards; synthetic archives, no network or uploads."""
import importlib.util
import json
from pathlib import Path
import tempfile
import unittest
import zipfile

SPEC = importlib.util.spec_from_file_location("release", Path(__file__).with_name("prepare-curseforge-release.py"))
RELEASE = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(RELEASE)


class ReleaseTests(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory(prefix="echo-release-")
        self.addCleanup(self.temporary.cleanup)
        self.root = Path(self.temporary.name)
        self.locales = self.root / "lang"
        self.locales.mkdir()
        (self.locales / "en_us.json").write_text('{"example": "Echo"}', encoding="utf-8")

    def entries(self, loader, minecraft="1.20.1", version="0.2.2"):
        entries = {name: Path(name.removeprefix("META-INF/").replace("_ECHO_WARRIOR", "")).read_text(encoding="utf-8") for name in RELEASE.REQUIRED_LICENSE_ENTRIES}
        entries["assets/echo_warrior/lang/en_us.json"] = '{"example": "Echo"}'
        descriptor = RELEASE.LOADERS[loader]["descriptor"]
        if loader == "fabric":
            entries[descriptor] = json.dumps({"id": "echo_warrior", "version": version, "depends": {"minecraft": minecraft}})
        else:
            entries[descriptor] = f'[[mods]]\nmodId="echo_warrior"\nversion="{version}"\n[[dependencies.echo_warrior]]\nmodId="minecraft"\nversionRange="[{minecraft},)"\n'
        return entries

    def validate(self, entries, loader="forge"):
        jar = self.root / "echo.jar"
        with zipfile.ZipFile(jar, "w") as archive:
            for name, data in entries.items():
                archive.writestr(name, data)
        RELEASE.validate_loader_jar(jar, RELEASE.LOADERS[loader]["descriptor"], loader, "0.2.2", "1.20.1", self.locales)

    def test_exact_six_target_matrix(self):
        self.assertEqual({(row[3], loader) for row in RELEASE.RELEASE_MATRIX for loader in row[4]}, {
            ("26.1.2", "fabric"), ("26.1.2", "neoforge"),
            ("1.21.1", "fabric"), ("1.21.1", "neoforge"),
            ("1.20.1", "fabric"), ("1.20.1", "forge"),
        })

    def test_three_loader_descriptors(self):
        for loader in RELEASE.LOADERS:
            with self.subTest(loader=loader):
                self.validate(self.entries(loader), loader)

    def test_foreign_descriptors_rejected(self):
        for loader in RELEASE.LOADERS:
            for other in RELEASE.LOADERS:
                if loader == other:
                    continue
                with self.subTest(loader=loader, other=other):
                    entries = self.entries(loader)
                    entries[RELEASE.LOADERS[other]["descriptor"]] = "wrong loader"
                    with self.assertRaisesRegex(ValueError, "other loader descriptor"):
                        self.validate(entries, loader)

    def test_missing_descriptor(self):
        entries = self.entries("forge")
        entries.pop("META-INF/mods.toml")
        with self.assertRaisesRegex(ValueError, "descriptor.*missing"):
            self.validate(entries)

    def test_wrong_version(self):
        with self.assertRaisesRegex(ValueError, "descriptor version"):
            self.validate(self.entries("forge", version="0.2.2-dev.1"))

    def test_wrong_minecraft(self):
        with self.assertRaisesRegex(ValueError, "Minecraft dependency"):
            self.validate(self.entries("forge", minecraft="1.21.1"))

    def test_missing_license(self):
        entries = self.entries("forge")
        entries.pop("META-INF/NOTICE_ECHO_WARRIOR")
        with self.assertRaisesRegex(ValueError, "license/credit"):
            self.validate(entries)

    def test_legacy_license_names(self):
        self.validate({name.replace("_ECHO_WARRIOR", ""): data for name, data in self.entries("forge").items()})

    def test_wrong_license_text(self):
        entries = self.entries("forge")
        entries["META-INF/NOTICE_ECHO_WARRIOR"] = "not the actual notice"
        with self.assertRaisesRegex(ValueError, "text differs"):
            self.validate(entries)

    def test_missing_or_stale_locale(self):
        for stale in (False, True):
            entries = self.entries("forge")
            if stale:
                entries["assets/echo_warrior/lang/en_us.json"] = '{"example": "Old"}'
            else:
                entries.pop("assets/echo_warrior/lang/en_us.json")
            with self.assertRaisesRegex(ValueError, "packaged locale"):
                self.validate(entries)

    def test_required_dependencies(self):
        for loader, config in RELEASE.LOADERS.items():
            projects = {dep["projectID"] for dep in config["dependencies"]}
            self.assertEqual(projects, {661293, 388172, 306612} if loader == "fabric" else {661293, 388172})


if __name__ == "__main__":
    unittest.main()
