# 第一批外语候选独立审校报告

审校日期：2026-09-19  
范围：六种基准语言各622键；双源为 `zh_cn` 与 `en_us`。本报告仅记录审阅结果，不是已接受语言基线。

## 结论与计数口径

六种语言均仍有必须修改项。共75张逐键审阅卡，其中52项必须修改、23项建议修改；同一问题涉及多个语言或重复显示键时逐键计数。作者只需决定一个共同的文化释义问题，其余均有可执行的修订建议或已有设计／实现证据。

|语言|必须修改|建议修改|合计|总评|
|---|---:|---:|---:|---|
|繁體中文 `zh_tw`|5|2|7|仍有必须修改项|
|日本語 `ja_jp`|7|4|11|仍有必须修改项|
|Русский `ru_ru`|13|1|14|仍有必须修改项|
|Português do Brasil `pt_br`|8|6|14|仍有必须修改项|
|Español de España `es_es`|9|5|14|仍有必须修改项|
|Español de Latinoamérica `es_mx`|10|5|15|仍有必须修改项|

“必须修改”包括明确条件遗漏、对象或类别误译、语法错误、历史确定程度改变，以及尚未裁决的文化释义。建议修改是精确度、自然度或消歧改进。UI长度在未实测前只列风险，不以字符数直接宣判截断。

## 依据与检查范围

已完整阅读根目录 `AGENTS.md`、`PROJECT.md` 5.11、`REVIEW_WORKFLOW.md`、`TERMINOLOGY.md`、`TARGET_TERMS_CORE.md` 和 `INDEPENDENT_REVIEW_PACKET.md`。逐键比较指定双源和六个基准目标文件；未阅读初译者另外的解释或推理。西班牙版与拉美版的596个同值键共享逐文语义核读结果，另独立核对26个差异键；不以“文件相同”代替它们的语言审阅。

只读复核确认：八份基准JSON均可解析，目标键集合与622键双源一致，格式占位符一致；六种目标语言在主线与1.21.1兼容线逐字节相同。两条版本线上的 `es_ar`、`es_cl`、`es_ec`、`es_uy`、`es_ve` 均与本线 `es_mx` 的SHA-256相同，符合现行别名策略。

Minecraft词汇按照给定术语表和候选中已经记录的官方映射复核，包括生物群系、盔甲／防御、击退、腐肉、灵魂沙／土、力量、再生、时运、刷子、罗盘和箱子；本轮未另行下载Mojang全量语言资源。原版生物类别的歧义另通过父任务的只读代码核实处理。历史审查比较40篇实际标题／正文，不按旧技术键名猜测文章内容，也不把本次语言审阅当成外部史料考证。

本轮没有修改任何语言JSON、术语表、state.json、代码或其他文档，没有运行 `--mark-current`，没有构建或启动游戏；唯一交付改动是本报告。不改变玩家行为或百科内容，故不需要百科更新。

## 已核实的双源差异

初次独立对照发现以下差异；父任务随后只读核对设计／调用点，并把结论提供给审校者。这里明确区分源文问题与译者自身错误，不要求作者再次确认已有事实。

|项目|两源差异／风险|核实结论与后续处理|
|---|---|---|
|残心临时闪避|中文要求命中，英文只有攻击后|PROJECT 2.4.4及两版本实现确认，实际伤害段成功降低目标生命才触发。补全英文和五种非繁中译文的命中条件。|
|残心排除伤害|中文列持续与环境伤害，英文只有持续伤害|设计和实现均排除持续状态及列举的环境伤害。补全英文及五种非繁中译文。|
|遗迹保底|中文教程 `page.discoveries.body.1` 写“往往”，英文写每处有一个|PROJECT 2.1／2.2保证每个活动遗迹恰有一个保底遗物方块。英文与五种非繁中译文的保证语气可保留；中文应统一。繁中沿中文同样有“往往”，须在相同任务同步修正，见繁中批次5审阅卡。本报告未把正确的五种外语保证语气判作错译。|
|袭击类天赋|中文“灾厄生物”、英文raiders，葡／西语又用与Pillager相同的词|两版本均用EntityTypeTags.RAIDERS，覆盖整个袭击类标签，不要求正在参加袭击；不是Pillager单体或仅Illager家族。日语襲撃者、俄语налётчики可保留；繁中及葡／西语需澄清。|
|墨西加饰品标签|中文Mexica，英文尚写Aztec|TERMINOLOGY的D3已确认文化分类采用Mexica。葡／西语不能只跟随尚未协调的英文。无需重开作者决策。|
|俄语死亡消息|译文额外加“玩家”|锥锋箭通常归因埃及弓箭手英灵；荆棘有主人时可归因玩家，无主人时归因英灵。`.player`键后缀不保证第二占位符的实体类型。|
|狸奴|中文仅给猫的昵称，英文追加raccoon servants|四种目标语言照搬了“浣熊仆人”。需作者确认是否统一改成猫的爱称并保留音译；问题A1见文末。|

## 分语言、分批审阅

### 繁體中文（zh_tw）

#### 批次1：品牌、核心系统和固定术语 — 通过

英靈、回聲召喚器、傳承、回收箱和書名含义稳定。“戰場遺蹟”在实际译文中一致，是可用的台湾写法；核心候选表仍写“戰場遺跡”，后续应协调候选表，不能把两种规范写法本身当作错字。

#### 批次2：五名英灵、文明名与技能名及技能说明 — 通过

五名英雄与召唤器／教程标题一致，技能专名未把投影误写成亡灵；关键比例、充能、距离和冷却正确。

#### 批次3：召唤器、模式、属性、状态和操作反馈 — 通过

跟随、等待、闲逛和三种警戒模式条件完整；生命、燃料20／50、最高等级30、恢复消耗及操作反馈正确。

#### 批次4：物品、遗物、饰品、传承与天赋 — 必须修改

饰品正负数值、暴击概率、额外经验及唯一装备规则保存；袭击类天赋需按实际类别澄清。

键：`trait.echo_warrior.raider_slayer.name`  
使用位置：遗物天赋悬浮提示

中文原文：灾厄杀手

英文原文：Raider Slayer

目标语言：`zh_tw`

```text
災厄殺手
```

中文直译回译：灾厄杀手。

自然中文释义：名称沿中文“灾厄”词根，容易与灾厄村民家族混同；实际按更广的袭击类标签生效。

术语对应：

- 袭击类 → 襲擊者／襲擊類生物
- 掠夺者单一实体 → 掠奪者

审校结论：必须修改

建议译文：

```text
襲擊者殺手
```

风险：Minecraft 生物类别；范围误读

审校依据：父任务只读核对确认两版本使用 EntityTypeTags.RAIDERS，指整个袭击类生物标签，且不要求该生物当前正在参加袭击。必须与 Pillager 单一实体以及仅 Illager 家族区分。建议使用集体类别词，不写成仅对正在袭击的生物生效。中文源“灾厄生物”也需协调为明确的袭击类口径。

需要作者决定：无需作者决定

---

键：`trait.echo_warrior.raider_slayer.description`  
使用位置：遗物天赋悬浮提示

中文原文：对灾厄生物造成的伤害提高20%

英文原文：Deals 20% more damage to raiders

目标语言：`zh_tw`

```text
對災厄生物造成的傷害提高20%
```

中文直译回译：对灾厄生物造成的伤害提高20%。

自然中文释义：没有明确是整个袭击类生物标签；不应让玩家理解成只限灾厄村民家族。

术语对应：

- 袭击类生物 → 襲擊類生物
- 伤害 → 傷害

审校结论：必须修改

建议译文：

```text
對襲擊類生物造成的傷害提高20%
```

风险：Minecraft 生物类别；范围误读

审校依据：父任务只读核对确认两版本使用 EntityTypeTags.RAIDERS，指整个袭击类生物标签，且不要求该生物当前正在参加袭击。必须与 Pillager 单一实体以及仅 Illager 家族区分。建议使用集体类别词，不写成仅对正在袭击的生物生效。中文源“灾厄生物”也需协调为明确的袭击类口径。

需要作者决定：无需作者决定

---

#### 批次5：教程手册、知识碎片与历史叙述 — 必须修改

40篇知识的主体内容和历史限定完整；剩余问题是繁体字义选择，“側發”“剋制”必须修正，“繫”的两处用字建议统一。

键：`knowledge.echo_warrior.entry.egypt_faience_not_glass.body`  
使用位置：知识碎片阅读器（历史文章）

中文原文：孔苏是古埃及月神，常以头戴月盘的年轻神祇出现。现存金质吊坠和“埃及蓝”小神龛都直接以他为主题；其他小像还可通过月盘、侧发和裹身衣辨认。古人把天空中的月神缩到几厘米大小，让月亮成了可以随身携带的护符。

英文原文：Khonsu was an Egyptian moon god, often shown as a youthful deity wearing the lunar disk. Surviving gold pendants and Egyptian-blue shrines name him directly, while other small figures can be recognised by the disk, sidelock, and wrapped body. The god of the sky was reduced to a few centimetres, turning the moon into a portable amulet.

目标语言：`zh_tw`

```text
孔蘇是古埃及月神，常以頭戴月盤的年輕神祇出現。現存金質吊墜和“埃及藍”小神龕都直接以他為主題；其他小像還可透過月盤、側發和裹身衣辨認。古人把天空中的月神縮到幾釐米大小，讓月亮成了可以隨身攜帶的護符。
```

中文直译回译：孔苏是古埃及月神，常以头戴月盘的年轻神祇出现。现存金质吊坠和“埃及蓝”小神龛都直接以他为主题；其他小像还可通过月盘、侧发和裹身衣辨认。古人把天空中的月神缩到几厘米大小，让月亮成了可以随身携带的护符。（目标原文把头发的“髮”错写为“發”，简体回译会掩盖这个错误。）

自然中文释义：孔苏像可以用月盘、侧边发辫和裹身衣辨认；这里指头发。

术语对应：

- 侧发 → 側髮
- 厘米 → 公分

审校结论：必须修改

建议译文：

```text
孔蘇是古埃及月神，常以頭戴月盤的年輕神祇出現。現存金質吊墜和“埃及藍”小神龕都直接以他為主題；其他小像還可透過月盤、側髮和裹身衣辨認。古人把天空中的月神縮到幾公分大小，讓月亮成了可以隨身攜帶的護符。
```

风险：简繁转换错误；台湾单位习惯

审校依据：“側發”中的“發”是发生／发出的字，不表示头发。应使用“側髮”。同句“釐米”可顺带换成台湾常用“公分”，后者属于建议润色，不是史实错误。

需要作者决定：无需作者决定

---

键：`knowledge.echo_warrior.entry.egypt_senet_rules_lost.body`  
使用位置：知识碎片阅读器（历史文章）

中文原文：中王国时期的鱼形护符可用黄金、绿石或水晶制成，有些系在儿童或年轻女性的发辫末端。罗非鱼会把卵含在口中直到孵化，因此也获得繁衍与再生的含义。尼罗河里的常见鱼由此成了贴身佩戴的保护符号。

英文原文：Middle Kingdom fish ornaments were made from gold, green stone, or crystal, and some hung from the ends of braids worn by children or young women. Tilapia brood their eggs in the mouth until they hatch, helping the fish acquire associations with fertility and rebirth. A familiar Nile fish became a protective emblem worn close to the body.

目标语言：`zh_tw`

```text
中王國時期的魚形護符可用黃金、綠石或水晶製成，有些系在兒童或年輕女性的髮辮末端。羅非魚會把卵含在口中直到孵化，因此也獲得繁衍與再生的含義。尼羅河裡的常見魚由此成了貼身佩戴的保護符號。
```

中文直译回译：中王国时期的鱼形护符可用黄金、绿石或水晶制成，有些系在儿童或年轻女性的发辫末端。罗非鱼会把卵含在口中直到孵化，因此也获得繁衍与再生的含义。尼罗河里的常见鱼由此成了贴身佩戴的保护符号。

自然中文释义：鱼形护符有的系在儿童或年轻女性的发辫末端。

术语对应：

- 系挂 → 繫

审校结论：建议修改

建议译文：

```text
中王國時期的魚形護符可用黃金、綠石或水晶製成，有些繫在兒童或年輕女性的髮辮末端。羅非魚會把卵含在口中直到孵化，因此也獲得繁衍與再生的含義。尼羅河裡的常見魚由此成了貼身佩戴的保護符號。
```

风险：简繁用字

审校依据：系在发辫上指用绳连结，台湾正文宜写“繫在”，避免把“系”作为简体动词原样保留。

需要作者决定：无需作者决定

---

键：`knowledge.echo_warrior.entry.egypt_book_of_dead_varied.body`  
使用位置：知识碎片阅读器（历史文章）

中文原文：晚期埃及护符常把阿努比斯塑成胡狼首、男性人身的小像，并在背部或头后留孔穿系。阿努比斯与木乃伊制作和亡者保护有关，很适合被做成贴身守护物。大型葬仪中的神祇，就这样缩到了可以随身携带的尺寸。

英文原文：Late Egyptian amulets often show Anubis as a jackal-headed man, with a hole or loop behind the head or back for suspension. His connection with mummification and protection of the dead made him well suited to a close-worn guardian. A god from large funerary rites was reduced to a figure small enough to carry.

目标语言：`zh_tw`

```text
晚期埃及護符常把阿努比斯塑成胡狼首、男性人身的小像，並在背部或頭後留孔穿系。阿努比斯與木乃伊製作和亡者保護有關，很適合被做成貼身守護物。大型葬儀中的神祇，就這樣縮到了可以隨身攜帶的尺寸。
```

中文直译回译：晚期埃及护符常把阿努比斯塑成胡狼首、男性人身的小像，并在背部或头后留孔穿系。阿努比斯与木乃伊制作和亡者保护有关，很适合被做成贴身守护物。大型葬仪中的神祇，就这样缩到了可以随身携带的尺寸。

自然中文释义：阿努比斯护符留孔，用绳穿系后贴身佩戴。

术语对应：

- 穿系 → 穿繫

审校结论：建议修改

建议译文：

```text
晚期埃及護符常把阿努比斯塑成胡狼首、男性人身的小像，並在背部或頭後留孔穿繫。阿努比斯與木乃伊製作和亡者保護有關，很適合被做成貼身守護物。大型葬儀中的神祇，就這樣縮到了可以隨身攜帶的尺寸。
```

风险：简繁用字

审校依据：“穿系”指穿绳系挂，繁中宜写“穿繫”；不改变考古说明或时期限定。

需要作者决定：无需作者决定

---

键：`knowledge.echo_warrior.entry.japan_bushido_changed.body`  
使用位置：知识碎片阅读器（历史文章）

中文原文：不同时代的武士都谈过忠诚、名誉、家业、勇武和克制，侧重点会随战争、主从关系和社会秩序改变。江户思想家与近代国家后来重新整理并推广“武士道”。它是一段不断被解释、重写的历史，从来没有一套供所有武士背诵的统一条文。

英文原文：Warriors in different periods discussed loyalty, honour, household survival, courage, and restraint, but the emphasis changed with warfare, lord-vassal relations, and social order. Edo thinkers and the modern state later reorganised and promoted bushido. Its history was repeatedly interpreted and rewritten; there was never one set of rules memorised by every samurai.

目标语言：`zh_tw`

```text
不同時代的武士都談過忠誠、名譽、家業、勇武和剋制，側重點會隨戰爭、主從關係和社會秩序改變。江戶思想家與近代國家後來重新整理並推廣“武士道”。它是一段不斷被解釋、重寫的歷史，從來沒有一套供所有武士背誦的統一條文。
```

