# 宠物项圈兼容提示本地化审校

日期：2026-10-27。范围：5 个新增键、6 个基准目标语言，连同地区别名共 12 个语言／地区。
译者为本会话主代理，独立审校者为 `pet_collar_localization_review` 子代理；后者使用独立上下文，
仅比较双源文本、术语表、第三方原包语言资源、现有用词和候选，不读取译者解释。
遵守 `REVIEW_WORKFLOW.md` 的角色分离要求。

**最终结果：** 初审的 8 条必须修改、6 条建议修改均已落实，复审确认 30 条语义与术语全部通过；
没有未解决的必须修改项或需作者裁决的问题。结构检查与源文差异检查通过后登记基线，
发布本地化门禁结果为 **0 个硬错误、0 个待处理项**，未使用 `--allow-pending`。
后文保留初审卡与精确改稿作为审计历史，不能将已解决的初审问题读成当前待办。

## 双源与使用位置

| 键 | 使用位置 | 中文源文 | 英文源文 |
|---|---|---|---|
| `message.echo_warrior.pet_management_restricted` | 禁止第三方管理入口时的动作栏 | 请使用回声召唤器管理这位英灵；宠物床、捕网和外部归属变更不可用。 | Use the Echo Summoner to manage this warrior. Pet beds, capture nets and external ownership changes are unavailable. |
| `tooltip.echo_warrior.pet_collar_limits` | 遗物保存对应受限附魔时的悬浮说明 | 全面召回与来世诅咒不对英灵生效；附魔仍保留在项圈上。 | Total Recall and Curse of Afterlife are inactive on Echo Warriors; their enchantments remain on the collar. |
| `tooltip.echo_warrior.pet_collar_saved` | 遗物已保存项圈时的悬浮说明 | 已保存宠物项圈：收回、死亡及遗物转装后保留 | Pet collar saved: kept after dismissal, death and relic transfers. |
| `message.echo_warrior.pet_compass_unavailable` | 宠物罗盘操作失败时的动作栏 | 无法通过宠物罗盘执行此操作，请检查英灵归属、燃料与召唤器。 | This pet-compass action is unavailable. Check ownership, fuel and your Echo Summoner. |
| `tooltip.echo_warrior.pet_collar_immaturity` | 主线遗物保存变小诅咒时的悬浮说明 | 此版本的英灵不支持幼体形态，变小诅咒保留但不生效。 | Echo Warriors in this version have no baby form. Curse of Immaturity is retained but inactive. |

源码位置为三个版本的 `PetCollarCompat*.java`。限制只针对英灵，不改写为普通宠物也不可用；
保留附魔与禁用效果必须同时说清。源文、数值、条件、运行时代码和既有译文在本批中均不改变。
这五条没有格式占位符或动态数量。

## 第三方名称核对

实际读取受测驯养革新 1.7.1 与六份 Pet Home JAR 内的语言 JSON，记录文件 SHA-256，
不按项目分支的最新译文推断已发布二进制内容。提取证据在
`build/localization-pet-collar-2026-10-27/upstream-terms.json`；
这些固定输入的完整哈希亦见项圈兼容测试记录。

- 驯养革新日语／西班牙西语三项附魔名，以及俄语／巴西葡语前两项附魔名均有原包译文；
  1.20.1 Pet Home 两加载器沿用相同名称。
- Pet Home 1.20.1 繁中使用「晶球保護」「幼稚詛咒」，与源中文的「全面召回」「变小诅咒」不同。
  最终提示保留两组对应名称，不让玩家猜测是否为同一附魔。
- 俄语、葡语缺少的变小诅咒，以及中性拉美西语的名称按双源与既有语言习惯补齐。
- 受测 Pet Home 1.21.1、26.1.2 的四个包没有这些目标语言文件，第三方自身仍可能回退为英文。
  本次只翻译 Echo Warrior 提示，不宣称补齐第三方模组的本地化。

目标语言术语已登记至 `TERMINOLOGY.md`。

## 本批界面验收范围

沿用 `GPT6_INDEPENDENT_REVIEW.md` 中 0.2.2 已接受的字体与代表性界面，
以及 `REVIEW_0.2.4.md` 的普通悬浮提示／动作栏局部更新流程。本次没有修改字体、布局、
固定宽度按钮、教程书页或提示渲染代码，不需要新世界。

独立审校已对可缩短的表达给出删减，保留操作对象和全部限制。按最终文本字符数，管理限制
动作栏为繁中 32、日语 53、俄语 112、葡语 121、西语 131；字符数仅用于预警，不冒充像素宽度。
繁中与日语有本模组旧语言文件未使用的常见汉字，仍应在游戏内随最终包抽查字形。

本次作为局部文本更新登记语言基线，明确将尚未逐语言重拍界面作为后续发行的剩余视觉风险：
窄窗口、较大的 GUI 缩放或替换字体资源包可能截断较长动作栏，较长悬浮说明也需随最终包抽查。
不声称这 30 条译文已经逐条实机视觉通过，不把本地化门禁通过等同于整个兼容功能完成发布验收。

本批只更新语言资源、术语与审校记录；玩法、控制、平衡和状态没有新增变化，中文百科已有对应
项圈规则，因此不再修改百科。既有六个候选 JAR 与候选 ZIP 生成于本批补译之前，正式出包需重新构建
并核对打包语言；本记录不替换此前专服和客户端测试对应的二进制哈希。

## 自动检查与基线登记

- 24 份目标文件（12 个语言／地区 × 主线及 1.21.1 资源线）各只新增 5 个键；
  中英文源文案和全部旧译文逐键保持不变，每份文件均为 631 个键。
