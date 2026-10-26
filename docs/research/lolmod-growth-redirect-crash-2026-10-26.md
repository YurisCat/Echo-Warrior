# 1.20.1 成长属性 Redirect 崩溃：LOL-new 联装排查

排查日期：2026-10-26。最初根据新报告定位启动崩溃；作者随后确认风险目标、要求推送恢复点，并授权继续实施和测试。修复通过下述功能候选验收后，作者进一步授权发布，已纳入 [0.2.10](../releases/0.2.10.md)；正式包哈希、额外原包检查和门户状态以发布记录为准。下方“原始诊断与方案评估”保留实施前的证据与边界。

## 当前候选与验收

恢复点 `940ec13f14efb679c436c480f0e04654b8c25d60` 已推送到 `codex/checkpoint-2026-10-26-before-growth-fix`，包含红沙、Lootr 共享考古、翻译及当时的项目改动。修复在本地 `codex/fix-growth-attribute-compat` 分支进行，没有移动恢复点、改版本号或发布。

实际改动：

- `EchoGrowthAttributeMixin1201` 统一改成 WrapOperation，使用 `original.call(attribute, value)` 执行已有操作链。成长接口、标记、缓存刷新及最终有限值/上界判断原样保留；没有新增 LOL 专用检测或运行时分支。
- 1.20.1 编译/AP 固定 MixinExtras 0.4.1；Forge 用 JarJar 内置 `mixinextras-forge`，范围 `[0.4.1,)`，保留内外层 `LICENSE_MixinExtras`。Fabric 使用已要求的 Loader 0.19.5 自带运行库，本轮日志实测为 MixinExtras 0.5.5；Forge 实测为 0.4.1。
- 扩充真实转换类自测：一次原清理调用、缓存命中/失效、三种 modifier 运算、下界/非有限值、五英雄高属性及撤除加成、普通实例保留 LOL 扩展和英灵不重复乘算。LOL 测试使用核对过的 modifier 命名契约创建局部夹具，不往真实实体添加兼容标记。
- 生产包基线检查包装注入、映射、Forge 外层引导和内层核心库；原损坏包/等价资源测试扩展到 64 项。服务端从实际 JAR 的 `mods.toml` 识别 LOL，要求独立 `lol=true` 成功标记。移植规则为 PORT-039。

候选仍使用内部版本字符串 0.2.9，**不等于已发布的 0.2.9 原包**。首轮候选：

| 候选 | SHA-256 |
| --- | --- |
| Fabric 1.20.1 | `f85cdd0d5c6a82834b5c257280ac608aaa99959f64f8355179aafbc7cf74a738` |
| Forge 1.20.1 | `3bc12719032fca5c077217f512c9637382c2f99fba1c2702ff69a2a5e42d9b2a` |

本地仓库 Wrapper 在 Java 21 / Java 17 工具链下双端构建通过；双 JAR 基线、64 项损坏/等价资源测试、15 项狐狸节点输入校验通过。

首轮候选在 FOXY-NODE 生产专服共十次成功启动，各配置均两次，包含磁盘重启和正常保存退出：

| 任务 | 配置与结果 | 证据 ZIP SHA-256 |
| --- | --- | --- |
| `20261026T120543Z-88376` | 普通 Fabric、Forge 47.4.10 共四次通过；随后联装的 Forge 安装器下载被连接重置，未启动该组合，整任务保留 failed | `498ab2ae7ad16adb9c722349c5c30995a978c0be39d1665524822cf56a5dae62` |
| `20261026T121237Z-83952` | Forge 47.4.26 + LOL-new 1.0.6 + Apothic 1.3.7 / Placebo 8.6.3，两次通过 | `503e62a5431bd9d36e7de34d8bbf9383353c108441bf9fcd91f0ab49157ca8be` |
| `20261026T121547Z-84168` | Forge 47.4.26 + LOL-new 1.0.6，无 Apothic，两次通过 | `64e178e62661eb6b23376a4c2cc3796d28e0ef609696bcdb7658d561da5a0275` |
| `20261026T122009Z-88636` | Forge 47.4.26 + Apothic 1.3.7 / Placebo 8.6.3，无 LOL，两次通过 | `346612d7e8764aa13ef93c7907ca64d888bfbb061cc12fd2411f5d24d38a9161` |

