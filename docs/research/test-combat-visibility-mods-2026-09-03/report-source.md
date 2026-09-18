# Minecraft 26.1.2 战斗可视化测试模组研究（内部来源稿）

- 受众：Echo Warrior 开发者与测试工作人员
- 研究日期：2026-09-03
- 项目目标环境：Minecraft 26.1.2、Fabric Loader 0.19.3、NeoForge 26.1.2.100、Java 25
- 目标：寻找无需改动 Echo Warrior、能直观看到实体血量与伤害反馈的可选测试模组
- 排除：不下载文件；不把第三方模组纳入 Echo Warrior 正式运行依赖；不推荐会改变伤害公式或玩法的模组

## 直接结论

### 第一推荐：Health Bars（Fuzs）

这是最适合双加载器测试交付的一体化方案。它在 Minecraft 26.1.2 上同时提供 Fabric 与 NeoForge 正式版，属于纯客户端模组；功能同时包含目标 HUD、实体头顶血条、伤害与治疗数字、护甲信息和快速开关。26.1.x 分支在作者仓库中标为 Maintained。

对应文件：

- Fabric：`HealthBars-v26.1.1-mc26.1.x-Fabric.jar`
- NeoForge：`HealthBars-v26.1.1-mc26.1.x-NeoForge.jar`

必要依赖：

- 两端：Puzzles Lib（选择标注兼容 26.1.x / 26.1.2 的同加载器版本）
- Fabric：Fabric API、Forge Config API Port
- NeoForge：无需 Fabric API / Forge Config API Port

优点：同一功能与配置思路覆盖两个测试渠道；能观察自定义 LivingEntity 的血量变化；只装客户端，不要求修改测试服务器；可把 HUD 与世界内显示分别调节。

限制：实体模型预览、发光、光影和其他实体渲染模组存在潜在兼容风险。公开问题中能看到 26.2 + Iris 光影下黑色血条，以及个别模组实体/渲染组合的问题；这些不是 26.1.2 Echo Warrior 的已证实问题，但足以支持测试时关闭模型预览、额外轮廓和非必要粒子，并保留 `latest.log`。

### Fabric 优先替代：Provi's Health Bars

如果测试流程主要使用项目默认的 Fabric 客户端，Provi's Health Bars 是更成熟、界面也更偏测试工具的一体化替代：Minecraft 26.1.2 对应 `2.4.3+26.1.2`，纯客户端，提供当前目标 HUD、世界内血条，以及伤害/治疗粒子；这些粒子被设计为出现在目标左右，减少遮挡。

限制：官方 26.1.2 文件仅覆盖 Fabric / Quilt，不能用作 Fabric 与 NeoForge 的统一测试方案。

### 模块化替代：Neat + HICUDAN

适合希望分别开关“血条”和“伤害跳字”、尽量减少对技能原始视觉效果干扰的测试员。

- 血条：Neat `26.1.2-48`，Fabric 与 NeoForge 都有正式版，纯客户端；显示实体名称、生命、护甲，并可用快捷键关闭。
- 跳字：Highly Customizable Damage Numbers（HICUDAN）`v1.1.3+26.1`，Fabric 与 NeoForge 都有 26.1.2 正式文件，纯客户端且无额外依赖；可调字体、颜色、动画、大小、持续时间和按伤害缩放。

代价是需要维护两个模组与两份配置，但测试时可以独立排除“血条渲染”和“伤害数字渲染”的影响。

## 其他候选与取舍

### Health HUD

功能表面上非常贴合：26.1.x、Fabric / Forge / NeoForge、纯客户端、数字/百分比/心形生命显示，以及伤害和治疗数字。最新 Fabric 1.1.0 已增加 CodxLib 必需依赖。项目较新、下载与关注量很低，且不同加载器最新文件状态不如第一推荐清晰，因此列为观察候选，不作为团队统一方案。

### Health Indicators

