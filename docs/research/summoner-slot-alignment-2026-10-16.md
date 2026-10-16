# 饰品槽对齐实机验收（2026-10-16）

## 范围与结果

1.21.1/1.20.1 右三个饰品槽的内容区从 `95/124/153` 改为 `94/123/152`，与原稿内框一致；空槽说明读取实际 `Slot.x/y`。GUI PNG、物品限制、存档与网络规则不变。没有新增文案或玩法，百科与本地化无需更新。

本轮修复候选通过以下验收，可以进入新版发布准备。候选仍使用内部 `0.2.8` 版本字符串，哈希与已发布 0.2.8 不同；本轮没有改版本号、提交或上传平台。

| 范围 | 结果 |
| --- | --- |
| 1.21.1 Fabric / NeoForge | 两包构建、源码/JAR 基线、真实生产专服各一次启动及正常保存退出通过 |
| 1.20.1 Fabric / Forge | 两包构建、基线、内容对齐、57 项损坏包/等价守卫、生产专服各两次启动及存档重载通过 |
| 1.21.1 NeoForge 21.1.252 / Java 21 | 正式映射 JAR 客户端，隔离 CATTEST；GUI 缩放 2、3、4 的右三格物品与高亮对齐 |
| 1.20.1 Forge 47.4.10 / Java 17 | 正式映射 JAR 客户端，隔离 CATTEST；GUI 缩放 2、3、4 的右三格物品与高亮对齐 |
| 两个实际客户端 | 右三格空槽说明、装入三种饰品、第 4 格左边缘取出/右边缘放回、空槽说明恢复、关闭重开保留物品均通过 |
| 发布本地化门禁 | `python scripts/check-localization.py --release-gate`：624 个键，0 个硬错误、0 个待处理项 |

Fabric 的图形客户端没有在本轮单独运行；不能将共享代码与专服通过写成四个图形客户端全部签收。本次也不复现或修复只有摘要、尚无原始日志的 `EchoGrowthAttributeMixin1201` 崩溃反馈。

## 输入与隔离

- FOXY-NODE、FOXY 账户、固定 SID、主板及解锁的 Session 1 已核验。
- 通过 `scripts/run-foxy-tests.ps1` 派发当前源码与候选 JAR，任务为 `20261016T075035Z-41616`。快照 HEAD 为 `c4d461c7b10b6851dccef059f49be9c244b6efa0`，`source_dirty=true`；不是仅测试 clean HEAD。新建的槽位守卫已补入未跟踪脚本打包清单，并在节点基线实际通过。
- 快照：`D:\Workspace-Terminal\EchoWarrior\20261016T075035Z-41616`；服务器实例与 1.21.1 GUI 位于同任务的 `D:\Games-Terminal\EchoWarrior` 目录。1.20.1 生产客户端由既有准备器放在快照的忽略 `build/compatibility-1.20.1-production-client/` 目录。
- GUI 世界来自本轮正常停止的对应版本专服副本，只在临时副本启用创造、命令和和平难度。没有打开玩家原存档。
- 客户端均由交互会话调用仓库 `scripts/run-test-client.ps1`，串行启动。1.21.1 使用核验参数文件和模组哈希的 `-PreparedProductionLaunch`；1.20.1 使用 `-Production -PauseOnJoin`。

## 实际画面与交互证据

`build/slot-ui-2026-10-16/evidence/` 保存窗口截图与逐次操作回执；`remote-evidence/gui/native-screenshots/` 保存 F2 游戏原始截图。

- 1.21.1：`1211-scale3-empty-slot4/5/6.png`、`1211-scale3-equipped.png`、`1211-scale3-removed-slot4.png`、`1211-scale3-reopened.png`、`1211-scale2-equipped.png`、`1211-scale4-equipped.png`。
- 1.20.1：`1201-scale4-empty-slot4/5/6.png`、`1201-scale4-equipped.png`、`1201-scale4-removed-slot4.png`、`1201-scale4-reopened.png`、`1201-scale2-menu.png`、`1201-scale3-menu.png`。
- 六张空槽悬浮实机截图逐像素检查：1.21.1 缩放 3 的每格为完整 `48×48` 个 `(197,197,197)` 高亮像素；1.20.1 缩放 4 为完整 `64×64`。均从原稿内容区开始，左侧 `(55,55,55)` 槽框未被覆盖。结果保存于 `live-pixel-checks.json`。
- 两个客户端的最终日志均包含 `All dimensions are saved`，正常退出码为 0，无 ERROR/FATAL 行、无崩溃报告。1.20.1 返回标题后有离线测试账户的 Realms 授权 INFO，不属于游戏崩溃。收尾 Java 进程数为 0、测试锁不存在，见 `acceptance.json` 与 `remote-evidence/gui/cleanup.json`。

初次 1.21.1 启动的自动输入没有产生游戏操作，未记为通过。保留了日志、截图与两次位于 OpenGL 查询的线程样本；这些样本不足以独立确定驱动卡死根因。新实例纠正夹具中无效的 `simulationDistance:4` 为 5、限帧 60，之后渲染循环正常。`SendInput` 仍不能触发游戏按键，改用任务目录中的窗口消息辅助脚本后实际操作成功；辅助脚本保留桌面解锁、目标窗口、前台、遮挡及点击边界检查，未修改全局 GUI worker 或模组包。首次实例按核验过归属的 PID 停止；最终通过的是新的实例和上述完整操作证据。

## 候选包与证据哈希

| 候选 JAR | SHA-256 |
| --- | --- |
| 1.21.1 Fabric | `0377f08dfd03d64fa0162bcaad893cef9ef0b2ce66beab958fe085e883c082a9` |
| 1.21.1 NeoForge | `bdc86288bf675c292b3864caeec1b59c7b983297527719703393e2e478a7d585` |
| 1.20.1 Fabric | `9dbe9d2fedcc7e7db9904101d0535a08454fd0dd84afb5b1a7fc1bad0de16ffa` |
| 1.20.1 Forge | `1a671d0cf69a80cac2230b15b9a04a29c43fd0bde303b5e3a3865acf48a3f18a` |

- 服务端证据：`build/foxy-tests/20261016T075035Z-41616/evidence.zip`，SHA-256 `d917f5ac8b5c3cc060f22a213afb7117d0ab1439b35175f496efeeba08491739`。
- GUI 节点证据：`build/slot-ui-2026-10-16/gui-evidence.zip`，SHA-256 `18ab244794f877e3841bd6afe741c4a952cefd1f7d92033b94dfc86fb6cc3b9c`。取回后与节点逐字节核验。
- 相对已发布 0.2.8 的 class 差异仅涉及两个召唤器类及其内部类，另有既知的 Forge 网络类编译字节差异，其 `javap -c -p` 指令列表完全一致。对照保存在 `artifact-diff.json`。属性成长 Mixin 不在改动范围内。