原始证据在 `build/foxy-tests/<任务>/evidence/`。网络失败后只从已通过的诊断任务 `20261026T092238Z-62844` 缓存 104 个官方 libraries 文件，逐文件 SHA-256 复核，复用现有 loader-cache 机制；没有改测试成功条件。

首轮客户端任务 `growth-clients-20261026T122303Z` 的 Fabric 已进入 CATTEST，随后因红沙物品纹理自测失败而停止，保留 failed。核对恢复点 `940ec13` 的控制器可确认这是之前新增红沙时遗漏的测试分支：其物品继承方块模型，旧测试却要求 `item/suspicious_red_sand`；外层启动器也仍要求 43 个物品。现仅为红沙补充精确的 `block/suspicious_red_sand_c05_s0` 烘焙纹理断言，并把启动器期望改为实际 44 项；没有修改红沙资源或玩法，也未忽略模型检查。失败证据包 `build/growth-fix-2026-10-26/growth-clients-20261026T122303Z.zip` 的 SHA-256 为 `86fb5cdca049de59f5b054413200107d3a39216c4b80fd1f15104b3b620b2450`。

补齐客户端自测后的最终候选：

| 候选 | SHA-256 |
| --- | --- |
| Fabric 1.20.1 | `52c5c7fadda81c9b4bcfe87b42aa701ea5405d017164a1548fff9cbf5e1c409c` |
| Forge 1.20.1 | `2c914d5c9c1280ee260c1e1f39c8665ef5932d699d8ad4201d06b91b3ac77262` |

逐个 JAR 条目比较确认，相对首轮候选两端均仅 `AutomatedTestController1201.class` 变化，其他运行类和资源完全相同；比较记录为 `build/growth-fix-2026-10-26/final-artifact-diff.json`。仍重新执行最终产物矩阵，不以首轮结果替代最终文件的验收。

| 最终候选任务 | 配置与结果 | 证据 ZIP SHA-256 |
| --- | --- | --- |
| `20261026T123315Z-87132` | 普通 Fabric、Forge 47.4.10、Forge 47.4.26 + LOL/Apothic 各两次，共六次通过；基线及 64 项损坏/等价资源测试通过 | `09bc8922bec2ed33422e77c8fa56da5eeabdc7371c74e6defdd7e46bca1e3d51` |
| `20261026T123709Z-70876` | Forge 47.4.26 + LOL-new 1.0.6，无 Apothic，两次通过 | `cd33f5c8db4f38f7ce6a139e07e4451f43f9039501b50c917c15eb87f8e4611c` |
| `20261026T123930Z-49456` | Forge 47.4.26 + Apothic 1.3.7 / Placebo 8.6.3，无 LOL，两次通过 | `70b9b1be78c6668d069487d87744ea7962645a3c0fa170863b8d01365ccd9500` |

最终专服共十次通过，每次均核对成长操作链、红沙和正常保存标记。

第二轮客户端任务 `growth-clients-final-20261026T124129Z` 的 Fabric 游戏报告为 `passed=true, cleanup_passed=true`，44 项物品及完整客户端自测通过并正常保存退出；但临时外层 PowerShell 5.1 驱动器通过 `Refresh()` 轮询后取得的 `ExitCode` 为 null，因而整任务仍保留 failed。证据包 `build/growth-fix-2026-10-26/growth-clients-final-20261026T124129Z.zip` 的 SHA-256 为 `3483a7b61cf588d62b75bc263593b26cfc435af5f833f747988d379262b26a2b`。驱动器改为启动后立即保留进程句柄、用有时限的 `WaitForExit()` 等待，明确拒绝 null；在节点上分别用退出码 0 与 7 校验读回。此项只修改本轮忽略目录中的临时驱动器，不修改候选 JAR 或正式启动器退出条件。

