# Minecraft 26.3 移植实施与验收

日期：2026-11-13（Asia/Shanghai）。作者在预评估后明确要求开始移植。
当前内部候选：`0.3.1-dev.1`；本地分支：`feat/minecraft-26.3`。
**双加载器构建、生产专服和生产客户端自动回归已通过；尚未提交、推送或发布。**

## 范围与工程

新增 `versions/26.3/`，保留现有 26.1.2 主线、1.21.1 和 1.20.1。五英灵、技能／饰品成长、协助战斗、考古、知识与教程、回收箱、绑定和可选项圈兼容以当前主线为基准；带上安全跟随回归，不加入新的装备系统或平衡改动。

| 组件 | 版本 |
| --- | --- |
| Minecraft / Java | 26.3 / 25 |
| Fabric Loader / Fabric API | 0.20.1 / 0.166.2+26.3 |
| NeoForge | 26.3.0.103 |
| SmartBrainLib / GeckoLib | 2.0.3 / 5.5.7 |
| 目标线 Wrapper / Loom / ModDev | 9.6.0 / 1.17.21 / 2.0.148 |

Java 在目标目录独立适配。通用资源经 `common:stageSharedResources` 复用根目录，目标仅覆盖访问规则和两份武士 shader。基线核对全部 829 项资源、14 个语言文件、加载器隔离、许可、字节码和依赖，并检查主线 Java 文件没有整项漏迁。新增加的客户端预览工厂及对应自测属于明确的目标版生命周期适配。

没有更改玩家玩法／数值／状态含义，因此无需改百科文章或新增译文。本地化门禁为 0 硬错误、0 待处理。

## 主要适配

- 输入改用 `InputConstants` 的 SDL 键／鼠标常量，左右 Alt 都按即时状态展开；GUI／HUD 状态和摄像机入口按新接口调整。
- 移除旧 `ChatFormatting` 依赖，保持原颜色／斜体等 Style；提示框和动作栏 Mixin 使用目标版真实类与完整签名。
- 武士渲染管线使用 Renderpearl 的类型、绑定布局；shader 改为新 include 和显式输入／输出位置，保留原溶解、透明度和染色逻辑。
- 回收箱破坏裂纹使用新版独立提交阶段；保留箱体贴图与开盖动画。
- 战利品使用有类型的整数提供器与注册表 Holder；更新实体／方块实体常量、容器组件、物品回收预测参数、结构查询和击退上下文，保持原技能数值。
- 召唤器离体预览统一分配互异负 ID，不加入 ClientLevel。五英雄各两实例由真实 renderer 生成状态，关刀完整动画衔接测试复用同一工厂。

## 问题、修复与失败证据

失败轮次保留，均以新快照／实例复测：

| 记录 | 发现与处理 |
| --- | --- |
| `pet-main-20261113T034820Z-72032` | 首轮 Fabric 的全部玩法自测通过；26.3 取消旧正常保存尾句，脚本误判失败。新门禁检查正常退出、退出阶段三维度保存及当轮 `level.dat`，保留其他日志错误拒绝；见 PORT-043 |
| `skill-client-20261113T040030Z-263` | 客户端初始化时 tooltip `@Shadow` 缺少新版 boolean 参数；修正 shadow 与集中列表重载，包基线和损坏签名反例防止回退；见 PORT-021 |
| `skill-client-20261113T041130Z-263` | 离体客户端关刀没有 ID，动画夹具调用 `getId()` 崩溃；召唤器预览使用同类创建路径，已统一修复；见 PORT-044 |
| `skill-client-20261113T041730Z-263` | 新预览测试误用 GeckoLib 故意返回 null 的无参 `createRenderState()`；夹具改用原版／GeckoLib 的上下文入口。没有通过放宽断言跳过渲染 |

构建初期有官方仓库 TLS／CDN 下载失败。仅复用或下载经过 Mojang/Maven SHA-1 核验的公开文件，不关闭 TLS，不更改系统 Java、不引入全局 Gradle。一次性下载与编译日志保留在 `build/port-26.3/`。

## 已通过的狐狸节点测试