中文直译回译：不同时代的武士都谈过忠诚、名誉、家业、勇武和克制，侧重点会随战争、主从关系和社会秩序改变。江户思想家与近代国家后来重新整理并推广“武士道”。它是一段不断被解释、重写的历史，从来没有一套供所有武士背诵的统一条文。（目标写的是“剋制”，即克服／压制对方的用字。）

自然中文释义：此处枚举武士讨论的品德，含克制自己、约束行为。

术语对应：

- 克制／节制 → 克制
- 克服、相克 → 剋

审校结论：必须修改

建议译文：

```text
不同時代的武士都談過忠誠、名譽、家業、勇武和克制，側重點會隨戰爭、主從關係和社會秩序改變。江戶思想家與近代國家後來重新整理並推廣“武士道”。它是一段不斷被解釋、重寫的歷史，從來沒有一套供所有武士背誦的統一條文。
```

风险：简繁转换错误；概念误用

审校依据：自我节制应写“克制”；“剋制”用于压制、相克之义。本段要保持 restraint 的品德含义。

需要作者决定：无需作者决定

---

键：`gui.echo_warrior.tutorial.page.discoveries.body.1`  
使用位置：教程手册／知识碎片

中文原文：战场考古地点往往都会有一个英灵遗物。

英文原文：Each Battlefield Ruins site contains one Echo Relic.

目标语言：`zh_tw`

```text
戰場考古地點往往都會有一個英靈遺物。
```

中文直译回译：战场考古地点往往都会有一个英灵遗物。

自然中文释义：只说通常有，没有传达每处活动遗迹保证一个遗物。

术语对应：

- 保证 → 保證
- 每处 → 每處
- 英灵遗物 → 英靈遺物

审校结论：必须修改

建议译文：

```text
每處戰場遺蹟都保證有一個英靈遺物。
```

风险：机制确定程度；源文同步

审校依据：繁中忠实沿用了中文的“往往”，不是独立译错。父任务核对PROJECT 2.1／2.2确认每个活动遗迹保证恰好一个保底遗物方块，故需随中文源文统一为明确保证；英文及另外五种目标语言可保留。这里不扩张为保底方块被人为破坏后仍必然有掉落。

需要作者决定：无需作者决定

---

#### 批次6：命令、死亡消息、标签、制作名单和其他短文本 — 通过

命令无遗物提示、死亡消息归因、标签和姓名保持一致；人员名未被当作漏译。

本语言总评：**仍有必须修改项**。先解决上述必须修改项，再进入接受基线所要求的视觉验收。

### 日本語（ja_jp）

#### 批次1：品牌、核心系统和固定术语 — 通过

エコー可稳定承担回声投影概念；エコー召喚器明确为器物，伝承／送還在游戏上下文自然。教程书名稍长但可读，无需改成灵魂相关词。

#### 批次2：五名英灵、文明名与技能名及技能说明 — 必须修改

文化技能名、五名英雄一致；残心的触发和排除条件遗漏，投射物击退的限定建议写清。

键：`gui.echo_warrior.summoner.skill.guandao.armor_clad.description.1`  
使用位置：召唤器技能悬浮提示

中文原文：投射物伤害降低50%，击退降低95%。

英文原文：Take 50% less projectile damage and 95% less projectile knockback.

目标语言：`ja_jp`

```text
飛び道具のダメージを50%、ノックバックを95%軽減する。
```

中文直译回译：飞行道具的伤害减轻50%，击退减轻95%。

自然中文释义：读者可能把后半句看成对所有击退均减免95%，没有明确同为投射物造成。

术语对应：

- 投射物 → 飛び道具
- 击退 → ノックバック

审校结论：建议修改

建议译文：

```text
飛び道具によるダメージを50%、飛び道具によるノックバックを95%軽減する。
```

风险：机制作用范围

审校依据：英文明确 projectile knockback；中文及日文的后半句均省略限定。日语可重复一次“飞行道具造成”，消除范围歧义。

需要作者决定：无需作者决定

---

键：`gui.echo_warrior.summoner.skill.samurai.zanshin.description.2`  
使用位置：召唤器技能悬浮提示

中文原文：攻击命中后1秒内额外获得20%闪避，最高80%。

英文原文：Gain 20% dodge for 1 second after attacking, up to 80% total.

目标语言：`ja_jp`

```text
攻撃後1秒間、回避率が20%増加。合計最大80%。
```

中文直译回译：攻击后1秒内增加20%闪避，合计最高80%。

自然中文释义：现在写成只要做出攻击，就得到闪避加成；没有要求命中。

术语对应：

- 命中 → 命中する
- 闪避率 → 回避率

审校结论：必须修改

建议译文：

```text
攻撃が命中すると1秒間、回避率が20%増加。合計最大80%。
```

风险：机制条件遗漏；双源差异

审校依据：中文明确“攻击命中后”，英文仅写 after attacking。译文沿用了英文的较宽触发条件。按中文设计语义补回命中要求；实际触发条件需统一双源。父任务只读核对 PROJECT 2.4.4 和双版本实现后确认：只有实际伤害段成功降低目标生命才触发，中文完整条件正确。

需要作者决定：无需作者决定

---

键：`gui.echo_warrior.summoner.skill.samurai.zanshin.description.3`  
使用位置：召唤器技能悬浮提示

中文原文：成功闪避整次攻击并获得1层踏込；持续与环境伤害无法闪避。

英文原文：A dodge negates the hit and grants 1 Fumikomi charge; ongoing damage cannot be dodged.

目标语言：`ja_jp`

```text
回避すると攻撃を無効化し、踏み込みを1回分得る。継続ダメージは回避不可。
```

中文直译回译：闪避使这次攻击无效并获得1次踏込；持续伤害不能闪避。

自然中文释义：译文只排除了持续伤害，未告诉玩家环境伤害同样不可闪避。

术语对应：

- 持续／环境伤害 → 継続／環境ダメージ
- 踏込 → 踏み込み

审校结论：必须修改

建议译文：

```text
回避すると攻撃を無効化し、踏み込みを1回分得る。継続ダメージと環境ダメージは回避不可。
```

风险：机制条件遗漏；双源差异

审校依据：中文明确“持续与环境伤害”，英文只剩 ongoing damage，译文随英文遗漏环境伤害。俄语另把“伤害”写成会主动闪避的主语，需一并改为不能躲避某类伤害。父任务只读核对设计和双版本实现后确认：持续状态及列举的环境伤害均不可闪避，按中文补全即可。

需要作者决定：无需作者决定

---

#### 批次3：召唤器、模式、属性、状态和操作反馈 — 通过

动作与警戒模式、召唤和收回操作、等级／燃料／槽位／反馈可理解；数值与双源一致。

#### 批次4：物品、遗物、饰品、传承与天赋 — 必须修改

饰品机制和天赋大体正确；罗马片甲物品名与知识正文使用了不同构形名，必须统一。袭击类使用襲撃者，未缩为ピリジャー。

键：`item.echo_warrior.plate_armor_accessory`  
使用位置：饰品名称／物品悬浮提示

中文原文：罗马环片坠

英文原文：Roman Segmental-Armor Pendant

目标语言：`ja_jp`

```text
ローマ環状甲のペンダント
```

中文直译回译：罗马环状甲的吊坠。

自然中文释义：物品名把分节片甲称为环状甲；同语言的知识正文却正确称为分节甲。

术语对应：

- 环片甲／分节甲 → 分節甲
- 环状 → 環状

审校结论：必须修改

建议译文：

```text
ローマ分節甲のペンダント
```

风险：文化专名；术语不一致

审校依据：英文 Segmental-Armor 和 knowledge...roman_vindolanda_socks 的日语“分節甲”相互对应。“環状甲”按环片汉字转译成了另一种构形名称，应与知识条目统一。

需要作者决定：无需作者决定

---

#### 批次5：教程手册、知识碎片与历史叙述 — 必须修改

历史、机制与语气问题分别列卡：龟甲阵崩溃的过强断言、唇饰专名、战车实体玩法暗示及“保证／大概”的反转须处理；其余自然度建议不要求作者判断日语。

键：`knowledge.echo_warrior.entry.roman_tortoise_limits.body`  
使用位置：知识碎片阅读器（历史文章）

中文原文：接近城墙或遭受投射物时，罗马士兵会把盾牌交叠成龟甲阵。严密遮护换来的是更慢的移动、更窄的视野和拥挤的近战空间；地面与队列稍有问题，阵形也会受影响。它适合特定局面，远非全天候、刀枪不入的移动堡垒。

英文原文：When approaching walls or coming under missile fire, Roman soldiers could overlap their shields into a testudo. The protection came at a cost: slower movement, poorer visibility, and little room for close combat, while the formation depended on suitable ground and tight order. It suited particular situations; it was no all-day, invulnerable shell.

目标语言：`ja_jp`

```text
城壁へ近づくときや飛び道具を浴びるとき、ローマ兵は盾を重ねてテストゥド（亀甲陣）を組んだ。厳重な防御と引き換えに、移動は遅く、視界は狭まり、接近戦の空間も窮屈になる。地面の状態や隊列が少し乱れるだけでも陣形は崩れた。特定の状況には強いが、一日中無敵でいられる移動要塞ではない。
```

中文直译回译：接近城墙或遭受投射物时，罗马兵叠盾组成龟甲阵。严密防御换来更慢移动、变窄视野和拥挤的近战空间。只要地面状态或队列稍乱，阵形就崩溃了。它在特定情况很强，并不是能整天无敌的移动堡垒。

自然中文释义：原文说地面和队列会影响阵形；日语改成轻微变化必然使阵形崩溃。

术语对应：

- 受影响 → 影響を受けることもあった
- 崩溃 → 崩れる

审校结论：必须修改

建议译文：

```text
城壁へ近づくときや飛び道具を浴びるとき、ローマ兵は盾を重ねてテストゥド（亀甲陣）を組んだ。厳重な防御と引き換えに、移動は遅く、視界は狭まり、接近戦の空間も窮屈になる。地面の状態や隊列の乱れによって、陣形が影響を受けることもあった。特定の状況には強いが、一日中無敵でいられる移動要塞ではない。
```

风险：历史确定程度过强

审校依据：“だけでも…崩れた”把受影响推成明确崩溃，强于中文“会受影响”和英文依赖合适地面与紧密队列的描述。

需要作者决定：无需作者决定

---

键：`knowledge.echo_warrior.entry.aztec_chinampa_not_floating.body`  
使用位置：知识碎片阅读器（历史文章）

中文原文：墨西加金质鹰首饰是一枚唇栓（tentetl）。后端穿过下唇下方的孔洞，鹰头贴在嘴边，金属鹰喙会随说话和动作格外醒目。大都会艺术博物馆将这类华贵唇饰与精英权力和军事服饰联系起来。

英文原文：This Mexica gold eagle head is a labret, or tentetl. Its stem passed through a piercing below the lower lip, leaving the eagle beside the mouth, where the metal beak would catch the eye during speech and movement. The Met links such lavish ornaments with elite power and military regalia.

目标语言：`ja_jp`

```text
このメシカの金製鷲頭は、テネテトルと呼ばれる唇飾りである。軸を下唇の下の穴へ通すと鷲が口元に現れ、話したり動いたりするたび金のくちばしが目を引いた。メトロポリタン美術館は、こうした豪華な装身具を支配層の権力や軍装と結び付けている。
```

中文直译回译：这件墨西加金质鹰首是称为“特内特托鲁”的唇饰。把杆穿过下唇下方的孔后，鹰出现在嘴边，说话、活动时金喙引人注目。大都会艺术博物馆将这类豪华饰品和统治阶层权力、军装联系起来。

自然中文释义：目标的音译多出一个音节；原文拉丁专名为 tentetl。

术语对应：

- 唇栓／唇饰 → 唇飾り
- 专名 → tentetl

审校结论：必须修改

建议译文：

```text
このメシカの金製鷲頭は、唇飾り（tentetl）である。軸を下唇の下の穴へ通すと鷲が口元に現れ、話したり動いたりするたび金のくちばしが目を引いた。メトロポリタン美術館は、こうした豪華な装身具を支配層の権力や軍装と結び付けている。
```

风险：文化专名拼写

审校依据：テネテトル 并不对应源文 tentetl 的字母分节。为不在审校中发明新的日语定名，建议直接保留拉丁基准与通俗释义；后续有可靠日文惯用名时再统一。

需要作者决定：无需作者决定

---

键：`knowledge.echo_warrior.entry.japan_yari_battlefield.body`  
使用位置：知识碎片阅读器（历史文章）

中文原文：战国时代的长枪有多种长度和刃形，足轻与高阶武士都可能使用。长柄扩大了威胁范围，也方便集体作战。打刀适合随身携带、近战和表明身份，大型战场上却远不只有拔刀单挑。

英文原文：Sengoku yari came in many lengths and blade shapes, and were used by both foot soldiers and high-ranking warriors. Their long shafts extended reach and suited collective fighting. The sword remained useful for carrying, close combat, and displaying status, but large battlefields held far more than formal sword duels.

目标语言：`ja_jp`

```text
戦国時代の槍にはさまざまな長さと穂先があり、足軽にも高位の武士にも使われた。長い柄は間合いを広げ、集団戦にも向いていた。刀は携行、接近戦、身分の表示に役立ったが、大きな戦場は礼儀正しい剣術試合とはまるで違っていた。
```

中文直译回译：战国的枪长短和枪头各异，足轻、高阶武士都用。长柄扩大距离，适合集体战斗。刀用于携带、近战和表示身份，但大型战场与礼貌规矩的剑术比赛完全不同。

自然中文释义：最后一句把“并不只有拔刀单挑”换成了“礼貌的剑术比赛”，增加了原文没有的礼仪评价。

术语对应：

- 单挑 → 一騎打ち
- 剑术比赛 → 剣術試合

审校结论：建议修改

建议译文：

```text
戦国時代の槍にはさまざまな長さと穂先があり、足軽にも高位の武士にも使われた。長い柄は間合いを広げ、集団戦にも向いていた。刀は携行、接近戦、身分の表示に役立ったが、大きな戦場で行われたのは、刀による一騎打ちだけではなかった。
```

风险：历史叙述语气偏移

审校依据：formal sword duels 指正式的单挑形式，不能自动扩成礼儀正しい剣術試合。保持“战场上还有许多其他作战形式”的原意。

需要作者决定：无需作者决定

---

键：`gui.echo_warrior.tutorial.page.summoner.body.6`  
使用位置：教程手册

中文原文：绝不会出现幻觉或因果问题。

英文原文：No strange visions or causal problems. Promise.

目标语言：`ja_jp`

```text
奇妙な幻覚も因果関係の乱れも起きません。たぶん。
```

中文直译回译：不会出现奇怪的幻觉，也不会发生因果紊乱。大概吧。

自然中文释义：源文是在一本正经地保证安全；末尾擅自变成不确定、心虚的保证。

术语对应：

- 保证 → 保証します
- 大概 → たぶん

审校结论：必须修改

建议译文：

```text
奇妙な幻覚も因果関係の乱れも起きません。保証します。
```

风险：语气与确定程度偏差

审校依据：中文“绝不会”和英文 Promise 都是坚决承诺；たぶん 为“也许／大概”，反转笑点的说话姿态。保留广告式保证即可，不需作者判断日语语法。

需要作者决定：无需作者决定

---

键：`gui.echo_warrior.tutorial.page.egyptian_archer.body.2`  
使用位置：教程手册

中文原文：对猫神的崇拜使其天生就是苦力怕和亡灵生物的克星。