最终客户端矩阵 `growth-clients-final-20261026T124530Z` 通过 GUI 桥、已有 `run-test-client.ps1 -Production -StartupOnly` 和独立 CATTEST 串行执行普通 Fabric、普通 Forge 47.4.26、Forge 47.4.26 + LOL/Apothic，**三组均通过**。普通与联装世界分别来自最终候选已正常保存的相应专服，避免把联装世界拿来当无模组对照。客户端快照使用 `20261026T123930Z-49456`，三个联装 JAR 使用 `20261026T123315Z-87132/test-extra-mods` 并由准备器记录哈希。

每组均核对最终候选 JAR 哈希、退出码 0、`passed=true` 和 `cleanup_passed=true`，并从真实日志逐项确认：44 项物品模型/纹理/名称，两种游戏模式真实网络往返，12 类创造销毁/保留路径，五英雄实际召唤、预览、血量同步、技能、AI 命中和撤回，以及书本、罗盘、回收箱、正常保存。任务结束后无本项目 Java 进程残留，节点锁已释放。证据包 `build/growth-fix-2026-10-26/growth-clients-final-20261026T124530Z.zip` 已下载并核对 SHA-256：`93f256cb489152b9867428f238ec7c6f1c2d8605a7eb6dd19ebd8a97ed94c4ba`；解包在同目录 `final-client-evidence/`。

另核对联装客户端导出的真实 `AttributeInstance.class`：`calculateValue` 调用本体 WrapOperation，包装器只调用一次 `Operation.call`，生成的桥接方法继续调用 LOL 的 `allowOverflow` Redirect，确认第三方处理没有被吞掉。反汇编记录为 `build/growth-fix-2026-10-26/final-joint-attribute-bytecode.txt`，原 class 在上述客户端证据包内。

成长公式、存档、网络、Apothic 构造适配以及其他 Minecraft 版本的实现没有修改；本轮无需同步百科或语言文案。原完整整合包及真人多人仍未验收，不将本次最小组合通过外推到全部第三方行为。需要重启，无需新世界。

## 原始诊断与方案评估

**结论：已确认并实跑复现 Echo Warrior 与 LOL-new 的同位成长属性 Redirect 冲突。** 同版 Forge/Apothic 对照通过，只加入 LOL-new 官方 1.0.6 后失败，真实日志同时点名双方注入。最小组合中是 LOL-new 的注入失败，玩家报告中是 Echo Warrior 的注入失败；确认了相同冲突机制，但没有还原原整合包的确切 JAR 与注入顺序。

## 新取得的证据

玩家补充的是完整 crash report，**不是 `logs/latest.log`**。报告自带时间为 `2026-10-14 23:32:56`，未标时区；不把链接分享时间当成新的崩溃发生时间。

- Minecraft 1.20.1，Forge **47.4.26**，Java 17。
- Echo Warrior **0.2.8**。
- Apothic Attributes 1.3.7、Placebo 8.6.3。
- 模组列表另有 `lolmod-1.0.0.jar | LOL Mod | lolmod | 1.0.0`。
- 四个 `ERROR` 条目是 Bosses'Rise、Apothic Attributes、Iron's Spells 'n Spellbooks、dacxirons；四条异常链均指向同一成长属性注入失败。
- 原报告保存于 `build/mixin-crash-2026-10-26/player-report.txt`，SHA-256 为 `fefa558b202e56865029b0c310d7d579bc4c7462e11bc29af02b1050d33da448`。不将带玩家环境信息的完整日志或有时限的签名下载地址加入公开源码。

关键错误为 `EchoGrowthAttributeMixin1201` 的 `echoWarrior$allowCultivatedAttribute` Redirect 检查 `(0/1) succeeded. Scanned 1 target(s)`。

实际目标类是 **`AttributeInstance`**，目标方法是 `calculateValue()`；被重定向的是它内部对 `Attribute.sanitizeValue(double)` 的调用。不能把方法参数里的 `Attribute` 误认为本体 Mixin 的目标类。

