"""Audit the two 26.3 production JARs against shared content and exact dependencies."""
from __future__ import annotations

import argparse
import importlib.util
import json
from pathlib import Path
import struct
import tomllib
import zipfile

ROOT = Path(__file__).resolve().parents[1]
LINE = ROOT / "versions/26.3"
spec = importlib.util.spec_from_file_location("release_audit", ROOT / "scripts/prepare-curseforge-release.py")
release = importlib.util.module_from_spec(spec)
spec.loader.exec_module(release)


def properties():
    return release.read_gradle_properties(LINE / "gradle.properties")


def equivalent(name, packaged, source):
    if name.endswith(".json"):
        return json.loads(packaged) == json.loads(source)
    if name.endswith((".cfg", ".classtweaker", ".vsh", ".fsh")):
        return packaged.decode("utf-8").splitlines() == source.decode("utf-8").splitlines()
    return packaged == source


def audit_jar(jar, loader, values):
    release.validate_loader_jar(jar, release.LOADERS[loader]["descriptor"], loader,
                               values["mod_version"], "26.3",
                               ROOT / "common/src/main/resources/assets/echo_warrior/lang")
    shared = ROOT / "common/src/main/resources"
    overrides = LINE / "common/src/main/resources"
    expected = {p.relative_to(shared).as_posix(): p for p in shared.rglob("*") if p.is_file()}
    expected.update({p.relative_to(overrides).as_posix(): p for p in overrides.rglob("*") if p.is_file()})
    with zipfile.ZipFile(jar) as archive:
        names = set(archive.namelist())
        for name, source in expected.items():
            # NeoForge consumes this rule while constructing its transformed game;
            # keeping the source rule in both JARs is harmless and auditable.
            if name not in names:
                raise ValueError(f"{loader}: missing shared resource {name}")
            if not equivalent(name, archive.read(name), source.read_bytes()):
                raise ValueError(f"{loader}: stale/different resource {name}")
        for hero in ("RomanLegionary", "AztecWarrior", "EgyptianArcher", "GuandaoWarrior", "JapaneseSamurai"):
            name = f"com/yuriscat/echowarrior/entity/{hero}EchoEntity.class"
            if name not in names:
                raise ValueError(f"{loader}: missing hero class {hero}")
        for name in names:
            if name.endswith(".class"):
                data = archive.read(name)
                if struct.unpack(">H", data[6:8])[0] != 69:
                    raise ValueError(f"{loader}: unexpected Java bytecode {name}")
                if b"org/lwjgl/glfw" in data:
                    raise ValueError(f"{loader}: obsolete GLFW reference {name}")
        for extension in ("vsh", "fsh"):
            shader = archive.read(f"assets/echo_warrior/shaders/core/samurai_afterimage.{extension}").decode()
            if "#moj_import" in shader or "layout(location =" not in shader:
                raise ValueError(f"{loader}: old shader syntax")
        tooltip = archive.read("com/yuriscat/echowarrior/mixin/GuiGraphicsExtractorMixin.class")
        if (b"setTooltipForNextFrame(Lnet/minecraft/client/gui/Font;Ljava/util/List;Ljava/util/Optional;IILnet/minecraft/resources/Identifier;Z)V"
                not in tooltip or
                b"(Lnet/minecraft/client/gui/Font;Ljava/util/List;IILnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipPositioner;Lnet/minecraft/resources/Identifier;Z)V"
                not in tooltip):
            raise ValueError(f"{loader}: old tooltip/Shadow signature")
        if loader == "fabric":
            data = json.loads(archive.read("fabric.mod.json"))
            required = {"minecraft": "~26.3", "java": ">=25",
                        "fabricloader": ">=" + values["loader_version"],
                        "fabric-api": ">=" + values["fabric_api_version"],
                        "geckolib": ">=" + values["geckolib_version"],
                        "smartbrainlib": ">=" + values["smartbrainlib_version"]}
            if data["depends"] != required:
                raise ValueError("Fabric dependency pins differ")
        else:
            data = tomllib.loads(archive.read("META-INF/neoforge.mods.toml").decode())
            dependencies = {d["modId"]: d for d in data["dependencies"]["echo_warrior"]}
            required = {"minecraft": values["minecraft_version_range"],
                        "neoforge": f"[{values['neoforge_version']},)",
                        "geckolib": f"[{values['geckolib_version']},)",
                        "smartbrainlib": f"[{values['smartbrainlib_version']},)"}
            if ({k: v["versionRange"] for k, v in dependencies.items()} != required
                    or any(d["type"] != "required" or d["side"] != "BOTH" for d in dependencies.values())):
                raise ValueError("NeoForge dependency pins differ")
    return {"loader": loader, "resources": len(expected), "jar": str(jar)}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--loader", choices=("fabric", "neoforge", "both"), default="both")
    args = parser.parse_args()
    values = properties()
    for module in ("common", "fabric", "neoforge"):
        upstream = {p.relative_to(ROOT / module / "src").as_posix()
                    for p in (ROOT / module / "src").rglob("*.java")}
        target = {p.relative_to(LINE / module / "src").as_posix()
                  for p in (LINE / module / "src").rglob("*.java")}
        missing = upstream - target
        if missing:
            raise ValueError(f"{module}: unaccounted mainline source omissions: {sorted(missing)}")
    loaders = ("fabric", "neoforge") if args.loader == "both" else (args.loader,)
    for loader in loaders:
        jar = LINE / loader / "build/libs" / f"echo-warrior-{loader}-26.3-{values['mod_version']}.jar"
        print(json.dumps(audit_jar(jar, loader, values), ensure_ascii=False))
    print("PASS 26.3 production artifacts; gameplay and client acceptance remain separate.")


if __name__ == "__main__":
    main()
