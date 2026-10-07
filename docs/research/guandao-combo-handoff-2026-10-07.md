# Guandao combo handoff — 2026-10-07

Report: Fabric Minecraft 1.21.1 / Echo Warrior 0.2.3 (interpreting the reporter's
`1.21.10.23`). The supplied GIF shows a brief whole-model tumble around the feet
after the fourth attack. Other versions were not identified by the reporter.

## Scope and cause

GeckoLib 4's unanimated-bone reset interpolates the combo's equivalent three-axis
approximately ±180-degree root pose towards zero over five ticks. Idle/walk had
no explicit Main rotation. In addition, a movement-controller transition can
start from the shared action-layer snapshot. Zero transition on the **action**
controller alone covers neither path. This matches the previously documented
1.20.1 PORT-003 fix; GeckoLib 4.9.2 requires its own runtime verification.

1.21.1 now explicitly rests Main in idle/walk and uses zero movement transition
only during the action/release seam, retaining ordinary three-tick transitions.
All authored combo/attack/hurt keyframes remain semantically unchanged.
1.20.1 retains its existing controller protection; its generator accepts the
upstream zero-rest tracks but still rejects any different authored root rotation.
The GeckoLib 5 mainline has a different, per-render-pass snapshot processor;
it receives an independent regression fixture, with no speculative gameplay fix.

No change to damage, timing, facing, entity identity, binding, save format,
growth or third-party compatibility. No localization/encyclopedia update is
needed: this restores the existing animation rather than changing its design.
No version bump or publication. Restart the client; no new world/chunks required.

## Verification

- Six loader builds passed (26.1.2 Fabric/NeoForge, 1.21.1 Fabric/NeoForge,
  1.20.1 Fabric/Forge), using the repository Wrapper and appropriate JDKs.
- Both compatibility artifact baselines passed; 1.21.1 additionally compares the
  entire shipped animation with the mainline source plus only two root-rest tracks.
- 1.20.1 corrupted-JAR checks reject both a missing rest track and an altered combo.
- Client launcher option regression: eight cases passed.
- Real processor fixtures sample every quarter tick through a combo, then idle
  or walk, with the action-state update two ticks early/on time/two ticks late.
  They require that the combo actually ran and check Main and Body after release.
- FOXY-NODE 1.21.1 Fabric, GeckoLib 4.9.2: before-fix fixture **failed**, at
  `walking=false packetOffset=-2 time=112.0 root=3.1415927,-3.1580706,3.1415927`
  (16:24:58 Asia/Shanghai). After-fix fixture **passed all six cases** at 16:26:35;
  the launcher also reported a successful client smoke test and closed its processes.
- FOXY-NODE 26.1.2 Fabric, GeckoLib 5.5.2: **all six pose-handoff cases passed**
  at 16:42:43 Asia/Shanghai, followed by a successful launcher smoke-test checkpoint.
  Mainline animation/controller logic stays unchanged. GeckoLib 5 may preserve the
  last equivalent Euler pose for a frame: its fixture checks physical quaternion
  rotation (root within 0.05 radians of rest) rather than incorrectly requiring all
  Euler components to be zero. The initial overly strict Euler-only fixture failure
  is retained in the evidence and is not classified as a mainline animation bug.
- 1.20.1 Fabric/Forge: packaged animation compared semantically equal to the
  pre-change generated output; existing controller/test code is unchanged. Its
  earlier runtime evidence remains separate; no new 1.20.1 graphical run this task.
- No separate NeoForge graphical run this task. Test snapshots are isolated from
  this canonical repository and use disposable server-test worlds, not player saves.

Evidence: `build/guandao-combo-2026-10-07/evidence/`, with before/after 1.21.1 logs,
mainline logs, launcher outcomes and `tested-inputs.json`. All nine final tested
source hashes match this workspace. Archive SHA-256:
`519288002f9123aa5dd15972294b5c0fd4c07f1c209b52f45530843aac3fe65d`.
Remote snapshots: `D:\Workspace-Terminal\EchoWarrior\guandao-20261007-before`
and `D:\Workspace-Terminal\EchoWarrior\guandao-20261007-after`.
Initial cold dependency/assets preparation and the disposable world's vanilla
upgrade prompts were completed before the successful tests. All launched game
clients are closed; unrelated desktop windows/processes were left alone.

Remaining manual acceptance: repeated full combos, immediate pursuit, successive
combat, and the original reporter's modpack. Bone assertions do not establish
overall visual feel or third-party renderer compatibility.

Local 1.21.1 candidate JARs (still named 0.2.3; not the published release):

| Loader | SHA-256 |
| --- | --- |
| Fabric | `71aea39c6e62485d94ae672cf1ea91f730a469fee441c904b40baa148907db3f` |
| NeoForge | `5b88e353cd03b917b5a7a7a138c5809edcb16abec27fd1e903beebd697bd80d0` |

2026-10-07 author update: the modeler performed a brief visual check and reported it looks OK. This records that limited manual acceptance, not an exhaustive modpack or all-loader visual test.