| RunId | 环境 | 已完成 |
| --- | --- | --- |
| `pet-main-20261113T035352Z-56204` | Fabric、NeoForge；无宠物模组 | 正式专服启动；五英雄成长、技能／Assist、饰品、跟随；红沙刷取、511／512 格间距、真实异地补位／卸载／拥挤退避；正常保存退出 |
| `pet-main-20261113T040834Z-63048` | 两加载器，各两次 JVM；Pet Home 1.1.3 | 基础回归、实际项圈交互、25 次重新召唤、替换／死亡／失败召唤、捕获／床限制、代次保护、40 条目目录；真实物品存盘与重启恢复 |
| `skill-client-20261113T042030Z-263` | Fabric、NeoForge 正式客户端 | CATTEST 入世；当前候选的集成服成长／技能／饰品／跟随；五英雄 10 个离体预览的实际 renderer；关刀 idle／walk、三种包顺序的动画衔接；技能文本／颜色／换行；暂停释放鼠标、正常保存退出与进程清理 |

Pet Home 来自作者 CF 文件 `9207480`（Fabric）和 `9207490`（NeoForge）。本轮收到的 SHA-256 分别为：

- Fabric：`d00744c412549bbb0b81fc29d313eca81057b145d6ac80586300d4c9b7e06f32`
- NeoForge：`7c23b67d6aab04392b52cfffee8d9f080a81b90b77474afd39aa16fd6ffd09c6`

最终候选相对 Pet Home 专服通过轮次仅改变 `client/` 下的预览工厂、GUI 和自测 class；全部服务端 class 与资源逐项一致，比较原件为 `build/port-26.3/server-equivalence.json`。不把之前的整个 JAR 哈希冒充当前哈希；最终两份 JAR 已在上述双客户端实际运行。

## 最终候选

| 加载器 | 路径 | SHA-256 |
| --- | --- | --- |
| Fabric | `versions/26.3/fabric/build/libs/echo-warrior-fabric-26.3-0.3.1-dev.1.jar` | `27c71ed08592a88e2868395a759b32a98ac8ae59c10a22e6fe163541c16ce401` |
| NeoForge | `versions/26.3/neoforge/build/libs/echo-warrior-neoforge-26.3-0.3.1-dev.1.jar` | `930222c81f6a8bcc2283bc643965dc930cbb636140a7befdb22fe169a79230c6` |

各轮输入清单、日志、报告、崩溃报告及可用截图已按 SHA-256 取回 `build/foxy-tests/<RunId>/evidence`。仅传公开依赖和隔离测试快照；没有复制认证目录或玩家存档，没有在主机启动 Minecraft。

收尾再次核验 FOXY-NODE：测试锁已释放，没有本项目残留 Java／Minecraft 进程。

## 工具与后续发行

- `scripts/build-26.3.ps1`、`check-26.3-baseline.py` 与 10 个损坏包反例。
- 现代专服入口支持哈希保护的 26.3 `runtime.json`；`test_modern_shutdown.py` 的 6 项正反例覆盖保存判据。
- 既有客户端预备器／启动器／批测入口支持 26.3；可用 `collect-26.3-client-evidence.ps1` 取回该轮证据。
- 发布矩阵由六包扩展为八包。11 项 CF、26 项 Modrinth、2 项历史原包恢复测试通过；历史 0.2.2 补发工作流固定使用六包工具提交，避免被新矩阵改写。
- 四线正式版本必须一致；当前 26.3 是内部 dev 候选，旧三线版本未提前变更。没有发起 GitHub Actions 发布、上传平台或回复社区。

## 验收边界

客户端自动测试不等于实机图形／输入全部通过。同日追加由 Codex 在狐狸节点完成的 [图形与输入检查](port-26.3-interactive-check-2026-11-13.md)：左右 Alt、五英雄 GUI、代表性长译文、Assist 点击与存读档，以及 NeoForge 武士实际残影已经补测。完整覆盖和剩余多人、旧版存档升级等范围见 [验收清单](../COMPATIBILITY_26.3_TEST_CHECKLIST.md)，未测项不自动交给作者。作者此前保留的关刀浮空／阿兹特克距离手感调整属于既有设计事项，不是移植新增确认。
