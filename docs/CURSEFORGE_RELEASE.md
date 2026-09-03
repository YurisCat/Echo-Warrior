# CurseForge 双端发布状态

当前 CurseForge 自动上传处于**主动禁用**状态。旧流程只能上传 Fabric 单包，已经被改为失败即停，推送 `v*` 标签也不会触发上传。

## 当前可用流程

在明确需要双端输出时，运行：

```powershell
.\scripts\build-dual-candidate.ps1
```

也可以双击 `tools\windows\Build Dual Package.bat`。脚本要求 Git 工作区干净，然后用 Java 25 从同一份源码、同一版本和同一提交构筑 Fabric 与 NeoForge 两个独立 JAR，并把以下文件写入被 Git 忽略的 `temporary-delivery/`：

- Fabric JAR 与 SHA-256；
- NeoForge JAR 与 SHA-256；
- `release-manifest.json`；
- 统一测试用例 HTML。

本地迁移或调试期间可用 `-AllowDirty` 生成非正式验证包；清单会明确记录 `dirty: true` 和 `publishReady: false`，不得当作正式上传候选。

## 未来恢复自动发布的前提

只有在明确下令更新自动发布时，才重新实现 `.github/workflows/publish-curseforge.yml`。新流程至少必须：

1. 对同一干净提交执行双端构筑；
2. 严格识别 Fabric 与 NeoForge JAR，拒绝源码包和其他产物；
3. 为两个文件分别提交正确的加载器、游戏版本和依赖关系；
4. 任一构筑、元数据校验或上传失败时停止，不能只发布其中一端；
5. 上传成功后再把双端发布分支合并回主分支，并在回复中明确说明已经合并。

在上述流程落地并经过演练前，不应绕过当前护栏恢复旧的 Fabric 单端上传。
