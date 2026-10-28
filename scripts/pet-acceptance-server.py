"""Owned loopback server and optional latency proxy for real-client FOXY acceptance."""
from __future__ import annotations
import argparse
import asyncio
from datetime import datetime, timezone
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import threading
import time
import traceback
import uuid


def digest(path):
    with Path(path).open("rb") as stream:
        return hashlib.file_digest(stream, "sha256").hexdigest()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source-runtime", type=Path, required=True)
    parser.add_argument("--root", type=Path, required=True)
    parser.add_argument("--echo-jar", type=Path, required=True)
    parser.add_argument("--world", type=Path)
    parser.add_argument("--java", type=Path, required=True)
    parser.add_argument("--minecraft", choices=["1.20.1", "1.21.1", "26.1.2"], required=True)
    parser.add_argument("--loader", choices=["fabric", "neoforge", "forge"], required=True)
    parser.add_argument("--port", type=int, default=25670)
    parser.add_argument("--proxy-port", type=int, default=25671)
    args = parser.parse_args()
    if os.environ.get("COMPUTERNAME", "").upper() != "FOXY-NODE":
        raise RuntimeError("Real-client acceptance belongs on FOXY-NODE")
    allowed = Path(r"D:\Games-Terminal\EchoWarrior")
    root = args.root.resolve()
    source = args.source_runtime.resolve(strict=True)
    if not root.is_relative_to(allowed) or not source.is_relative_to(allowed) or root.exists():
        raise ValueError("Fresh isolated server root and existing test runtime required")
    if not 1024 <= args.port <= 65535 or not 1024 <= args.proxy_port <= 65535 or args.port == args.proxy_port:
        raise ValueError("Distinct high loopback ports required")
    lock_path = Path(r"D:\Workspace-Terminal\EchoWarrior\active-test.lock")
    lock = lock_path.open("x", encoding="utf-8")
    lock.write(json.dumps({"owner": "pet-real-client-server", "pid": os.getpid(), "root": str(root)}))
    lock.flush()
    process = None
    state = {"state": "preparing", "minecraft": args.minecraft, "loader": args.loader,
             "source_runtime": str(source), "server": f"127.0.0.1:{args.port}",
             "proxy": f"127.0.0.1:{args.proxy_port}", "pid": os.getpid()}
    try:
        ignored = shutil.ignore_patterns("*test-world", "battlefield-generation-test", "acceptance-world",
                                         "logs", "crash-reports", "*.log", "world", "session.lock")
        shutil.copytree(source, root, ignore=ignored)
        control = root / "acceptance-control"
        control.mkdir(exist_ok=False)
        for path in (root / "mods").glob("echo-warrior-*.jar"):
            path.unlink()  # Only files in this just-created isolated copy.
        shutil.copy2(args.echo_jar, root / "mods" / args.echo_jar.name)
        if args.world:
            shutil.copytree(args.world, root / "acceptance-world", ignore=shutil.ignore_patterns("session.lock"))
        (root / "eula.txt").write_text("eula=true\n")
        flat = json.dumps({"layers": [{"block": "minecraft:bedrock", "height": 1},
                                     {"block": "minecraft:dirt", "height": 2},
                                     {"block": "minecraft:grass_block", "height": 1}],
                           "biome": "minecraft:plains", "structures": {}})
        (root / "server.properties").write_text(
            f"server-ip=127.0.0.1\nserver-port={args.port}\nonline-mode=false\nenforce-secure-profile=false\n"
            "level-name=acceptance-world\nlevel-type=minecraft:flat\ngenerate-structures=false\n"
            f"generator-settings={flat}\nmax-players=4\nspawn-protection=0\nview-distance=5\nsimulation-distance=5\n"
            "gamemode=creative\ndifficulty=normal\nspawn-monsters=false\nallow-flight=true\nsync-chunk-writes=true\n",
            encoding="utf-8")
        names = ["EchoPetOwner", "EchoPetGuest", "Echo1211Fabric", "Echo1211Neo",
                 "EchoWarriorDev", "EchoNeoForge", "Echo1201Fabric", "Echo1201Forge"]
        ops = [{"name": name, "uuid": str(uuid.UUID(bytes=hashlib.md5(("OfflinePlayer:" + name).encode()).digest(), version=3)),
                "level": 4, "bypassesPlayerLimit": False} for name in names]
        (root / "ops.json").write_text(json.dumps(ops))
        state["artifacts"] = {p.name: digest(p) for p in (root / "mods").glob("*.jar")}
        state["world_source"] = str(args.world) if args.world else None
        (control / "proxy-delay.json").write_text('{"delay_ms":0}')
        java_args = [str(args.java), "-Xms512M", "-Xmx3072M", "-Dfile.encoding=UTF-8",
                     "-Dio.netty.transport.noNative=true"]
        if args.loader == "fabric":
            launchers = list(root.glob("fabric-server-*.jar"))
            if len(launchers) != 1:
                launchers = list(root.glob("fabric-server-launch.jar"))
            if len(launchers) != 1:
                raise ValueError("Expected one official Fabric server launcher")
            java_args += ["-jar", launchers[0].name, "nogui"]
        else:
            namespace = "neoforged" if args.loader == "neoforge" else "minecraftforge"
            args_files = list((root / f"libraries/net/{namespace}").rglob("win_args.txt"))
            if len(args_files) != 1:
                raise ValueError("Expected one official loader argument file")
            java_args += ["@" + str(args_files[0].relative_to(root)), "nogui"]
        state["command"] = java_args
        state["started_utc"] = datetime.now(timezone.utc).isoformat()
        def save_state():
            path = root / "acceptance-server-state.json"
            temporary = path.with_suffix(".tmp")
            temporary.write_text(json.dumps(state, indent=2), encoding="utf-8")
            temporary.replace(path)
        save_state()
        process = subprocess.Popen(java_args, cwd=root, stdin=subprocess.PIPE, stdout=subprocess.PIPE,
                                   stderr=subprocess.STDOUT, text=True, encoding="utf-8", errors="replace",
                                   creationflags=subprocess.CREATE_NO_WINDOW)
        state["java_pid"] = process.pid
        state["state"] = "starting"
        save_state()
        def read_console():
            with (root / "acceptance-console.log").open("w", encoding="utf-8") as output:
                for line in process.stdout:
                    output.write(line)
                    output.flush()
                    if "Done (" in line:
                        state["state"] = "ready"
                        save_state()
        reader = threading.Thread(target=read_console, daemon=True)
        reader.start()
        async def proxy():
            async def connection(client_reader, client_writer):
                server_reader, server_writer = await asyncio.open_connection("127.0.0.1", args.port)
                async def pump(origin, destination, direction):
                    try:
                        while data := await origin.read(65536):
                            delay = json.loads((control / "proxy-delay.json").read_text(encoding="utf-8-sig")).get("delay_ms", 0)
                            if not 0 <= delay <= 2000:
                                raise ValueError("Proxy delay outside acceptance bounds")
                            if delay:
                                await asyncio.sleep(delay / 1000)
                            destination.write(data)
                            await destination.drain()
                            with (control / "proxy-traffic.jsonl").open("a") as evidence:
                                evidence.write(json.dumps({"time_ms": int(time.time() * 1000),
                                                          "direction": direction, "bytes": len(data), "delay_ms": delay}) + "\n")
                    except (ConnectionError, OSError, asyncio.CancelledError):
                        pass
                    finally:
                        destination.close()
                await asyncio.gather(pump(client_reader, server_writer, "client-to-server"),
                                     pump(server_reader, client_writer, "server-to-client"))
            proxy_server = await asyncio.start_server(connection, "127.0.0.1", args.proxy_port)
            async with proxy_server:
                await proxy_server.serve_forever()
        proxy_thread = threading.Thread(target=lambda: asyncio.run(proxy()), daemon=True)
        proxy_thread.start()
        next_command = 1
        while process.poll() is None:
            request = control / f"{next_command:05d}.json"
            if request.is_file():
                data = json.loads(request.read_text(encoding="utf-8-sig"))
                commands = data["commands"]
                if not isinstance(commands, list) or any("\n" in command or "\r" in command for command in commands):
                    raise ValueError("One console command per list element required")
                process.stdin.write(f"say EW_BEGIN_{next_command:05d}\n")
                for command in commands:
                    process.stdin.write(command + "\n")
                if "stop" not in commands:
                    process.stdin.write(f"say EW_END_{next_command:05d}\n")
                process.stdin.flush()
                (control / f"{next_command:05d}-submitted.json").write_text(json.dumps({"commands": commands}))
                next_command += 1
            time.sleep(0.1)
        reader.join(timeout=10)
        state["exit_code"] = process.returncode
        state["state"] = "stopped" if process.returncode == 0 else "failed"
        state["normal_save"] = "All dimensions are saved" in (root / "acceptance-console.log").read_text(encoding="utf-8")
        state["finished_utc"] = datetime.now(timezone.utc).isoformat()
        save_state()
    finally:
        if process and process.poll() is None:
            process.stdin.write("stop\n")
            process.stdin.flush()
            try:
                process.wait(timeout=45)
            except subprocess.TimeoutExpired:
                process.terminate()
        lock.close()
        lock_path.unlink()


if __name__ == "__main__":
    try:
        main()
    except BaseException:
        if "--root" in sys.argv:
            failed_root = Path(sys.argv[sys.argv.index("--root") + 1]).resolve()
            if failed_root.is_relative_to(Path(r"D:\Games-Terminal\EchoWarrior")) and failed_root.is_dir():
                failure = traceback.format_exc()
                (failed_root / "acceptance-worker-error.log").write_text(failure, encoding="utf-8")
                state_path = failed_root / "acceptance-server-state.json"
                failed_state = json.loads(state_path.read_text()) if state_path.is_file() else {}
                failed_state.update(state="failed", worker_error=failure)
                state_path.write_text(json.dumps(failed_state, indent=2), encoding="utf-8")
        raise
