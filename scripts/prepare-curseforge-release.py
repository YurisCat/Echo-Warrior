#!/usr/bin/env python3
"""Prepare and validate the six CurseForge uploads for one Echo Warrior release."""

from __future__ import annotations

import argparse
import hashlib
import json
import re
import subprocess
import sys
import tomllib
import zipfile
from pathlib import Path


PROJECT_NAME = "Echo Warrior"
COMMON_REQUIRED_DEPENDENCIES = (
    {"slug": "smartbrainlib", "projectID": 661293, "type": "requiredDependency"},
    {"slug": "geckolib", "projectID": 388172, "type": "requiredDependency"},
)
FABRIC_API_DEPENDENCY = {
    "slug": "fabric-api",
    "projectID": 306612,
    "type": "requiredDependency",
}
LOADERS = {
    "fabric": {
        "display_name": "Fabric",
        "game_version_name": "Fabric",
        "descriptor": "fabric.mod.json",
        "dependencies": (FABRIC_API_DEPENDENCY, *COMMON_REQUIRED_DEPENDENCIES),
    },
    "neoforge": {
        "display_name": "NeoForge",
        "game_version_name": "NeoForge",
        "descriptor": "META-INF/neoforge.mods.toml",
        "dependencies": COMMON_REQUIRED_DEPENDENCIES,
    },
    "forge": {
        "display_name": "Forge",
        "game_version_name": "Forge",
        "descriptor": "META-INF/mods.toml",
        "dependencies": COMMON_REQUIRED_DEPENDENCIES,
    },
}
RELEASE_MATRIX = (
    ("main", ".", "properties", "26.1.2", ("fabric", "neoforge")),
    ("compat_1211", "versions/1.21.1", "compat_properties", "1.21.1", ("fabric", "neoforge")),
    ("compat_1201", "versions/1.20.1", "legacy_properties", "1.20.1", ("fabric", "forge")),
)
REQUIRED_LICENSE_ENTRIES = {
    "META-INF/LICENSE_ECHO_WARRIOR",
    "META-INF/LICENSE-CODE_ECHO_WARRIOR",
    "META-INF/LICENSE-ASSETS_ECHO_WARRIOR.md",
    "META-INF/NOTICE_ECHO_WARRIOR",
    "META-INF/CREDITS_ECHO_WARRIOR.md",
}


def read_gradle_properties(path: Path) -> dict[str, str]:
    properties: dict[str, str] = {}
    for raw_line in path.read_text(encoding="utf-8").splitlines():
        line = raw_line.strip()
        if not line or line.startswith(("#", "!")):
            continue

        separator = "=" if "=" in line else ":" if ":" in line else None
        if separator is None:
            continue

        key, value = line.split(separator, 1)
        properties[key.strip()] = value.strip()

    return properties


def require_property(properties: dict[str, str], key: str) -> str:
    value = properties.get(key, "").strip()
    if not value:
        raise ValueError(f"Missing required Gradle property: {key}")
    return value


def extract_release_changelog(path: Path, version: str) -> str:
    lines = path.read_text(encoding="utf-8").splitlines()
    release_heading = re.compile(rf"^##\s+{re.escape(version)}(?:\s+-\s+.+)?\s*$")

    start: int | None = None
    for index, line in enumerate(lines):
        if release_heading.match(line):
            start = index + 1
            break

    if start is None:
        raise ValueError(
            f"CHANGELOG section for {version} was not found. "
            f"Expected a heading such as '## {version} - YYYY-MM-DD'."
        )

    end = len(lines)
    for index in range(start, len(lines)):
        if lines[index].startswith("## "):
            end = index
            break

    changelog = "\n".join(lines[start:end]).strip()
    if not changelog:
        raise ValueError(f"CHANGELOG section for {version} is empty.")
    return changelog


def parse_boolean(value: str) -> bool:
    normalized = value.strip().lower()
    if normalized in {"1", "true", "yes", "on"}:
        return True
    if normalized in {"0", "false", "no", "off", ""}:
        return False
    raise ValueError(f"Invalid boolean value: {value}")


