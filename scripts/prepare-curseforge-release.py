#!/usr/bin/env python3
"""Prepare and validate the two CurseForge uploads for one Echo Warrior release."""

from __future__ import annotations

import argparse
import json
import re
import sys
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
        "jar_directory": Path("fabric/build/libs"),
        "descriptor": "fabric.mod.json",
        "forbidden_descriptor": "META-INF/neoforge.mods.toml",
        "dependencies": (FABRIC_API_DEPENDENCY, *COMMON_REQUIRED_DEPENDENCIES),
    },
    "neoforge": {
        "display_name": "NeoForge",
        "game_version_name": "NeoForge",
        "jar_directory": Path("neoforge/build/libs"),
        "descriptor": "META-INF/neoforge.mods.toml",
        "forbidden_descriptor": "fabric.mod.json",
        "dependencies": COMMON_REQUIRED_DEPENDENCIES,
    },
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
    forbidden_descriptor: str,
    loader_name: str,
) -> None:
    if not jar_path.is_file():
        raise ValueError(f"Expected {loader_name} release JAR was not produced: {jar_path}")
    if jar_path.name.endswith("-sources.jar"):
        raise ValueError(f"Refusing source JAR as a {loader_name} release artifact: {jar_path}")

    try:
        with zipfile.ZipFile(jar_path) as archive:
            entries = set(archive.namelist())
    except zipfile.BadZipFile as error:
        raise ValueError(f"Invalid {loader_name} JAR: {jar_path}") from error

    if descriptor not in entries:
        raise ValueError(
            f"{loader_name} descriptor '{descriptor}' is missing from {jar_path}."
        )
    if forbidden_descriptor in entries:
        raise ValueError(
            f"{loader_name} JAR unexpectedly contains the other loader descriptor "
            f"'{forbidden_descriptor}': {jar_path}"
        )


def append_github_output(path: Path, values: dict[str, str]) -> None:
    with path.open("a", encoding="utf-8", newline="\n") as output:
        for key, value in values.items():
            if "\n" in value or "\r" in value:
                raise ValueError(f"GitHub output '{key}' cannot contain a newline.")
            output.write(f"{key}={value}\n")


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--properties", type=Path, default=Path("gradle.properties"))
    parser.add_argument("--changelog", type=Path, default=Path("CHANGELOG.md"))
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
        help="Fail unless both exact loader-specific release JARs exist and validate.",
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
        properties = read_gradle_properties(args.properties)
        version = require_property(properties, "mod_version")
        minecraft_version = require_property(properties, "minecraft_version")
        archive_name = require_property(properties, "archives_base_name")
        manual_release = parse_boolean(args.manual_release)

        expected_tag = args.expected_tag.strip()
        release_tag = f"v{version}"
        if expected_tag and expected_tag != release_tag:
            raise ValueError(
                f"Git tag '{expected_tag}' does not match mod_version {version}; "
                f"expected '{release_tag}'."
            )

        changelog = extract_release_changelog(args.changelog, version)
        args.output_directory.mkdir(parents=True, exist_ok=True)

        outputs = {
            "version": version,
            "release_tag": release_tag,
        }
        for loader_key, loader in LOADERS.items():
            jar_name = f"{archive_name}-{loader_key}-{minecraft_version}-{version}.jar"
            jar_path = loader["jar_directory"] / jar_name
            if args.require_jars:
                validate_loader_jar(
                    jar_path,
                    str(loader["descriptor"]),
                    str(loader["forbidden_descriptor"]),
                    str(loader["display_name"]),
                )

            display_name = f"{PROJECT_NAME} {version} ({loader['display_name']})"
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
            metadata_path = args.output_directory / f"{loader_key}-metadata.json"
            metadata_path.write_text(
                json.dumps(metadata, ensure_ascii=False, indent=2) + "\n",
                encoding="utf-8",
            )

            outputs.update(
                {
                    f"{loader_key}_jar_path": jar_path.as_posix(),
                    f"{loader_key}_jar_name": jar_path.name,
                    f"{loader_key}_metadata_path": metadata_path.as_posix(),
                    f"{loader_key}_display_name": display_name,
                }
            )

        if args.github_output:
            append_github_output(args.github_output, outputs)

        print(f"Prepared CurseForge metadata for {PROJECT_NAME} {version}.")
        print(f"Minecraft: {minecraft_version}; release type: {args.release_type}")
        for loader_key in LOADERS:
            print(f"{loader_key}: {outputs[f'{loader_key}_jar_path']}")
            print(f"metadata: {outputs[f'{loader_key}_metadata_path']}")
        return 0
    except (OSError, ValueError) as error:
        print(f"Release preparation failed: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