- JSON、键集合、占位符、控制字符、固定术语和既有 UI 源码守卫通过。
- 主线与 1.21.1、香港繁中与台湾繁中、五个拉美地区别名与 `es_mx` 均逐字节一致。
- 已核对 `versions/1.20.1/build.gradle` 的语言合并规则，并确认本地覆盖文件没有这 5 个键；
  1.20.1 构建会复用主线新译文。
- 登记前逐语言核对源文指纹差异恰好只有本批 5 个键；登记后所有旧指纹保持不变。
- 执行 `python scripts/check-localization.py --mark-current all` 后，
  再执行 `python scripts/check-localization.py --release-gate`，结果 0 个硬错误、0 个待处理项。
- 局部差异检查通过；验证明细及最终语言文件 SHA-256 位于
  `build/localization-pet-collar-2026-10-27/verification.json`。
- 独立复审的最终候选 SHA-256：
  `457d0d4bdaebc53ab87deca0c40bcd17151289d32e9a6e07efad55bbb26d0089`。

## 独立审校原始记录与复审

## 宠物项圈兼容文案独立审校 — 2026-10-27

本报告由未参与初稿写作的独立审校角色完成。仅比较双源文本、固定术语、既有语言资源、候选及所提供的第三方原包术语证据；未读取译者的解释。没有修改候选、语言文件或术语表。

初审对象为 `candidates.json` 的六种基准语言、每种五条，共 30 条。初审候选 SHA-256：`994bcc3e6c464cd81186771aad349429e8f485d678d656c55ab97b9f1d993863`。审校中收到的上游证据扩展至 7 个 JAR；扩展后 `upstream-terms.json` SHA-256：`38891ed215fb4add02429236a7ecfdbfd900ac2ccd341242440fb3de48be7149`。

### 最终复审结论

**最终 30 条均通过独立语义与术语审校；未解决的必须修改项为 0；需要作者决定：无。** 初审的 8 项必须修改和 6 项建议修改均已逐字采纳指定文本。下文初审卡保留原稿及诊断作为历史证据，其中“必须修改”不是最终未解决状态；卡片中的“精确建议文本”为最终值，写“保留原值”的 16 条保持初稿。

最终候选 SHA-256：`457d0d4bdaebc53ab87deca0c40bcd17151289d32e9a6e07efad55bbb26d0089`。

| 基准语言 | S1 管理限制 | S2 两项附魔限制 | S3 项圈保存 | S4 罗盘受限 | S5 幼体形态 |
|---|---|---|---|---|---|
| zh_tw | 通过 | 通过 | 通过 | 通过 | 通过 |
| ja_jp | 通过 | 通过 | 通过 | 通过 | 通过 |
| ru_ru | 通过 | 通过 | 通过 | 通过 | 通过 |
| pt_br | 通过 | 通过 | 通过 | 通过 | 通过 |
| es_es | 通过 | 通过 | 通过 | 通过 | 通过 |
| es_mx | 通过 | 通过 | 通过 | 通过 | 通过 |

独立复核将每条审阅卡的原值/精确建议值与最终 `candidates.json` 逐字比较，确认 14 条改稿和 16 条保留文本均吻合；又检查了主线及 1.21.1 两条资源线共 24 个实际语言文件中的全部本批键，且香港繁中与拉美地区别名逐字节对应其基准文件。双源 10 个值与初审引用完全一致。术语表已补繁中原包别名。

本次复审不构成视觉通过：动作栏宽度、悬浮提示换行、真实字形及第三方语言回退均未实机验收。没有进行新的客户端、服务器或兼容性测试，也没有给任何语言标记 `--mark-current`。

### 初审结论

| 语言 | 通过 | 建议修改 | 必须修改 |
|---|---:|---:|---:|
| zh_tw | 2 | 1 | 2 |
| ja_jp | 4 | 1 | 0 |
| ru_ru | 4 | 1 | 0 |
| pt_br | 2 | 1 | 2 |
| es_es | 2 | 1 | 2 |
| es_mx | 2 | 1 | 2 |
| 合计 | 16 | 6 | 8 |

必须解决的项目：

1. `zh_tw` 的 `pet_collar_limits`、`pet_collar_immaturity`：Pet Home 1.20.1 的实际附魔名分别是「晶球保護」「幼稚詛咒」，候选仅写「全面召回」「變小詛咒」，玩家无法直接对照原包名称。建议保留规范称呼并附实际别名；不改变附魔机制或双源含义。
2. `pt_br`、`es_es`、`es_mx` 的两个 `message`：按已锁定的目标术语规则，正文应以 `aparelho` / `dispositivo` 明确 `Invocador de Ecos` 是装置。原稿保留了正式名称，但没有落实此项约束。建议稿补足，且保持归属检查，不擅自改成控制权检查。

没有需要作者裁决的玩法、语气或文化问题。第三方名称对应、目标语言语法及正文装置消歧可以在本次本地化工作内解决。所有“通过”仅表示语言审查通过，均不代表游戏内视觉通过。

### 双源与界面基准

各审阅卡的 S1–S5 引用下列完整双源，同一键六种语言均按相同源文审查。两条 `message` 用于受限操作的原版动作栏；三条 `tooltip` 用于遗物悬浮提示。S2 / S5 只在保存了对应附魔时出现，不能让译文暗示所有项圈都不能生效。

#### S1 — `message.echo_warrior.pet_management_restricted`

- 中文原文：请使用回声召唤器管理这位英灵；宠物床、捕网和外部归属变更不可用。
- 英文原文：Use the Echo Summoner to manage this warrior. Pet beds, capture nets and external ownership changes are unavailable.
- 使用位置：受限操作动作栏。

#### S2 — `tooltip.echo_warrior.pet_collar_limits`

- 中文原文：全面召回与来世诅咒不对英灵生效；附魔仍保留在项圈上。
- 英文原文：Total Recall and Curse of Afterlife are inactive on Echo Warriors; their enchantments remain on the collar.
- 使用位置：保存对应附魔时的遗物悬浮提示。

