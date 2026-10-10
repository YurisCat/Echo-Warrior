# Apothic Attributes 联装崩溃排查

排查日期：2026-10-08。首次排查范围为翻译玩家反馈、核对原始崩溃报告、本仓库源码及第三方官方源码。作者随后授权狐狐副机复现并要求评估可选兼容层；本记录保留当时最小组合未复现、未修改游戏代码的结果。**2026-10-10 后续已取得分享包并自然复现，实施可选兼容层；当前结果见 [整合包复现与修复记录](apothic-pack-reproduction-2026-10-10.md)。**

## 反馈和已确认环境

Dracoruff1211 的反馈译文：“1.20.1 出现了一个奇怪的崩溃，我怀疑是 Apothic Attributes 引起的，1.21.1 可能也有同样的问题。”

原报告：https://mclo.gs/C77iCm6

- Minecraft 1.20.1，Forge 47.4.20。
- Echo Warrior 0.2.4，Apothic Attributes 1.3.7（模组 ID `attributeslib`），Placebo 8.6.3。
- 客户端、单人集成服务器；异常描述 `Ticking screen`。
- 界面为 `SummonerScreen1201`；本次正在创建埃及弓箭手预览实体。
- 报告自带时间 `2026-10-08 03:45:24`，未标时区，不转换为北京时间。

核心异常：

```text
java.lang.RuntimeException: An AttributeMap object was modified without a set owner!
```

## 根因

报告调用链（下列方向为调用者到抛错点）：

```text
SummonerScreen1201.refreshPreviewEntity
  → EntityType.create / EgyptianArcherEchoEntity1201.<init>
  → LivingEntity.<init>
  → EchoGrowthEntityMixin1201.echoWarrior$initializeGrowthRange
  → LivingEntity.getAttribute
  → AttributeMap.getInstance / AttributeSupplier 创建属性实例
  → AttributeInstance 标记变更
  → Apothic AttributeMapMixin.apoth_attrModifiedEvent
  → owner == null，抛出异常
```

本仓库 `versions/1.20.1/common/src/main/java/com/yuriscat/echowarrior/compat/mixin/EchoGrowthEntityMixin1201.java:16` 在 `LivingEntity` 构造函数的 `TAIL` 访问生命和攻击属性，为英灵实例启用成长上限扩展。

Apothic Attributes 1.20 分支同样在 `LivingEntity` 构造函数的 `TAIL` 为属性表设置 owner；其 `AttributeMapMixin` 在属性变更入口要求 owner 非空，而且检查发生在客户端/服务端判断之前。玩家报告证明，本次我们的回调先执行，属性的惰性实例化就触发了变更回调，此时对方尚未完成 owner 初始化。异常发生在 `getAttribute` 内，甚至还没执行到我们的 `enableGrowthRange`；后者自身也调用 `setDirty`，因此只去掉该调用不能解决已报告的触发链。

这是两个模组构造阶段初始化顺序的兼容问题。不能仅凭最上层抛错类就归咎于 Apothic，也不是高等级属性实际超过上限后才会出现的问题。

Git 记录确认，该成长 Mixin 新增于提交 `fde285a`（2026-10-07，Release 0.2.4）。当前实现不检查等级配置，因此把 `maxLevel` 改回 30 不会绕过这条初始化路径。

## 影响边界

- **已证实**：报告中的 1.20.1 Forge 组合，在召唤器创建埃及弓箭手客户端预览时崩溃。
- **源码推断**：五位英灵都经过同一个成长 Mixin，实际召唤与实体加载也会构造实体，因此风险不限于埃及弓箭手或预览界面；其他路径仍需联装运行验证。
- **1.21.1 未证实**：本仓库 `EchoGrowthEntityMixin1211` 使用相同构造尾部模式，但所检查的 Apothic 官方 `1.21` 分支（属性文件标为 Minecraft 1.21.1 / Apothic 2.11.0）没有 `AttributeMapMixin`，`LivingEntityMixin` 也没有旧版 owner 绑定回调。没有证据表明当前检查的上游实现会出现这一条相同异常；不将源码检查等同于所有历史发行版或其他组合已通过联装验收。
- 主线 26.1.2 同样存在我们的构造尾部模式；本轮没有审计其第三方版本，不登记为已受影响或已通过。

