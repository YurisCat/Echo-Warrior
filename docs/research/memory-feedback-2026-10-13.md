# 0.2.7 玩家整合包与 spark 原始报告诊断

日期：2026-10-13（Asia/Shanghai）。

## 结论

**找到了明确的主线程性能瓶颈：战场遗迹补位任务在服务器 tick 中同步加载／生成区块。尚未找到能证明持续内存泄漏的证据。**

玩家所说的约 13% 与本次执行采样中 Echo Warrior 的 **13.33% 主线程采样耗时**吻合；它不是“本模组持有了 13% 的内存”。约 **12.26% 的整段主线程采样耗时**位于遗迹补位路径，相当于本模组被采样耗时的约 **91.9%**。读取玩家分享存档后，还发现三个补位任务分别记录了 **302、280、194 次重试**。

本轮只读分析用户提供的包、日志、存档中的两份本模组 SavedData 和 spark 导出，没有运行整合包、修改存档或提交运行时修复。不能把已定位的性能瓶颈直接命名为已经证实的内存泄漏，也不能借此否定另一位用户的 32 GB 反馈。

## 输入与真实环境

用户分享目录有 `latest.log` 和 `1.21.1-NeoForge.zip`。ZIP 为 1,107,944,254 字节、7480 个条目，直接包含 125 个模组 JAR；spark 包含嵌套依赖的来源清单为 154 项。通过公开下载接口的字节范围读取 ZIP 目录和必要条目，没有为读几份配置而解包整个玩家世界。已提取的条目均通过解压大小和 ZIP CRC32 校验。

| 项目 | 实际输入 |
| --- | --- |
| Minecraft / 加载器 | 1.21.1 / NeoForge 21.1.252 |
| Echo Warrior | 0.2.7 |
| GeckoLib / SmartBrainLib | 4.9.3 / 1.16.11 |
| 平台 | macOS ARM64，Apple M1，8 个逻辑处理器 |
| Java | Oracle Java 21.0.10 |
| 最大堆参数 | `-Xmx3698m`；报告中实际堆上限 3.625 GiB |
| TBF | 日志、包清单和 spark 来源清单均未发现 |
| 本模组配置 | `maxLevel=100`，`maxLivingEchoesPerController=0`，均为默认值 |

包内 Echo Warrior JAR 的 SHA-256 为
`e205290b14bfe9acb580bc14a6539f231856d34edbcdc250db021089da9fab8f`，
与 `docs/releases/0.2.7.md` 的正式 1.21.1 NeoForge 包一致。

这与 2026-10-12 的持续内存实验环境不同：上一轮是 1.20.1 Forge、Windows、GeckoLib 4.8.4 和小型测试世界，且未覆盖多个历史补位任务长时间失败的场景。上一轮的通过不能替代此环境的验收。

## spark 的 13% 指什么

`latest.log` 第 2698 行给出了当前报告。下载了其概要 JSON、完整 JSON 和原始 sampler 数据；玩家分享报告的访问标识只保留在本地诊断输入中。

- 时间：**2026-10-13 13:09:52.787—13:13:09.350**，共 **196.563 秒**。
- 仅采样 `Server thread`，`samplerMode=0`，官方 protobuf 定义为 `EXECUTION`；`ALLOCATION=1`。本报告不是内存分配剖析或对象保留分析。
- 间隔 4000 微秒；四个时间窗口合计 195,568 毫秒的采样权重。
- 完整调用树有 52,063 个节点。脚本核验了所有引用均有且仅有一个父节点／根入口、引用无环、四个窗口的根节点权重和均等于线程总权重。
- 按首次进入 Echo Warrior 栈的路径去重，避免把父方法和子方法重复相加，得到 26,076 毫秒，即 **13.3335%**。这与用户描述的约 13% 相符；没有将它换算为 JVM 或系统内存份额。

官方说明与协议：

