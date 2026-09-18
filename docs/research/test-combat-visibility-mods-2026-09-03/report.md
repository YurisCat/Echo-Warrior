# Echo Warrior 测试用伤害数字与血量模组建议

研究日期：2026-09-03
目标环境：Minecraft 26.1.2，Fabric + NeoForge，Java 25

## 最终推荐

### 1. 团队统一首选：Health Bars（Fuzs）

最符合当前需求：一个模组同时提供目标 HUD、实体头顶血条、伤害/治疗数字和护甲信息；Minecraft 26.1.2 的 Fabric 与 NeoForge 都有正式文件，而且只需安装在测试员客户端。

对应版本：

- Fabric：`HealthBars-v26.1.1-mc26.1.x-Fabric.jar`
- NeoForge：`HealthBars-v26.1.1-mc26.1.x-NeoForge.jar`

依赖：两端都需要 Puzzles Lib；Fabric 还需要项目本来就有的 Fabric API，以及 Forge Config API Port。

适合：希望 Fabric 和 NeoForge 测试员看到同样反馈、使用同一份测试说明。

来源：[CurseForge 项目页](https://www.curseforge.com/minecraft/mc-mods/health-bars) · [作者源码与维护状态](https://github.com/Fuzss/health-bars)

### 2. Fabric 体验优先：Provi's Health Bars

`2.4.3+26.1.2` 精确支持 Fabric 26.1.2。它同时提供当前目标 HUD、世界内血条，以及不会正面遮住目标的伤害/治疗粒子；项目成熟度和使用量都较高。

缺点：没有 NeoForge 版本，不能作为双端统一标准。

来源：[CurseForge 项目页](https://www.curseforge.com/minecraft/mc-mods/provihealth)

### 3. 模块化方案：Neat + HICUDAN

如果希望血条和跳字能完全独立开关：

- `Neat 26.1.2-48`：显示名称、生命和护甲；Fabric 与 NeoForge 均有正式版。
- `HICUDAN v1.1.3+26.1`：显示并高度自定义伤害数字；Fabric 与 NeoForge 均有 26.1.2 文件，纯客户端且无额外依赖。

优点是排查渲染冲突或录制技能原始表现时更灵活；缺点是要维护两个模组与两份配置。

来源：[Neat Fabric](https://www.curseforge.com/minecraft/mc-mods/neat-fabric-quilt) · [Neat NeoForge](https://www.curseforge.com/minecraft/mc-mods/neat) · [HICUDAN](https://www.curseforge.com/minecraft/mc-mods/hicudan)

## 建议测试配置

1. 血量使用数值或“当前 / 最大”；能设置时保留 1 位小数。
2. 打开伤害与治疗数字，关闭夸张缩放、震动和花哨暴击动画。
3. 只对准星目标、被攻击目标或最近受伤实体显示，避免群战铺满屏幕。
4. 关闭实体模型预览、目标轮廓、额外受击染色、替换原版暴击粒子和 Boss 条，避免掩盖 Echo Warrior 自身反馈。
5. 用无护甲与有护甲/抗性目标分别测试；第三方浮字只能作为直观反馈，最终数值与伤害归属仍以服务端逻辑、日志和 GameTest 为准。

## 不建议作为首选

- ToroHealth：经典但官方项目停留在 2022 年和 Minecraft 1.19.1，不支持 26.1.2。
- Health Indicators：26.1.x 当前是 beta，而且主要显示血量，不负责每次伤害跳字。
- Mob Health Display：仅 Fabric、仍为公开 beta，轮廓/染色/粒子较多，容易干扰技能视觉测试。
- Damage Engine / Floating Damage Indicators：诊断信息更强，但完整多人精度需要服务器也安装；适合开发者专项排查，不适合测试员默认零侵入配置。

## 项目使用边界

这些模组只应作为测试员客户端的可选工具，不加入 Echo Warrior 正式依赖、不打入发布 JAR。它们没有改变项目玩法，因此本次无需更新 `PROJECT.md` 或回声档案馆。