## 修复建议和后续验证

优先将英灵成长属性初始化移到自有实体构造阶段、父类 `super(...)` 完整返回之后，并在应用成长数值前完成；避免依赖两个模组在同一个 `LivingEntity.<init>` 尾部注入点的排序。需要覆盖五英雄、预览、服务端构造、存档重载及高等级属性缓存刷新。此为待实施方案，不是已验证补丁。

不建议以捕获并吞掉异常、关闭整个成长 Mixin、抢先替第三方设置 owner 或单纯调整等级配置来掩盖问题。

修复任务应固定 Apothic Attributes 1.3.7 和 Placebo 8.6.3 的正式 JAR 及哈希，先验证旧包失败，再验证新包通过；至少覆盖五英雄预览/实际生成、保存重载、高生命/攻击和普通实体属性上界。长测试使用 FOXY-NODE；客户端测试沿用 `scripts/run-test-client.ps1`，生产专服沿用 `scripts/run-foxy-tests.ps1` 并扩展联合测试夹具。1.21.1 使用独立版本依赖和证据，不借用 1.20.1 结果。

维护分类：可复用的 Mixin 构造生命周期/可选依赖兼容问题。实施修复时须同步更新 `VERSION_PORTING_PLAYBOOK.md`（可扩充 PORT-032）与确定性联装回归守卫。本轮仅诊断，不把尚未实施的方案登记为已完成的自动守卫。玩法、文案和数值均未改变，无需更新本地化或回声档案馆。

## 上游依据

以下官方源码均在本轮实际读取；分支 URL 可继续变化，发行联测仍须固定正式 JAR 哈希。

- 1.20 属性变更检查：https://raw.githubusercontent.com/Shadows-of-Fire/Apothic-Attributes/1.20/src/main/java/dev/shadowsoffire/attributeslib/mixin/AttributeMapMixin.java
- 1.20 owner 初始化：https://raw.githubusercontent.com/Shadows-of-Fire/Apothic-Attributes/1.20/src/main/java/dev/shadowsoffire/attributeslib/mixin/LivingEntityMixin.java
- 1.20 版本声明：https://raw.githubusercontent.com/Shadows-of-Fire/Apothic-Attributes/1.20/gradle.properties
- 1.21 Mixin 目录：https://api.github.com/repos/Shadows-of-Fire/Apothic-Attributes/contents/src/main/java/dev/shadowsoffire/apothic_attributes/mixin?ref=1.21
- 1.21 LivingEntity Mixin：https://raw.githubusercontent.com/Shadows-of-Fire/Apothic-Attributes/1.21/src/main/java/dev/shadowsoffire/apothic_attributes/mixin/LivingEntityMixin.java
- 1.21 版本声明：https://raw.githubusercontent.com/Shadows-of-Fire/Apothic-Attributes/1.21/gradle.properties

## 可选兼容层评估

当前建议：如果本次目标是最小化未联装用户的行为变化，可以为 1.20.1 增加条件启用的成长初始化适配层。它仍处理构造生命周期，不能把“放在 compat 包里”理解成完全不涉及底层或免除回归。

候选实现：

1. 在本体 Mixin 配置增加早期选择插件，仅判断已核验的第三方存在性/API，不提前加载 Minecraft 或第三方实体类。沿用仓库 TBF 适配的字节码检查模式；`IMixinConfigPlugin.shouldApplyMixin` 是 Mixin 的正式选择接口。
2. 未联装 Apothic 时继续应用现有 `EchoGrowthEntityMixin1201`，保留原行为。
3. 联装已支持的 Apothic 时，选择插件只停用该过早的初始化 Mixin，并启用一个独立的较晚初始化 Mixin。例如在 `Mob.<init>` 尾部过滤 `EchoWarriorEntity1201`；五位英灵都继承 Mob，而此时 `LivingEntity` 构造及其所有尾部回调已返回。
4. 适配层继续给相同的生命/攻击 `AttributeInstance` 设置成长标记，保留现有 `EchoGrowthAttributeMixin1201` 的范围计算和脏标记；保持成长公式、血量保持规则、网络和存档事务不变。