def validate_loader_jar(
    jar_path: Path,
    descriptor: str,
    loader_name: str,
    expected_version: str,
    expected_minecraft: str,
    language_directory: Path = Path("common/src/main/resources/assets/echo_warrior/lang"),
) -> None:
    if not jar_path.is_file():
        raise ValueError(f"Expected {loader_name} release JAR was not produced: {jar_path}")
    if jar_path.name.endswith("-sources.jar"):
        raise ValueError(f"Refusing source JAR as a {loader_name} release artifact: {jar_path}")

    try:
        with zipfile.ZipFile(jar_path) as archive:
            entries = set(archive.namelist())
            if descriptor not in entries:
                raise ValueError(f"{loader_name} descriptor '{descriptor}' is missing from {jar_path}.")
            descriptor_text = archive.read(descriptor).decode("utf-8")
            for source in language_directory.glob("*.json"):
                name = f"assets/echo_warrior/lang/{source.name}"
                if name not in entries or json.loads(archive.read(name)) != json.loads(source.read_text(encoding="utf-8")):
                    raise ValueError(f"{loader_name} packaged locale does not match reviewed source: {source.name}")
    except zipfile.BadZipFile as error:
        raise ValueError(f"Invalid {loader_name} JAR: {jar_path}") from error

    if descriptor not in entries:
        raise ValueError(
            f"{loader_name} descriptor '{descriptor}' is missing from {jar_path}."
        )
    foreign_descriptors = {str(loader["descriptor"]) for loader in LOADERS.values()} - {descriptor}
    if foreign_descriptors & entries:
        raise ValueError(
            f"{loader_name} JAR unexpectedly contains the other loader descriptor "
            f"'{', '.join(sorted(foreign_descriptors & entries))}': {jar_path}"
        )
    # Legacy build keeps original filenames; both layouts must carry the same full texts.
    license_entries = {
        name: name if name in entries else name.replace("_ECHO_WARRIOR", "")
        for name in REQUIRED_LICENSE_ENTRIES
    }
    missing_license_entries = {name for name, actual in license_entries.items() if actual not in entries}
    if missing_license_entries:
        raise ValueError(
            f"{loader_name} JAR is missing required license/credit entries: "
            f"{', '.join(sorted(missing_license_entries))}"
        )
    with zipfile.ZipFile(jar_path) as archive:
        for name, actual in license_entries.items():
            source = Path(name.removeprefix("META-INF/").replace("_ECHO_WARRIOR", ""))
            if archive.read(actual).decode("utf-8").replace("\r\n", "\n") != source.read_text(encoding="utf-8"):
                raise ValueError(f"{loader_name} license/credit text differs from source: {actual}")
    if descriptor == "fabric.mod.json":
        metadata = json.loads(descriptor_text)
        descriptor_version = str(metadata.get("version", ""))
        minecraft_range = metadata.get("depends", {}).get("minecraft", "")
        if metadata.get("id") != "echo_warrior":
            raise ValueError(f"Wrong mod ID in {jar_path}")
    else:
        metadata = tomllib.loads(descriptor_text)
        own_mods = [mod for mod in metadata.get("mods", []) if mod.get("modId") == "echo_warrior"]
        if len(own_mods) != 1:
            raise ValueError(f"Expected exactly one Echo Warrior mod in {jar_path}")
        descriptor_version = str(own_mods[0].get("version", ""))
        minecraft_dependencies = [dep for dep in metadata.get("dependencies", {}).get("echo_warrior", []) if dep.get("modId") == "minecraft"]
        minecraft_range = minecraft_dependencies[0].get("versionRange", "") if len(minecraft_dependencies) == 1 else ""
    if not re.search(rf"(?<![\d.]){re.escape(expected_minecraft)}(?![\d.])", str(minecraft_range)):
        raise ValueError(f"{loader_name} Minecraft dependency does not declare {expected_minecraft}: {minecraft_range}")
    if descriptor_version != expected_version:
        raise ValueError(
            f"{loader_name} descriptor version is '{descriptor_version}', "
            f"expected '{expected_version}': {jar_path}"
        )


def append_github_output(path: Path, values: dict[str, str]) -> None:
    with path.open("a", encoding="utf-8", newline="\n") as output:
        for key, value in values.items():
            if "\n" in value or "\r" in value:
                raise ValueError(f"GitHub output '{key}' cannot contain a newline.")
            output.write(f"{key}={value}\n")


def validate_localization(allow_pending: bool) -> None:
    command = [
        sys.executable,
        "scripts/check-localization.py",
        "--release-gate",
    ]
    if allow_pending:
        command.append("--allow-pending")
    result = subprocess.run(command, check=False)
    if result.returncode != 0:
        raise ValueError(
            "Localization release gate failed. Review the report above, update the "
            "translations, or obtain the author's explicit waiver and rerun with "
            "--allow-pending-localization."
        )


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--properties", type=Path, default=Path("gradle.properties"))
    parser.add_argument(
        "--compat-properties",
        type=Path,
        default=Path("versions/1.21.1/gradle.properties"),
    )
    parser.add_argument("--changelog", type=Path, default=Path("CHANGELOG.md"))
    parser.add_argument("--legacy-properties", type=Path, default=Path("versions/1.20.1/gradle.properties"))
    parser.add_argument(
        "--output-directory",
        type=Path,
        default=Path("build/curseforge"),
    )
    parser.add_argument(
        "--release-type",
        choices=("alpha", "beta", "release"),
        default="release",
    )
    parser.add_argument("--manual-release", default="false")
    parser.add_argument(
        "--expected-tag",
        default="",
        help="If set, it must exactly equal v<mod_version>.",
    )
    parser.add_argument(
        "--require-jars",
        action="store_true",
        help="Fail unless all six exact version- and loader-specific JARs exist and validate.",
    )
    parser.add_argument(
        "--allow-pending-localization",
        action="store_true",
        help=(
            "Explicit author waiver for missing or stale non-core localizations. "
            "Malformed files and placeholder errors still fail."
        ),
    )
    parser.add_argument(
        "--github-output",
        type=Path,
        help="Optional GitHub Actions GITHUB_OUTPUT path.",
    )
    return parser


