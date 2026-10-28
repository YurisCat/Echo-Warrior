"""Offline validation of explicit FOXY joint-test dependency inputs."""

import importlib.util
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
import zipfile

spec = importlib.util.spec_from_file_location("foxy_worker", Path(__file__).with_name("foxy-test-worker.py"))
worker = importlib.util.module_from_spec(spec)
spec.loader.exec_module(worker)
smoke_spec = importlib.util.spec_from_file_location("forge_smoke", Path(__file__).with_name("smoke-test-1.20.1-servers.py"))
smoke = importlib.util.module_from_spec(smoke_spec)
smoke_spec.loader.exec_module(smoke)


class JointInputsTest(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporary.cleanup)
        self.root = Path(self.temporary.name)

    def jar(self, relative):
        path = self.root / relative
        path.parent.mkdir(parents=True, exist_ok=True)
        with zipfile.ZipFile(path, "w") as archive:
            archive.writestr("META-INF/mods.toml", 'modId="test"')
        return path

    def test_absent_is_optional(self):
        self.assertEqual(worker.extra_mod_inputs(None, ["1.20.1"]), [])

    def test_pet_diagnostic_filter_rejects_unrelated_errors(self):
        line = "[12:00:00] [main/ERROR]: No data fixer registered for net"
        self.assertTrue(smoke.pet_datafixer_diagnostic(line, {"pet_home"}))
        for altered, mods in ((line, set()), (line + " unexpected failure", {"pet_home"}),
                              (line.replace("for net", "for unknown"), {"pet_home"}),
                              (line.replace("main/ERROR", "Server thread/ERROR"), {"pet_home"}),
                              ("[main/ERROR]: Pet Home entity crash", {"pet_home"})):
            self.assertFalse(smoke.pet_datafixer_diagnostic(altered, mods))

    def test_pet_acceptance_requires_real_metadata_and_both_boot_modes(self):
        mods = self.root / "mods"
        mods.mkdir()
        with zipfile.ZipFile(mods / "unrelated-filename.jar", "w") as archive:
            archive.writestr("fabric.mod.json", '{"id":"pet_home"}')
        self.assertEqual(smoke.installed_pet_mods(self.root), {"pet_home"})
        api = "[Pet collar compatibility] pet_home storage API verified\n"
        joint = "[PetCollarJointSelfTest] PASS\n"
        written = "[PetCollarDiskSelfTest] PASS mode=written\n"
        reloaded = "[PetCollarDiskSelfTest] PASS mode=reloaded\n"
        for incomplete in ("Done (1.0s)!", api + joint, joint + written, api + written):
            with self.assertRaises(RuntimeError):
                smoke.require_pet_joint(incomplete, self.root)
        smoke.require_pet_joint(api + joint + written, self.root, 1)
        smoke.require_pet_joint(api + joint + reloaded, self.root, 2)
        with self.assertRaises(RuntimeError):
            smoke.require_pet_joint(api + joint + written, self.root, 2)

    def test_pet_filename_is_not_installation_evidence(self):
        mods = self.root / "mods"
        mods.mkdir()
        with zipfile.ZipFile(mods / "pet_home-fake.jar", "w") as archive:
            archive.writestr("fabric.mod.json", '{"id":"ordinary_fixture"}')
        self.assertEqual(smoke.installed_pet_mods(self.root), set())

    def test_generic_tbf_combo_requires_handler_acceptance_despite_non_utf8_comment(self):
        mods = self.root / "mods"
        mods.mkdir()
        with zipfile.ZipFile(mods / "official-fixture.jar", "w") as archive:
            archive.writestr("META-INF/mods.toml", b'# \xb8\n[[mods]]\nmodId="trulybestfriends"\n')
        with patch.dict("os.environ", {}, clear=True):
            self.assertEqual(smoke.joint_test_flags(self.root), ["-DechoWarrior.tbfRequired=true"])
        self.assertEqual(smoke.installed_pet_mods(self.root), set())

    def test_valid_multiple_dependencies(self):
        first = self.jar("1.20.1/forge/apothic.jar")
        second = self.jar("1.20.1/forge/placebo.jar")
        self.assertEqual(worker.extra_mod_inputs(self.root, ["1.20.1"]),
                         [("1.20.1", "forge", first), ("1.20.1", "forge", second)])

    def test_wrong_version_rejected(self):
        self.jar("1.21.1/neoforge/test.jar")
        with self.assertRaises(ValueError):
            worker.extra_mod_inputs(self.root, ["1.20.1"])

    def test_wrong_loader_rejected(self):
        self.jar("1.20.1/neoforge/test.jar")
        with self.assertRaises(ValueError):
            worker.extra_mod_inputs(self.root, ["1.20.1"])

    def test_unstructured_path_rejected(self):
        self.jar("test.jar")
        with self.assertRaises(ValueError):
            worker.extra_mod_inputs(self.root, ["1.20.1"])

    def test_invalid_jar_rejected(self):
        path = self.jar("1.20.1/forge/test.jar")
        path.write_text("not a jar")
        with self.assertRaises(ValueError):
            worker.extra_mod_inputs(self.root, ["1.20.1"])

    def test_empty_directory_rejected(self):
        with self.assertRaises(ValueError):
            worker.extra_mod_inputs(self.root, ["1.20.1"])

    def test_extra_only_cannot_silently_run_without_mods(self):
        with self.assertRaises(ValueError):
            worker.package(self.root / "output.zip", ["1.20.1"], extra_only=True)

    def test_tbf_and_generic_modes_do_not_mix(self):
        path = self.jar("1.20.1/forge/test.jar")
        with self.assertRaises(ValueError):
            worker.package(self.root / "output.zip", ["1.20.1"],
                           tbf_forge=path, extra_directory=self.root)

    def test_joint_forge_override_requires_forge_inputs(self):
        self.jar("1.21.1/neoforge/test.jar")
        with self.assertRaises(ValueError):
            worker.package(self.root / "output.zip", ["1.21.1"],
                           extra_directory=self.root, joint_forge_version="47.4.20")

    def test_tbf_forge_override_reaches_packaging_after_input_validation(self):
        path = self.jar("trulybestfriends.jar")
        # The explicit TBF input is valid; stop before packaging this repository.
        with patch.object(worker.subprocess, "check_output", side_effect=RuntimeError("packaging reached")):
            with self.assertRaisesRegex(RuntimeError, "packaging reached"):
                worker.package(self.root / "output.zip", ["1.20.1"],
                               tbf_forge=path, joint_forge_version="47.4.20")

    def test_tbf_override_is_only_forwarded_to_forge_1201(self):
        self.assertEqual(worker.joint_server_command("1.20.1", "forge", "47.4.20")[-2:],
                         ["--forge-version", "47.4.20"])
        self.assertNotIn("--forge-version", worker.joint_server_command("1.21.1", "neoforge", "47.4.20"))
        self.assertNotIn("--forge-version", worker.joint_server_command("1.20.1", "fabric", "47.4.20"))

    def test_forge_cache_rejects_corrupted_bundle_before_staging(self):
        (self.root / "1.20.1-server.jar").write_bytes(b"corrupted download")
        run = self.root / "run"
        with patch.dict(smoke.os.environ, {"ECHO_WARRIOR_VANILLA_CACHE": str(self.root)}):
            with self.assertRaisesRegex(RuntimeError, "cache hash mismatch"):
                smoke.seed_forge_vanilla(run, "1.20.1")
        self.assertFalse(run.exists())

    def test_explicit_pack_library_is_used_without_a_download(self):
        library = self.jar("geckolib.jar")
        with patch.dict(smoke.os.environ, {"ECHO_WARRIOR_TEST_REQUIRED_MODS": str(self.root)}):
            with patch.object(smoke, "download", side_effect=AssertionError("Unexpected network fallback")):
                self.assertEqual(smoke.required_dependency_source("https://example.invalid", library.name), library)

    def test_missing_explicit_library_cannot_fall_back_to_a_different_package(self):
        with patch.dict(smoke.os.environ, {"ECHO_WARRIOR_TEST_REQUIRED_MODS": str(self.root)}):
            with patch.object(smoke, "download", side_effect=AssertionError("Unexpected network fallback")):
                with self.assertRaisesRegex(RuntimeError, "reported-pack dependency"):
                    smoke.required_dependency_source("https://example.invalid", "missing.jar")


if __name__ == "__main__":
    unittest.main()