本次也不是 0.2.5 修过的 Apothic owner 构造时序异常：该问题发生在启动期类转换，而 owner 问题发生在实体构造期间，错误消息不同。

## 直接命中的第三方字节码

LOL-new 的 CurseForge 项目 ID 为 `1696119`，模组 ID 为 `lolmod`。它与报告中的 **LOLAccessories (`lolaccessories`) 是不同模组**。

检查作者官方发行包后发现多个不同文件的文件名和 `mods.toml` 内部版本均固定为 `lolmod-1.0.0.jar` / `1.0.0`。因此不能仅凭玩家报告将其识别为平台上最早的 1.0.0：

| CurseForge 文件 ID | 平台显示版本 | 内部版本 | 属性 Redirect | SHA-256 |
| --- | --- | --- | --- | --- |
| 8881057 | 1.0.0 | 1.0.0 | 无 | `a37348b2a362998211bd79e7c6a3748fe5c0e4ca01603247065086bcb5330112` |
| 8893408 | 1.0.1 | 1.0.0 | 有 | `55cd3c2bfb66d0884273c1b63ac0f35a1699eceb6de5f000258ed3c01607daf4` |
| 8901226 | 1.0.2 | 1.0.0 | 有 | `ecf46486a93722239d1b34eeca5d7e04a3a3be4e208cab280938c1e1e1662df4` |
| 9054822 | 1.0.6（2026-10-04） | 1.0.0 | 有 | `12c3d3d328a8403e05fadc1d5ddf36500603e859762a65897a49cf0aa77858ba` |

文件 8893408 的官方说明明确提到内置属性修复。文件 8901226 内的 `com.lolmod.mixin.AttributeInstanceMixin` 使用：

- `@Mixin(AttributeInstance.class)`；
- `@Redirect(method = "calculateValue", ...)`；
- 同一个 `Attribute;sanitizeValue(D)D` 调用；
- 默认 Mixin 优先级；
- `lolmod.mixins.json` 的 `defaultRequire: 1`。

Echo Warrior 1.20.1 的实现也用必需的 Redirect 接管这个调用。两个独占 Redirect 不能叠加，一个抢占后另一个注入数为零。双方各自单装可以工作，不代表同装可工作。

反编译证据为 `build/mixin-crash-2026-10-26/lolmod-attribute-javap.txt`；回响战士正式包 refmap 将目标映射为 `AttributeInstance.m_22137_()D` 和 `Attribute.m_6740_(D)D`。

0.2.8 / 0.2.9 正式 Forge 包中的成长属性 Mixin class 逐字节相同，SHA-256 都为 `f67d8a4cf1dd1fdf1f0b9d17b45499d0c8391d6e86572cb65ff9f865100ab307`。因此 0.2.9 的饰品槽修复没有包含此项兼容修复；不应建议单凭升级 0.2.9 解决。

## 狐狸节点对照

全部使用固定 SSH 身份核验后的 FOXY-NODE、Java 17、正式发行 JAR、新建隔离测试世界和既有 `foxy-test-worker.py` / `smoke-test-1.20.1-servers.py`。不运行或修改玩家存档；没有运行图形客户端。

当前工作区有其他开发任务的内容改动，不能用当前源码基线去审计旧发行 JAR。因此真正的 0.2.8 对照使用发布验收任务 `20261014T023817Z-30268` 保存的不可变源码/原包快照，逐文件验证旧清单，再只追加本轮依赖与加载器版本。辅助派发脚本为 `build/mixin-crash-2026-10-26/dispatch-release-diagnostic.ps1`；状态与证据收取仍用 `scripts/run-foxy-tests.ps1`。此快照不是另一个开发源码根。

共同输入：