#### S3 — `tooltip.echo_warrior.pet_collar_saved`

- 中文原文：已保存宠物项圈：收回、死亡及遗物转装后保留
- 英文原文：Pet collar saved: kept after dismissal, death and relic transfers.
- 使用位置：遗物悬浮提示。

#### S4 — `message.echo_warrior.pet_compass_unavailable`

- 中文原文：无法通过宠物罗盘执行此操作，请检查英灵归属、燃料与召唤器。
- 英文原文：This pet-compass action is unavailable. Check ownership, fuel and your Echo Summoner.
- 使用位置：宠物罗盘操作不可用的动作栏。

#### S5 — `tooltip.echo_warrior.pet_collar_immaturity`

- 中文原文：此版本的英灵不支持幼体形态，变小诅咒保留但不生效。
- 英文原文：Echo Warriors in this version have no baby form. Curse of Immaturity is retained but inactive.
- 使用位置：保存对应附魔时的遗物悬浮提示。

### 第三方名称证据与适用边界

- 驯养革新 1.7.1 / 1.20.1 Forge 原包：日语三项、西班牙西语三项、俄语及巴西葡语前两项均与候选一致。俄语、葡语的变小诅咒没有该语言原包键，候选是本体补译，不能称为原包正式名称。
- Pet Home 1.20.1 Fabric / Forge 的相关目标语言名称一致。繁中 `total_recall` 为「晶球保護」，`undead_curse` 为「來世詛咒」，`immaturity_curse` 为「幼稚詛咒」。本报告建议的括号内容是原包识别别名，并非新增效果。
- 受测 Pet Home 1.21.1 Fabric / NeoForge 和 26.1.2 Fabric / NeoForge 均没有六种基准目标语言文件；不能宣称这些版本中第三方界面已经显示本报告采用的目标语言名称。实际显示仍受原包缺键、语言回退及用户资源包影响，需记录并在视觉验收时确认。
- `es_mx` 的第三方附魔名称沿用通用西语名称；没有原包拉美语言文件可以声称逐项匹配。香港繁中、拉美地区别名由主任务同步，本报告不将其算作额外独立语言审校。
- 俄语「Отзыв」在 S2 中有引号，是附魔专名；S3 的 `отзыва` 是本体“收回”的名词形式，语境能够区分。葡语「Maldição do vivo-morto」是第三方附魔名称，没有把英灵本身改译为亡灵。
- “宠物项圈”是源文使用的通称，允许使用 `首輪` / `ошейник` / `coleira` / `collar`；不要求把通称错误地替换为第三方物品“项圈标签”的完整名称。

### 逐条审阅卡

#### zh_tw / S1

键：`message.echo_warrior.pet_management_restricted`；使用位置：动作栏；双源：S1。

- 目标语言：`請使用回聲召喚器管理這位英靈；寵物床、捕捉網和外部所有權變更皆無法使用。`
- 中文直译回译：请使用回声召唤器管理这位英灵；宠物床、捕捉网和外部所有权变更都不能使用。
- 自然中文释义：这名英灵应通过召唤器管理，所列第三方管理方式不可用。
- 术语对应：英灵 → 英靈；召唤器装置 → 回聲召喚器；归属 → 所有權；捕网 → 捕捉網。
- 审校结论：**建议修改**。语义准确，繁中自然；可以去掉不增加信息的字词以减轻动作栏长度。
- 精确建议文本：`請用回聲召喚器管理此英靈；寵物床、捕捉網及外部所有權變更不可用。`
- 风险：可能过长；36 → 32 个字符，仍需实际宽度验收。
- 需要作者决定：无需作者决定。

#### zh_tw / S2

键：`tooltip.echo_warrior.pet_collar_limits`；使用位置：条件悬浮提示；双源：S2。

- 目标语言：`全面召回與來世詛咒對英靈無效；附魔仍保留在項圈上。`
- 中文直译回译：全面召回与来世诅咒对英灵无效；附魔仍保留在项圈上。
- 自然中文释义：只有指名的两项附魔不生效，已保存的附魔不会被删掉。
- 术语对应：Total Recall → 全面召回，Pet Home 1.20.1 实际名为晶球保護；Curse of Afterlife → 來世詛咒；保留 → 仍保留。
- 审校结论：**必须修改**。机制正确，但缺少实际第三方名称，无法直接对照装备上的附魔。
- 精确建议文本：`全面召回（晶球保護）與來世詛咒對英靈無效；附魔仍保留在項圈上。`
- 风险：名称识别；25 → 31 个字符，增加的别名需检查悬浮框。
- 需要作者决定：无需作者决定；保留规范名称并附别名即可，不改变玩法。

#### zh_tw / S3

键：`tooltip.echo_warrior.pet_collar_saved`；使用位置：遗物悬浮提示；双源：S3。

- 目标语言：`已儲存寵物項圈：收回、死亡及遺物轉裝後仍會保留`
- 中文直译回译：已保存宠物项圈：收回、死亡及遗物转装后仍会保留。
- 自然中文释义：三种事件都不会丢失保存的项圈。
- 术语对应：保存 → 儲存；收回 → 收回；遗物转装 → 遺物轉裝。
- 审校结论：**通过**。保留三个事件，未承诺英灵免死或自动复活。
- 精确建议文本：保留原值。
- 风险：无新增语言风险；23 个字符，未检查游戏内显示。
- 需要作者决定：无需作者决定。

#### zh_tw / S4

键：`message.echo_warrior.pet_compass_unavailable`；使用位置：动作栏；双源：S4。