英文原文：Devotion to the cat goddess makes this Echo a natural foe of creepers and undead.

目标语言：`ja_jp`

```text
猫の女神への信仰により、クリーパーとアンデッドを得意とします。
```

中文直译回译：因对猫女神的信仰，擅长苦力怕和亡灵。

自然中文释义：“擅长”直接支配两类生物，缺少擅长与它们战斗这一层关系。

术语对应：

- 克星／擅长对付 → ～との戦いを得意とする
- 亡灵 → アンデッド

审校结论：建议修改

建议译文：

```text
猫の女神への信仰により、クリーパーやアンデッドとの戦いを得意とします。
```

风险：日语搭配自然度

审校依据：“クリーパーとアンデッドを得意とする”像把生物当作擅长的科目；补“との戦い”即可自然表达克星之意。

需要作者决定：无需作者决定

---

键：`gui.echo_warrior.tutorial.page.egyptian_archer.body.3`  
使用位置：教程手册

中文原文：多种箭矢、战车和齐射的传承，使其拥有灵活走位与多重射击能力。

英文原文：Special arrows, chariots, and volleys provide agile movement and multiple shots.

目标语言：`ja_jp`

```text
特殊な矢、戦車、一斉射撃を使い、軽快に動きながら何本もの矢を放ちます。
```

中文直译回译：使用特殊的箭、战车和齐射，一边轻快移动，一边放出许多支箭。

自然中文释义：日语明确说角色会使用战车，中文讲的是继承战车与齐射的传统带来的能力。

术语对应：

- 战车 → 戦車
- 传承的技艺 → 伝統を受け継ぐ技術
- 多重射击 → 複数の矢による射撃

审校结论：必须修改

建议译文：

```text
多彩な矢と、戦車兵や斉射の伝統を受け継ぐ技術により、軽快な移動と複数の矢による射撃を得意とします。
```

风险：机制凭空增加；双源压缩

审校依据：“戦車…を使い”会让玩家期待角色实际驾车。中文“传承”及技能“战车与齐射之魂”要求表达传统／能力来源；英文简写也应复核，不能据此增添坐骑玩法。

需要作者决定：无需作者决定

---

键：`gui.echo_warrior.tutorial.page.japanese_samurai.body.1`  
使用位置：教程手册

中文原文：一名已入心流的日本武士。

英文原文：A Japanese samurai who has entered a state of flow.

目标语言：`ja_jp`

```text
無心の境地へ入った日本の侍です。
```

中文直译回译：进入无心境界的日本武士。

自然中文释义：心理学上的高度专注心流被转换成带武道或禅意的无心境界。

术语对应：

- 心流 → フロー状態
- 无心境界 → 無心の境地

审校结论：建议修改

建议译文：

```text
フロー状態に入った日本の侍です。
```

风险：文化含义偏移

审校依据：无心可以营造武道气氛，但双源明确写心流／state of flow。直接采用日语フロー状態可避免追加修行境界设定。

需要作者决定：无需作者决定

---

#### 批次6：命令、死亡消息、标签、制作名单和其他短文本 — 通过

死亡消息的命名与效果名称相符，保留任意实体占位符；署名、机构和按钮用语可接受。

本语言总评：**仍有必须修改项**。先解决上述必须修改项，再进入接受基线所要求的视觉验收。

### Русский（ru_ru）

#### 批次1：品牌、核心系统和固定术语 — 通过

Отголосок没有亡灵／复活含义；Устройство призыва明确为装置。Наследие符合事迹与品质的传承，Отозвать符合保留数据的收回。回收箱复合词эхо-предметов可理解为回声相关物品；未把它用作英雄实体的新名称。

#### 批次2：五名英灵、文明名与技能名及技能说明 — 必须修改

五名英雄与技能专名稳定；残心条件、命中目标限定、举盾冲锋的推开结果必须修正。层数用词建议改得更清楚。

键：`gui.echo_warrior.summoner.skill.guandao.growing_valor.description.1`  
使用位置：召唤器技能悬浮提示

中文原文：每次有效攻击获得6%攻击力，最多5层，持续8秒。

英文原文：Effective attacks grant 6% attack damage, up to 5 stacks for 8 seconds.

目标语言：`ru_ru`

```text
Успешные атаки дают +6% к урону, до 5 эффектов на 8 секунд.
```

中文直译回译：成功攻击带来6%伤害，持续8秒，最多5种效果。

自然中文释义：把叠加层数写成最多五个效果，可能被读作获得五种不同效果。

术语对应：

- 叠层 → эффект суммируется
- 伤害 → урон

审校结论：建议修改

建议译文：

```text
Успешные атаки дают +6% к урону; эффект суммируется до 5 раз и длится 8 секунд.
```

风险：机制术语不清

审校依据：采用“效果最多叠加五次”明确层数；6%、5、8均保留。

需要作者决定：无需作者决定

---

键：`gui.echo_warrior.summoner.skill.guandao.crescent_blade.description.2`  
使用位置：召唤器技能悬浮提示

中文原文：3.25格内所有命中目标受到完整伤害。

英文原文：Every target hit within 3.25 blocks takes full damage.

目标语言：`ru_ru`

```text
Все цели в пределах 3,25 блока получают полный урон.
```

中文直译回译：3.25格以内的所有目标都受到完整伤害。

自然中文释义：缺少“被命中的”，读起来像范围内所有目标都会自动受伤。

术语对应：

- 命中的目标 → поражённые цели
- 完整伤害 → полный урон

审校结论：必须修改

建议译文：

```text
Все поражённые цели в пределах 3,25 блока получают полный урон.
```

风险：命中条件遗漏

审校依据：双源都限定 target hit／命中目标。范围斩击不等于对范围内每一个实体无条件结算。

需要作者决定：无需作者决定

---

键：`gui.echo_warrior.summoner.skill.samurai.zanshin.description.2`  
使用位置：召唤器技能悬浮提示

中文原文：攻击命中后1秒内额外获得20%闪避，最高80%。

英文原文：Gain 20% dodge for 1 second after attacking, up to 80% total.

目标语言：`ru_ru`

```text
После атаки на 1 секунду даёт +20% к уклонению, но не более 80% суммарно.
```

中文直译回译：攻击后1秒内增加20%闪避，合计最高80%。

自然中文释义：现在写成只要做出攻击，就得到闪避加成；没有要求命中。

术语对应：

- 命中 → попадание
- 闪避率 → шанс уклонения

审校结论：必须修改

建议译文：

```text
После попадания атакой шанс уклонения повышается на 20% на 1 секунду, максимум — до 80%.
```

风险：机制条件遗漏；双源差异

审校依据：中文明确“攻击命中后”，英文仅写 after attacking。译文沿用了英文的较宽触发条件。按中文设计语义补回命中要求；实际触发条件需统一双源。父任务只读核对 PROJECT 2.4.4 和双版本实现后确认：只有实际伤害段成功降低目标生命才触发，中文完整条件正确。

需要作者决定：无需作者决定

---

键：`gui.echo_warrior.summoner.skill.samurai.zanshin.description.3`  
使用位置：召唤器技能悬浮提示

中文原文：成功闪避整次攻击并获得1层踏込；持续与环境伤害无法闪避。

英文原文：A dodge negates the hit and grants 1 Fumikomi charge; ongoing damage cannot be dodged.

目标语言：`ru_ru`

```text
Уклонение полностью отменяет удар и даёт 1 заряд Фумикоми; периодический урон не уклоняется.
```

中文直译回译：闪避使这次攻击无效并获得1次踏込；持续伤害不能闪避。

自然中文释义：译文只排除了持续伤害，未告诉玩家环境伤害同样不可闪避。

术语对应：

- 持续／环境伤害 → периодический урон／урон окружающей среды
- 踏込 → Фумикоми

审校结论：必须修改

建议译文：

```text
Уклонение полностью отменяет удар и даёт 1 заряд Фумикоми; от периодического урона и урона окружающей среды уклониться нельзя.
```

风险：机制条件遗漏；双源差异；语法

审校依据：中文明确“持续与环境伤害”，英文只剩 ongoing damage，译文随英文遗漏环境伤害。俄语另把“伤害”写成会主动闪避的主语，需一并改为不能躲避某类伤害。父任务只读核对设计和双版本实现后确认：持续状态及列举的环境伤害均不可闪避，按中文补全即可。

需要作者决定：无需作者决定

---

键：`gui.echo_warrior.summoner.skill.roman.charge.description.2`  
使用位置：召唤器技能悬浮提示

中文原文：冲开威胁主人的投射物或即将爆炸的苦力怕。

英文原文：Charges away projectiles threatening the owner or creepers about to explode.

目标语言：`ru_ru`

```text
Устремляется к снарядам, угрожающим владельцу, и готовым взорваться криперам.
```

中文直译回译：冲向威胁主人的投射物，以及即将爆炸的苦力怕。

自然中文释义：只写出了冲过去，没有写把威胁冲开这一效果。

术语对应：

- 冲刺 → рывком
- 推开 → отталкивает
- 苦力怕 → крипер

审校结论：必须修改

建议译文：

```text
Рывком отталкивает снаряды, угрожающие владельцу, или криперов, готовых взорваться.
```

风险：机制结果遗漏

审校依据：Устремляется к 表示奔向，缺少 source charges away／冲开。补出 отталкивает，并让教程与技能提示保持一致。

需要作者决定：无需作者决定

---

#### 批次3：召唤器、模式、属性、状态和操作反馈 — 必须修改

自然恢复模板插入主格术语后不合语法，动态距离的单位变化也需处理；其余模式与燃料信息准确。

键：`item.echo_warrior.test_echo_summoner.tooltip.detail.healing`  
使用位置：召唤器物品悬浮提示

中文原文：%s的自然生命回复也需要消耗%s

英文原文：Natural health regeneration for the %s also consumes %s

目标语言：`ru_ru`

```text
Естественное восстановление здоровья %s также расходует %s
```

中文直译回译：自然恢复%s的生命值也会消耗%s。

自然中文释义：插入术语后是“恢复生命〔回声：主格〕”，但这个名词在现句中需要属格。

术语对应：

- 英灵 → отголосок（主格）
- 燃料 → топливо

审校结论：必须修改

建议译文：

```text
При естественном восстановлении здоровья %s расходует %s
```

风险：占位符拼接语法

审校依据：term.echo 是 отголосок。直接套入 Естественное восстановление здоровья %s 后应为 отголоска，现有共享词不变格。建议改成“回声消耗燃料”的主语结构，保留两个%s顺序。

需要作者决定：无需作者决定

---

键：`gui.echo_warrior.summoner.attribute.alert_range`  
使用位置：召唤器界面／模式与属性提示

中文原文：警戒范围：%s格

英文原文：Alert Range: %s blocks

目标语言：`ru_ru`

```text
Радиус боя: %s блоков
```

中文直译回译：战斗半径：%s格〔固定用块的复数属格〕。

自然中文释义：所有数值均接 блоков，遇到22、24等结尾时格变化错误。

术语对应：

- 格／方块 → блок
- 以方块计 → в блоках

审校结论：必须修改

建议译文：

```text
Радиус боя (в блоках): %s
```

风险：数字与格变化

审校依据：俄语数词后的单位随末位变化。固定 %s блоков 对所有动态数值不成立；采用“以方块计”的标签结构，不依赖运行时变格。此项不表示半径数值本身错误。

需要作者决定：无需作者决定

---

#### 批次4：物品、遗物、饰品、传承与天赋 — 必须修改

主要数值、概率与对象正确；知识碎片合成句拼接的工具格不匹配必须修正。袭击类налётчики没有缩成原版单体разбойник。

键：`item.echo_warrior.knowledge_fragment.detail.craft`  
使用位置：知识碎片物品悬浮提示

中文原文：+和其他%s一同合成为%s！

英文原文：+ Combine with other %s to make a %s!

目标语言：`ru_ru`

```text
+ Объедините с другими %s, чтобы создать %s!
```

中文直译回译：把它与其他%s组合，制成%s。

自然中文释义：实际拼接为 с другими фрагменты знаний：介词结构要求工具格，填入的术语却是主格／宾格复数。

术语对应：

- 知识碎片 → фрагменты знаний
- 这些和其他 → эти и другие

审校结论：必须修改

建议译文：

```text
+ Объедините эти и другие %s, чтобы создать %s!
```

风险：占位符拼接语法

审校依据：现有 term.fragment 为 фрагменты знаний，不能放在 с другими 后。改成直接宾语“这些和其他碎片”，无需改共享名词，也保留两个占位符顺序。

需要作者决定：无需作者决定

---

#### 批次5：教程手册、知识碎片与历史叙述 — 必须修改

完整历史条目基本保留限定语；“狸奴”译义待作者裁决，知识合集数量变格和重复出现的冲锋描述须同步修正。

键：`item.echo_warrior.knowledge_fragment_collection.summary`  
使用位置：知识碎片集物品摘要

中文原文：收录 %s 种 · 共 %s 张

英文原文：%s entries · %s fragments

目标语言：`ru_ru`

```text
%s записей · %s фрагментов
```

中文直译回译：%s条目 · %s碎片〔两处都固定为复数属格〕。

自然中文释义：数值为1、2、21、22等时，固定名词形式会出错。

术语对应：

- 条目数 → Записей
- 碎片数 → Фрагментов

审校结论：必须修改

建议译文：

```text
Записей: %s · Фрагментов: %s
```

风险：数字与格变化

审校依据：用数量标签作表头，适配零、单数与各类复数；不需要增加占位符或专门的俄语复数代码。

需要作者决定：无需作者决定

---

键：`knowledge.echo_warrior.entry.china_guandao_late.body`  
使用位置：知识碎片阅读器（历史文章）

中文原文：古人常把猫昵称为“狸奴”。传为宋人的《戏猫图》画了八只猫在庭园嬉戏，连正午缩成细线的瞳孔都没有漏掉。明代《猫蝶图》又借“猫蝶”与“耄耋”的谐音祝寿；画里的猫保留着生活观察，也承担吉祥寓意。

英文原文：Historical Chinese writers affectionately called cats linu, or “raccoon servants”. A painting traditionally attributed to a Song artist shows eight cats playing in a garden and even catches their pupils narrowed to slits at noon. A Ming Cat and Butterfly painting turns the shared sound of “cat and butterfly” and “advanced age” into a wish for longevity; the painted cats carry close observation and auspicious meaning together.

目标语言：`ru_ru`

```text
Китайские авторы ласково называли кошек «лину», то есть «слуга-енот». На картине, которую традиционно приписывают художнику эпохи Сун, восемь кошек играют в саду, а их полуденные зрачки сужены в щели. Минская «Кошка и бабочка» превращает созвучие слов «кошка и бабочка» и «почтенный возраст» в пожелание долголетия, соединяя наблюдение и благой знак.
```

中文直译回译：古代中国作者亲昵地把猫叫作“狸奴”，也就是“浣熊仆人”。一幅传统上归于宋代画家的画描绘八只猫在庭园玩耍，连中午细缝般的瞳孔也画出。明代猫蝶画利用“猫蝶”与“高龄”的近音祝寿，把观察与吉祥寓意结合起来。

自然中文释义：后面的画作归属限定与谐音寓意都保留了；问题是把猫的昵称直接解释成了“浣熊仆人”。

术语对应：

- 狸奴 → лину
- 浣熊 → енот

审校结论：必须修改

建议译文：

```text
Китайские авторы ласково называли кошек «лину». На картине, которую традиционно приписывают художнику эпохи Сун, восемь кошек играют в саду, а их полуденные зрачки сужены в щели. Минская «Кошка и бабочка» превращает созвучие слов «кошка и бабочка» и «почтенный возраст» в пожелание долголетия, соединяя наблюдение и благой знак.
```