- Echo Warrior Forge 0.2.8：`52593fdae460ee6a5cdf10656f385be9ad1d92f4baee2a1fae59d2112b804226`
- GeckoLib 4.8.4：`33765775385857e7be6dca712d37f1cf05b48d052b86a8453c93631009ea1106`
- SmartBrainLib 1.15：`f441f0d103d0691befbd1fd99decc55915dae73e112ee7efefd82d45aee253e6`
- Apothic Attributes 1.3.7：`68487b11c0d4e2f67a85f2b04bf65e99ac15294f2923f4391f83e9542293187a`
- Placebo 8.6.3：`1cdf906cfbcbb5e5be2ef1bb721f79a24b01bd2ac559c8cdbfb7693f62421571`
- Forge 47.4.26，匹配玩家报告。

### 无 LOL-new 对照：通过

任务 `20261026T092238Z-62844`：正式 0.2.8 + 上述依赖，两次专服启动、既有自测、五英雄成长范围与磁盘重启、正常保存退出均通过。

证据：`build/foxy-tests/20261026T092238Z-62844/evidence/`；证据 ZIP SHA-256 为 `56fb805d2e291bca84bdabb6733275ecad315d746efcf2cfd17e22ebadf27c03`。

这证明 Forge 47.4.26 + 当前选定 Apothic 最小组合本身不足以触发报告中的崩溃，不将四个 ERROR 条目直接作为冲突来源。

### 加入 LOL-new 1.0.2：先撞到另一处冲突

任务 `20261026T092731Z-78216`：相对上组只加入官方文件 8901226，保持真实文件名 `lolmod-1.0.0.jar`。启动失败，但最先触发的是 **LOL-new 与 Apothic 的护甲计算 Redirect 冲突**：`lolmod$armorPenetration` 被 `apoth_applyArmorPen` 抢占。**这轮不是玩家报告的成长属性崩溃复现**，不能因都出现 `(0/1)` 就混为一谈。

证据：`build/foxy-tests/20261026T092731Z-78216/evidence/`；证据 ZIP SHA-256 为 `30d63911c86a8b3db7457b375a06f49691b8f7d06c646dd6b7f5e3bcfe0b1357`。

继续检查崩溃日期之前的官方 1.0.6（文件 9054822）：其护甲处理已改为不同的 Inject 路径，但成长属性 Redirect class 与 1.0.2 逐字节相同，SHA-256 都为 `fe9fbc04045f5d7095806e7f55d69d73131ac49bb376f725fcdfbbf580556ad0`。后续使用此原包，避免人工改写第三方字节码或用旧版独立问题遮住目标异常。

### 加入 LOL-new 1.0.6

任务 `20261026T093704Z-40220`：相对通过的最小组合，只加入官方文件 9054822。实际 Forge 47.4.26 启动失败，`console-1.log:28` 明确报告：

```text
@Redirect conflict.
Skipping lolmod.mixins.json:AttributeInstanceMixin
  -> lolmod$allowOverflow(...) with priority 1000
already redirected by echo_warrior_1201.mixins.json:EchoGrowthAttributeMixin1201
  -> echoWarrior$allowCultivatedAttribute(...) with priority 1000
```

随后在 `console-1.log:75` 出现 `lolmod$allowOverflow` 的 `(0/1)` InjectionError，Apothic 初始化被该属性类转换失败连带中断。此轮已经到达玩家报告涉及的**同一目标类、同一计算方法、同一被重定向调用**。

与原报告的区别必须保留：最小组合中 Echo Warrior 先占到调用点，LOL-new 失败；玩家报告中失败的是 Echo Warrior。双方均为默认 1000 优先级，不能把一方在当前最小组合先应用当作跨整合包排序保证，也不能声称已逐字复现原报告的失败方向。没有为取得匹配错误而改写第三方包、提高优先级或人工强制排序。

证据：`build/foxy-tests/20261026T093704Z-40220/evidence/`；证据 ZIP SHA-256 为 `ea7133cf8537b01a5e0210c046ce1f81260c3aae9e163a2d0901973cd1dc7d41`。本轮专服未进世界，不能登记为功能或客户端验收通过。

收尾复核两个快照的逐文件输入清单：唯一增加的文件是 `test-extra-mods/1.20.1/forge/lolmod-1.0.0.jar`，既有文件没有变化；验证结果保存于 `build/mixin-crash-2026-10-26/diagnosis-verification.json`。本轮所属 Java/Python 进程均已退出，收尾检查时统一测试锁为空。

