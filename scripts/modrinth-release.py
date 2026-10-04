#!/usr/bin/env python3
"""Prepare, check, and publish the verified Echo Warrior six-file release to Modrinth."""

from __future__ import annotations

import argparse
import hashlib
import importlib.util
import json
import os
from pathlib import Path
import re
import sys
import urllib.error
import urllib.parse
import urllib.request
import uuid


SPEC = importlib.util.spec_from_file_location(
    "curseforge_release", Path(__file__).with_name("prepare-curseforge-release.py")
)
CF = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(CF)

API_URL = "https://api.modrinth.com/v2"
USER_AGENT = "YurisCat/Echo-Warrior/release (https://github.com/YurisCat/Echo-Warrior)"
LICENSE_ID = "LicenseRef-Echo-Warrior-Mixed"
LICENSE_URL = "https://github.com/YurisCat/Echo-Warrior/blob/main/LICENSE"
DEPENDENCIES = {
    "geckolib": "8BmcQJ2H",
    "smartbrainlib": "PuyPazRT",
    "fabric-api": "P7dR8mSH",
}
VERSION_FIELDS = (
    "project_id", "name", "version_number", "game_versions", "loaders",
    "version_type", "changelog", "status",
)


def read_json(path: Path):
    return json.loads(path.read_text(encoding="utf-8"))


def write_json(path: Path, value) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def project_id(value: str, required: bool = True) -> str:
    if not value and not required:
        return ""
    if not re.fullmatch(r"[A-Za-z0-9]{8}", value):
        raise ValueError("MODRINTH_PROJECT_ID must be the stable eight-character project ID.")
    return value


def expected_targets(version: str):
    for key, directory, _, minecraft, loaders in CF.RELEASE_MATRIX:
        for loader in loaders:
            yield (
                f"{key}_{loader}", minecraft, loader,
                (Path(directory) / loader / "build/libs" /
                 f"echo-warrior-{loader}-{minecraft}-{version}.jar").as_posix(),
            )


def prepare_plan(
    source_manifest: Path,
    output_directory: Path,
    destination: str,
    root: Path = Path("."),
    language_directory: Path = Path("common/src/main/resources/assets/echo_warrior/lang"),
) -> dict:
    """Convert the already verified CF matrix, rechecking JARs and hashes before use."""
    destination = project_id(destination, required=False)
    source = read_json(source_manifest)
    version = source["version"]
    if not re.fullmatch(r"\d+\.\d+\.\d+", version):
        raise ValueError("Public release requires a plain x.y.z version.")
    expected = {target: (minecraft, loader, path) for target, minecraft, loader, path in expected_targets(version)}
    artifacts = source["artifacts"]
    if len(artifacts) != 6 or {a["target"] for a in artifacts} != set(expected):
        raise ValueError("Source manifest must contain exactly the six release targets, without duplicates.")
    result = {"version": version, "project_id": destination, "artifacts": []}
    for artifact in artifacts:
        target = artifact["target"]
        minecraft, loader, expected_path = expected[target]
        if artifact["jar_path"] != expected_path:
            raise ValueError(f"{target}: unexpected release JAR path.")
        jar = root / expected_path
        contents = jar.read_bytes()
        sha256 = hashlib.sha256(contents).hexdigest()
        if sha256 != artifact["sha256"]:
            raise ValueError(f"{target}: JAR SHA-256 differs from the verified source manifest.")
        CF.validate_loader_jar(jar, CF.LOADERS[loader]["descriptor"], loader, version, minecraft, language_directory)
        metadata_path = Path(artifact["metadata_path"])
        if not metadata_path.resolve().is_relative_to(root.resolve()):
            raise ValueError(f"{target}: metadata must be inside the project root.")
        metadata = read_json(metadata_path)
        wanted_versions = ["Client", "Server", minecraft, CF.LOADERS[loader]["game_version_name"]]
        if metadata["gameVersionNames"] != wanted_versions:
            raise ValueError(f"{target}: incorrect Minecraft, loader, or environment metadata.")
        if metadata["relations"]["projects"] != list(CF.LOADERS[loader]["dependencies"]):
            raise ValueError(f"{target}: incorrect required dependencies.")
        if metadata["releaseType"] not in {"release", "beta", "alpha"} or not metadata["changelog"].strip():
            raise ValueError(f"{target}: invalid release type or empty changelog.")
        if metadata["displayName"] != f"Echo Warrior {version} ({CF.LOADERS[loader]['display_name']} {minecraft})":
            raise ValueError(f"{target}: incorrect display name.")
        required = ["geckolib", "smartbrainlib"] + (["fabric-api"] if loader == "fabric" else [])
        payload = {
            "project_id": destination,
            "name": metadata["displayName"],
            "version_number": f"{version}+mc{minecraft}-{loader}",
            "changelog": metadata["changelog"],
            "game_versions": [minecraft],
            "loaders": [loader],
            "version_type": metadata["releaseType"],
            "dependencies": [{"project_id": DEPENDENCIES[slug], "dependency_type": "required"} for slug in required],
            "featured": False,
            "status": "listed",
            "file_parts": ["file"],
            "primary_file": "file",
        }
        metadata_path = output_directory / f"{minecraft}-{loader}-metadata.json"
        write_json(metadata_path, payload)
        result["artifacts"].append({
            "target": target, "jar_path": expected_path,
            "metadata_path": metadata_path.as_posix(), "metadata": payload,
            "sha256": sha256, "sha512": hashlib.sha512(contents).hexdigest(), "size": len(contents),
        })
    write_json(output_directory / "release-manifest.json", result)
    return result