风险：文化释义；双源差异

审校依据：这是英文 raccoon servants 带入的解释，中文只说明“狸奴”是猫的昵称。两源并未共同确认这个具体动物释义；不应让目标语言把追加的词源解释坐实。建议保留 linu 和“猫的爱称”，不增添动物义。本报告不作外部词源考证。

需要作者决定：问题 A1：是否将“狸奴”统一解释为古人对猫的爱称，保留音译并删除“浣熊仆人”的字面解释？建议采用这一做法，英文也需同步。

---

键：`gui.echo_warrior.tutorial.skill.roman.charge.description.2`  
使用位置：教程手册（罗马技能卡）

中文原文：冲开威胁主人的投射物或即将爆炸的苦力怕。

英文原文：Charges away projectiles threatening the owner or creepers about to explode.

目标语言：`ru_ru`

```text
Устремляется к снарядам, угрожающим владельцу, и готовым взорваться криперам.
```

中文直译回译：冲向威胁主人的投射物，以及即将爆炸的苦力怕。

自然中文释义：只写出了冲过去，没有写把威胁冲开这一效果。

术语对应：

- 冲刺 → рывком
- 推开 → отталкивает
- 苦力怕 → крипер

审校结论：必须修改

建议译文：

```text
Рывком отталкивает снаряды, угрожающие владельцу, или криперов, готовых взорваться.
```

风险：机制结果遗漏

审校依据：Устремляется к 表示奔向，缺少 source charges away／冲开。补出 отталкивает，并让教程与技能提示保持一致。

需要作者决定：无需作者决定

---

#### 批次6：命令、死亡消息、标签、制作名单和其他短文本 — 必须修改

两条死亡消息误加“玩家”，存活数量固定变格也需修正；其余命令、标签和人员署名可接受。

键：`death.attack.armor_piercing_arrow.player`  
使用位置：死亡消息

中文原文：%1$s被%2$s的锥锋箭射杀

英文原文：%1$s was slain by %2$s's cone-point arrow

目标语言：`ru_ru`

```text
%1$s был убит конической стрелой игрока %2$s
```

中文直译回译：%1$s被玩家%2$s的锥锋箭杀死。

自然中文释义：把第二个任意攻击实体的名字强行标成玩家。

术语对应：

- 玩家 → игрок
- 伤害来源 → источник
- 锥锋箭 → коническая стрела

审校结论：必须修改

建议译文：

```text
%1$s был убит конической стрелой (источник: %2$s)
```

风险：占位符语义；新增对象限定

审校依据：双源仅写%2$s而未限定玩家。父任务只读核对调用点进一步确认：常见锥锋箭造成者是埃及弓箭手英灵。键后缀 .player 不能当作显示对象类型保证。中性来源标签也避开任意显示名的格变化问题。

需要作者决定：无需作者决定

---

键：`death.attack.spiked_armor_reflection.player`  
使用位置：死亡消息

中文原文：%1$s在攻击%2$s时被黑曜荆棘符反伤击败

英文原文：%1$s was defeated by %2$s's Obsidian Thorn Talisman

目标语言：`ru_ru`

```text
%1$s был повержен талисманом обсидиановых шипов игрока %2$s
```

中文直译回译：%1$s被玩家%2$s的黑曜荆棘符击败。

自然中文释义：同样把任意伤害归因实体硬写成玩家，而且没有表达反伤。

术语对应：

- 反伤 → отражённый урон
- 来源 → источник
- 黑曜荆棘符 → талисман обсидиановых шипов

审校结论：必须修改

建议译文：

```text
%1$s погиб от отражённого урона талисмана обсидиановых шипов (источник: %2$s)
```

风险：占位符语义；新增对象限定

审校依据：双源没有“玩家”限定。父任务确认：荆棘有主人时可归因玩家，无主人时归因英灵。用中性来源标签容纳所有显示名，并保留中文的反伤含义。

需要作者决定：无需作者决定

---

键：`message.echo_warrior.echo_count_performance_warning`  
使用位置：聊天／性能反馈

中文原文：你当前拥有 %s 名存活英灵；过多英灵可能影响服务器性能。

英文原文：You currently control %s living Echo Warriors; large groups may affect server performance.

目标语言：`ru_ru`

```text
Сейчас под вашим управлением %s живых воинов-отголосков; большие группы могут повлиять на производительность сервера.
```

中文直译回译：你目前控制%s名存活回声战士；大群体可能影响服务器性能。

自然中文释义：数量后固定用复数属格，1名、2名、21名等场景语法错误。

术语对应：

- 存活回声战士 → живые воины-отголоски
- 服务器性能 → производительность сервера

审校结论：必须修改

建议译文：

```text
Сейчас под вашим управлением живых воинов-отголосков: %s. Большие группы могут повлиять на производительность сервера.
```

风险：数字与格变化

审校依据：固定 %s живых воинов-отголосков 不适配所有数量。移动数量到标签后，保留存活、服务器性能、可能影响三层含义。

需要作者决定：无需作者决定

---

本语言总评：**仍有必须修改项**。先解决上述必须修改项，再进入接受基线所要求的视觉验收。

### Português do Brasil（pt_br）

#### 批次1：品牌、核心系统和固定术语 — 建议修改

Eco作为可召唤投影稳定；Legado／Talento／Habilidade层次清楚。既定Invocador de Ecos可保留，在首次正文补出aparelho即可消除人物歧义。Desmonte作为拆解取材的系统词可用。

键：`gui.echo_warrior.tutorial.page.summoner.body.1`  
使用位置：教程手册

中文原文：扫描遗物数据，重建英灵战士的实体影像。

英文原文：Scan relic data and reconstruct an Echo Warrior projection.

目标语言：`pt_br`

```text
Escaneie os dados de uma relíquia e reconstrua a projeção de um Guerreiro do Eco.
```

中文直译回译：扫描遗物数据，重建一个回声战士的投影。

自然中文释义：句子像是在命令玩家扫描；正式物品名中的“召唤者”也可指人，首次说明可直接补出这是装置。

术语对应：

- 装置 → aparelho
- 英灵投影 → projeção de um Guerreiro do Eco

审校结论：建议修改

建议译文：

```text
Este aparelho lê os dados de uma relíquia e reconstrói a projeção de um Guerreiro do Eco.
```

风险：人物／装置歧义

审校依据：Invocador de Ecos 在装有燃料、槽位的物品上下文可用，且项目已同意该名称；无需推翻正式名。在说明首次出现时用 aparelho／dispositivo 消歧即可。

需要作者决定：无需作者决定

---

#### 批次2：五名英灵、文明名与技能名及技能说明 — 必须修改

英雄、文明和文化专名总体稳定；残心条件须补，战车用词建议和教程的biga统一。

键：`gui.echo_warrior.summoner.skill.egyptian.chariot_volley.name`  
使用位置：召唤器技能悬浮提示

中文原文：战车与齐射之魂

英文原文：Spirit of Chariot and Volley

目标语言：`pt_br`

```text
Espírito da Carruagem e da Salva
```

中文直译回译：马车与齐射之魂。

自然中文释义：Carruagem 容易理解成普通马车，教程另一段却使用更明确的古代战车 biga。

术语对应：

- 战车 → Biga
- 齐射 → Salva

审校结论：建议修改

建议译文：

```text
Espírito da Biga e da Salva
```

风险：文化用词；术语不一致

审校依据：gui...egyptian_archer.body.3 已用 bigas。统一为 Biga 更清楚表示古代战车，也比加一长串战车解释更适合技能标题。

需要作者决定：无需作者决定

---

键：`gui.echo_warrior.summoner.skill.samurai.zanshin.description.2`  
使用位置：召唤器技能悬浮提示

中文原文：攻击命中后1秒内额外获得20%闪避，最高80%。

英文原文：Gain 20% dodge for 1 second after attacking, up to 80% total.

目标语言：`pt_br`

```text
Recebe 20% de esquiva por 1 segundo após atacar, até 80% no total.
```

中文直译回译：攻击后1秒内增加20%闪避，合计最高80%。

自然中文释义：现在写成只要做出攻击，就得到闪避加成；没有要求命中。

术语对应：

- 命中 → acertar
- 闪避 → esquiva

审校结论：必须修改

建议译文：

```text
Após acertar um ataque, ganha 20% de esquiva por 1 segundo, até 80% no total.
```

风险：机制条件遗漏；双源差异

审校依据：中文明确“攻击命中后”，英文仅写 after attacking。译文沿用了英文的较宽触发条件。按中文设计语义补回命中要求；实际触发条件需统一双源。父任务只读核对 PROJECT 2.4.4 和双版本实现后确认：只有实际伤害段成功降低目标生命才触发，中文完整条件正确。

需要作者决定：无需作者决定

---

键：`gui.echo_warrior.summoner.skill.samurai.zanshin.description.3`  
使用位置：召唤器技能悬浮提示

中文原文：成功闪避整次攻击并获得1层踏込；持续与环境伤害无法闪避。

英文原文：A dodge negates the hit and grants 1 Fumikomi charge; ongoing damage cannot be dodged.

目标语言：`pt_br`

```text
Uma esquiva anula o golpe e concede 1 carga de Fumikomi; dano contínuo não pode ser evitado.
```

中文直译回译：闪避使这次攻击无效并获得1次踏込；持续伤害不能闪避。

自然中文释义：译文只排除了持续伤害，未告诉玩家环境伤害同样不可闪避。

术语对应：

- 持续／环境伤害 → dano contínuo／dano ambiental
- 踏込 → Fumikomi

审校结论：必须修改

建议译文：

```text
Uma esquiva anula o golpe e concede 1 carga de Fumikomi; não é possível esquivar de dano contínuo nem de dano ambiental.
```

风险：机制条件遗漏；双源差异

审校依据：中文明确“持续与环境伤害”，英文只剩 ongoing damage，译文随英文遗漏环境伤害。俄语另把“伤害”写成会主动闪避的主语，需一并改为不能躲避某类伤害。父任务只读核对设计和双版本实现后确认：持续状态及列举的环境伤害均不可闪避，按中文补全即可。

需要作者决定：无需作者决定

---

#### 批次3：召唤器、模式、属性、状态和操作反馈 — 必须修改

被动防御把主人的攻击目标写成受保护对象，必须修正；回收产物的修饰关系建议润色。其余属性和控制正确。

键：`gui.echo_warrior.summoner.alert.defensive.description`  
使用位置：召唤器界面／模式与属性提示

中文原文：仅反击伤害自身、主人或被主人攻击的生物。

英文原文：Only retaliates for itself, its owner, or the owner's attacks.

目标语言：`pt_br`

```text
Só revida ataques contra si, seu dono ou o alvo atacado pelo dono.
```

中文直译回译：只报复针对自己、主人，或主人所攻击目标的攻击。

自然中文释义：第三项把被主人攻击的敌人读成了受保护对象。

术语对应：

- 反击对象 → criaturas que ferem
- 主人攻击的生物 → criaturas que o dono ataca

审校结论：必须修改

建议译文：

```text
Ataca apenas criaturas que ferem o Eco ou seu dono, ou que o dono ataca.
```

风险：攻击对象反转

审校依据：contra si, seu dono ou o alvo 把三个对象都放进“受到攻击的一方”。双源的第三类应是英灵要打的目标，不能变成替该目标报复。

需要作者决定：无需作者决定

---

键：`item.echo_warrior.echo_recycler.summary`  
使用位置：回收箱物品悬浮提示

中文原文：放入英灵相关物品，在下一个午夜后收取回收原料

英文原文：Store Echo-related items and collect salvaged materials after the next midnight

目标语言：`pt_br`

```text
Guarde itens relacionados aos Ecos e recolha os materiais desmontados após a próxima meia-noite
```

中文直译回译：存入与回声相关的物品，下个午夜后领取拆开的材料。

自然中文释义：materiais desmontados 说成材料本身被拆开；应当是从拆解物品中回收的材料。

术语对应：

- 拆解／回收过程 → desmonte
- 回收所得材料 → materiais recuperados

审校结论：建议修改

建议译文：

```text
Guarde itens relacionados aos Ecos e recolha os materiais recuperados após a próxima meia-noite
```

风险：自然度；回收结果指代

审校依据：Desmonte 可保留为系统动词和装置名；此处结果用 materiais recuperados 即可，避免对原料作不自然的“被拆解”修饰。

需要作者决定：无需作者决定

---

#### 批次4：物品、遗物、饰品、传承与天赋 — 必须修改

占位符导致dois acessório，必须改句法；袭击类不应缩成Saqueadores。两类经验收益建议明确重复经验。其他数值与效果通过。

键：`trait.echo_warrior.raider_slayer.name`  
使用位置：遗物天赋悬浮提示

中文原文：灾厄杀手

英文原文：Raider Slayer

目标语言：`pt_br`

```text
Matador de Saqueadores
```

中文直译回译：掠夺者杀手。

自然中文释义：Saqueadores 同时是原版掠夺者实体名称，容易把天赋覆盖范围读成单一实体。

术语对应：

- 袭击者集体 → Invasores
- 掠夺者单一实体 → Saqueadores

审校结论：必须修改

建议译文：

```text
Matador de Invasores
```

风险：Minecraft 生物类别；范围误读

审校依据：父任务只读核对确认两版本使用 EntityTypeTags.RAIDERS，指整个袭击类生物标签，且不要求该生物当前正在参加袭击。必须与 Pillager 单一实体以及仅 Illager 家族区分。建议使用集体类别词，不写成仅对正在袭击的生物生效。中文源“灾厄生物”也需协调为明确的袭击类口径。

需要作者决定：无需作者决定

---

键：`trait.echo_warrior.raider_slayer.description`  
使用位置：遗物天赋悬浮提示

中文原文：对灾厄生物造成的伤害提高20%

英文原文：Deals 20% more damage to raiders

目标语言：`pt_br`

```text
Causa 20% a mais de dano a saqueadores
```

中文直译回译：对掠夺者造成的伤害提高20%。

自然中文释义：当前词语会让人认为仅对手持弩的掠夺者生效，不能清晰覆盖整个袭击类标签。

术语对应：

- 袭击类生物 → criaturas de invasão
- 伤害 → dano

审校结论：必须修改

建议译文：

```text
Causa 20% a mais de dano a criaturas de invasão
```

风险：Minecraft 生物类别；范围误读

审校依据：父任务只读核对确认两版本使用 EntityTypeTags.RAIDERS，指整个袭击类生物标签，且不要求该生物当前正在参加袭击。必须与 Pillager 单一实体以及仅 Illager 家族区分。建议使用集体类别词，不写成仅对正在袭击的生物生效。中文源“灾厄生物”也需协调为明确的袭击类口径。

需要作者决定：无需作者决定

---

键：`trait.echo_warrior.wise.description`  
使用位置：遗物天赋悬浮提示

中文原文：主人拾取经验球及本英灵成长获得的经验提高25%

英文原文：Increases experience from orbs collected by the owner and this Echo's growth by 25%

目标语言：`pt_br`

```text
Aumenta em 25% a experiência dos orbes coletados pelo dono e o crescimento deste Eco
```

中文直译回译：主人的经验球经验与这个回声的成长提高25%。

自然中文释义：把第二项写成了笼统的成长，而不是这个英灵获得的成长经验。

术语对应：

- 成长经验 → experiência de crescimento
- 经验球 → orbes

审校结论：建议修改

建议译文：

```text
Aumenta em 25% a experiência que o dono recebe de orbes e a experiência de crescimento deste Eco
```