项目成熟、纯客户端、支持 Fabric 与 NeoForge，能显示条形或数值生命和护甲，并提供丰富筛选。26.1.x 当前文件为 `26.1.0.0-beta`；它主要解决“血量指示”，不是独立的每次伤害跳字，因此若选择它仍需搭配伤害数字模组。当前 26.1 分支为 beta，使它在本次推荐中落后于 Neat。

### Mob Health Display

精确支持 Minecraft 26.1.2 + Fabric + Java 25，纯客户端且有中英文配置，目标 HUD 很丰富。但项目仍标为公开测试版，并包含目标轮廓、受击染色和大量战斗粒子；这些视觉增强会干扰对 Echo Warrior 自身技能粒子、命中染色和动画的观察。只适合关闭大部分效果后做 Fabric 单端尝试。

### Floating Damage Indicators / Damage Engine

这类方案更重视服务器提供的伤害类型、来源或会话统计。Floating Damage Indicators 对 26.1.2 有 Fabric 与 NeoForge 文件，并能按普通、暴击、投射物、火焰、毒、凋零等类型着色，但多人环境要求客户端和服务器都安装。Damage Engine 提供总伤害、连击、历史、目标面板、浮字和调试输出，也以双端安装获得完整精度。它们适合开发者专项诊断，不适合作为零侵入的测试员默认配置；还需单独验证英灵代替玩家造成伤害时的归属显示。

### 不推荐旧 ToroHealth

ToroHealth Damage Indicators 是这类模组的经典来源，但官方 CurseForge 项目最后更新于 2022-07-23，最高只到 Minecraft 1.19.1，不支持本项目 26.1.2。不要把旧教程中的 ToroHealth 文件交给当前测试员。

## 建议配置基线

无论采用哪一套，团队应固定一份截图或配置文件作为测试基线：

1. 生命显示使用数值或“当前 / 最大”，保留至少 1 位小数（模组支持时）。
2. 开启伤害与治疗数字；关闭按伤害夸张缩放、屏幕震动和花哨暴击动画。
3. 血条只对准星目标、被攻击目标或最近受伤实体显示，避免多人/群怪场景铺满屏幕。
4. 关闭实体模型预览、目标轮廓、额外受击染色、替换原版暴击粒子和 Boss 条。
5. 测试“技能视觉表现”时临时一键关闭第三方血条；测试“命中时机/数值”时再打开。
6. 用无护甲目标与有护甲/抗性目标分别测试。客户端浮字通常反映观察到的最终生命变化，不能替代服务端日志、GameTest 或确定性的伤害断言，也不总能证明伤害来自哪名英灵或哪段技能。

## 项目边界

这些模组应仅作为测试员客户端的可选工具：不加入 Echo Warrior 的 `depends`、不打进发布 JAR、不作为正式玩法的一部分。研究本身没有改变玩家可见玩法、控制或平衡，因此不需要同步更新回声档案馆，也不需要更新 `PROJECT.md`。

## 证据差异与信心

- 高信心：Health Bars、Provi's Health Bars、Neat、HICUDAN 的 26.1.2 文件、加载器和客户端/依赖属性；均由官方 CurseForge / Modrinth 页面或作者仓库交叉核验。
- 中等信心：任意客户端血量差分模组对所有 Echo Warrior GeckoLib 实体都不会出现渲染问题。通用 LivingEntity 支持使其大概率可用，但必须实际跑一次五名英灵和群战场景。
- 低信心/未验证：服务器感知型模组能否把英灵造成的伤害正确归属于召唤者；候选描述主要围绕“玩家命中”。

## 检索停止条件

已完成以下答案槽：26.1.2 精确版本、Fabric/NeoForge 覆盖、客户端/服务端要求、核心功能、依赖、维护状态、视觉干扰和团队使用建议。首选与替代方案的证据已收敛；继续增加低下载量同类候选不太可能改变结论，因此停止扩展检索。

