"""Real-client 26.1.2 -> 26.3 save migration and multiplayer acceptance on FOXY."""
import argparse
import hashlib
import importlib.util
import json
import os
from pathlib import Path
import re
import time
import traceback
import uuid

from acceptance_nbt import read_nbt

spec = importlib.util.spec_from_file_location("pet_cases", Path(__file__).with_name("pet-acceptance-cases.py"))
pet = importlib.util.module_from_spec(spec)
spec.loader.exec_module(pet)
atomic, next_sequence = pet.atomic, pet.next_sequence
HEROES = ("roman_legionary", "aztec_warrior", "egyptian_archer", "guandao_warrior", "japanese_samurai")
PLAYERS = {"owner": "EchoPetOwner", "guest": "EchoPetGuest"}
OLD_HASHES = {
    "fabric": "a6923639313b5d50aa5ddf30fe19917328b9740aee0092aef0ecb9eaddcfced7",
    "neoforge": "d96d2f0b91b6f207f1df4bd5187929b223e2d13c91d65d54c66bd4356f455adb",
}


def player_uuid(name):
    return str(uuid.UUID(bytes=hashlib.md5(("OfflinePlayer:" + name).encode()).digest(), version=3))


def owner_matches(text, identifier):
    expected = uuid.UUID(identifier)
    for value in re.findall(r"\[I;\s*([-\d,\s]+)\]", text):
        parts = [int(x.strip()) for x in value.split(",")]
        wanted = [int.from_bytes(expected.bytes[i:i+4], "big", signed=True) for i in range(0,16,4)]
        if parts == wanted:
            return True
    return str(expected) in re.findall(r'[0-9a-fA-F]{8}-(?:[0-9a-fA-F]{4}-){3}[0-9a-fA-F]{12}', text)


def stable_summoner(stack):
    components = stack.get("components", {})
    custom = components.get("minecraft:custom_data", {})
    contents = components.get("minecraft:container", [])
    relic = next((e["item"] for e in contents if e["slot"] == 7), {})
    relic_data = relic.get("components", {}).get("minecraft:custom_data", {})
    keys = ("EchoWarriorRelicId", "EchoWarriorLevel", "EchoWarriorExperience",
            "EchoWarriorTraitMask", "EchoWarriorBiomeAffinity", "EchoWarriorAssistMode",
            "EchoWarriorEnabledSkills", "EchoWarriorSkillLayoutVersion")
    return {"summoner_id": custom.get("EchoWarriorSummonerId"),
            "relic_id": relic.get("id"), "relic": {k: relic_data.get(k) for k in keys},
            "accessories": sorted((e["slot"], e["item"]["id"], e["item"].get("count", 1))
                                  for e in contents if e["slot"] < 6)}


