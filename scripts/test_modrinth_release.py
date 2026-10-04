"""Offline regression guards for six-file Modrinth publishing and safe resumption."""

import copy
import contextlib
import hashlib
import importlib.util
import io
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
import urllib.error
import urllib.parse
import zipfile


SPEC = importlib.util.spec_from_file_location("modrinth_release", Path(__file__).with_name("modrinth-release.py"))
RELEASE = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(RELEASE)
PROJECT = "AbCd1234"


class FakeApi:
    def __init__(self):
        self.project = {"id": PROJECT, "slug": "echo-warrior", "title": "Echo Warrior", "project_type": "mod",
                        "source_url": "https://github.com/YurisCat/Echo-Warrior", "status": "draft",
                        "license": {"id": "LicenseRef-Custom"}}
        self.versions = []
        self.posts = []
        self.gets = []
        self.fail_post = None
        self.bad_hash = False
        self.missing_dependency = False

    def get(self, route):
        self.gets.append(route)
        if route == f"/project/{PROJECT}":
            return self.project
        if route == f"/project/{PROJECT}/version?include_changelog=true":
            return copy.deepcopy(self.versions)
        if route.startswith("/version/"):
            return copy.deepcopy(next(v for v in self.versions if v["id"] == route.removeprefix("/version/")))
        for slug, identifier in RELEASE.DEPENDENCIES.items():
            if route == f"/project/{identifier}":
                return {"id": identifier, "slug": slug}
            if route.startswith(f"/project/{identifier}/version?"):
                if self.missing_dependency:
                    return []
                query = urllib.parse.parse_qs(urllib.parse.urlsplit(route).query)
                return [{"status": "listed", "files": [{"filename": "dependency.jar"}],
                         "game_versions": json.loads(query["game_versions"][0]), "loaders": json.loads(query["loaders"][0])}]
        raise AssertionError(f"Unexpected API route: {route}")

    def create_version(self, payload, jar):
        self.posts.append(payload["version_number"])
        if self.fail_post == len(self.posts):
            raise ValueError("Response timed out; remote outcome unknown")
        result = copy.deepcopy(payload)
        result["id"] = f"{len(self.versions) + 1:08d}"
        result["files"] = [{"filename": jar.name, "primary": True, "size": jar.stat().st_size,
                            "hashes": {"sha512": hashlib.sha512(jar.read_bytes()).hexdigest()}}]
        if self.bad_hash:
            result["files"][0]["hashes"]["sha512"] = "0" * 128
        self.versions.append(result)
        return copy.deepcopy(result)


