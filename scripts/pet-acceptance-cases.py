"""Run concrete game actions against the opt-in clients and owned console queues."""
import argparse
import hashlib
import json
from pathlib import Path
import time
import traceback
import uuid
import os


def atomic(path, value):
    temporary = path.with_suffix(".tmp")
    temporary.write_text(json.dumps(value, ensure_ascii=False), encoding="utf-8")
    temporary.replace(path)


def next_sequence(directory):
    return max([int(p.stem) for p in directory.glob("*.json") if p.stem.isdigit()] or [0]) + 1


class Case:
    def __init__(self, root, prefix, phase, attempt=1):
        self.root, self.prefix = root, prefix
        self.server_root = root / (prefix + "-server")
        self.console = self.server_root / "acceptance-console.log"
        self.control = self.server_root / "acceptance-control"
        self.phase = phase
        self.evidence = root / (prefix + "-" + phase + f"-evidence-{attempt}")
        self.evidence.mkdir(exist_ok=False)
        self.result = {"state": "running", "phase": phase, "steps": [],
                       "controller_sha256": hashlib.sha256(Path(__file__).read_bytes()).hexdigest()}
        self.save()

    def save(self):
        atomic(self.evidence / "result.json", self.result)

    def record(self, label, **values):
        self.result["steps"].append({"label": label, "time_ms": int(time.time()*1000), **values})
        self.save()
        print(label, flush=True)

    def client(self, role, action, timeout=30):
        mapping = self.root / (self.prefix + "-clients.json")
        directory = json.loads(mapping.read_text(encoding="utf-8-sig"))[role] if mapping.is_file() else self.prefix + "-" + role
        instance = (self.root / directory).resolve()
        if not instance.is_relative_to(self.root.resolve()):
            raise ValueError("Client queue must stay inside the isolated acceptance root")
        queue = instance / "acceptance-queue"
        sequence = next_sequence(queue)
        atomic(queue / f"{sequence:05d}.json", action)
        output = queue / f"{sequence:05d}-result.json"
        deadline = time.time() + timeout
        while not output.is_file():
            if time.time() > deadline:
                raise RuntimeError(f"Client {role} action timed out: {action}")
            time.sleep(.1)
        result = json.loads(output.read_text(encoding="utf-8"))
        (self.evidence / f"{role}-{sequence:05d}.json").write_text(json.dumps(result, indent=2))
        if not result["passed"]:
            raise RuntimeError(f"Client {role}: {result}")
        return result

    def snapshot(self, role="owner"):
        return self.client(role, {"action": "snapshot"})

    def wait(self, predicate, role="owner", seconds=20):
        deadline = time.time() + seconds
        last = None
        while time.time() < deadline:
            last = self.snapshot(role)
            if predicate(last):
                return last
            time.sleep(.3)
        raise AssertionError(f"Client state timeout for {role}: {last}")

    def server(self, commands, timeout=15):
        sequence = next_sequence(self.control)
        before = self.console.read_text(encoding="utf-8")
        atomic(self.control / f"{sequence:05d}.json", {"commands": commands})
        marker = f"EW_END_{sequence:05d}"
        deadline = time.time() + timeout
        while time.time() < deadline:
            text = self.console.read_text(encoding="utf-8")
            if marker in text[len(before):] or ("stop" in commands and "All dimensions are saved" in text[len(before):]):
                output = text[len(before):]
                (self.evidence / f"server-{sequence:05d}.log").write_text(output, encoding="utf-8")
                return output
            time.sleep(.1)
        raise RuntimeError(f"Console command timeout: {commands}")

    @staticmethod
    def heroes(snapshot, kind=None):
        return [e for e in snapshot.get("entities", []) if e["type"].startswith("echo_warrior:")
                and (kind is None or e["type"] == "echo_warrior:" + kind + "_echo")]

    def menu(self, role="owner"):
        self.client(role, {"action": "select", "slot": 0})
        self.client(role, {"action": "use"})
        return self.wait(lambda state: "SummonerMenu" in state.get("menu", ""), role)

    def button(self, button=0, role="owner"):
        # Record the server's reply, rather than equating sending a packet with
        # acceptance. Only the explicit stale-state response permits a retry.
        for attempt in range(3):
            before = self.snapshot(role)
            if "menu_feedback" not in before:
                raise AssertionError("Current acceptance client must expose server menu feedback")
            self.client(role, {"action": "button", "id": button})
            after = self.wait(lambda s: s.get("menu_feedback") != before["menu_feedback"], role)
            code = after.get("menu_feedback", 0) & 255
            if code != 14:
                assert code in (1, 2, 8, 9), (button, code, after)
                return after
            self.record("server explicitly rejected stale menu action; refreshed retry", role=role,
                        button=button, attempt=attempt + 1, reply=after["menu_feedback"])
            time.sleep(.25)
        raise AssertionError("Menu remained stale after three acknowledged attempts")

    def close(self, role="owner"):
        return self.client(role, {"action": "close"})

    def one_hero(self, role="owner", maximum=None):
        state = self.wait(lambda s: len(self.heroes(s)) == 1 and
                          (maximum is None or self.heroes(s)[0]["max_health"] == maximum), role)
        return self.heroes(state)[0]

    def assert_one_server_hero(self):
        output = self.server(["scoreboard players set echoes ew_pet 0",
                              "execute as @e[type=echo_warrior:roman_legionary_echo] run scoreboard players add echoes ew_pet 1",
                              "execute if score echoes ew_pet matches 1 run say EW_EXACTLY_ONE_HERO"])
        assert "EW_EXACTLY_ONE_HERO" in output, output

    def assert_collar_count(self, count, stored=1):
        time.sleep(.8)
        selector = '@e[type=minecraft:item,nbt={Item:{id:"pet_home:collar_tag"}}]'
        output = self.server([
            "execute store result score own ew_pet run clear EchoPetOwner pet_home:collar_tag 0",
            "execute store result score guest ew_pet run clear EchoPetGuest pet_home:collar_tag 0",
            "scoreboard players set dropped ew_pet 0",
            f"execute as {selector} store result score @s ew_pet run data get entity @s Item.count",
            f"execute as {selector} run scoreboard players operation dropped ew_pet += @s ew_pet",
            "scoreboard players operation total ew_pet = own ew_pet",
            "scoreboard players operation total ew_pet += guest ew_pet",
            "scoreboard players operation total ew_pet += dropped ew_pet",
            f"scoreboard players add total ew_pet {stored}",
            f"execute if score total ew_pet matches {count} run say EW_COLLAR_TOTAL_{count}",
        ])
        assert f"EW_COLLAR_TOTAL_{count}" in output, output

    def upgrade(self):
        legacy = json.loads((self.root / "legacy-1211-legacy-evidence-2/result.json").read_text())
        old = next(step for step in legacy["steps"] if step["label"] == "real collar interaction on released server")
        maximum = old["equipped_max"]
        self.wait(lambda s: s.get("connected") and s.get("player") == "EchoPetOwner", seconds=90)
        guest = self.wait(lambda s: s.get("connected") and s.get("player") == "EchoPetGuest", "guest", seconds=90)
        hero = self.one_hero(maximum=maximum)
        self.one_hero("guest", maximum)
        output = self.server(["scoreboard objectives add ew_pet dummy", f'data get entity {hero["uuid"]}',
                              "data get entity EchoPetOwner Inventory"])
        assert "health_boost" in output
        self.assert_one_server_hero()
        self.assert_collar_count(1)
        self.record("released world loaded with collar visible to both clients", hero=hero, legacy_max=maximum)
        for index in range(3):
            previous = hero["uuid"]
            self.menu()
            self.button()
            self.wait(lambda s: not self.heroes(s))
            self.button()
            hero = self.one_hero(maximum=maximum)
            assert hero["uuid"] != previous
            self.close()
            self.one_hero("guest", maximum)
            self.assert_one_server_hero()
            self.assert_collar_count(1)
            self.record("real menu dismissal/resummon after upgrade", cycle=index+1, hero=hero)
        output = self.server(["data get entity EchoPetOwner Inventory"])
        assert "EchoWarriorPetCollars" in output and "health_boost" in output
        self.record("upgraded relic stores the original collar", server_snapshot=output)
        self.server([f'kill {hero["uuid"]}'])
        self.wait(lambda s: not self.heroes(s))
        self.menu()
        self.button()
        hero = self.one_hero(maximum=maximum)
        self.close()
        self.assert_collar_count(1)
        self.record("actual death and paid resummon preserve collar", hero=hero)

        # Drop the one physical summoner through the actual vanilla inventory packet.
        self.client("owner", {"action": "select", "slot": 0})
        self.client("owner", {"action": "slot", "slot": 36, "button": 1, "type": "THROW"})
        self.wait(lambda s: s["inventory"][0].startswith("0 "))
        self.server([
            "tp EchoPetOwner 32.5 64 40.5",
            'execute as @e[type=minecraft:item,nbt={Item:{id:"echo_warrior:test_echo_summoner"}}] run data get entity @s Pos',
            'tp EchoPetGuest @e[type=minecraft:item,nbt={Item:{id:"echo_warrior:test_echo_summoner"}},limit=1]',
        ])
        # A freshly thrown item is still moving. Reposition after it lands, rather
        # than treating its mid-flight coordinates as the final pickup location.
        time.sleep(1)
        self.server(['tp EchoPetGuest @e[type=minecraft:item,nbt={Item:{id:"echo_warrior:test_echo_summoner"}},limit=1]'])
        self.wait(lambda s: "echo_warrior:test_echo_summoner" in s["inventory"][0], "guest")
        self.menu("guest")
        self.button(10, "guest")
        self.close("guest")
        hero = self.one_hero("guest", maximum)
        integers = [int.from_bytes(uuid.UUID(guest["uuid"]).bytes[i:i+4], "big", signed=True) for i in range(0,16,4)]
        output = self.server([f'data get entity {hero["uuid"]} Owner'])
        assert all(str(value) in output for value in integers), output
        self.assert_one_server_hero()
        self.assert_collar_count(1)
        self.record("physical drop/pickup and native follow transfer preserve collar", hero=hero, controller=guest["uuid"])

        # Delay every TCP chunk in both directions; real clients keep independent state.
        atomic(self.control / "proxy-delay.json", {"delay_ms": 150})
        self.menu("guest")
        self.button(0, "guest")
        self.wait(lambda s: not self.heroes(s), "guest")
        self.button(0, "guest")
        hero = self.one_hero("guest", maximum)
        self.close("guest")
        self.wait(lambda s: any(e["uuid"] == hero["uuid"] for e in self.heroes(s)), "owner")
        self.assert_one_server_hero()
        self.assert_collar_count(1)
        self.record("150 ms each-direction proxy preserves dual-client reconstruction", hero=hero)

        before = self.server([f'execute store result score speed_before ew_pet run attribute {hero["uuid"]} minecraft:generic.movement_speed get 10000'])
        self.server(['item replace entity EchoPetGuest hotbar.1 with pet_home:collar_tag[minecraft:enchantments={levels:{"pet_home:speedster":2}}]'])
        self.client("guest", {"action":"select","slot":1})
        self.client("guest", {"action":"interact","uuid":hero["uuid"]})
        hero = self.one_hero("guest", old["base_max"])
        output = self.server([
            f'execute store result score speed_after ew_pet run attribute {hero["uuid"]} minecraft:generic.movement_speed get 10000',
            "execute if score speed_after ew_pet > speed_before ew_pet run say EW_SPEED_INCREASED",
        ])
        assert "EW_SPEED_INCREASED" in output, before + output
        self.assert_collar_count(2)
        self.menu("guest"); self.button(0,"guest")
        self.wait(lambda s: not self.heroes(s),"guest")
        self.button(0,"guest"); hero=self.one_hero("guest", old["base_max"]); self.close("guest")
        output=self.server([
            f'execute store result score speed_restored ew_pet run attribute {hero["uuid"]} minecraft:generic.movement_speed get 10000',
            "execute if score speed_restored ew_pet = speed_after ew_pet run say EW_SPEED_RESTORED_EXACTLY",
        ])
        assert "EW_SPEED_RESTORED_EXACTLY" in output,output
        self.assert_collar_count(2)
        self.record("actual replacement returns exactly one collar and restores speed without health stacking",hero=hero)
        self.server(['item replace entity EchoPetGuest hotbar.2 with pet_home:net'])
        self.client("guest",{"action":"select","slot":2})
        self.client("guest",{"action":"interact","uuid":hero["uuid"]})
        time.sleep(1)
        state=self.snapshot("guest")
        assert any(e["uuid"]==hero["uuid"] for e in self.heroes(state))
        assert state["inventory"][2].startswith("1 pet_home:net")
        self.record("real capture-ball packet cannot remove the bound hero",hero_uuid=hero["uuid"])
        self.server(['item replace entity EchoPetGuest hotbar.3 with pet_home:deed_of_ownership'])
        self.client("guest",{"action":"select","slot":3})
        self.client("guest",{"action":"interact","uuid":hero["uuid"]})
        output=self.server([f'data get entity {hero["uuid"]} Owner'])
        assert all(str(value) in output for value in integers),output
        self.record("real ownership-deed packet cannot change the bound controller")
        atomic(self.control / "proxy-delay.json", {"delay_ms":0})
        self.server(["save-all flush"])
        self.record("upgrade and multiplayer phase saved; clients retained for combat acceptance")

    def legacy(self):
        owner = self.wait(lambda s: s.get("connected") and s.get("player") == "EchoPetOwner"
                          and not s.get("screen", "").endswith("DeathScreen"), seconds=90)
        guest = self.wait(lambda s: s.get("connected") and s.get("player") == "EchoPetGuest"
                          and not s.get("screen", "").endswith("DeathScreen"), "guest", seconds=90)
        assert owner["uuid"] != guest["uuid"]
        self.record("two real clients connected to released server", owner_uuid=owner["uuid"], guest_uuid=guest["uuid"])
        self.server(["gamerule doMobSpawning false", "time set day", "weather clear",
                     "forceload add 16 16 64 64"])
        time.sleep(1)
        output = self.server([
            "fill 24 63 24 56 63 56 minecraft:stone",
            "fill 24 64 24 56 78 56 minecraft:air",
            "tp EchoPetOwner 40.5 64 40.5 0 0",
            "tp EchoPetGuest 43.5 64 40.5 0 0",
            "clear EchoPetOwner", "clear EchoPetGuest",
            "effect give EchoPetOwner minecraft:resistance infinite 4 true",
            "effect give EchoPetGuest minecraft:resistance infinite 4 true",
            "gamemode survival EchoPetOwner", "gamemode survival EchoPetGuest",
            'item replace entity EchoPetOwner hotbar.0 with echo_warrior:test_echo_summoner[minecraft:container=[{slot:7,item:{id:"echo_warrior:roman_legionary_relic",count:1}}],minecraft:custom_data={EchoWarriorFuel:1000}]',
        ])
        assert "Unknown item" not in output and "Invalid" not in output
        self.menu()
        self.button()
        self.close()
        state = self.wait(lambda s: len(self.heroes(s, "roman_legionary")) == 1)
        hero = self.heroes(state, "roman_legionary")[0]
        self.wait(lambda s: any(e["uuid"] == hero["uuid"] for e in self.heroes(s)), "guest")
        self.record("native summon packet visible on both clients", hero=hero)
        output = self.server([
            'item replace entity EchoPetOwner hotbar.1 with pet_home:collar_tag[minecraft:enchantments={levels:{"pet_home:health_boost":2}}]',
            f'data get entity {hero["uuid"]}',
        ])
        assert "Unknown item" not in output and "Invalid" not in output
        self.client("owner", {"action": "select", "slot": 1})
        self.client("owner", {"action": "interact", "uuid": hero["uuid"]})
        state = self.wait(lambda s: any(e["uuid"] == hero["uuid"] and e.get("max_health") == hero["max_health"] + 20
                                       for e in self.heroes(s)))
        self.wait(lambda s: any(e["uuid"] == hero["uuid"] and e.get("max_health") == hero["max_health"] + 20
                               for e in self.heroes(s)), "guest")
        snapshot = self.server([f'data get entity {hero["uuid"]}', "data get entity EchoPetOwner Inventory"])
        assert "health_boost" in snapshot
        self.record("real collar interaction on released server", hero_uuid=hero["uuid"],
                    base_max=hero["max_health"], equipped_max=hero["max_health"] + 20, server_snapshot=snapshot)
        self.server(["save-all flush"])
        self.record("legacy collar fixture saved while active")
        self.client("owner", {"action": "stop"})
        self.client("guest", {"action": "stop"})
        time.sleep(3)
        self.server(["forceload remove all", "stop"], timeout=45)
        deadline = time.time() + 15
        while time.time() < deadline:
            saved = json.loads((self.server_root / "acceptance-server-state.json").read_text())
            if saved["state"] == "stopped":
                break
            time.sleep(.2)
        assert saved["state"] == "stopped" and saved["normal_save"]
        world = self.server_root / "acceptance-world"
        hashes = {p.relative_to(world).as_posix(): hashlib.sha256(p.read_bytes()).hexdigest()
                  for p in world.rglob("*") if p.is_file() and p.name != "session.lock"}
        atomic(self.evidence / "world-sha256.json", hashes)
        self.record("released-version world normally saved and preserved", files=len(hashes),
                    source_mods=saved["artifacts"])

    def finish(self):
        self.server(["tick unfreeze", "save-all flush"])
        self.client("owner", {"action":"stop"})
        self.client("guest", {"action":"stop"})
        time.sleep(2)
        self.server(["stop"], timeout=45)
        deadline=time.time()+15
        while time.time()<deadline:
            state=json.loads((self.server_root/"acceptance-server-state.json").read_text())
            if state["state"]=="stopped":
                break
            time.sleep(.2)
        assert state["state"]=="stopped" and state["normal_save"],state
        self.record("clients stopped and server normally saved", artifacts=state["artifacts"])

    def dimensions(self):
        self.server(["tick unfreeze"])
        if "minecraft:overworld]" not in self.snapshot("guest").get("dimension", ""):
            self.server(["execute in minecraft:overworld run tp EchoPetGuest 40.5 64 40.5"])
            self.wait(lambda s: "minecraft:overworld]" in s.get("dimension", ""), "guest")
        hero = self.one_hero("guest")
        self.menu("guest")
        initial_fuel = self.snapshot("guest")["menu_fuel"]
        self.close("guest")
        for dimension in ("the_nether", "overworld"):
            self.server([
                f"execute in minecraft:{dimension} run forceload add 32 32 48 48",
                f"execute in minecraft:{dimension} run fill 32 63 32 48 63 48 minecraft:stone",
                f"execute in minecraft:{dimension} run fill 32 64 32 48 72 48 minecraft:air",
                f"execute in minecraft:{dimension} run tp EchoPetGuest 40.5 64 40.5",
            ])
            state = self.wait(lambda s: f"minecraft:{dimension}]" in s.get("dimension", ""), "guest")
            hero = self.one_hero("guest")
            self.menu("guest")
            assert self.snapshot("guest")["menu_fuel"] == initial_fuel
            self.close("guest")
            out = self.server([
                f"execute in minecraft:{dimension} run data get entity {hero['uuid']}",
                "scoreboard players set echoes ew_pet 0",
                *[f"execute in minecraft:{d} as @e[type=echo_warrior:roman_legionary_echo,distance=0..] run scoreboard players add echoes ew_pet 1"
                  for d in ("overworld", "the_nether", "the_end")],
                "execute if score echoes ew_pet matches 1 run say EW_ONE_ACROSS_DIMENSIONS",
            ])
            assert "speedster" in out and "EW_ONE_ACROSS_DIMENSIONS" in out
            self.record("real controller changes dimension; collar and fuel preserved", dimension=dimension, hero=hero)
        self.server(["save-all flush"])

    def disconnect(self):
        self.server(["tick unfreeze"])
        self.menu("guest")
        self.record("before disconnect", snapshot=self.snapshot("guest"), hero=self.one_hero("guest"))
        self.close("guest")
        self.client("guest", {"action": "disconnect"})
        self.wait(lambda s: not s.get("connected"), "guest")
        self.client("guest", {"action": "stop"})
        self.record("real guest socket disconnected and client stopped")

    def reconnected(self):
        previous = json.loads((self.root / (self.prefix + "-disconnect-evidence-1/result.json")).read_text())
        before = next(s for s in previous["steps"] if s["label"] == "before disconnect")
        self.wait(lambda s: s.get("connected") and s.get("player") == "EchoPetGuest", "guest", seconds=90)
        hero = self.one_hero("guest")
        self.menu("guest")
        after = self.snapshot("guest")
        assert after["uuid"] == before["snapshot"]["uuid"]
        assert after["menu_fuel"] == before["snapshot"]["menu_fuel"]
        self.close("guest")
        self.assert_one_server_hero()
        self.assert_collar_count(2)
        output = self.server([f"data get entity {hero['uuid']}"])
        assert "speedster" in output
        self.record("same real player reconnects without collar duplication or fuel loss", hero=hero)

    def concurrent(self):
        self.server(["tick unfreeze",
                     "item replace entity EchoPetOwner hotbar.0 from entity EchoPetGuest hotbar.0"])
        self.menu("owner"); self.menu("guest")
        before = self.snapshot("guest")["menu_fuel"]
        atomic(self.control / "proxy-delay.json", {"delay_ms": 150})
        at = int(time.time() * 1000) + 1000
        pending = []
        mapping_path = self.root / (self.prefix + "-clients.json")
        mapping = json.loads(mapping_path.read_text(encoding="utf-8-sig")) if mapping_path.is_file() else {}
        for role in ("owner", "guest"):
            queue = self.root / mapping.get(role, self.prefix + "-" + role) / "acceptance-queue"
            seq = next_sequence(queue)
            atomic(queue / f"{seq:05d}.json", {"action": "button", "id": 10, "at_ms": at})
            pending.append(queue / f"{seq:05d}-result.json")
        deadline = time.time() + 20
        while not all(p.is_file() for p in pending):
            if time.time() > deadline:
                raise AssertionError("Simultaneous client actions did not execute")
            time.sleep(.1)
        replies = [json.loads(p.read_text()) for p in pending]
        assert all(r["passed"] for r in replies)
        time.sleep(2)
        self.assert_one_server_hero()
        self.assert_collar_count(2)
        assert self.snapshot("guest")["menu_fuel"] == before
        hero = self.one_hero("guest")
        self.record("two real clients concurrently request FOLLOW on duplicate identity mirrors",
                    sent_at=at, actual_client_times=[r["time_ms"] for r in replies],
                    hero=hero, owner=self.snapshot("owner"), guest=self.snapshot("guest"))
        self.button(10, "guest")
        self.close("owner"); self.close("guest")
        atomic(self.control / "proxy-delay.json", {"delay_ms": 0})
        self.server(["item replace entity EchoPetOwner hotbar.0 with minecraft:air", "save-all flush"])
        self.assert_one_server_hero()
        self.assert_collar_count(2)

    def ordinary(self):
        guest = self.snapshot("guest")
        owner = [int.from_bytes(uuid.UUID(guest["uuid"]).bytes[i:i+4], "big", signed=True) for i in range(0, 16, 4)]
        owner_nbt = "[I;" + ",".join(map(str, owner)) + "]"
        self.server([
            "kill @e[tag=ew_ordinary]",
            f"summon minecraft:wolf 42 64 40.5 {{Owner:{owner_nbt},Sitting:1b,Tags:[\"ew_ordinary\"]}}",
            "tp EchoPetGuest 40.5 64 40.5",
            'item replace entity EchoPetGuest hotbar.1 with pet_home:collar_tag[minecraft:enchantments={levels:{"pet_home:health_boost":2}}]',
        ])
        state = self.wait(lambda s: any(e["type"] == "minecraft:wolf" for e in s["entities"]), "guest")
        wolf = next(e for e in state["entities"] if e["type"] == "minecraft:wolf")
        self.client("guest", {"action": "select", "slot": 1})
        self.client("guest", {"action": "interact", "uuid": wolf["uuid"]})
        self.wait(lambda s: any(e["uuid"] == wolf["uuid"] and e["max_health"] == wolf["max_health"] + 20
                               for e in s["entities"]), "guest")
        self.server(['item replace entity EchoPetGuest hotbar.1 with pet_home:collar_tag[minecraft:enchantments={levels:{"pet_home:speedster":2}}]'])
        time.sleep(1.2)
        self.client("guest", {"action": "interact", "uuid": wolf["uuid"]})
        self.wait(lambda s: any(e["uuid"] == wolf["uuid"] and e["max_health"] == wolf["max_health"]
                               for e in s["entities"]), "guest")
        self.record("ordinary tamed wolf accepts and replaces real collar; old health modifier removed", wolf=wolf)
        self.server(["save-all flush"])

    def combat(self):
        self.server(["tick unfreeze", "time set noon", "gamerule doDaylightCycle false"])
        if self.heroes(self.snapshot("guest")):
            self.menu("guest"); self.button(0, "guest"); self.close("guest")
            self.wait(lambda s: not self.heroes(s), "guest")
        self.server(["clear EchoPetGuest", "clear EchoPetOwner",
                     "tp EchoPetGuest 40.5 64 40.5", "tp EchoPetOwner 34.5 64 40.5"])
        for kind in ("roman_legionary", "aztec_warrior", "egyptian_archer", "guandao_warrior", "japanese_samurai"):
            self.server([
                "kill @e[tag=ew_target]",
                f'item replace entity EchoPetGuest hotbar.0 with echo_warrior:test_echo_summoner[minecraft:container=[{{slot:7,item:{{id:"echo_warrior:{kind}_relic",count:1,components:{{"minecraft:custom_data":{{EchoWarriorTraitsInitialized:1b,EchoWarriorTraitMask:0,EchoWarriorRelicId:"{uuid.uuid4()}"}}}}}}}}],minecraft:custom_data={{EchoWarriorFuel:1000}}]',
            ])
            self.menu("guest"); self.button(0, "guest"); self.close("guest")
            hero = self.one_hero("guest")
            base = hero["max_health"]
            self.server([
                f"execute store result score base_speed ew_pet run attribute {hero['uuid']} minecraft:generic.movement_speed get 10000",
                'item replace entity EchoPetGuest hotbar.1 with pet_home:collar_tag[minecraft:enchantments={levels:{"pet_home:health_boost":2,"pet_home:speedster":2,"pet_home:fireproof":1,"pet_home:poison_resistance":1,"pet_home:frost_fang":1}}]',
                f"tp {hero['uuid']} 39 64 40.5",
            ])
            self.client("guest", {"action": "select", "slot": 1})
            self.client("guest", {"action": "interact", "uuid": hero["uuid"]})
            hero = self.one_hero("guest", base + 20)
            self.one_hero("owner", base + 20)
            path = 'Inventory[{Slot:0b}].components."minecraft:container"[{slot:7}].item.components."minecraft:custom_data".EchoWarriorPetCollars'
            snapshots = []
            for _ in range(2):
                output = self.server([f"data get entity EchoPetGuest {path}"])
                values = [line.split("entity data: ", 1)[1] for line in output.splitlines() if "entity data: " in line]
                assert len(values) == 1, output
                snapshots.append(values[0])
                time.sleep(1.2)
            self.record("idle equipped collar snapshots across real server ticks", kind=kind, snapshots=snapshots)
            assert snapshots[0] == snapshots[1], "Idle collar data changed without a gameplay state change"
            # Drive real removal/reconstruction before testing the enchantment effects.
            self.menu("guest"); self.button(0, "guest")
            self.wait(lambda s: not self.heroes(s), "guest")
            self.button(0, "guest"); self.close("guest")
            hero = self.one_hero("guest", base + 20)
            self.one_hero("owner", base + 20)
            output = self.server([
                f"execute store result score new_speed ew_pet run attribute {hero['uuid']} minecraft:generic.movement_speed get 10000",
                "execute if score new_speed ew_pet > base_speed ew_pet run say EW_SPEED_RESTORED",
                f"execute store result score hp_before ew_pet run data get entity {hero['uuid']} Health 100",
                f"damage {hero['uuid']} 2 minecraft:in_fire",
                f"execute store result score hp_after ew_pet run data get entity {hero['uuid']} Health 100",
                "execute if score hp_after ew_pet = hp_before ew_pet run say EW_FIREPROOF",
                f"effect give {hero['uuid']} minecraft:poison 30 0 true",
            ])
            assert "EW_SPEED_RESTORED" in output and "EW_FIREPROOF" in output, output
            time.sleep(.6)
            output = self.server([f'execute unless data entity {hero["uuid"]} active_effects[{{id:"minecraft:poison"}}] run say EW_POISON_REMOVED'])
            assert "EW_POISON_REMOVED" in output, output
            self.record("five-hero restored attributes and defensive effects", kind=kind, hero=hero)
            self.server([
                f"tp {hero['uuid']} 39 64 40.5",
                "tp EchoPetGuest 40.5 64 40.5",
                'summon minecraft:husk 42 64 40.5 {NoAI:1b,PersistenceRequired:1b,Tags:["ew_target"],Health:200f,attributes:[{id:"minecraft:generic.max_health",base:200d}]}',
            ])
            target = self.wait(lambda s: any(e["type"] == "minecraft:husk" for e in s.get("entities", [])), "guest")
            target = next(e for e in target["entities"] if e["type"] == "minecraft:husk")
            self.client("guest", {"action": "select", "slot": 8})
            self.client("guest", {"action": "attack", "uuid": target["uuid"]})
            deadline = time.time() + 25
            frozen = False
            while time.time() < deadline:
                output = self.server([
                    "scoreboard players set frozen ew_pet 0",
                    f"execute store result score frozen ew_pet run data get entity {target['uuid']} TicksFrozen",
                    "execute if score frozen ew_pet matches 1.. run say EW_REAL_FROST_HIT",
                ])
                if "EW_REAL_FROST_HIT" in output:
                    frozen = True
                    break
                time.sleep(.3)
            assert frozen, f"{kind} did not deliver a real frost-enchanted AI hit"
            self.record("five-hero actual AI attack triggers restored frost enchantment", kind=kind, target=target["uuid"])
            self.server(["kill @e[tag=ew_target]"])
            self.menu("guest"); self.button(0, "guest"); self.close("guest")
            self.wait(lambda s: not self.heroes(s), "guest")
        self.server(["save-all flush"])

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, required=True)
    parser.add_argument("--prefix", required=True)
    parser.add_argument("--phase", choices=["legacy", "upgrade", "dimensions", "disconnect", "reconnected", "concurrent", "combat", "ordinary", "finish"], required=True)
    parser.add_argument("--attempt", type=int, default=1)
    args = parser.parse_args()
    if os.environ.get("COMPUTERNAME", "").upper() != "FOXY-NODE":
        raise RuntimeError("Actual game acceptance belongs on FOXY-NODE")
    case = Case(args.root, args.prefix, args.phase, args.attempt)
    try:
        getattr(case, args.phase)()
        case.result["state"] = "passed"
    except BaseException:
        case.result["state"] = "failed"
        case.result["error"] = traceback.format_exc()
        try:
            case.server(["tick freeze"], timeout=3)
            case.result["diagnostic_world_frozen"] = True
        except Exception:
            pass
        raise
    finally:
        case.save()


if __name__ == "__main__":
    main()