- 目标语言：`無法透過寵物羅盤執行此操作，請檢查英靈所有權、燃料與召喚器。`
- 中文直译回译：无法通过宠物罗盘执行此操作，请检查英灵所有权、燃料与召唤器。
- 自然中文释义：本次罗盘操作失败，应核对归属、燃料和召唤装置三个条件。
- 术语对应：宠物罗盘 → 寵物羅盤；归属 → 英靈所有權；燃料 → 燃料；装置 → 召喚器。
- 审校结论：**通过**。保留“此操作”的范围，没有说宠物罗盘全部功能永久禁用；没有把所有权变成控制权。
- 精确建议文本：保留原值。
- 风险：动作栏长度；30 个字符，未检查游戏内显示。
- 需要作者决定：无需作者决定。

#### zh_tw / S5

键：`tooltip.echo_warrior.pet_collar_immaturity`；使用位置：条件悬浮提示；双源：S5。

- 目标语言：`此版本的英靈沒有幼體形態；變小詛咒會保留，但不生效。`
- 中文直译回译：此版本的英灵没有幼体形态；变小诅咒会保留，但不生效。
- 自然中文释义：这个版本不支持幼体表现；保存诅咒不等于启用其效果。
- 术语对应：此版本 → 此版本；baby form → 幼體形態；Curse of Immaturity → 變小詛咒，Pet Home 1.20.1 实际名为幼稚詛咒。
- 审校结论：**必须修改**。版本限定和机制准确，但应补上实际第三方名称。
- 精确建议文本：`此版本的英靈沒有幼體形態；變小詛咒（幼稚詛咒）會保留，但不生效。`
- 风险：名称识别；26 → 32 个字符。此别名并不表示 26.1.2 原包已有繁中翻译。
- 需要作者决定：无需作者决定。

#### ja_jp / S1

键：`message.echo_warrior.pet_management_restricted`；使用位置：动作栏；双源：S1。

- 目标语言：`このエコーの管理にはエコー召喚器を使ってください。ペット用ベッド、捕獲ネット、外部からの所有者変更は利用できません。`
- 中文直译回译：管理这名回声请使用回声召唤器。宠物用床、捕获网、来自外部的所有者变更不能使用。
- 自然中文释义：这名英灵通过召唤装置管理，列出的其他管理方式不可用。
- 术语对应：英灵 → エコー；回声召唤器 → エコー召喚器；宠物床 → ペット用ベッド；外部归属变更 → 外部からの所有者変更。
- 审校结论：**建议修改**。自然且准确，可缩短助词和同义动词；没有“召唤师”歧义。
- 精确建议文本：`このエコーはエコー召喚器で管理してください。ペット用ベッド、捕獲ネット、外部からの所有者変更は使えません。`
- 风险：长 CJK 动作栏；58 → 53 个字符，不能用字符数代替屏幕宽度验收。
- 需要作者决定：无需作者决定。

#### ja_jp / S2

键：`tooltip.echo_warrior.pet_collar_limits`；使用位置：条件悬浮提示；双源：S2。

- 目标语言：`トータルリコールと来世の呪いはエコーには効果がありませんが、首輪のエンチャントは保持されます。`
- 中文直译回译：全面召回和来世的诅咒对回声没有效果，但项圈的附魔被保留。
- 自然中文释义：这两项效果对英灵无效，附魔数据仍留在项圈上。
- 术语对应：Total Recall → トータルリコール；Curse of Afterlife → 来世の呪い；附魔 → エンチャント；保留 → 保持。
- 审校结论：**通过**。两项专名匹配具有日语文件的所查原包；没有把全部项圈附魔一概判为无效。
- 精确建议文本：保留原值。
- 风险：长悬浮行；47 个字符，未检查游戏内显示。
- 需要作者决定：无需作者决定。

#### ja_jp / S3

键：`tooltip.echo_warrior.pet_collar_saved`；使用位置：遗物悬浮提示；双源：S3。

- 目标语言：`ペットの首輪を保存済み：送還、死亡、遺物の移し替え後も保持`
- 中文直译回译：宠物的项圈已保存：送还、死亡、遗物移装之后也保留。
- 自然中文释义：遣散英灵、英灵死亡或遗物转装均不清除保存的项圈。
- 术语对应：宠物项圈 → ペットの首輪；收回 → 送還；遗物转装 → 遺物の移し替え。
- 审校结论：**通过**。`送還` 与现有召唤器按钮一致，结尾省略句适用于悬浮提示。
- 精确建议文本：保留原值。
- 风险：无新增语言风险；29 个字符，未检查游戏内显示。
- 需要作者决定：无需作者决定。

#### ja_jp / S4

键：`message.echo_warrior.pet_compass_unavailable`；使用位置：动作栏；双源：S4。

- 目标语言：`ペットコンパスではこの操作はできません。所有者、燃料、エコー召喚器を確認してください。`
- 中文直译回译：用宠物罗盘无法进行这项操作。请确认所有者、燃料、回声召唤器。
- 自然中文释义：这次罗盘操作不可用，需要检查归属、燃料与召唤装置。
- 术语对应：宠物罗盘 → ペットコンパス；归属检查 → 所有者を確認；燃料 → 燃料；装置 → エコー召喚器。
- 审校结论：**通过**。`所有者` 与既有语言用词一致；不同于已有 TBF 提示的 `操作権限`，没有混淆两个源词。
- 精确建议文本：保留原值。
- 风险：CJK 动作栏长度；43 个字符，未检查游戏内显示。所查目标语言原包没有 Pet Compass 目标语言名称，不能声称此通称已匹配原包界面。
- 需要作者决定：无需作者决定。

#### ja_jp / S5

键：`tooltip.echo_warrior.pet_collar_immaturity`；使用位置：条件悬浮提示；双源：S5。