### 保留的无效首轮

任务 `20261026T091858Z-52456` 使用 0.2.9 原包配当前工作区源码，在基线阶段因当前新增 `suspicious_red_sand` 文案与旧包不匹配而失败，**没有启动 Minecraft，不属于游戏崩溃复现**。该轮额外输入还包括既有 TBF；不能混入以上最小对照结果。证据 ZIP SHA-256 为 `ff90d89d4fa4f29da1e5e6eeda5d2884ebd02c380b14092e28354e83e2a98feb`。

## 修复方向与验收边界

建议把 1.20.1 的独占 Redirect 改成可组合的属性操作包装，并验证生产重映射和必要的 MixinExtras 运行时可用性；保留双方属性处理链、本体英灵成长范围和普通实体的第三方处理规则。这是待实现方案，不能把现有主线/1.21.1 使用 WrapOperation 当成 1.20.1 已修好。

不采用单纯 `require = 0`、关闭成长 Mixin 或提高 priority 抢占：前两者可能静默丢失成长范围，后者可能只是把失败转移给另一个模组。

修复验收至少包含旧正式包失败、新候选通过、无 LOL-new 对照；同时检查五英雄超过原版上限、普通属性保持外部规则、LOL-new 自己的 modifier 仍生效，以及 Fabric 缺席路径与客户端启动。玩家原整合包仍未取得，不能声明完整整合包或其中所有问题都已修复；玩家本地 LOL JAR 的确切 SHA-256 仍未知。

维护分类：可复用的同位 Redirect / 可选依赖兼容问题，涉及 PORT-021/032 的注入与成长范围约束。**本轮只诊断，没有修改游戏实现**；正式修复时需将已确认规则与确定性联装回归一起纳入移植手册，不把待实施方案写成现有自动守卫。

本轮没有改变玩法、控制、数值、存档或玩家文案，无需修改回声档案馆和本地化；没有发布补丁。

## 风险目标与当前推荐（2026-10-26，尚未实施）

作者关注的是修改是否会连带破坏其他功能，接受有明确影响分析和狐狸节点自动测试支持的底层修改；**没有要求未联装用户必须保留旧代码路径**。上一轮把这理解成“尽量少影响现有用户”并优先推荐条件分支，依据不准确，现予纠正。

**当前建议统一把 1.20.1 的这一处成长属性 Redirect 改为 WrapOperation，不为 LOL-new 新增条件分支。** 目标是在保持既有属性规则的前提下，用一个可组合实现替换一个独占实现；不是重写属性系统。范围与依据如下：

1. 只调整 `EchoGrowthAttributeMixin1201` 的注入方式和原操作调用方式。原 `EchoGrowthAttribute1201` 接口、实例成长标记、`setDirty()` 缓存刷新及最终有限值/上界判断保留。无第三方改写该调用时，包装器应执行与旧实现相同的属性清理，并得到相同数值；有第三方时，应保留实际操作链。
2. 现有 Apothic 构造时序适配继续保留，不因本次修复撤掉。它解决实体初始化先后的独立问题；新增 LOL-new 检测、分支选择、两份属性实现及第三方私有方法依赖均非本案必要条件。
3. 项目主线和 1.21.1 的对应成长属性 Mixin 已采用 WrapOperation，成长标记、脏缓存和最终上界判断与 1.20.1 对应。这提供已有实现参考，但不能替代 1.20.1 Forge/Fabric 的生产包验收。
4. MixinExtras 官方文档建议在包装原操作时优先使用可组合的 WrapOperation；官方 0.4.1 实现明确处理已被 Redirect 替换的调用及其额外参数。这提供实现依据，不构成所有第三方组合均兼容的保证。
5. 1.20.1 Forge 当前没有显式提供该运行库，需要核对并补齐构建/打包声明，兼顾 Fabric 的实际运行库来源。不能依赖整合包中恰好有 EndingLibrary 等模组携带它；具体依赖版本在实施时核对，不盲目升级工具链。
6. 预定修改限定为 1.20.1 的该 Mixin、必要构建声明和相关回归守卫。成长公式、等级/经验、英雄 AI、召唤绑定、网络及存档格式保持原设计；1.21.1 与 26.1.2 无需为此改动。