def dependency_set(dependencies):
    return sorted((dep.get("project_id"), dep.get("version_id"), dep.get("dependency_type")) for dep in dependencies)


def verify_local_plan(plan: dict, root: Path = Path(".")) -> None:
    project_id(plan["project_id"])
    version = plan["version"]
    if not re.fullmatch(r"\d+\.\d+\.\d+", version):
        raise ValueError("Invalid public release version.")
    expected = {target: (minecraft, loader, path) for target, minecraft, loader, path in expected_targets(version)}
    artifacts = plan["artifacts"]
    if len(artifacts) != 6 or {a["target"] for a in artifacts} != set(expected):
        raise ValueError("Expected all six unique release targets.")
    for artifact in artifacts:
        target = artifact["target"]
        minecraft, loader, expected_path = expected[target]
        if artifact["jar_path"] != expected_path:
            raise ValueError(f"{target}: unexpected JAR path.")
        contents = (root / expected_path).read_bytes()
        if (len(contents) != artifact["size"] or hashlib.sha256(contents).hexdigest() != artifact["sha256"]
                or hashlib.sha512(contents).hexdigest() != artifact["sha512"]):
            raise ValueError(f"{target}: release JAR changed after preparation.")
        payload = artifact["metadata"]
        required = ["geckolib", "smartbrainlib"] + (["fabric-api"] if loader == "fabric" else [])
        wanted_dependencies = [{"project_id": DEPENDENCIES[slug], "dependency_type": "required"} for slug in required]
        if (payload["project_id"] != plan["project_id"] or payload["game_versions"] != [minecraft]
                or payload["loaders"] != [loader]
                or payload["version_number"] != f"{version}+mc{minecraft}-{loader}"
                or payload["name"] != f"Echo Warrior {version} ({CF.LOADERS[loader]['display_name']} {minecraft})"
                or payload["status"] != "listed" or payload["version_type"] not in {"release", "beta", "alpha"}
                or not payload["changelog"].strip()
                or payload["file_parts"] != ["file"] or payload["primary_file"] != "file"
                or dependency_set(payload["dependencies"]) != dependency_set(wanted_dependencies)):
            raise ValueError(f"{target}: invalid Modrinth metadata.")
        if read_json(Path(artifact["metadata_path"])) != payload:
            raise ValueError(f"{target}: metadata file differs from the upload manifest.")


