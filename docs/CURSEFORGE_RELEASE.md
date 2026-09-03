# CurseForge 双端发布流程

CurseForge 项目 ID 为 `1677436`。正式发布使用同一版本、同一 Git 提交产生的 Fabric 与 NeoForge 两个独立 JAR；依赖 JAR 始终保持外置，不打入 Echo Warrior 包内。

## 本地双端候选

在明确需要双端输出或交给测试人员时，运行：

```powershell
.\scripts\build-dual-candidate.ps1
```

也可以双击 `tools\windows\Build Dual Package.bat`。脚本默认要求 Git 工作区干净，然后用 Java 25 从同一份源码、同一版本和同一提交构筑 Fabric 与 NeoForge 两个独立 JAR，并把以下四个文件写入被 Git 忽略的 `temporary-delivery/`：

- Fabric JAR；
- NeoForge JAR；
- 统一测试用例 HTML；
- 自动生成的测试交接说明，其中记录版本、依赖、分支、提交、SHA-256、本轮重点和结果返还要求。

本地迁移或调试期间可用 `-AllowDirty` 生成非正式验证包；交接说明会明确记录工作区不干净且不可发布，不得把它当作正式上传候选。

## GitHub Actions 自动发布

`.github/workflows/publish-curseforge.yml` 支持两种入口：

- 推送与 `gradle.properties` 中 `mod_version` 完全一致的 `v<版本>` 标签时，自动构筑并公开上传两个文件；
- 手动运行时默认只做双端构筑、JAR/元数据校验和 GitHub Artifact 留档，只有明确打开 `publish` 才上传；`manual_release` 可让两个文件在 CurseForge 审核后继续保持手动发布状态。

自动化按以下顺序执行：

1. 在标签对应的同一提交上用 Java 25 执行 Fabric 与 NeoForge 构筑；
2. 只接受两个精确命名的正式 JAR，拒绝源码包，并检查 Fabric/NeoForge 描述文件没有串端；
3. 分别生成 CurseForge 元数据。Fabric 声明 Fabric API、SmartBrainLib 和 GeckoLib；NeoForge 声明 SmartBrainLib 和 GeckoLib；
4. 把两个验证过的 JAR 和两份元数据保存为 GitHub Actions Artifact；
5. 需要上传时依次调用 CurseForge 文件上传接口。任何一步失败，工作流立即失败，不能把发布视为完成；
6. 两个 CurseForge 文件均成功后，人工确认工作流结果，再把双端发布分支合并回 `main` 并推送。

CurseForge 的两个文件是两次独立上传，平台没有提供本项目可用的原子“双文件事务”。因此如果网络在两次请求之间中断，工作流会失败并停止合并；必须先检查 CurseForge 已出现哪一端，再处理重试，不能直接宣称双端发布完成。

## 正式发布检查

- `CHANGELOG.md` 存在与 `mod_version` 一致的版本章节；
- 双端 JAR 来自标签所指向的同一提交；
- `CURSEFORGE_API_TOKEN` 只保存在 GitHub Actions Secret；
- GitHub Actions 显示两个文件 ID 后，才合并发布分支；
- 回复中明确说明 CurseForge 双端结果、GitHub 标签以及发布分支是否已经合并进 `main`。
