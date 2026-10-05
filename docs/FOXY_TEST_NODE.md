# 狐狐副机测试节点

2026-10-05 用户指定 `FOXY-NODE`（狐狐副机）长期承担稍长时间的 Minecraft 测试。主机当前仓库是唯一开发源码根，副机只有隔离测试快照。短小检查可留在主机；副机不可用时报告原因，勿自动在主机启动长测试。

## 连接与目录

现有节点管理工作区：`D:\AI-Workshop\60_Infrastructure\desktop-ms-xamanfjjoqzl`。先使用其 `scripts/test-desktop-connection.ps1` 验证 FOXY-NODE、FOXY 账户、SID 和主板，再用 `connect-desktop.ps1`。默认 Tailscale `100.121.97.8`，显式 LAN 备用 `192.168.31.250`。沿用其固定 ED25519 主机公钥和专用 SSH 身份；不要扫描替换公钥或读取/复制私钥。网络、账户、防火墙和节点 Codex CLI 都不属于测试安装范围。

| 用途 | 副机目录 |
| --- | --- |
| 便携完整 JDK | `D:\Tools-Terminal\EchoWarrior\jdk-17`、`jdk-21`、`jdk-25` |
| 当前文件的只读测试快照 | `D:\Workspace-Terminal\EchoWarrior\<RunId>` |
| 测试实例、世界、下载缓存 | `D:\Games-Terminal\EchoWarrior\<RunId>` |
| 状态、逐步日志、证据包 | `D:\Artifacts-Terminal\Reports\EchoWarrior\<RunId>` |
| 上传暂存 | `D:\Download-Terminal\Incoming\EchoWarrior` |
| 主机取回证据 | 仓库 `build/foxy-tests/<RunId>/evidence` |

首次环境配置从主机已有 JDK 的完整目录制作 ZIP，保留 `legal`、`release`、`jmods` 等文件。经固定公钥 SCP 上传，比较整个 ZIP SHA-256 后解压到全新专用目录，分别检查 `java -version` 与 `javac -version`。不要覆盖别的工具或修改全局 PATH/JAVA_HOME。Java 17 用于 1.20.1 游戏运行，Java 21 用于其 Gradle 和 1.21.1，Java 25 用于主线 26.1.2。新快照的 `.toolchains/jdk-<版本>` 通过目录联接复用这些工具，兼容既有启动脚本。当前副机已有 Python 3.14、PowerShell 7、Git 和 7-Zip。

## 派发与取回

在仓库根 PowerShell 执行：

```powershell
# 默认用仓库 Wrapper 构建两条兼容线，再异步派发。
.\scripts\run-foxy-tests.ps1

# 只测反馈所涉 1.21.1。已核验当前构建产物时可加 -SkipBuild。
.\scripts\run-foxy-tests.ps1 -Versions 1.21.1 -SkipBuild

# 使用派发输出中的 RunId。
.\scripts\run-foxy-tests.ps1 -Action Status -RunId <RunId>
.\scripts\run-foxy-tests.ps1 -Action Collect -RunId <RunId>
```

脚本打包当前文件，包含未提交的源码修复和新增回归守卫，不把 clean HEAD 当作本轮版本。文件清单、源码 HEAD、dirty 状态、四个 JAR 的 SHA-256 写入 `foxy-source-manifest.json`，整个压缩包和逐文件输入都在副机核对。不会上传 `.git`、认证文件、Gradle 用户目录、私人工程或已有玩家存档。

每次 RunId 新建测试实例，通过本机 WMI 创建独立隐藏 Python 工作进程并写 `launch.json`/`status.json`，避免普通 SSH 子进程随连接结束被清理；不增加计划任务或修改 SSH 服务。测试串行执行，全节点使用专用 `active-test.lock` 防止重叠。只停止本轮创建的 Java 子进程，不按进程名结束其他客户端。异常断电/手动结束工作进程后如留有锁，先核验锁内 PID、源目录、报告及相关 Java 子进程，确认该任务已停止后才移除该精确锁文件，不自动清锁。

1.21.1 执行源码守卫、双端 JAR 基线、Fabric 和 NeoForge 发行包专服自测；直接从官方仓库安装固定加载器及依赖。无需 GUI 或 RCON。1.20.1 另执行内容对齐、47 项资源等价/损坏包守卫，以及 Fabric/Forge 各两次启动的存档重启回归。JSON 资源按完整数据树比较，避免 CI 的 LF 与 Windows 源码 CRLF 被误报；PNG 和发布 JAR 哈希仍按字节严格核对。仅回环地址开服，EULA 沿用 `PROJECT.md` 5.4.1 的隔离自动测试授权。

成功必须同时满足步骤退出 0、真实运行日志的性能守卫和完整自测标记、正常保存退出；仅有 `status.json` 或静态源码检查不足以确认修复。性能守卫验证一万条历史区域下锁定查询 1 次、2048 格搜索至多 49 次，以及真实方块移除/属性变更、清场与本区块重载修复。它不衡量反馈者整合包的实际 TPS 改善。

Collect 仅取回本轮状态、输入清单、步骤日志、服务端日志/崩溃报告和结果 JSON，不取回世界或配置。证据 ZIP 再次比较 SHA-256。首次真实结果记录在 `docs/FOXY_TEST_2026-10-05.md`。

## 主线与图形测试

当前入口支持 1.21.1/1.20.1 两条兼容线。26.1.2 长测试使用相同固定 SSH 和独立快照，设置任务的 Java 25 后调用仓库 Wrapper 和既有测试脚本；不要用 Java 17，也不要引入全局 Gradle。运行 1.20.1 Gradle 构建时显式传入 `-Dorg.gradle.java.installations.paths=D:\Tools-Terminal\EchoWarrior\jdk-17,D:\Tools-Terminal\EchoWarrior\jdk-21`，让 Java 21 上的 Wrapper 发现 Java 17 编译/运行工具链。

需要实际客户端时先读节点 `GUI.md`，通过现有 `invoke-desktop-gui.ps1` 在已登录且解锁的 FOXY 会话执行 `scripts/run-test-client.ps1`，检查已有客户端并串行运行。普通 SSH 验收只证明无界面测试可运行；画面、声音、控制、真实多人和第三方整合包仍需相应实机验收。
