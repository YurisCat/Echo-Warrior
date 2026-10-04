# CurseForge 六文件发布流程

项目 ID：`1677436`。从同一版本号、同一 Git 提交构建六个独立 JAR：

| Minecraft | 加载器 | 构建与游戏 Java |
| --- | --- | --- |
| 26.1.2 | Fabric / NeoForge | 25 |
| 1.21.1 | Fabric / NeoForge | 21 |
| 1.20.1 | Fabric / Forge（不是 NeoForge） | Gradle 21；编译与游戏 17 |

SmartBrainLib 与 GeckoLib 始终外置；Fabric 另需 Fabric API。不制作跨加载器通用单包。

同一工作流现支持 Modrinth：六份 JAR 共用一次构建及同一组 SHA-256，平台元数据分别生成。手动输入 `publish` 仍只控制 CF，`publish_modrinth` 独立控制 MR；`manual_release` 只适用于 CF。登记 `MODRINTH_PROJECT_ID` 后，未来版本标签才自动启用 MR。MR 预检在任何平台 POST 之前完成，配置或已有版本冲突会先阻止两平台上传。首次补发 MR 时关闭 CF，不重推旧标签。详见 `docs/MODRINTH_RELEASE.md`。

## 本地发布候选

```powershell
.\scripts\build-1.20.1.ps1 -Loader Dual -Clean
python scripts/check-1.20.1-baseline.py
python scripts/test_compatibility_1201_baseline.py
python scripts/check-1.20.1-content-parity.py
python scripts/smoke-test-1.20.1-servers.py --loader both

.\scripts\build-1.21.1.ps1 -Loader Dual -Clean
.\scripts\check-1.21.1-baseline.ps1 -SkipBuild
.\scripts\smoke-test-1.21.1-servers.ps1 -Loader Dual -SkipBuild

$env:JAVA_HOME = (Resolve-Path '.toolchains\jdk-25').Path
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat clean dualBuild

python scripts/check-localization.py --release-gate
python scripts/test_curseforge_release.py
python scripts/prepare-curseforge-release.py --release-type release --require-jars
```

客户端统一使用 `scripts/run-test-client.ps1`，串行运行；自动检查启用 `-StartupOnly` 并确认释放鼠标、关闭所有自启进程。1.20.1 用 `-Production` 检查真实发行包。每条版本线沿用已完成的针对性人工交互验收，其余加载器完成启动冒烟；不要求每次局部修复重新全量人工验收，也不把自动检查当作视觉/声音/联机签收。

正式候选必须来自已提交的发布分支。内部 `-dev.N`、sources、temporary-delivery 或脏工作区临时包不得上传。公开编号采用干净的 `x.y.z`，三个 gradle.properties 必须一致。每次准备产物记录六份元数据与 SHA-256 清单。

## 本地化门禁

准备脚本自动执行 `python scripts/check-localization.py --release-gate`。无效 JSON、占位符损坏、源线差异等硬错误不能放行；缺失或过期的非核心译文必须先报告作者。只有作者对当次发布明确豁免，才能使用 `--allow-pending-localization` 或工作流的同名选项，不能沿用旧授权。

审校完成、作者语义问题已解决，且代表界面验收完成或本次剩余风险被明确接受后，才登记：

```powershell
python scripts/check-localization.py --mark-current all
python scripts/check-localization.py --release-gate
```

0.2.2 的具体接受依据见 `docs/localization/GPT6_INDEPENDENT_REVIEW.md` 末尾。本轮沿用未改动的既有译文，不是放行已知漏译。1.20.1 没有另一套源语言目录，构建时继承主线全部 14 个语言文件；准备脚本还逐个比较六份最终 JAR 的语言内容，防止生成资源漏包。

## GitHub Actions

`.github/workflows/publish-curseforge.yml` 支持：

- 推送 `v<版本>` 标签：自动构建、校验并公开上传六个 Release 文件，**推标签就是发布操作**。
- 手动执行：默认 `publish=false`，只构建校验并存 GitHub Artifact；显式打开 publish 才上传。`release_type` 支持 Release / Beta / Alpha。`manual_release=true` 仅暂存待公开文件。

推荐先推发布分支并跑一次不上传的工作流，再在同一提交创建发布标签。CI 先安装 Java 17 工具链，再切 Java 21 构建两条兼容线，最后 Java 25 构建主线。依赖版本冻结，不顺便升级。

上传前必须通过：

1. 三条版本线版本一致、标签严格匹配、CHANGELOG 对应章节存在且非空。
2. 六个精确命名 JAR 都存在；只携带本加载器描述文件，内部模组版本与 Minecraft 声明正确。
3. 许可和署名全文与仓库一致。高版本使用 `*_ECHO_WARRIOR` 命名，1.20.1 使用原始文件名，两者均在 META-INF。
4. 14 个语言资源与审校源文一致，未违反本次本地化门禁。
5. 六份元数据准确标记 Client、Server、游戏版本、加载器和必需前置。
6. 所有 JAR、元数据和哈希清单先保存为 Actions Artifact，再开始上传。

上传使用仓库 Secret `CURSEFORGE_API_TOKEN`，不将令牌复制到本地、日志或仓库。每份上传成功立即记录文件 ID 和链接；即使后续失败也保留响应 Artifact。POST 不自动重试：超时不表示 CF 没有收到文件，必须先查询现状。

## 完成条件与部分失败

CurseForge 不提供六文件原子事务。任何一步失败都不能声称完整发布，不能直接整轮重跑制造重复文件。先检查 Actions 的逐文件结果、保留响应和 CF 实际页面，再决定只补缺失项。

六份均获文件 ID 后，核对 CF 每个文件的版本、加载器、Release 标记及更新日志；待审核和已公开要分别说明。只有确认六份上传成功才把发布分支合并到 main 并推送。回复提供文件结果、GitHub 标签与 main 合并状态。

本流程更新不改变玩法；相应玩法修复在 CHANGELOG / PROJECT / 百科既有条目记录，不为纯发布工具另造百科内容。