方案比较：

| 方案 | 优点 | 代价与结论 |
| --- | --- | --- |
| 统一把 1.20.1 Redirect 换成 WrapOperation | 一个实现、一套语义，直接消除本体对调用点的独占；有项目其他版本的实现参考 | 需验证两加载器的库打包、无联装数值等价和真实联装；当前首选 |
| 条件启用可组合分支 | 可以为特定第三方版本保留独立行为 | 同样需要新的运行库与数值验证，另外增加检测、互斥选择及双实现维护；本案没有足够收益成为首选 |
| 关闭注入检查、抢优先级、依赖第三方假 modifier | 改动表面上很少 | 可能静默截断成长、转移崩溃或污染属性状态，不采用 |

风险判断为局部、可验证的属性接入修正，并非零风险。核心风险集中于运行库打包/类加载、生产 refmap、原操作链是否保留、普通实例透传、英灵范围扩展及缓存刷新。代码行数少或测试通过均不能证明所有整合包绝无问题；应以实际产物、明确输入和断言范围作验收结论。

建议的验收：

- **启动与联装**：Forge 无 LOL-new、LOL-new 1.0.6、有/无 Apothic；Fabric 无 LOL-new。旧包失败证据保留；新包必须消除目标 Redirect 冲突。验证仅有本体和必需依赖时也能正常启动，避免偶然借用第三方携带的运行库。额外的小型排序夹具可验证双方应用顺序，但必须标为人工夹具，不能冒充玩家原包。
- **真实数值**：五英雄高生命/攻击、饰品与天赋加成、重应用不补血；普通实体未带 LOL modifier 时维持原范围，带真实 LOL modifier 时其扩展仍生效；下界、NaN/无穷等输入继续遵守原清理行为。
- **生命周期**：预览创建、实际召唤、撤回重召、保存重进，以及既有 Apothic 较晚构造路径。
- **自动回归**：狐狸节点跑受影响 1.20.1 两加载器的既有自动套件，专项组合按固定 JAR 哈希留证据。其他版本未修改时不要求重新进行其全部人工验收。
- **人工检查**：聚焦真实客户端启动、五英雄召唤器预览及高等级战斗/血量显示；不要求重新逐项观察全部模型、声音或遗迹生成。完整玩家整合包仍属于未取得的最终外部验收。

先验证只含这项修复的候选，再纳入日常开发版；测试未通过时保留失败证据，修正或撤回该候选，不扩大为属性系统重构。预定不引入存档迁移，因此应能通过恢复旧 JAR 回退；实施后仍要以保存重进测试确认。该条是方案评估，没有执行游戏代码修改或启动新的测试。

## 官方来源

- https://www.curseforge.com/minecraft/mc-mods/lol-new/files/8881057
- https://www.curseforge.com/minecraft/mc-mods/lol-new/files/8893408
- https://www.curseforge.com/minecraft/mc-mods/lol-new/files/8901226
- https://www.curseforge.com/minecraft/mc-mods/lol-new/files/9054822
- https://github.com/LlamaLad7/MixinExtras/wiki/WrapOperation
- https://raw.githubusercontent.com/LlamaLad7/MixinExtras/0.4.1/src/main/java/com/llamalad7/mixinextras/injector/wrapoperation/WrapOperationInjector.java
- https://raw.githubusercontent.com/LlamaLad7/MixinExtras/0.4.1/src/main/java/com/llamalad7/mixinextras/injector/wrapoperation/WrapOperationInjectionInfo.java

正式 JAR 从对应官方 `edge.forgecdn.net` 文件目录取得，以上哈希按实际下载字节计算。静态排查中读取的其他模组不因此被登记为已通过整合包兼容验收。
