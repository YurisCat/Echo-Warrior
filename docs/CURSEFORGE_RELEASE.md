# CurseForge 自动发布

本项目使用 `.github/workflows/publish-curseforge.yml` 构建并发布 CurseForge 文件。CurseForge 项目 ID 固定为 `1677436`。

## 一次性设置

1. 在 CurseForge Authors 的 API Tokens 页面生成上传令牌。
2. 打开 GitHub 仓库的 `Settings` → `Secrets and variables` → `Actions`。
3. 新建 Repository secret，名称必须为 `CURSEFORGE_API_TOKEN`，值为刚生成的令牌。

令牌不得写进 `.env`、工作流文件、文档、Issue、构建日志或 Git 历史。需要更换令牌时，只更新 GitHub Secret。

## 第一次发布

1. 打开 GitHub 仓库的 `Actions` → `Publish CurseForge` → `Run workflow`。
2. 首次保持 `publish` 未勾选，运行一次演练。工作流会使用 Java 25 执行干净构建、核对版本和 Changelog，并保留验证后的 JAR，但不会上传 CurseForge。
3. 确认演练成功后再次运行，勾选 `publish`。正式版保持 `release_type = release`；通常不勾选 `manual_release`。

新 CurseForge 项目的第一个文件会连同项目进入平台审核。`manual_release` 未勾选时，文件审核通过后会自动公开；勾选后则需要作者再手动放出。

## 后续版本自动发布

每次发布前同时完成以下三项：

1. 把 `gradle.properties` 中的 `mod_version` 更新为新版本，例如 `0.1.1`。
2. 在 `CHANGELOG.md` 中添加完全匹配的二级标题，例如 `## 0.1.1 - 2026-09-15`。
3. 确认代码已经提交并推送后，为同一提交创建并推送标签 `v0.1.1`。

标签必须严格等于 `v` 加 `mod_version`。标签推送后，GitHub Actions 会自动构建并上传；版本不一致、Changelog 缺失或正式 JAR 未生成时，工作流会在上传前失败。

```powershell
git tag v0.1.1
git push origin v0.1.1
```

工作流会把 Minecraft 版本、Fabric、客户端/服务端兼容性和三个必需依赖一并提交给 CurseForge：

- Fabric API（项目 ID `306612`）
- SmartBrainLib（项目 ID `661293`）
- GeckoLib（项目 ID `388172`）

发布上传只使用 `build/libs/echo-warrior-<版本>.jar`，不会上传源码包、测试目录、`human-work/` 或百科网页导出物。
