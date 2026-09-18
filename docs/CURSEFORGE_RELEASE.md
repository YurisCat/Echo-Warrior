# CurseForge 四文件发布流程

CurseForge 项目 ID 为 `1677436`。正式发布从同一版本、同一 Git 提交构建四个独立 JAR：Minecraft 26.1.2 Fabric、Minecraft 26.1.2 NeoForge、Minecraft 1.21.1 Fabric 和 Minecraft 1.21.1 NeoForge。依赖 JAR 始终保持外置，不打入 Echo Warrior 包内。

## 本地发布候选

发布前必须执行以下检查：

```powershell
# Minecraft 1.21.1 / Java 21
.\scripts\build-1.21.1.ps1 -Loader Dual -Clean
.\scripts\check-1.21.1-baseline.ps1 -SkipBuild
.\scripts\smoke-test-1.21.1-servers.ps1 -Loader Dual -SkipBuild

# Minecraft 26.1.2 / Java 25
$env:JAVA_HOME = (Resolve-Path '.toolchains\jdk-25').Path
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat clean dualBuild

# 四个 JAR 与四份 CurseForge 元数据
python .\scripts\prepare-curseforge-release.py --release-type release --require-jars
```

客户端冒烟检查使用 `scripts/run-test-client.ps1`。每个 Minecraft 版本至少完成一次真实交互检查，其余加载器必须完成自动启动检查；脚本结束后确认没有遗留的开发客户端进程。

内部测试包、`temporary-delivery/` 文件、来源包以及工作区不干净时生成的临时候选不得上传。正式候选必须来自已经提交的发布分支。

## GitHub Actions 自动发布

`.github/workflows/publish-curseforge.yml` 支持两种入口：

- 推送与两个 `gradle.properties` 中共同 `mod_version` 完全一致的 `v<版本>` 标签时，自动构建并公开上传四个 Release 文件；
- 手动运行时默认只执行四端构建、JAR/元数据校验和 GitHub Artifact 留档。只有明确打开 `publish` 才上传；`release_type` 可选择 Release、Beta 或 Alpha，`manual_release` 可让四个已审核文件等待作者稍后手动公开。

自动化按以下顺序执行：

1. 在标签对应的同一提交上用 Java 21 构建 Minecraft 1.21.1 Fabric 与 NeoForge；
2. 切换 Java 25，构建 Minecraft 26.1.2 Fabric 与 NeoForge；
3. 要求根目录和 `versions/1.21.1/` 使用同一 `mod_version`，并要求标签严格等于 `v<版本>`；
4. 只接受四个精确命名的正式 JAR，拒绝来源包，检查每个 JAR 只携带自己的加载器描述文件、准确的 `mod_version` 与完整许可/署名文件；
5. 生成四份 CurseForge 元数据。Fabric 文件声明 Fabric API、SmartBrainLib 和 GeckoLib，NeoForge 文件声明 SmartBrainLib 和 GeckoLib；每份文件明确标记对应 Minecraft 版本与加载器；
6. 将四个 JAR 和四份元数据保存为 GitHub Actions Artifact；
7. 需要上传时依次调用 CurseForge 文件上传接口。任一步失败，工作流立即失败，不能把发布视为完成；
8. 四个 CurseForge 文件均成功后，人工核对文件 ID，再将发布分支合并回 `main` 并推送。

CurseForge 的四个文件是四次独立上传，平台没有本项目可用的原子“四文件事务”。如果网络在上传之间中断，必须先查询 CurseForge 已经出现哪些文件，再决定是否补传；不得直接重新运行并制造重复文件，也不得宣称四端发布完成。

## 正式发布检查

- 根目录与 `versions/1.21.1/gradle.properties` 的 `mod_version` 完全一致；
- `CHANGELOG.md` 存在与 `mod_version` 一致且非空的版本章节；
- 四个 JAR 来自标签所指向的同一提交；
- Minecraft 26.1.2 使用 Java 25，Minecraft 1.21.1 使用 Java 21；
- 四份元数据分别声明正确的 Minecraft 版本、加载器和依赖；
- `CURSEFORGE_API_TOKEN` 只保存在 GitHub Actions Secret；
- GitHub Actions 显示四个文件 ID 后，才合并发布分支；
- 回复中明确说明四个 CurseForge 文件结果、GitHub 标签以及发布分支是否已经合并进 `main`。