class ModrinthTests(unittest.TestCase):
    def setUp(self):
        captured = contextlib.redirect_stdout(io.StringIO())
        captured.__enter__()
        self.addCleanup(captured.__exit__, None, None, None)
        temporary = tempfile.TemporaryDirectory(prefix="echo-modrinth-")
        self.addCleanup(temporary.cleanup)
        self.root = Path(temporary.name)
        self.locales = self.root / "lang"
        self.locales.mkdir()
        (self.locales / "en_us.json").write_text('{"example":"Echo"}', encoding="utf-8")
        self.output = self.root / "modrinth"
        self.responses = self.root / "responses"
        self.source_manifest = self.root / "source.json"
        source = {"version": "0.2.2", "artifacts": []}
        for target, minecraft, loader, jar_path in RELEASE.expected_targets("0.2.2"):
            jar = self.root / jar_path
            jar.parent.mkdir(parents=True, exist_ok=True)
            entries = {name: Path(name.removeprefix("META-INF/").replace("_ECHO_WARRIOR", "")).read_text(encoding="utf-8")
                       for name in RELEASE.CF.REQUIRED_LICENSE_ENTRIES}
            entries["assets/echo_warrior/lang/en_us.json"] = '{"example":"Echo"}'
            descriptor = RELEASE.CF.LOADERS[loader]["descriptor"]
            if loader == "fabric":
                entries[descriptor] = json.dumps({"id": "echo_warrior", "version": "0.2.2", "depends": {"minecraft": minecraft}})
            else:
                entries[descriptor] = f'[[mods]]\nmodId="echo_warrior"\nversion="0.2.2"\n[[dependencies.echo_warrior]]\nmodId="minecraft"\nversionRange="[{minecraft},)"\n'
            with zipfile.ZipFile(jar, "w") as archive:
                for name, text in entries.items():
                    archive.writestr(name, text)
            metadata = self.root / f"{target}-cf.json"
            RELEASE.write_json(metadata, {
                "displayName": f"Echo Warrior 0.2.2 ({RELEASE.CF.LOADERS[loader]['display_name']} {minecraft})",
                "gameVersionNames": ["Client", "Server", minecraft, RELEASE.CF.LOADERS[loader]["game_version_name"]],
                "relations": {"projects": list(RELEASE.CF.LOADERS[loader]["dependencies"])},
                "releaseType": "release", "changelog": "Added the complete 1.20.1 line.\n\nFixed snow preservation.",
            })
            source["artifacts"].append({"target": target, "jar_path": jar_path,
                                        "metadata_path": str(metadata), "sha256": hashlib.sha256(jar.read_bytes()).hexdigest()})
        RELEASE.write_json(self.source_manifest, source)
        self.plan = self.prepare()
        self.api = FakeApi()

    def prepare(self, destination=PROJECT):
        return RELEASE.prepare_plan(self.source_manifest, self.output, destination, self.root, self.locales)

    def publish(self):
        return RELEASE.publish(self.plan, self.api, self.responses, self.root)

    def test_six_independent_payloads_and_fabric_only_api(self):
        RELEASE.verify_local_plan(self.plan, self.root)
        numbers = {a["metadata"]["version_number"] for a in self.plan["artifacts"]}
        self.assertEqual(len(numbers), 6)
        for artifact in self.plan["artifacts"]:
            payload = artifact["metadata"]
            self.assertEqual(len(payload["loaders"]), 1)
            self.assertEqual(len(payload["game_versions"]), 1)
            dependencies = {d["project_id"] for d in payload["dependencies"]}
            expected = set(RELEASE.DEPENDENCIES.values()) if payload["loaders"] == ["fabric"] else {RELEASE.DEPENDENCIES["geckolib"], RELEASE.DEPENDENCIES["smartbrainlib"]}
            self.assertEqual(dependencies, expected)

    def test_preview_without_project_id_cannot_publish(self):
        self.plan = self.prepare("")
        with self.assertRaisesRegex(ValueError, "project ID"):
            self.publish()
        self.assertEqual(self.api.gets, [])

    def test_duplicate_or_missing_source_target_rejected(self):
        source = RELEASE.read_json(self.source_manifest)
        source["artifacts"][-1] = source["artifacts"][0]
        RELEASE.write_json(self.source_manifest, source)
        with self.assertRaisesRegex(ValueError, "six release targets"):
            self.prepare()

    def test_modified_jar_rejected_before_network(self):
        jar = self.root / self.plan["artifacts"][0]["jar_path"]
        jar.write_bytes(jar.read_bytes() + b"changed")
        with self.assertRaisesRegex(ValueError, "changed after preparation"):
            self.publish()
        self.assertEqual(self.api.gets, [])

    def test_changed_source_manifest_hash_rejected(self):
        source = RELEASE.read_json(self.source_manifest)
        source["artifacts"][0]["sha256"] = "0" * 64
        RELEASE.write_json(self.source_manifest, source)
        with self.assertRaisesRegex(ValueError, "SHA-256"):
            self.prepare()

    def test_wrong_loader_metadata_rejected(self):
        source = RELEASE.read_json(self.source_manifest)
        path = Path(source["artifacts"][0]["metadata_path"])
        metadata = RELEASE.read_json(path)
        metadata["gameVersionNames"][-1] = "Forge"
        RELEASE.write_json(path, metadata)
        with self.assertRaisesRegex(ValueError, "incorrect Minecraft"):
            self.prepare()

    def test_removed_required_dependency_rejected(self):
        artifact = self.plan["artifacts"][0]
        artifact["metadata"]["dependencies"].pop()
        RELEASE.write_json(Path(artifact["metadata_path"]), artifact["metadata"])
        with self.assertRaisesRegex(ValueError, "invalid Modrinth metadata"):
            self.publish()
        self.assertEqual(self.api.posts, [])

    def test_destination_identity_and_license_checked(self):
        for key, value in (("slug", "other-mod"), ("title", "Other Mod"), ("source_url", "https://example.com"),
                           ("license", {"id": "Apache-2.0"}), ("status", "rejected")):
            with self.subTest(key=key):
                api = FakeApi()
                api.project[key] = value
                with self.assertRaises(ValueError):
                    RELEASE.publish(self.plan, api, self.responses, self.root)
                self.assertEqual(api.posts, [])

    def test_missing_dependency_version_stops_every_upload(self):
        self.api.missing_dependency = True
        with self.assertRaisesRegex(ValueError, "no matching Modrinth version"):
            self.publish()
        self.assertEqual(self.api.posts, [])

    def test_preflight_never_posts(self):
        result = RELEASE.preflight(self.plan, self.api, self.root)
        self.assertEqual(len(result["missing"]), 6)
        self.assertEqual(self.api.posts, [])

    def test_empty_generic_draft_accepts_first_mod_versions(self):
        self.api.project.update(project_type="project", loaders=[], game_versions=[])
        self.assertEqual(self.publish()["uploaded"], 6)

    def test_generic_nonempty_or_public_project_is_rejected(self):
        for update in ({"status":"approved"}, {"loaders":["fabric"]}, {"game_versions":["1.20.1"]}):
            with self.subTest(update=update):
                api=FakeApi()
                api.project.update(project_type="project", **update)
                with self.assertRaises(ValueError):
                    RELEASE.preflight(self.plan, api, self.root)
                self.assertEqual(api.posts, [])

    def test_complete_upload_then_rerun_does_not_duplicate(self):
        first = self.publish()
        second = self.publish()
        self.assertEqual(first["uploaded"], 6)
        self.assertEqual(second["uploaded"], 0)
        self.assertEqual(second["reused"], 6)
        self.assertEqual(len(self.api.posts), 6)
        self.assertEqual(RELEASE.read_json(self.responses / "complete.json")["verified_versions"], first["verified_versions"])

    def test_partial_failure_preserves_receipts_and_resume_only_missing(self):
        self.api.fail_post = 2
        with self.assertRaisesRegex(ValueError, "timed out"):
            self.publish()
        targets = [a["target"] for a in self.plan["artifacts"]]
        self.assertEqual(RELEASE.read_json(self.responses / f"{targets[0]}.json")["outcome"], "uploaded_and_verified")
        self.assertEqual(RELEASE.read_json(self.responses / f"{targets[1]}.json")["outcome"], "upload_started_remote_outcome_unknown")
        self.assertFalse((self.responses / "complete.json").exists())
        self.api.fail_post = None
        report = self.publish()
        self.assertEqual(report["uploaded"], 5)
        self.assertEqual(report["reused"], 1)
        self.assertEqual(self.api.posts.count(self.plan["artifacts"][0]["metadata"]["version_number"]), 1)

    def test_timeout_after_acceptance_is_reconciled_by_remote_state(self):
        original = self.api.create_version
        def accept_then_timeout(payload, jar):
            original(payload, jar)
            raise ValueError("Response timed out")
        self.api.create_version = accept_then_timeout
        with self.assertRaises(ValueError):
            self.publish()
        self.api.create_version = original
        self.assertEqual(self.publish()["reused"], 1)
        self.assertEqual(len(self.api.posts), 6)

    def test_conflict_detected_before_uploading_any_missing_file(self):
        last = self.plan["artifacts"][-1]
        remote = self.api.create_version(last["metadata"], self.root / last["jar_path"])
        self.api.versions[0]["files"][0]["hashes"]["sha512"] = "0" * 128
        self.api.posts.clear()
        with self.assertRaisesRegex(ValueError, "file differs"):
            self.publish()
        self.assertEqual(self.api.posts, [])
        self.assertEqual(remote["version_number"], last["metadata"]["version_number"])

    def test_remote_channel_or_dependencies_not_silently_changed(self):
        artifact = self.plan["artifacts"][0]
        self.api.create_version(artifact["metadata"], self.root / artifact["jar_path"])
        self.api.posts.clear()
        for field, value in (("version_type", "beta"), ("dependencies", []), ("changelog", "different")):
            original = self.api.versions[0][field]
            self.api.versions[0][field] = value
            with self.assertRaises(ValueError):
                self.publish()
            self.api.versions[0][field] = original
        self.assertEqual(self.api.posts, [])

    def test_duplicate_hash_under_another_version_number_rejected(self):
        artifact = self.plan["artifacts"][0]
        self.api.create_version(artifact["metadata"], self.root / artifact["jar_path"])
        self.api.versions[0]["version_number"] = "0.2.2"
        self.api.posts.clear()
        with self.assertRaisesRegex(ValueError, "another version number"):
            self.publish()
        self.assertEqual(self.api.posts, [])

    def test_accepted_version_id_saved_before_response_validation(self):
        self.api.bad_hash = True
        with self.assertRaisesRegex(ValueError, "file differs"):
            self.publish()
        saved = RELEASE.read_json(self.responses / f"{self.plan['artifacts'][0]['target']}.json")
        self.assertEqual(saved["version_id"], "00000001")
        self.assertEqual(saved["outcome"], "accepted_pending_verification")
        self.assertEqual(len(self.api.posts), 1)

    def test_multipart_preserves_metadata_and_jar_bytes(self):
        artifact = self.plan["artifacts"][0]
        jar = self.root / artifact["jar_path"]
        data, content_type = RELEASE.multipart(artifact["metadata"], jar)
        self.assertIn(b'name="data"', data)
        self.assertIn(b'name="file"; filename="' + jar.name.encode() + b'"', data)
        self.assertIn(jar.read_bytes(), data)
        self.assertIn(json.dumps(artifact["metadata"], ensure_ascii=False).encode("utf-8"), data)
        self.assertTrue(data.endswith(("--" + content_type.split("boundary=")[1] + "--\r\n").encode()))

    def test_missing_token_and_header_injection_rejected(self):
        for token in ("", "token\nInjected: yes"):
            with self.assertRaises(ValueError):
                RELEASE.Api(token)

    def test_http_error_does_not_expose_token_or_response_body(self):
        api = RELEASE.Api("secret-value")
        with patch.object(api.opener, "open", side_effect=urllib.error.HTTPError("https://api.modrinth.com/v2/version", 401, "secret-value", {}, None)):
            with self.assertRaises(ValueError) as raised:
                api.get("/project/AbCd1234")
            self.assertNotIn("secret-value", str(raised.exception))
            self.assertIn("HTTP 401", str(raised.exception))

    def test_redirects_never_forward_credentials(self):
        self.assertIsNone(RELEASE.NoRedirect().redirect_request(None, None, 302, "", {}, "https://other.example"))

    def test_token_check_requires_private_access_not_just_http_200(self):
        class TokenApi:
            def get(self, route):
                return [{"id": "public01"}]
            def get_public(self, route):
                return [{"id": "public01"}]
        with self.assertRaisesRegex(ValueError, "Cannot prove PROJECT_READ"):
            RELEASE.check_token(TokenApi())
        api = TokenApi()
        api.get = lambda route: [{"id": "public01"}, {"id": "private1"}]
        self.assertEqual(RELEASE.check_token(api), 1)


if __name__ == "__main__":
    unittest.main()