class Acceptance(pet.Case):
    def __init__(self, args):
        super().__init__(args.root, args.prefix, args.phase, args.attempt)
        self.loader = args.loader
        self.world = self.server_root / "acceptance-world"
        self.legacy_evidence = self.root / f"legacy-{args.loader}-legacy-evidence-1"
        self.upgrade_evidence = self.root / f"upgrade-{args.loader}-upgrade-evidence-1"

    def server(self, commands, timeout=20):
        sequence = next_sequence(self.control)
        before = self.console.read_text(encoding="utf-8")
        atomic(self.control / f"{sequence:05d}.json", {"commands": commands})
        marker = f"EW_END_{sequence:05d}"
        until = time.time() + timeout
        while time.time() < until:
            text = self.console.read_text(encoding="utf-8")[len(before):]
            if marker in text or ("stop" in commands and self.server_state().get("state") in ("stopped", "failed")):
                (self.evidence / f"server-{sequence:05d}.log").write_text(text, encoding="utf-8")
                if any(s in text for s in ("Unknown or incomplete command", "Unknown item", "Incorrect argument")):
                    raise AssertionError(text)
                return text
            time.sleep(.1)
        raise RuntimeError(f"Console timeout: {commands}")

    def server_state(self):
        return json.loads((self.server_root / "acceptance-server-state.json").read_text(encoding="utf-8-sig"))

    def joined(self):
        for role, name in PLAYERS.items():
            # Cold production clients may still be loading fonts/native libraries.
            # Only initial readiness gets the longer budget; gameplay actions keep 30s.
            self.client(role, {"action": "snapshot"}, timeout=150)
            state = self.wait(lambda s: s.get("connected") and s.get("player") == name,
                              role, seconds=100)
            assert state["uuid"] == player_uuid(name), state
        self.record("two distinct production clients connected",
                    owner=self.snapshot("owner")["uuid"], guest=self.snapshot("guest")["uuid"])

    def menu_slot(self, slot, role="owner"):
        self.close(role)
        self.client(role, {"action": "select", "slot": slot})
        self.client(role, {"action": "use"})
        return self.wait(lambda s: "SummonerMenu" in s.get("menu", ""), role)

    def press(self, button, role="owner"):
        # Delivery is not acceptance: every caller verifies a server/data/client postcondition.
        self.client(role, {"action": "button", "id": button})
        time.sleep(.3)

    def client_barrier(self, role):
        # Same real TCP stream: observing this command on the server proves that
        # preceding menu packets reached the server, not merely the client queue.
        token = "EW_CLIENT_BARRIER_" + uuid.uuid4().hex
        self.client(role, {"action": "command", "command": "say " + token})
        until = time.time() + 30
        while time.time() < until:
            text = self.console.read_text(encoding="utf-8")
            if token in text:
                (self.evidence / (token + ".log")).write_text(
                    "\n".join(line for line in text.splitlines() if token in line), encoding="utf-8")
                return
            time.sleep(.1)
        raise AssertionError(f"Client command did not reach server: {role}")

    def hero_set(self, count=5, role="owner"):
        return self.heroes(self.wait(lambda s: len(self.heroes(s)) == count, role, seconds=40))

    def assert_server_count(self, count=5):
        commands = ["scoreboard objectives add ew_upgrade dummy",
                    "scoreboard players set count ew_upgrade 0"]
        commands += [f"execute as @e[type=echo_warrior:{kind}_echo] run scoreboard players add count ew_upgrade 1"
                     for kind in HEROES]
        commands += [f"execute if score count ew_upgrade matches {count} run say EW_HERO_COUNT_{count}"]
        text = self.server(commands)
        assert f"EW_HERO_COUNT_{count}" in text, text

    def saved_data(self, name):
        paths = list((self.world / "dimensions/minecraft/overworld/data").rglob(f"*{name}.dat"))
        assert len(paths) == 1, (name, paths)
        return read_nbt(paths[0]).get("data", {})

    def snapshot_disk(self):
        self.server(["save-all flush"])
        players = {}
        for role, name in PLAYERS.items():
            players[role] = read_nbt(self.world / "players/data" / (player_uuid(name)+".dat"))
        battlefield = self.saved_data("battlefield_sites")
        binding = self.saved_data("echo_bindings")
        return {
            "players": players, "battlefield": battlefield, "bindings": binding,
            "level": read_nbt(self.world / "level.dat")["Data"],
        }

    def stable_inventory(self, snapshot):
        result = {}
        for role, data in snapshot["players"].items():
            for stack in data["Inventory"]:
                if stack["id"] == "echo_warrior:test_echo_summoner":
                    value = stable_summoner(stack)
                    assert value["summoner_id"] and value["relic"]["EchoWarriorRelicId"]
                    key = value["relic"]["EchoWarriorRelicId"]
                    assert key not in result, "Duplicate physical relic identity"
                    result[key] = value
        return result

    def assert_world_preserved(self, before, after, check_inventory=True):
        if check_inventory:
            assert self.stable_inventory(before) == self.stable_inventory(after), "Relic/accessory/progress changed"
        old_book = next(x for x in before["players"]["owner"]["Inventory"]
                        if x["id"] == "echo_warrior:knowledge_fragment_collection")
        new_book = next(x for x in after["players"]["owner"]["Inventory"]
                        if x["id"] == "echo_warrior:knowledge_fragment_collection")
        assert old_book["components"]["minecraft:custom_data"] == new_book["components"]["minecraft:custom_data"]
        old_regions = [x for x in before["battlefield"]["regions"] if x["status"] == "ACTIVE"]
        assert old_regions, "Fixture must contain a real active battlefield"
        new_regions = {(x["region_x"], x["region_z"]): x for x in after["battlefield"]["regions"]}
        for old in old_regions:
            current = new_regions[(old["region_x"], old["region_z"])]
            for key in ("status", "center", "relic", "seed", "culture", "brushables"):
                assert old.get(key) == current.get(key), ("Battlefield changed", key, old, current)
        old_ids = {x["summoner_id"] for x in before["bindings"]["bindings"]}
        new_ids = {x["summoner_id"] for x in after["bindings"]["bindings"]}
        assert old_ids == new_ids and len(old_ids) == 5, "Bindings changed or invalid fixture"
        self.record("disk data preserved: five relic identities, levels, XP, traits, Assist, accessories, book and battlefield",
                    bindings=len(old_ids), active_battlefields=len(old_regions))

    def stop_all(self):
        for role in PLAYERS:
            self.close(role)
            self.client(role, {"action": "stop"})
        time.sleep(3)
        self.server(["save-all flush", "stop"], timeout=50)
        state = self.server_state()
        assert state["state"] == "stopped" and state["normal_save"] and state["exit_code"] == 0, state
        hashes = {p.relative_to(self.world).as_posix(): hashlib.sha256(p.read_bytes()).hexdigest()
                  for p in self.world.rglob("*") if p.is_file() and p.name != "session.lock"}
        atomic(self.evidence / "world-sha256.json", hashes)
        self.record("both clients closed and server normally saved", artifacts=state["artifacts"], world_files=len(hashes))

    def legacy(self):
        self.joined()
        artifacts = self.server_state()["artifacts"]
        assert artifacts[f"echo-warrior-{self.loader}-26.1.2-0.3.0.jar"] == OLD_HASHES[self.loader]
        assert "Starting minecraft server version 26.1.2" in self.console.read_text(encoding="utf-8")
        self.server(["time set day", "weather clear", "forceload add 16 16 64 64",
                     "fill 24 63 24 56 63 56 minecraft:stone",
                     "fill 24 64 24 56 78 56 minecraft:air",
                     "tp EchoPetOwner 40.5 64 40.5 0 0", "tp EchoPetGuest 43.5 64 40.5 0 0",
                     "clear EchoPetOwner", "clear EchoPetGuest",
                     "gamemode creative EchoPetOwner", "gamemode creative EchoPetGuest"])
        for index, hero in enumerate(HEROES):
            relic_id = str(uuid.uuid4())
            custom = (f'EchoWarriorTraitsInitialized:1b,EchoWarriorRelicId:"{relic_id}",'
                      f'EchoWarriorLevel:{17+index*7},EchoWarriorExperience:{3+index},'
                      f'EchoWarriorTraitMask:0,EchoWarriorBiomeAffinity:2,EchoWarriorAssistMode:{index%3},'
                      f'EchoWarriorSkillLayoutVersion:2,EchoWarriorEnabledSkills:{31 if index==1 else 15}')
            command = (f'item replace entity EchoPetOwner hotbar.{index} with echo_warrior:test_echo_summoner'
                       '[minecraft:custom_data={EchoWarriorFuel:1000},minecraft:container=['
                       '{slot:0,item:{id:"echo_warrior:feast_ham_accessory",count:1}},'
                       f'{{slot:7,item:{{id:"echo_warrior:{hero}_relic",count:1,components:'
                       f'{{"minecraft:custom_data":{{{custom}}}}}}}}}]]')
            self.server([command])
            self.menu_slot(index)
            self.press(0)
            self.hero_set(index+1)
            self.close()
        for role in PLAYERS:
            self.hero_set(role=role)
        entries = json.loads((Path(__file__).resolve().parents[1] /
                              "common/src/main/resources/data/echo_warrior/knowledge/entries.json").read_text(encoding="utf-8"))["entries"]
        ids = [entries[n]["id"] for n in (0, 1, 2)]
        pages = ",".join(f'{{Id:"{name}",Count:{i+1}}}' for i, name in enumerate(ids))
        self.server([
            'item replace entity EchoPetOwner hotbar.5 with echo_warrior:knowledge_fragment_collection'
            f'[minecraft:custom_data={{EchoWarriorKnowledgePages:[{pages}],EchoWarriorKnowledgeBookmark:"{ids[1]}"}}]',
            "item replace entity EchoPetOwner hotbar.6 with echo_warrior:tutorial_manual",
            "item replace entity EchoPetOwner hotbar.7 with echo_warrior:echo_compass",
            "execute as EchoPetOwner at @s run echo_warrior battlefield generate 1",
        ])
        until = time.time()+150
        while time.time() < until:
            snap = self.snapshot_disk()
            if any(x["status"] == "ACTIVE" for x in snap["battlefield"].get("regions", [])):
                break
            time.sleep(2)
        else:
            raise AssertionError("No actual generated battlefield")
        self.assert_server_count()
        atomic(self.evidence / "legacy-state.json", snap)
        atomic(self.evidence / "legacy-heroes.json", self.hero_set())
        assert len(self.stable_inventory(snap)) == 5
        self.record("released 0.3.0 created five populated summoners and a real battlefield",
                    inventory=self.stable_inventory(snap))
        self.stop_all()

    def upgrade(self):
        self.joined()
        assert "Starting minecraft server version 26.3" in self.console.read_text(encoding="utf-8")
        before = json.loads((self.legacy_evidence / "legacy-state.json").read_text(encoding="utf-8"))
        after = self.snapshot_disk()
        self.assert_world_preserved(before, after)
        assert after["level"]["DataVersion"] > before["level"]["DataVersion"], "World was not upgraded"
        old_health = {x["type"]: x["max_health"] for x in json.loads((self.legacy_evidence / "legacy-heroes.json").read_text(encoding="utf-8"))}
        for role in PLAYERS:
            assert {x["type"]: x["max_health"] for x in self.hero_set(role=role)} == old_health
        self.assert_server_count()
        self.record("both clients see all five upgraded heroes with unchanged maximum health",
                    old_data_version=before["level"]["DataVersion"], new_data_version=after["level"]["DataVersion"])
        self.client("owner", {"action": "select", "slot": 5})
        self.client("owner", {"action": "use"})
        self.wait(lambda s: "KnowledgeReaderMenu" in s.get("menu", ""))
        self.close()
        for i in range(5):
            self.menu_slot(i)
            previous = {x["uuid"] for x in self.hero_set()}
            self.press(0); self.hero_set(4)
            self.press(0); current = self.hero_set()
            assert {x["uuid"] for x in current} != previous
            assert {x["type"]: x["max_health"] for x in current} == old_health
            self.close()
        self.record("all five actual menu dismissal/resummon cycles preserve upgraded attributes")
        # Use ordinary survival inventory semantics for the handoff. Creative
        # container prediction can emit a separate creative drop/copy packet.
        self.server(["difficulty peaceful", "gamemode survival EchoPetOwner",
                     "gamemode survival EchoPetGuest",
                     "effect give EchoPetOwner minecraft:resistance infinite 4 true",
                     "effect give EchoPetGuest minecraft:resistance infinite 4 true"])
        for role in PLAYERS:
            self.client_barrier(role)
        # Physical handoff through a real inventory THROW packet; no item copy for the transfer.
        self.client("owner", {"action": "select", "slot": 0})
        self.client("owner", {"action": "slot", "slot": 36, "button": 1, "type": "THROW"})
        self.wait(lambda s: s["inventory"][0].startswith("0 "))
        self.client_barrier("owner")
        dropped = self.server([
            "scoreboard players set dropped ew_upgrade 0",
            'execute as @e[type=minecraft:item,nbt={Item:{id:"echo_warrior:test_echo_summoner"}}] run scoreboard players add dropped ew_upgrade 1',
            "execute if score dropped ew_upgrade matches 1 run say EW_EXACTLY_ONE_DROPPED_SUMMONER",
        ])
        assert "EW_EXACTLY_ONE_DROPPED_SUMMONER" in dropped, dropped
        self.server(["tp EchoPetOwner 32.5 64 40.5",
                     'tp EchoPetGuest @e[type=minecraft:item,nbt={Item:{id:"echo_warrior:test_echo_summoner"}},limit=1]'])
        time.sleep(1)
        self.server(['tp EchoPetGuest @e[type=minecraft:item,nbt={Item:{id:"echo_warrior:test_echo_summoner"}},limit=1]'])
        self.wait(lambda s: "echo_warrior:test_echo_summoner" in s["inventory"][0], "guest")
        transferred = self.snapshot_disk()
        for role, count in (("owner", 4), ("guest", 1)):
            stacks = [x for x in transferred["players"][role]["Inventory"]
                      if x["id"] == "echo_warrior:test_echo_summoner"]
            assert len(stacks) == count and all(x.get("count", 1) == 1 for x in stacks), (role, stacks)
        self.assert_world_preserved(before, transferred)
        self.menu_slot(0, "guest"); self.press(10, "guest"); self.close("guest")
        self.assert_controller("guest")
        self.assert_server_count()
        self.record("physical transfer plus native FOLLOW changes controller without duplicating hero")
        # Two real owners operate distinct physical summoners at the same time.
        # Copying a UUID is not shared ownership: modern bindings deliberately
        # assign a new identity to each physical copy.
        baseline = self.stable_inventory(self.snapshot_disk())
        identities = {value["relic_id"]: key for key, value in baseline.items()}
        owner_relic = identities["echo_warrior:aztec_warrior_relic"]
        guest_relic = identities["echo_warrior:roman_legionary_relic"]
        self.menu_slot(1, "owner"); self.menu_slot(0, "guest")
        atomic(self.control / "proxy-delay.json", {"delay_ms": 150})
        at = int(time.time()*1000)+1000
        replies = []
        for role in PLAYERS:
            q = self.root / f"{self.prefix}-{role}" / "acceptance-queue"
            n = next_sequence(q)
            atomic(q / f"{n:05d}.json", {"action": "button", "id": 42 if role == "owner" else 41, "at_ms": at})
            replies.append(q / f"{n:05d}-result.json")
        until=time.time()+20
        while not all(p.exists() for p in replies):
            if time.time()>until:
                raise AssertionError("Concurrent request acknowledgements missing")
            time.sleep(.1)
        data=[json.loads(p.read_text(encoding="utf-8")) for p in replies]
        assert all(x["passed"] for x in data)
        for role in PLAYERS:
            self.client_barrier(role)
        changed = self.stable_inventory(self.snapshot_disk())
        expected = json.loads(json.dumps(baseline))
        # JSON turns accessory tuples into lists; normalize both comparison sides.
        expected[owner_relic]["relic"]["EchoWarriorAssistMode"] = 2
        expected[guest_relic]["relic"]["EchoWarriorAssistMode"] = 1
        assert json.loads(json.dumps(changed)) == expected, "Concurrent menu update crossed owners or was lost"
        self.assert_server_count()
        self.press(41, "owner"); self.press(40, "guest")
        for role in PLAYERS:
            self.client_barrier(role)
        self.close("owner");self.close("guest")
        for role in PLAYERS:
            self.client_barrier(role)
        atomic(self.control / "proxy-delay.json", {"delay_ms": 0})
        assert self.stable_inventory(self.snapshot_disk()) == baseline, "Menu restoration changed inventory"
        self.assert_controller("guest")
        self.assert_controller("owner", "aztec_warrior")
        self.assert_server_count()
        self.record("concurrent distinct summoner menus with 150ms bidirectional latency update only their own Assist state",
                    requested_at=at, client_times=[x["time_ms"] for x in data],
                    owner_relic=owner_relic, guest_relic=guest_relic, server_verified_modes=[2, 1])
        for dimension in ("the_nether", "overworld"):
            self.server([
                f"execute in minecraft:{dimension} run forceload add 32 32 48 48",
                f"execute in minecraft:{dimension} run fill 32 63 32 48 63 48 minecraft:stone",
                f"execute in minecraft:{dimension} run fill 32 64 32 48 72 48 minecraft:air",
                f"execute in minecraft:{dimension} run tp EchoPetGuest 40.5 64 40.5",
            ])
            self.wait(lambda s: f"minecraft:{dimension}]" in s.get("dimension", "") and
                      len(self.heroes(s, "roman_legionary")) == 1, "guest", seconds=40)
        self.assert_controller("guest")
        self.assert_server_count()
        self.record("real guest and bound hero cross dimensions and return")
        saved=self.snapshot_disk()
        self.assert_world_preserved(before, saved)
        atomic(self.evidence / "before-reconnect.json", saved)
        self.client("guest", {"action": "disconnect"})
        self.wait(lambda s: not s.get("connected"), "guest")
        self.client("guest", {"action": "stop"})
        self.record("real guest socket disconnected; waiting for a fresh client process")

    def assert_controller(self, role, kind="roman_legionary"):
        state = self.snapshot(role)
        until=time.time()+15
        while time.time()<until:
            heroes=self.heroes(self.snapshot(role),kind)
            if len(heroes)==1:
                text=self.server([f'data get entity {heroes[0]["uuid"]} EchoOwner'])
                if owner_matches(text, state["uuid"]):
                    return
            time.sleep(.3)
        raise AssertionError(f"Bound {kind} controller did not match actual client UUID")

    def reconnected(self):
        self.joined()
        self.hero_set(role="guest")
        self.assert_controller("guest")
        self.assert_server_count()
        before=json.loads((self.upgrade_evidence/"before-reconnect.json").read_text(encoding="utf-8"))
        after=self.snapshot_disk()
        self.assert_world_preserved(before,after)
        old_item=next(x for x in before["players"]["guest"]["Inventory"] if x["id"]=="echo_warrior:test_echo_summoner")
        new_item=next(x for x in after["players"]["guest"]["Inventory"] if x["id"]=="echo_warrior:test_echo_summoner")
        assert old_item["components"]["minecraft:custom_data"]["EchoWarriorFuel"] == new_item["components"]["minecraft:custom_data"]["EchoWarriorFuel"]
        atomic(self.evidence/"upgraded-state.json",after)
        self.record("same identity reconnects with preserved items, controller and fuel")
        self.stop_all()

    def restart(self):
        self.joined()
        before=json.loads((self.root/f"upgrade-{self.loader}-reconnected-evidence-1/upgraded-state.json").read_text(encoding="utf-8"))
        after=self.snapshot_disk()
        self.assert_world_preserved(before,after)
        self.hero_set(role="owner");self.hero_set(role="guest")
        self.assert_controller("guest")
        self.assert_server_count()
        self.record("upgraded world survives a second 26.3 JVM boot")
        self.stop_all()


def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument("--root",type=Path,required=True)
    p.add_argument("--prefix",required=True)
    p.add_argument("--loader",choices=("fabric","neoforge"),required=True)
    p.add_argument("--phase",choices=("legacy","upgrade","reconnected","restart"),required=True)
    p.add_argument("--attempt",type=int,default=1)
    args=p.parse_args()
    if os.environ.get("COMPUTERNAME")!="FOXY-NODE" or not args.root.resolve().is_relative_to(Path(r"D:\Games-Terminal\EchoWarrior")):
        raise ValueError("Only isolated FOXY worlds")
    case=Acceptance(args)
    try:
        getattr(case,args.phase)()
        case.result["state"]="passed"
    except BaseException:
        case.result["state"]="failed";case.result["error"]=traceback.format_exc()
        raise
    finally:
        case.save()


if __name__=="__main__":
    main()