风险：机制对象不够明确

审校依据：原文提升的是两份经验收益，不是直接提升等级、体型或整体属性；应重复“经验”以排除歧义。

需要作者决定：无需作者决定

---

键：`item.echo_warrior.accessory.detail.unique`  
使用位置：饰品名称／物品悬浮提示

中文原文：同名%s不可重复佩戴

英文原文：Duplicate %s cannot be equipped

目标语言：`pt_br`

```text
Não é possível equipar dois %s com o mesmo nome
```

中文直译回译：不能装备两个同名的%s。

自然中文释义：插入现有术语后，葡语成为“两个饰品〔单数〕”，西语成为“两个饰品〔单数〕”。

术语对应：

- 饰品 → acessório（现有单数）
- 超过一个 → mais de um

审校结论：必须修改

建议译文：

```text
Não é possível equipar mais de um %s com o mesmo nome
```

风险：占位符拼接语法

审校依据：term.accessory 分别为 acessório／accesorio，都是单数。不要为这一个模板把共享术语全局改成复数；把模板改成“超过一个”。

需要作者决定：无需作者决定

---

#### 批次5：教程手册、知识碎片与历史叙述 — 必须修改

40篇历史总体保留“可能”“某些”“传统归属”及考古证据；“狸奴”的追加动物释义必须处理。战车名称的一致性属于建议。

键：`knowledge.echo_warrior.entry.egypt_chariot_archery.body`  
使用位置：知识碎片阅读器（历史文章）

中文原文：新王国时期的轻型双轮战车通常由驭手与战士配合。战士用复合弓在车上远射，战车负责快速移动、追击，也承担展示权威的作用。神庙浮雕总把法老画得势不可挡；它们服务于王权表达，不能直接当作客观的战场快照。

英文原文：New Kingdom light chariots usually paired a driver with a warrior. The warrior shot a composite bow from the vehicle, while the chariot supplied speed, pursuit, and a display of authority. Temple reliefs make the pharaoh look unstoppable; they served royal representation and cannot be read as neutral snapshots of battle.

目标语言：`pt_br`

```text
Carruagens leves do Reino Novo geralmente levavam um condutor e um guerreiro. O guerreiro disparava um arco composto do veículo, enquanto a carruagem oferecia velocidade, perseguição e uma demonstração de autoridade. Relevos de templos fazem o faraó parecer imparável; serviam à representação real e não podem ser lidos como registros neutros da batalha.
```

中文直译回译：新王国的轻型马车通常载一名驭手与一名战士。战士从车上使用复合弓，马车提供速度、追击与权威展示。神庙浮雕把法老画得不可阻挡；这些画面服务王权，不能读成中立战场记录。

自然中文释义：正文的史实限定没有丢；只建议把泛称马车与同语言教程的古代战车名称统一。

术语对应：

- 战车 → biga
- 驭手 → condutor

审校结论：建议修改

建议译文：

```text
Bigas leves do Reino Novo geralmente levavam um condutor e um guerreiro. O guerreiro disparava um arco composto do veículo, enquanto a biga oferecia velocidade, perseguição e uma demonstração de autoridade. Relevos de templos fazem o faraó parecer imparável; serviam à representação real e não podem ser lidos como registros neutros da batalha.
```

风险：文化用词；术语一致性

审校依据：和技能203及教程564的战车用词一同统一。保留 geralmente 与不能把浮雕当客观记录的限制。

需要作者决定：无需作者决定

---

键：`knowledge.echo_warrior.entry.china_guandao_late.body`  
使用位置：知识碎片阅读器（历史文章）

中文原文：古人常把猫昵称为“狸奴”。传为宋人的《戏猫图》画了八只猫在庭园嬉戏，连正午缩成细线的瞳孔都没有漏掉。明代《猫蝶图》又借“猫蝶”与“耄耋”的谐音祝寿；画里的猫保留着生活观察，也承担吉祥寓意。

英文原文：Historical Chinese writers affectionately called cats linu, or “raccoon servants”. A painting traditionally attributed to a Song artist shows eight cats playing in a garden and even catches their pupils narrowed to slits at noon. A Ming Cat and Butterfly painting turns the shared sound of “cat and butterfly” and “advanced age” into a wish for longevity; the painted cats carry close observation and auspicious meaning together.

目标语言：`pt_br`

```text
Autores chineses antigos chamavam os gatos carinhosamente de linu, ou “servos-guaxinins”. Uma pintura tradicionalmente atribuída a um artista Song mostra oito gatos brincando em um jardim e registra até suas pupilas estreitas como fendas ao meio-dia. Uma pintura Ming de Gato e Borboleta transforma a semelhança sonora entre “gato e borboleta” e “idade avançada” em um desejo de longevidade; os gatos pintados unem observação atenta e significado auspicioso.
```

中文直译回译：古代中国作者亲昵地把猫叫作“狸奴”，也就是“浣熊仆人”。一幅传统上归于宋代画家的画描绘八只猫在庭园玩耍，连中午细缝般的瞳孔也画出。明代猫蝶画利用“猫蝶”与“高龄”的近音祝寿，把观察与吉祥寓意结合起来。

自然中文释义：后面的画作归属限定与谐音寓意都保留了；问题是把猫的昵称直接解释成了“浣熊仆人”。

术语对应：

- 狸奴 → linu
- 浣熊 → guaxinim

审校结论：必须修改

建议译文：

```text
Autores chineses antigos chamavam os gatos carinhosamente de linu. Uma pintura tradicionalmente atribuída a um artista Song mostra oito gatos brincando em um jardim e registra até suas pupilas estreitas como fendas ao meio-dia. Uma pintura Ming de Gato e Borboleta transforma a semelhança sonora entre “gato e borboleta” e “idade avançada” em um desejo de longevidade; os gatos pintados unem observação atenta e significado auspicioso.
```

风险：文化释义；双源差异

审校依据：这是英文 raccoon servants 带入的解释，中文只说明“狸奴”是猫的昵称。两源并未共同确认这个具体动物释义；不应让目标语言把追加的词源解释坐实。建议保留 linu 和“猫的爱称”，不增添动物义。本报告不作外部词源考证。

需要作者决定：问题 A1：是否将“狸奴”统一解释为古人对猫的爱称，保留音译并删除“浣熊仆人”的字面解释？建议采用这一做法，英文也需同步。

---

键：`knowledge.echo_warrior.entry.china_great_wall_network.body`  
使用位置：教程手册／知识碎片

中文原文：良渚文化的高等级墓葬中，有的墓主身旁放着多达约四十枚玉璧。它们有时成组靠近身体，并与其他玉礼器一起标示身份。单枚玉璧造型极简，数量叠加后，财富、等级和仪式分量都随之放大。

英文原文：Some high-status Liangzhu burials placed as many as roughly forty jade bi beside one person. The discs sometimes lay in groups near the body alongside other jade ritual objects. One bi looks austere; repeated across a burial, the same shape magnifies wealth, rank, and ceremonial weight.

目标语言：`pt_br`

```text
Alguns sepultamentos de alto status da cultura Liangzhu colocaram cerca de quarenta discos bi de jade junto a uma só pessoa. Às vezes os discos ficavam agrupados perto do corpo, ao lado de outros objetos rituais de jade. Um bi parece austero; repetida por todo o sepultamento, a mesma forma amplia a riqueza, a posição e o peso cerimonial.
```

中文直译回译：良渚文化的一些高等级墓葬把约四十枚玉璧放到一个人旁边。有时玉璧在遗体旁与其他玉礼器成组出现。单枚玉璧看起来朴素，同一形状在墓中重复，会扩大财富、等级和仪式分量。

自然中文释义：墓葬被写成会主动摆放玉璧的人，照搬了英文主语。

术语对应：

- 在某些墓葬中 → Em alguns sepultamentos
- 多达约 → até cerca de

审校结论：建议修改

建议译文：

```text
Em alguns sepultamentos de alto status da cultura Liangzhu, foram colocados até cerca de quarenta discos bi de jade junto a uma só pessoa. Às vezes os discos ficavam agrupados perto do corpo, ao lado de outros objetos rituais de jade. Um bi parece austero; repetida por todo o sepultamento, a mesma forma amplia a riqueza, a posição e o peso cerimonial.
```

风险：葡语自然度；主语选择

审校依据：用“在某些墓葬中曾放置”表达地点和考古发现。保留某些墓葬的范围，并用até cerca de对应中文多达约、英文as many as roughly。

需要作者决定：无需作者决定

---

#### 批次6：命令、死亡消息、标签、制作名单和其他短文本 — 必须修改

墨西加文物标签沿用了英文尚未协调的Asteca，须回到固定规则；死亡消息和作者署名通过。

键：`tag.item.echo_warrior.accessories.culture.aztec`  
使用位置：物品标签／分类显示

中文原文：墨西加饰品

英文原文：Aztec Accessories

目标语言：`pt_br`

```text
Acessórios Astecas
```

中文直译回译：阿兹特克饰品。

自然中文释义：文物文化分类沿用了通俗英雄名“阿兹特克”，没有用已锁定的历史分类“墨西加”。

术语对应：

- 墨西加 → Mexica／Mexicas
- 阿兹特克 → Asteca

审校结论：必须修改

建议译文：

```text
Acessórios Mexicas
```

风险：固定术语；双源差异

审校依据：中文写“墨西加饰品”；英文仍为 Aztec Accessories。术语表 D3 已明确文化分类和具体文物采用 Mexica；此处应服从已确认规则，英文源也应协调。

需要作者决定：无需作者决定

---

本语言总评：**仍有必须修改项**。先解决上述必须修改项，再进入接受基线所要求的视觉验收。

### Español de España（es_es）

#### 批次1：品牌、核心系统和固定术语 — 建议修改

Eco、Legado和技能／天赋区分清楚；Invocador de Ecos可在设备上下文保留，首次说明建议补dispositivo。desguace带拆解感，配合午夜返还材料的说明能够表达此系统，不必重启正式命名。

键：`gui.echo_warrior.tutorial.page.summoner.body.1`  
使用位置：教程手册

中文原文：扫描遗物数据，重建英灵战士的实体影像。

英文原文：Scan relic data and reconstruct an Echo Warrior projection.

目标语言：`es_es`

```text
Escanea los datos de una reliquia y reconstruye la proyección de un Guerrero del Eco.
```

中文直译回译：扫描遗物数据，重建一个回声战士的投影。

自然中文释义：句子像是在命令玩家扫描；正式物品名中的“召唤者”也可指人，首次说明可直接补出这是装置。

术语对应：

- 装置 → dispositivo
- 英灵投影 → proyección de un Guerrero del Eco

审校结论：建议修改

建议译文：

```text
Este dispositivo lee los datos de una reliquia y reconstruye la proyección de un Guerrero del Eco.
```

风险：人物／装置歧义

审校依据：Invocador de Ecos 在装有燃料、槽位的物品上下文可用，且项目已同意该名称；无需推翻正式名。在说明首次出现时用 aparelho／dispositivo 消歧即可。

需要作者决定：无需作者决定

---

#### 批次2：五名英灵、文明名与技能名及技能说明 — 必须修改

五名英雄、文明、关刀与日语技能借词一致；残心的命中条件和环境伤害排除须补回。

键：`gui.echo_warrior.summoner.skill.samurai.zanshin.description.2`  
使用位置：召唤器技能悬浮提示

中文原文：攻击命中后1秒内额外获得20%闪避，最高80%。

英文原文：Gain 20% dodge for 1 second after attacking, up to 80% total.

目标语言：`es_es`

```text
Obtiene un 20 % de esquiva durante 1 segundo tras atacar, hasta un 80 % en total.
```

中文直译回译：攻击后1秒内增加20%闪避，合计最高80%。

自然中文释义：现在写成只要做出攻击，就得到闪避加成；没有要求命中。

术语对应：

- 命中 → acertar
- 闪避 → esquiva

审校结论：必须修改

建议译文：

```text
Tras acertar un ataque, obtiene un 20 % de esquiva durante 1 segundo, hasta un 80 % en total.
```

风险：机制条件遗漏；双源差异

审校依据：中文明确“攻击命中后”，英文仅写 after attacking。译文沿用了英文的较宽触发条件。按中文设计语义补回命中要求；实际触发条件需统一双源。父任务只读核对 PROJECT 2.4.4 和双版本实现后确认：只有实际伤害段成功降低目标生命才触发，中文完整条件正确。

需要作者决定：无需作者决定

---

键：`gui.echo_warrior.summoner.skill.samurai.zanshin.description.3`  
使用位置：召唤器技能悬浮提示

中文原文：成功闪避整次攻击并获得1层踏込；持续与环境伤害无法闪避。

英文原文：A dodge negates the hit and grants 1 Fumikomi charge; ongoing damage cannot be dodged.

目标语言：`es_es`

```text
Una esquiva anula el golpe y concede 1 carga de Fumikomi; el daño continuo no se puede esquivar.
```

中文直译回译：闪避使这次攻击无效并获得1次踏込；持续伤害不能闪避。

自然中文释义：译文只排除了持续伤害，未告诉玩家环境伤害同样不可闪避。

术语对应：

- 持续／环境伤害 → daño continuo／daño ambiental
- 踏込 → Fumikomi

审校结论：必须修改

建议译文：

```text
Una esquiva anula el golpe y concede 1 carga de Fumikomi; no se puede esquivar el daño continuo ni el daño ambiental.
```

风险：机制条件遗漏；双源差异

审校依据：中文明确“持续与环境伤害”，英文只剩 ongoing damage，译文随英文遗漏环境伤害。俄语另把“伤害”写成会主动闪避的主语，需一并改为不能躲避某类伤害。父任务只读核对设计和双版本实现后确认：持续状态及列举的环境伤害均不可闪避，按中文补全即可。

需要作者决定：无需作者决定

---

#### 批次3：召唤器、模式、属性、状态和操作反馈 — 建议修改

三种警戒模式总体可理解，但被动防御句机械、对象关系含糊，建议明确列出攻击对象；属性与反馈数值正确。

键：`gui.echo_warrior.summoner.alert.defensive.description`  
使用位置：召唤器界面／模式与属性提示

中文原文：仅反击伤害自身、主人或被主人攻击的生物。

英文原文：Only retaliates for itself, its owner, or the owner's attacks.

目标语言：`es_es`

```text
Solo contraataca por sí mismo, por su dueño o ante los ataques de este.
```

中文直译回译：只为自己、为主人，或面对主人的攻击而反击。

自然中文释义：por sí mismo 也表示自行；ante los ataques de este 没有清楚指明它会攻击主人的攻击目标。

术语对应：

- 受到伤害 → dañan
- 主人的攻击对象 → a las que ataca el dueño

审校结论：建议修改

建议译文：

```text
Solo ataca a criaturas que dañan al Eco o a su dueño, o a las que ataca el dueño.
```

风险：机制对象表述含糊

审校依据：明确写出伤害英灵、伤害主人、被主人攻击三类生物，可以保留同一模式而消除机械直译。

需要作者决定：无需作者决定

---

#### 批次4：物品、遗物、饰品、传承与天赋 — 必须修改

dos accesorio的数变化必须修正；saqueadores需要改为袭击类集体称呼；经验收益建议更精确。

键：`trait.echo_warrior.raider_slayer.name`  
使用位置：遗物天赋悬浮提示

中文原文：灾厄杀手

英文原文：Raider Slayer

目标语言：`es_es`

```text
Cazador de saqueadores
```

中文直译回译：掠夺者杀手。

自然中文释义：Saqueadores 同时是原版掠夺者实体名称，容易把天赋覆盖范围读成单一实体。

