# Minecraft 26.3 移植评估

核对日期：2026-11-13（Asia/Shanghai）。本次只评估可行性，没有新增兼容目录、改变主线或承诺发布日期。

同日后续：作者已批准开始移植。以下保留评估时点，实施与实际验收以 [实施记录](port-26.3-implementation-2026-11-13.md) 为准。

## 结论

可以新增 26.3 Fabric／NeoForge 维护线。主要前置已有目标版本，没有发现必须等待上游发布的障碍；建议从 26.1.2 主线移植。工程判断为**中等偏大的首次适配**，不能仅修改版本号。未做目标版编译或启动，以下不是“已兼容”结论。

## 已核实的依赖

直接读取各项目官方 Maven 元数据，另以作者发布页交叉核对 SBL／GeckoLib。以下是本次查到的可选版本，不自动更新现有构建：

| 项目 | 26.3 可用版本 | 证据 |
| --- | --- | --- |
| Fabric Loader | 0.20.1；另列有 0.19.5 | 官方 Meta API 的 26.3 loader 清单 |
| Fabric API | 0.166.2+26.3 | Fabric Maven 元数据 |
| NeoForge | 26.3.0.103 | NeoForge Maven 元数据 |
| SmartBrainLib | 2.0.3，Fabric／NeoForge 均有 | 官方 Cloudsmith Maven，两加载器分别查询；CF 作者发布页一致 |
| GeckoLib | 5.5.7，Fabric／NeoForge 均有 | 官方 Cloudsmith Maven，两加载器分别查询；作者 Wiki 一致 |

Fabric 的 26.3 指南推荐 Loom 1.17／Gradle 9.6.0（指南发布日期时的组合）；仓库 Wrapper 当前为 Gradle 9.5.1。实际试移植时应在目标线隔离验证工具链，而非为了评估升级现有三线。公开元数据与源码扫描原件留在忽略目录 `build/port-26.3-assessment/`。

## 与本项目相关的工作

| 部分 | 已有代码与目标差异 | 预计工作 |
| --- | --- | --- |
| GUI／按键 | `EchoSkillTooltips` 直接调用 GLFW 检测左右 Alt；召唤器等页面使用 GLFW 鼠标常量。26.3 改用 SDL3，按键与鼠标编码改变。扫描发现 4 个相关 Java 文件 | 适配按住／松开 Alt、左右键、翻页与焦点；实机验证 |
| 渲染／特效 | 武士残影自定义 RenderPipeline／RenderType；26.2 改动渲染管线，26.3 又迁移至 Renderpearl。扫描发现 9 个引用渲染 API 的文件 | 逐项适配材质、混合、透明度及 GUI 渲染；双加载器客户端验收 |
| 注册／数据／世界 | 实体注册、考古与战利品、物品组件、绑定保存需要对照 26.2 和 26.3 两份迁移说明；目标数据包 121.0、资源包 97.1 | 验证加载、生成、刷取、遗物／召唤器读写与旧存档副本升级 |
| Mixin／可选兼容 | 当前主线有 35 处 `@Mixin`，包含可选宠物模组适配 | 审核目标方法和注入点；第三方联装需另外核实其目标版，不能由基础依赖可用推断 |
| 玩法内容 | 五英雄技能、数值、协助战斗、饰品、译文与人工美术可作为移植基准 | 保持当前玩法语义并运行回归，无需借移植重做平衡或绘制全部素材 |
| 构建／发行／测试 | 当前 3 条 MC 线、6 个加载器包 | 增加后为 4 条线、8 个包；扩展发布矩阵、哈希与依赖检查、FOXY 专服／客户端验收 |

文件数量只是定位改动面的源码扫描，不能当作编译错误数或工期承诺。增加两个包也不意味着维护时间精确增加三分之一；新版本初期的 API 差异会额外占用调试时间。

## 建议顺序

先保留现有三线，把本次已定位的旧线跟随问题作为独立修复。若作者决定新增 26.3，再按移植手册完成 Fabric 的注册→召唤→GUI→战斗→动画→保存纵向验证，随后补 NeoForge 和四线对齐。正式发布前仍需翻译门禁及狐狸节点测试。

## 官方来源

- [Minecraft Java Edition 26.3 发布说明](https://www.minecraft.net/en-us/article/minecraft-java-edition-26-3)：2026-09-15 发布，输入后端和资源／数据格式。
- [Fabric for Minecraft 26.3](https://www.fabricmc.net/2026/09/15/263.html)：工具链、API 移除及组件变动。
- [NeoForged 26.1.x → 26.2 迁移说明](https://docs.neoforged.net/primer/docs/26.2/)。
- [NeoForged 26.2 → 26.3 迁移说明](https://docs.neoforged.net/primer/docs/26.3/)：SDL、Renderpearl、注册与数据变化。
- [SmartBrainLib 作者发布页](https://www.curseforge.com/minecraft/mc-mods/smartbrainlib/files/all)。
- [GeckoLib 官方支持表](https://github.com/Tslat/Geckolib-Wiki/blob/main/docs/index.mdx)。
- [Fabric Loader API](https://meta.fabricmc.net/v2/versions/loader/26.3)、[Fabric API Maven](https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/maven-metadata.xml)、[NeoForge Maven](https://maven.neoforged.net/releases/net/neoforged/neoforge/maven-metadata.xml)。
- SmartBrainLib 官方 Maven：`https://dl.cloudsmith.io/public/tslat/sbl/maven/net/tslat/smartbrainlib-{fabric|neoforge}-26.3/maven-metadata.xml`。
- GeckoLib 官方 Maven：`https://dl.cloudsmith.io/public/geckolib3/geckolib/maven/com/geckolib/geckolib-{fabric|neoforge}-26.3/maven-metadata.xml`。
