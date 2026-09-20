"""Offline unit checks: no installer import, downloads, Minecraft or account access."""
import ast
import hashlib
import io
from pathlib import Path
import shutil
import tempfile
import types
import unittest
from unittest.mock import Mock

SOURCE = Path(__file__).with_name("prepare-1.20.1-production-client.py")


class ProductionPreparationTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(prefix="echo-client-prep-")
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.request = Mock()
        self.sleep = Mock()
        self.context = {
            "Path": Path, "hashlib": hashlib, "shutil": shutil,
            "CACHE": self.root, "CACHED_JARS": {},
            "urllib": types.SimpleNamespace(request=self.request),
            "time": types.SimpleNamespace(sleep=self.sleep),
        }
        tree = ast.parse(SOURCE.read_text(encoding="utf-8"))
        tree.body = [node for node in tree.body if isinstance(node, ast.FunctionDef)
                     and node.name in ("download_file", "quote_java_argument")]
        exec(compile(tree, str(SOURCE), "exec"), self.context)
        self.download = self.context["download_file"]
        self.target = self.root / "out" / "client.jar"
        self.data = b"official pinned bytes"
        self.digest = hashlib.sha1(self.data).hexdigest()

    def download_target(self):
        return self.download("https://example.invalid/client.jar", self.target, {}, sha1=self.digest)

    def test_valid_cache_avoids_network(self):
        cached = self.root / "cached.jar"
        cached.write_bytes(self.data)
        self.context["CACHED_JARS"][self.target.name] = [cached]
        self.assertTrue(self.download_target())
        self.assertEqual(self.target.read_bytes(), self.data)
        self.request.urlopen.assert_not_called()

    def test_remapped_cache_not_accepted(self):
        cached = self.root / "remapped.jar"
        cached.write_bytes(b"named game classes are not production")
        self.context["CACHED_JARS"][self.target.name] = [cached]
        self.request.urlopen.return_value = io.BytesIO(self.data)
        self.download_target()
        self.assertEqual(self.target.read_bytes(), self.data)
        self.request.urlopen.assert_called_once()

    def test_download_hash_mismatch_fails_closed(self):
        self.request.urlopen.return_value = io.BytesIO(b"corrupt")
        with self.assertRaisesRegex(RuntimeError, "hash mismatch"):
            self.download_target()
        self.assertFalse(self.target.exists())

    def test_existing_file_is_revalidated(self):
        self.target.parent.mkdir()
        self.target.write_bytes(b"stale")
        self.request.urlopen.return_value = io.BytesIO(self.data)
        self.download_target()
        self.assertEqual(self.target.read_bytes(), self.data)

    def test_tls_failures_are_not_ignored(self):
        self.request.urlopen.side_effect = OSError("certificate verification failed")
        with self.assertRaises(OSError):
            self.download_target()
        self.assertEqual(self.request.urlopen.call_count, 3)
        self.assertEqual(self.sleep.call_count, 2)
        self.assertFalse(self.target.exists())

    def test_arguments_quote_spaces_unicode_backslashes_and_quotes(self):
        quote = self.context["quote_java_argument"]
        self.assertEqual(quote(''), '""')
        self.assertEqual(quote('C:\\测试 path\\a"b'), '"C:\\\\测试 path\\\\a\\"b"')
        for argument in ("safe\ninjected", "safe\rinjected"):
            with self.assertRaises(ValueError):
                quote(argument)


if __name__ == "__main__":
    unittest.main()
