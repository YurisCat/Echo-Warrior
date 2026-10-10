# 回收箱渲染自测误报复测（2026-10-10）

继续 [Apothic 整合包检修](apothic-pack-reproduction-2026-10-10.md)。用户要求解释“独立渲染自测断言失败”并复测；本次在 FOXY-NODE 使用同一分享包和隔离 CATTEST。

## 结论与根因

**144 顶点是模型 72 + ShadowDrop 阴影 72，自测错误地合计了两种通道。** 原回收箱模型、纹理和渲染器无需修复。异常由仅在自动测试启动时执行的断言抛出，不是普通玩家打开回收箱就会触发的异常。

原代码将所有 `MultiBufferSource.getBuffer(RenderType)` 请求返回同一个计数器。已检查原包 `shadowdrop-3.0.1+1.20.1-forge.jar` 的实际字节码：`ItemRendererMixin.shadowdrop$renderShadow` 再次渲染物品，`ShadowBufferSource` 将几何送入独立 `shadowdrop_shadow` 材质。运行日志也明确显示该阴影通道 72、箱子 `entity_cutout` 通道 72。

## 改动

- `ExplorationClientSelfTest1201` 按 RenderType 分组；箱子本体通道仍严格要求 72，附加通道另行记录。没有把断言放宽为大于等于或任意倍数，没有删除检查，也没有对 ShadowDrop 添加运行时依赖。
- 新增 `RecyclerClientSelfTest1201`：在可恢复的隔离测试平台放置回收箱和普通箱作参照，走实际客户端使用/点击数据包打开三行菜单，存入再取出三颗钻石，分别检查服务端内容、物品守恒及客户端箱盖打开/关闭，最后恢复现场。启动器要求该自测 PASS，产物基线拒绝遗漏该类的包。
- 可选 `-Decho_warrior.recycler_screenshots=true` 保存游戏自身渲染目标的画面，涵盖落地方块、手持、GUI 图标、打开的箱盖与真实容器。仅自动测试使用，不改变正常游戏流程。
- 本轮相对已通过六次专服回归的 Apothic 候选，仅四个客户端自测相关 class 文件改变，所有服务端 class 字节一致；没有重跑未改变的长专服套件。
- 玩法、回收收益、实际渲染器、资源、本地化、百科和版本号均不变。可复用根因归入 PORT-009。

## 同包对照

两次使用完全相同的候选 JAR 和 73 份 config/defaultconfigs/datapacks 文件，唯一依赖差异是是否装入 ShadowDrop（SHA-256 `00e666317cfc6d25063e49b920ecfe3019310f62ddcba29cf8c023a00f34dfea`）。Apothic 均启用。

| 项目 | 原包 + ShadowDrop | 仅移除 ShadowDrop |
| --- | --- | --- |
| 本体材质通道 | 72 | 72 |
| 附加阴影通道 | 72 | 0 |
| 五英雄预览/召唤及既有客户端检查点 | 通过 | 通过 |
| 回收箱 27 格、存入/取回 3 钻石、箱盖开合 | 通过 | 通过 |
| 正常请求退出、保存所有维度 | 通过 | 通过 |
| 启动器严格零 ERROR 总门禁 | 未通过：其他模组原有资源/配方/进度错误 | 未通过：同类原有错误 |

原包另外包含模型缺失、非法声音路径、书本/配方/进度数据错误。Crash Assistant 还把 Java 的 `Picked up JAVA_TOOL_OPTIONS` 标成 ERROR。保留日志和严格门禁，未用白名单掩盖这些条目；不能将本次“回收箱与本体客户端检查点通过”宣称为第三方整合包所有错误已修复。

任务及证据：

- 有 ShadowDrop：`apothic-pack-recycler-with-shadow-20261010T040539Z`；ZIP SHA-256 `0b0282cf2134cacc1d8461461e3f81a250362f7a169f7af43721a34c820ee8be`。
- 无 ShadowDrop：`apothic-pack-recycler-no-shadow-20261010T040900Z`；ZIP SHA-256 `2b969e42bba884d7cfa8e1e4774cc5ecb17fcb71db8290b53d336bb2481ed10e`。
- 本地证据：`build/recycler-render-2026-10-10/{with-shadow,no-shadow}/`，其中包含截图、启动清单及日志；`comparison.json` 记录输入一致性和两个通道的对照。
- 已目视检查有 ShadowDrop 的两张截图：箱体/箱盖/卡扣完整，手持和 GUI 图标正常，菜单显示三颗钻石，背景可见打开的箱盖。截图上的其他模组教程/进度提示不作为回收箱异常。

## 当前候选与回归

双加载器构建、产物检查、52 项损坏包/资源等价测试通过。普通 Fabric 客户端任务 `apothic-pack-recycler-clean-fabric-20261010T041157Z` **完整通过**，包括严格日志门禁、回收箱本体 72/附加 0、实际菜单存取、五英雄及其余客户端检查点、正常保存退出和进程清理。版本为 Fabric Loader 0.19.5、Fabric API 0.92.12，只有本体与必需依赖。报告 `passed=true`、`cleanup_passed=true`；证据 ZIP SHA-256 `49decfa94b6de171efcd9715ce071d0ba3ef0638454c56f1185cb83b028b2bab` 已取回核验，位于 `build/recycler-render-2026-10-10/clean-fabric/`。最终独立查询确认 FOXY Java 进程为 0、任务锁已释放。

| 未发布候选 | SHA-256 |
| --- | --- |
| Forge 1.20.1 / 0.2.4 | `81589e8310c0e540a1509c0cb9048edf91a9989103ffa7866a84c74b541110b5` |
| Fabric 1.20.1 / 0.2.4 | `d929c55dd9f6e06cb1cf525cba3ba2656af839d6d0290b7567f3643b743a95b7` |

用户无需为了诊断此断言再手动复现。正式发布前仍可按既有人工清单验收个人画质设置下的观感；本次没有发布、提交或联系报告者。