术语对应：

- 袭击者集体 → asaltantes
- 掠夺者单一实体 → saqueadores

审校结论：必须修改

建议译文：

```text
Cazador de asaltantes
```

风险：Minecraft 生物类别；范围误读

审校依据：父任务只读核对确认两版本使用 EntityTypeTags.RAIDERS，指整个袭击类生物标签，且不要求该生物当前正在参加袭击。必须与 Pillager 单一实体以及仅 Illager 家族区分。建议使用集体类别词，不写成仅对正在袭击的生物生效。中文源“灾厄生物”也需协调为明确的袭击类口径。

需要作者决定：无需作者决定

---

键：`trait.echo_warrior.raider_slayer.description`  
使用位置：遗物天赋悬浮提示

中文原文：对灾厄生物造成的伤害提高20%

英文原文：Deals 20% more damage to raiders

目标语言：`es_es`

```text
Inflige un 20 % más de daño a los saqueadores
```

中文直译回译：对掠夺者造成的伤害提高20%。

自然中文释义：当前词语会让人认为仅对手持弩的掠夺者生效，不能清晰覆盖整个袭击类标签。

术语对应：

- 袭击类生物 → criaturas de asalto
- 伤害 → daño

审校结论：必须修改

建议译文：

```text
Inflige un 20 % más de daño a las criaturas de asalto
```

风险：Minecraft 生物类别；范围误读

审校依据：父任务只读核对确认两版本使用 EntityTypeTags.RAIDERS，指整个袭击类生物标签，且不要求该生物当前正在参加袭击。必须与 Pillager 单一实体以及仅 Illager 家族区分。建议使用集体类别词，不写成仅对正在袭击的生物生效。中文源“灾厄生物”也需协调为明确的袭击类口径。

需要作者决定：无需作者决定

---

键：`trait.echo_warrior.wise.description`  
使用位置：遗物天赋悬浮提示

中文原文：主人拾取经验球及本英灵成长获得的经验提高25%

英文原文：Increases experience from orbs collected by the owner and this Echo's growth by 25%

目标语言：`es_es`

```text
Aumenta un 25 % la experiencia de los orbes recogidos por el dueño y el crecimiento de este Eco
```

中文直译回译：主人的经验球经验与这个回声的成长提高25%。

自然中文释义：把第二项写成了笼统的成长，而不是这个英灵获得的成长经验。

术语对应：

- 成长经验 → experiencia de crecimiento
- 经验球 → orbes

审校结论：建议修改

建议译文：

```text
Aumenta un 25 % la experiencia que el dueño obtiene de los orbes y la experiencia de crecimiento de este Eco
```

风险：机制对象不够明确

审校依据：原文提升的是两份经验收益，不是直接提升等级、体型或整体属性；应重复“经验”以排除歧义。

需要作者决定：无需作者决定

---

键：`item.echo_warrior.accessory.detail.unique`  
使用位置：饰品名称／物品悬浮提示

中文原文：同名%s不可重复佩戴

英文原文：Duplicate %s cannot be equipped

目标语言：`es_es`

```text
No puedes equipar dos %s con el mismo nombre
```

中文直译回译：不能装备两个同名的%s。

自然中文释义：插入现有术语后，葡语成为“两个饰品〔单数〕”，西语成为“两个饰品〔单数〕”。

术语对应：

- 饰品 → accesorio（现有单数）
- 超过一个 → más de un

审校结论：必须修改

建议译文：

```text
No puedes equipar más de un %s con el mismo nombre
```

风险：占位符拼接语法

审校依据：term.accessory 分别为 acessório／accesorio，都是单数。不要为这一个模板把共享术语全局改成复数；把模板改成“超过一个”。

需要作者决定：无需作者决定

---

#### 批次5：教程手册、知识碎片与历史叙述 — 必须修改

“向后齐射”改变了后撤动作；历史鱼饰把儿童缩成女童，“狸奴”追加释义也待处理。两篇器物文章有可直接修正的自然度问题。

键：`knowledge.echo_warrior.entry.egypt_senet_rules_lost.body`  
使用位置：知识碎片阅读器（历史文章）

中文原文：中王国时期的鱼形护符可用黄金、绿石或水晶制成，有些系在儿童或年轻女性的发辫末端。罗非鱼会把卵含在口中直到孵化，因此也获得繁衍与再生的含义。尼罗河里的常见鱼由此成了贴身佩戴的保护符号。

英文原文：Middle Kingdom fish ornaments were made from gold, green stone, or crystal, and some hung from the ends of braids worn by children or young women. Tilapia brood their eggs in the mouth until they hatch, helping the fish acquire associations with fertility and rebirth. A familiar Nile fish became a protective emblem worn close to the body.

目标语言：`es_es`

```text
Los adornos con forma de pez del Reino Medio se fabricaban con oro, piedra verde o cristal, y algunos colgaban de los extremos de las trenzas de niñas o mujeres jóvenes. Las tilapias incuban los huevos en la boca hasta que eclosionan, lo que ayudó a asociar el pez con la fertilidad y el renacimiento. Un pez habitual del Nilo se convirtió en emblema protector llevado cerca del cuerpo.
```

中文直译回译：中王国鱼形饰物由黄金、绿石或水晶制作，其中一些挂在女童或年轻女性的发辫末端。罗非鱼在嘴里孵卵，使它与繁衍、再生关联。尼罗河常见鱼成了贴身佩戴的保护标志。

自然中文释义：把源文不限定性别的儿童收窄成女童。

术语对应：

- 儿童 → niños（泛指）
- 女童 → niñas
- 年轻女性 → mujeres jóvenes

审校结论：必须修改

建议译文：

```text
Los adornos con forma de pez del Reino Medio se fabricaban con oro, piedra verde o cristal, y algunos colgaban de los extremos de las trenzas de niños o mujeres jóvenes. Las tilapias incuban los huevos en la boca hasta que eclosionan, lo que ayudó a asociar el pez con la fertilidad y el renacimiento. Un pez habitual del Nilo se convirtió en emblema protector llevado cerca del cuerpo.
```

风险：历史对象范围缩小

审校依据：中文“儿童”、英文 children 都未限定女孩。西语 niñas 增添史实限定；niños 在这里用作泛指儿童。

需要作者决定：无需作者决定

---

键：`knowledge.echo_warrior.entry.china_great_wall_network.body`  
使用位置：知识碎片阅读器（历史文章）

中文原文：良渚文化的高等级墓葬中，有的墓主身旁放着多达约四十枚玉璧。它们有时成组靠近身体，并与其他玉礼器一起标示身份。单枚玉璧造型极简，数量叠加后，财富、等级和仪式分量都随之放大。

英文原文：Some high-status Liangzhu burials placed as many as roughly forty jade bi beside one person. The discs sometimes lay in groups near the body alongside other jade ritual objects. One bi looks austere; repeated across a burial, the same shape magnifies wealth, rank, and ceremonial weight.

目标语言：`es_es`

```text
Algunos enterramientos de alto rango de la cultura Liangzhu colocaron junto a una sola persona hasta unos cuarenta discos bi de jade. Los discos aparecían a veces agrupados cerca del cuerpo junto a otros objetos rituales de jade. Un solo bi parece austero; repetida por todo un enterramiento, la misma forma multiplica la riqueza, el rango y el peso ceremonial.
```

中文直译回译：良渚文化的一些高等级墓葬把多达约四十枚玉璧放到一个人身旁。玉璧有时与其他玉礼器在遗体旁成组放置。一个玉璧朴素，同一形式在墓中重复，会增加财富、等级和仪式的分量。

自然中文释义：墓葬被写成亲手放置玉璧的行动者，句法照搬英文。

术语对应：

- 在某些墓葬中 → En algunos enterramientos
- 曾放置 → se colocaron

审校结论：建议修改

建议译文：

```text
En algunos enterramientos de alto rango de la cultura Liangzhu se colocaron junto a una sola persona hasta unos cuarenta discos bi de jade. Los discos aparecían a veces agrupados cerca del cuerpo junto a otros objetos rituales de jade. Un solo bi parece austero; repetida por todo un enterramiento, la misma forma multiplica la riqueza, el rango y el peso ceremonial.
```

风险：西语自然度；主语选择

审校依据：用 En ... se colocaron 表达“某些墓葬中曾放置”，保留“某些”“多达约四十”与地点关系，不把所有墓葬概括成相同数量。

需要作者决定：无需作者决定

---

键：`knowledge.echo_warrior.entry.china_ji_combined_weapon.body`  
使用位置：知识碎片阅读器（历史文章）

中文原文：许多汉代玉蝉属于放在亡者口中的“琀”，并不用来悬挂。故宫藏品显示，这类琀蝉往往没有穿绳孔，造型扁平简洁。判断玉蝉是佩饰、冠饰还是葬玉，要同时看孔道与出土位置，单凭轮廓不够。

英文原文：Many Han jade cicadas were han, mouth jades placed with the dead, rather than objects made for suspension. Palace Museum examples show that such cicadas often lack a hole and favour a flat, concise form. Distinguishing pendant, headdress fitting, and burial jade requires the perforations and archaeological context; silhouette alone is not enough.

目标语言：`es_es`

```text
Muchas cigarras de jade Han eran han, jades bucales colocados con los muertos, y no objetos diseñados para colgar. Los ejemplares del Museo del Palacio muestran que estas cigarras suelen carecer de orificio y prefieren una forma plana y concisa. Para distinguir un colgante, una pieza de tocado y un jade funerario hacen falta las perforaciones y el contexto arqueológico; la silueta por sí sola no basta.
```

中文直译回译：许多汉代玉蝉是琀，即放在死者身边的口用玉，而不是悬挂物。故宫的例子表明这些玉蝉常没有孔，并偏爱扁平简洁的形状。分辨吊坠、冠饰和葬玉需要看孔洞和考古语境，只看轮廓不够。

自然中文释义：prefieren 把玉蝉写成会偏好自身形状的主体；“置于口中”也可比“死者身边的口用玉”说得更直接。

术语对应：

- 呈现某种形状 → presentan una forma
- 口内放置 → en la boca

审校结论：建议修改

建议译文：

```text
Muchas cigarras de jade Han eran han, piezas de jade colocadas en la boca de los difuntos, y no objetos diseñados para colgar. Los ejemplares del Museo del Palacio muestran que estas cigarras suelen carecer de orificio y presentan una forma plana y sencilla. Para distinguir un colgante, una pieza de tocado y un jade funerario hacen falta las perforaciones y el contexto arqueológico; la silueta por sí sola no basta.
```

风险：自然度；文化器物说明

审校依据：这是英文 favour a form 的生硬转译；应写呈现某种形态。开头按中文补足在口内放置的位置，保留“许多”“往往”与鉴别证据边界。

需要作者决定：无需作者决定

---

键：`knowledge.echo_warrior.entry.china_guandao_late.body`  
使用位置：知识碎片阅读器（历史文章）

中文原文：古人常把猫昵称为“狸奴”。传为宋人的《戏猫图》画了八只猫在庭园嬉戏，连正午缩成细线的瞳孔都没有漏掉。明代《猫蝶图》又借“猫蝶”与“耄耋”的谐音祝寿；画里的猫保留着生活观察，也承担吉祥寓意。

英文原文：Historical Chinese writers affectionately called cats linu, or “raccoon servants”. A painting traditionally attributed to a Song artist shows eight cats playing in a garden and even catches their pupils narrowed to slits at noon. A Ming Cat and Butterfly painting turns the shared sound of “cat and butterfly” and “advanced age” into a wish for longevity; the painted cats carry close observation and auspicious meaning together.

目标语言：`es_es`

```text
Los autores chinos antiguos llamaban cariñosamente a los gatos linu, o «sirvientes mapache». Una pintura atribuida tradicionalmente a un artista Song muestra ocho gatos jugando en un jardín y capta incluso sus pupilas estrechas como rendijas al mediodía. Una pintura Ming de Gato y mariposa convierte la similitud sonora entre «gato y mariposa» y «edad avanzada» en un deseo de longevidad; los gatos pintados reúnen una observación atenta y un significado auspicioso.
```

中文直译回译：古代中国作者亲昵地把猫叫作“狸奴”，也就是“浣熊仆人”。一幅传统上归于宋代画家的画描绘八只猫在庭园玩耍，连中午细缝般的瞳孔也画出。明代猫蝶画利用“猫蝶”与“高龄”的近音祝寿，把观察与吉祥寓意结合起来。

自然中文释义：后面的画作归属限定与谐音寓意都保留了；问题是把猫的昵称直接解释成了“浣熊仆人”。

术语对应：

- 狸奴 → linu
- 浣熊 → mapache

审校结论：必须修改

建议译文：

```text
Los autores chinos antiguos llamaban cariñosamente a los gatos linu. Una pintura atribuida tradicionalmente a un artista Song muestra ocho gatos jugando en un jardín y capta incluso sus pupilas estrechas como rendijas al mediodía. Una pintura Ming de Gato y mariposa convierte la similitud sonora entre «gato y mariposa» y «edad avanzada» en un deseo de longevidad; los gatos pintados reúnen una observación atenta y un significado auspicioso.
```

风险：文化释义；双源差异

审校依据：这是英文 raccoon servants 带入的解释，中文只说明“狸奴”是猫的昵称。两源并未共同确认这个具体动物释义；不应让目标语言把追加的词源解释坐实。建议保留 linu 和“猫的爱称”，不增添动物义。本报告不作外部词源考证。

需要作者决定：问题 A1：是否将“狸奴”统一解释为古人对猫的爱称，保留音译并删除“浣熊仆人”的字面解释？建议采用这一做法，英文也需同步。

---

键：`gui.echo_warrior.tutorial.page.egyptian_archer.body.4`  
使用位置：教程手册

中文原文：陷入危险时，还可以使用后撤步齐射紧急逃脱！

英文原文：When threatened, a backstep volley creates an emergency escape.

目标语言：`es_es`

```text
Cuando se ve amenazado, una salva hacia atrás le abre una vía de escape.
```

中文直译回译：受到威胁时，向后的一轮齐射打开逃生道路。

自然中文释义：后撤动作被变成了射击方向朝后，玩家无法读出英灵先向后跳开。

术语对应：

- 后撤跳跃 → retrocede de un salto
- 齐射 → salva

审校结论：必须修改

建议译文：

```text
Cuando se ve amenazado, retrocede de un salto y lanza una salva para escapar del peligro.
```

风险：动作／方向错译

审校依据：backstep volley 是后撤步配合齐射。当前 salva hacia atrás 修饰的是射击方向，和同语技能说明 Salta hacia atrás ... 不一致。

需要作者决定：无需作者决定

---

#### 批次6：命令、死亡消息、标签、制作名单和其他短文本 — 必须修改

墨西加文物分类词与已确认规则不符，必须协调；其他死亡消息、命令、机构和署名可接受。

键：`tag.item.echo_warrior.accessories.culture.aztec`  
使用位置：物品标签／分类显示

中文原文：墨西加饰品

英文原文：Aztec Accessories

目标语言：`es_es`

```text
Accesorios aztecas
```

中文直译回译：阿兹特克饰品。

自然中文释义：文物文化分类沿用了通俗英雄名“阿兹特克”，没有用已锁定的历史分类“墨西加”。

术语对应：

- 墨西加 → mexica／mexicas
- 阿兹特克 → azteca

审校结论：必须修改

建议译文：

```text
Accesorios mexicas
```

风险：固定术语；双源差异

审校依据：中文写“墨西加饰品”；英文仍为 Aztec Accessories。术语表 D3 已明确文化分类和具体文物采用 Mexica；此处应服从已确认规则，英文源也应协调。

