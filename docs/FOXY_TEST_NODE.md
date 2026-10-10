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

1.20.1 正式客户端可通过 `run-test-client.ps1 -Production -ProductionForgeVersion <版本>` 指定隔离实例的 Forge 版本，仍要求 `-TargetVersion 1.20.1 -Loader Forge -StartupOnly`。可选依赖沿用 `ECHO_WARRIOR_TEST_EXTRA_MODS` 并写入产物哈希；`ECHO_WARRIOR_TEST_ARTIFACT_ROOT` 指向已正常保存退出的专服证据/测试世界根。新客户端从该测试世界复制独立 CATTEST，不复用玩家存档，不修改 Gradle 的构建版本。

整合包复现可设置 `ECHO_WARRIOR_TEST_PROFILE_OVERRIDES=<导入实例>`，只复制 `config`、`defaultconfigs`、`datapacks` 并记录启动前 SHA-256，不导入玩家世界、mods 或启动凭据，不覆盖既有文件。`ECHO_WARRIOR_TEST_REQUIRED_MODS=<原包mods目录>` 要求 GeckoLib/SBL 等基础依赖直接来自该目录；缺文件/损坏即失败，不能静默替换为 Maven 同名包。`ECHO_WARRIOR_TEST_CLIENT_HEAP_MIB` 可为大型包指定 1024～16384 MiB（默认 3072）。这些输入只影响测试实例。第三方首次入服界面需要实际处理，超时不算崩溃复现；大包中某项自测断言不适用时保留失败，不放宽整个 ERROR 日志过滤。

## 可选联装测试与官方服务端缓存（2026-10-07）

- 工作进程把 `ECHO_WARRIOR_VANILLA_CACHE` 指向节点工具目录的 `vanilla-server-cache`。只缓存 1.20.1/1.21.1 官方服务端 bundle，使用 Mojang 对象 SHA-1 校验后复制到 Fabric 安装器路径；不复用世界、模组或玩家数据。用于避免官方 CDN 下载超时掩盖运行测试。
- 两条 production smoke 脚本支持 `ECHO_WARRIOR_TEST_EXTRA_MODS=<目录>`，按 `<目录>/<MC版本>/<加载器>/*.jar` 加入可选测试依赖，所有 JAR 均写入测试报告哈希；与已有文件重名即失败。
- 联装 TBF 时同时设置 `ECHO_WARRIOR_TBF_JOINT=1`，强制要求适配层启用及真实 TBF handler 自测通过，不能把“禁用适配也正常启动”计为兼容成功。仍通过同一副机串行测试工作流，不与普通回归或图形客户端抢占节点。

- `-TbfForgeJar <jar> -TbfNeoForgeJar <jar>` 可把明确指定的官方包加入源码快照并核对哈希；默认先做无 TBF 回归，再做两个指定加载器的联装。`-TbfOnly` 仅补跑联合测试，不能宣称包含未执行的无 TBF 检查。
- 通用联装使用 `-ExtraModsDirectory <目录>`，只接受 `<目录>/<选定MC版本>/<合法加载器>/*.jar`，全部随快照核对哈希；默认先跑无额外模组套件，再逐个运行有依赖输入的加载器。`-ExtraModsOnly` 仅跑所选联装，不代表无模组回归通过；通用联装与专用 TBF 模式分开派发。`-SkipBuild -ArtifactRoot <目录>` 可从同样的 `versions/<MC>/<loader>/build/libs/` 层级读取已发布原包，不覆盖本地构建产物；证据以实际包哈希为准，不把当前源码 HEAD 等同于外部原包源码。`-JointForgeVersion 47.4.20` 只覆盖 1.20.1 Forge 联装实例的加载器，不修改项目构建版本。输入校验与损坏缓存拒绝由 `scripts/test_foxy_joint_inputs.py` 覆盖。
- 2026-10-08 首次使用新的 Forge 版本时，安装器的 Mojang 下载停留在零字节；1.20.1 测试脚本现会把已核验官方 SHA-1 的原版服务端缓存复制到 Forge 的正常下载路径。仍由正式安装器安装库文件，不把缓存命中当作 Minecraft 已启动或联装已通过。
- 可选 `fabric-server-cache/<MC>/<loader版本>` 与 `loader-server-cache/<MC>/<加载器>/<版本>` 仅含先前成功官方安装的公共库、启动 JAR/参数文件及 SHA-256 清单，逐文件复核后复制到新实例；无玩家、模组或世界数据。节点工作进程自动传入相应缓存目录。缓存来源为 2026-10-05 的 1.20.1 成功实例和 `20261007T112007Z-27188` 的 1.21.1 成功实例。

- Fabric 通用启动器也可作为 SHA-256 清单中的根目录 JAR 复用；2026-10-07 追加来自成功快照 `20261007T123558Z-43952` 的 `fabric-server-<MC>-0.19.5-1.1.1.jar`。1.20.1 SHA-256 `084080ff36433a56fb26b90e8e5392d6daf4883586ec7198179476a0e184b6a7`；1.21.1 `e25c50698e0c05f07c230fe05c663022c9a675fbca15b81a5d49cac81ca6689c`。

## Windows 控制台收尾修复（2026-10-07）

本轮三个 cmd /c exit -1 遗留错误窗口，发生时间与自动客户端通过检查点后强制收尾一致。原启动器将 conhost.exe / OpenConsole.exe 也纳入按 PID 大小强制结束的集合；这是控制台关闭顺序风险，不应据此认定 Windows DLL 文件损坏。Win32 层实际失败的 DLL/句柄没有进一步跟踪。

自动 StartupOnly 路径改为 -NoNewWindow，沿用启动器控制台，保留标准输出/错误日志。清理遍历继续经过控制台节点寻找真实子进程，但不强制终止控制台宿主；宿主由 Windows 回收。手动客户端路径不变。scripts/test-client-console-cleanup.ps1 校验宿主排除、宿主下子进程保留、其他进程排除、空根拒绝认领、自动路径保留控制台及日志重定向。

FOXY Session 1 / Windows PowerShell 5.1 下，使用新的独立 CMD-CLEANUP-e85a67eb 世界进行 1.21.1 Fabric 启动、检查点和收尾回归，全部通过，未留新 cmd/Java 或错误窗口。之前的发布测试和证据不修改；当前只补签控制台收尾这一范围，没有据此扩展到其他游戏。

基础设施记录：D:\AI-Workshop\60_Infrastructure\desktop-ms-xamanfjjoqzl\operations\2026-10-07-cmd-startup-errors\README.md。微软说明控制台连接尚未完成就关闭时可出现 0xc0000142：https://learn.microsoft.com/en-us/windows/console/creating-a-pseudoconsole-session 。