class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        return None  # Never forward the token to a redirected host.


def multipart(payload: dict, jar: Path) -> tuple[bytes, str]:
    boundary = "echo-warrior-" + uuid.uuid4().hex
    if not re.fullmatch(r"[A-Za-z0-9_.+-]+\.jar", jar.name):
        raise ValueError("Unexpected upload filename.")
    body = (
        f'--{boundary}\r\nContent-Disposition: form-data; name="data"\r\n'
        'Content-Type: application/json\r\n\r\n'
    ).encode() + json.dumps(payload, ensure_ascii=False).encode("utf-8") + (
        f'\r\n--{boundary}\r\nContent-Disposition: form-data; name="file"; filename="{jar.name}"\r\n'
        'Content-Type: application/java-archive\r\n\r\n'
    ).encode() + jar.read_bytes() + f"\r\n--{boundary}--\r\n".encode()
    return body, f"multipart/form-data; boundary={boundary}"


class Api:
    def __init__(self, token: str):
        if not token or any(c in token for c in "\r\n"):
            raise ValueError("Missing or invalid repository secret MODRINTH_API_TOKEN.")
        self.token = token
        self.opener = urllib.request.build_opener(NoRedirect())

    def request(self, method: str, route: str, data=None, content_type=None, authenticated=True):
        headers = {"User-Agent": USER_AGENT, "Accept": "application/json"}
        if authenticated:
            headers["Authorization"] = self.token
        if content_type:
            headers["Content-Type"] = content_type
        request = urllib.request.Request(API_URL + route, data=data, headers=headers, method=method)
        try:
            with self.opener.open(request, timeout=300 if method == "POST" else 30) as response:
                return json.load(response)
        except urllib.error.HTTPError as error:
            # Do not print headers, credentials, or arbitrary response bodies.
            raise ValueError(f"Modrinth {method} {route} returned HTTP {error.code}; inspect the saved receipts before retrying.") from None
        except (urllib.error.URLError, TimeoutError, OSError, ValueError):
            raise ValueError(f"Modrinth {method} {route} did not return a verified response; inspect remote state before retrying.") from None

    def get(self, route: str):
        return self.request("GET", route)

    def get_public(self, route: str):
        return self.request("GET", route, authenticated=False)

    def create_version(self, payload: dict, jar: Path):
        body, content_type = multipart(payload, jar)
        return self.request("POST", "/version", body, content_type)


def verify_remote_version(artifact: dict, remote: dict) -> str:
    identifier = project_id(remote.get("id", ""))
    payload = artifact["metadata"]
    if any(remote.get(field) != payload[field] for field in VERSION_FIELDS):
        raise ValueError(f"{artifact['target']}: existing version metadata differs; refusing to overwrite or duplicate it.")
    if dependency_set(remote.get("dependencies", [])) != dependency_set(payload["dependencies"]):
        raise ValueError(f"{artifact['target']}: existing version dependencies differ.")
    files = remote.get("files", [])
    if (len(files) != 1 or not files[0].get("primary") or files[0].get("filename") != Path(artifact["jar_path"]).name
            or files[0].get("size") != artifact["size"] or files[0].get("hashes", {}).get("sha512") != artifact["sha512"]):
        raise ValueError(f"{artifact['target']}: existing version file differs from the verified JAR.")
    return identifier


def find_existing(plan: dict, remote_versions: list) -> dict:
    result = {}
    for artifact in plan["artifacts"]:
        number = artifact["metadata"]["version_number"]
        matches = [v for v in remote_versions if v.get("version_number") == number]
        if len(matches) > 1:
            raise ValueError(f"{artifact['target']}: multiple remote versions use {number}; resolve them before publishing.")
        if matches:
            result[artifact["target"]] = verify_remote_version(artifact, matches[0])
        else:
            same_file = [v for v in remote_versions if any(f.get("hashes", {}).get("sha512") == artifact["sha512"] for f in v.get("files", []))]
            if same_file:
                raise ValueError(f"{artifact['target']}: this JAR already exists under another version number; do not duplicate it.")
    return result