需要作者决定：无需作者决定

---

本语言总评：**仍有必须修改项**。先解决上述必须修改项，再进入接受基线所要求的视觉验收。

### Español de Latinoamérica（es_mx）

#### 批次1：品牌、核心系统和固定术语 — 建议修改

保持中性拉美用语，Toma／ítem／Brocha可接受。Invocador问题可由正文dispositivo解决。desmantelamiento含义清楚但标题较长，应做实际宽度验收；无需仅为变短擅改正式名。

键：`gui.echo_warrior.tutorial.page.summoner.body.1`  
使用位置：教程手册

中文原文：扫描遗物数据，重建英灵战士的实体影像。

英文原文：Scan relic data and reconstruct an Echo Warrior projection.

目标语言：`es_mx`

```text
Escanea los datos de una reliquia y reconstruye la proyección de un Guerrero del Eco.
```

中文直译回译：扫描遗物数据，重建一个回声战士的投影。

自然中文释义：句子像是在命令玩家扫描；正式物品名中的“召唤者”也可指人，首次说明可直接补出这是装置。

术语对应：

- 装置 → dispositivo
- 英灵投影 → proyección de un Guerrero del Eco

审校结论：建议修改

建议译文：

```text
Este dispositivo lee los datos de una reliquia y reconstruye la proyección de un Guerrero del Eco.
```

风险：人物／装置歧义

审校依据：Invocador de Ecos 在装有燃料、槽位的物品上下文可用，且项目已同意该名称；无需推翻正式名。在说明首次出现时用 aparelho／dispositivo 消歧即可。

需要作者决定：无需作者决定

---

#### 批次2：五名英灵、文明名与技能名及技能说明 — 必须修改

与西班牙版相同的名称和技能语义已经核读；残心存在同样两项条件遗漏。

键：`gui.echo_warrior.summoner.skill.samurai.zanshin.description.2`  
使用位置：召唤器技能悬浮提示

中文原文：攻击命中后1秒内额外获得20%闪避，最高80%。

英文原文：Gain 20% dodge for 1 second after attacking, up to 80% total.

目标语言：`es_mx`

```text
Obtiene un 20 % de esquiva durante 1 segundo tras atacar, hasta un 80 % en total.
```

中文直译回译：攻击后1秒内增加20%闪避，合计最高80%。

自然中文释义：现在写成只要做出攻击，就得到闪避加成；没有要求命中。

术语对应：

- 命中 → acertar
- 闪避 → esquiva

审校结论：必须修改

建议译文：

```text
Tras acertar un ataque, obtiene un 20 % de esquiva durante 1 segundo, hasta un 80 % en total.
```

风险：机制条件遗漏；双源差异

审校依据：中文明确“攻击命中后”，英文仅写 after attacking。译文沿用了英文的较宽触发条件。按中文设计语义补回命中要求；实际触发条件需统一双源。父任务只读核对 PROJECT 2.4.4 和双版本实现后确认：只有实际伤害段成功降低目标生命才触发，中文完整条件正确。

需要作者决定：无需作者决定

---

键：`gui.echo_warrior.summoner.skill.samurai.zanshin.description.3`  
使用位置：召唤器技能悬浮提示

中文原文：成功闪避整次攻击并获得1层踏込；持续与环境伤害无法闪避。

英文原文：A dodge negates the hit and grants 1 Fumikomi charge; ongoing damage cannot be dodged.

目标语言：`es_mx`

```text
Una esquiva anula el golpe y concede 1 carga de Fumikomi; el daño continuo no se puede esquivar.
```

中文直译回译：闪避使这次攻击无效并获得1次踏込；持续伤害不能闪避。

自然中文释义：译文只排除了持续伤害，未告诉玩家环境伤害同样不可闪避。

术语对应：

- 持续／环境伤害 → daño continuo／daño ambiental
- 踏込 → Fumikomi

审校结论：必须修改

建议译文：

```text
Una esquiva anula el golpe y concede 1 carga de Fumikomi; no se puede esquivar el daño continuo ni el daño ambiental.
```

风险：机制条件遗漏；双源差异

审校依据：中文明确“持续与环境伤害”，英文只剩 ongoing damage，译文随英文遗漏环境伤害。俄语另把“伤害”写成会主动闪避的主语，需一并改为不能躲避某类伤害。父任务只读核对设计和双版本实现后确认：持续状态及列举的环境伤害均不可闪避，按中文补全即可。

需要作者决定：无需作者决定

---

#### 批次3：召唤器、模式、属性、状态和操作反馈 — 建议修改

模式、数值、反馈与西班牙版语义一致；被动防御建议明确三个目标关系。

键：`gui.echo_warrior.summoner.alert.defensive.description`  
使用位置：召唤器界面／模式与属性提示

中文原文：仅反击伤害自身、主人或被主人攻击的生物。

英文原文：Only retaliates for itself, its owner, or the owner's attacks.

目标语言：`es_mx`

```text
Solo contraataca por sí mismo, por su dueño o ante los ataques de este.
```

中文直译回译：只为自己、为主人，或面对主人的攻击而反击。

自然中文释义：por sí mismo 也表示自行；ante los ataques de este 没有清楚指明它会攻击主人的攻击目标。

术语对应：

- 受到伤害 → dañan
- 主人的攻击对象 → a las que ataca el dueño

审校结论：建议修改

建议译文：

```text
Solo ataca a criaturas que dañan al Eco o a su dueño, o a las que ataca el dueño.
```

风险：机制对象表述含糊

审校依据：明确写出伤害英灵、伤害主人、被主人攻击三类生物，可以保留同一模式而消除机械直译。

需要作者决定：无需作者决定

---

#### 批次4：物品、遗物、饰品、传承与天赋 — 必须修改

除共同的复数和袭击类问题外，地区替换pincel→brocha后遗留un brocha，是本语言独有的拼接错误。

键：`trait.echo_warrior.raider_slayer.name`  
使用位置：遗物天赋悬浮提示

中文原文：灾厄杀手

英文原文：Raider Slayer

目标语言：`es_mx`

```text
Cazador de saqueadores
```

中文直译回译：掠夺者杀手。

自然中文释义：Saqueadores 同时是原版掠夺者实体名称，容易把天赋覆盖范围读成单一实体。

术语对应：

- 袭击者集体 → asaltantes
- 掠夺者单一实体 → saqueadores

审校结论：必须修改

建议译文：

```text
Cazador de asaltantes
```

风险：Minecraft 生物类别；范围误读

审校依据：父任务只读核对确认两版本使用 EntityTypeTags.RAIDERS，指整个袭击类生物标签，且不要求该生物当前正在参加袭击。必须与 Pillager 单一实体以及仅 Illager 家族区分。建议使用集体类别词，不写成仅对正在袭击的生物生效。中文源“灾厄生物”也需协调为明确的袭击类口径。

需要作者决定：无需作者决定

---

键：`trait.echo_warrior.raider_slayer.description`  
使用位置：遗物天赋悬浮提示

中文原文：对灾厄生物造成的伤害提高20%

英文原文：Deals 20% more damage to raiders

目标语言：`es_mx`

```text
Inflige un 20 % más de daño a los saqueadores
```

中文直译回译：对掠夺者造成的伤害提高20%。

自然中文释义：当前词语会让人认为仅对手持弩的掠夺者生效，不能清晰覆盖整个袭击类标签。

术语对应：

- 袭击类生物 → criaturas de asalto
- 伤害 → daño

审校结论：必须修改

建议译文：

```text
Inflige un 20 % más de daño a las criaturas de asalto
```

风险：Minecraft 生物类别；范围误读

审校依据：父任务只读核对确认两版本使用 EntityTypeTags.RAIDERS，指整个袭击类生物标签，且不要求该生物当前正在参加袭击。必须与 Pillager 单一实体以及仅 Illager 家族区分。建议使用集体类别词，不写成仅对正在袭击的生物生效。中文源“灾厄生物”也需协调为明确的袭击类口径。

需要作者决定：无需作者决定

---

键：`trait.echo_warrior.wise.description`  
使用位置：遗物天赋悬浮提示

中文原文：主人拾取经验球及本英灵成长获得的经验提高25%

英文原文：Increases experience from orbs collected by the owner and this Echo's growth by 25%

目标语言：`es_mx`

```text
Aumenta un 25 % la experiencia de los orbes recogidos por el dueño y el crecimiento de este Eco
```

中文直译回译：主人的经验球经验与这个回声的成长提高25%。

自然中文释义：把第二项写成了笼统的成长，而不是这个英灵获得的成长经验。

术语对应：

- 成长经验 → experiencia de crecimiento
- 经验球 → orbes

审校结论：建议修改

建议译文：

```text
Aumenta un 25 % la experiencia que el dueño obtiene de los orbes y la experiencia de crecimiento de este Eco
```

风险：机制对象不够明确

审校依据：原文提升的是两份经验收益，不是直接提升等级、体型或整体属性；应重复“经验”以排除歧义。

需要作者决定：无需作者决定

---

键：`item.echo_warrior.echo_compass.tooltip.brush_prefix`  
使用位置：回声罗盘物品悬浮提示（前缀＋名词＋后缀）

中文原文：记得带上考古用的

英文原文：Remember to bring an archaeology 

目标语言：`es_mx`

```text
Recuerda llevar un 
```

中文直译回译：记得带上一个〔阳性〕……

自然中文释义：下一段插入阴性名词 brocha 后成为 un brocha，冠词性别错误。

术语对应：

- 刷子 → brocha（阴性）
- 一个 → una

审校结论：必须修改

建议译文：

```text
Recuerda llevar una 
```

风险：拼接语法；地区词替换

审校依据：es_mx 把 pincel 改成 brocha，却未同步前缀 un→una。建议保留结尾空格，与 brush 和 brush_suffix 连成完整句；其他地区别名以后随基准同步。

需要作者决定：无需作者决定

---

键：`item.echo_warrior.accessory.detail.unique`  
使用位置：饰品名称／物品悬浮提示

中文原文：同名%s不可重复佩戴

英文原文：Duplicate %s cannot be equipped

目标语言：`es_mx`

```text
No puedes equipar dos %s con el mismo nombre
```

中文直译回译：不能装备两个同名的%s。

自然中文释义：插入现有术语后，葡语成为“两个饰品〔单数〕”，西语成为“两个饰品〔单数〕”。

术语对应：

- 饰品 → accesorio（现有单数）
- 超过一个 → más de un

审校结论：必须修改

建议译文：

```text
No puedes equipar más de un %s con el mismo nombre
```

风险：占位符拼接语法

审校依据：term.accessory 分别为 acessório／accesorio，都是单数。不要为这一个模板把共享术语全局改成复数；把模板改成“超过一个”。

需要作者决定：无需作者决定

---

#### 批次5：教程手册、知识碎片与历史叙述 — 必须修改

与西班牙版共有的历史和后撤步问题均适用；其余26个地区差异涉及器物、拆解和用词，未新增机制变化。

键：`knowledge.echo_warrior.entry.egypt_senet_rules_lost.body`  
使用位置：知识碎片阅读器（历史文章）

中文原文：中王国时期的鱼形护符可用黄金、绿石或水晶制成，有些系在儿童或年轻女性的发辫末端。罗非鱼会把卵含在口中直到孵化，因此也获得繁衍与再生的含义。尼罗河里的常见鱼由此成了贴身佩戴的保护符号。

英文原文：Middle Kingdom fish ornaments were made from gold, green stone, or crystal, and some hung from the ends of braids worn by children or young women. Tilapia brood their eggs in the mouth until they hatch, helping the fish acquire associations with fertility and rebirth. A familiar Nile fish became a protective emblem worn close to the body.

目标语言：`es_mx`

```text
Los adornos con forma de pez del Reino Medio se fabricaban con oro, piedra verde o cristal, y algunos colgaban de los extremos de las trenzas de niñas o mujeres jóvenes. Las tilapias incuban los huevos en la boca hasta que eclosionan, lo que ayudó a asociar el pez con la fertilidad y el renacimiento. Un pez habitual del Nilo se convirtió en emblema protector llevado cerca del cuerpo.
```

中文直译回译：中王国鱼形饰物由黄金、绿石或水晶制作，其中一些挂在女童或年轻女性的发辫末端。罗非鱼在嘴里孵卵，使它与繁衍、再生关联。尼罗河常见鱼成了贴身佩戴的保护标志。

自然中文释义：把源文不限定性别的儿童收窄成女童。

术语对应：

- 儿童 → niños（泛指）
- 女童 → niñas
- 年轻女性 → mujeres jóvenes

审校结论：必须修改

建议译文：

```text
Los adornos con forma de pez del Reino Medio se fabricaban con oro, piedra verde o cristal, y algunos colgaban de los extremos de las trenzas de niños o mujeres jóvenes. Las tilapias incuban los huevos en la boca hasta que eclosionan, lo que ayudó a asociar el pez con la fertilidad y el renacimiento. Un pez habitual del Nilo se convirtió en emblema protector llevado cerca del cuerpo.
```

风险：历史对象范围缩小

审校依据：中文“儿童”、英文 children 都未限定女孩。西语 niñas 增添史实限定；niños 在这里用作泛指儿童。

需要作者决定：无需作者决定

---

键：`knowledge.echo_warrior.entry.china_great_wall_network.body`  
使用位置：知识碎片阅读器（历史文章）

中文原文：良渚文化的高等级墓葬中，有的墓主身旁放着多达约四十枚玉璧。它们有时成组靠近身体，并与其他玉礼器一起标示身份。单枚玉璧造型极简，数量叠加后，财富、等级和仪式分量都随之放大。

英文原文：Some high-status Liangzhu burials placed as many as roughly forty jade bi beside one person. The discs sometimes lay in groups near the body alongside other jade ritual objects. One bi looks austere; repeated across a burial, the same shape magnifies wealth, rank, and ceremonial weight.

目标语言：`es_mx`

```text
Algunos enterramientos de alto rango de la cultura Liangzhu colocaron junto a una sola persona hasta unos cuarenta discos bi de jade. Los discos aparecían a veces agrupados cerca del cuerpo junto a otros objetos rituales de jade. Un solo bi parece austero; repetida por todo un enterramiento, la misma forma multiplica la riqueza, el rango y el peso ceremonial.
```

中文直译回译：良渚文化的一些高等级墓葬把多达约四十枚玉璧放到一个人身旁。玉璧有时与其他玉礼器在遗体旁成组放置。一个玉璧朴素，同一形式在墓中重复，会增加财富、等级和仪式的分量。

自然中文释义：墓葬被写成亲手放置玉璧的行动者，句法照搬英文。

术语对应：

- 在某些墓葬中 → En algunos enterramientos
- 曾放置 → se colocaron

审校结论：建议修改

建议译文：

```text
En algunos enterramientos de alto rango de la cultura Liangzhu se colocaron junto a una sola persona hasta unos cuarenta discos bi de jade. Los discos aparecían a veces agrupados cerca del cuerpo junto a otros objetos rituales de jade. Un solo bi parece austero; repetida por todo un enterramiento, la misma forma multiplica la riqueza, el rango y el peso ceremonial.
```

风险：西语自然度；主语选择

审校依据：用 En ... se colocaron 表达“某些墓葬中曾放置”，保留“某些”“多达约四十”与地点关系，不把所有墓葬概括成相同数量。

需要作者决定：无需作者决定

---

键：`knowledge.echo_warrior.entry.china_ji_combined_weapon.body`  
使用位置：知识碎片阅读器（历史文章）

