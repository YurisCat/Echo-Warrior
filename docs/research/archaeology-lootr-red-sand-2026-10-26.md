# 考古罗盘、Lootr 与可疑红沙 — 2026-10-26

后续发布状态：本次功能及补齐的翻译已纳入 [0.2.10](../releases/0.2.10.md)。正式六包另完成同提交构建和狐狸节点原包检查，包括本记录功能阶段尚未运行的主线 NeoForge；下文候选哈希、早期失败与人工待验项目保留，不冒充正式包或人工签收。

## 反馈与边界

玩家在 2026-10-21 报告：可疑沙子刷出物品后仍占据罗盘目标，破坏后才推进；可疑草正常。作者没有反馈者的 Minecraft、加载器、Lootr 版本或整合包。

本轮使用官方 Lootr **1.21.1-1.11.38.127** 的 Fabric / NeoForge 发行包检查相同行为。不能把这个固定组合的结果称为原整合包复现，不能外推为所有 Lootr 版本兼容。1.20.1 与 26.1.2 本轮同步的是红沙功能；现代 Lootr 适配限定在已经核对 API 的 1.21.1。

公开实现核对：

- Lootr 源码树 `fef3d548482ce08ba43fa9acaeaac71bc842e9ed`，仓库 `LootrMinecraft/Lootr`，分支 `mdg-1.21.1`。
- `common/.../block/entity/BlockEntityTicker.java`：转换原版考古方块前查询 `LootrAPI.isLootTableBlacklisted`。
- `common/.../block/entity/LootrBrushableBlockEntity.java`：`IBrushable$brush` 第十次完成后掉物、登记 opener，但保留方块；`hasBeenOpened` 在第一次刷取时就置位。
- `common/.../api/data/ILootrInfoProvider.java`：`getActualOpeners` 从 Lootr SavedData 读取已经完成领取的集合。
- 来源入口：<https://github.com/LootrMinecraft/Lootr/tree/fef3d548482ce08ba43fa9acaeaac71bc842e9ed>。真实联合回归使用下列发行 JAR，不用源码树代替发行包验收。

| 输入 | SHA-256 |
|---|---|
| lootr-fabric-1.21.1-1.11.38.127.jar | `2b33fa73a28c14f0983459c165e8c7b491cd1d798c947b967207f66194fb531d` |
| lootr-neoforge-1.21.1-1.11.38.127.jar | `5c74de4709d1c13b25880fdd241a89d5d9f331346d20edc76f80ccd4d99dc66c` |
| cloth-config-15.0.140-fabric.jar | `33894e95da3af59014b39d126586d5241e07ea39f8618763e30dab637405f95f` |

下载时另对照 Modrinth 官方版本元数据验证了 SHA-512。源文件与元数据工作副本在 `build/lootr-research/`。

## 实现

- 罗盘索引原来依赖方块移除回调，无法把 Lootr 的个人领取完成视为共享完成。保持已有“战场奖励全服共享一次”的规则，精确排除五文明普通/保底及两个旧入口共 12 个表；不全局屏蔽 Lootr，不改配置文件，不调整原版考古或其他容器。
- 可选 Mixin 选择插件只读映射后的字节码，验证实际使用的 `ResourceKey -> boolean` 静态 API。Lootr 缺席时跳过，签名不兼容时禁用并说明，不提前初始化游戏或第三方类。
- 已转换的旧方块在自然区块加载时修复，只遍历本区块索引登记的位置。保留尚未完成的战利品表与种子，恢复为原版可疑沙子/砂砾；已经有完成领取者时恢复普通沙子/砂砾，并沿用原回调推进清场和补位。修复不读取个人库存、不生成物品、不加载其他区块、不删除 Lootr 数据。中途刷取进度会重置为共享刷取。
- 三条版本线在普通 `sand` 标签判断前先识别 `Blocks.RED_SAND`，使用新注册的 `echo_warrior:suspicious_red_sand`。保留原版刷取及下落行为，完成后恢复红沙。物品沿用现有“单独摆放无战利品”提示。
- 红沙资源由已有 Java 生成器、原版红沙底图和保留的五张人工痕迹生成；十种痕迹组合、四个朝向和四个刷取阶段。没有生成新的 AI 美术源文件。1.20.1 按现有 Gradle 规则消费 1.21.1 的资源。
- 中英文名称、术语、PROJECT 与百科同步。其他语言的新增名称仍待翻译，未修改翻译接受基线，未发布。

## 回归与证据

自动守卫：

- `scripts/check-archaeology.py`：三线生成选择顺序、注册/刷取回归入口、40 个模型/贴图与四阶段资源、双语名称；发行包额外检查 Lootr 类及两加载器配置。
- `RedSandSelfTest*`：调用真实地表选择、方块实体注册、十次原版刷取、一次实际物品掉落和恢复红沙，接入既有服务端测试入口。
- `LootrArchaeologySelfTest1211`：真实 Lootr API 的 12 表排除及无关表透传；实际 Lootr 沙/砂砾各刷 0 / 1 / 10 次，保存/恢复方块实体后检查迁移、表键/种子保持、完成目标释放、幂等及奖励恰好一次。
- 两条 packaged server 流程要求独立红沙 PASS 标记；装入 Lootr 时必须出现联合测试 PASS，适配缺席不算通过。

