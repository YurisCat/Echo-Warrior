"""Reject silently omitted 1.21.1 Java modules; semantic/runtime checks remain separate."""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
# Old APIs are deliberately replaced, not missing gameplay. Each replacement must still exist.
ADAPTERS = {
    "AutomatedTestPauseController": ("AutomatedTestController", "JoinPausePolicy"),
    "CompatSelfTestCommand": ("HeroLifecycleSelfTest", "HeroDiskRestartSelfTest", "ExplorationSelfTest"),
    "CreativeSummonerInsertionSender": ("InventoryInsertionClient", "InventoryNetwork"),
    "CreativeSummonerDestructionSender": ("InventoryNetwork", "CreativeDestructionRequest"),
    "ClientScreenHooks": ("EchoClient1201Fabric", "EchoClient1201Forge"),
    "CreativeSummonerDestructionPayload": ("CreativeDestructionRequest",),
    "CreativeSummonerInsertionPayload": ("CreativeInsertRequest", "CreativeInsertReply"),
    "EchoProgressionPayload": ("EchoProgressionPacket",),
}
for packet in ("RecallPet", "SummonPet", "TeleportPetToPlayer", "DirectTeleportPetToPlayer",
               "ReleaseRecalledPet", "RevivePet", "HealPet", "DeletePetData",
               "SetPriority", "RequestPetData", "AreaRecall"):
    ADAPTERS[f"TbfNeoforge{packet}PacketMixin"] = (
        f"TbfForge{packet}PacketMixin", f"TbfFabric{packet}PacketMixin")


def audit() -> None:
    source = {p.stem.removesuffix("1211") for p in (ROOT / "versions/1.21.1/common/src").rglob("*.java")}
    target = {p.stem.removesuffix("1201") for p in (ROOT / "versions/1.20.1").glob("*/src/**/*.java")}
    missing = source - target - ADAPTERS.keys()
    assert not missing, f"Unaccounted source modules: {sorted(missing)}"
    for name, replacements in ADAPTERS.items():
        assert name in source, f"Stale adapter explanation: {name}"
        assert set(replacements) <= target, f"Missing adapter for {name}: {set(replacements) - target}"
    print(f"PASS source coverage: {len(source)} modules accounted for; {len(ADAPTERS)} explicit old-API adapters.")


if __name__ == "__main__":
    audit()
