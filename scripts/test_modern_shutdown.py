"""A changed log message cannot make a crash, stale save or missing dimension pass."""
import importlib.util
import os
from pathlib import Path
import tempfile
import time
import unittest

spec = importlib.util.spec_from_file_location("smoke", Path(__file__).with_name("smoke-test-battlefield-mainline.py"))
smoke = importlib.util.module_from_spec(spec)
spec.loader.exec_module(smoke)


class ShutdownTests(unittest.TestCase):
    def setUp(self):
        directory = tempfile.TemporaryDirectory()
        self.addCleanup(directory.cleanup)
        self.world = Path(directory.name)
        self.started = time.time() - 1
        (self.world / "level.dat").write_bytes(b"fixture")
        self.log = "Stopping server\nSaving players\nSaving worlds\n" + "\n".join(
            f"Saving chunks for level 'ServerLevel[test]'/minecraft:{dimension}"
            for dimension in ("overworld", "the_end", "the_nether"))

    def test_new_shutdown(self):
        smoke.require_normal_save(self.log, self.world, "26.3", self.started)

    def test_each_required_record(self):
        for line in self.log.splitlines():
            with self.subTest(line=line), self.assertRaises(RuntimeError):
                smoke.require_normal_save(self.log.replace(line, ""), self.world, "26.3", self.started)

    def test_save_before_shutdown_does_not_count(self):
        with self.assertRaises(RuntimeError):
            smoke.require_normal_save(self.log.replace("Stopping server\n", "") + "\nStopping server",
                                      self.world, "26.3", self.started)

    def test_missing_and_empty_save(self):
        for exists in (True, False):
            if exists:
                (self.world / "level.dat").write_bytes(b"")
            else:
                (self.world / "level.dat").unlink()
            with self.assertRaises(RuntimeError):
                smoke.require_normal_save(self.log, self.world, "26.3", self.started)

    def test_previous_run_save(self):
        os.utime(self.world / "level.dat", (0, 0))
        with self.assertRaises(RuntimeError):
            smoke.require_normal_save(self.log, self.world, "26.3", self.started)

    def test_legacy_checkpoint_retained(self):
        smoke.require_normal_save("All dimensions are saved", self.world, "26.1.2", self.started)
        with self.assertRaises(RuntimeError):
            smoke.require_normal_save(self.log, self.world, "26.1.2", self.started)


if __name__ == "__main__":
    unittest.main()
