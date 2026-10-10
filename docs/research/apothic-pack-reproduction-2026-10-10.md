# Apothic 原整合包复现与兼容修复（2026-10-10）

本记录继续 `apothic-attributes-crash-2026-10-08.md`；此前最小组合通过的结论仍成立。用户授权在 FOXY-NODE 使用官方 CurseForge 客户端导入分享码 `qXoNRAA6` 并检修。所有世界均为独立 CATTEST 测试副本。

**后续更新**：下文当时未解决的回收箱 144 顶点断言已确认是 ShadowDrop 阴影计数混入导致的自测误报；现已修正测试并通过有/无 ShadowDrop 对照和实际箱盖/存取检查。本文保留原阶段的包哈希与失败记录，当前候选和新增证据见 [回收箱复测](recycler-render-selftest-2026-10-10.md)。

## 输入与导入

- 官方签名的 CurseForge 1.322.0-40357.40357 在副机导入成功，实例名 `test`。安装器本身有 WebView2/NSIS 错误，使用原官方签名 payload 的独立目录运行；没有修改安装器或应用代码。客户端下载停顿时复用已验证的官方运行库缓存，正式导入仍由 CurseForge 完成。
- 分享 ZIP SHA-256：`99a8f269b0312d3ed121eedbe0b55780c7edc0bb07c01edfa1adcac53f76381c`。
- Minecraft 1.20.1 / Forge 47.4.20 / Echo Warrior 0.2.4。分享文件引用 41 个模组，实际启用 38 个；Apothic 1.3.7、Mana and Artifice、MNA Attributes 均为 `.jar.disabled`。保留原 config/defaultconfigs/datapacks（包括作者将成长上限设为 100000 的配置），只在第二种案例启用 Apothic。
- Echo 原包 SHA-256：`69631a65b08074bc6de011b8c0a5c736090a390380207a8f3de5c402b5c2fa69`；Apothic：`68487b11c0d4e2f67a85f2b04bf65e99ac15294f2923f4391f83e9542293187a`；Placebo 8.6.3：`1cdf906cfbcbb5e5be2ef1bb721f79a24b01bd2ac559c8cdbfb7693f62421571`。
- 原包 GeckoLib/SBL 与此前 Maven 文件哈希不同，但 ZIP 逐项比较只有 `META-INF/MANIFEST.MF` 不同，无 `.class` 差异。此次仍使用原包文件，不据此指认第三方代码版本。
- 实际游戏经仓库 `scripts/run-test-client.ps1 -TargetVersion 1.20.1 -Loader Forge -Production -ProductionForgeVersion 47.4.20 -StartupOnly -RequireExistingWorld` 启动；不是开发映射环境，也不把 CurseForge 导入成功本身当成游戏验证。

## 修复前证据

1. `apothic-pack-baseline-20261010T032602Z/as-shared`：五英雄实际预览、召唤、AI 命中和撤回通过。之后停在 `ExplorationClientSelfTest1201` 回收箱渲染顶点数断言（144，预期 72）；这是启用自动测试时的另一个断言失败，不是玩家的 owner 异常，也不记为全套通过。
2. 同任务 `with-apothic` 被 Confluence 首次入服提示挡住而超时；第二次尝试设置超出启动器上限的超时参数在启动前失败。这两次不计作游戏复现。
3. **`apothic-pack-baseline-confirm-20261010T033916Z/with-apothic`：正式原包自然复现。** 处理首次入服提示后，2026-10-10 11:40:55（Asia/Shanghai）在 `SummonerScreen1201.refreshPreviewEntity` 创建罗马军团兵时出现同一 owner 异常。原玩家是埃及弓箭手，二者均经过同一共享构造入口。
4. 转换后的 `LivingEntity` 字节码中，本体 `initializeGrowthRange` 在 Apothic `apoth_ownedAttrMap` 之前。未强制调整 priority/加载顺序；此证据与实际堆栈吻合。