- 目标语言：`このバージョンのエコーに子どもの姿はありません。未熟の呪いは保持されますが、効果はありません。`
- 中文直译回译：这个版本的回声没有孩子的样子。未熟的诅咒被保留，但没有效果。
- 自然中文释义：本版本英灵无幼体形态；该诅咒会保存，但不起效。
- 术语对应：此版本 → このバージョン；幼体形态 → 子どもの姿；Curse of Immaturity → 未熟の呪い。
- 审校结论：**通过**。专名符合所查 1.20.1 日语原包；没有丢失版本限制，也没有变成成长或生育规则。
- 精确建议文本：保留原值。
- 风险：长悬浮行；47 个字符，未检查游戏内显示。
- 需要作者决定：无需作者决定。

#### ru_ru / S1

键：`message.echo_warrior.pet_management_restricted`；使用位置：动作栏；双源：S1。

- 目标语言：`Управляйте отголоском через устройство призыва. Лежанки, сети для поимки и сторонняя смена владельца недоступны.`
- 中文直译回译：通过召唤装置管理回声。宠物卧床、用来捕获的网以及外部所有者变更不可用。
- 自然中文释义：该英灵应由召唤装置管理，不能通过列出的第三方途径管理或改归属。
- 术语对应：英灵 → отголосок；装置 → устройство призыва；宠物床 → лежанки；捕网 → сети для поимки；归属变更 → смена владельца。
- 审校结论：**通过**。`устройство` 明确为设备；简称没有变成职业。外部变更被限定，没有声称英灵永远不能转移。
- 精确建议文本：保留原值。
- 风险：动作栏长句；112 个字符，未检查游戏内显示。
- 需要作者决定：无需作者决定。

#### ru_ru / S2

键：`tooltip.echo_warrior.pet_collar_limits`；使用位置：条件悬浮提示；双源：S2。

- 目标语言：`«Отзыв» и «С того света» не действуют на отголосков, но сохраняются на ошейнике.`
- 中文直译回译：“召回”和“来自彼世”不作用于回声，但保留在项圈上。
- 自然中文释义：指名的两项附魔对英灵不起效，但不会从项圈上消失。
- 术语对应：Total Recall → «Отзыв»；Curse of Afterlife → «С того света»；项圈 → ошейник；保留 → сохраняются。
- 审校结论：**通过**。引号使两个原包附魔专名可识别，未把“召回”误解为本体按钮。
- 精确建议文本：保留原值。
- 风险：第三方专名自身不够直观，但必须复用且引号已消歧；80 个字符，未检查游戏内显示。
- 需要作者决定：无需作者决定。

#### ru_ru / S3

键：`tooltip.echo_warrior.pet_collar_saved`；使用位置：遗物悬浮提示；双源：S3。

- 目标语言：`Ошейник питомца сохранён: остаётся после отзыва, смерти и переноса реликвии.`
- 中文直译回译：宠物项圈已保存：收回、死亡和遗物转移后仍留下。
- 自然中文释义：收回英灵、死亡或转装遗物之后，保存的项圈仍然存在。
- 术语对应：宠物项圈 → ошейник питомца；收回 → отзыв；遗物转装 → перенос реликвии。
- 审校结论：**通过**。`отзыв` 对应现有 `Отозвать` 按钮，格变化正确；与 S2 引号内附魔名有足够语境区分。
- 精确建议文本：保留原值。
- 风险：长悬浮行；76 个字符，未检查游戏内显示。
- 需要作者决定：无需作者决定。

#### ru_ru / S4

键：`message.echo_warrior.pet_compass_unavailable`；使用位置：动作栏；双源：S4。

- 目标语言：`Компас питомцев не может выполнить это действие. Проверьте владельца, топливо и устройство призыва.`
- 中文直译回译：宠物罗盘不能执行这个动作。请检查所有者、燃料和召唤装置。
- 自然中文释义：该罗盘操作不可用，需要核对归属、燃料及装置。
- 术语对应：宠物罗盘 → компас питомцев；归属 → владелец；燃料 → топливо；召唤器装置 → устройство призыва。
- 审校结论：**建议修改**。原稿准确；改用常见“操作不可用”句式可缩短，也更直接。
- 精确建议文本：`Действие недоступно через компас питомцев. Проверьте владельца, топливо и устройство призыва.`
- 风险：动作栏长度；99 → 93 个字符，未检查游戏内显示。
- 需要作者决定：无需作者决定。

#### ru_ru / S5

键：`tooltip.echo_warrior.pet_collar_immaturity`；使用位置：条件悬浮提示；双源：S5。

- 目标语言：`В этой версии у отголосков нет детской формы. Проклятие незрелости сохраняется, но не действует.`
- 中文直译回译：在这个版本中，回声没有儿童形态。未成熟诅咒被保留，但不作用。
- 自然中文释义：本版本不支持英灵幼体形态；这项诅咒虽保存仍不生效。
- 术语对应：此版本 → в этой версии；幼体形态 → детская форма；Curse of Immaturity → Проклятие незрелости。
- 审校结论：**通过**。补译名称表达 Immaturity，符合版本限定和保留/失效的关系；没有把英灵说成动物或亡灵。
- 精确建议文本：保留原值。
- 风险：该附魔在所查俄语原包资源中缺键，名称属于本体补译；96 个字符，未检查游戏内显示。
- 需要作者决定：无需作者决定。

#### pt_br / S1

键：`message.echo_warrior.pet_management_restricted`；使用位置：动作栏；双源：S1。