- [spark viewer 说明](https://spark.lucko.me/docs/Using-the-viewer)：调用帧百分比以所属线程的采样时间为分母，总时间包含子调用。
- [spark sampler 协议](https://github.com/lucko/spark/blob/3e8745a76eaabe9a4f70aad6283876fb518d2ae8/spark-common/src/main/proto/spark/spark_sampler.proto)：`SamplerMode` 枚举。

该整合包还保存了较早的 spark 历史链接。其中堆摘要生成于 **2026-10-05 00:06:24.715**，当时 Echo Warrior 为 **0.2.2**；不能拿它证明 0.2.7 的内存保留情况。另一个 2026-10-11 的执行报告也不是当前版本的堆转储证据。

## 明确的耗时路径

下表均为相对整个 `Server thread` 的包含子调用的耗时；父子行重叠，不能相加。

| 调用 | 采样折算耗时 | 占比 |
| --- | ---: | ---: |
| Echo Warrior 栈路径去重合计 | 26.076 s | 13.33% |
| `BattlefieldSystem1211.tickReplacementGeneration` | 23.980 s | 12.26% |
| `BattlefieldSystem1211.tryReplacementAttempt` | 23.968 s | 12.26% |
| 候选中心 `Level.getChunk → ServerChunkCache.getChunkBlocking` | 19.356 s | 9.90% |
| `loadSiteChunks` 加载候选周边区块 | 4.600 s | 2.35% |

主要调用链：

```text
服务器 tick 结束事件
  → EchoWarrior1211NeoForge.onServerTick
    → BattlefieldSystem1211.tick
      → tickReplacementGeneration
        → tryReplacementAttempt
          → Level.getChunk
            → ServerChunkCache.getChunkBlocking
              → managedBlock / waitForTasks
```

这里的等待发生在 tick 内的同步区块请求中，不能等同于服务器完成一刻之后正常休眠。栈中还包含处理区块相关任务的时间，不应把整个 13% 说成 Echo Warrior 自有 Java 运算消耗的 CPU 百分比。

补位路径在四个窗口中的占比分别为 **2.99%、7.21%、19.79%、37.81%**。最后约 16 秒中，约 6 秒采样时间落在此路径。原报告同时记录了秒级的慢 tick；最后窗口的最大单 tick 为 **3517 ms**。没有仅凭汇总采样把每条具体慢 tick 都归因于这一条路径。

对应当前源码：

- `versions/1.21.1/common/src/main/java/com/yuriscat/echowarrior/compat/world/BattlefieldSystem1211.java:134`：补位入口。
- 同文件 `:147`：失败后通常延迟 20 tick；每个任务每第 8 次失败延迟 200 tick。成功放置的 600 tick 节流不是每次失败加载的耗时预算。
- 同文件 `:186`：在服务器线程直接 `level.getChunk(candidate.x, candidate.z)`。
- 同文件 `:345`：`loadSiteChunks` 再对周边区块逐个同步调用 `getChunk`。

只限制尝试频率，并不能限制单次生成或读取区块需要的时间。在含世界生成内容的整合包里，这条路径可能产生明显停顿和临时对象／区块内存峰值；本次调用树确认了阻塞开销，但没有提供这些对象永久无法回收的证据。

## 存档佐证

只读解析用户分享的两份小型 SavedData，未载入游戏或修改原 ZIP：

- 战场数据仅 **5710 字节（gzip）**：81 个区域，其中 42 ACTIVE、38 WAITING、1 COOLDOWN；381 个已修改区块坐标。
- 有 **3 个未完成的 ReplacementJobs**，`Attempts` 分别为 **302、280、194**。
- 绑定数据仅 **1153 字节（gzip）**：2 个绑定，1 个活动绑定。
- 当前 spark 报告结束时，有 329 个世界实体，其中 Echo Warrior 实体为 **1 名罗马军团兵**。这只是结束时快照，不代表全程只出现过一种英雄。

因此应重点复现“已有多个补位任务长期失败”的场景。现有证据不足以说明具体候选地形为什么失败，也没有证明队列条目本身无界增长；不能将 3 条任务记录直接称作泄漏。

## 内存与日志的边界

当前 spark 概要中的内存快照：

| 指标 | 数值 |
| --- | ---: |
| 系统物理内存 | 8.000 GiB |
| 系统已使用内存 | 7.932 GiB |
| 已使用交换空间 | 650.38 MiB |
| JVM 已使用堆 | 2.645 GiB |
| JVM 已提交／最大堆 | 3.625 GiB |
| JVM 非堆已使用 | 约 0.742 GiB |

这些数据说明该时点资源余量较小、已经使用交换空间，值得纳入卡顿诊断；单个“系统已用内存”读数也包含操作系统的内存管理影响，不能据此证明是谁泄漏。

平台统计记录了 133 次 Young GC、平均约 50.8 ms、平均间隔约 1.78 s；Old Generation GC 次数为 0。该情况与较高分配压力相容，**仍不足以证明或排除泄漏**。缺少当前问题下长时间的回收后堆基线、分配分析和保留对象链。

四个窗口的加载区块数为 **2934 → 2847 → 3472 → 2847**。至少这段采样中不是单调累积，不能将同步加载自动推断成永久强加载。

日志共有 2771 行、236 条 ERROR，其中 98 条是其他模组的 `ysm.particle` 表达式解析失败；未发现 Echo Warrior ERROR 行、`OutOfMemoryError`、`GC overhead limit` 或 direct-buffer OOM 标记。存在四次 `Can't keep up` 警告，最后正常保存全部维度并退出。

其他模组的资源或表达式错误是单独的排查线索；本报告只有服务端线程采样，不能据此判定它们对客户端渲染或内存的影响。没有 ERROR 或 OOM 也不等于已经证明没有泄漏。

## 修复与验证优先级

已确定应优先处理**补位流程的阻塞区块请求和长期失败重试开销**。后续实现应研究分步／非阻塞等待区块、限制同时进行的候选请求和失败预算，保持存档兼容与遗迹安全检查；不应以删除玩家任务、粗暴清空存档或只增加 JVM 堆掩盖问题。

回归必须覆盖三个高重试次数任务、无法找到合法地形的连续失败、区块加载完成／失败、关服清理及重启恢复。性能通过标准需包括主线程不再长时间同步等待、同时加载与保留的区块／请求数量有界。完成修复后，按 `VERSION_PORTING_PLAYBOOK.md` 的规则将可复用根因和自动守卫一起登记；本轮没有虚构已通过的修复测试。

仍需对原环境做持续内存观测，才能确认是否另有真正的内存泄漏。另一名用户“32 GB 一小时占满”的反馈仍是独立待复现问题。

## 本地证据与可重复计算

证据在 `build/memory-feedback-2026-10-13/`，不将玩家原始日志、存档、路径或完整配置提交到仓库。关键文件：

- `spark-full.json`、`spark-summary.json`、`spark-profile.bin`：原始分析导出。
- `latest.log`：原日志，SHA-256 `bfea6c00d1e4bb14b0c97150e7404fcc9b4d28cd4425f8b507a4974c3257fbb7`。
- `findings.json`、`echo-hotspots.json`、`saveddata-summary.json`、`log-summary.json`：计算结果。
- `selected-manifest.json`：实际提取条目的大小、CRC32 和 SHA-256。
- `evidence-sha256.json`：主要输入与结果哈希；完整 spark JSON SHA-256 `49cd4ba7143480a219e854b87218cd2b5d438cba7f94c7c0ee3ff8b020197d9f`。
- `summarize-profile.py`：重新核验 52,063 个调用树节点和去重百分比；`read-nbt.py` 只读重算本模组 SavedData 摘要。

本轮仅补充诊断记录，玩家规则、语言和百科内容没有变化，无需同步这些资源。
