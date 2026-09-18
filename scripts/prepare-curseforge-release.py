#!/usr/bin/env python3
"""Prepare and validate the four CurseForge uploads for one Echo Warrior release."""

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
        "descriptor": "fabric.mod.json",
        "forbidden_descriptor": "META-INF/neoforge.mods.toml",
        "dependencies": (FABRIC_API_DEPENDENCY, *COMMON_REQUIRED_DEPENDENCIES),
    },
    "neoforge": {
        "display_name": "NeoForge",
        "game_version_name": "NeoForge",
        "descriptor": "META-INF/neoforge.mods.toml",
        "forbidden_descriptor": "fabric.mod.json",
        "dependencies": COMMON_REQUIRED_DEPENDENCIES,
    },
}
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
    forbidden_descriptor: str,
    loader_name: str,
    expected_version: str,
) -> None:
    if not jar_path.is_file():
        raise ValueError(f"Expected {loader_name} release JAR was not produced: {jar_path}")
    if jar_path.name.endswith("-sources.jar"):
        raise ValueError(f"Refusing source JAR as a {loader_name} release artifact: {jar_path}")

    try:
        with zipfile.ZipFile(jar_path) as archive:
            entries = set(archive.namelist())
            descriptor_text = archive.read(descriptor).decode("utf-8")
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
    missing_license_entries = REQUIRED_LICENSE_ENTRIES - entries
    if missing_license_entries:
        raise ValueError(
            f"{loader_name} JAR is missing required license/credit entries: "
            f"{', '.join(sorted(missing_license_entries))}"
        )
    if descriptor == "fabric.mod.json":
        descriptor_version = str(json.loads(descriptor_text).get("version", ""))
    else:
        version_match = re.search(
            r'(?m)^\s*version\s*=\s*"([^"]+)"\s*$',
            descriptor_text,
        )
        descriptor_version = version_match.group(1) if version_match else ""
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


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--properties", type=Path, default=Path("gradle.properties"))
    parser.add_argument(
        "--compat-properties",
        type=Path,
        default=Path("versions/1.21.1/gradle.properties"),
    )
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
        help="Fail unless all four exact version- and loader-specific JARs exist and validate.",
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
        release_lines = (
            ("main", Path("."), args.properties),
            ("compat_1211", Path("versions/1.21.1"), args.compat_properties),
        )
        parsed_lines: list[dict[str, str | Path]] = []
        for line_key, project_root, properties_path in release_lines:
            properties = read_gradle_properties(properties_path)
            parsed_lines.append(
                {
                    "key": line_key,
                    "project_root": project_root,
                    "version": require_property(properties, "mod_version"),
                    "minecraft_version": require_property(properties, "minecraft_version"),
                    "archive_name": require_property(properties, "archives_base_name"),
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
        for line in parsed_lines:
            line_key = str(line["key"])
            project_root = Path(line["project_root"])
            minecraft_version = str(line["minecraft_version"])
            archive_name = str(line["archive_name"])
            for loader_key, loader in LOADERS.items():
                target_key = f"{line_key}_{loader_key}"
                jar_name = f"{archive_name}-{loader_key}-{minecraft_version}-{version}.jar"
                jar_path = project_root / loader_key / "build" / "libs" / jar_name
                target_display_name = f"{loader['display_name']} {minecraft_version}"
                if args.require_jars:
                    validate_loader_jar(
                        jar_path,
                        str(loader["descriptor"]),
                        str(loader["forbidden_descriptor"]),
                        target_display_name,
                        version,
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

                outputs.update(
                    {
                        f"{target_key}_jar_path": jar_path.as_posix(),
                        f"{target_key}_jar_name": jar_path.name,
                        f"{target_key}_metadata_path": metadata_path.as_posix(),
                        f"{target_key}_display_name": display_name,
                    }
                )

        if args.github_output:
            append_github_output(args.github_output, outputs)

        print(f"Prepared CurseForge metadata for {PROJECT_NAME} {version}.")
        print(f"Release type: {args.release_type}")
        for line in parsed_lines:
            line_key = str(line["key"])
            for loader_key in LOADERS:
                target_key = f"{line_key}_{loader_key}"
                print(f"{target_key}: {outputs[f'{target_key}_jar_path']}")
                print(f"metadata: {outputs[f'{target_key}_metadata_path']}")
        return 0
    except (OSError, ValueError) as error:
        print(f"Release preparation failed: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