这是可行性评估，并未通过补丁联装运行。目标方法的生产映射、两条互斥路径恰好启用一条、安装/未安装第三方时的类加载，以及服务器/客户端都必须验证。

| 方案 | 行为影响范围 | 代价与判断 |
| --- | --- | --- |
| 条件选择较晚初始化的兼容层 | 已支持的 Apothic 联装组合 | 增加选择插件和适配 Mixin，保留未联装路径；适合这次局部修复目标 |
| 统一将初始化移出 LivingEntity 构造尾部 | 被修改版本的全部英灵创建 | 代码通常更简单，也规避未来同类构造冲突；需要对修改到的加载器做有/无依赖回归，不等于重测全部玩法 |
| 仅修改 Mixin priority | 仍依赖双方尾部注入顺序 | 改动小但顺序耦合保留，不作为首选 |
| 吞掉异常、关闭成长或抢先写第三方 owner | 可能留下半初始化状态或改变成长/第三方约束 | 不采用；其 setOwner 明确拒绝重复赋值 |

建议的修复验收范围：

- 1.20.1 Forge 正式包：有/无 Apothic 的五英雄创建、真实召唤、撤回重召、磁盘重载；高生命/攻击和普通生物范围，以及不误回血。
- 客户端：五英雄召唤器预览、切换遗物、关闭重开、实际召唤；原反馈整合包另作最终确认。
- 1.20.1 Fabric：若共用 Mixin 配置增加了选择插件，验证无 Apothic 路径仍正常，第三方类未加载。
- 已支持的 TBF + Apothic 三方组合做一次重点交叉回归，避免另一个外部召唤入口遗漏。
- 1.21.1/26.1.2 不因类名相似自动登记为故障或强制修改；若决定同步改其初始化入口，再对实际修改到的加载器验收。
- 世界生成、美术、音效及没有改动的界面布局不需要因这一修复全面重做人工验收。现有自动专服套件可继续整套运行，耗时与手工重测不同。

接口依据：https://raw.githubusercontent.com/SpongePowered/Mixin/master/src/main/java/org/spongepowered/asm/mixin/extensibility/IMixinConfigPlugin.java

## 中文名称与用途

建议译为“神化属性”，强调其前置库角色时可称“神化属性库”。这是沿神化系列的译法，不能声称是作者公布的唯一中文名；本轮核对 MC 百科主条目仍使用英文名，官方中文语言文件主要翻译属性和界面，没有给出模组标题译名。

它提供暴击、生命偷取、穿甲等属性与事件工具，提供物品栏属性面板，并调整护甲/保护的伤害计算。它不只是一个显示属性的客户端界面。官方说明其依赖 Placebo；本次复现固定为报告同版的 1.3.7 + Placebo 8.6.3。

- 官方功能说明：https://www.curseforge.com/minecraft/mc-mods/apothic-attributes
- 官方属性注册：https://raw.githubusercontent.com/Shadows-of-Fire/Apothic-Attributes/1.20/src/main/java/dev/shadowsoffire/attributeslib/api/ALObjects.java
- 官方中文语言：https://raw.githubusercontent.com/Shadows-of-Fire/Apothic-Attributes/1.20/src/main/resources/assets/attributeslib/lang/zh_cn.json
- MC 百科命名：https://www.mcmod.cn/class/12036.html

## 发行包复现输入

作者已授权本次副机测试。使用 `scripts/run-foxy-tests.ps1` 创建独立快照和世界，主机不启动游戏。Echo Warrior 直接取已发布 CI 原包，不重新编译；新增通用联装参数只是测试工作流。

| 输入 | SHA-256 |
| --- | --- |
| Echo Warrior Forge 1.20.1 / 0.2.4 正式包 | `69631a65b08074bc6de011b8c0a5c736090a390380207a8f3de5c402b5c2fa69` |
| Apothic Attributes 1.3.7 | `68487b11c0d4e2f67a85f2b04bf65e99ac15294f2923f4391f83e9542293187a` |
| Placebo 8.6.3 | `1cdf906cfbcbb5e5be2ef1bb721f79a24b01bd2ac559c8cdbfb7693f62421571` |