证据存于 `build/apothic-pack-2026-10-10/`，包括分享 ZIP、导入清单、哈希、指定窗口截图、任务回执及基线 `evidence.zip`。副机报告根为 `D:\Artifacts-Terminal\Reports\EchoWarrior\`，源码快照只用于测试。

## 修复范围

新增 1.20.1 `GrowthMixinPlugin1201`，仅从字节码判断旧版 Apothic owner 接口是否存在。存在时停用原 LivingEntity 初始化钩子，改用 `ApothicGrowthEntityMixin1201` 在 Mob 构造尾部初始化两项成长属性；父类构造及其全部尾部回调已完成。无 Apothic 时仍应用原钩子。

不改成长公式、属性上限计算、保存/网络、版本号、1.21.1 或 26.1.2。需要重启，不需要新存档/新区块。没有玩家文案、玩法或数值变化，所以本次不改本地化和百科。可复用根因分类为 Mixin/构造生命周期，已记录 PORT-034，并补入产物缺失拒绝测试和五英雄真实属性自测。

## 修复验证

- 双加载器编译、JAR 基线通过；生产客户端准备器 9 项、联装输入 13 项本地单测通过。
- 客户端 `apothic-pack-fixed-20261010T034354Z`：Apothic 路径日志确认启用，五英雄真实预览/召唤/AI 命中/撤回全部通过，原 owner 异常消失。与失败原包比较，73 份配置/数据及 38 个第三方 JAR 完全一致，唯一变化为本体 JAR。
- 全客户端套件仍在原样配置就存在的回收箱顶点数断言（144 vs 72）处失败，未放宽断言或 ERROR 过滤；该项不属于本次 owner 修复，不能称整合包所有功能均已通过。所有本轮客户端及其 Crash Assistant 进程均按 PID 和任务路径核验后退出。
- 构建后的 Forge 包与正式原包只有新增两类、扩充的成长自测及 `EchoNetworking1201Forge` 四个 class 文件哈希不同；后者源码无变化，`javap -c -p` 指令完全一致，属于编译产物差异。
- **副机专服任务 `20261010T034819Z-79120` 全部通过。** 无可选依赖的 Fabric、Forge 47.4.10 各两次启动；Forge 47.4.20 + Apothic 1.3.7 + Placebo 8.6.3 + TBF 0.2.3 两次启动，共六次正式服启动及磁盘重载。六份控制台日志均有成长自测 PASS；两个联装启动均确认 Mob 兼容入口，并通过 TBF 实际处理器 100 次循环、单条目、队伍连续性、快照保护、取消追踪和普通宠物透传。两个无 Apothic 加载器均确认仍走 LivingEntity 原入口。
- 源码性能守卫、内容一致性及 51 项损坏包/资源等价检查通过。专服证据位于 `build/foxy-tests/20261010T034819Z-79120/evidence/`；ZIP SHA-256 为 `75e049d3ba31d16ea54b68ed694798035a679f496ef1f7922a6b8f5f12a4359f`，取回时已核验。该专服任务自身仍记录 `client_tested=false`，客户端证据由上面的独立任务提供，不修改历史语义。
- 收尾独立查询确认副机 Java 进程为 0、测试锁已释放，官方 CurseForge 窗口及导入实例保留。没有发布、提交或向报告者发送消息。

| 候选包（仍标 0.2.4，未发布） | SHA-256 |
| --- | --- |
| 1.20.1 Forge | `2e97feabdd4de30e6b381fe87955c292cf16781777931d886de5544ece4d6cea` |
| 1.20.1 Fabric | `978aa2dacd501c92d2cd9bbd67474323763d5777956282ce9efebe597f7f171d` |

本地 `comparison.json` 和 `verify-evidence.py` 验证原异常、五英雄 PASS、修复路径标记、输入一致性及非全套通过边界；三个证据 ZIP 已与副机 SHA-256 对照：

| 客户端任务 | 证据 ZIP SHA-256 |
| --- | --- |
| 原样/首次启用 | `2212af227a6f5212b9893554c093935bbac14b38cb4103d425581c98fa1bd575` |
| 启用后自然复现 | `e81b381304ff1dd0da892c60f1396da46afec378e815946c7e20f9b1f721d734` |
| 修复包 | `2c4b366bceae2caa4a832550f972fc33df608a21b46b432bc8f0fcd71f079995` |