本地：26.1.2 Fabric、1.21.1 Fabric/NeoForge、1.20.1 Fabric/Forge 已构建；两兼容线 JAR 基线及原有 57 项损坏/格式等价测试通过。长运行交给 FOXY-NODE，互斥锁占用时未强行启动。

首次节点任务 `20261026T092931Z-77164` 在新源码检查器读取 UTF-8 文件时因节点默认 GBK 而失败，未进入 Minecraft；修正为显式 `encoding="utf-8"`。失败证据保留在 `build/foxy-tests/20261026T092931Z-77164/evidence`，不计作运行验收。

节点结果：

| 任务 | 结果与范围 |
|---|---|
| `20261026T093128Z-14476` | 无 Lootr 的 1.21.1 Fabric/NeoForge、1.20.1 Fabric/Forge 生产专服均通过，1.20.1 各两次启动；57 项损坏/格式等价检查通过。Lootr Fabric 通过，NeoForge 联装因模拟玩家无 Netty channel 而失败，整份任务保留 failed。 |
| `20261026T093932Z-69528` | 增加异常栈后再次确认 NeoForge 失败来自测试模拟连接：Lootr 完成通知经 `PacketDistributor` 进入 `NetworkRegistry.checkPacket`，而模拟连接没有真实 channel。未放宽成功条件；此诊断失败保留。 |
| `20261026T094513Z-6188` | 修正模拟监听器，在发送入口记录实际通知包后，Lootr Fabric / NeoForge 联装均通过。两端都有完整 0/1/10 次刷取、NBT 恢复、目标释放、奖励一次、原有全套 selftest 与正常保存退出标记。此任务只跑联装，不冒充又跑了一轮无 Lootr 套件。 |
| `archaeology-main-20261026T0934` | 26.1.2 初次夹具在实体区块尚未可见时统计掉落而失败；保留崩溃报告。 |
| `archaeology-main-20261026T0946` | 修正为跨 tick 等待真实实体区块就绪后，26.1.2 Fabric 通过红沙、生成/间距/卸载回归及正常保存退出（18.45 秒）；没有测试主线 NeoForge。 |

兼容线证据分别在 `build/foxy-tests/<RunId>/evidence`；主线失败与通过证据在 `build/lootr-research/mainline-first-evidence`、`mainline-final-evidence`。取回 ZIP 均核对了 SHA-256。最终 1.21.1 JAR 与已通过无 Lootr 检查的 JAR 按解压后逐项比较，仅 `LootrArchaeologySelfTest1211.class` 与新增的监听器匿名类不同，生产实现和资源逐字节相同。

最终候选 SHA-256（本地版本标识 `0.2.9`，不是本轮发布声明）：

| 候选 | SHA-256 |
|---|---|
| 26.1.2 Fabric | `dadf4d584228ea2336d8b1e44bfe7ef0aaef4739f76c3914398663b3c8d4fe29` |
| 1.21.1 Fabric | `72822ad7ee60699f9154c5ba1adb5565b9581ef7a7367c5880ae49c9ba148443` |
| 1.21.1 NeoForge | `fc222e5b1c1966535cf176ecfcc367b5adfebe4470af2e964c0f9933d79e8d13` |
| 1.20.1 Fabric | `67781687d898eb743925d833e9bcf4d755c8faa64402c48ab3df078a301958aa` |
| 1.20.1 Forge | `68ec0d5f2c0fa999ab1f32abe938469661cba044cac1a0a0ebe86acb1c1e30e9` |

考古实现阶段的本地化结构审计为 0 个硬错误；当时非源语言缺少新增的 block/item 两个红沙名称键，24 个待处理报告项。当时未使用 `--allow-pending` 或 `--mark-current`，没有上传发行版。

2026-10-26 后续本地化任务已补齐六套基准译文和全部地区别名，通过独立审校后登记源文基线，发布本地化门禁为 **0 个硬错误、0 个待处理项**，未使用缺译放行。详见 `docs/localization/REVIEW_RED_SAND_2026-10-26.md`。上述运行证据与 JAR 哈希仍对应补译前的候选，最终合包须重新构建，不能把语言源文件已更新等同于旧 JAR 已包含新译文。

## 人工验收

- [ ] 重启客户端/服务器，在红沙地形新生成一处遗迹，观察四阶段纹理、掉物显示、沙类下落和恢复红沙。
- [ ] 手持罗盘完成沙、砂砾、红沙刷取，检查方向文字与声音移动到下一目标。
- [ ] 1.21.1 联装固定 Lootr 包，旧已领取目标重载区块后解除；未完成目标仍可刷一次。两名真实玩家验证共享一次及其他 Lootr 容器仍各自可领。
- [ ] 验证资源包/整合包组合与真实多人延迟，不把服务器模拟玩家等同于客户端视觉或真人多人验收。

需要重启。无需新世界；红沙生成只影响之后生成的遗迹，旧黄沙遗迹不重铺，也不要求所有测试必须进入新区块。