第三方包来自作者的 Modrinth 正式版本 `fwL9CWGd` / `6SkuAGoz`，均核对平台 SHA-512。原报告未提供玩家本地 JAR 哈希，因此这里只声称相同版本的官方包，不声称与玩家文件逐字节一致。已通过 javap 核对该 Apothic 实包仍包含 owner 空值异常、构造绑定与重复 owner 拒绝。

首次任务 `20261008T154904Z-68688` 在 Forge 47.4.20 安装器下载原版服务端时停留在零字节；核对所属 PID/父进程/命令行后只停止本轮安装器，任务正常记录失败并释放锁。它未进入 Minecraft，不算游戏失败或复现。证据 ZIP SHA-256：`31c317735e16dff1ef1cbed7c2241bbacadb863d1abff1319e93be5587d24274`。随后补齐 Forge 安装路径的官方 SHA-1 缓存复用，再派发新任务；未把缓存误差加入日志忽略规则。

## 实际结果：最小组合未复现

**本次正式包最小组合在专服和真实客户端都通过，因此不能登记为“只要安装这两个版本就必崩”。** 玩家报告中的调用链与双方源码仍说明该次发生了 owner 尚未完成初始化的冲突；为何玩家组合的执行时序不同，尚未通过其完整整合包或转换后字节码进一步验证。其他模组或加载顺序的影响属于待核实原因，不指认具体第三个模组。

最小组合包含 Echo Warrior、GeckoLib 4.8.4、SmartBrainLib 1.15、Apothic Attributes 1.3.7、Placebo 8.6.3，Forge 固定 47.4.20；全部使用发行包，没有安装玩家列表中的其他模组。

- **专服任务 `20261008T155155Z-55112`：通过。** 同版 Forge 正式安装，两次启动、五英雄生命周期和磁盘重启、完整现有专服自测与正常保存退出均通过。任务记录 `client_tested=false`、`original_modpack_reproduced=false`，保持原证据含义。证据目录：`build/foxy-tests/20261008T155155Z-55112/evidence/`；ZIP SHA-256 `649ed40cadd26e1cf51f1f780eb06306d5456ff38b5a948f4d879092ce5f1341`。
- **客户端任务 `apothic-client-20261008T160439Z`：通过。** 在已核验的 FOXY Session 1 / Default 桌面经 GUI 桥启动 `run-test-client.ps1 -TargetVersion 1.20.1 -Loader Forge -Production -ProductionForgeVersion 47.4.20 -StartupOnly -RequireExistingWorld`。使用新的只读源码快照、官方客户端与独立 CATTEST，五英雄均实际打开 SummonerScreen、创建/绘制预览并执行真实召唤、AI 命中和撤回；包括报告中的埃及弓箭手。正常保存退出，启动器确认所属进程全部关闭，独立进程查询未见残余 Java。证据目录：`build/apothic-evaluation-2026-10-08/apothic-client-20261008T160439Z/evidence/`；ZIP SHA-256 `59b14b742827212882c0dbd59a876167a084a64eaf3a655cceb2a197cb40181e`。
- 客户端结果文件精确列出与专服相同的 Echo/Apothic/Placebo 哈希，配置为 `1.20.1-forge-47.4.20`，`passed=true`、`cleanup_passed=true`。它不等同于原整合包或所有注入排序的兼容保证。
- 脚本本地检查：11 项可选依赖/损坏缓存校验、6 项生产客户端准备器检查、Python 编译与 PowerShell 解析通过。新增参数只影响测试工具，未修改任何游戏 Java、资源或发行版本号，也没有发布。

后续要获得稳定失败用例，应取得玩家完整整合包/完整启动日志，或在独立测试夹具中还原“我们的成长回调早于 Apothic owner 绑定”的排序。任何强制排序夹具必须明确标为人工构造，不能冒充玩家原包自然复现。拟议兼容层在该失败用例与无 Apothic 对照中通过后，才可以登记为修复完成；不能把本轮原包就通过的最小组合用作修复前失败证据。