## 来源账本

| 论点 | 来源 | 发布者 | 页面日期/状态 | URL | 访问说明 |
|---|---|---|---|---|---|
| Health Bars 功能、客户端环境、两加载器、26.1.2 文件与依赖 | Health Bars | Fuzs / CurseForge | 更新 2026-06-19 | https://www.curseforge.com/minecraft/mc-mods/health-bars | 页面与文件条目可访问 |
| Health Bars 26.1.x 仍维护 | health-bars repository | Fuzss / GitHub | 26.1.x 标为 Maintained | https://github.com/Fuzss/health-bars | README 与 26.1.x 分支可访问 |
| Health Bars 公开渲染问题边界 | health-bars issues | Fuzss / GitHub | 2026-03 至 2026-08 公开问题 | https://github.com/Fuzss/health-bars/issues | 作为风险信号，不推断为本项目必现 |
| Puzzles Lib 26.1.2 双加载器可用 | Puzzles Lib | Fuzs / CurseForge | 最新 26.1.x 文件更新 2026-08-05 | https://www.curseforge.com/minecraft/mc-mods/puzzles-lib | 页面与文件列表可访问 |
| Fabric 配置依赖支持 26.1.x | Forge Config API Port | Fuzs / Modrinth | 更新 2026 年 | https://modrinth.com/mod/forge-config-api-port/versions | 版本兼容页可访问 |
| Provi 26.1.2、Fabric/Quilt、客户端与功能 | Provi's Health Bars | Provismet / CurseForge | 26.1.2 文件 2026-05-16；项目更新 2026-06-25 | https://www.curseforge.com/minecraft/mc-mods/provihealth | 页面与文件列表可访问 |
| Neat 26.1.2 Fabric 正式文件 | Neat (Fabric/Quilt) | Vazkii / CurseForge | 2026-06-19 | https://www.curseforge.com/minecraft/mc-mods/neat-fabric-quilt | 文件列表可访问 |
| Neat 26.1.2 NeoForge 正式文件与功能 | Neat | Vazkii / CurseForge | 2026-06-19 | https://www.curseforge.com/minecraft/mc-mods/neat | 页面与文件列表可访问 |
| HICUDAN 26.1.2 双加载器、纯客户端、无依赖与配置范围 | Highly Customizable Damage Numbers | DevK75L / CurseForge | 26.1.2 文件 2026-04-02 | https://www.curseforge.com/minecraft/mc-mods/hicudan | 页面与文件列表可访问 |
| Health HUD 功能、平台与最新 Fabric 依赖 | Health HUD | codx / Modrinth | 1.1.0 发布 2026 年中 | https://modrinth.com/mod/health-hud | 项目与版本页可访问 |
| Health Indicators 26.1 beta 与功能 | Health Indicators | AdyTech99 / Modrinth | 26.1 beta 更新 2026-06-20 | https://modrinth.com/mod/health-indicators | 项目/版本页可访问 |
| Mob Health Display 精确环境、beta 与视觉功能 | Mob Health Display | MoyuHero / Modrinth | 2026 年公开 beta | https://modrinth.com/mod/mob-health-display | 中英文描述可访问 |
| Floating Damage Indicators 精确版本、双端要求与类型着色 | Floating Damage Indicators | 3CTeam / CurseForge | 更新 2026-08-27 | https://www.curseforge.com/minecraft/mc-mods/floating-damage-indicators | 页面与文件列表可访问 |
| Damage Engine 的诊断模块和双端精度要求 | Damage Engine | mitama / Modrinth | 2026 年持续更新 | https://modrinth.com/mod/damage-engine | 项目描述可访问 |
| ToroHealth 不支持当前版本 | ToroHealth Damage Indicators | ToroCraft / CurseForge | 最后更新 2022-07-23 | https://www.curseforge.com/minecraft/mc-mods/torohealth-damage-indicators | 官方旧项目页可访问 |
