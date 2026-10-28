"""Offline guards for Maven-only Fabric profile libraries."""
import ast
import hashlib
from pathlib import Path
import re
import tempfile
import unittest


class FabricLibraryPreparationTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.data = b"verified official test fixture"
        self.sha = hashlib.sha1(self.data).hexdigest()
        source = Path(__file__).with_name("prepare-pet-acceptance-client.py")
        tree = ast.parse(source.read_text(encoding="utf-8"))
        tree.body = [n for n in tree.body if isinstance(n, ast.FunctionDef)
                     and n.name in ("sha", "install_fabric_libraries")]
        context = {"Path": Path, "hashlib": hashlib, "re": re}
        exec(compile(tree, str(source), "exec"), context)
        self.install = context["install_fabric_libraries"]
        self.profile = {"libraries": [{"name": "net.fabricmc:fabric-loader:0.19.5",
                                       "url": "https://maven.fabricmc.net/"}]}
        self.requests = []

    def download(self, url, target, sha1):
        self.requests.append((url, sha1))
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_bytes(self.data)

    def test_maven_only_profile_is_downloaded_and_verified(self):
        self.install(self.profile, self.root, self.download, lambda url: self.sha)
        self.assertEqual(len(self.requests), 1)
        self.assertEqual(self.requests[0][1], self.sha)
        self.assertTrue((self.root / "libraries/net/fabricmc/fabric-loader/0.19.5/fabric-loader-0.19.5.jar").is_file())

    def test_silent_installer_skip_is_a_failure(self):
        with self.assertRaisesRegex(ValueError, "absent"):
            self.install(self.profile, self.root, lambda *a, **kw: True, lambda url: self.sha)

    def test_corrupt_download_is_a_failure(self):
        with self.assertRaisesRegex(ValueError, "changed"):
            self.install(self.profile, self.root, self.download, lambda url: "0" * 40)

    def test_invalid_checksum_is_a_failure(self):
        with self.assertRaisesRegex(ValueError, "checksum"):
            self.install(self.profile, self.root, self.download, lambda url: "<html>error</html>")

    def test_coordinate_path_traversal_is_rejected(self):
        self.profile["libraries"][0]["name"] = "../outside:loader:1"
        with self.assertRaisesRegex(ValueError, "coordinate"):
            self.install(self.profile, self.root, self.download, lambda url: self.sha)


if __name__ == "__main__":
    unittest.main()