def preflight(plan: dict, api: Api, root: Path = Path(".")) -> dict:
    verify_local_plan(plan, root)
    destination = plan["project_id"]
    project = api.get(f"/project/{destination}")
    remote_versions = api.get(f"/project/{destination}/version?include_changelog=true")
    # Modrinth derives the type from uploaded loaders; a new empty draft is
    # 'project' until its first version (LegacyProject::get_project_type).
    empty_draft = (project.get("project_type") == "project" and project.get("status") == "draft"
                   and not remote_versions and not project.get("loaders") and not project.get("game_versions"))
    if (project.get("id") != destination or project.get("slug") != "echo-warrior"
            or project.get("title") != "Echo Warrior" or (project.get("project_type") != "mod" and not empty_draft)
            or project.get("source_url", "").rstrip("/") != "https://github.com/YurisCat/Echo-Warrior"
            or project.get("license", {}).get("id") != LICENSE_ID
            or project.get("license", {}).get("url") != LICENSE_URL):
        raise ValueError("Destination must be the official Echo Warrior mod with its source link and custom license.")
    if project.get("status") not in {"draft", "processing", "approved", "unlisted"}:
        raise ValueError("Project status does not permit this release; resolve moderation status first.")
    for slug, dependency in DEPENDENCIES.items():
        remote = api.get(f"/project/{dependency}")
        if remote.get("id") != dependency or remote.get("slug") != slug:
            raise ValueError(f"Required dependency {slug} does not match its configured project ID.")
    # Check that each loader/game combination has a downloadable dependency version.
    for artifact in plan["artifacts"]:
        payload = artifact["metadata"]
        query = urllib.parse.urlencode({"game_versions": json.dumps(payload["game_versions"]),
                                        "loaders": json.dumps(payload["loaders"]), "include_changelog": "false"})
        for dep in payload["dependencies"]:
            versions = api.get(f"/project/{dep['project_id']}/version?{query}")
            if not any(v.get("files") and v.get("status") == "listed"
                       and all(g in v.get("game_versions", []) for g in payload["game_versions"])
                       and all(l in v.get("loaders", []) for l in payload["loaders"]) for v in versions):
                raise ValueError(f"{artifact['target']}: no matching Modrinth version for dependency {dep['project_id']}.")
    existing = find_existing(plan, remote_versions)
    return {"project_id": destination, "version": plan["version"], "project_status": project["status"],
            "existing": existing, "missing": [a["target"] for a in plan["artifacts"] if a["target"] not in existing]}


def receipt(directory: Path, artifact: dict, state: str, identifier: str | None = None):
    value = {"target": artifact["target"], "outcome": state, "version_id": identifier,
             "project_id": artifact["metadata"]["project_id"], "version_number": artifact["metadata"]["version_number"],
             "jar_path": artifact["jar_path"], "sha256": artifact["sha256"], "sha512": artifact["sha512"]}
    write_json(directory / f"{artifact['target']}.json", value)


