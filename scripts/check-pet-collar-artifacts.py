"""Distribution linkage/packaging guards; installed-mod tests cover actual behavior."""
from __future__ import annotations

import argparse
import json
from pathlib import Path
import struct
import tomllib
import zipfile

ROOT = Path(__file__).resolve().parents[1]


def class_references(data: bytes) -> list[str]:
    """Distinguish CONSTANT_Class links from safe reflective string literals."""
    assert data[:4] == b"\xca\xfe\xba\xbe"
    count = struct.unpack_from(">H", data, 8)[0]
    entries, references, offset, index = {}, [], 10, 1
    sizes = {3: 4, 4: 4, 5: 8, 6: 8, 8: 2, 9: 4, 10: 4, 11: 4,
             12: 4, 15: 3, 16: 2, 17: 4, 18: 4, 19: 2, 20: 2}
    while index < count:
        kind = data[offset]
        offset += 1
        if kind == 1:
            length = struct.unpack_from(">H", data, offset)[0]
            offset += 2
            entries[index] = data[offset:offset + length].decode("utf-8", errors="replace")
            offset += length
        elif kind == 7:
            references.append(struct.unpack_from(">H", data, offset)[0])
            offset += 2
        else:
            offset += sizes[kind]
            if kind in (5, 6):
                index += 1
        index += 1
    return [entries[index] for index in references]


def audit(path: Path, version: str, loader: str) -> None:
    suffix = {"26.1.2": "", "1.21.1": "_1211", "1.20.1": "_1201"}[version]
    number = suffix.removeprefix("_")
    config_name = f"echo_warrior_pet_collars{suffix}.mixins.json"
    with zipfile.ZipFile(path) as jar:
        names = set(jar.namelist())
        config = json.loads(jar.read(config_name))
        client_config = json.loads(jar.read(f"echo_warrior{suffix}.mixins.json"))
        feedback_mixin = f"PetFeedbackGuiMixin{number}"
        assert feedback_mixin in client_config["client"], "Missing client-only pet feedback hook"
        client_package = client_config["package"].replace(".", "/").rsplit("/", 1)[0]
        assert f"{client_package}/client/PetFeedbackLayout{number}.class" in names, "Missing wrapped feedback renderer"
        assert f'{client_config["package"].replace(".", "/")}/{feedback_mixin}.class' in names
        assert feedback_mixin not in client_config.get("mixins", []), "Client feedback must not load on servers"
        if version == "1.20.1" and loader == "forge":
            forge_client = "com/yuriscat/echowarrior/compat/forge/"
            assert forge_client + "PetFeedbackOverlay1201Forge.class" in names, "ForgeGui bypasses vanilla render"
            assert b"PetFeedbackOverlay1201Forge" in jar.read(forge_client + "EchoClient1201Forge.class"), "Forge feedback event is not wired"
        package = config["package"].replace(".", "/") + "/"
        plugin = config["plugin"].replace(".", "/") + ".class"
        assert plugin in names and not plugin.startswith(package), "Plugin in reserved Mixin package"
        assert config.get("injectors", {}).get("defaultRequire") == 1, "Changed hook targets must fail"
        assert config.get("mixins", []) == [], "Optional targets require bytecode selection"
        for name in names:
            if not name.endswith(".class") or "/integration/" not in name:
                continue
            payload = jar.read(name)
            assert not any(ref.startswith(("com/github/yzqdev/", "com/github/alexthe"))
                           for ref in class_references(payload)), f"Hard optional dependency: {name}"
            if name.startswith(package):
                assert b"Lorg/spongepowered/asm/mixin/Mixin;" in payload, f"Helper in Mixin package: {name}"
        base = plugin.rsplit("/", 1)[0] + "/"
        for name in ("PetCollarCompat", "PetCollarJointSelfTest", "PetCollarDiskSelfTest"):
            assert base + name + number + ".class" in names, f"Missing collar implementation: {name}"
        if loader == "fabric":
            metadata = json.loads(jar.read("fabric.mod.json"))
            configured = [v if isinstance(v, str) else v["config"] for v in metadata["mixins"]]
            assert config_name in configured, "Fabric does not load the collar config"
            assert not {"pet_home", "domesticationinnovation", "citadel"} & metadata["depends"].keys()
        elif loader == "neoforge":
            metadata = tomllib.loads(jar.read("META-INF/neoforge.mods.toml").decode())
            assert config_name in [entry["config"] for entry in metadata["mixins"]]
        else:
            manifest = jar.read("META-INF/MANIFEST.MF").decode().replace("\r\n ", "").replace("\n ", "")
            assert config_name in manifest, "Forge does not load the collar config"
        for locale in ("en_us", "zh_cn"):
            language = json.loads(jar.read(f"assets/echo_warrior/lang/{locale}.json"))
            for key in ("message.echo_warrior.pet_management_restricted",
                        "message.echo_warrior.pet_compass_unavailable",
                        "tooltip.echo_warrior.pet_collar_saved",
                        "tooltip.echo_warrior.pet_collar_limits"):
                assert language.get(key), f"Missing player feedback: {locale}/{key}"
        if version == "1.20.1":
            ores = json.loads(jar.read("data/c/tags/blocks/ores.json"))
            assert not ores.get("replace", False)
            assert {"id": "#forge:ores", "required": False} in ores["values"], "Missing conventional ore alias"
    print(f"PASS optional collar artifact: {path.name}")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--version", choices=("all", "26.1.2", "1.21.1", "1.20.1"), default="all")
    args = parser.parse_args()
    for version in (("26.1.2", "1.21.1", "1.20.1") if args.version == "all" else (args.version,)):
        root = ROOT if version == "26.1.2" else ROOT / "versions" / version
        props = dict(line.split("=", 1) for line in (root / "gradle.properties").read_text().splitlines()
                     if "=" in line and not line.startswith("#"))
        for loader in ("fabric", "forge" if version == "1.20.1" else "neoforge"):
            audit(root / loader / "build/libs" / f"echo-warrior-{loader}-{version}-{props['mod_version']}.jar",
                  version, loader)


if __name__ == "__main__":
    main()