def main() -> int:
    args = build_parser().parse_args()

    try:
        parsed_lines = []
        for line_key, project_root, argument, expected_minecraft, loaders in RELEASE_MATRIX:
            properties = read_gradle_properties(getattr(args, argument))
            if require_property(properties, "minecraft_version") != expected_minecraft:
                raise ValueError(f"{line_key} must target Minecraft {expected_minecraft}")
            parsed_lines.append(
                {
                    "key": line_key,
                    "project_root": project_root,
                    "version": require_property(properties, "mod_version"),
                    "minecraft_version": require_property(properties, "minecraft_version"),
                    "archive_name": require_property(properties, "archives_base_name"),
                    "loaders": loaders,
                }
            )

        versions = {str(line["version"]) for line in parsed_lines}
        if len(versions) != 1:
            version_summary = ", ".join(
                f"{line['key']}={line['version']}" for line in parsed_lines
            )
            raise ValueError(
                "Every Minecraft compatibility line must use the same release version: "
                f"{version_summary}"
            )
        archive_names = {str(line["archive_name"]) for line in parsed_lines}
        if len(archive_names) != 1:
            raise ValueError("Every compatibility line must use the same archives_base_name.")

        version = versions.pop()
        if not re.fullmatch(r"\d+\.\d+\.\d+", version):
            raise ValueError(f"Public release requires a plain x.y.z version, not '{version}'.")
        manual_release = parse_boolean(args.manual_release)
        expected_tag = args.expected_tag.strip()
        release_tag = f"v{version}"
        if expected_tag and expected_tag != release_tag:
            raise ValueError(
                f"Git tag '{expected_tag}' does not match mod_version {version}; "
                f"expected '{release_tag}'."
            )

        changelog = extract_release_changelog(args.changelog, version)
        validate_localization(args.allow_pending_localization)
        args.output_directory.mkdir(parents=True, exist_ok=True)

        outputs = {
            "version": version,
            "release_tag": release_tag,
        }
        artifacts = []
        for line in parsed_lines:
            line_key = str(line["key"])
            project_root = Path(line["project_root"])
            minecraft_version = str(line["minecraft_version"])
            archive_name = str(line["archive_name"])
            for loader_key in line["loaders"]:
                loader = LOADERS[loader_key]
                target_key = f"{line_key}_{loader_key}"
                jar_name = f"{archive_name}-{loader_key}-{minecraft_version}-{version}.jar"
                jar_path = project_root / loader_key / "build" / "libs" / jar_name
                target_display_name = f"{loader['display_name']} {minecraft_version}"
                if args.require_jars:
                    validate_loader_jar(
                        jar_path,
                        str(loader["descriptor"]),
                        target_display_name,
                        version,
                        minecraft_version,
                    )

                display_name = (
                    f"{PROJECT_NAME} {version} "
                    f"({loader['display_name']} {minecraft_version})"
                )
                metadata = {
                    "changelog": changelog,
                    "changelogType": "markdown",
                    "displayName": display_name,
                    "gameVersionNames": [
                        "Client",
                        "Server",
                        minecraft_version,
                        loader["game_version_name"],
                    ],
                    "releaseType": args.release_type,
                    "isMarkedForManualRelease": manual_release,
                    "relations": {"projects": list(loader["dependencies"])},
                }
                metadata_path = (
                    args.output_directory
                    / f"{minecraft_version}-{loader_key}-metadata.json"
                )
                metadata_path.write_text(
                    json.dumps(metadata, ensure_ascii=False, indent=2) + "\n",
                    encoding="utf-8",
                )
                artifacts.append({
                    "target": target_key,
                    "jar_path": jar_path.as_posix(),
                    "metadata_path": metadata_path.as_posix(),
                    "sha256": hashlib.sha256(jar_path.read_bytes()).hexdigest() if args.require_jars else None,
                })

                outputs.update(
                    {
                        f"{target_key}_jar_path": jar_path.as_posix(),
                        f"{target_key}_jar_name": jar_path.name,
                        f"{target_key}_metadata_path": metadata_path.as_posix(),
                        f"{target_key}_display_name": display_name,
                    }
                )

        manifest_path = args.output_directory / "release-manifest.json"
        manifest_path.write_text(json.dumps({"version": version, "artifacts": artifacts}, indent=2) + "\n", encoding="utf-8")
        outputs["manifest_path"] = manifest_path.as_posix()
        if args.github_output:
            append_github_output(args.github_output, outputs)

        print(f"Prepared CurseForge metadata for {PROJECT_NAME} {version}.")
        print(f"Release type: {args.release_type}")
        for line in parsed_lines:
            line_key = str(line["key"])
            for loader_key in line["loaders"]:
                target_key = f"{line_key}_{loader_key}"
                print(f"{target_key}: {outputs[f'{target_key}_jar_path']}")
                print(f"metadata: {outputs[f'{target_key}_metadata_path']}")
        return 0
    except (OSError, ValueError) as error:
        print(f"Release preparation failed: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
