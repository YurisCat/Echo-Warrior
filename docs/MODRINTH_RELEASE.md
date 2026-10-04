# Modrinth 六文件自动发布

Modrinth 与 CurseForge 共用 `.github/workflows/publish-curseforge.yml` 的一次六文件构建，避免分别构建出不同的包。Modrinth 元数据由 `scripts/modrinth-release.py` 从已校验的 CF 清单转换，并再次检查精确文件路径、哈希、加载器描述、许可、署名和打包译文。

## 一次性设置

1. 在 https://modrinth.com/settings/pats 创建发布 Token，只授权 `PROJECT_READ`、`VERSION_READ`、`VERSION_CREATE`。日常发布不需要删除、财务、账号管理或项目修改权限。Token 到期后只需轮换 Secret。
2. 在 https://github.com/YurisCat/Echo-Warrior/settings/secrets/actions 保存仓库 Secret `MODRINTH_API_TOKEN`。直接在 GitHub 填入值，不写入代码、本地文件、聊天或 Actions 输出。
3. 首次通过 Modrinth 页面创建 `Echo Warrior` 项目草稿，slug 使用 `echo-warrior`，类型为 Mod。使用现有人工制作图标、宣传素材和真实游戏截图，保留署名。页面内容参考 `docs/MODRINTH_DESCRIPTION.md`，设源码链接为 `https://github.com/YurisCat/Echo-Warrior`，许可证为 Custom，说明代码与素材的混合许可。
4. 勾选平台的 **Contains AI-generated content**，具体披露辅助开发、翻译和部分页面准备；玩法与设计、美术、模型、动画、GUI、物品图标和宣传美术按实际人工贡献署名。不能仅靠正文一句说明替代平台披露字段。
5. 获得项目稳定的八位 ID 后，在仓库 Actions **Variables** 保存 `MODRINTH_PROJECT_ID`。它不是密钥；不要填 slug 或网址。该变量启用未来 `v*` 标签的 Modrinth 自动上传。
6. 上传六个首次版本后再通过页面提交项目审核。日常版本上传 Token 无需扩大到 `PROJECT_WRITE`；首次审核与页面修改保留为明确的作者操作。API 接受版本、项目处于审核中、公开可见分别记录，不能混为同一个结果。

截至 2026-10-04：作者已保存 `MODRINTH_API_TOKEN`，Secret 名称已核对；首次项目 ID 尚未登记。不为缺失的 ID 填占位值，也不把尚未上传或审核的项目说成已上架。

2026-10-05，[GitHub Actions 只读验证](https://github.com/YurisCat/Echo-Warrior/actions/runs/37215490592)通过：保存的 Token 具有真实私有项目读取能力；11 项 CF 与 22 项 Modrinth 离线检查、本地化门禁均通过。此轮没有上传文件，VERSION_CREATE 权限仍留待首次正式发布实证。

## 触发方式

- 推送新的 `v<mod_version>` 标签：上传 CF；配置 `MODRINTH_PROJECT_ID` 后，同一工作流也上传 Modrinth。
- 手动运行：默认不上传。`publish` 控制 CF，`publish_modrinth` 控制 Modrinth，两者可独立选择。
- 首次补发现有 `0.2.2` 到 Modrinth：在包含本工作流的提交上手动运行，选择 `publish=false`、`publish_modrinth=true`。不重推 `v0.2.2`，不重复发布已有 CF 文件。
- `manual_release` 只控制 CF 待公开标记，不控制 Modrinth；Modrinth 的版本使用 `listed`，项目草稿/审核状态仍由平台管理。
- `allow_pending_localization` 仅在作者明确豁免当次发布的缺失或过期译文后开启，不能复用历史豁免。硬错误始终阻止上传。

## 无上传验证

`Check Modrinth configuration` 工作流运行离线守卫、本地化门禁，再用 Secret 执行只读 API 检查。变更该工作流或 Modrinth 脚本的直接 push 会触发检查，也支持手动执行。

Token 检查比较携带 Token 与匿名请求可见的 YurisCat 项目 ID；至少出现一个匿名不可见的项目才证明 Token 与 `PROJECT_READ` 有效。单独 HTTP 200 不能证明鉴权成功，因为 Modrinth 的公开读取接口可能把无效 Token 当作匿名请求。检查不会输出私有项目详情。如果账号已经没有私有项目，改用正式项目的 preflight；创建版本权限只在实际授权上传时才能实证。

本地准备仍先运行既有 CF 准备器：

```powershell
python scripts/check-localization.py --release-gate
python scripts/test_curseforge_release.py
python scripts/test_modrinth_release.py
python scripts/prepare-curseforge-release.py --require-jars
python scripts/modrinth-release.py prepare
```

不填 ID 时可以生成六份本地预览元数据，不能上传。已有正式包可用于元数据验证，但不得从有未提交代码修改的工作区重建同名正式版本。正式 CI 沿用 CF 的 Java 25 / 21 / 17 工具链和提交约束。

## 上传与恢复

- 六个独立版本号形如 `0.2.2+mc1.20.1-forge`，JAR 内仍是 `0.2.2`；每个版本只有一个准确加载器与 Minecraft 版本的主文件。
- 每个版本登记 GeckoLib、SmartBrainLib 为必需依赖；Fabric 另登记 Fabric API。上传前查询 Modrinth，确认每个组合有对应依赖文件；不捆绑前置、不承诺任意新版依赖都兼容。
- 第一次 POST 前检查两平台需要的 Secret/变量，并完成 Modrinth 项目身份、源码网址、自定义许可、依赖可用性和已有版本检查。随后先 CF、再 Modrinth；任一步失败，整个发布任务失败。
- 已有相同版本号必须具有完全相同的版本元数据、必需依赖及主文件名、大小、SHA-512，才复用已有版本 ID。发现差异或同 JAR 使用其他版本号时停止，不覆盖、不删除、不重复创建。
- 每个 POST 只执行一次。发起请求前保存“不确定远端结果”的记录，收到版本 ID 后立即保存，再校验响应和读取远端版本。网络超时不代表没有上传成功。
- `build/modrinth/responses/` 在失败后也保存为 Actions Artifact。六个版本最后再次查询全部确认，才写 `complete.json` 和完整发布 Summary。审核状态独立说明。
- Modrinth 部分失败后的恢复应手动选择 **CF 不上传、MR 上传**，脚本只补缺失项；不要重新跑 CF POST。CF 的部分失败继续遵循 `docs/CURSEFORGE_RELEASE.md`。
- 两平台不提供共同原子事务；不能因一平台成功就声称两平台完成发布。初次配置不改变游戏行为、控制或平衡，因此无需修改回声档案馆。

## 官方依据

- [Modrinth API 鉴权和 User-Agent](https://docs.modrinth.com/api/)
- [创建版本的 multipart 格式](https://docs.modrinth.com/api/operations/createversion)
- [Token 权限定义](https://github.com/modrinth/code/blob/main/apps/labrinth/src/models/v3/pats.rs)
- [AI 使用及披露要求](https://support.modrinth.com/en/articles/16551575-disclosure-and-usage-of-ai)
- [GitHub Actions Secret 设置](https://docs.github.com/en/actions/security-for-github-actions/security-guides/using-secrets-in-github-actions)