def publish(plan: dict, api: Api, responses: Path, root: Path = Path("."), summary: Path | None = None) -> dict:
    check = preflight(plan, api, root)
    write_json(responses / "preflight.json", check)
    identifiers = dict(check["existing"])
    for artifact in plan["artifacts"]:
        target = artifact["target"]
        if target in identifiers:
            receipt(responses, artifact, "already_verified", identifiers[target])
            continue
        # Re-read and hash immediately before the POST; no automatic POST retries.
        verify_local_plan(plan, root)
        receipt(responses, artifact, "upload_started_remote_outcome_unknown")
        remote = api.create_version(artifact["metadata"], root / artifact["jar_path"])
        identifier = project_id(remote.get("id", ""))
        receipt(responses, artifact, "accepted_pending_verification", identifier)
        verify_remote_version(artifact, remote)
        verified = api.get(f"/version/{identifier}")
        verify_remote_version(artifact, verified)
        identifiers[target] = identifier
        receipt(responses, artifact, "uploaded_and_verified", identifier)
        print(f"Verified {target}: https://modrinth.com/mod/echo-warrior/version/{identifier}")
    final = find_existing(plan, api.get(f"/project/{plan['project_id']}/version?include_changelog=true"))
    if final != identifiers or len(final) != 6:
        raise ValueError("Final remote verification did not confirm all six versions.")
    report = {"project_id": plan["project_id"], "version": plan["version"], "project_status": check["project_status"],
              "verified_versions": final, "uploaded": len(check["missing"]), "reused": len(check["existing"])}
    write_json(responses / "complete.json", report)
    if summary:
        with summary.open("a", encoding="utf-8") as stream:
            stream.write(f"\n## Modrinth {plan['version']}\n\nSix versions confirmed; {report['uploaded']} uploaded, {report['reused']} reused. Project status: {check['project_status']} (approval is separate).\n\n")
            for target, identifier in final.items():
                stream.write(f"- {target}: https://modrinth.com/mod/echo-warrior/version/{identifier}\n")
    return report


def check_token(api: Api) -> int:
    # Public routes may silently ignore an invalid token. A 200 alone proves nothing.
    route = "/user/YurisCat/projects"
    visible = api.get(route)
    public = api.get_public(route)
    private_ids = {p["id"] for p in visible} - {p["id"] for p in public}
    if not private_ids:
        raise ValueError("Cannot prove PROJECT_READ access: no private YurisCat project is visible with this token. Check the token/account/scopes, or use project preflight if there are no private projects.")
    print("Modrinth token and PROJECT_READ access verified through private project visibility; no uploads.")
    print("VERSION_CREATE is exercised only during an explicitly enabled release upload.")
    return len(private_ids)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("mode", choices=("prepare", "preflight", "upload", "check-token"))
    parser.add_argument("--source-manifest", type=Path, default=Path("build/curseforge/release-manifest.json"))
    parser.add_argument("--output-directory", type=Path, default=Path("build/modrinth"))
    parser.add_argument("--project-id", default=os.environ.get("MODRINTH_PROJECT_ID", ""))
    parser.add_argument("--allow-pending-localization", action="store_true")
    args = parser.parse_args()
    try:
        if args.mode == "check-token":
            check_token(Api(os.environ.get("MODRINTH_API_TOKEN", "")))
            return 0
        CF.validate_localization(args.allow_pending_localization)
        if args.mode == "prepare":
            plan = prepare_plan(args.source_manifest, args.output_directory, args.project_id)
            print(f"Prepared six Modrinth {plan['version']} metadata files; no network requests or uploads.")
            if not plan["project_id"]:
                print("Preview only: configure repository variable MODRINTH_PROJECT_ID before publishing.")
        else:
            plan = read_json(args.output_directory / "release-manifest.json")
            if args.project_id and args.project_id != plan["project_id"]:
                raise ValueError("Configured project ID differs from the prepared manifest.")
            api = Api(os.environ.get("MODRINTH_API_TOKEN", ""))
            if args.mode == "preflight":
                check = preflight(plan, api)
                write_json(args.output_directory / "responses/preflight.json", check)
                print(f"Modrinth preflight passed: {len(check['existing'])} existing, {len(check['missing'])} missing; no uploads.")
            else:
                summary = os.environ.get("GITHUB_STEP_SUMMARY")
                publish(plan, api, args.output_directory / "responses", summary=Path(summary) if summary else None)
                print("All six Modrinth versions accepted and verified. Project review/public visibility is separate.")
        return 0
    except (KeyError, TypeError, OSError, ValueError) as error:
        print(f"Modrinth release failed: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