中文原文：许多汉代玉蝉属于放在亡者口中的“琀”，并不用来悬挂。故宫藏品显示，这类琀蝉往往没有穿绳孔，造型扁平简洁。判断玉蝉是佩饰、冠饰还是葬玉，要同时看孔道与出土位置，单凭轮廓不够。

英文原文：Many Han jade cicadas were han, mouth jades placed with the dead, rather than objects made for suspension. Palace Museum examples show that such cicadas often lack a hole and favour a flat, concise form. Distinguishing pendant, headdress fitting, and burial jade requires the perforations and archaeological context; silhouette alone is not enough.

目标语言：`es_mx`

```text
Muchas cigarras de jade Han eran han, jades bucales colocados con los muertos, y no objetos diseñados para colgar. Los ejemplares del Museo del Palacio muestran que estas cigarras suelen carecer de orificio y prefieren una forma plana y concisa. Para distinguir un colgante, una pieza de tocado y un jade funerario hacen falta las perforaciones y el contexto arqueológico; la silueta por sí sola no basta.
```

中文直译回译：许多汉代玉蝉是琀，即放在死者身边的口用玉，而不是悬挂物。故宫的例子表明这些玉蝉常没有孔，并偏爱扁平简洁的形状。分辨吊坠、冠饰和葬玉需要看孔洞和考古语境，只看轮廓不够。

自然中文释义：prefieren 把玉蝉写成会偏好自身形状的主体；“置于口中”也可比“死者身边的口用玉”说得更直接。

术语对应：

- 呈现某种形状 → presentan una forma
- 口内放置 → en la boca

审校结论：建议修改

建议译文：

```text
Muchas cigarras de jade Han eran han, piezas de jade colocadas en la boca de los difuntos, y no objetos diseñados para colgar. Los ejemplares del Museo del Palacio muestran que estas cigarras suelen carecer de orificio y presentan una forma plana y sencilla. Para distinguir un colgante, una pieza de tocado y un jade funerario hacen falta las perforaciones y el contexto arqueológico; la silueta por sí sola no basta.
```

风险：自然度；文化器物说明

审校依据：这是英文 favour a form 的生硬转译；应写呈现某种形态。开头按中文补足在口内放置的位置，保留“许多”“往往”与鉴别证据边界。

需要作者决定：无需作者决定

---

键：`knowledge.echo_warrior.entry.china_guandao_late.body`  
使用位置：知识碎片阅读器（历史文章）

中文原文：古人常把猫昵称为“狸奴”。传为宋人的《戏猫图》画了八只猫在庭园嬉戏，连正午缩成细线的瞳孔都没有漏掉。明代《猫蝶图》又借“猫蝶”与“耄耋”的谐音祝寿；画里的猫保留着生活观察，也承担吉祥寓意。

英文原文：Historical Chinese writers affectionately called cats linu, or “raccoon servants”. A painting traditionally attributed to a Song artist shows eight cats playing in a garden and even catches their pupils narrowed to slits at noon. A Ming Cat and Butterfly painting turns the shared sound of “cat and butterfly” and “advanced age” into a wish for longevity; the painted cats carry close observation and auspicious meaning together.

目标语言：`es_mx`

```text
Los autores chinos antiguos llamaban cariñosamente a los gatos linu, o «sirvientes mapache». Una pintura atribuida tradicionalmente a un artista Song muestra ocho gatos jugando en un jardín y capta incluso sus pupilas estrechas como rendijas al mediodía. Una pintura Ming de Gato y mariposa convierte la similitud sonora entre «gato y mariposa» y «edad avanzada» en un deseo de longevidad; los gatos pintados reúnen una observación atenta y un significado auspicioso.
```

中文直译回译：古代中国作者亲昵地把猫叫作“狸奴”，也就是“浣熊仆人”。一幅传统上归于宋代画家的画描绘八只猫在庭园玩耍，连中午细缝般的瞳孔也画出。明代猫蝶画利用“猫蝶”与“高龄”的近音祝寿，把观察与吉祥寓意结合起来。

自然中文释义：后面的画作归属限定与谐音寓意都保留了；问题是把猫的昵称直接解释成了“浣熊仆人”。

术语对应：

- 狸奴 → linu
- 浣熊 → mapache

审校结论：必须修改

建议译文：

```text
Los autores chinos antiguos llamaban cariñosamente a los gatos linu. Una pintura atribuida tradicionalmente a un artista Song muestra ocho gatos jugando en un jardín y capta incluso sus pupilas estrechas como rendijas al mediodía. Una pintura Ming de Gato y mariposa convierte la similitud sonora entre «gato y mariposa» y «edad avanzada» en un deseo de longevidad; los gatos pintados reúnen una observación atenta y un significado auspicioso.
```

风险：文化释义；双源差异

审校依据：这是英文 raccoon servants 带入的解释，中文只说明“狸奴”是猫的昵称。两源并未共同确认这个具体动物释义；不应让目标语言把追加的词源解释坐实。建议保留 linu 和“猫的爱称”，不增添动物义。本报告不作外部词源考证。

需要作者决定：问题 A1：是否将“狸奴”统一解释为古人对猫的爱称，保留音译并删除“浣熊仆人”的字面解释？建议采用这一做法，英文也需同步。

---

键：`gui.echo_warrior.tutorial.page.egyptian_archer.body.4`  
使用位置：教程手册

中文原文：陷入危险时，还可以使用后撤步齐射紧急逃脱！

英文原文：When threatened, a backstep volley creates an emergency escape.

目标语言：`es_mx`

```text
Cuando se ve amenazado, una salva hacia atrás le abre una vía de escape.
```

中文直译回译：受到威胁时，向后的一轮齐射打开逃生道路。

自然中文释义：后撤动作被变成了射击方向朝后，玩家无法读出英灵先向后跳开。

术语对应：

- 后撤跳跃 → retrocede de un salto
- 齐射 → salva

审校结论：必须修改

建议译文：

```text
Cuando se ve amenazado, retrocede de un salto y lanza una salva para escapar del peligro.
```

风险：动作／方向错译

审校依据：backstep volley 是后撤步配合齐射。当前 salva hacia atrás 修饰的是射击方向，和同语技能说明 Salta hacia atrás ... 不一致。

需要作者决定：无需作者决定

---

#### 批次6：命令、死亡消息、标签、制作名单和其他短文本 — 必须修改

文化分类必须用mexica；人员署名的ítems替换正确，命令／死亡消息可接受。五个地区别名只验证同步，不重复算语言问题。

键：`tag.item.echo_warrior.accessories.culture.aztec`  
使用位置：物品标签／分类显示

中文原文：墨西加饰品

英文原文：Aztec Accessories

目标语言：`es_mx`

```text
Accesorios aztecas
```

中文直译回译：阿兹特克饰品。

自然中文释义：文物文化分类沿用了通俗英雄名“阿兹特克”，没有用已锁定的历史分类“墨西加”。

术语对应：

- 墨西加 → mexica／mexicas
- 阿兹特克 → azteca

审校结论：必须修改

建议译文：

```text
Accesorios mexicas
```

风险：固定术语；双源差异

审校依据：中文写“墨西加饰品”；英文仍为 Aztec Accessories。术语表 D3 已明确文化分类和具体文物采用 Mexica；此处应服从已确认规则，英文源也应协调。

需要作者决定：无需作者决定

---

本语言总评：**仍有必须修改项**。先解决上述必须修改项，再进入接受基线所要求的视觉验收。

## UI长度、字形与实际显示风险

下表是现稿的Unicode码点数，包含空格；它不是Minecraft像素宽度，也不直接等同于溢出。交接包记录已有按宽高缩放的布局保护，本轮没有运行客户端，因此没有把保护代码的存在当作视觉验收通过。

|语言|召唤器名称|知识碎片集名称|回收箱名称|抽出单页按钮|
|---|---:|---:|---:|---:|
|`zh_tw`|5|5|7|9|
|`ja_jp`|6|6|9|12|
|`ru_ru`|29|26|32|32|
|`pt_br`|17|37|24|30|
|`es_es`|17|39|25|32|
|`es_mx`|17|39|33|32|

对应键依次为 `item.echo_warrior.test_echo_summoner`、`item.echo_warrior.knowledge_fragment_collection`、`block.echo_warrior.echo_recycler`、`gui.echo_warrior.knowledge.extract`。这些名称不是单凭长度就必须重命名的条目。

- 繁中：检查“䴉、琀、虺、夔、祓”等字形、全角标点和长知识正文的末行；“羅非魚”等一般文化用词可理解，不凭地域词偏好制造必须修改项。
- 日语：检查“ウィツィロポチトリの祝福”、知识标题、长句的换行禁则和“鑢”等字形；换行不宜让助词或闭括号孤立，缩放后仍须可读。
- 俄语：召唤器29字符、回收箱32字符，鹰首唇饰43字符；优先检查标题、分区、技能悬浮框、知识抽页按钮和动态数量。修订后的句子也须进入视觉验收。对1、2、5、21、22、25等数量应验证实际拼接，而不只检查%s数量。
- 葡语：合集37字符、鹰首唇饰39字符；检查长物品名、天赋和书页正文。消除dois acessório后，测试实际插入词；既定Desmonte可保留，结果材料用recuperados更自然。
- 西班牙西语：合集39字符、抽页按钮32字符、祝福名28字符；检查小字号可读性、百分号附近断行以及多段物品提示的完整拼接。desguace作为系统名可保留。
- 拉美西语：回收箱33字符，比西班牙版长8字符；检查名称与标题栏、知识合集与抽页按钮。必须实际拼接“Recuerda llevar una ”＋“brocha de arqueología”＋“!”；保留前缀末尾空格。现稿的Toma、ítem、brocha未带出需改成某一个国家口语的必要。

代表性界面应覆盖召唤器、每名英雄技能、天赋、教程手册、知识碎片、创造栏和物品悬浮框，至少分别在一个1.21.1和一个26.1.2客户端验收。这里只记录后续要求；本轮没有启动游戏，也没有给出任何界面“已通过”的结论。

## 需要作者决定

### A1：如何解释“狸奴”

中文源文只说古人把猫昵称为“狸奴”；英文增加了“浣熊仆人”的字面解释，俄语、葡语和两种西语据此继承。目标文本都保留了宋画“传统归属”的限定，问题集中在这个追加的词源解释，不在画作年代或谐音祝寿的翻译。

**中文问题：是否将“狸奴”统一解释为古人对猫的爱称，保留音译并删除“浣熊仆人”的字面解释？** 推荐采用这一做法，同时修正英文源。作者只需判断文化含义是否符合意图，无需选择俄、葡、西语句子。若希望保留具体动物词源解释，应先补充可靠依据并统一双源，再复核这四张审阅卡；本报告没有自行作外部词源考证。

对应键：`knowledge.echo_warrior.entry.china_guandao_late.body`；受影响目标语言：`ru_ru`、`pt_br`、`es_es`、`es_mx`。四张卡共用这一问题，只需裁决一次。

## 后续修订与验收边界

建议在明确授权的修订任务中同步双源、目标语言、兼容线和拉美别名；本报告没有执行这些更改。源文侧应处理残心的命中／环境伤害条件、袭击类口径、墨西加饰品标签、中文遗迹保底措辞以及作者裁决后的“狸奴”。英文仍使用普通inheritance指代传承的个别句子也应在同轮与已锁定Legacy协调；本轮目标译文已经使用对应的固定传承术语，因此未把正确译文另列为问题。

修订后的结构检查应再执行 `python scripts/check-localization.py`，并验证实际拼接句，而不仅是占位符数量。可直接确定的回归样例包括俄语共享名词格变化、不同数值标签、葡／西语单数共享名词配“超过一个”、拉美西语冠词、繁中“側髮／克制”，以及关键技能的命中／环境伤害限定。只有翻译审校、作者文化裁决、结构检查及代表性视觉验收符合既定流程后，才可考虑逐语言接受基线。

### 最终逐语言结论

|语言|结论|必须先处理的主要问题|
|---|---|---|
|繁中|仍有必须修改项|袭击类名称与描述、側發／剋制、遗迹保底保证措辞。|
|日语|仍有必须修改项|残心两项条件、罗马片甲名称、龟甲阵断言、唇饰专名、保证语气反转、实际使用战车的暗示。|
|俄语|仍有必须修改项|残心、命中目标、冲锋推开结果，共享占位符和动态数量格变化，死亡消息的玩家限定，狸奴释义。|
|葡语|仍有必须修改项|被动防御对象、残心、袭击类名称与描述、两个饰品的单复数、墨西加分类、狸奴释义。|
|西班牙西语|仍有必须修改项|残心、袭击类名称与描述、两个饰品的单复数、后撤步方向、历史儿童性别范围、墨西加分类、狸奴释义。|
|拉美西语|仍有必须修改项|西班牙版共同问题，以及un brocha的阴性冠词错误。|

### 审校文本快照

以下为主线文件的SHA-256；兼容线六种目标语言及全部拉美别名同步检查在本报告生成前执行。

|文件|SHA-256|
|---|---|
|`zh_cn.json`|`BDED7A2CAEB7161568A179126F7C3DBD367A49C5CB57075E612BBC50492AA0F7`|
|`en_us.json`|`5CBBBB10859837F9D919EB5D9AAFAB48C50A5CC81BDD5C4095BD9C123160C664`|
|`zh_tw.json`|`64A4DCB54267A03EB63B209EEA6A122BAD7B059313544B5CE715045E8F8543EF`|
|`ja_jp.json`|`D789F816320CFBF592A28F54D2C13D1073A0AEBFE52C6538299C29FB6E5A2FB9`|
|`ru_ru.json`|`03C6AC27D704D1D8A4334EF216E9200587215FE41A35EA9978ABDC217BC2DF4B`|
|`pt_br.json`|`C9FB2FBBC4B6058EF8F33BCFBEB72C0DA9F24834D11D47A49BC78861F5F74C1E`|
|`es_es.json`|`A433F4598364F485063F4B29FBB9D8BD119EB657FAD5502D900C1863F3A23D3D`|
|`es_mx.json`|`93594EADCD706D16F166A2CA75AEF9BC51F25BC12E7D54D6A77FA131D7551493`|

## 审校落实记录（2026-09-19）

本节记录对上述审校快照的后续落实；前文审校卡和哈希保持原样，以便追溯审校时看到的文本。

- 作者接受问题 A1：将“狸奴”统一解释为古人对猫的爱称。繁中保留“狸奴”，日语保留文化专名，俄语使用 `лину`，葡语和西语使用 `linu`；英文及目标语言均删除“浣熊仆人”的错误字面解释。
- 75 张审校卡中的 52 项必须修改与 23 项建议修改已经全部落实，共修改 84 个语言×键实例、涉及 43 个不同翻译键。修改同步进入 26.1.2 主线和 1.21.1 兼容线；`es_ar`、`es_cl`、`es_ec`、`es_uy`、`es_ve` 继续与 `es_mx` 完全一致。
- 双源一并校正了残心“攻击命中后”与持续／环境伤害边界、袭击者杀手对应 `EntityTypeTags.RAIDERS` 的范围、每处战场遗迹保证一个遗物、墨西加文化标签和 `Legacy` 固定术语。这里是文案与既有实现对齐，不是玩法改动。
- 发布门禁补充了上述高风险语义、文化释义和动态拼接的确定性守卫，避免只凭键集合和占位符数量误判为正确。
- 独立语言审校和作者文化裁决已经完成；代表性界面的双版本视觉验收仍未完成，因此所有目标语言继续保持未接受状态，不执行 `--mark-current`，也不使用 `--allow-pending`。
