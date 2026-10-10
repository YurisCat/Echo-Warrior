"""Small, offline API tests using real TBF JARs as bytecode fixtures (no Minecraft process)."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import subprocess

ROOT = Path(__file__).resolve().parents[1]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--jar", action="append", required=True, type=Path)
    parser.add_argument("--java-home", type=Path, default=ROOT / ".toolchains/jdk-25")
    parser.add_argument("--gradle-cache", type=Path, default=Path.home() / ".gradle/caches/modules-2/files-2.1")
    args = parser.parse_args()
    dependencies = [
        next((args.gradle_cache / f"org.ow2.asm/{name}").glob(f"9.5/*/{name}-9.5.jar"))
        for name in ("asm", "asm-tree")
    ]
    output = ROOT / "build/tbf-contract-tests"
    output.mkdir(parents=True, exist_ok=True)
    classes = output / "classes"
    classes.mkdir(exist_ok=True)
    source = [
        ROOT / f"versions/{mc}/common/src/main/java/com/yuriscat/echowarrior/compat/integration/TbfCompatibility{suffix}.java"
        for mc, suffix in (("1.20.1", "1201"), ("1.21.1", "1211"))
    ]
    source.append(ROOT / "scripts/tests/TbfCompatibilityContractTest.java")
    executable = ".exe" if os.name == "nt" else ""
    classpath = os.pathsep.join(map(str, dependencies))
    subprocess.run([str(args.java_home / f"bin/javac{executable}"), "-proc:none", "--release", "17",
                    "-cp", classpath, "-d", str(classes), *map(str, source)], check=True)
    result = subprocess.run([str(args.java_home / f"bin/java{executable}"), "-cp",
                             str(classes) + os.pathsep + classpath, "TbfCompatibilityContractTest",
                             *map(str, args.jar)], capture_output=True, text=True, encoding="utf-8")
    (output / "output.txt").write_text(result.stdout + result.stderr, encoding="utf-8")
    print(result.stdout, end="")
    print(result.stderr, end="")
    result.check_returncode()
    report = {"passed": True, "jars": [
        {"path": str(path.resolve()), "sha256": hashlib.sha256(path.read_bytes()).hexdigest()}
        for path in args.jar
    ]}
    (output / "result.json").write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")


if __name__ == "__main__":
    main()
