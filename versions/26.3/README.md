# Echo Warrior · Minecraft 26.3

2026-11-13 批准新增的 Fabric／NeoForge 维护线，玩法以根目录 26.1.2 主线为准。`0.3.1` 两包及真实双人／旧档升级回归已完成，并已在 GitHub／CurseForge 公开；Modrinth 已上传、项目审核中。产物和证据见 [发行记录](../../docs/releases/0.3.1.md)。

| 组件 | 固定版本 |
| --- | --- |
| Java | 25 |
| Gradle Wrapper | 9.6.0（此目录独立 Wrapper） |
| Fabric Loader / API | 0.20.1 / 0.166.2+26.3 |
| NeoForge | 26.3.0.103 |
| SmartBrainLib / GeckoLib | 2.0.3 / 5.5.7 |
| Loom / ModDev | 1.17.21 / 2.0.148 |

从仓库根执行：

```powershell
.\scripts\build-26.3.ps1 -Loader Dual
python scripts/check-26.3-baseline.py
python scripts/test_compatibility_263_baseline.py
python scripts/check-pet-collar-artifacts.py --version 26.3
```

Java 源码保存在此兼容目录。模型、贴图、动画、声音、数据和全部语言由 `common:stageSharedResources` 从根目录 `common/src/main/resources` 合并；此目录的同路径资源覆盖主线版本。当前覆盖仅为访问规则与两份适配 Renderpearl 的武士 shader。不要手动复制另一套译文或全部美术。

长测用狐狸节点。`prepare-26.3-server-runtime.py` 仅准备并校验官方运行时，不在主机开服；随后由 `run-pet-mainline-tests.ps1` 派发。客户端使用 `run-test-client.ps1 -TargetVersion 26.3`，不得把已升级的测试世界交回旧版本打开。

移植没有新增玩法／数值，百科不需要改变。自动化证据与剩余人工验收见 [实施记录](../../docs/research/port-26.3-implementation-2026-11-13.md) 和 [验收清单](../../docs/COMPATIBILITY_26.3_TEST_CHECKLIST.md)。