- 目标语言：`Use o Invocador de Ecos para controlar este Eco. Camas de pets, redes de captura e mudanças externas de dono não estão disponíveis.`
- 中文直译回译：使用回声召唤者来控制这个回声。宠物床、捕获网和外部主人变更不可用。
- 自然中文释义：通过名为 Invocador de Ecos 的物件管理这名英灵；所列外部管理途径不可用。
- 术语对应：英灵 → Eco；召唤器正式名称 → Invocador de Ecos；宠物床 → camas de pets；归属变更 → mudanças externas de dono。
- 审校结论：**必须修改**。机制无遗漏，但需落实既有术语表“正文用 aparelho 明确装置”的要求；同时可以缩短。
- 精确建议文本：`Gerencie este Eco pelo aparelho Invocador de Ecos. Camas de pets, redes de captura e troca externa de dono não funcionam.`
- 建议稿回译：通过回声召唤装置管理这名英灵；宠物床、捕网及外部主人更换无效。
- 风险：装置/人物歧义与动作栏长度；131 → 121 个字符，仍需视觉检查。
- 需要作者决定：无需作者决定。

#### pt_br / S2

键：`tooltip.echo_warrior.pet_collar_limits`；使用位置：条件悬浮提示；双源：S2。

- 目标语言：`Chamamento total e Maldição do vivo-morto não afetam Ecos, mas permanecem na coleira.`
- 中文直译回译：全面召唤和活死人诅咒不影响回声，但留在项圈上。
- 自然中文释义：名为这两项的附魔对英灵无效，附魔仍保存于项圈。
- 术语对应：Total Recall → Chamamento total；Curse of Afterlife → Maldição do vivo-morto；项圈 → coleira；保留 → permanecem。
- 审校结论：**通过**。两项都沿用实际第三方名称；“活死人”只属于诅咒专名，没有改写英灵世界观。
- 精确建议文本：保留原值。
- 风险：第三方专名的字面含义与中文不同，但有原包证据；85 个字符，未检查游戏内显示。
- 需要作者决定：无需作者决定。

#### pt_br / S3

键：`tooltip.echo_warrior.pet_collar_saved`；使用位置：遗物悬浮提示；双源：S3。

- 目标语言：`Coleira do pet salva: mantida após dispensa, morte e transferência da relíquia.`
- 中文直译回译：宠物项圈已保存：遣散、死亡以及遗物转移后保持。
- 自然中文释义：收回英灵、英灵死亡或遗物转装都不会清除保存的项圈。
- 术语对应：项圈 → coleira；保存 → salva / mantida；收回 → dispensa；遗物转装 → transferência da relíquia。
- 审校结论：**通过**。`salva` / `mantida` 与阴性 `coleira` 配合；`dispensa` 与现有 `Dispensar` 操作一致。
- 精确建议文本：保留原值。
- 风险：长悬浮行；79 个字符，未检查游戏内显示。
- 需要作者决定：无需作者决定。

#### pt_br / S4

键：`message.echo_warrior.pet_compass_unavailable`；使用位置：动作栏；双源：S4。

- 目标语言：`Esta ação não está disponível pela bússola de pets. Verifique o dono, o combustível e o Invocador de Ecos.`
- 中文直译回译：这个动作不能通过宠物罗盘使用。检查主人、燃料及回声召唤者。
- 自然中文释义：这次罗盘操作不可用，核对英灵归属、燃料和召唤装置。
- 术语对应：宠物罗盘 → bússola de pets；归属 → dono；燃料 → combustível；召唤器 → Invocador de Ecos。
- 审校结论：**必须修改**。依既有规则明确为 `aparelho`；不应把 `dono` 擅自替换为 `permissão de controle`。
- 精确建议文本：`Ação indisponível na bússola de pets. Verifique o dono, o combustível e o aparelho Invocador de Ecos.`
- 建议稿回译：宠物罗盘中的该操作不可用；检查主人、燃料和回声召唤装置。
- 风险：装置/人物歧义与动作栏长度；106 → 101 个字符，未检查游戏内显示。
- 需要作者决定：无需作者决定。

#### pt_br / S5

键：`tooltip.echo_warrior.pet_collar_immaturity`；使用位置：条件悬浮提示；双源：S5。

- 目标语言：`Nesta versão, Ecos não têm forma de filhote. A Maldição da Imaturidade permanece, mas não tem efeito.`
- 中文直译回译：此版本中，回声没有幼崽形态。未成熟诅咒保留，但没有效果。
- 自然中文释义：本版本英灵不能呈现幼体；该诅咒会保存，但不会生效。
- 术语对应：此版本 → nesta versão；幼体 → filhote；Curse of Immaturity → Maldição da Imaturidade。
- 审校结论：**建议修改**。`filhote` 常用于动物幼崽；改为 `forma infantil` 对历史战士投影更自然，并保留同一机制。
- 精确建议文本：`Nesta versão, Ecos não têm forma infantil. A Maldição da Imaturidade permanece, mas não tem efeito.`
- 风险：幼崽用词的对象联想，不构成原稿机制错误；101 → 99 个字符。诅咒名为本体补译，不是所查葡语原包已有名称。
- 需要作者决定：无需作者决定。

#### es_es / S1

键：`message.echo_warrior.pet_management_restricted`；使用位置：动作栏；双源：S1。

- 目标语言：`Usa el Invocador de Ecos para controlar este Eco. No se permiten camas de mascotas, redes de captura ni cambios externos de dueño.`
- 中文直译回译：使用回声召唤者控制这名回声。不允许宠物床、捕获网和外部主人变更。
- 自然中文释义：应通过召唤器管理这名英灵，列出的外部管理方式不能使用。
- 术语对应：英灵 → Eco；召唤器正式名 → Invocador de Ecos；宠物床 → camas de mascotas；捕网 → redes de captura；归属 → dueño。
- 审校结论：**必须修改**。补 `dispositivo` 明确装置；`No sirven` 更直接表达功能不可用，避免偏向行为禁令。
- 精确建议文本：`Gestiona este Eco con el dispositivo Invocador de Ecos. No sirven camas de mascotas, redes de captura ni cambios externos de dueño.`
- 建议稿回译：通过回声召唤装置管理此英灵；宠物床、捕网或外部所有者变更无法起作用。
- 风险：装置/人物歧义；130 → 131 个字符。本次为保留完整名称和限制清单而增加 1 字符，不能宣称已解决动作栏宽度风险。
- 需要作者决定：无需作者决定。

