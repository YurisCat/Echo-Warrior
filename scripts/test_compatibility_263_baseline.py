"""Corrupted candidate archives must fail the 26.3 content gate."""
import importlib.util
from pathlib import Path
import tempfile
import unittest
import zipfile

spec = importlib.util.spec_from_file_location("baseline", Path(__file__).with_name("check-26.3-baseline.py"))
baseline = importlib.util.module_from_spec(spec)
spec.loader.exec_module(baseline)


class BaselineTests(unittest.TestCase):
    def test_corruption_rejected(self):
        values = baseline.properties()
        for loader in ("fabric", "neoforge"):
            original = baseline.LINE / loader / "build/libs" / f"echo-warrior-{loader}-26.3-{values['mod_version']}.jar"
            baseline.audit_jar(original, loader, values)
            with zipfile.ZipFile(original) as archive:
                entries = {name: archive.read(name) for name in archive.namelist()}
            for name, replacement in (
                    ("com/yuriscat/echowarrior/entity/RomanLegionaryEchoEntity.class", None),
                    ("assets/echo_warrior/lang/zh_cn.json", b'{}'),
                    ("assets/echo_warrior/shaders/core/samurai_afterimage.vsh", b'#version 330\n'),
                    ("assets/echo_warrior/icon.png", b'broken'),
                    ("com/yuriscat/echowarrior/mixin/GuiGraphicsExtractorMixin.class",
                     entries["com/yuriscat/echowarrior/mixin/GuiGraphicsExtractorMixin.class"].replace(
                         b"Identifier;Z)V", b"Identifier;I)V"))):
                with self.subTest(loader=loader, entry=name), tempfile.TemporaryDirectory() as directory:
                    broken = Path(directory) / "broken.jar"
                    with zipfile.ZipFile(broken, "w") as archive:
                        for key, value in entries.items():
                            if key != name:
                                archive.writestr(key, value)
                            elif replacement is not None:
                                archive.writestr(key, replacement)
                    with self.assertRaises(ValueError):
                        baseline.audit_jar(broken, loader, values)


if __name__ == "__main__":
    unittest.main()