#### es_es / S2

键：`tooltip.echo_warrior.pet_collar_limits`；使用位置：条件悬浮提示；双源：S2。

- 目标语言：`Retiro total y Maldición del más allá no afectan a los Ecos, pero permanecen en el collar.`
- 中文直译回译：全面撤回和彼世诅咒不影响回声，但留在项圈上。
- 自然中文释义：这两项附魔对英灵不生效，但附魔仍在项圈上。
- 术语对应：Total Recall → Retiro total；Curse of Afterlife → Maldición del más allá；项圈 → collar；保留 → permanecen。
- 审校结论：**通过**。附魔名与具有西语资源的原包一致；没有把附魔 Retiro total 等同于本体的普通收回操作。
- 精确建议文本：保留原值。
- 风险：长悬浮行；90 个字符，未检查游戏内显示。
- 需要作者决定：无需作者决定。

#### es_es / S3

键：`tooltip.echo_warrior.pet_collar_saved`；使用位置：遗物悬浮提示；双源：S3。

- 目标语言：`Collar de mascota guardado: se conserva tras retirar al Eco, su muerte o el traslado de la reliquia.`
- 中文直译回译：宠物项圈已保存：收回回声、它死亡或遗物搬移后仍被保留。
- 自然中文释义：三类事件中的任一种发生后，保存的项圈都会保留。
- 术语对应：保存项圈 → collar guardado；收回英灵 → retirar al Eco；死亡 → su muerte；遗物转装 → traslado de la reliquia。
- 审校结论：**通过**。`su muerte` 明确指英灵；列表使用 `o` 表示任一事件均保留，未削弱源文的三项保证。
- 精确建议文本：保留原值。
- 风险：长悬浮行；100 个字符，未检查游戏内显示。
- 需要作者决定：无需作者决定。

#### es_es / S4

键：`message.echo_warrior.pet_compass_unavailable`；使用位置：动作栏；双源：S4。

- 目标语言：`La brújula de mascotas no permite esta acción. Comprueba el dueño, el combustible y el Invocador de Ecos.`
- 中文直译回译：宠物罗盘不允许这个动作。检查主人、燃料和回声召唤者。
- 自然中文释义：该罗盘操作不可用，需要确认英灵归属、燃料和召唤装置。
- 术语对应：宠物罗盘 → brújula de mascotas；归属 → dueño；检查 → Comprueba；燃料 → combustible；召唤器 → Invocador de Ecos。
- 审校结论：**必须修改**。补 `dispositivo`；保留西班牙版 `Comprueba` 和归属含义。
- 精确建议文本：`Brújula de mascotas: acción no disponible. Comprueba el dueño, el combustible y el dispositivo Invocador de Ecos.`
- 建议稿回译：宠物罗盘：此操作不可用。检查主人、燃料与回声召唤装置。
- 风险：装置/人物歧义；105 → 113 个字符。名称消歧后略长，仍需动作栏视觉验收。
- 需要作者决定：无需作者决定。

#### es_es / S5

键：`tooltip.echo_warrior.pet_collar_immaturity`；使用位置：条件悬浮提示；双源：S5。

- 目标语言：`En esta versión, los Ecos no tienen forma de cría. Maldición de Inmadurez se conserva, pero no tiene efecto.`
- 中文直译回译：在此版本中，回声没有幼崽形态。未成熟诅咒保留，但没有效果。
- 自然中文释义：本版本不支持英灵幼体形态；诅咒保存下来也不生效。
- 术语对应：此版本 → en esta versión；幼体 → cría；Curse of Immaturity → Maldición de Inmadurez。
- 审校结论：**建议修改**。`cría` 更容易指动物幼崽；`forma infantil` 适合历史战士，补定冠词后句法也更自然。专名保持实际原包写法。
- 精确建议文本：`Los Ecos no tienen forma infantil en esta versión. La Maldición de Inmadurez se conserva sin efecto.`
- 风险：轻微对象联想及自然度；108 → 100 个字符，未检查游戏内显示。
- 需要作者决定：无需作者决定。

#### es_mx / S1

键：`message.echo_warrior.pet_management_restricted`；使用位置：动作栏；双源：S1。

- 目标语言：`Usa el Invocador de Ecos para controlar este Eco. No se permiten camas de mascotas, redes de captura ni cambios externos de dueño.`
- 中文直译回译：使用回声召唤者控制这个回声。不允许宠物床、捕网和外部主人变更。
- 自然中文释义：应使用召唤装置管理这名英灵，所列第三方途径不能使用。
- 术语对应：英灵 → Eco；装置正式名 → Invocador de Ecos；宠物床 → camas de mascotas；外部归属变更 → cambios externos de dueño。
- 审校结论：**必须修改**。补 `dispositivo`；建议表达在中性拉美西语同样成立，不依赖西班牙专属用法。
- 精确建议文本：`Gestiona este Eco con el dispositivo Invocador de Ecos. No sirven camas de mascotas, redes de captura ni cambios externos de dueño.`
- 建议稿回译：通过回声召唤装置管理此英灵；宠物床、捕网及外部主人变更不可用。
- 风险：装置/人物歧义；130 → 131 个字符，未检查游戏内显示。
- 需要作者决定：无需作者决定。

#### es_mx / S2

键：`tooltip.echo_warrior.pet_collar_limits`；使用位置：条件悬浮提示；双源：S2。

- 目标语言：`Retiro total y Maldición del más allá no afectan a los Ecos, pero permanecen en el collar.`
- 中文直译回译：全面撤回和彼世诅咒不影响回声，但保留在项圈上。
- 自然中文释义：指名的两项附魔对英灵不起效，附魔数据仍在项圈上。
- 术语对应：Total Recall → Retiro total；Curse of Afterlife → Maldición del más allá；保留 → permanecen；项圈 → collar。
- 审校结论：**通过**。通用西语专名和语法适用于中性拉美西语；不声称存在已核验的原包 es_mx 译名。
- 精确建议文本：保留原值。
- 风险：上游拉美语言资源缺失，实际界面名可能回退；90 个字符，未检查游戏内显示。
- 需要作者决定：无需作者决定。

#### es_mx / S3

键：`tooltip.echo_warrior.pet_collar_saved`；使用位置：遗物悬浮提示；双源：S3。

- 目标语言：`Collar de mascota guardado: se conserva tras retirar al Eco, su muerte o el traslado de la reliquia.`
- 中文直译回译：宠物项圈已保存：回声被收回、它死亡或遗物转移后被保留。
- 自然中文释义：收回、死亡、遗物转装三类事件都不会使保存的项圈丢失。
- 术语对应：宠物项圈 → collar de mascota；收回 → retirar；遗物 → reliquia；转装 → traslado。
- 审校结论：**通过**。信息完整，`tras` 的书面程度适合短提示，不构成地区用词障碍。
- 精确建议文本：保留原值。
- 风险：长悬浮行；100 个字符，未检查游戏内显示。
- 需要作者决定：无需作者决定。

#### es_mx / S4

键：`message.echo_warrior.pet_compass_unavailable`；使用位置：动作栏；双源：S4。

- 目标语言：`La brújula de mascotas no permite esta acción. Revisa el dueño, el combustible y el Invocador de Ecos.`
- 中文直译回译：宠物罗盘不允许这项动作。检查主人、燃料和回声召唤者。
- 自然中文释义：该操作不能由宠物罗盘执行，需检查归属、燃料和装置；原句没有说明具体失败原因或恢复时间。
- 术语对应：宠物罗盘 → brújula de mascotas；检查 → Revisa；归属 → dueño；燃料 → combustible；装置正式名 → Invocador de Ecos。
- 审校结论：**必须修改**。补 `dispositivo`；保留拉美基准 `Revisa`，不把归属改成控制权限。
- 精确建议文本：`Brújula de mascotas: acción no disponible. Revisa el dueño, el combustible y el dispositivo Invocador de Ecos.`
- 建议稿回译：宠物罗盘：此操作不可用。检查主人、燃料与回声召唤装置。
- 风险：装置/人物歧义；102 → 110 个字符，未检查游戏内显示。
- 需要作者决定：无需作者决定。

#### es_mx / S5

键：`tooltip.echo_warrior.pet_collar_immaturity`；使用位置：条件悬浮提示；双源：S5。

- 目标语言：`En esta versión, los Ecos no tienen forma de cría. Maldición de Inmadurez se conserva, pero no tiene efecto.`
- 中文直译回译：这个版本中的回声没有幼崽形态。未成熟诅咒保留，但没有作用。
- 自然中文释义：本版本英灵没有幼体形态；已保存的诅咒不会生效。
- 术语对应：版本限制 → en esta versión；幼体 → cría；Curse of Immaturity → Maldición de Inmadurez；保留 → se conserva。
- 审校结论：**建议修改**。用 `forma infantil` 降低动物幼崽联想，适用于中性拉美语境；保留通用西语附魔名。
- 精确建议文本：`Los Ecos no tienen forma infantil en esta versión. La Maldición de Inmadurez se conserva sin efecto.`
- 风险：轻微对象联想及上游名称回退；108 → 100 个字符，未检查游戏内显示。
- 需要作者决定：无需作者决定。

### UI 长度与验收边界

以下是 Unicode 字符数，只用于对比初稿与建议稿。它没有计量 Minecraft 字形宽度、GUI 缩放或实际显示时长，不能证明任何一句已放得下。既有更长消息的存在也不是视觉通过证据。

| 语言 | S1 初稿 → 建议 | S4 初稿 → 建议 | 优先检查 |
|---|---:|---:|---|
| zh_tw | 36 → 32 | 30 → 30 | 动作栏横向边界、繁中字形；新增附魔别名悬浮框 |
| ja_jp | 58 → 53 | 43 → 43 | CJK 动作栏横向边界、长悬浮框 |
| ru_ru | 112 → 112 | 99 → 93 | 西里尔动作栏与装置名 |
| pt_br | 131 → 121 | 106 → 101 | 长动作栏、装置名及长附魔名 |
| es_es | 130 → 131 | 105 → 113 | 补装置词后的动作栏；长悬浮框 |
| es_mx | 130 → 131 | 102 → 110 | 同上；地区别名同步 |

本审校没有启动 Minecraft，没有完成 1.21.1 或 26.1.2 的视觉验收，也没有检验真实字形、截断、自动换行、提示停留时间或资源回退后的混合语言界面。结构验证由主任务负责；审校收到的 `verification.json` 可支持候选长度与文件集合说明，不能代替语言复审或游戏内验收。

最终稿只调整译文，没有改变原有操作范围、所有权/控制权规则、附魔持久化语义或版本限定。主任务采纳后的逐字复核已完成，结果见报告开头；语言审查通过不能直接授权 `--mark-current`。

最终复审记录（2026-10-27）：已核对最终候选、两条资源线及术语表，14 条改稿逐字落实，最终 30 条语义与术语审查通过；未解决必须修改项 0，需作者决定事项 0。候选 SHA-256 为 `457d0d4bdaebc53ab87deca0c40bcd17151289d32e9a6e07efad55bbb26d0089`。游戏内 UI 尚未验收。
